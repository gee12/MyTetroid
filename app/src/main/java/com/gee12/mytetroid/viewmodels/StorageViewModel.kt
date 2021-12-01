package com.gee12.mytetroid.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import com.gee12.htmlwysiwygeditor.Dialogs
import com.gee12.htmlwysiwygeditor.Dialogs.*
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.data.crypt.TetroidCrypter
import com.gee12.mytetroid.data.ini.DatabaseConfig.EmptyFieldException
import com.gee12.mytetroid.helpers.NetworkHelper
import com.gee12.mytetroid.helpers.NetworkHelper.IWebFileResult
import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.utils.UriUtils
import com.gee12.mytetroid.utils.Utils
import com.gee12.mytetroid.views.activities.TetroidActivity.IDownloadFileResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * (замена StorageManager)
 */
open class StorageViewModel(
    app: Application,
    /*logger: TetroidLogger?,*/
    storagesRepo: StoragesRepo?,
    xmlLoader: TetroidXml?,
    crypter: TetroidCrypter?
) : StorageEncryptionViewModel(
    app,
    /*logger,*/
    storagesRepo ?: StoragesRepo(app),
    xmlLoader ?: TetroidXml(),
    crypter
) {

    var isCheckFavorMode = true
    var isAlreadyTryDecrypt = false
    var syncType = SyncStorageType.Manually
    var syncCallback: ICallback? = null


    //region Init

    init {
        // первоначальная инициализация компонентов приложения.
        App.init(
            context = getContext(),
            logger = this.logger,
            xmlLoader = this.xmlLoader,
            crypter = this.crypter,
            storagesRepo = this.storagesRepo,
            storageInteractor = this.storageInteractor
        )
    }

    /**
     * Инициализация хранилища по ID, переданному в Intent.
     */
    fun initStorage(intent: Intent): Boolean {
        if (storage != null) {
            return true
        }
        val storageId = intent.getIntExtra(Constants.EXTRA_STORAGE_ID, 0)
        return if (storageId > 0) {
            setStorageFromBase(storageId)
            true
        } else {
            initStorageFromLastStorageId()
        }
    }

    /**
     * Инициализация хранилища по ID хранилища, загруженному в последний раз.
     */
    fun initStorageFromLastStorageId(): Boolean {
        val storageId = CommonSettings.getLastStorageId(getContext())
        return if (storageId > 0) {
            setStorageFromBase(storageId)
            true
        } else {
            logError(getString(R.string.log_not_transferred_storage_id), true)
            setViewEvent(Constants.ViewEvents.FinishActivity)
            false
        }
    }

    open fun setStorageFromBase(id: Int) {
        launch {
            withContext(Dispatchers.IO) { storagesRepo.getStorage(id) }?.let { storage ->
                this@StorageViewModel.storage = storage
                setStorageEvent(Constants.StorageEvents.Changed, storage)

                if (xmlLoader.mIsStorageLoaded) {
                    storage.isLoaded = true
                }

                // загружаем настройки, но не загружаем само хранилище
                initStorage()
            } ?: run {
                log(getString(R.string.log_storage_not_found_mask).format(id))
//                setStorageEvent(Constants.StorageEvents.NotFound)
            }
        }
    }

    /**
     * Запуск первичной инициализации хранилища по-умолчанию с указанием флага isCheckFavorMode
     * @param isCheckFavorMode Стоит ли проверять необходимость загрузки только избранных записей.
     *                          Если отключено, то всегда загружать хранилище полностью.
     */
    fun startInitStorage(isLoadLastForced: Boolean, isCheckFavorMode: Boolean) {
        this.isCheckFavorMode = isCheckFavorMode
        startInitStorage()
    }

    /**
     * Поиск хранилища по-умолчанию в базе данных и запуск первичной его инициализация.
     */
    fun startInitStorage() {
        log(R.string.log_start_load_def_storage)
        launch {
            storagesRepo.getDefaultStorage()?.let { storage ->
                startInitStorage(storage)
            } ?: run {
                log(R.string.log_def_storage_not_specified)
                setStorageEvent(Constants.StorageEvents.NoDefaultStorage)
            }
        }
    }

    /**
     * Поиск хранилища в базе данных и запуск первичной его инициализация.
     */
    fun startInitStorage(id: Int) {
        launch {
            withContext(Dispatchers.IO) { storagesRepo.getStorage(id) }?.let { storage ->
                startInitStorage(storage)
            } ?: run {
                log(getString(R.string.log_storage_not_found_mask).format(id))
//                setStorageEvent(Constants.StorageEvents.NotFound)
            }
        }
    }

    /**
     * Запуск первичной инициализации хранилища.
     * Начинается с проверки разрешения на запись во внешнюю память устройства.
     */
    fun startInitStorage(storage: TetroidStorage) {
        this.storage = storage
        postStorageEvent(Constants.StorageEvents.Changed, storage)

        CommonSettings.setLastStorageId(getContext(), storage.id)

        // сначала проверяем разрешение на запись во внешнюю память
        postStorageEvent(Constants.StorageEvents.PermissionCheck)
    }

    /**
     * Запуск перезагрузки хранилища.
     *
     * TODO: может здесь нужно сразу loadStorage() ?
     */
    fun startReinitStorage() {
        // TODO: ?
//        val storageId = SettingsManager.getLastStorageId(getContext())
        App.CurrentStorageId?.let {
            startInitStorage(it)
        }
    }

    /**
     * Инициализация хранилища (с созданием файлов, если оно новое).
     */
    fun initStorage(isLoadFavoritesOnly: Boolean? = null): Boolean {
        isAlreadyTryDecrypt = false

        val result = initOrCreateStorage(storage!!)
        if (result) {
            log(getString(R.string.log_storage_settings_loaded) + getStoragePath())
            setStorageEvent(Constants.StorageEvents.Inited, storage)
        } else {
            logError(getString(R.string.log_failed_storage_init) + getStoragePath(), true)
            postStorageEvent(Constants.StorageEvents.InitFailed, isLoadFavoritesOnly ?: checkIsNeedLoadFavoritesOnly())
        }
        return result
    }

    /**
     * Инициализация хранилища (с созданием файлов, если оно новое),
     *   а затем его непосредственная загрузка (с расшифровкой, если зашифровано).
     * Вызывается уже после выполнения синхронизации.
     */
    fun initStorageAndLoad() {
        val isLoadFavoritesOnly = checkIsNeedLoadFavoritesOnly()

        if (initStorage(isLoadFavoritesOnly)) {
            val params = StorageParams(
                node = null,
                isNodeOpening = false,
                isFavoritesOnly = isLoadFavoritesOnly,
                isHandleReceivedIntent = true
            )
            if (isCrypted()) {
                // сначала устанавливаем пароль, а потом загружаем (с расшифровкой)
                params.isDecrypt = true
                checkPassAndDecryptStorage(params)
            } else {
                // загружаем
                loadOrDecryptStorage(params)
            }
        }
    }

    private fun checkIsNeedLoadFavoritesOnly(): Boolean {
        var isLoadFavoritesOnly = false
        if (isCheckFavorMode) {
            // читаем установленную опцию isLoadFavoritesOnly только 1 раз.
            // isCheckFavorMode может быть = false, когда, например:
            //  * загружаем хранилище из временной записи, созданной из виджета.
            //    В этом случае хранилище нужно загружать полностью.
            isLoadFavoritesOnly = !isLoaded() && isLoadFavoritesOnly()
                    || (isLoaded() && isLoadedFavoritesOnly())
        }
        // уже воспользовались, сбрасываем
        this.isCheckFavorMode = true
        return isLoadFavoritesOnly
    }

    /**
     * Непосредственная инициализация хранилища, с созданием файлов, если оно новое.
     * Загрузка параметров из файла database.ini и инициализация переменных.
     * @param storage
     * @return
     */
    private fun initOrCreateStorage(storage: TetroidStorage): Boolean {
//        storage.isLoaded = false
        databaseConfig.setFileName(storageInteractor.getPathToDatabaseIniConfig())

        val res: Boolean
        try {
            if (storage.isNew) {
                logDebug(getString(R.string.log_start_storage_creating) + storage.path)
                res = storageInteractor.createStorage(storage)
                if (res) {
                    storage.isNew = false
                    storage.isLoaded = true
                    log((R.string.log_storage_created), true)
                    storageEvent.postValue(ViewModelEvent(Constants.StorageEvents.FilesCreated, storage))
                    return true
                } else {
                    logError(getString(R.string.log_failed_storage_create_mask, storage.path), true)
                }
            } else {
                // загружаем database.ini
                res = databaseConfig.load()
            }
        } catch (ex: Exception) {
            logError(ex)
            return false
        }
        storage.isInited = res
        return res
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
                        && (node.isCrypted || node == FavoritesManager.FAVORITES_NODE)))
        if (isLoaded() && isDecrypt && isNodesExist()) {
            // расшифровываем уже загруженное хранилище
            startDecryptStorage(node)
        } else {
            // загружаем хранилище впервые, с расшифровкой
            startReadStorage(isDecrypt, params.isFavoritesOnly, params.isHandleReceivedIntent)
        }
    }

    //endregion Load

    //region Read

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

        launch {
            // FIXME: где правильнее сохранить ?
            App.CurrentStorageId = null

            // перед загрузкой
            val stringResId = if (isDecrypt) R.string.task_storage_decrypting else R.string.task_storage_loading
            setViewEvent(Constants.ViewEvents.TaskStarted, stringResId)

            // непосредственное чтение структуры хранилища
            val result = readStorage(isDecrypt, isFavoritesOnly)

            // после загрузки
            setViewEvent(Constants.ViewEvents.TaskFinished)

            if (result) {
                // FIXME: где правильнее сохранить ?
                App.CurrentStorageId = storage?.id

                val mes = getString(
                    when {
                        isFavoritesOnly -> R.string.log_storage_favor_loaded_mask
                        isDecrypt -> R.string.log_storage_loaded_decrypted_mask
                        else -> R.string.log_storage_loaded_mask
                    }
                )
                log(mes.format(getStorageName()), true)
            } else {
                logWarning(getString(R.string.log_failed_storage_load) + getStoragePath(), true)
            }

            val params = StorageParams(
                isDecrypt = isDecrypt,
                isFavoritesOnly = isFavoritesOnly,
                isHandleReceivedIntent = isOpenLastNode,
                result = result
            )
            // инициализация контролов
            setViewEvent(Constants.ViewEvents.InitGUI, params)
            // действия после загрузки хранилища
            setStorageEvent(Constants.StorageEvents.Loaded, result)
        }
    }

    /**
     * Загрузка хранилища из файла mytetra.xml.
     * @param isDecrypt Расшифровывать ли ветки
     * @param isFavorite Загружать ли только избранные записи
     * @return
     */
    suspend fun readStorage(isDecrypt: Boolean, isFavorite: Boolean): Boolean {
        val myTetraXmlFile = File(storageInteractor.getPathToMyTetraXml())
        if (!myTetraXmlFile.exists()) {
            logError(getString(R.string.log_file_is_absent) + StorageInteractor.MYTETRA_XML_FILE_NAME, true)
            return false
        }
        // получаем id избранных записей из настроек
        FavoritesManager.load(getContext())

        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            val result = withContext(Dispatchers.IO) {
                val fis = FileInputStream(myTetraXmlFile)
                // непосредственная обработка xml файла со структурой хранилища
                xmlLoader.parse(getContext(), fis, isDecrypt, isFavorite)
            }
            storage?.isLoaded = result

            // удаление не найденных записей из избранного
            FavoritesManager.check()
            // загрузка ветки для быстрой вставки
            updateQuicklyNode()
            return result
        } catch (ex: Exception) {
            logError(ex)
            postViewEvent(Constants.ViewEvents.ShowMoreInLogs)
            return false
        }
    }

    //endregion Read

    //region Decrypt

    /**
     * Непосредственная расшифровка уже загруженного хранилища.
     */
    private fun startDecryptStorage(node: TetroidNode?) {
        logOperStart(LogObj.STORAGE, LogOper.DECRYPT)

        launch {
            // перед расшифровкой
            setViewEvent(Constants.ViewEvents.TaskStarted, R.string.task_storage_decrypting)

            // непосредственная расшифровка
            val result = cryptInteractor.decryptStorage(getContext(), false)
            setIsDecrypted(result)

            // после расшифровки
            setViewEvent(Constants.ViewEvents.TaskFinished)

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
        postStorageEvent(Constants.StorageEvents.Decrypted, node)
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

        var middlePassHash: String?
        when {
            crypter.middlePassHash.also { middlePassHash = it } != null -> {
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
                    postStorageEvent(Constants.StorageEvents.AskForEmptyPassCheckingField,
                        EmptyPassCheckingFieldCallbackParams(ex.fieldName, middlePassHash!!, callback)
                    )
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

    override fun onPasswordCanceled(isSetup: Boolean, callback: EventCallbackParams) {
        if (!isSetup) {
            isAlreadyTryDecrypt = true
            super.onPasswordCanceled(isSetup, callback)
        }
    }

    fun getPinCodeInputHandler(params: StorageParams) = Dialogs.IApplyResult {
        params.isDecrypt = true
        loadOrDecryptStorage(params)
    }

    /**
     *
     */
    fun checkAndDecryptNode(node: TetroidNode): Boolean {
        if (!node.isNonCryptedOrDecrypted) {
            val params = StorageParams(
                node = node,
                isDecrypt = true,
                isNodeOpening = true,
                isFavoritesOnly = false,
                isHandleReceivedIntent = false
            )
//            if (isRequestPINCode()) {
//                // сначала просто проверяем пароль
//                //  затем спрашиваем ПИН,
//                //  а потом уже расшифровываем (!)
//
//                // FIXME:
//                //  раньше вместо node стояло FavoritesManager.FAVORITES_NODE
//
//                decryptStorage(params)
//            } else {
//                askPassword(params)
//            }
            checkPassAndDecryptStorage(params)
            // выходим,запрос пароля будет в асинхронном режиме
            return true
        }
        return false
    }

    // FIXME: почему-то нигде не вызывается
    fun onRecordDecrypt(record: TetroidRecord): Boolean {
        if (record.isFavorite && !record.isNonCryptedOrDecrypted) {
            // запрос пароля в асинхронном режиме

            // запрос на расшифровку записи может поступить только из списка Избранных записей,
            //  поэтому отправляем FavoritesManager.FAVORITES_NODE
            val params = StorageParams(
                node = FavoritesManager.FAVORITES_NODE,
                isNodeOpening = true,
                isFavoritesOnly = isLoadFavoritesOnly(),
                isHandleReceivedIntent = false
            )

            checkPassAndDecryptStorage(params)
            return true
        }
        return false
    }

    fun setIsPINNeedToEnter() {
        isPinNeedEnter = true
    }

    fun clearSavedPass() {
        passInteractor.clearSavedPass(storage!!)
        updateStorage(storage!!)
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
        getSyncProfile()?.let {
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
                            postStorageEvent(Constants.StorageEvents.AskBeforeSyncOnInit)
                        } else {
                            startStorageSync(activity, callback)
                        }
                    }
                    // перед выходом из приложения
                    syncType == SyncStorageType.BeforeExit && it.isSyncBeforeExit -> {
                        if (it.isAskBeforeSyncOnExit) {
                            postStorageEvent(Constants.StorageEvents.AskBeforeSyncOnExit)
                        } else {
                            startStorageSync(activity, callback)
                        }
                    }
                    else -> {}
                }

            } else {
                callback?.run(false)
            }
        } ?: callback?.run(false)
    }

    fun startStorageSync(activity: Activity) {
        startStorageSync(activity, syncCallback)
    }

    fun cancelStorageSync() {
        syncCallback?.run(false)
    }

    /**
     * Отправка запроса на синхронизацию стороннему приложению перед загрузкой хранилища.
     * @param activity
     * @param callback
     */
    private fun startStorageSync(activity: Activity, callback: ICallback?) {
        val res = syncInteractor.startStorageSync(activity, getStoragePath(), Constants.REQUEST_CODE_SYNC_STORAGE)
        if (callback != null) {
            // запускаем обработчик сразу после синхронизации, не дожидаясь ответа, если:
            //  1) синхронизацию не удалось запустить
            //  2) выбрана синхронизация с помощью Termux,
            //  т.к. в этом случае нет простого механизма получить ответ
            if (!res || getSyncAppName() == activity.getString(R.string.title_app_termux)) {
                callback.run(res)
            }
        }
    }

    /**
     * Обработка результата синхронизации хранилища.
     *
     * @param res
     */
    fun onStorageSyncFinished(res: Boolean) {
        log(if (res) R.string.log_sync_successful else R.string.log_sync_failed, true)
        when (syncType) {
            SyncStorageType.Manually -> {
                syncCallback?.run(res)
            }
            SyncStorageType.BeforeInit -> {
                if (res) syncCallback?.run(res)
                else postStorageEvent(Constants.StorageEvents.AskAfterSyncOnInit, res)
            }
            SyncStorageType.BeforeExit -> {
                if (res) syncCallback?.run(res)
                else postStorageEvent(Constants.StorageEvents.AskAfterSyncOnExit, res)
            }
        }
        syncType = SyncStorageType.Manually
        syncCallback = null
    }

    //endregion Sync

    //region Records

    fun getRecord(id: String?): TetroidRecord? = recordsInteractor.getRecord(id)

    open fun editRecordFields(record: TetroidRecord, name: String, tags: String, author: String, url: String, node: TetroidNode, isFavor: Boolean) {
        launch {
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
        launch {
            if (!nodesInteractor.isExistCryptedNodes(true)) {
                setStorageEvent(Constants.StorageEvents.AskForClearStoragePass)
            }
        }
    }

    /**
     * Загрузка всех веток, когда загружено только избранное.
     * @param isHandleReceivedIntent Нужно ли обработать mReceivedIntent после загрузки веток.
     */
    fun loadAllNodes(isHandleReceivedIntent: Boolean) {
        val params = StorageParams(
            node = null,
            isNodeOpening = false,
            isFavoritesOnly = false,
            isHandleReceivedIntent = isHandleReceivedIntent
        )
        if (isCrypted()) {
            // FIXME: не передаем node=FAVORITES_NODE, т.к. тогда хранилище сразу расшифровуется без запроса ПИН-кода
            //  По-идее, нужно остановить null, но сразу расшифровывать хранилище, если до этого уже
            //    вводили ПИН-код (для расшифровки избранной записи)
            //  Т.Е. сохранять признак того, что ПИН-крд уже вводили в этой "сессии"
            checkPassAndDecryptStorage(params)
        } else {
            loadOrDecryptStorage(params)
        }
    }

    //endregion Nodes

    //region Tags

//    /**
//     * Разбираем строку с метками записи и добавляем метки в запись и в дерево.
//     * @param record
//     * @param tagsString Строка с метками (не зашифрована).
//     * Передается отдельно, т.к. поле в записи может быть зашифровано.
//     */
//    override fun parseRecordTags(record: TetroidRecord?, tagsString: String) {
//        if (record == null) return
//        if (!TextUtils.isEmpty(tagsString)) {
//            for (tagName in tagsString.split(TetroidXml.TAGS_SEPAR.toRegex()).toTypedArray()) {
//                val lowerCaseTagName = tagName.lowercase(Locale.getDefault())
//                var tag: TetroidTag
//                if (xmlLoader.mTagsMap.containsKey(lowerCaseTagName)) {
//                    tag = xmlLoader.mTagsMap.get(lowerCaseTagName) ?: continue
//                    // добавляем запись по метке, только если ее еще нет
//                    // (исправление дублирования записей по метке, если одна и та же метка
//                    // добавлена в запись несколько раз)
//                    if (!tag.records.contains(record)) {
//                        tag.addRecord(record)
//                    }
//                } else {
//                    val tagRecords: MutableList<TetroidRecord> = ArrayList()
//                    tagRecords.add(record)
//                    tag = TetroidTag(lowerCaseTagName, tagRecords)
//                    xmlLoader.mTagsMap.put(lowerCaseTagName, tag)
//                }
//                record.addTag(tag)
//            }
//        }
//    }

//    /**
//     * Удаление меток записи из списка.
//     * @param record
//     */
//    override fun deleteRecordTags(record: TetroidRecord?) {
//        if (record == null) return
//        if (record.tags.isNotEmpty()) {
//            for (tag in record.tags) {
//                val foundedTag = tagsInteractor.getTag(tag.name)
//                if (foundedTag != null) {
//                    // удаляем запись из метки
//                    foundedTag.records.remove(record)
//                    if (foundedTag.records.isEmpty()) {
//                        // удаляем саму метку из списка
//                        xmlLoader.mTagsMap.remove(tag.name.lowercase(Locale.getDefault()))
//                    }
//                }
//            }
//            record.tags.clear()
//        }
//    }

    //endregion Tags

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
        postViewEvent(Constants.ViewEvents.ShowProgressText, getString(R.string.title_file_downloading))
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
                postViewEvent(Constants.ViewEvents.ShowProgress, false)
            }

            override fun onError(ex: Exception) {
                callback?.onError(ex)
                logError(getString(R.string.log_error_download_file_mask, ex.message ?: ""))
                postViewEvent(Constants.ViewEvents.ShowProgress, false)
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

    suspend fun setDefaultStorage() {
//        val storage = storagesRepo.getDefaultStorage()
//        _storage.postValue(storage)
        this.storage = storagesRepo.getDefaultStorage()
        postStorageEvent(Constants.StorageEvents.Changed, storage)
    }

    fun getStorageFolderSize() = storageInteractor.getStorageFolderSize(getContext()) ?: getString(R.string.title_error)

    fun getMyTetraXmlLastModifiedDate() = storageInteractor.getMyTetraXmlLastModifiedDate(getContext())?.let {
        Utils.dateToString(it, getString(R.string.full_date_format_string))
    } ?: getString(R.string.title_error)

    fun checkPermission(activity: Activity) {
        if (permissionInteractor.checkWriteExtStoragePermission(activity, Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE)) {
            onPermissionChecked()
        }
    }

    //endregion Other

//    override fun isRecordFavorite(id: String?): Boolean {
//        // TODO: to Interactor
//        return FavoritesManager.isFavorite(id)
//    }

//    override fun addRecordFavorite(record: TetroidRecord?) {
//        // TODO: to Interactor
//        FavoritesManager.set(record)
//    }

//    override fun loadIcon(context: Context, node: TetroidNode) {
//        if (node.isNonCryptedOrDecrypted) {
//            node.loadIcon(context, storageInteractor.getPathToIcons())
//        }
//    }

}

data class StorageParams(
    var result: Boolean? = null, // результат открытия/расшифровки
    var isDecrypt: Boolean? = null, // расшифровка хранилища, а не просто открытие
    val node: TetroidNode? = null, // ветка, которую нужно открыть после расшифровки хранилища
    val isNodeOpening: Boolean = false, // если true, значит хранилище уже было загружено, и нажали на еще не расшифрованную ветку
    var isFavoritesOnly: Boolean, // нужно ли загружать только избранные записи,
                                  //  или загружены только избранные записи, т.е. в избранном нажали на не расшифрованную запись
    val isHandleReceivedIntent: Boolean, // ужно ли после загрузки открыть ветку, сохраненную в опции getLastNodeId() 
                                 //  или ветку с избранным (если именно она передана в node)
    var passHash: String? = null, // хеш пароля
    var fieldName: String? = null, // поле в database.ini
    var openedDrawer: Int? = null, //TODO: почему не используется ?
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