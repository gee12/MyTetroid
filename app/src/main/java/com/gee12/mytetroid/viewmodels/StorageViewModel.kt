package com.gee12.mytetroid.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.FileObserver
import android.text.TextUtils
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.gee12.htmlwysiwygeditor.Dialogs
import com.gee12.htmlwysiwygeditor.Dialogs.*
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.SingleLiveEvent
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.data.ini.DatabaseConfig.EmptyFieldException
import com.gee12.mytetroid.helpers.NetworkHelper
import com.gee12.mytetroid.helpers.NetworkHelper.IWebFileResult
import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.logs.TetroidLog
import com.gee12.mytetroid.logs.TetroidLog.*
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.services.FileObserverService
import com.gee12.mytetroid.utils.FileUtils
import com.gee12.mytetroid.utils.UriUtils
import com.gee12.mytetroid.views.Message
import com.gee12.mytetroid.views.activities.TetroidActivity.IDownloadFileResult
import com.gee12.mytetroid.views.dialogs.AskDialogs
import com.gee12.mytetroid.views.dialogs.PassDialogs.IPassInputResult
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
    private val storagesRepo: StoragesRepo
) : StorageSettingsViewModel(app, storagesRepo) {

    val objectAction: SingleLiveEvent<ViewModelEvent<Constants.ObjectEvents, Any>> = SingleLiveEvent()

    override val storageLoadHelper = StorageLoadHelper()

    var isCheckFavorMode = true
    var isAlreadyTryDecrypt = false
    var isLoadStorageAfterSync = false
    var isPINNeedEnter = false

    var databaseConfig: DatabaseConfig? = null
        private set

    private var isStorageChangingHandled = false


    //region Init

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
        setDefaultStorage()
        startInitStorage(storage.value)
    }

    /**
     * Поиск хранилища в базе данных и запуск первичной его инициализация.
     */
    fun startInitStorage(id: Int) {
        super.setStorageFromBase(id)
        startInitStorage(storage.value)
    }

    /**
     * Запуск первичной инициализации хранилища.
     * Начинается с проверки разрешения на запись во внешнюю память устройства.
     */
    private fun startInitStorage(storage: TetroidStorage?) {
        if (storage != null) {
            SettingsManager.setLastStorageId(getContext(), storage.id);

            // сначала проверяем разрешение на запись во внешнюю память
            updateStorageState(Constants.StorageEvents.PermissionCheck)
        } else {
            //TODO: error
        }
    }

    /**
     * Запуск перезагрузки хранилища.
     *
     * TODO: может здесь нужно сразу loadStorage() ?
     */
    fun startReinitStorage() {
        val storageId: Int = SettingsManager.getLastStorageId(getContext())
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
                    updateStorageState(Constants.StorageEvents.AskBeforeSyncOnInit, storage.value)
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

        val res = initOrCreateStorage(storage.value!!)
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
            updateStorageState(Constants.StorageEvents.InitFailed, isFavorMode)
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

        this.databaseConfig = DatabaseConfig(logger, storageInteractor.getPathToDatabaseIniConfig())

        val res: Boolean
        try {
            val storageDir = File(storage.path)
            if (storage.isNew) {
                log(getString(R.string.log_start_storage_creating) + storage.path, ILogger.Types.DEBUG)
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
                res = databaseConfig?.saveDefault() ?: false
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
                res = databaseConfig?.load() ?: false
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
                && (!PINManager.isRequestPINCode(getContext())
                    || PINManager.isRequestPINCode(getContext()) && node != null
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
        logOperStart(getContext(), Objs.STORAGE, Opers.LOAD)

        viewModelScope.launch {
            // перед загрузкой
            val stringResId = if (isDecrypt) R.string.task_storage_decrypting else R.string.task_storage_loading
            viewEvent.postValue(ViewModelEvent(Constants.ViewEvents.TaskStarted, stringResId))

            // непосредственное чтение структуры хранилища
            val result = readStorage(isDecrypt, isFavoritesOnly)

            // после загрузки
            viewEvent.postValue(ViewModelEvent(Constants.ViewEvents.TaskFinished))
            if (result) {
                // устанавливаем глобальную переменную
//                App.IsLoadedFavoritesOnly = isFavoritesOnly

                val mes: String = getString(
                    when {
                        isFavoritesOnly -> R.string.log_storage_favor_loaded_mask
                        isDecrypt -> R.string.log_storage_loaded_decrypted_mask
                        else -> R.string.log_storage_loaded_mask
                    }
                )
                LogManager.log(getContext(), String.format(mes, getStorageName()), Toast.LENGTH_SHORT)
            } else {
                LogManager.log(
                    getContext(), getString(R.string.log_failed_storage_load) + getStoragePath(),
                    ILogger.Types.WARNING, Toast.LENGTH_LONG
                )
            }

            val params = ReadDecryptStorageState(
                isDecrypt = isDecrypt,
                isFavoritesOnly = isFavoritesOnly,
                isOpenLastNode = isOpenLastNode,
                result = result
            )
            // инициализация контролов
            viewEvent.postValue(ViewModelEvent(Constants.ViewEvents.InitGUI, params))
            // действия после загрузки хранилища
            updateStorageState(Constants.StorageEvents.Loaded, result)
        }
    }

    /**
     * Загрузка хранилища из файла mytetra.xml.
     * @param isDecrypt Расшифровывать ли ветки
     * @param isFavorite Загружать ли только избранные записи
     * @return
     */
    suspend fun readStorage(isDecrypt: Boolean, isFavorite: Boolean): Boolean {
        var res = false
        val file = File(storageInteractor.getPathToMyTetraXml())
        if (!file.exists()) {
            LogManager.log(getContext(), getString(R.string.log_file_is_absent) + StorageInteractor.MYTETRA_XML_FILE_NAME, ILogger.Types.ERROR)
            return false
        }
        // получаем id избранных записей из настроек
        FavoritesManager.load(getContext())
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            res = withContext(Dispatchers.IO) {
                val fis = FileInputStream(file)
                xmlLoader.parse(getContext(), fis, isDecrypt, isFavorite)
            }

//            if (BuildConfig.DEBUG) {
//                TestData.addNodes(mInstance.mRootNodesList, 100, 100);
//            }

            // удаление не найденных записей из избранного
            FavoritesManager.check()
            // загрузка ветки для быстрой вставки
            updateQuicklyNode()
        } catch (ex: java.lang.Exception) {
            LogManager.log(getContext(), ex)
            Message.showSnackMoreInLogs(getContext(), R.id.layout_coordinator)
        }
        return res
    }

    //endregion Read

    //region Decrypt

    /**
     * Непосредственная расшифровка уже загруженного хранилища.
     */
    private fun doDecryptStorage(node: TetroidNode?) {
        logOperStart(getContext(), Objs.STORAGE, Opers.DECRYPT)

        viewModelScope.launch {
            // перед расшифровкой
            updateViewState(Constants.ViewEvents.TaskStarted)

            // непосредственная расшифровка
            val res = cryptInteractor.decryptStorage(getContext(), false)
            storage.value?.isDecrypted = res

            // после расшифровки
            updateViewState(Constants.ViewEvents.TaskFinished)
            if (res) {
                LogManager.log(getContext(), R.string.log_storage_decrypted, ILogger.Types.INFO, Toast.LENGTH_SHORT)
            } else {
                logDuringOperErrors(getContext(), Objs.STORAGE, Opers.DECRYPT, Toast.LENGTH_LONG)
            }
            // действия после расшифровки хранилища
            afterStorageDecrypted(node)
        }
    }

    open fun afterStorageDecrypted(node: TetroidNode?) {
        updateStorageState(Constants.StorageEvents.Decrypted, node)
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
                if (PassManager.checkMiddlePassHash(middlePassHash)) {
                    cryptInteractor.initCryptPass(middlePassHash!!, true)
                    // запрос ПИН-кода
                    PINManager.askPINCode(getContext(), isNodeOpening) {
                        loadOrDecryptStorage(node, true, isOnlyFavorites, isHandleReceivedIntent)
                    }
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
                updateStorageState(Constants.StorageEvents.EmptyPassCheck, params)
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
            updateStorageState(Constants.StorageEvents.AskPinCode, params)
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
        updateStorageState(Constants.StorageEvents.AskPassword, params)
    }

    fun getPassInputHandler(params: AskPasswordParams) = object : IPassInputResult {
        override fun applyPass(pass: String, node: TetroidNode) {
            // подтверждение введенного пароля
            PassManager.checkPass(getContext(), pass, { res: Boolean ->
                if (res) {
                    PassManager.initPass(getContext(), pass)
                    //
                    updateStorageState(Constants.StorageEvents.AskPinCode, params)
                } else {
                    // повторяем запрос
                    askPassword(node, params.isNodeOpening, params.isOnlyFavorites, params.isOpenLastNode)
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
            if (PINManager.isRequestPINCode(getContext())) {
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
            if (PINManager.isRequestPINCode(getContext())) {
                decryptStorage(FavoritesManager.FAVORITES_NODE, true, isLoadFavoritesOnly(), false)
            } else {
                askPassword(FavoritesManager.FAVORITES_NODE, true, isLoadFavoritesOnly(), false)
            }
            return true
        }
        return false
    }

    fun setIsPINNeedToEnter() {
        isPINNeedEnter = true
    }

    fun clearSavedPass() {
        PassManager.clearSavedPass(getContext())
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
                    updateStorageState(Constants.StorageEvents.AskBeforeSyncOnExit, true)

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
                updateStorageState(Constants.StorageEvents.AskAfterSyncOnInit, true)
            }
        } else {
            log(getString(R.string.log_sync_failed), true)
            if (isLoadStorageAfterSync) {
                updateStorageState(Constants.StorageEvents.AskAfterSyncOnInit, false)
            }
        }
        this.isLoadStorageAfterSync = false
    }

    //endregion Sync

    //region Records

    fun getRecord(id: String?): TetroidRecord? = recordsInteractor.getRecord(id)

    open fun editRecordFields(record: TetroidRecord, name: String, tags: String, author: String, url: String, node: TetroidNode, isFavor: Boolean) {
        viewModelScope.launch {
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

    fun createNode(name: String, trueParentNode: TetroidNode?) = nodesInteractor.createNode(getContext(), name, trueParentNode)

    fun createNodesHierarchy(node: TetroidNode) = nodesInteractor.createNodesHierarchy(node)

    /**
     * Проверка существования зашифрованных веток.
     */
    protected fun checkExistenceCryptedNodes() {
        if (!nodesInteractor.isExistCryptedNodes(true)) {
            doAction(Constants.ObjectEvents.AskForClearStoragePass)
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
    fun parseRecordTags(record: TetroidRecord?, tagsString: String) {
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
    fun deleteRecordTags(record: TetroidRecord?) {
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
            log(getContext(), R.string.log_link_is_empty, ILogger.Types.ERROR, Toast.LENGTH_SHORT)
            return
        }
        updateViewState(Constants.ViewEvents.ShowProgressText, getString(R.string.title_file_downloading))
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
                updateViewState(Constants.ViewEvents.ShowProgress, false)
            }

            override fun onError(ex: Exception) {
                callback?.onError(ex)
                log(getContext(), getString(R.string.log_error_download_file_mask, ex.message ?: ""), Toast.LENGTH_LONG)
                updateViewState(Constants.ViewEvents.ShowProgress, false)
            }
        })
    }

    //endregion Attaches

    //region FileObserver

    /**
     * Обработчик изменения структуры хранилища извне.
     */
    fun startStorageTreeObserver() {
        if (isCheckOutsideChanging()) {
            // запускаем мониторинг, только если хранилище загружено
            if (isLoaded()) {
                this.isStorageChangingHandled = false
                val bundle = Bundle()
                bundle.putInt(FileObserverService.EXTRA_ACTION_ID, FileObserverService.ACTION_START)
//                bundle.putString(FileObserverService.EXTRA_FILE_PATH, StorageManager.getStoragePath() + "/" + DataManager.MYTETRA_XML_FILE_NAME);
                bundle.putString(FileObserverService.EXTRA_FILE_PATH, storageInteractor.getPathToMyTetraXml())
                bundle.putInt(FileObserverService.EXTRA_EVENT_MASK, FileObserver.MODIFY)

                doAction(Constants.ObjectEvents.StartFileObserver, bundle)
            }
        } else {
            doAction(Constants.ObjectEvents.StopFileObserver)
        }
    }

    fun onStorageOutsideChanged() {
        // проверяем, не был ли запущен обработчик второй раз подряд
        if (!isStorageChangingHandled) {
            isStorageChangingHandled = true
            LogManager.log(getContext(), R.string.ask_storage_changed_outside, ILogger.Types.INFO)
            updateStorageState(Constants.StorageEvents.ChangedOutside)
        }
    }

    fun dropIsStorageChangingHandled() {
        isStorageChangingHandled = false
    }
    
    //endregion FileObserver

    //region Other

    fun swapTetroidObjects(list: List<Any>, pos: Int, isUp: Boolean, through: Boolean): Int {
        return dataInteractor.swapTetroidObjects(getContext(), list, pos, isUp, through)
    }

    fun getExternalCacheDir() = getContext().externalCacheDir.toString()

    fun setDefaultStorage() {
        viewModelScope.launch {
            val storage = storagesRepo.getDefaultStorage()
            _storage.postValue(storage)
        }
    }

    fun doAction(action: Constants.ObjectEvents, param: Any? = null) {
        objectAction.postValue(ViewModelEvent(action, param))
    }

    //endregion Other

    /**
     *
     */
    inner class StorageLoadHelper : StorageSettingsViewModel.StorageLoadHelper() {

        override fun isRecordFavorite(id: String?): Boolean {
            // TODO: to Interactor
            return FavoritesManager.isFavorite(id)
        }

        override fun addRecordFavorite(record: TetroidRecord?) {
            // TODO: to Interactor
            FavoritesManager.set(record)
        }

        override fun parseRecordTags(record: TetroidRecord?, tagsString: String) {
            this@StorageViewModel.parseRecordTags(record, tagsString)
        }

        override fun deleteRecordTags(record: TetroidRecord?) {
            this@StorageViewModel.deleteRecordTags(record)
        }

        override fun loadIcon(context: Context, node: TetroidNode) {
            if (node.isNonCryptedOrDecrypted) {
                node.loadIcon(context, storageInteractor.getPathToIcons())
            }
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