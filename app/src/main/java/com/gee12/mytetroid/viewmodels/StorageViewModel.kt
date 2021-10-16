package com.gee12.mytetroid.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import com.gee12.htmlwysiwygeditor.Dialogs
import com.gee12.htmlwysiwygeditor.Dialogs.*
import com.gee12.mytetroid.App.init
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.data.ini.DatabaseConfig.EmptyFieldException
import com.gee12.mytetroid.helpers.NetworkHelper
import com.gee12.mytetroid.helpers.NetworkHelper.IWebFileResult
import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.utils.FileUtils
import com.gee12.mytetroid.utils.UriUtils
import com.gee12.mytetroid.utils.Utils
import com.gee12.mytetroid.views.activities.TetroidActivity.IDownloadFileResult
import com.gee12.mytetroid.views.dialogs.AskDialogs
import com.gee12.mytetroid.views.dialogs.pass.PassDialogs.IPassInputResult
import com.gee12.mytetroid.views.dialogs.pass.PassEnterDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * (замена StorageManager)
 */
open class StorageViewModel/*<E>*/(
    app: Application,
    storagesRepo: StoragesRepo,
    xmlLoader: TetroidXml
) : StorageEncryptionViewModel(app, storagesRepo, xmlLoader) {

    val tagsInteractor = TagsInteractor(logger, storageInteractor, xmlLoader)
    val attachesInteractor = AttachesInteractor(logger, storageInteractor, cryptInteractor, dataInteractor, interactionInteractor, recordsInteractor)

    var isCheckFavorMode = true
    var isAlreadyTryDecrypt = false
    var isLoadStorageAfterSync = false


    //region Init

    /**
     * Первоначальная инициализация компонентов приложения.
     */
    fun initApp() {
        init(getContext(), logger, xmlLoader, storageInteractor)
    }

    /**
     *
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

    fun initStorageFromLastStorageId(): Boolean {
        val storageId = SettingsManager.getLastStorageId(getContext())
        return if (storageId > 0) {
            setStorageFromBase(storageId)
            true
        } else {
            logError(getString(R.string.log_not_transferred_storage_id), true)
            setViewEvent(Constants.ViewEvents.FinishActivity)
            false
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
        launch {
//            setDefaultStorage()
            startInitStorage(storagesRepo.getDefaultStorage())
        }
    }

    /**
     * Поиск хранилища в базе данных и запуск первичной его инициализация.
     */
    fun startInitStorage(id: Int) {
        launch {
            startInitStorage(storagesRepo.getStorage(id))
        }
    }

    /**
     * Запуск первичной инициализации хранилища.
     * Начинается с проверки разрешения на запись во внешнюю память устройства.
     */
    private fun startInitStorage(storage: TetroidStorage?) {
        this.storage = storage
        postStorageEvent(Constants.StorageEvents.Changed, storage)
        if (storage != null) {
            SettingsManager.setLastStorageId(getContext(), storage.id);

            // сначала проверяем разрешение на запись во внешнюю память
            postStorageEvent(Constants.StorageEvents.PermissionCheck)
        } else {
            logError(getString(R.string.log_not_transferred_storage), true)
        }
    }

    /**
     * Запуск перезагрузки хранилища.
     *
     * TODO: может здесь нужно сразу loadStorage() ?
     */
    fun startReinitStorage() {
        val storageId = SettingsManager.getLastStorageId(getContext())
        startInitStorage(storageId)
    }

    /**
     * Первичная инициализация хранилища или запуск синхронизации перед этим, если она включена.
     * Выполняется после проверки разрешения на запись во внешнюю память устройства.
     */
    fun initOrSyncStorage(activity: Activity) {
        getSyncProfile()?.let {
            if (it.isEnabled && it.isSyncBeforeInit) {
                // устанавливаем в true, чтобы после окончания синхронизации запустить загрузку хранилища
                this.isLoadStorageAfterSync = true

                if (it.isAskBeforeSyncOnInit) {
                    postStorageEvent(Constants.StorageEvents.AskBeforeSyncOnInit, storage)
                } else {
                    syncStorage(activity)
                }
            } else {
                initStorageAndLoad()
            }
        } ?: initStorageAndLoad()
    }

    /**
     * Инициализация хранилища (с созданием файлов, если оно новое),
     *   а затем его непосредственная загрузка (с расшифровкой, если зашифровано).
     * Вызывается уже после выполнения синхронизации.
     */
    fun initStorageAndLoad() {
        this.isAlreadyTryDecrypt = false
//        this.isLoadStorageAfterSync = false

        var isFavorMode = false;
        if (isCheckFavorMode) {
            // читаем установленную опцию isLoadFavoritesOnly только при первой загрузке
            isFavorMode = !isLoaded() && isLoadFavoritesOnly()
                    || (isLoaded() && isLoadedFavoritesOnly());
        }
        // уже воспользовались, сбрасываем
        this.isCheckFavorMode = true

        val res = initOrCreateStorage(storage!!)
        if (res) {
            log(getString(R.string.log_storage_settings_inited) + getStoragePath())
            if (isCrypted()) {
                // сначала устанавливаем пароль, а потом загружаем (с расшифровкой)
                decryptStorage(null, false, isFavorMode, true)
            } else {
                // загружаем
                loadOrDecryptStorage(null, false, isFavorMode, true)
            }
        } else {
            logError(getString(R.string.log_failed_storage_init) + getStoragePath(), true)
            postStorageEvent(Constants.StorageEvents.InitFailed, isFavorMode)
        }
    }

    /**
     * Непосредственная инициализация хранилища, с созданием файлов, если оно новое.
     * Загрузка параметров из файла database.ini и инициализация переменных.
     * @param storage
     * @return
     */
    private fun initOrCreateStorage(storage: TetroidStorage): Boolean {
        storage.isLoaded = false

//        this.databaseConfig = DatabaseConfig(logger, storageInteractor.getPathToDatabaseIniConfig())
        databaseConfig.setFileName(storageInteractor.getPathToDatabaseIniConfig())

        val res: Boolean
        try {
            val storageDir = File(storage.path)
            if (storage.isNew) {
                logDebug(getString(R.string.log_start_storage_creating) + storage.path)
                if (storageDir.exists()) {
                    // проверяем, пуст ли каталог
                    if (!FileUtils.isDirEmpty(storageDir)) {
                        logError(R.string.log_dir_not_empty)
                        return false
                    }
                } else {
                    logError(R.string.log_dir_is_missing)
                    return false
                }
                // сохраняем новый database.ini
                res = databaseConfig.saveDefault()
                // создаем каталог base
                if (!storageInteractor.createBaseFolder()) {
                    return false
                }
                // добавляем корневую ветку
                xmlLoader.init()
                storage.isLoaded = true
                // создаем Favorites
                FavoritesManager.create()
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
    private fun loadOrDecryptStorage(
        node: TetroidNode?,
        isDecrypt: Boolean,
        isOnlyFavorites: Boolean,
        isHandleReceivedIntent: Boolean
    ) {
        // расшифровуем хранилище только в том случаем, если:
        //  1) не используем проверку ПИН-кода
        //  2) используем проверку ПИН-кода, при этом расшифровуем с открытием конкретной <b>зашифрованной</b> ветки
        //   (или ветки Избранное)
        var isDecrypt = isDecrypt
        isDecrypt = (isDecrypt
                && (!isRequestPINCode()
                    || isRequestPINCode() && node != null
                        && (node.isCrypted || node == FavoritesManager.FAVORITES_NODE)))
        if (isDecrypt && isNodesExist()) {
            // расшифровываем уже загруженное хранилище
            doDecryptStorage(node)
        } else {
            // загружаем хранилище впервые, с расшифровкой
            startReadStorage(isDecrypt, isOnlyFavorites, isHandleReceivedIntent)
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

        // FIXME: нужно выполнять в IO потоке, иначе событие TaskFinished не выполняется..
        launch {
            // перед загрузкой
            val stringResId = if (isDecrypt) R.string.task_storage_decrypting else R.string.task_storage_loading
            setViewEvent(Constants.ViewEvents.TaskStarted, stringResId)

            // непосредственное чтение структуры хранилища
            val result = readStorage(isDecrypt, isFavoritesOnly)

            // после загрузки
            setViewEvent(Constants.ViewEvents.TaskFinished)

            if (result) {
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

            val params = ReadDecryptStorageState(
                isDecrypt = isDecrypt,
                isFavoritesOnly = isFavoritesOnly,
                isOpenLastNode = isOpenLastNode,
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
    private fun doDecryptStorage(node: TetroidNode?) {
        logOperStart(LogObj.STORAGE, LogOper.DECRYPT)

        launch {
            // перед расшифровкой
            postViewEvent(Constants.ViewEvents.TaskStarted)

            // непосредственная расшифровка
            val result = cryptInteractor.decryptStorage(getContext(), false)
            storage?.isDecrypted = result

            // после расшифровки
            postViewEvent(Constants.ViewEvents.TaskFinished)

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
     * Получение пароля и расшифровка хранилища. Вызывается при:
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
    private fun decryptStorage(
        node: TetroidNode?,
        isNodeOpening: Boolean,
        isOnlyFavorites: Boolean,
        isHandleReceivedIntent: Boolean
    ) {
        // устанавливаем признак
        setIsPINNeedToEnter()

        var middlePassHash: String?
        // пароль уже вводили или он сохранен локально?
        if (getCrypter().middlePassHash.also { middlePassHash = it } != null
            || isSaveMiddlePassLocal() && getMiddlePassHash().also { middlePassHash = it } != null) {
            // проверяем
            try {
                if (passInteractor.checkMiddlePassHash(middlePassHash)) {
                    cryptInteractor.initCryptPass(middlePassHash!!, true)
                    // запрос ПИН-кода
                    val callback = CallbackParam(
                        Constants.StorageEvents.LoadOrDecryptStorage,
                        null
                    )
//                    askPinCode(isNodeOpening) {
//                        loadOrDecryptStorage(node, true, isOnlyFavorites, isHandleReceivedIntent)
//                    }
                    askPinCode(isNodeOpening, callback)
                } else if (isNodeOpening) {
                    // спрашиваем пароль
                    askPassword(node, isNodeOpening, isOnlyFavorites, isHandleReceivedIntent)
                } else {
                    log(R.string.log_wrong_saved_pass, true)
                    if (!isAlreadyTryDecrypt) {
                        isAlreadyTryDecrypt = true
                        loadOrDecryptStorage(node, false, isOnlyFavorites, isHandleReceivedIntent)
                    }
                }
            } catch (ex: EmptyFieldException) {
                // если поля в INI-файле для проверки пустые
                logError(ex)
                // спрашиваем "continue anyway?"
                val passHash = middlePassHash

                //
                val params = AskPasswordParams(node, isNodeOpening, isOnlyFavorites, isHandleReceivedIntent, passHash, ex.fieldName)
                postStorageEvent(Constants.StorageEvents.EmptyPassCheck, params)
//                PassDialogs.showEmptyPassCheckingFieldDialog(context, ex.fieldName, getEmptyPassCheckingFieldCallback())
            }
        } else if (SettingsManager.isAskPassOnStart(getContext()) || isNodeOpening) {
            // если пароль не сохранен, то спрашиваем его, когда также:
            //      * если нужно расшифровывать хранилище сразу на старте
            //      * если функция вызвана во время открытия зашифрованной ветки
            //      * ??? если мы не вызвали загрузку всех веток
            askPassword(node, isNodeOpening, isOnlyFavorites, isHandleReceivedIntent)
        } else {
            // тогда просто загружаем хранилище без расшифровки, если:
            //      * не сохранен пароль
            //      * пароль не нужно спрашивать на старте
            //      * функция не вызвана во время открытия зашифрованной ветки
            loadOrDecryptStorage(node, false, isOnlyFavorites, isHandleReceivedIntent)
        }
    }

    fun getEmptyPassCheckingFieldCallback(params: AskPasswordParams) = object : IApplyCancelResult {
        override fun onApply() {
            cryptInteractor.initCryptPass(params.passHash!!, true)
            // запрос ПИН-кода
            postStorageEvent(Constants.StorageEvents.AskPinCode, params)
        }
        override fun onCancel() {
            if (!params.isNodeOpening) {
                // загружаем хранилище без пароля
                loadOrDecryptStorage(params.node, false, params.isOnlyFavorites, params.isOpenLastNode)
            }
        }
    }

    /**
     * Отображения запроса пароля от хранилища.
     *
     * @param node            Зашифрованная ветка, которую нужно открыть после засшифровки.
     * @param isNodeOpening   Вызвана ли функция при попытке открытия зашифрованной ветки
     * @param isOnlyFavorites Нужно ли загружать только избранные записи
     * @param isOpenLastNode  Нужно ли после загрузки открыть ветку, сохраненную в опции getLastNodeId()
     * или ветку с избранным (если именно она передана в node)
     */
    fun askPassword(node: TetroidNode?, isNodeOpening: Boolean, isOnlyFavorites: Boolean, isOpenLastNode: Boolean) {
        log(R.string.log_show_pass_dialog)
        val params = AskPasswordParams(node, isNodeOpening, isOnlyFavorites, isOpenLastNode, null, null)
        postStorageEvent(Constants.StorageEvents.AskPassword, params)
    }

    fun getPassInputHandler(params: AskPasswordParams) = object : IPassInputResult {
        override fun applyPass(pass: String, node: TetroidNode?) {
            // подтверждение введенного пароля
            checkPass(pass, { res: Boolean ->
                launch {
                    if (res) {
                        initPass(pass)
                        //
                        postStorageEvent(Constants.StorageEvents.AskPinCode, params)
                    } else {
                        // повторяем запрос
                        askPassword(node, params.isNodeOpening, params.isOnlyFavorites, params.isOpenLastNode)
                    }
                }
            }, R.string.log_pass_is_incorrect)
        }

        override fun cancelPass() {
            // ---Если при первой загрузке хранилища установлена текущей зашифрованная ветка (node),
            // --- и пароль не сохраняли, то нужно его спросить.
            if ( /*!getInstance().mIsAlreadyTryDecrypt
//                        && !StorageManager.isLoaded()
                        && !isNodeOpening
                        && StorageManager.isFavoritesMode()*/params.node == null) {
                // ---Но если в первый раз пароль вводить отказались, то при втором отказе ввода
                // --- просто грузим хранилище как есть (чтобы небыло циклического запроса пароля, если
                // --- его просто не хотим вводить)
                isAlreadyTryDecrypt = true
                loadOrDecryptStorage(params.node, false, params.isOnlyFavorites, params.isOpenLastNode)
            }
        }
    }

    fun getPinCodeInputHandler(params: AskPasswordParams) = Dialogs.IApplyResult {
            loadOrDecryptStorage(params.node, true, params.isOnlyFavorites, params.isOpenLastNode)
        }

    fun onNodeDecrypt(node: TetroidNode): Boolean {
        if (!node.isNonCryptedOrDecrypted) {
            if (isRequestPINCode()) {
                // TODO: сначала просто проверяем пароль
                //  затем спрашиваем ПИН,
                //  а потом уже расшифровываем (!)
                decryptStorage(FavoritesManager.FAVORITES_NODE, true, false, false)
            } else {
                askPassword(FavoritesManager.FAVORITES_NODE, true, false, false)
            }
            // выходим, т.к. запрос пароля будет в асинхронном режиме
            return true
        }
        return false
    }

    fun onRecordDecrypt(record: TetroidRecord): Boolean {
        if (record.isFavorite && !record.isNonCryptedOrDecrypted) {
            // запрос пароля в асинхронном режиме
            if (isRequestPINCode()) {
                decryptStorage(FavoritesManager.FAVORITES_NODE, true, isLoadFavoritesOnly(), false)
            } else {
                askPassword(FavoritesManager.FAVORITES_NODE, true, isLoadFavoritesOnly(), false)
            }
            return true
        }
        return false
    }

    fun setIsPINNeedToEnter() {
        isPinNeedEnter = true
    }

    fun clearSavedPass() {
        passInteractor.clearSavedPass()
        updateStorage(storage!!)
    }

    //endregion Decrypt

    //region Sync

    fun syncStorage(activity: Activity, isLoadStorageAfterSync: Boolean = true) {
        this.isLoadStorageAfterSync = isLoadStorageAfterSync
        startStorageSync(activity) {
            initStorageAndLoad()
        }
    }

    /**
     * Отправка запроса на синхронизацию стороннему приложению перед загрузкой хранилища.
     * @param activity
     * @param callback
     */
    fun startStorageSync(activity: Activity, callback: Runnable?) {
        val res = syncInteractor.startStorageSync(activity, getStoragePath(), Constants.REQUEST_CODE_SYNC_STORAGE)
        if (callback != null) {
            // запускаем обработчик сразу после синхронизации, не дожидаясь ответа, если:
            //  1) синхронизацию не удалось запустить
            //  2) выбрана синхронизация с помощью Termux,
            //  т.к. в этом случае нет простого механизма получить ответ
            if (!res || getSyncAppName() == activity.getString(R.string.title_app_termux)) {
                callback.run()
            }
        }
    }

    fun startStorageSyncAndInit(activity: Activity) {
        startStorageSync(activity) {
            initStorageAndLoad()
        }
    }

    /**
     * Отправка запроса на синхронизацию стороннему приложению перед выходом из приложения.
     * @param activity
     * @param callback Обработчик события после выполнения синхронизации или вместо нее.
     */
    fun startStorageSyncAndExit(activity: Activity, callback: Runnable) {
        getSyncProfile()?.let {
            if (it.isEnabled && it.isSyncBeforeExit) {
                if (it.isAskBeforeSyncOnExit) {
                    postStorageEvent(Constants.StorageEvents.AskBeforeSyncOnExit, true)

                    // TODO: перенести в MainActivity
                    AskDialogs.showSyncRequestDialog(activity, object : IApplyCancelDismissResult {
                        override fun onApply() {
                            startStorageSync(activity, null)
                            callback.run()
                        }

                        override fun onCancel() {
                            callback.run()
                        }

                        override fun onDismiss() {
                            callback.run()
                        }
                    })
                } else {
                    startStorageSync(activity, null)
                    callback.run()
                }
            } else {
                callback.run()
            }
        } ?: callback.run()
    }

    /**
     * Обработка результата синхронизации хранилища.
     *
     * @param res
     */
    fun onSyncStorageFinished(res: Boolean) {
        if (res) {
            log(R.string.log_sync_successful, true)
            if (isLoadStorageAfterSync)
                initStorageAndLoad()
            else {
                postStorageEvent(Constants.StorageEvents.AskAfterSyncOnInit, true)
            }
        } else {
            log(getString(R.string.log_sync_failed), true)
            if (isLoadStorageAfterSync) {
                postStorageEvent(Constants.StorageEvents.AskAfterSyncOnInit, false)
            }
        }
        this.isLoadStorageAfterSync = false
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
        if (!nodesInteractor.isExistCryptedNodes(true)) {
            postStorageEvent(Constants.StorageEvents.AskForClearStoragePass)
        }
    }

    /**
     * Загрузка всех веток, когда загружено только избранное.
     * @param isHandleReceivedIntent Нужно ли обработать mReceivedIntent после загрузки веток.
     */
    fun loadAllNodes(isHandleReceivedIntent: Boolean) {
        if (isCrypted()) {
            // FIXME: не передаем node=FAVORITES_NODE, т.к. тогда хранилище сразу расшифровуется без запроса ПИН-кода
            //  По-идее, нужно остановить null, но сразу расшифровывать хранилище, если до этого уже
            //    вводили ПИН-код (для расшифровки избранной записи)
            //  Т.Е. сохранять признак того, что ПИН-крд уже вводили в этой "сессии"
            decryptStorage(null, false, false, isHandleReceivedIntent)
        } else {
            loadOrDecryptStorage(null, false, false, isHandleReceivedIntent)
        }
    }

    //endregion Nodes

    //region Tags

    /**
     * Разбираем строку с метками записи и добавляем метки в запись и в дерево.
     * @param record
     * @param tagsString Строка с метками (не зашифрована).
     * Передается отдельно, т.к. поле в записи может быть зашифровано.
     */
    override fun parseRecordTags(record: TetroidRecord?, tagsString: String) {
        if (record == null) return
        if (!TextUtils.isEmpty(tagsString)) {
            for (tagName in tagsString.split(TetroidXml.TAGS_SEPAR.toRegex()).toTypedArray()) {
                val lowerCaseTagName = tagName.lowercase(Locale.getDefault())
                var tag: TetroidTag
                if (xmlLoader.mTagsMap.containsKey(lowerCaseTagName)) {
                    tag = xmlLoader.mTagsMap.get(lowerCaseTagName) ?: continue
                    // добавляем запись по метке, только если ее еще нет
                    // (исправление дублирования записей по метке, если одна и та же метка
                    // добавлена в запись несколько раз)
                    if (!tag.records.contains(record)) {
                        tag.addRecord(record)
                    }
                } else {
                    val tagRecords: MutableList<TetroidRecord> = ArrayList()
                    tagRecords.add(record)
                    tag = TetroidTag(lowerCaseTagName, tagRecords)
                    xmlLoader.mTagsMap.put(lowerCaseTagName, tag)
                }
                record.addTag(tag)
            }
        }
    }

    /**
     * Удаление меток записи из списка.
     * @param record
     */
    override fun deleteRecordTags(record: TetroidRecord?) {
        if (record == null) return
        if (record.tags.isNotEmpty()) {
            for (tag in record.tags) {
                val foundedTag = tagsInteractor.getTag(tag.name)
                if (foundedTag != null) {
                    // удаляем запись из метки
                    foundedTag.records.remove(record)
                    if (foundedTag.records.isEmpty()) {
                        // удаляем саму метку из списка
                        xmlLoader.mTagsMap.remove(tag.name.lowercase(Locale.getDefault()))
                    }
                }
            }
            record.tags.clear()
        }
    }

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
        return withContext(Dispatchers.IO) { dataInteractor.swapTetroidObjects(getContext(), list, pos, isUp, through) }
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

    //endregion Other

    override fun isRecordFavorite(id: String?): Boolean {
        // TODO: to Interactor
        return FavoritesManager.isFavorite(id)
    }

    override fun addRecordFavorite(record: TetroidRecord?) {
        // TODO: to Interactor
        FavoritesManager.set(record)
    }

    override fun loadIcon(context: Context, node: TetroidNode) {
        if (node.isNonCryptedOrDecrypted) {
            node.loadIcon(context, storageInteractor.getPathToIcons())
        }
    }

}

data class AskPasswordParams(
    val node: TetroidNode?,
    val isNodeOpening: Boolean,
    val isOnlyFavorites: Boolean,
    val isOpenLastNode: Boolean,
    val passHash: String?,
    val fieldName: String?
)

class ReadDecryptStorageState(
    var result: Boolean? = null,
    var isDecrypt: Boolean? = null,
    var isFavoritesOnly:Boolean? = null,
    var isOpenLastNode: Boolean? = null,
    var openedDrawer: Int? = null,
    var node: TetroidNode? = null
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