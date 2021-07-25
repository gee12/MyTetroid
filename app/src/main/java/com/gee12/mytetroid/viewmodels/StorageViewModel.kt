package com.gee12.mytetroid.viewmodels

//import com.gee12.mytetroid.data.StorageManager.DecryptStorageTask
//import com.gee12.mytetroid.data.StorageManager.ReadStorageTask
import android.app.Activity
import android.app.Application
import android.content.Context
import android.text.TextUtils
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.gee12.htmlwysiwygeditor.Dialogs
import com.gee12.htmlwysiwygeditor.Dialogs.IApplyCancelDismissResult
import com.gee12.htmlwysiwygeditor.Dialogs.IApplyCancelResult
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.data.ini.DatabaseConfig.EmptyFieldException
import com.gee12.mytetroid.data.xml.IStorageLoadHelper
import com.gee12.mytetroid.interactor.DataInteractor
import com.gee12.mytetroid.interactor.EncryptionInteractor
import com.gee12.mytetroid.interactor.InteractionInteractor
import com.gee12.mytetroid.interactor.SyncInteractor
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.logs.TetroidLog
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.model.TetroidTag
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.utils.FileUtils
import com.gee12.mytetroid.views.Message
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
class StorageViewModel(
    app: Application,
    private val mStoragesRepo: StoragesRepo,
    private val mDataInteractor: DataInteractor,
    private val mInteractionInteractor: InteractionInteractor,
    private val mSyncInteractor: SyncInteractor
) : StorageSettingsViewModel(app, mStoragesRepo) {

    var mIsCheckFavorMode = true
    var mIsAlreadyTryDecrypt = false
    var mIsLoadStorageAfterSync = false
    var mIsPINNeedEnter = false

    val mLogger = LogManager.createLogger(getContext())

    var mDatabaseConfig: DatabaseConfig? = null
        protected set
    var mXml = TetroidXml(StorageLoadHelper())
        protected set
//    var mCrypter = TetroidCrypter(logger, mXml.loadHelper, cryptRepo)
//        protected set
    var mCryptRepo = EncryptionInteractor(mXml, mLogger)

    //region Init

    /**
     * Запуск первичной инициализации хранилища по-умолчанию с указанием флага isCheckFavorMode
     * @param isCheckFavorMode Стоит ли проверять необходимость загрузки только избранных записей.
     *                          Если отключено, то всегда загружать хранилище полностью.
     */
    fun startInitStorage(isLoadLastForced: Boolean, isCheckFavorMode: Boolean) {
        this.mIsCheckFavorMode = isCheckFavorMode
//        if (storagePath == null) {
////            StorageManager.startInitStorage(this, this, isLoadLastForced, isCheckFavorMode);
//            startInitStorage(this, this, isLoadLastForced, isCheckFavorMode)
//        } else {
////            StorageManager.initOrSyncStorage(this, storagePath, isCheckFavorMode);
//            initOrSyncStorage(storagePath, isCheckFavorMode)
//        }
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
    fun startInitStorage(storage: TetroidStorage?) {
        if (storage != null) {
            SettingsManager.setLastStorageId(getContext(), storage.id);

            // сначала проверяем разрешение на запись во внешнюю память
            stateEvent.postValue(ViewModelEvent(Constants.StorageEvents.PermissionCheck))
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
    fun initOrSyncStorage() {
        getSyncProfile()?.let {
            if (it.isEnabled && it.isSyncBeforeInit) {
                // устанавливаем в true, чтобы после окончания синхронизации запустить загрузку хранилища
//                this.mIsLoadStorageAfterSync = false
                this.mIsLoadStorageAfterSync = true

                if (it.isAskBeforeSyncOnInit) {
                    stateEvent.postValue(ViewModelEvent(Constants.StorageEvents.AskBeforeSyncOnInit, storage.value))
                } else {
                    syncStorage()
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
        this.mIsAlreadyTryDecrypt = false
//        this.mIsLoadStorageAfterSync = false

        var isFavorMode = false;
        if (mIsCheckFavorMode) {
            // читаем установленную опцию isLoadFavoritesOnly только при первой загрузке
            isFavorMode = !isLoaded() && isLoadFavoritesOnly()
                    || (isLoaded() && isInFavoritesMode());
        }
        // уже воспользовались, сбрасываем
        this.mIsCheckFavorMode = true

        val res = initOrCreateStorage(storage.value!!)
        if (res) {
            log(getString(R.string.log_storage_settings_inited) + getStoragePath())

            // сохраняем путь к хранилищу
            //SettingsManager.setStoragePath(context, storagePath);

            if (isCrypted()) {
                // сначала устанавливаем пароль, а потом загружаем (с расшифровкой)
                decryptStorage(null, false, isFavorMode, true)
            } else {
                // загружаем
                loadOrDecryptStorage(null, false, isFavorMode, true)
            }
        } else {
            logError(getString(R.string.log_failed_storage_init) + getStoragePath(), true)
//            Message.showSnackMoreInLogs(context, R.id.layout_coordinator)

            /*mDrawerLayout.openDrawer(Gravity.LEFT);*/
//            StorageManager.getStorageInitCallback().initGUI(false, isFavorMode, false)
            stateEvent.postValue(ViewModelEvent(Constants.StorageEvents.InitFailed, isFavorMode))
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

        this.mDatabaseConfig = DatabaseConfig(mLogger, storage.path + SEPAR + DATABASE_INI_FILE_NAME)

        val res: Boolean
        try {
            val storageDir = File(storage.path)
            if (storage.isNew) {
                log(getString(R.string.log_start_storage_creating) + storage.path, ILogger.Types.DEBUG)
                if (storageDir.exists()) {
                    /*// очищаем каталог
                    LogManager.log(context, R.string.log_clear_storage_dir, ILogger.Types.INFO);
                    FileUtils.clearDir(storageDir);*/
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
                res = mDatabaseConfig?.saveDefault() ?: false
                // создаем каталог base
                val baseDir = File(storage.path, BASE_FOLDER_NAME)
                if (!baseDir.mkdir()) {
                    return false
                }
                // добавляем корневую ветку
                mXml.init()
//                inst.mXml.mIsStorageLoaded = true
                storage.isLoaded = true
                // создаем Favorites
                FavoritesManager.create()
            } else {
                // загружаем database.ini
                res = mDatabaseConfig?.load() ?: false
            }
//            inst.mStorageName = storageDir.name
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
            DecryptStorageTask(node)
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
    fun startReadStorage(isDecrypt: Boolean, isFavoritesOnly: Boolean, isOpenLastNode: Boolean) {
        TetroidLog.logOperStart(getContext(), TetroidLog.Objs.STORAGE, TetroidLog.Opers.LOAD)

        viewModelScope.launch {
            val params = ReadDecryptStorageState(
                isDecrypt = isDecrypt,
                isFavoritesOnly = isFavoritesOnly,
                isOpenLastNode = isOpenLastNode
            )
            // перед загрузкой
            readStorageStateEvent.postValue(ViewModelEvent(Constants.ActivityEvents.TaskPreExecute, params))

            // непосредственное чтение структуры хранилища
            val res = readStorage(isDecrypt, isFavoritesOnly)
            params.result = res

            // после загрузки
            readStorageStateEvent.postValue(ViewModelEvent(Constants.ActivityEvents.TaskPostExecute, params))
            if (res) {
                // устанавливаем глобальную переменную
                App.IsLoadedFavoritesOnly = isFavoritesOnly

                val mes: String = getString(
                    when {
                        isFavoritesOnly -> R.string.log_storage_favor_loaded_mask
                        isDecrypt -> R.string.log_storage_loaded_decrypted_mask
                        else -> R.string.log_storage_loaded_mask
                    }
                )
                LogManager.log(getContext(), String.format(mes, StorageManager.getStorageName()), Toast.LENGTH_SHORT)
            } else {
                LogManager.log(
                    getContext(), getString(R.string.log_failed_storage_load) + StorageManager.getStoragePath(),
                    ILogger.Types.WARNING, Toast.LENGTH_LONG
                )
            }
            // инициализация контролов
            readStorageStateEvent.postValue(ViewModelEvent(Constants.ActivityEvents.InitGUI, params))
            // действия после загрузки хранилища
            stateEvent.postValue(ViewModelEvent(Constants.StorageEvents.Loaded, res))
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
        val file = File(getPathToMyTetraXml())
        if (!file.exists()) {
            LogManager.log(getContext(), getString(R.string.log_file_is_absent) + MYTETRA_XML_FILE_NAME, ILogger.Types.ERROR)
            return false
        }
        // получаем id избранных записей из настроек
        FavoritesManager.load(getContext())
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            res = withContext(Dispatchers.IO) {
                val fis = FileInputStream(file)
                mXml.parse(getContext(), fis, isDecrypt, isFavorite)
            }

//            if (BuildConfig.DEBUG) {
//                TestData.addNodes(mInstance.mRootNodesList, 100, 100);
//            }

            // удаление не найденных записей из избранного
            FavoritesManager.check()
            // загрузка ветки для быстрой вставки
            NodesManager.updateQuicklyNode(getContext())
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
    fun DecryptStorageTask(node: TetroidNode) {
        TetroidLog.logOperStart(getContext(), TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT)

        viewModelScope.launch {
            val params = ReadDecryptStorageState(
                isDecrypt = true,
                node = node
            )
            // перед расшифровкой
            readStorageStateEvent.postValue(ViewModelEvent(Constants.ActivityEvents.TaskPreExecute, params))

            // непосредственная расшифровка
            val res = mCryptRepo.decryptStorage(getContext(), false)
            storage.value?.isDecrypted = res
            params.result = res

            // после расшифровки
            readStorageStateEvent.postValue(ViewModelEvent(Constants.ActivityEvents.TaskPostExecute, params))
            if (res) {
                LogManager.log(getContext(), R.string.log_storage_decrypted, ILogger.Types.INFO, Toast.LENGTH_SHORT)
            } else {
                TetroidLog.logDuringOperErrors(getContext(), TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT, Toast.LENGTH_LONG)
            }
            // действия после расшифровки хранилища
            stateEvent.postValue(ViewModelEvent(Constants.StorageEvents.Decrypted, node))
        }
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
        StorageManager.setIsPINNeedToEnter()
        var middlePassHash: String?
        // пароль уже вводили или он сохранен локально?
        if (getCrypter().middlePassHash.also { middlePassHash = it } != null
            || isSaveMiddlePassLocal() && getMiddlePassHash().also { middlePassHash = it } != null) {
            // проверяем
            try {
                if (PassManager.checkMiddlePassHash(middlePassHash)) {
                    mCryptRepo.initCryptPass(middlePassHash, true)
                    // запрос ПИН-кода
                    PINManager.askPINCode(getContext(), isNodeOpening) {
                        loadOrDecryptStorage(node, true, isOnlyFavorites, isHandleReceivedIntent)
                    }
                } else if (isNodeOpening) {
                    // спрашиваем пароль
                    askPassword(node, isNodeOpening, isOnlyFavorites, isHandleReceivedIntent)
                } else {
                    log(R.string.log_wrong_saved_pass, true)
                    if (!mIsAlreadyTryDecrypt) {
                        mIsAlreadyTryDecrypt = true
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
                stateEvent.postValue(ViewModelEvent(Constants.StorageEvents.EmptyPassCheck, params))
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
            mCryptRepo.initCryptPass(params.passHash, true)
            // запрос ПИН-кода
            stateEvent.postValue(ViewModelEvent(Constants.StorageEvents.AskPinCode, params))
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
        //
        val params = AskPasswordParams(node, isNodeOpening, isOnlyFavorites, isOpenLastNode, null, null)
        stateEvent.postValue(ViewModelEvent(Constants.StorageEvents.AskPassword, params))
    }

    fun getPassInputHandler(params: AskPasswordParams) = object : IPassInputResult {
        override fun applyPass(pass: String, node: TetroidNode) {
            // подтверждение введенного пароля
            PassManager.checkPass(getContext(), pass, { res: Boolean ->
                if (res) {
                    PassManager.initPass(getContext(), pass)
                    //
                    stateEvent.postValue(ViewModelEvent(Constants.StorageEvents.AskPinCode, params))
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
                mIsAlreadyTryDecrypt = true
                loadOrDecryptStorage(params.node, false, params.isOnlyFavorites, params.isOpenLastNode)
            }
        }
    }

    fun getPinCodeInputHandler(params: AskPasswordParams) = Dialogs.IApplyResult {
            loadOrDecryptStorage(params.node, true, params.isOnlyFavorites, params.isOpenLastNode)
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
        mIsPINNeedEnter = true
    }

    //endregion Decrypt

    //region Sync

    fun syncStorage(isLoadStorageAfterSync: Boolean = true) {
        this.mIsLoadStorageAfterSync = isLoadStorageAfterSync
        startStorageSync() {
            initStorageAndLoad()
        }
    }

    /**
     * Отправка запроса на синхронизацию стороннему приложению перед загрузкой хранилища.
     * @param activity
     * @param callback
     */
    fun startStorageSync(activity: Activity, callback: Runnable?) {
        val res = mSyncInteractor.startStorageSync(activity, getStoragePath(), Constants.REQUEST_CODE_SYNC_STORAGE)
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
                    stateEvent.postValue(ViewModelEvent(Constants.StorageEvents.AskBeforeSyncOnExit, true))

                    // TODO: перенести в MainActivity
                    AskDialogs.showSyncRequestDialog(activity, object : IApplyCancelDismissResult {
                        override fun onApply() {
                            startStorageSync(activity, getStoragePath(), null)
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
                    startStorageSync(activity, getStoragePath(), null)
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
//        val storagePath: String = SettingsManager.getStoragePath(this)
        if (res) {
            log(R.string.log_sync_successful, true)
            if (mIsLoadStorageAfterSync)
//                initStorage(storagePath)
                initStorageAndLoad()
            else {
//                AskDialogs.showSyncDoneDialog(this, true, IApplyResult {
//                    initStorage(storagePath)
//                })
                stateEvent.postValue(ViewModelEvent(Constants.StorageEvents.AskAfterSyncOnInit, true))
            }
        } else {
            log(getString(R.string.log_sync_failed), true)
            if (mIsLoadStorageAfterSync) {
//                AskDialogs.showSyncDoneDialog(this, false, IApplyResult {
//                    initStorage(storagePath)
//                })
                stateEvent.postValue(ViewModelEvent(Constants.StorageEvents.AskAfterSyncOnInit, false))
            }
        }
        this.mIsLoadStorageAfterSync = false
    }

    //endregion Sync

    // region ?

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
                val lowerCaseTagName = tagName.toLowerCase()
                var tag: TetroidTag
                if (mXml.mTagsMap.containsKey(lowerCaseTagName)) {
                    tag = mXml.mTagsMap.get(lowerCaseTagName) ?: continue
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
                    mXml.mTagsMap.put(lowerCaseTagName, tag)
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
                val foundedTag = DataManager.getTag(tag.name)
                if (foundedTag != null) {
                    // удаляем запись из метки
                    foundedTag.records.remove(record)
                    if (foundedTag.records.isEmpty()) {
                        // удаляем саму метку из списка
                        mXml.mTagsMap.remove(tag.name.toLowerCase())
                    }
                }
            }
            record.tags.clear()
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

    //endregion ?

    fun setDefaultStorage() {
        viewModelScope.launch {
            val storage = mStoragesRepo.getDefaultStorage()
            _storage.postValue(storage)
        }
    }
//    = withContext(Dispatchers.IO) {
//        repo.getDefaultStorage()
//    }

    fun isNodesExist(): Boolean = mXml.mRootNodesList != null && !mXml.mRootNodesList.isEmpty()

    fun getCrypter() = mCryptRepo.mCrypter

    fun getPathToMyTetraXml(): String {
        return getStoragePath() + SEPAR + MYTETRA_XML_FILE_NAME
    }

    /**
     *
     */
    inner class StorageLoadHelper : IStorageLoadHelper {
        override fun decryptNode(context: Context, node: TetroidNode?): Boolean {
            return getCrypter().decryptNode(context, node, false, false,
                this, false, false)
        }

        override fun decryptRecord(context: Context, record: TetroidRecord?): Boolean {
            return getCrypter().decryptRecordAndFiles(context, record, false, false)
        }

        override fun isRecordFavorite(id: String?): Boolean {
            return FavoritesManager.isFavorite(id)
        }

        override fun addRecordFavorite(record: TetroidRecord?) {
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
                node.loadIcon(context, getStoragePath() + SEPAR + ICONS_FOLDER_NAME)
            }
        }
    }

    companion object {
        const val ID_SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyz"
        const val QUOTES_PARAM_STRING = "\"\""

        val SEPAR = File.separator
        const val UNIQUE_ID_HALF_LENGTH = 10
        const val PREFIX_DATE_TIME_FORMAT = "yyyyMMddHHmmssSSS"

        const val BASE_FOLDER_NAME = "base"
        const val ICONS_FOLDER_NAME = "icons"
        const val MYTETRA_XML_FILE_NAME = "mytetra.xml"
        const val DATABASE_INI_FILE_NAME = "database.ini"
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