package com.gee12.mytetroid.ui.storage

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import com.gee12.htmlwysiwygeditor.Dialogs.*
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.domain.NetworkHelper.IWebFileResult
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.common.utils.UriUtils
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.domain.IStorageCrypter
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.domain.interactor.*
import com.gee12.mytetroid.domain.FavoritesManager
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.NetworkHelper
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.domain.usecase.InitAppUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.TaskStage
import com.gee12.mytetroid.domain.repo.StoragesRepo
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.BaseStorageViewModel
import com.gee12.mytetroid.ui.base.TetroidActivity
import com.gee12.mytetroid.ui.base.VMEvent
import com.gee12.mytetroid.domain.usecase.storage.CheckStorageFilesExistingUseCase
import com.gee12.mytetroid.domain.usecase.storage.InitOrCreateStorageUseCase
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.domain.usecase.crypt.*
import com.gee12.mytetroid.domain.usecase.file.GetFileModifiedDateUseCase
import com.gee12.mytetroid.domain.usecase.file.GetFolderSizeUseCase
import com.gee12.mytetroid.domain.usecase.node.GetNodeByIdUseCase
import com.gee12.mytetroid.domain.usecase.record.GetRecordByIdUseCase
import com.gee12.mytetroid.domain.usecase.storage.ReadStorageUseCase
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*

open class StorageViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,

    commonSettingsProvider: CommonSettingsProvider,
    storageProvider: IStorageProvider,
    val buildInfoProvider: BuildInfoProvider,
    val sensitiveDataProvider: ISensitiveDataProvider,
    val storagesRepo: StoragesRepo,
    val storagePathProvider: IStoragePathProvider,
    val recordPathProvider: IRecordPathProvider,
    val dataNameProvider: IDataNameProvider,

    val storageCrypter: IStorageCrypter,

    val interactionInteractor: InteractionInteractor,
    val syncInteractor: SyncInteractor,
    val favoritesManager: FavoritesManager, //TODO
    val passInteractor: PasswordInteractor, //TODO
    val tagsInteractor: TagsInteractor, //TODO
    val trashInteractor: TrashInteractor,

    protected val initAppUseCase: InitAppUseCase,
    protected val getFileModifiedDateUseCase: GetFileModifiedDateUseCase,
    protected val getFolderSizeUseCase: GetFolderSizeUseCase,

    protected val initOrCreateStorageUseCase: InitOrCreateStorageUseCase,
    protected val readStorageUseCase: ReadStorageUseCase,
    protected val saveStorageUseCase: SaveStorageUseCase,
    protected val checkStoragePasswordUseCase: CheckStoragePasswordAndAskUseCase,
    protected val changePasswordUseCase: ChangePasswordUseCase,
    protected val decryptStorageUseCase: DecryptStorageUseCase,
    protected val checkStoragePasswordAndDecryptUseCase: CheckStoragePasswordAndDecryptUseCase,
    protected val checkStorageFilesExistingUseCase: CheckStorageFilesExistingUseCase,
    protected val setupPasswordUseCase: SetupPasswordUseCase,
    protected val initPasswordUseCase: InitPasswordUseCase,

    protected val getNodeByIdUseCase: GetNodeByIdUseCase,
    protected val getRecordByIdUseCase: GetRecordByIdUseCase,
) : BaseStorageViewModel(
    app,
    resourcesProvider,
    logger,
    notificator,
    failureHandler,
    commonSettingsProvider,
    storageProvider,
) {

    private var isPinNeedEnter = false

    init {
        // первоначальная инициализация компонентов приложения
        initAppUseCase.execute(InitAppUseCase.Params)
            .onFailure {
                logFailure(it)
            }
    }

    var isLoadAllNodesForced = false
    var isAlreadyTryDecrypt = false
    var syncType = SyncStorageType.Manually
    var syncCallback: ICallback? = null


    //region Init

    fun checkStorageIsReady(checkIsFavorMode: Boolean, showMessage: Boolean): Boolean {
        return when {
            !isStorageInited() -> {
                if (showMessage) {
                    showMessage(
                        if (permissionInteractor.hasWriteExtStoragePermission(getContext())) R.string.mes_storage_must_be_inited
                        else R.string.mes_must_grant_perm_and_storage_inited
                    )
                }
                false
            }
            !isStorageLoaded() -> {
                if (showMessage) {
                    showMessage(R.string.mes_storage_must_be_loaded)
                }
                false
            }
            checkIsFavorMode && isLoadedFavoritesOnly() -> {
                if (showMessage) {
                    showMessage(R.string.mes_all_nodes_must_be_loaded)
                }
                false
            }
            else -> {
                true
            }
        }
    }

    /**
     * Инициализация хранилища по ID, переданному в Intent.
     */
    open fun initStorage(intent: Intent): Boolean {
        val storageId = intent.getIntExtra(Constants.EXTRA_STORAGE_ID, 0)

        return if (storage != null && storage?.id == storageId) {
            launchOnMain {
                storage?.let {
                    sendEvent(StorageEvent.FoundInBase(it))
                    sendEvent(StorageEvent.Inited(it))
                }
            }
            true
        } else {
            if (storageId > 0) {
                startInitStorageFromBase(storageId)
                true
            } else {
                initStorageFromLastStorageId()
            }
        }
    }

    /**
     * Инициализация хранилища по ID хранилища, загруженному в последний раз.
     */
    fun initStorageFromLastStorageId(): Boolean {
        val storageId = CommonSettings.getLastStorageId(getContext())
        return if (storageId > 0) {
            startInitStorageFromBase(storageId)
            true
        } else {
            logError(getString(R.string.log_not_transferred_storage_id), true)
            launchOnMain {
                sendEvent(BaseEvent.FinishActivity)
            }
            false
        }
    }

    fun startReloadStorageEntity() {
        storage?.let { currentStorage ->
            launchOnIo {
                val storageId = currentStorage.id
                val storage = storagesRepo.getStorage(storageId)
                if (storage != null) {
                    storageProvider.setStorage(currentStorage.resetFields(storage))

                    sendEvent(StorageEvent.FoundInBase(storage))
                } else {
                    sendEvent(StorageEvent.NotFoundInBase(storageId))
                    log(getString(R.string.log_storage_not_found_mask, storageId), show = true)
                }
            }
        }
    }

    override fun startInitStorageFromBase(storageId: Int) {
        launchOnIo {
            val storage = storagesRepo.getStorage(storageId)
            if (storage != null) {
                storageProvider.setStorage(storage)

                withMain {
                    sendEvent(StorageEvent.FoundInBase(storage))
                }

                // если используется уже загруженное дерево веток из кэша
                if (storageProvider.isLoaded()) {
                    storage.isLoaded = true
                }

                // загружаем настройки, но не загружаем само хранилище
                initStorage()
            } else {
                withMain {
                    sendEvent(StorageEvent.NotFoundInBase(storageId = storageId))
                }
                log(getString(R.string.log_storage_not_found_mask, storageId))
            }
        }
    }

    /**
     * Запуск первичной инициализации хранилища по-умолчанию с указанием флага isCheckFavorMode
     * @param isLoadAllNodesForced Если true, то всегда загружать хранилище полностью.
     */
    fun startInitStorage(storageId: Int, isLoadAllNodesForced: Boolean) {
        this.isLoadAllNodesForced = isLoadAllNodesForced
        startInitStorage(storageId)
    }

    /**
     * Поиск хранилища по-умолчанию в базе данных и запуск первичной его инициализации.
     */
    fun startInitStorage() {
        log(R.string.log_start_load_def_storage)
        launchOnMain {
            val storageId = storagesRepo.getDefaultStorageId()
            if (storageId > 0) {
                startInitStorage(storageId)
            } else {
                log(R.string.log_def_storage_not_specified)
                sendEvent(StorageEvent.NoDefaultStorage)
            }
        }
    }

    /**
     * Поиск хранилища в базе данных и запуск первичной его инициализация.
     */
    open fun startInitStorage(storageId: Int) {
        launchOnIo {
            val storage = storagesRepo.getStorage(storageId)
            if (storage != null) {
                sendEvent(StorageEvent.FoundInBase(storage))
                startInitStorage(storage)
            } else {
                sendEvent(StorageEvent.NotFoundInBase(storageId))
                log(getString(R.string.log_storage_not_found_mask, storageId), true)
            }
        }
    }

    /**
     * Запуск первичной инициализации хранилища.
     * Начинается с проверки разрешения на запись во внешнюю память устройства.
     */
    fun startInitStorage(storage: TetroidStorage) {
        storageProvider.setStorage(storage)

        CommonSettings.setLastStorageId(getContext(), storage.id)

        // сначала проверяем разрешение на запись во внешнюю память
        launchOnMain {
            sendEvent(BaseEvent.PermissionCheck)
        }
    }

    /**
     * Запуск перезагрузки хранилища.
     *
     * TODO: может здесь нужно сразу loadStorage() ?
     */
    fun startReinitStorage() {
        storage?.id?.let {
            startInitStorage(it)
        }
    }

    /**
     * Инициализация хранилища (с созданием файлов, если оно новое).
     */
    fun initStorage(isLoadFavoritesOnly: Boolean? = null, isLoadAfter: Boolean = false) {
        initStorage(
            storage = storage!!, // FIXME
            isLoadFavoritesOnly = isLoadFavoritesOnly,
            isLoadAfter = isLoadAfter,
        )
    }
    fun initStorage(storage: TetroidStorage, isLoadFavoritesOnly: Boolean? = null, isLoadAfter: Boolean = false) {
        launchOnMain {
            isAlreadyTryDecrypt = false
            withIo {
                initOrCreateStorageUseCase.run(
                    InitOrCreateStorageUseCase.Params(
                        storage = storage,
                        databaseConfig = storageProvider.databaseConfig,
                    )
                )
            }.onFailure { failure ->
                logFailure(failure)
                //logError(getString(R.string.log_failed_storage_init) + getStoragePath(), false)
                sendEvent(
                    StorageEvent.InitFailed(
                        isOnlyFavorites = isLoadFavoritesOnly ?: checkIsNeedLoadFavoritesOnly()
                    )
                )
            }.onSuccess { result ->
                when (result) {
                    is InitOrCreateStorageUseCase.Result.CreateStorage -> {
                        logger.log(R.string.log_storage_created, true)
                        sendEvent(StorageEvent.FilesCreated(storage))
                    }
                    is InitOrCreateStorageUseCase.Result.InitStorage -> {

                    }
                }
                log(getString(R.string.log_storage_inited) + getStoragePath())
                sendEvent(StorageEvent.Inited(storage))
                if (isLoadAfter) {
                    startLoadStorage(
                        isLoadFavoritesOnly = isLoadFavoritesOnly ?: checkIsNeedLoadFavoritesOnly()
                    )
                }
            }
        }
    }

    /**
     * Инициализация хранилища (с созданием файлов, если оно новое),
     *   а затем его непосредственная загрузка (с расшифровкой, если зашифровано).
     * Вызывается уже после выполнения синхронизации.
     */
    fun initStorageAndLoad() {
        initStorage(
            isLoadFavoritesOnly = checkIsNeedLoadFavoritesOnly(),
            isLoadAfter = true
        )
    }

    fun startLoadStorage(
        isLoadFavoritesOnly: Boolean,
        isHandleReceivedIntent: Boolean = true,
        isAllNodesLoading: Boolean = false,
        isNeedDecrypt: Boolean = true,
    ) {
        val params = StorageParams(
            storage = storage!!,
            node = null,
            isNodeOpening = false,
            isLoadFavoritesOnly = isLoadFavoritesOnly,
            isHandleReceivedIntent = isHandleReceivedIntent,
            isAllNodesLoading = isAllNodesLoading
        )
        if (isStorageCrypted() && isNeedDecrypt) {
            // сначала устанавливаем пароль, а потом загружаем (с расшифровкой)
            params.isDecrypt = true
            checkPassAndDecryptStorage(params)
        } else {
            // загружаем
            loadOrDecryptStorage(params)
        }
    }

    /**
     * Читаем установленную опцию isLoadAllNodesForced только 1 раз.
     * isLoadAllNodesForced может быть = true, когда, например:
     *  1) загружаем хранилище из временной записи, созданной из виджета.
     *  2) загружаем хранилище из настроек при выборе quicklyNode.
     *  В этих случаях хранилище нужно загружать полностью.
     */
    fun checkIsNeedLoadFavoritesOnly(): Boolean {
        return if (isLoadAllNodesForced) {
            // уже воспользовались, сбрасываем
            this.isLoadAllNodesForced = false
            false
        } else {
            !isStorageLoaded() && isLoadFavoritesOnly()
                    || (isStorageLoaded() && isLoadedFavoritesOnly())
        }
    }

    fun onStoragePathChanged() {
        startReinitStorage()
    }

    //endregion Init

    // region Load

    /**
     * Непосредственное чтение или расшифровка (если зашифровано) данных хранилища.
     *
     * @param node            Зашифрованная ветка, которую нужно открыть после засшифровки.
     * @param isDecrypt       Нужно ли вызвать процесс расшифровки хранилища.
     * @param isOnlyFavorites Нужно ли загружать только избранные записи
     * @param isHandleReceivedIntent  Нужно ли после загрузки открыть ветку, сохраненную в опции getLastNodeId()
     * или ветку с избранным (если именно она передана в node)
     */
    open fun loadOrDecryptStorage(params: StorageParams) {
        // расшифровуем хранилище только в том случаем, если:
        //  1) не используем проверку ПИН-кода
        //  2) используем проверку ПИН-кода, при этом расшифровуем с открытием конкретной <b>зашифрованной</b> ветки
        //   (или ветки Избранное)
        val node = params.node
        var isDecrypt = params.isDecrypt ?: false
        isDecrypt = (isDecrypt
                && (!isRequestPINCode()
                    || isRequestPINCode() && node != null
                        && (node.isCrypted || node == FavoritesManager.FAVORITES_NODE)))
        if (isStorageLoaded() && isDecrypt && isNodesExist()) {
            // расшифровываем уже загруженное хранилище
            startDecryptStorage(node)
        } else {
            // загружаем хранилище впервые, с расшифровкой (если нужно)
            startReadStorage(
                storage = params.storage,
                isDecrypt = isDecrypt,
                isFavoritesOnly = params.isLoadFavoritesOnly,
                isOpenLastNode = params.isHandleReceivedIntent
            )
        }
    }

    //endregion Load

    //region Read

    /**
     * Непосредственная загрузка структуры хранилища.
     * @param isDecrypt Необходимость расшифровки зашифрованных веток.
     */
    private fun startReadStorage(
        storage: TetroidStorage,
        isDecrypt: Boolean,
        isFavoritesOnly: Boolean,
        isOpenLastNode: Boolean
    ) {
        logOperStart(LogObj.STORAGE, LogOper.LOAD)

        launchOnMain {
            sendEvent(
                BaseEvent.TaskStarted(
                    if (isDecrypt) R.string.task_storage_decrypting else R.string.task_storage_loading
                )
            )

            val result = withIo {
                readStorageUseCase.run(
                    ReadStorageUseCase.Params(
                        storageProvider = storageProvider,
                        storage = storage,
                        isDecrypt = isDecrypt,
                        isFavoritesOnly = isFavoritesOnly,
                        isOpenLastNode = isOpenLastNode,
                    )
                )
            }.foldResult(
                onLeft = { failure ->
                    logFailure(failure, show = true)
                    sendEvent(BaseEvent.TaskFinished)

                    // TODO ?
                    //logWarning(getString(R.string.log_failed_storage_load_mask) + getStoragePath(), true)
                    //postViewEvent(Constants.ViewEvents.ShowMoreInLogs)
                    false
                },
                onRight = {
                    sendEvent(BaseEvent.TaskFinished)

                    storageProvider.setStorage(storage)

                    val mes = getString(
                        when {
                            isFavoritesOnly -> R.string.log_storage_favor_loaded_mask
                            isDecrypt -> R.string.log_storage_loaded_decrypted_mask
                            else -> R.string.log_storage_loaded_mask
                        },
                        getStorageName()
                    )
                    log(mes, show = true)

                    // загрузка ветки для быстрой вставки
                    updateQuicklyNode()
                    true
                }
            )

            // инициализация контролов
            sendEvent(
                BaseEvent.InitUI(
                    storage = storage,
                    isLoadFavoritesOnly = isFavoritesOnly,
                    isHandleReceivedIntent = isOpenLastNode,
                    result = result
                )
            )
            // действия после загрузки хранилища
            sendEvent(StorageEvent.Loaded(result))

        }
    }

    //endregion Read

    //region Decrypt

    /**
     * Непосредственная расшифровка уже загруженного хранилища.
     */
    private fun startDecryptStorage(node: TetroidNode?) {
        logOperStart(LogObj.STORAGE, LogOper.DECRYPT)

        launchOnMain {
            // перед расшифровкой
            sendEvent(BaseEvent.TaskStarted(R.string.task_storage_decrypting))

            // непосредственная расшифровка
            withIo {
                decryptStorageUseCase.run(
                    DecryptStorageUseCase.Params(decryptFiles = false)
                )
            }.map { result ->
                result.also { setIsDecrypted(result) }
            }.onFailure {
                logFailure(it)
                logDuringOperErrors(LogObj.STORAGE, LogOper.DECRYPT, show = true)
                afterStorageDecrypted(null)
            }.onSuccess { result ->
                if (result) {
                    log(R.string.log_storage_decrypted, show = true)
                    // действия после расшифровки хранилища
                    afterStorageDecrypted(node)
                } else {
                    logDuringOperErrors(LogObj.STORAGE, LogOper.DECRYPT, show = true)
                    afterStorageDecrypted(null)
                }
            }
            // после расшифровки
            sendEvent(BaseEvent.TaskFinished)
        }
    }

    open fun afterStorageDecrypted(node: TetroidNode?) {
        launchOnMain {
            sendEvent(StorageEvent.Decrypted/*, node*/)
        }
    }

    /**
     * Проверка и установка пароля с последующим запуском расшифровки хранилаща.
     * Вызывается при:
     * 1) запуске приложения, если есть зашифрованные ветки и сохранен пароль
     * 2) запуске приложения, если есть зашифрованные ветки и установлен isAskPasswordOnStart
     * 3) запуске приложения, если выделение было сохранено на зашифрованной ветке
     * 4) выборе зашифрованной ветки
     * 5) выборе зашифрованной записи в избранном
     *
     * @param node            Зашифрованная ветка, которую нужно открыть после засшифровки.
     * @param isNodeOpening   Вызвана ли функция при попытке открытия зашифрованной ветки
     * @param isOnlyFavorites Нужно ли загружать только избранные записи
     * @param isHandleReceivedIntent  Нужно ли после загрузки открыть ветку, сохраненную в опции getLastNodeId(),
     * или ветку с избранным (если именно она передана в node)
     */
    private fun checkPassAndDecryptStorage(params: StorageParams) {
        val callbackEvent = StorageEvent.LoadOrDecrypt(params)

        // устанавливаем признак
        setIsPINNeedToEnter()

        launchOnMain {
            withIo {
                checkStoragePasswordAndDecryptUseCase.run(
                    CheckStoragePasswordAndDecryptUseCase.Params(
                        params = params,
                        storage = storage,
                        isAlreadyTryDecrypt = isAlreadyTryDecrypt,
                    )
                )
            }.onFailure {
                logFailure(it)
            }.onSuccess { result ->
                when (result) {
                    is CheckStoragePasswordAndDecryptUseCase.Result.AskPassword -> {
                        askPassword(callbackEvent)
                    }
                    is CheckStoragePasswordAndDecryptUseCase.Result.AskPin -> {
                        askPinCode(params.isNodeOpening, callbackEvent)
                    }
                    is CheckStoragePasswordAndDecryptUseCase.Result.LoadWithoutDecrypt -> {
                        isAlreadyTryDecrypt = true
                        loadOrDecryptStorage(params)
                    }
                    is CheckStoragePasswordAndDecryptUseCase.Result.AskForEmptyPassCheckingField -> {
                        sendEvent(
                            StorageEvent.AskForEmptyPassCheckingField(
                                fieldName = result.fieldName,
                                passHash = result.passHash,
                                callbackEvent = callbackEvent
                            )
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun checkAndDecryptNode(node: TetroidNode): Boolean {
        if (!node.isNonCryptedOrDecrypted) {
            val params = StorageParams(
                storage = storage!!,
                node = node,
                isDecrypt = true,
                isNodeOpening = true,
                isLoadFavoritesOnly = false,
                isHandleReceivedIntent = false
            )

            checkPassAndDecryptStorage(params)
            // выходим,запрос пароля будет в асинхронном режиме
            return true
        }
        return false
    }

    fun checkAndDecryptRecord(record: TetroidRecord): Boolean {
        if (record.isFavorite && !record.isNonCryptedOrDecrypted) {
            // запрос на расшифровку записи может поступить только из списка Избранных записей,
            //  поэтому отправляем FAVORITES_NODE
            val params = StorageParams(
                storage = storage!!, // FIXME
                isDecrypt = true,
                node = FavoritesManager.FAVORITES_NODE,
                isNodeOpening = true,
                isLoadFavoritesOnly = isLoadFavoritesOnly(),
                isHandleReceivedIntent = false
            )

            checkPassAndDecryptStorage(params)
            // выходим,запрос пароля будет в асинхронном режиме
            return true
        }
        return false
    }

    fun setIsPINNeedToEnter() {
        isPinNeedEnter = true
    }

    fun clearSavedPass() {
        storage?.let {
            passInteractor.clearSavedPass(it)
            updateStorageAsync(it)
        }
    }

    //endregion Decrypt

    //region Sync

    /**
     * Запуск синхронизации вручную.
     */
    fun syncStorage(activity: Activity) {
        syncStorageAndRunCallback(activity, SyncStorageType.Manually, null)
    }

    /**
     * Запуск синхронизации (если она включена) и загрузка хранилища.
     * Выполняется после проверки разрешения на запись во внешнюю память устройства.
     */
    fun syncAndInitStorage(activity: Activity) {
        syncStorageAndRunCallback(activity, SyncStorageType.BeforeInit) {
            initStorageAndLoad()
        }
    }

    /**
     * Запуск синхронизации (если она включена) и выход из приложения.
     */
    fun syncStorageAndExit(activity: Activity, callback: ICallback?) {
        syncStorageAndRunCallback(activity, SyncStorageType.BeforeExit, callback)
    }

    /**
     * Отправка запроса на синхронизацию стороннему приложению перед выполнением действия в callback.
     * @param activity
     * @param callback Обработчик события после выполнения синхронизации или вместо нее.
     */
    fun syncStorageAndRunCallback(activity: Activity, syncType: SyncStorageType, callback: ICallback?) {
        getStorageSyncProfile()?.let {
            if (it.isEnabled) {
                this.syncType = syncType
                this.syncCallback = callback

                when {
                    // запуск синхронизации вручную в меню
                    syncType == SyncStorageType.Manually -> {
                        startStorageSync(activity, callback)
                    }
                    // перед загрузкой хранилища
                    syncType == SyncStorageType.BeforeInit && it.isSyncBeforeInit -> {
                        if (it.isAskBeforeSyncOnInit) {
                            launchOnMain {
                                sendEvent(StorageEvent.AskOnSync.BeforeInit(callback))
                            }
                        } else {
                            startStorageSync(activity, callback)
                        }
                    }
                    // перед выходом из приложения
                    syncType == SyncStorageType.BeforeExit && it.isSyncBeforeExit -> {
                        if (it.isAskBeforeSyncOnExit) {
                            launchOnMain {
                                sendEvent(StorageEvent.AskOnSync.BeforeExit(callback))
                            }
                        } else {
                            startStorageSync(activity, callback)
                        }
                    }
                    else -> {
                        callback?.run(true)
                    }
                }

            } else {
                callback?.run(true)
            }
        } ?: callback?.run(true)
    }

    fun cancelStorageSync(callback: ICallback?) {
        callback?.run(true)
    }

    /**
     * Отправка запроса на синхронизацию стороннему приложению перед загрузкой хранилища.
     * @param activity
     * @param callback
     */
    fun startStorageSync(activity: Activity, callback: ICallback?) {
        if (storage?.syncProfile?.appName == getString(R.string.title_app_termux)) {
            if (!checkTermuxPermission(activity)) {
                return
            }
        }
        val result = syncInteractor.startStorageSync(
            activity = activity,
            storagePath = getStoragePath(),
            command = storage?.syncProfile?.command.orEmpty(),
            appName = storage?.syncProfile?.appName.orEmpty(),
            requestCode = Constants.REQUEST_CODE_SYNC_STORAGE
        )
        if (callback != null) {
            // запускаем обработчик сразу после синхронизации, не дожидаясь ответа, если:
            //  1) синхронизацию не удалось запустить
            //  2) выбрана синхронизация с помощью приложения, не предусматривающего ответ
            val waitSyncResult = syncInteractor.isWaitSyncResult(
                appName = storage?.syncProfile?.appName.orEmpty()
            )
            if (!result || !waitSyncResult) {
                callback.run(result)
            }
        }
    }

    /**
     * Обработка результата синхронизации хранилища.
     *
     * @param result
     */
    fun onStorageSyncFinished(result: Boolean) {
        log(if (result) R.string.log_sync_successful else R.string.log_sync_failed, true)
        when (syncType) {
            SyncStorageType.Manually -> {
                syncCallback?.run(result)
            }
            SyncStorageType.BeforeInit -> {
                if (result) {
                    syncCallback?.run(result)
                } else {
                    launchOnMain {
                        sendEvent(StorageEvent.AskOnSync.AfterInit(result))
                    }
                }
            }
            SyncStorageType.BeforeExit -> {
                if (result) {
                    syncCallback?.run(result)
                } else {
                    launchOnMain {
                        sendEvent(StorageEvent.AskOnSync.AfterExit(result))
                    }
                }
            }
        }
        syncType = SyncStorageType.Manually
        syncCallback = null
    }

    //endregion Sync

    //region Records

    // TODO: вынести в конкретную VM
    suspend fun getEditedDate(record: TetroidRecord): Date? {
        return withMain {
            withIo {
                getFileModifiedDateUseCase.run(
                    GetFileModifiedDateUseCase.Params(
                        filePath = recordPathProvider.getPathToFileInRecordFolder(record, record.fileName),
                    )
                )
            }.foldResult(
                onLeft = { null },
                onRight = { it }
            )
        }
    }

    // TODO: вынести в конкретную VM
    suspend fun getRecordFolderSize(record: TetroidRecord): String? {
        return withMain {
            withIo {
                getFolderSizeUseCase.run(
                    GetFolderSizeUseCase.Params(
                        folderPath = recordPathProvider.getPathToRecordFolder(record),
                    )
                )
            }.foldResult(
                onLeft = { null },
                onRight = { it }
            )
        }
    }

    //endregion Records

    //region Nodes

    suspend fun getNode(nodeId: String): TetroidNode? {
        return withIo {
            getNodeByIdUseCase.run(
                GetNodeByIdUseCase.Params(nodeId)
            )
        }.foldResult(
            onLeft = {
                logFailure(it)
                null
            },
            onRight = { it }
        )
    }

    suspend fun getRecord(recordId: String): TetroidRecord? {
        return withIo {
            getRecordByIdUseCase.run(
                GetRecordByIdUseCase.Params(
                    recordId = recordId,
                )
            )
        }.foldResult(
            onLeft = {
                logFailure(it)
                null
            },
            onRight = { it }
        )
    }

    /**
     * Получение иерархии веток. В корне стека - исходная ветка, на верхушке - ее самый дальний предок.
     * @param node
     * @return
     */
    fun createNodesHierarchy(node: TetroidNode): Stack<TetroidNode>? {
        val hierarchy = Stack<TetroidNode>()
        createNodesHierarchy(hierarchy, node)
        return hierarchy
    }

    private fun createNodesHierarchy(hierarchy: Stack<TetroidNode>, node: TetroidNode) {
        hierarchy.push(node)
        if (node.level > 0) {
            createNodesHierarchy(hierarchy, node.parentNode)
        }
    }

    /**
     * Проверка существования зашифрованных веток.
     */
    protected fun checkExistenceCryptedNodes() {
        launchOnMain {
            if (!isExistCryptedNodes(true)) {
                sendEvent(StorageEvent.AskForClearStoragePass)
            }
        }
    }

    /**
     * Загрузка всех веток, когда загружено только избранное.
     * @param isHandleReceivedIntent Нужно ли обработать [receivedIntent] после загрузки веток.
     */
    fun loadAllNodes(isHandleReceivedIntent: Boolean) {
        startLoadStorage(
            isLoadFavoritesOnly = false,
            isHandleReceivedIntent = isHandleReceivedIntent,
            isAllNodesLoading = true
        )
    }

    //endregion Nodes

    //region Attaches

    /**
     * Загрузка файла по URL в каталог кэша на устройстве.
     * @param url
     * @param callback
     */
    open suspend fun downloadFileToCache(url: String?, callback: TetroidActivity.IDownloadFileResult?) {
        if (TextUtils.isEmpty(url)) {
            logError(R.string.log_link_is_empty)
            return
        }
        this.sendEvent(
            BaseEvent.ShowProgressText(
                message = getString(R.string.title_file_downloading)
            )
        )
        var fileName = UriUtils.getFileName(url)
        if (TextUtils.isEmpty(fileName)) {
//            Exception ex = new Exception("");
//            if (callback != null) {
//                callback.onError(ex);
//            }
//            TetroidLog.log(TetroidActivity.this, getString(R.string.log_error_download_file_mask, ex.getMessage()), Toast.LENGTH_LONG);
//            return;
            fileName = dataNameProvider.createDateTimePrefix()
        }
        val outputFileName: String = getExternalCacheDir() + "/" + fileName
        NetworkHelper.downloadFileAsync(url, outputFileName, object : IWebFileResult {
            override fun onSuccess() {
                callback?.onSuccess(Uri.fromFile(File(outputFileName)))
                launchOnMain {
                    sendEvent(BaseEvent.ShowProgress(isVisible = false))
                }
            }

            override fun onError(ex: Exception) {
                callback?.onError(ex)
                logError(getString(R.string.log_error_download_file_mask, ex.message ?: ""))
                launchOnMain {
                    sendEvent(BaseEvent.ShowProgress(isVisible = false))
                }
            }
        })
    }

    /**
     * Получение полного имени файла.
     */
    // TODO: вынести в место использования
    fun getAttachFullName(attach: TetroidFile?): String? {
        if (attach == null) {
            return null
        }
        val record = attach.record
        if (record == null) {
            logger.logError(resourcesProvider.getString(R.string.log_file_record_is_null))
            return null
        }
        val ext = FileUtils.getExtensionWithComma(attach.name)
        return recordPathProvider.getPathToFileInRecordFolder(record, attach.id + ext)
    }

    fun getAttachEditedDate(context: Context, attach: TetroidFile): Date? {
        return try {
            FileUtils.getFileModifiedDate(context, getAttachFullName(attach))
        } catch (ex: java.lang.Exception) {
            logger.logError(resourcesProvider.getString(R.string.error_get_attach_file_size_mask, ex.localizedMessage), false)
            null
        }
    }

    fun getAttachFileSize(context: Context, attach: TetroidFile): String? {
        return try {
            FileUtils.getFileSize(context, getAttachFullName(attach))
        } catch (ex: java.lang.Exception) {
            logger.logError(resourcesProvider.getString(R.string.error_get_attach_file_size_mask, ex.localizedMessage), false)
            null
        }
    }

    //endregion Attaches

    //region Other

    // TODO SwapTetroidObjectsUseCase
    suspend fun swapTetroidObjects(list: List<Any>, pos: Int, isUp: Boolean, through: Boolean): Int {
        return if (swapTetroidObjectsDirectly(list, pos, isUp, through)) {
            if (saveStorage()) 1 else -1
        } else 0
    }

    /**
     * Замена местами 2 объекта хранилища в списке.
     * @param list
     * @param pos
     * @param isUp
     * @return 1 - успешно
     * 0 - перемещение невозможно (пограничный элемент)
     * -1 - ошибка
     */
    protected fun swapTetroidObjectsDirectly(
        list: List<*>?,
        pos: Int, isUp: Boolean,
        through: Boolean
    ): Boolean {
        return try {
            Utils.swapListItems(list, pos, isUp, through)
        } catch (ex: Exception) {
            logger.logError(ex, false)
            false
        }
    }

    fun getExternalCacheDir() = getContext().externalCacheDir.toString()

    fun checkStorageFilesExistingError(): String? {
        return checkStorageFilesExistingUseCase.execute(
            CheckStorageFilesExistingUseCase.Params(storage!!)
        ).foldResult(
            onLeft = {
                failureHandler.getFailureMessage(it).title
            },
            onRight = { result ->
                when (result) {
                    is CheckStorageFilesExistingUseCase.Result.Error -> {
                        result.errorsString
                    }
                    is CheckStorageFilesExistingUseCase.Result.Success -> {
                        null
                    }
                }
            }
        )
    }

    //endregion Other

    //region Encryption

    override fun isStorageCrypted(): Boolean {
        var iniFlag = false
        try {
            iniFlag = storageProvider.databaseConfig.isCryptMode
        } catch (ex: Exception) {
            logError(ex, true)
        }
        return iniFlag || isExistCryptedNodes(false)
    }

    /**
     * Проверка существования шифрованных веток в хранилище.
     */
    // TODO: CheckIsExistEncryptedNodesUseCase
    // TODO: можно перенести в BaseStorageViewModel
    private fun isExistCryptedNodes(recheck: Boolean): Boolean {
        var isExistCryptedNodes = storageProvider.isExistCryptedNodes()
        if (recheck) {
            storageProvider.setIsExistCryptedNodes(isExistCryptedNodes(storageProvider.getRootNodes()))
            isExistCryptedNodes = storageProvider.isExistCryptedNodes()
        }
        return isExistCryptedNodes
    }

    private fun isExistCryptedNodes(nodes: List<TetroidNode>): Boolean {
        for (node in nodes) {
            if (node.isCrypted) return true
            if (node.subNodesCount > 0) {
                if (isExistCryptedNodes(node.subNodes)) return true
            }
        }
        return false
    }

    //region Password

    /**
     * Асинхронная проверка - имеется ли сохраненный пароль, и его запрос при необходимости.
     * Используется:
     *      * когда хранилище уже загружено (зашифровка/сброс шифровки ветки)
     *      * либо когда загрузка хранилища не требуется (установка/сброс ПИН-кода)
     * @param callback
     */
    fun checkStoragePass(callbackEvent: VMEvent) {
        launchOnMain {
            withIo {
                checkStoragePasswordUseCase.run(
                    CheckStoragePasswordAndAskUseCase.Params(
                        storage = storage,
                        isStorageCrypted = isStorageCrypted(),
                    )
                )
            }.onFailure { failure ->
                logFailure(failure)
            }.onSuccess { result ->
                when (result) {
                    is CheckStoragePasswordAndAskUseCase.Result.AskPassword -> {
                        askPassword(callbackEvent)
                    }
                    is CheckStoragePasswordAndAskUseCase.Result.AskPin -> {
                        askPinCode(true, callbackEvent)
                    }
                    is CheckStoragePasswordAndAskUseCase.Result.AskForEmptyPassCheckingField -> {
                        sendEvent(
                            StorageEvent.AskForEmptyPassCheckingField(
                                fieldName = result.fieldName,
                                passHash = result.passHash,
                                callbackEvent = callbackEvent,
                            )
                        )
                    }
                }
            }
        }
    }

    fun confirmEmptyPassCheckingFieldDialog(passHash: String, callbackEvent: VMEvent) {
//        cryptInteractor.initCryptPass(passHash, true)
        storageCrypter.setKeyFromMiddleHash(passHash)
        askPinCode(true, callbackEvent)
    }

    fun cancelEmptyPassCheckingFieldDialog(callbackEvent: VMEvent) {
        when (callbackEvent) {
            is StorageEvent.LoadOrDecrypt -> {
                if (!callbackEvent.params.isNodeOpening) {
                    // загружаем хранилище без расшифровки
                    callbackEvent.params.isDecrypt = false
                    loadOrDecryptStorage(callbackEvent.params)
                }
            }
            else -> {}
        }
    }

    /**
     * Отображения запроса пароля от хранилища.
     */
    fun askPassword(callbackEvent: VMEvent) {
        logger.log(R.string.log_show_pass_dialog, false)
        // выводим окно с запросом пароля в асинхронном режиме
        launchOnMain {
            sendEvent(StorageEvent.AskPassword(callbackEvent))
        }
    }

    fun onPasswordEntered(password: String, isSetup: Boolean, callbackEvent: VMEvent) {
        if (isSetup) {
            launchOnMain {
                setupPass(password)
                sendEventFromCallbackParam(callbackEvent)
            }
        } else {
            checkPassOnDecrypt(
                password = password,
                callbackEvent = callbackEvent,
            )
        }
    }

    fun onPasswordCanceled(isSetup: Boolean, callbackEvent: VMEvent) {
        if (!isSetup) {
            isAlreadyTryDecrypt = true
            //super.onPasswordCanceled(isSetup, callback)
            // FIXME: не уверен в решении ..
            when (callbackEvent) {
                is StorageEvent.LoadOrDecrypt -> {
                    if (!callbackEvent.params.isNodeOpening) {
                        //isAlreadyTryDecrypt = true
                        callbackEvent.params.isDecrypt = false
                        launchOnMain {
                            sendEventFromCallbackParam(callbackEvent)
                        }
                    }
                }
            }
        }
    }

    fun startSetupPass(password: String) {
        launchOnMain {
            setupPass(password)
        }
    }

    suspend fun setupPass(password: String) {
        log(R.string.log_start_pass_setup)
        sendEvent(BaseEvent.TaskStarted(R.string.task_pass_setting))
        isBusy = true

        withIo {
            setupPasswordUseCase.run(
                SetupPasswordUseCase.Params(
                    storage = storage!!,
                    databaseConfig = storageProvider.databaseConfig,
                    password = password,
                )
            )
        }.onComplete {
            isBusy = false
            sendEvent(BaseEvent.TaskFinished)
        }.onFailure {
            // TODO
            //logger.log(R.string.log_pass_set_error, true)
            logFailure(it)
        }.onSuccess {
            logger.log(R.string.log_pass_setted, true)
            sendEvent(StorageEvent.PassSetuped)
        }
    }

    private suspend fun initPassword(password: String) {
        withIo {
            initPasswordUseCase.run(
                InitPasswordUseCase.Params(
                    storage = storage!!,
                    databaseConfig = storageProvider.databaseConfig,
                    password = password,
                )
            )
        }.onFailure {
            // TODO
            //logger.log(R.string.log_pass_set_error, true)
            logFailure(it)
        }.onSuccess {
            //logger.log(R.string.log_pass_inited, true)
            //sendEvent(StorageEvent.PasswordInited)
        }
    }

    private fun checkPassOnDecrypt(password: String, callbackEvent: VMEvent) {
        try {
            if (passInteractor.checkPass(password)) {
                launchOnMain {
                    initPassword(password)
                    sendEventFromCallbackParam(callbackEvent)
                }
            } else {
                logger.logError(R.string.log_pass_is_incorrect, show = true)
                // повторяем запрос
                askPassword(callbackEvent)
            }
        } catch (ex: DatabaseConfig.EmptyFieldException) {
            // если поля в INI-файле для проверки пустые
            logger.logError(ex)
            // спрашиваем "continue anyway?"
            launchOnMain {
                sendEvent(
                    StorageEvent.AskForEmptyPassCheckingField(
                        fieldName = ex.fieldName,
                        passHash = "",
                        callbackEvent = callbackEvent,
                    )
                )
            }
        }
    }

    fun checkPassAndChange(curPass: String, newPass: String): Boolean {
        try {
            if (passInteractor.checkPass(curPass)) {
                startChangePass(curPass, newPass)
            } else {
                logger.logError(R.string.log_cur_pass_is_incorrect, show = true)
                return false
            }
        } catch (ex: DatabaseConfig.EmptyFieldException) {
            // если поля в INI-файле для проверки пустые
            logger.logError(ex)
            // спрашиваем "continue anyway?"
            launchOnMain {
                sendEvent(
                    StorageEvent.AskForEmptyPassCheckingField(
                        fieldName = ex.fieldName,
                        passHash = "",
                        callbackEvent = StorageEvent.ChangePassDirectly(
                            curPass = curPass,
                            newPass = newPass,
                        ),
                    )
                )
            }
        }
        return true
    }

    fun startChangePass(curPass: String, newPass: String) {
        launchOnMain {
            sendEvent(BaseEvent.TaskStarted(R.string.task_pass_changing))
            isBusy = true
            withIo {
                changePasswordUseCase.run(
                    ChangePasswordUseCase.Params(
                        storage = storage!!,
                        databaseConfig = storageProvider.databaseConfig,
                        curPassword = curPass,
                        newPassword = newPass,
                        taskProgress = taskProgressHandler,
                    )
                )
            }.onFailure {
                logger.logFailure(it)
                isBusy = false
                sendEvent(BaseEvent.TaskFinished)
            }.onSuccess { result ->
                isBusy = false
                sendEvent(BaseEvent.TaskFinished)
                if (result) {
                    sendEvent(StorageEvent.PassChanged)
                    logger.log(R.string.log_pass_changed, true)
                } else {
                    logger.logError(R.string.log_pass_change_error, true)
                    sendEvent(BaseEvent.ShowMoreInLogs)
                }

            }
        }
    }

    //endregion Password

    //region Pin

    /**
     * Проверка использования ПИН-кода с учетом версии приложения.
     * @return
     */
    fun isRequestPINCode(): Boolean {
        return buildInfoProvider.isFullVersion()
                && CommonSettings.isRequestPINCode(getContext())
                && isPinNeedEnter
    }

    /**
     * Запрос ПИН-кода, если установлена опция.
     * К этому моменту факт того, что хэш пароля сохранен локально, должен быть уже проверен.
     * @param specialFlag Дополнительный признак, указывающий на то, нужно ли спрашивать ПИН-код
     * конкретно в данный момент.
     * @param callback Обработчик обратного вызова.
     */
    fun askPinCode(specialFlag: Boolean, callbackEvent: VMEvent) {
        launchOnMain {
            if (isRequestPINCode() && specialFlag) {
                // выводим запрос ввода ПИН-кода
                sendEvent(StorageEvent.AskPinCode(callbackEvent))
            } else {
                sendEventFromCallbackParam(callbackEvent)
            }
        }
    }

    fun startCheckPinCode(pin: String, callbackEvent: VMEvent): Boolean {
        // зашифровываем введеный пароль перед сравнением
        val res = checkPinCode(pin)
        if (res) {
            launchOnMain {
                sendEventFromCallbackParam(callbackEvent)
            }
            // сбрасываем признак
            isPinNeedEnter = false
            logger.log(R.string.log_pin_code_entered, false)
        }
        return res
    }

    fun checkPinCode(pin: String): Boolean {
        // сравниваем хеши
        val pinHash = storageCrypter.passToHash(pin)
        return (pinHash == CommonSettings.getPINCodeHash(getContext()))
    }

    //endregion Pin

    fun onPassLocalHashLocalParamChanged(isSaveLocal: Boolean): Boolean {
        return if (getMiddlePassHash() != null) {
            // если пароль задан, то проверяем ПИН-код
            askPinCode(
                specialFlag = true,
                callbackEvent = StorageEvent.SavePassHashLocalChanged(isSaveLocal)
            )
            false
        } else {
            launchOnMain {
                sendEvent(StorageEvent.SavePassHashLocalChanged(isSaveLocal))
            }
            false
        }
    }

    private val taskProgressHandler = object : ITaskProgress {
        override suspend fun nextStage(obj: LogObj, oper: LogOper, stage: TaskStage.Stages) {
            setStage(obj, oper, stage)
        }

        override suspend fun <L,R> nextStage(obj: LogObj, oper: LogOper, executeStage: suspend () ->  Either<L,R>):  Either<L,R> {
            setStage(obj, oper, TaskStage.Stages.START)
            return executeStage()
                .onFailure {
                    setStage(obj, oper, TaskStage.Stages.FAILED)
                }.onSuccess {
                    setStage(obj, oper, TaskStage.Stages.SUCCESS)
                }
        }

        private fun setStage(obj: LogObj, oper: LogOper, stage: TaskStage.Stages) {
            val taskStage = TaskStage(Constants.TetroidView.Settings, obj, oper, stage)
            launchOnMain {
                sendEvent(
                    BaseEvent.ShowProgressText(
                        message = logger.logTaskStage(taskStage).orEmpty()
                    )
                )
            }
        }
    }

    //endregion Encryption

    // region StorageProperties

    var quicklyNode: TetroidNode?
        get() {
            val nodeId = storage?.quickNodeId
            if (nodeId != null && isStorageLoaded() && !isLoadedFavoritesOnly()) {
                // TODO ?
                return runBlocking { getNode(nodeId) }
            }
            return null
        }
        set(value) {
            updateStorageOption(getString(R.string.pref_key_quickly_node_id), value?.id ?: "")
        }

    open fun updateStorageOption(key: String, value: Any) {}


    fun updateStorageAsync(storage: TetroidStorage) {
        launchOnMain {
            if (!storagesRepo.updateStorage(storage)) {
//                logFailure()
            }
        }
    }

    fun updateStorageAsync() {
        storage?.let {
            updateStorageAsync(it)
        }
    }

    suspend fun updateStorage(storage: TetroidStorage): Boolean {
        return storagesRepo.updateStorage(storage)
    }

    fun getFavoriteRecords(): List<TetroidRecord> {
        return favoritesManager.getFavoriteRecords()
    }

    /**
     * Задана ли ветка для быстрой вставки в дереве.
     */
    fun isQuicklyNodeSet(): Boolean {
        return storage?.quickNodeId != null
    }

    /**
     * Актуализация ветки для быстрой вставки в дереве.
     */
    fun updateQuicklyNode() {
        val nodeId = storage?.quickNodeId
        if (nodeId != null && isStorageLoaded() && !isLoadedFavoritesOnly()) {
            launchOnIo {
                quicklyNode = getNode(nodeId)
            }
        }
    }

    fun dropSavedLocalPassHash() {
        storage?.apply {
            // удаляем хэш пароля и сбрасываем галку
            middlePassHash = null
            isSavePassLocal = false
            updateStorageAsync(this)
        }
    }

    fun clearTrashFolder() {
        launchOnMain {
            if (trashInteractor.clearTrashFolder(storage ?: return@launchOnMain)) {
                log(R.string.title_trash_cleared, true)
            } else {
                logError(R.string.title_trash_clear_error, true)
            }
        }
    }

    //region Migration

    fun isNeedMigration(): Boolean {
        val fromVersion = CommonSettings.getSettingsVersion(getContext())
        return (fromVersion != 0 && fromVersion < Constants.SETTINGS_VERSION_CURRENT)
    }

    //endregion Migration

    //region Getters

    fun getQuicklyNodeName() = quicklyNode?.name.orEmpty()

    fun getQuicklyNodeNameOrMessage(): String? {
        return if (storage?.quickNodeId != null) {
            if (!isStorageLoaded()) {
                getString(R.string.hint_need_load_storage)
            } else if (isLoadedFavoritesOnly()) {
                getString(R.string.hint_need_load_all_nodes)
            } else quicklyNode?.name
        } else null
    }

    fun getQuicklyNodeId() = quicklyNode?.id.orEmpty()

    fun saveMiddlePassHashLocalIfCached() {
        // сохраняем хеш локально, если пароль был введен
        sensitiveDataProvider.getMiddlePassHashOrNull()?.let { passHash ->
            storage?.apply {
                isSavePassLocal = true
                middlePassHash = passHash
                updateStorageAsync(this)
                log(R.string.log_pass_hash_saved_local, show = true)
            }
        }
    }

    //endregion Getters

    // region Setters

    fun setIsDecrypted(value: Boolean) {
        storage?.isDecrypted = value
    }

    fun setLastNodeId(nodeId: String?) {
        storage?.lastNodeId = nodeId
    }

    //endregion Setters

    //region IStorageCallback

    suspend fun saveStorage(): Boolean {
        return withIo {
            saveStorageUseCase.run(/*storageProvider*/)
        }.foldResult(
            onLeft = {
                logFailure(it)
                false
            },
            onRight = { true }
        )
    }

    fun getPathToRecordFolder(record: TetroidRecord): String {
        return recordPathProvider.getPathToRecordFolder(record)
    }

    //endregion IStorageCallback

    //endregion StorageProperties

}
