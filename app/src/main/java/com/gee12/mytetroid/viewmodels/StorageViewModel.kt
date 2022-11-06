package com.gee12.mytetroid.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import com.gee12.htmlwysiwygeditor.Dialogs.*
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.onFailure
import com.gee12.mytetroid.common.onSuccess
import com.gee12.mytetroid.usecase.InitAppUseCase
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.data.ini.DatabaseConfig.EmptyFieldException
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.helpers.NetworkHelper.IWebFileResult
import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.common.utils.UriUtils
import com.gee12.mytetroid.data.crypt.IEncryptHelper
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.TaskStage
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.usecase.crypt.ChangePasswordUseCase
import com.gee12.mytetroid.usecase.crypt.CheckStoragePasswordUseCase
import com.gee12.mytetroid.usecase.storage.InitOrCreateStorageUseCase
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.views.activities.TetroidActivity.IDownloadFileResult
import com.gee12.mytetroid.usecase.storage.ReadStorageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

open class StorageViewModel(
    app: Application,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    commonSettingsProvider: CommonSettingsProvider,
    val appBuildHelper: AppBuildHelper,
    storageProvider: IStorageProvider,
    val favoritesInteractor: FavoritesInteractor,
    val sensitiveDataProvider: ISensitiveDataProvider,
    val passInteractor: PasswordInteractor,
    val storageCrypter: IEncryptHelper,
    val cryptInteractor: EncryptionInteractor,
    val recordsInteractor: RecordsInteractor,
    val nodesInteractor: NodesInteractor,
    val tagsInteractor: TagsInteractor,
    val attachesInteractor: AttachesInteractor,
    val storagesRepo: StoragesRepo,
    val storagePathHelper: IStoragePathHelper,
    val recordPathHelper: IRecordPathHelper,
    val dataInteractor: DataInteractor,
    val interactionInteractor: InteractionInteractor,
    val syncInteractor: SyncInteractor,
    val trashInteractor: TrashInteractor,
    protected val initAppUseCase: InitAppUseCase,
    protected val initOrCreateStorageUseCase: InitOrCreateStorageUseCase,
    protected val readStorageUseCase: ReadStorageUseCase,
    protected val saveStorageUseCase: SaveStorageUseCase,
    protected val checkStoragePasswordUseCase: CheckStoragePasswordUseCase,
    protected val changePasswordUseCase: ChangePasswordUseCase,
) : BaseStorageViewModel(
    app,
    logger,
    notificator,
    failureHandler,
    commonSettingsProvider,
    storageProvider,
) {

    /*protected*/ lateinit var storageInteractor: StorageInteractor

    private val databaseConfig = DatabaseConfig(this.logger)

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
            else -> true
        }
    }

    /**
     * Инициализация хранилища по ID, переданному в Intent.
     */
    fun initStorage(intent: Intent): Boolean {
        val storageId = intent.getIntExtra(Constants.EXTRA_STORAGE_ID, 0)

        return if (storage != null && storage?.id == storageId) {
            launchOnMain {
                sendStorageEvent(Constants.StorageEvents.GetEntity, storage)
                sendStorageEvent(Constants.StorageEvents.Inited, storage)
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
                sendViewEvent(Constants.ViewEvents.FinishActivity)
            }
            false
        }
    }

    fun startReloadStorageEntity() {
        storage?.let { currentStorage ->
            launch(Dispatchers.IO) {
                val id = currentStorage.id
                val storage = storagesRepo.getStorage(id)
                if (storage != null) {
                    storageProvider.setStorage(currentStorage.resetFields(storage))

                    this@StorageViewModel.sendStorageEvent(Constants.StorageEvents.GetEntity, storage)
                } else {
                    this@StorageViewModel.sendStorageEvent(Constants.StorageEvents.NotFoundInBase, id)
                    log(getString(R.string.log_storage_not_found_mask).format(id), true)
                }
            }
        }
    }

    open fun startInitStorageFromBase(id: Int) {
        launchOnIo {
            val storage = storagesRepo.getStorage(id)
            if (storage != null) {
                storageProvider.setStorage(storage)

                withMain {
                    sendStorageEvent(Constants.StorageEvents.GetEntity, storage)
                }

                // если используется уже загруженное дерево веток из кэша
                if (storageProvider.isLoaded()) {
                    storage.isLoaded = true
                }

                // загружаем настройки, но не загружаем само хранилище
                initStorage()
            } else {
                withMain {
                    sendStorageEvent(Constants.StorageEvents.NotFoundInBase, id)
                }
                log(getString(R.string.log_storage_not_found_mask).format(id))
            }
        }
    }

    /**
     * Запуск первичной инициализации хранилища по-умолчанию с указанием флага isCheckFavorMode
     * @param isCheckFavoritesOnlyMode Стоит ли проверять необходимость загрузки только избранных записей.
     *                          Если отключено, то всегда загружать хранилище полностью.
     */
    fun startInitStorage(id: Int, isLoadAllNodesForced: Boolean) {
        this.isLoadAllNodesForced = isLoadAllNodesForced
        startInitStorage(id)
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
                sendStorageEvent(Constants.StorageEvents.NoDefaultStorage)
            }
        }
    }

    /**
     * Поиск хранилища в базе данных и запуск первичной его инициализация.
     */
    fun startInitStorage(id: Int) {
        launch(Dispatchers.IO) {
            val storage = storagesRepo.getStorage(id)
            if (storage != null) {
                this@StorageViewModel.sendStorageEvent(Constants.StorageEvents.GetEntity, storage)
                startInitStorage(storage)
            } else {
                this@StorageViewModel.sendStorageEvent(Constants.StorageEvents.NotFoundInBase, id)
                log(getString(R.string.log_storage_not_found_mask).format(id), true)
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
            this@StorageViewModel.sendViewEvent(Constants.ViewEvents.PermissionCheck)
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
        launchOnMain {
            isAlreadyTryDecrypt = false
            withContext(Dispatchers.IO) {
                initOrCreateStorageUseCase.run(
                    InitOrCreateStorageUseCase.Params(
                        storage = storage!!, // FIXME
                        databaseConfig = databaseConfig,
                    )
                )
            }.onFailure { failure ->
                logFailure(failure)
                //logError(getString(R.string.log_failed_storage_init) + getStoragePath(), false)
                this@StorageViewModel.sendStorageEvent(Constants.StorageEvents.InitFailed, isLoadFavoritesOnly ?: checkIsNeedLoadFavoritesOnly())
            }.onSuccess { result ->
                when (result) {
                    is InitOrCreateStorageUseCase.Result.CreateStorage -> {
                        this@StorageViewModel.sendStorageEvent(Constants.StorageEvents.FilesCreated, storage)
                    }
                    is InitOrCreateStorageUseCase.Result.InitStorage -> {

                    }
                }
                log(getString(R.string.log_storage_inited) + getStoragePath())
                sendStorageEvent(Constants.StorageEvents.Inited, storage)
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
        isAllNodesLoading: Boolean = false
    ) {
        val params = StorageParams(
            storage = storage!!,
            node = null,
            isNodeOpening = false,
            isLoadFavoritesOnly = isLoadFavoritesOnly,
            isHandleReceivedIntent = isHandleReceivedIntent,
            isAllNodesLoading = isAllNodesLoading
        )
        if (isStorageCrypted()) {
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
    private fun checkIsNeedLoadFavoritesOnly(): Boolean {
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
    fun loadOrDecryptStorage(params: StorageParams) {
        // расшифровуем хранилище только в том случаем, если:
        //  1) не используем проверку ПИН-кода
        //  2) используем проверку ПИН-кода, при этом расшифровуем с открытием конкретной <b>зашифрованной</b> ветки
        //   (или ветки Избранное)
        val node = params.node
        var isDecrypt = params.isDecrypt ?: false
        isDecrypt = (isDecrypt
                && (!isRequestPINCode()
                    || isRequestPINCode() && node != null
                        && (node.isCrypted || node == FavoritesInteractor.FAVORITES_NODE)))
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
            val stringResId = if (isDecrypt) R.string.task_storage_decrypting else R.string.task_storage_loading
            sendViewEvent(Constants.ViewEvents.TaskStarted, stringResId)

            val result = withContext(Dispatchers.IO) {
                readStorageUseCase.run(
                    ReadStorageUseCase.Params(
                        storage = storage,
                        isDecrypt = isDecrypt,
                        isFavoritesOnly = isFavoritesOnly,
                        isOpenLastNode = isOpenLastNode,
                    )
                )
            }.foldResult(
                onLeft = { failure ->
                    logFailure(failure, show = true)
                    sendViewEvent(Constants.ViewEvents.TaskFinished)

                    // TODO ?
                    //logWarning(getString(R.string.log_failed_storage_load_mask) + getStoragePath(), true)
                    //postViewEvent(Constants.ViewEvents.ShowMoreInLogs)
                    false
                },
                onRight = {
                    sendViewEvent(Constants.ViewEvents.TaskFinished)

                    val mes = getString(
                        when {
                            isFavoritesOnly -> R.string.log_storage_favor_loaded_mask
                            isDecrypt -> R.string.log_storage_loaded_decrypted_mask
                            else -> R.string.log_storage_loaded_mask
                        }
                    )
                    log(mes.format(getStorageName()), true)

                    // загрузка ветки для быстрой вставки
                    updateQuicklyNode()
                    true
                }
            )

            // инициализация контролов
            sendViewEvent(
                event = Constants.ViewEvents.InitGUI,
                param = StorageParams(
                    storage = storage,
                    isDecrypt = isDecrypt,
                    isLoadFavoritesOnly = isFavoritesOnly,
                    isHandleReceivedIntent = isOpenLastNode,
                    result = result
                )
            )
            // действия после загрузки хранилища
            sendStorageEvent(Constants.StorageEvents.Loaded, result)

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
            sendViewEvent(Constants.ViewEvents.TaskStarted, R.string.task_storage_decrypting)

            // непосредственная расшифровка
            val result = cryptInteractor.decryptStorage(getContext(), false)
            setIsDecrypted(result)

            // после расшифровки
            sendViewEvent(Constants.ViewEvents.TaskFinished)

            if (result) {
                log(R.string.log_storage_decrypted, true)
            } else {
                logDuringOperErrors(LogObj.STORAGE, LogOper.DECRYPT, true)
            }
            // действия после расшифровки хранилища
            afterStorageDecrypted(node)
        }
    }

    open fun afterStorageDecrypted(node: TetroidNode?) {
        launchOnMain {
            this@StorageViewModel.sendStorageEvent(Constants.StorageEvents.Decrypted, node)
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
        val callback = EventCallbackParams(Constants.StorageEvents.LoadOrDecrypt, params)

        // устанавливаем признак
        setIsPINNeedToEnter()

        var middlePassHash: String? = null
        when {
            sensitiveDataProvider.getMiddlePassHashOrNull()?.also { middlePassHash = it } != null -> {
                // хэш пароля уже установлен (вводили до этого и проверяли)
                cryptInteractor.initCryptPass(middlePassHash!!, true)
                // спрашиваем ПИН-код
                askPinCode(params.isNodeOpening, callback)
            }
            isSaveMiddlePassLocal() && storage?.middlePassHash.also { middlePassHash = it } != null -> {
                // хэш пароля сохранен локально, проверяем
                params.passHash = middlePassHash

                try {
                    if (passInteractor.checkMiddlePassHash(middlePassHash)) {
                        // сохраненный хеш пароля подошел, устанавливаем его
                        cryptInteractor.initCryptPass(middlePassHash!!, true)
                        // спрашиваем ПИН-код
                        askPinCode(params.isNodeOpening, callback)
                    } else if (params.isNodeOpening) {
                        // если сохраненный хэш пароля не подошел, и это открытие зашифрованной ветки,
                        //  то сразу спрашиваем пароль
                        askPassword(callback)
                    } else {
                        // в остальных случаях, когда сохраненный хэш пароля не подошел,
                        //  загружаем хранилище без расшифровки
                        log(R.string.log_wrong_saved_pass, true)
                        if (!isAlreadyTryDecrypt) {
                            isAlreadyTryDecrypt = true
                            params.isDecrypt = false
                            loadOrDecryptStorage(params)
                        }
                    }
                } catch (ex: EmptyFieldException) {
                    // если поля в INI-файле для проверки пустые
                    logError(ex)
                    // спрашиваем "continue anyway?"
                    params.fieldName = ex.fieldName
                    launchOnMain {
                        this@StorageViewModel.sendStorageEvent(
                            Constants.StorageEvents.AskForEmptyPassCheckingField,
                            EmptyPassCheckingFieldCallbackParams(ex.fieldName, middlePassHash!!, callback)
                        )
                    }
                }
            }
            CommonSettings.isAskPassOnStart(getContext()) || params.isNodeOpening -> {
                // если пароль не установлен и не сохранен локально, то спрашиваем его, если:
                //  * нужно расшифровывать хранилище сразу на старте
                //  * функция вызвана во время открытия зашифрованной ветки
                //  * ??? если мы не вызвали загрузку всех веток
                askPassword(callback)
            }
            else -> {
                // если пароль не установлен и не сохранен локально, и его не нужно спрашивать, то
                //  просто загружаем хранилище без расшифровки
                params.isDecrypt = false
                loadOrDecryptStorage(params)
            }
        }
    }

    fun getEmptyPassCheckingFieldCallback(
        params: StorageParams,
        callbackEvent: Constants.StorageEvents
    ) = object : IApplyCancelResult {
        val callback = EventCallbackParams(callbackEvent, params)

        override fun onApply() {
            cryptInteractor.initCryptPass(params.passHash ?: "", true)
            // спрашиваем ПИН-код
            askPinCode(params.isNodeOpening, callback)
        }
        override fun onCancel() {
            if (!params.isNodeOpening) {
                // загружаем хранилище без расшифровки
                params.isDecrypt = false
                loadOrDecryptStorage(params)
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
                node = FavoritesInteractor.FAVORITES_NODE,
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
                                this@StorageViewModel.sendStorageEvent(Constants.StorageEvents.AskBeforeSyncOnInit, callback)
                            }
                        } else {
                            startStorageSync(activity, callback)
                        }
                    }
                    // перед выходом из приложения
                    syncType == SyncStorageType.BeforeExit && it.isSyncBeforeExit -> {
                        if (it.isAskBeforeSyncOnExit) {
                            launchOnMain {
                                this@StorageViewModel.sendStorageEvent(Constants.StorageEvents.AskBeforeSyncOnExit, callback)
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
                context = getContext(),
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
                        this@StorageViewModel.sendStorageEvent(Constants.StorageEvents.AskAfterSyncOnInit, result)
                    }
                }
            }
            SyncStorageType.BeforeExit -> {
                if (result) {
                    syncCallback?.run(result)
                } else {
                    launchOnMain {
                        this@StorageViewModel.sendStorageEvent(Constants.StorageEvents.AskAfterSyncOnExit, result)
                    }
                }
            }
        }
        syncType = SyncStorageType.Manually
        syncCallback = null
    }

    //endregion Sync

    //region Records

    fun getRecord(id: String?): TetroidRecord? = recordsInteractor.getRecord(id)

    open fun editRecordFields(record: TetroidRecord, name: String, tags: String, author: String, url: String, node: TetroidNode, isFavor: Boolean) {
        launchOnMain {
            recordsInteractor.editRecordFields(getContext(), record, name, tags, author, url, node, isFavor)
        }
    }

    suspend fun cutRecord(record: TetroidRecord, withoutDir: Boolean): Int {
        return recordsInteractor.cutRecord(getContext(), record, withoutDir)
    }

    suspend fun deleteRecord(record: TetroidRecord, withoutDir: Boolean): Int {
        return recordsInteractor.deleteRecord(getContext(), record, withoutDir)
    }

    //endregion Records

    //region Nodes

    fun getNode(nodeId: String) = nodesInteractor.getNode(nodeId)

    fun createNodesHierarchy(node: TetroidNode) = nodesInteractor.createNodesHierarchy(node)

    /**
     * Проверка существования зашифрованных веток.
     */
    protected fun checkExistenceCryptedNodes() {
        launchOnMain {
            if (!nodesInteractor.isExistCryptedNodes(true)) {
                sendStorageEvent(Constants.StorageEvents.AskForClearStoragePass)
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
    open suspend fun downloadFileToCache(url: String?, callback: IDownloadFileResult?) {
        if (TextUtils.isEmpty(url)) {
            logError(R.string.log_link_is_empty)
            return
        }
        this.sendViewEvent(Constants.ViewEvents.ShowProgressText, getString(R.string.title_file_downloading))
        var fileName = UriUtils.getFileName(url)
        if (TextUtils.isEmpty(fileName)) {
//            Exception ex = new Exception("");
//            if (callback != null) {
//                callback.onError(ex);
//            }
//            TetroidLog.log(TetroidActivity.this, getString(R.string.log_error_download_file_mask, ex.getMessage()), Toast.LENGTH_LONG);
//            return;
            fileName = dataInteractor.createDateTimePrefix()
        }
        val outputFileName: String = getExternalCacheDir() + "/" + fileName
        NetworkHelper.downloadFileAsync(url, outputFileName, object : IWebFileResult {
            override fun onSuccess() {
                callback?.onSuccess(Uri.fromFile(File(outputFileName)))
                launchOnMain {
                    this@StorageViewModel.sendViewEvent(Constants.ViewEvents.ShowProgress, false)
                }
            }

            override fun onError(ex: Exception) {
                callback?.onError(ex)
                logError(getString(R.string.log_error_download_file_mask, ex.message ?: ""))
                launchOnMain {
                    this@StorageViewModel.sendViewEvent(Constants.ViewEvents.ShowProgress, false)
                }
            }
        })
    }

    //endregion Attaches

    //region Other

    suspend fun swapTetroidObjects(list: List<Any>, pos: Int, isUp: Boolean, through: Boolean): Int {
        return if (dataInteractor.swapTetroidObjects(list, pos, isUp, through)) {
            if (saveStorage()) 1 else -1
        } else 0
    }

    fun getExternalCacheDir() = getContext().externalCacheDir.toString()

    fun checkStorageFilesExistingError() = storagePathHelper.checkStorageFilesExistingError(getContext())

    //endregion Other

    //region Encryption


    fun isStorageCrypted(): Boolean {
        var iniFlag = false
        try {
            iniFlag = databaseConfig.isCryptMode
        } catch (ex: Exception) {
            logError(ex, true)
        }
        /*return (iniFlag == 1 && instance.mIsExistCryptedNodes) ? true
                : (iniFlag != 1 && !instance.mIsExistCryptedNodes) ? false
                : (iniFlag == 1 && !instance.mIsExistCryptedNodes) ? true
                : (iniFlag == 0 && instance.mIsExistCryptedNodes) ? true : false;*/
        return iniFlag || nodesInteractor.isExistCryptedNodes(false)
    }

    //region Password

    /**
     * Асинхронная проверка - имеется ли сохраненный пароль, и его запрос при необходимости.
     * Используется:
     *      * когда хранилище уже загружено (зашифровка/сброс шифровки ветки)
     *      * либо когда загрузка хранилища не требуется (установка/сброс ПИН-кода)
     * @param callback
     */
    fun checkStoragePass(callback: EventCallbackParams) {
        launchOnMain {
            withContext(Dispatchers.IO) {
                checkStoragePasswordUseCase.run(
                    CheckStoragePasswordUseCase.Params(
                        storage = storage,
                        isStorageCrypted = isStorageCrypted(),
                        callback = callback,
                    )
                )
            }.onFailure { failure ->
                logFailure(failure)
            }.onSuccess { result ->
                when (result) {
                    is CheckStoragePasswordUseCase.Result.AskPassword -> {
                        askPassword(callback)
                    }
                    is CheckStoragePasswordUseCase.Result.AskPin -> {
                        askPinCode(true, callback)
                    }
                    is CheckStoragePasswordUseCase.Result.AskForEmptyPassCheckingField -> {
                        this@StorageViewModel.sendStorageEvent(
                            event = Constants.StorageEvents.AskForEmptyPassCheckingField,
                            param = result.params
                        )
                    }
                }
            }
        }
    }

    fun confirmEmptyPassCheckingFieldDialog(callback: EmptyPassCheckingFieldCallbackParams) {
        cryptInteractor.initCryptPass(callback.passHash, true)
        askPinCode(true, callback)
    }

    /**
     * Отображения запроса пароля от хранилища.
     * @param callback
     */
    fun askPassword(callback: EventCallbackParams) {
        logger.log(R.string.log_show_pass_dialog, false)
        // выводим окно с запросом пароля в асинхронном режиме
        launchOnMain {
            this@StorageViewModel.sendStorageEvent(Constants.StorageEvents.AskPassword, callback)
        }
    }

    fun onPasswordEntered(pass: String, isSetup: Boolean, callback: EventCallbackParams) {
        if (isSetup) {
            launchOnMain {
                setupPass(pass)
                postEventFromCallbackParam(callback)
            }
        } else {
            checkPass(pass, { res: Boolean ->
                launchOnMain {
                    if (res) {
                        initPass(pass)
                        postEventFromCallbackParam(callback)
                    } else {
                        // повторяем запрос
                        askPassword(callback)
                    }
                }
            }, R.string.log_pass_is_incorrect)
        }
    }

    fun onPasswordCanceled(isSetup: Boolean, callback: EventCallbackParams) {
        if (!isSetup) {
            isAlreadyTryDecrypt = true
            //super.onPasswordCanceled(isSetup, callback)
            (callback.data as? StorageParams)?.let {
                if (!it.isNodeOpening) {
                    //isAlreadyTryDecrypt = true
                    it.isDecrypt = false
                    launchOnMain {
                        postEventFromCallbackParam(callback)
                    }
                }
            }
        }
    }

    fun startSetupPass(pass: String) {
        launchOnMain {
            setupPass(pass)
        }
    }

    // TODO: SetupPasswordUseCase
    suspend fun setupPass(pass: String) {
        log(R.string.log_start_pass_setup)
        sendViewEvent(Constants.ViewEvents.TaskStarted, getString(R.string.task_pass_setting))
        isBusy = true
        val result = withContext(Dispatchers.IO) {
            passInteractor.setupPass(storage!!, pass).also {
                // сохраняем хэш пароля в бд (если установлена соответствующая опция)
                updateStorageAsync(storage!!)
            }
        }
        isBusy = false
        sendViewEvent(Constants.ViewEvents.TaskFinished)
        if (result) {
            sendStorageEvent(Constants.StorageEvents.PassSetuped)
        }
    }

    suspend fun initPass(pass: String) {
        passInteractor.initPass(storage!!, pass)
        updateStorageAsync(storage!!)
    }

    /**
     * Каркас проверки введенного пароля.
     * @param context
     * @param pass
     * @param callback
     * @param wrongPassRes
     */
    // TODO: CheckPasswordUseCase
    fun checkPass(pass: String, callback: ICallback, wrongPassRes: Int): Boolean {
        try {
            if (passInteractor.checkPass(pass)) {
                callback.run(true)
            } else {
                logger.logError(wrongPassRes, true)
                callback.run(false)
                return false
            }
        } catch (ex: DatabaseConfig.EmptyFieldException) {
            // если поля в INI-файле для проверки пустые
            logger.logError(ex)
            // спрашиваем "continue anyway?"
            launchOnMain {
                this@StorageViewModel.sendStorageEvent(
                    Constants.StorageEvents.AskForEmptyPassCheckingField,
                    // TODO: тут спрашиваем нормально ли расшифровались данные
                    callback
                )
            }
        }
        return true
    }

    fun startChangePass(curPass: String, newPass: String) {
        launchOnMain {
            sendViewEvent(Constants.ViewEvents.TaskStarted, getString(R.string.task_pass_changing))
            isBusy = true
            withContext(Dispatchers.IO) {
                changePasswordUseCase.run(
                    ChangePasswordUseCase.Params(
                        storage = storage!!,
                        curPass = curPass,
                        newPass = newPass,
                        taskProgress = taskProgressHandler,
                    )
                )
            }.onFailure {
                logger.logFailure(it)
                isBusy = false
                sendViewEvent(Constants.ViewEvents.TaskFinished)
            }.onSuccess { result ->
                isBusy = false
                sendViewEvent(Constants.ViewEvents.TaskFinished)
                if (result) {
                    sendStorageEvent(Constants.StorageEvents.PassChanged)
                    logger.log(R.string.log_pass_changed, true)
                } else {
                    logger.logError(R.string.log_pass_change_error, true)
                    sendViewEvent(Constants.ViewEvents.ShowMoreInLogs)
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
        return appBuildHelper.isFullVersion()
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
    fun askPinCode(specialFlag: Boolean, callback: EventCallbackParams) {
        launchOnMain {
            if (isRequestPINCode() && specialFlag) {
                // выводим запрос ввода ПИН-кода
                this@StorageViewModel.sendStorageEvent(Constants.StorageEvents.AskPinCode, callback)
            } else {
                postEventFromCallbackParam(callback)
            }
        }
    }

    fun startCheckPinCode(pin: String, callback: EventCallbackParams): Boolean {
        // зашифровываем введеный пароль перед сравнением
        val res = checkPinCode(pin)
        if (res) {
            launchOnMain {
                postEventFromCallbackParam(callback)
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


    fun onPassLocalHashLocalParamChanged(newValue: Any): Boolean {
        return if (getMiddlePassHash() != null) {
            askPinCode(
                specialFlag = true,
                callback = EventCallbackParams(Constants.StorageEvents.SavePassHashLocalChanged, newValue)
            )
            false
        } else {
            // если пароль не задан, то нечего очищать, не задаем вопрос
            true
        }
    }

    private val taskProgressHandler = object : ITaskProgress {
        override suspend fun nextStage(obj: LogObj, oper: LogOper, stage: TaskStage.Stages) {
            setStage(obj, oper, stage)
        }

        override suspend fun nextStage(obj: LogObj, oper: LogOper, stageExecutor: suspend () -> Boolean): Boolean {
            setStage(obj, oper, TaskStage.Stages.START)
            return if (stageExecutor.invoke()) {
                setStage(obj, oper, TaskStage.Stages.SUCCESS)
                true
            } else {
                setStage(obj, oper, TaskStage.Stages.FAILED)
                false
            }
        }

        private fun setStage(obj: LogObj, oper: LogOper, stage: TaskStage.Stages) {
            val taskStage = TaskStage(Constants.TetroidView.Settings, obj, oper, stage)
            val mes = logger.logTaskStage(taskStage)
            launchOnMain {
                this@StorageViewModel.sendViewEvent(Constants.ViewEvents.ShowProgressText, mes)
            }
        }
    }

    //endregion Encryption

    // region StorageProperties

    var quicklyNode: TetroidNode?
        get() {
            val nodeId = storage?.quickNodeId
            if (nodeId != null && isStorageLoaded() && !isLoadedFavoritesOnly()) {
                return nodesInteractor.getNode(nodeId)
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
                //...
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
        return favoritesInteractor.getFavoriteRecords()
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
            this.quicklyNode = nodesInteractor.getNode(nodeId)
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

    fun isStorageInited() = storage?.isInited ?: false

    fun isStorageLoaded() = /*(storage?.isLoaded ?: false) &&*/ storageProvider.isLoaded()

    fun isStorageDecrypted() = storage?.isDecrypted ?: false

    fun isStorageNonEncryptedOrDecrypted() = !isStorageCrypted() || isStorageDecrypted()

    fun getStorageId() = storage?.id ?: 0

    fun getStoragePath() = storage?.path.orEmpty()

    fun getStorageName() = storage?.name ?: ""

    fun isStorageDefault() = storage?.isDefault ?: false

    fun isStorageReadOnly() = storage?.isReadOnly ?: false

    fun getTrashPath() = storage?.trashPath.orEmpty()

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

    fun getStorageSyncProfile() = storage?.syncProfile

    fun isStorageSyncEnabled() = storage?.syncProfile?.isEnabled ?: false

    fun getStorageSyncAppName() = storage?.syncProfile?.appName.orEmpty()

    fun getStorageSyncCommand() = storage?.syncProfile?.command.orEmpty()

    fun isLoadFavoritesOnly() = /*storageProvider.isLoadedFavoritesOnly()*/ storage?.isLoadFavoritesOnly ?: false

    fun isKeepLastNode() = storage?.isKeepLastNode ?: false

    fun getLastNodeId() = storage?.lastNodeId

    fun isSaveMiddlePassLocal() = storage?.isSavePassLocal ?: false

    fun isDecryptAttachesToTemp() = storage?.isDecyptToTemp ?: false

    fun getMiddlePassHash() = storage?.middlePassHash

    fun isCheckOutsideChanging() = storage?.syncProfile?.isCheckOutsideChanging ?: false

    fun isNodesExist() = storageProvider.getRootNodes().isNotEmpty()

    fun isLoadedFavoritesOnly() = storageProvider.isLoadedFavoritesOnly()

    fun getRootNodes(): List<TetroidNode> = storageProvider.getRootNodes()

    fun getTagsMap(): Map<String,TetroidTag> = storageProvider.getTagsMap()

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
        return withContext(Dispatchers.IO) {
            saveStorageUseCase.run()
        }.foldResult(
            onLeft = {
                logFailure(it)
                false
            },
            onRight = { it }
        )
    }

    fun getPathToRecordFolder(record: TetroidRecord): String {
        return recordPathHelper.getPathToRecordFolder(record)
    }

    //endregion IStorageCallback

    //endregion StorageProperties

}

data class StorageParams(
    val storage: TetroidStorage,
    var result: Boolean = false, // результат открытия/расшифровки
    var isDecrypt: Boolean? = null, // расшифровка хранилища, а не просто открытие
    val node: TetroidNode? = null, // ветка, которую нужно открыть после расшифровки хранилища
    val isNodeOpening: Boolean = false, // если true, значит хранилище уже было загружено, и нажали на еще не расшифрованную ветку
    var isLoadFavoritesOnly: Boolean, // нужно ли загружать только избранные записи,
                                  //  или загружены только избранные записи, т.е. в избранном нажали на не расшифрованную запись
    val isHandleReceivedIntent: Boolean, // ужно ли после загрузки открыть ветку, сохраненную в опции getLastNodeId() 
                                 //  или ветку с избранным (если именно она передана в node)
    var passHash: String? = null, // хеш пароля
    var fieldName: String? = null, // поле в database.ini
    var isAllNodesLoading: Boolean = false, // загрузка всех веток после режима isLoadedFavoritesOnly
)

open class ObjectsInView<T : TetroidObject>(
    val objects: List<T>,
    val viewId: Int? = null,
    val dropSearch: Boolean = true
)

class FilteredObjectsInView<T : TetroidObject>(
    val query: String,
    objects: List<T>,
    viewId: Int
) : ObjectsInView<T>(objects, viewId)

enum class SyncStorageType {
    Manually,
    BeforeInit,
    BeforeExit
}

open class EventCallbackParams(
    val event: Any,
    val data: Any?
)

class EmptyPassCheckingFieldCallbackParams(
    val fieldName: String,
    val passHash: String,
    callback: EventCallbackParams
) : EventCallbackParams(callback.event, callback.data)
