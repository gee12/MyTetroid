package com.gee12.mytetroid.ui.storage

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.toRawFile
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getExtensionWithoutComma
import com.gee12.mytetroid.common.extensions.parseUri
import com.gee12.mytetroid.common.extensions.withExtension
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.domain.interactor.*
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.manager.*
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.TaskStage
import com.gee12.mytetroid.domain.repo.StoragesRepo
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.BaseStorageViewModel
import com.gee12.mytetroid.domain.usecase.crypt.*
import com.gee12.mytetroid.domain.usecase.file.GetFileModifiedDateInStorageUseCase
import com.gee12.mytetroid.domain.usecase.file.GetFolderSizeInStorageUseCase
import com.gee12.mytetroid.domain.usecase.node.GetNodeByIdUseCase
import com.gee12.mytetroid.domain.usecase.record.GetRecordByIdUseCase
import com.gee12.mytetroid.domain.usecase.storage.*
import com.gee12.mytetroid.model.QuicklyNode
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.model.permission.TetroidPermission
import kotlinx.coroutines.runBlocking
import java.util.*

open class StorageViewModel(
    app: Application,
    buildInfoProvider: BuildInfoProvider,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,

    settingsManager: CommonSettingsManager,
    appPathProvider: IAppPathProvider,
    storageProvider: IStorageProvider,
    storagePathProvider: IStoragePathProvider,
    val sensitiveDataProvider: ISensitiveDataProvider,
    val storagesRepo: StoragesRepo,
    val recordPathProvider: IRecordPathProvider,
    val dataNameProvider: IDataNameProvider,

    val cryptManager: IStorageCryptManager,

    val interactionManager: InteractionManager,
    val syncInteractor: SyncInteractor,
    val favoritesManager: FavoritesManager,

    protected val getFileModifiedDateUseCase: GetFileModifiedDateInStorageUseCase,
    protected val getFolderSizeUseCase: GetFolderSizeInStorageUseCase,

    protected val initOrCreateStorageUseCase: InitOrCreateStorageUseCase,
    protected val readStorageUseCase: ReadStorageUseCase,
    protected val saveStorageUseCase: SaveStorageUseCase,
    protected val decryptStorageUseCase: DecryptStorageUseCase,
    protected val checkStorageFilesExistingUseCase: CheckStorageFilesExistingUseCase,
    protected val clearStorageTrashFolderUseCase: ClearStorageTrashFolderUseCase,
    protected val checkPasswordOrPinAndDecryptUseCase: CheckPasswordOrPinAndDecryptUseCase,
    protected val checkPasswordOrPinUseCase: CheckPasswordOrPinAndAskUseCase,
    protected val changePasswordUseCase: ChangePasswordUseCase,
    protected val setupPasswordUseCase: SetupPasswordUseCase,

    protected val getNodeByIdUseCase: GetNodeByIdUseCase,
    protected val getRecordByIdUseCase: GetRecordByIdUseCase,
) : BaseStorageViewModel(
    app = app,
    buildInfoProvider = buildInfoProvider,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,
    settingsManager = settingsManager,
    appPathProvider = appPathProvider,
    storageProvider = storageProvider,
    storagePathProvider = storagePathProvider,
) {

    private var isPinNeedEnter = false

    var isLoadAllNodesForced = false
    var isAlreadyTryDecrypt = false
    var syncType = SyncStorageType.Manually
    var syncCallback: ICallback? = null


    // region Init

    protected fun clearStorageDataFromMemory() {
        // удаляем сохраненный хэш пароля из памяти
        sensitiveDataProvider.resetMiddlePasswordHash()
        // сбрасываем данные шифровальщика
        cryptManager.reset()
        // удаляем загруженные данные хранилища из памяти
        storageProvider.reset()
    }

    fun checkStorageIsReady(checkIsFavorMode: Boolean, showMessage: Boolean): Boolean {
        return when {
            !isStorageInited() -> {
                if (showMessage) {
                    showMessage(
                        if (permissionManager.hasWriteExtStoragePermission(getContext())) R.string.mes_storage_must_be_inited
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
    fun checkPermissionsAndInitStorage(storageId: Int, isLoadAllNodesForced: Boolean) {
        this.isLoadAllNodesForced = isLoadAllNodesForced
        checkPermissionsAndInitStorageById(storageId)
    }

    /**
     * Поиск хранилища по-умолчанию в базе данных и запуск первичной его инициализации.
     */
    fun checkPermissionsAndInitDefaultStorage() {
        log(R.string.log_start_load_def_storage)
        launchOnMain {
            val storageId = storagesRepo.getDefaultStorageId()
            if (storageId > 0) {
                checkPermissionsAndInitStorageById(storageId)
            } else {
                log(R.string.log_def_storage_not_specified)
                sendEvent(StorageEvent.NoDefaultStorage)
            }
        }
    }

    /**
     * Поиск хранилища в базе данных и запуск первичной его инициализация.
     */
    open fun checkPermissionsAndInitStorageById(storageId: Int) {
        launchOnIo {
            val storage = storagesRepo.getStorage(storageId)
            if (storage != null) {
                sendEvent(StorageEvent.FoundInBase(storage))
                checkPermissionsAndInitStorage(storage)
            } else {
                sendEvent(StorageEvent.NotFoundInBase(storageId))
                log(getString(R.string.log_storage_not_found_mask, storageId), true)
            }
        }
    }

    /**
     * Запуск первичной инициализации хранилища.
     * Начинается с проверки и предоставления разрешения на доступ к хранилищу устройства.
     */
    suspend fun checkPermissionsAndInitStorage(storage: TetroidStorage) {
        // сразу сбрасываем все данные о предыдущем хранилище, сохраненные в памяти (если было загружено)
        clearStorageDataFromMemory()

        storageProvider.setStorage(storage)

        CommonSettings.setLastStorageId(getContext(), storage.id)

        val storageFolderUri = storage.uri.parseUri()

        val optimizedStorageFolderUri = DocumentFileCompat.fromUri(getContext(), storageFolderUri)?.let { file ->
            fileStorageManager.checkFolder(file)?.uri?.also {
                storage.uri = it.toString()
                updateStorageInDb(storage)
            }
        } ?: storageFolderUri

        checkAndRequestFileStoragePermission(
            storage = storage,
            uri = optimizedStorageFolderUri,
            requestCode = PermissionRequestCode.OPEN_STORAGE_FOLDER,
        )
    }

    fun checkAndRequestFileStoragePermission(
        storage: TetroidStorage,
        uri: Uri,
        requestCode: PermissionRequestCode,
    ) {
        if (storage.isReadOnly) {
            checkAndRequestReadFileStoragePermission(uri, requestCode)
        } else {
            checkAndRequestWriteFileStoragePermission(uri, requestCode)
        }
    }

    fun startInitStorageAfterPermissionsGranted(
        activity: Activity,
        uri: Uri,
    ) {
        launchOnIo {
            storage?.also { storage ->
                val storageUri = storage.uri
                val selectedFile = DocumentFileCompat.fromUri(getContext(), uri)
                val selectedFileRawPath = selectedFile?.toRawFile(getContext())?.path
                val (storageRootUri, storageRoot) = if (selectedFileRawPath == storageUri) {
                    uri to selectedFile
                } else {
                    val parsedUri = storageUri.parseUri()
                    parsedUri to DocumentFileCompat.fromUri(getContext(), parsedUri)
                }
                storageRoot?.let {

                    // перепроверяем доступ к файловому хранилищу
                    if (fileStorageManager.checkWriteFileStoragePermission(storageRoot)) {

                        // если в storage.uri у нас был старый path
                        //  и мы выбрали этот же каталог (т.е. выбранный uri.toRawFile.path совпадает с storage.uri),
                        //  значит берем выбранный uri и перезаписываем вместо старого storage.uri
                        if (storageRootUri.toString() != storageUri) {
                            storage.uri = storageRootUri.toString()
                            updateStorageInDb(storage)
                        }
                        // устанавливаем полученный DocumentFile
                        storageProvider.setRootFolder(storageRoot)

                        syncAndInitStorage(activity)
                    } else {
                        showManualPermissionRequest(
                            permission = TetroidPermission.FileStorage.Write(storageRootUri),
                            requestCode = PermissionRequestCode.OPEN_STORAGE_FOLDER,
                        )
                    }
                } ?: showManualPermissionRequest(
                        permission = TetroidPermission.FileStorage.Write(storageRootUri),
                        requestCode = PermissionRequestCode.OPEN_STORAGE_FOLDER,
                    )
            }
        }
    }

    /**
     * Запуск перезагрузки хранилища.
     */
    fun startReinitStorage() {
        // TODO: может здесь нужно сразу loadStorage() ?
        storage?.id?.let {
            checkPermissionsAndInitStorageById(it)
        }
    }

    /**
     * Инициализация хранилища (с созданием файлов, если оно новое).
     */
    fun initStorage(isLoadFavoritesOnly: Boolean? = null, isLoadAfter: Boolean = false) {
        storage?.also {
            initStorage(
                storage = it,
                isLoadFavoritesOnly = isLoadFavoritesOnly,
                isLoadAfter = isLoadAfter,
            )
        }
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
                sendEvent(
                    StorageEvent.InitFailed(
                        isOnlyFavorites = isLoadFavoritesOnly ?: checkIsNeedLoadFavoritesOnly()
                    )
                )
            }.onSuccess { result ->
                when (result) {
                    is InitOrCreateStorageUseCase.Result.Created -> {
                        logger.log(getString(R.string.log_storage_created_mask, storage.name), show = true)
                        sendEvent(StorageEvent.FilesCreated(storage))
                    }
                    is InitOrCreateStorageUseCase.Result.Inited -> {

                    }
                }
                log(getString(R.string.log_storage_config_loaded_mask, getStorageFolderPath().fullPath))
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
            node = null,
            isNodeOpening = false,
            isLoadFavoritesOnly = isLoadFavoritesOnly,
            isHandleReceivedIntent = isHandleReceivedIntent,
            isAllNodesLoading = isAllNodesLoading
        )
        if (isStorageEncrypted() && isNeedDecrypt) {
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

    // endregion Init

    // region Load

    /**
     * Непосредственный запуск чтения и/или расшифровки (если зашифровано) данных хранилища.
     */
    open fun loadOrDecryptStorage(params: StorageParams) {
        // расшифровуем хранилище только в том случаем, если:
        //  1) не используем проверку ПИН-кода
        //  2) используем проверку ПИН-кода, при этом расшифровуем с открытием конкретной <b>зашифрованной</b> ветки
        //   (или ветки Избранное)
        val node = params.node
        var isDecrypt = params.isDecrypt ?: false
        isDecrypt = (isDecrypt
                && (!isRequestPinCode()
                    || isRequestPinCode() && node != null
                        && (node.isCrypted || node == FavoritesManager.FAVORITES_NODE)))
        if (isStorageLoaded() && isDecrypt && isNodesExist()) {
            // расшифровываем уже загруженное хранилище
            startDecryptStorage(node)
        } else {
            // загружаем хранилище впервые, с расшифровкой (если нужно)
            startReadStorage(
                isDecrypt = isDecrypt,
                isFavoritesOnly = params.isLoadFavoritesOnly,
                isOpenLastNode = params.isHandleReceivedIntent
            )
        }
    }

    // endregion Load

    // region Read

    /**
     * Непосредственная загрузка структуры хранилища.
     * @param isDecrypt Необходимость расшифровки зашифрованных веток.
     */
    private fun startReadStorage(
        isDecrypt: Boolean,
        isFavoritesOnly: Boolean,
        isOpenLastNode: Boolean
    ) {
        logOperStart(LogObj.STORAGE, LogOper.LOAD)

        launchOnMain {
            sendEvent(StorageEvent.StartLoadingOrDecrypting)
            sendEvent(BaseEvent.TaskStarted(
                if (isDecrypt) R.string.task_storage_decrypting else R.string.task_storage_loading
            ))

            val result = withIo {
                readStorageUseCase.run(
                    ReadStorageUseCase.Params(
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

                    val isDefault = isDefaultStorage()
                    val mes = getString(
                        when {
                            isFavoritesOnly -> R.string.log_storage_favor_loaded_mask
                            isDecrypt -> if (isDefault) {
                                R.string.log_default_storage_loaded_decrypted_mask
                            } else {
                                R.string.log_storage_loaded_decrypted_mask
                            }
                            else -> if (isDefault) {
                                R.string.log_default_storage_loaded_mask
                            } else {
                                R.string.log_storage_loaded_mask
                            }
                        },
                        getStorageName()
                    )
                    log(mes, show = true)

                    // загрузка ветки для быстрой вставки
                    updateQuicklyNode()
                    true
                }
            )

            // действия после загрузки хранилища
            sendEvent(StorageEvent.Loaded(
                isLoaded = result,
                isLoadedFavoritesOnly = isFavoritesOnly,
                isOpenLastNode = isOpenLastNode,
            ))

        }
    }

    // endregion Read

    // region Decrypt

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
                checkPasswordOrPinAndDecryptUseCase.run(
                    CheckPasswordOrPinAndDecryptUseCase.Params(
                        params = params,
                        isAlreadyTryDecrypt = isAlreadyTryDecrypt,
                    )
                )
            }.onFailure {
                logFailure(it)
            }.onSuccess { result ->
                when (result) {
                    is CheckPasswordOrPinAndDecryptUseCase.Result.AskPassword -> {
                        askPassword(callbackEvent)
                    }
                    is CheckPasswordOrPinAndDecryptUseCase.Result.AskPin -> {
                        askPinCode(params.isNodeOpening, callbackEvent)
                    }
                    is CheckPasswordOrPinAndDecryptUseCase.Result.LoadWithoutDecrypt -> {
                        isAlreadyTryDecrypt = true
                        loadOrDecryptStorage(params)
                    }
                    is CheckPasswordOrPinAndDecryptUseCase.Result.AskForEmptyPassCheckingField -> {
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

    // endregion Decrypt

    // region Sync

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
            if (!checkAndRequestTermuxPermission(activity)) {
                return
            }
        }
        val result = syncInteractor.startStorageSync(
            activity = activity,
            storagePath = getStorageFolderPath().fullPath,
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

    // endregion Sync

    // region Records

    // TODO: вынести в конкретную VM
    suspend fun getEditedDate(record: TetroidRecord): Date? {
        return withIo {
            getFileModifiedDateUseCase.run(
                GetFileModifiedDateInStorageUseCase.Params(
                    fileRelativePath = recordPathProvider.getRelativePathToFileInRecordFolder(record, record.fileName),
                )
            ).foldResult(
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
                    GetFolderSizeInStorageUseCase.Params(
                        folderRelativePath = recordPathProvider.getRelativePathToRecordFolder(record),
                    )
                )
            }.foldResult(
                onLeft = { null },
                onRight = { it }
            )
        }
    }

    // endregion Records

    // region Nodes

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

    // endregion Nodes

    // region Attaches

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
        val ext = attach.name.getExtensionWithoutComma()
        return recordPathProvider.getPathToFileInRecordFolder(record, attach.id.withExtension(ext)).fullPath
    }

    fun getAttachEditedDate(context: Context, attach: TetroidFile): Date? {
        return try {
            FileUtils.getFileModifiedDate(context, getAttachFullName(attach))
        } catch (ex: Exception) {
            logger.logError(resourcesProvider.getString(R.string.error_get_mytetra_xml_modified_date_mask, ex.localizedMessage.orEmpty()), false)
            null
        }
    }

    fun getAttachFileSize(context: Context, attach: TetroidFile): String? {
        return try {
            FileUtils.getFileSize(context, getAttachFullName(attach))
        } catch (ex: Exception) {
            logger.logError(resourcesProvider.getString(R.string.error_get_attach_file_size_mask, ex.localizedMessage.orEmpty()), false)
            null
        }
    }

    // endregion Attaches

    // region Other

    fun checkStorageFilesExistingError(): String? {
        return checkStorageFilesExistingUseCase.execute(
            CheckStorageFilesExistingUseCase.Params(storage!!)
        ).foldResult(
            onLeft = {
                failureHandler.getFailureMessage(it).getFullMassage()
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

    // endregion Other

    // region Encryption

    override fun isStorageEncrypted(): Boolean {
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

    // region Password

    /**
     * Отображения запроса пароля от хранилища.
     */
    fun askPassword(callbackEvent: BaseEvent) {
        logger.log(R.string.log_show_pass_dialog, false)
        // выводим окно с запросом пароля в асинхронном режиме
        launchOnMain {
            sendEvent(StorageEvent.AskPassword(callbackEvent))
        }
    }

    suspend fun setupPassword(password: String, callbackEvent: BaseEvent? = null) {
        log(R.string.log_start_pass_setup)
        sendEvent(BaseEvent.TaskStarted(R.string.task_pass_setting))
        isBusy = true

        withIo {
            setupPasswordUseCase.run(
                SetupPasswordUseCase.Params(
                    password = password,
                )
            )
        }.onComplete {
            isBusy = false
            sendEvent(BaseEvent.TaskFinished)
        }.onFailure {
            logFailure(it)
        }.onSuccess {
            logger.log(R.string.log_pass_setted, true)
            sendEvent(StorageEvent.PassSetuped)

            callbackEvent?.also {
                sendEvent(it)
            }
        }
    }

    fun startChangePassword(curPass: String, newPass: String) {
        launchOnMain {
            sendEvent(BaseEvent.TaskStarted(R.string.task_pass_changing))
            isBusy = true
            withIo {
                changePasswordUseCase.run(
                    ChangePasswordUseCase.Params(
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

    // endregion Password

    // region Pin

    /**
     * Проверка использования ПИН-кода с учетом версии приложения.
     * @return
     */
    fun isRequestPinCode(): Boolean {
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
    fun askPinCode(specialFlag: Boolean, callbackEvent: BaseEvent) {
        launchOnMain {
            if (isRequestPinCode() && specialFlag) {
                // выводим запрос ввода ПИН-кода
                sendEvent(StorageEvent.AskPinCode(callbackEvent))
            } else {
                sendEvent(callbackEvent)
            }
        }
    }

    fun startCheckPinCode(pin: String, callbackEvent: BaseEvent): Boolean {
        // зашифровываем введеный пароль перед сравнением
        val res = checkPinCode(pin)
        if (res) {
            launchOnMain {
                sendEvent(callbackEvent)
            }
            // сбрасываем признак
            isPinNeedEnter = false
            logger.log(R.string.log_pin_code_entered, false)
        }
        return res
    }

    fun checkPinCode(pin: String): Boolean {
        // сравниваем хеши
        val pinHash = cryptManager.passToHash(pin)
        return (pinHash == CommonSettings.getPINCodeHash(getContext()))
    }

    // endregion Pin

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
                showProgressWithText(message = logger.logTaskStage(taskStage).orEmpty())
            }
        }
    }

    // endregion Encryption

    // region StorageProperties

    open fun updateStorageOption(key: String, value: Any) {}

    suspend fun updateStorageInDb(storage: TetroidStorage): Boolean {
        return storagesRepo.updateStorage(storage)
    }

    // region QuicklyNode

    fun getQuicklyNode(): QuicklyNode {
        val nodeId = storage?.quickNodeId
        return when {
            nodeId == null -> QuicklyNode.IsNotSet
            !isStorageLoaded() -> QuicklyNode.NeedLoadStorage
            isLoadedFavoritesOnly() -> QuicklyNode.NeedLoadAllNodes
            else -> {
                runBlocking {
                    getNode(nodeId)
                }?.let { node ->
                    QuicklyNode.Loaded(node)
                } ?: QuicklyNode.NotFound(nodeId)
            }
        }
    }

    fun getQuicklyOrRootNode(): TetroidNode {
        val quicklyNode = getQuicklyNode()
        return if (quicklyNode is QuicklyNode.Loaded) {
            quicklyNode.node
        } else {
            getRootNode()
        }
    }

    fun getQuicklyNodeOrNull(): TetroidNode? {
        val quicklyNode = getQuicklyNode()
        return if (quicklyNode is QuicklyNode.Loaded) {
            quicklyNode.node
        } else {
            null
        }
    }

    fun setQuicklyNodeId(nodeId: String) {
        updateStorageOption(getString(R.string.pref_key_quickly_node_id), nodeId)
    }

    fun updateQuicklyNode() {
        val nodeId = storage?.quickNodeId
        if (nodeId != null && isStorageLoaded() && !isLoadedFavoritesOnly()) {
            launchOnIo {
                setQuicklyNodeId(nodeId)
            }
        }
    }

    fun getQuicklyNodeName(): String {
        return getQuicklyNode().getName(resourcesProvider)
    }

    fun getQuicklyNodeId(): String? {
        return storage?.quickNodeId
    }

    // endregion QuicklyNode

    fun clearTrashFolder() {
        launchOnMain {
            withIo {
                clearStorageTrashFolderUseCase.run(
                    ClearStorageTrashFolderUseCase.Params(
                        storage = storage!!,
                    )
                )
            }.onFailure {
                logFailure(it)
            }.onSuccess {
                log(R.string.title_trash_cleared, show = true)
            }
        }
    }

    // region Migration

    fun isNeedMigration(): Boolean {
        val fromVersion = CommonSettings.getSettingsVersion(getContext())
        return (fromVersion != 0 && fromVersion < Constants.SETTINGS_VERSION_CURRENT)
    }

    // endregion Migration

    // region Getters

    fun getFavoriteRecords(): List<TetroidRecord> {
        return favoritesManager.getFavoriteRecords()
    }

    fun getPathToRecordFolder(record: TetroidRecord): FilePath.Folder {
        return recordPathProvider.getPathToRecordFolder(record)
    }

    // endregion Getters

    // region Setters

    fun setIsDecrypted(value: Boolean) {
        storage?.isDecrypted = value
    }

    suspend fun setLastNodeIdAndSaveStorageInDb(nodeId: String?): Boolean {
        return storage?.let {
            it.lastNodeId = nodeId
            updateStorageInDb(storage = it)
        } ?: false
    }

    suspend fun setIsDecryptToTempAndSaveStorageInDb(value: Boolean): Boolean {
        return storage?.let {
            it.isDecryptToTemp = value
            updateStorageInDb(storage = it)
        } ?: false
    }

    // endregion Setters

    // region IStorageCallback

    suspend fun saveStorage(): Boolean {
        return withIo {
            saveStorageUseCase.run()
        }.foldResult(
            onLeft = {
                logFailure(it)
                false
            },
            onRight = { true }
        )
    }

    // endregion IStorageCallback

    // endregion StorageProperties

}
