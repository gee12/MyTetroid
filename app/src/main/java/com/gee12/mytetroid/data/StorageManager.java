package com.gee12.mytetroid.data;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.PermissionManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TetroidTask2;
import com.gee12.mytetroid.crypt.TetroidCrypter;
import com.gee12.mytetroid.dialogs.AskDialogs;
import com.gee12.mytetroid.dialogs.PassDialogs;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.logs.TetroidLog;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.FileUtils;
import com.gee12.mytetroid.views.StorageChooserDialog;

import java.io.File;

import lib.folderpicker.FolderPicker;

public class StorageManager extends DataManager {

    /**
     *
     */
    public interface IStorageInitCallback extends TetroidTask2.IAsyncTaskCallback {
        void initGUI(boolean res, boolean mIsFavoritesOnly, boolean mIsOpenLastNode);
        boolean taskPreExecute(int sRes);
        void taskPostExecute(boolean isDrawerOpened);
        void afterStorageLoaded(boolean res);
        void afterStorageDecrypted(TetroidNode node);
        void taskStarted(TetroidTask2 task);
    }

    public static final int REQUEST_CODE_OPEN_STORAGE = 1;
    public static final int REQUEST_CODE_CREATE_STORAGE = 2;
    public static final int REQUEST_CODE_SYNC_STORAGE = 3;

    public static final int REQUEST_CODE_PERMISSION_WRITE_STORAGE = 1;
    public static final int REQUEST_CODE_PERMISSION_WRITE_TEMP = 2;
    public static final int REQUEST_CODE_PERMISSION_CAMERA = 3;


    protected boolean mIsAlreadyTryDecrypt;
    protected boolean mIsLoadStorageAfterSync;
    protected boolean mIsPINNeedEnter;
    private IStorageInitCallback mStorageInitCallback;


    public static void setIsPINNeedToEnter() {
        if (Instance != null) {
            Instance.mIsPINNeedEnter = true;
        }
    }

    public static void resetIsPINNeedToEnter() {
        if (Instance != null) {
            Instance.mIsPINNeedEnter = false;
        }
    }

    public static boolean isPINNeedToEnter() {
        return (Instance != null) && Instance.mIsPINNeedEnter;
    }

//    /**
//     * Первоначальная инициализация.
//     * @param storageInitCallback
//     */
//    public static void setStorageCallback(IStorageInitCallback storageInitCallback) {
//        getInstance().mStorageInitCallback = storageInitCallback;
//    }

    protected static IStorageInitCallback getStorageInitCallback() {
        return getInstance().mStorageInitCallback;
    }

    /**
     * Загрузка параметров из файла database.ini и инициализация переменных.
     * @param storagePath
     * @return
     */
    public static boolean initOrCreateStorage(Context context, String storagePath, boolean isNew) {
        DataManager inst = getInstance();
        inst.mStoragePath = storagePath;
        ILogger logger = LogManager.createLogger(context);
        inst.mDatabaseConfig = new DatabaseConfig(logger,storagePath + SEPAR + DATABASE_INI_FILE_NAME);
        inst.mCrypter = new TetroidCrypter(logger, inst.mXml, Instance);
        boolean res;
        try {
            File storageDir = new File(storagePath);
            if (isNew) {
                LogManager.log(context, context.getString(R.string.log_start_storage_creating) + storagePath, ILogger.Types.DEBUG);
                if (storageDir.exists()) {
                    // очищаем каталог
                    LogManager.log(context, R.string.log_clear_storage_dir, ILogger.Types.INFO);
                    FileUtils.clearDir(storageDir);
                    // проверяем, пуст ли каталог
                } else {
                    LogManager.log(context, R.string.log_dir_is_missing, ILogger.Types.ERROR);
                    return false;
                }
                // сохраняем новый database.ini
                res = inst.mDatabaseConfig.saveDefault();
                // создаем каталог base
                File baseDir = new File(storagePath, BASE_FOLDER_NAME);
                if (!baseDir.mkdir()) {
                    return false;
                }
                // добавляем корневую ветку
                inst.mXml.init();
                inst.mXml.mIsStorageLoaded = true;
            }  else {
                // загружаем database.ini
                res = inst.mDatabaseConfig.load();
            }
            inst.mStorageName = storageDir.getName();
        } catch (Exception ex) {
            LogManager.log(context, ex);
            return false;
        }
        inst.mIsStorageInited = res;
        return res;
    }

    /**
     * Начало загрузки хранилища.
     *
     * @isLoadLastForced Загружать по сохраненнному пути, даже если не установлена опция isLoadLastStoragePath
     */
    public static void startInitStorage(Activity activity, IStorageInitCallback storageInitCallback,
                                        boolean isLoadLastForced) {
        startInitStorage(activity, storageInitCallback, isLoadLastForced, true);
    }

    public static void startInitStorage(Activity activity, IStorageInitCallback storageInitCallback,
                                        boolean isLoadLastForced, boolean isCheckFavorMode) {
        // сначала проверяем разрешение на запись во внешнюю память
        if (!PermissionManager.checkWriteExtStoragePermission(activity, StorageManager.REQUEST_CODE_PERMISSION_WRITE_STORAGE)) {
            return;
        }

        getInstance().mStorageInitCallback = storageInitCallback;

        String storagePath = SettingsManager.getStoragePath(activity);
        if ((isLoadLastForced || SettingsManager.isLoadLastStoragePath(activity)) && storagePath != null) {
            initOrSyncStorage(activity, storagePath, isCheckFavorMode);
        } else {
            StorageChooserDialog.createDialog(activity, isNew -> StorageManager.showStorageFolderChooser(activity, isNew));
        }
    }

    /**
     * Проверка нужно ли синхронизировать хранилище перед загрузкой.
     *
     * @param storagePath
     */
    public static void initOrSyncStorage(Activity activity, final String storagePath, boolean isCheckFavorMode) {
        if (SettingsManager.isSyncStorage(activity) && SettingsManager.isSyncBeforeInit(activity)) {
            // спрашиваем о необходимости запуска синхронизации, если установлена опция
            if (SettingsManager.isAskBeforeSync(activity)) {
                AskDialogs.showSyncRequestDialog(activity, new Dialogs.IApplyCancelResult() {
                    @Override
                    public void onApply() {
                        getInstance().mIsLoadStorageAfterSync = true;
                        startStorageSync(activity, storagePath);
                    }

                    @Override
                    public void onCancel() {
                        initStorage(activity, storagePath, isCheckFavorMode);
                    }
                });
            } else {
                getInstance().mIsLoadStorageAfterSync = true;
                startStorageSync(activity, storagePath);
            }
        } else {
            initStorage(activity, storagePath, isCheckFavorMode);
        }
    }

    /**
     * Загрузка хранилища по указанному пути.
     *
     * @param storagePath Путь хранилища
     */
    public static boolean initStorage(Context context, String storagePath) {
        return initStorage(context, storagePath, true);
    }

    public static boolean initStorage(Context context, String storagePath, boolean isCheckFavorMode) {
        Instance.mIsAlreadyTryDecrypt = false;

        boolean isFavorMode = false;
        if (isCheckFavorMode) {
            // читаем установленную опцию isLoadFavoritesOnly только при первой загрузке
            isFavorMode = !isLoaded() && SettingsManager.isLoadFavoritesOnly(context)
                    || (isLoaded() && isFavoritesMode());
        }

        boolean res = initOrCreateStorage(context, storagePath, false);
        if (res) {
            LogManager.log(context, context.getString(R.string.log_storage_settings_inited) + storagePath);
            // сохраняем путь к хранилищу
//            if (SettingsManager.isLoadLastStoragePath()) {
            SettingsManager.setStoragePath(context, storagePath);
//            }
            if (isCrypted(context) /*&& !isFavorMode*/) {
                // сначала устанавливаем пароль, а потом загружаем (с расшифровкой)
                Instance.decryptStorage(context, null, false, isFavorMode, true);
            } else {
                // загружаем
                Instance.loadStorage(context, null, false, isFavorMode, true);
            }
        } else {
            LogManager.log(context, context.getString(R.string.log_failed_storage_init) + storagePath,
                    ILogger.Types.ERROR, Toast.LENGTH_LONG);
            /*mDrawerLayout.openDrawer(Gravity.LEFT);*/
            getStorageInitCallback().initGUI(false, isFavorMode, false);
        }
        return res;
    }

    /**
     * Загрузка всех веток, когда загружено только избранное.
     */
    public static void loadAllNodes(Context context) {
        if (isCrypted(context)) {
            // FIXME: не передаем node=FAVORITES_NODE, т.к. тогда хранилище сразу расшифровуется без запроса ПИН-кода
            //  По-идее, нужно остановить null, но сразу расшифровывать хранилище, если до этого уже
            //    вводили ПИН-код (для расшифровки избранной записи)
            //  Т.Е. сохранять признак того, что ПИН-крд уже вводили в этой "сессии"
            Instance.decryptStorage(context, null, false, false, false);
        } else {
            Instance.loadStorage(context, null, false, false, false);
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
     * @param isOpenLastNode  Нужно ли после загрузки открыть ветку, сохраненную в опции getLastNodeId(),
     *                        или ветку с избранным (если именно она передана в node)
     */
    public void decryptStorage(Context context, TetroidNode node, boolean isNodeOpening,
                                       boolean isOnlyFavorites, boolean isOpenLastNode) {
        // устанавливаем признак
        StorageManager.setIsPINNeedToEnter();

        String middlePassHash;
        // пароль уже вводили или он сохранен локально?
        if ((middlePassHash = Instance.mCrypter.getMiddlePassHash()) != null
                || SettingsManager.isSaveMiddlePassHashLocal(context)
                    && (middlePassHash = SettingsManager.getMiddlePassHash(context)) != null) {
            // проверяем
            try {
                if (PassManager.checkMiddlePassHash(middlePassHash)) {
                    initCryptPass(middlePassHash, true);
                    // запрос ПИН-кода
                    PINManager.askPINCode(context, isNodeOpening, () -> {
                        loadStorage(context, node, true, isOnlyFavorites, isOpenLastNode);
                    });

                } else if (isNodeOpening) {
                    // спрашиваем пароль
                    askPassword(context, node, isNodeOpening, isOnlyFavorites, isOpenLastNode);
                } else {
                    LogManager.log(context, R.string.log_wrong_saved_pass, Toast.LENGTH_LONG);
                    if (!getInstance().mIsAlreadyTryDecrypt) {
                        getInstance().mIsAlreadyTryDecrypt = true;
                        loadStorage(context, node, false, isOnlyFavorites, isOpenLastNode);
                    }
                }
            } catch (DatabaseConfig.EmptyFieldException ex) {
                // если поля в INI-файле для проверки пустые
                LogManager.log(context, ex);
                // спрашиваем "continue anyway?"
                final String passHash = middlePassHash;
                PassDialogs.showEmptyPassCheckingFieldDialog(context, ex.getFieldName(), new Dialogs.IApplyCancelResult() {
                            @Override
                            public void onApply() {
                                initCryptPass(passHash, true);
                                // запрос ПИН-кода
                                PINManager.askPINCode(context, isNodeOpening, () -> {
                                    loadStorage(context, node, true, isOnlyFavorites, isOpenLastNode);
                                });
                            }

                            @Override
                            public void onCancel() {
                                if (!isNodeOpening) {
                                    // загружаем хранилище без пароля
                                    loadStorage(context, node, false, isOnlyFavorites, isOpenLastNode);
                                }
                            }
                        }
                );
            }
        } else if (SettingsManager.getWhenAskPass(context).equals(context.getString(R.string.pref_when_ask_password_on_start))
                || isNodeOpening) {
            // если пароль не сохранен, то спрашиваем его, когда также:
            //      * если нужно расшифровывать хранилище сразу на старте
            //      * если функция вызвана во время открытия зашифрованной ветки
            //      * ??? если мы не вызвали загрузку всех веток
            askPassword(context, node, isNodeOpening, isOnlyFavorites, isOpenLastNode);
        } else {
            // тогда просто загружаем хранилище без расшифровки, если:
            //      * не сохранен пароль
            //      * пароль не нужно спрашивать на старте
            //      * функция не вызвана во время открытия зашифрованной ветки
            loadStorage(context, node, false, isOnlyFavorites, isOpenLastNode);
        }
    }

    /**
     * Отображения запроса пароля от хранилища.
     *
     * @param node            Зашифрованная ветка, которую нужно открыть после засшифровки.
     * @param isNodeOpening   Вызвана ли функция при попытке открытия зашифрованной ветки
     * @param isOnlyFavorites Нужно ли загружать только избранные записи
     * @param isOpenLastNode  Нужно ли после загрузки открыть ветку, сохраненную в опции getLastNodeId()
     *                        или ветку с избранным (если именно она передана в node)
     */
    public void askPassword(Context context, final TetroidNode node, boolean isNodeOpening,
                                    boolean isOnlyFavorites, boolean isOpenLastNode) {
        LogManager.log(context, R.string.log_show_pass_dialog);
        // выводим окно с запросом пароля в асинхронном режиме
        PassDialogs.showPassEnterDialog(context, node, false, new PassDialogs.IPassInputResult() {
            @Override
            public void applyPass(final String pass, TetroidNode node) {
                // подтверждение введенного пароля
                PassManager.checkPass(context, pass, (res) -> {
                    if (res) {
                        PassManager.initPass(context, pass);
                        PINManager.askPINCode(context, isNodeOpening, () -> {
                            loadStorage(context, node, true, isOnlyFavorites, isOpenLastNode);
                        });
                    } else {
                        // повторяем запрос
                        askPassword(context, node, isNodeOpening, isOnlyFavorites, isOpenLastNode);
                    }
                }, R.string.log_pass_is_incorrect);
            }

            @Override
            public void cancelPass() {
                // ---Если при первой загрузке хранилища установлена текущей зашифрованная ветка (node),
                // --- и пароль не сохраняли, то нужно его спросить.
                if (/*!getInstance().mIsAlreadyTryDecrypt
//                        && !StorageManager.isLoaded()
                        && !isNodeOpening
                        && StorageManager.isFavoritesMode()*/
                    node == null) {
                    // ---Но если в первый раз пароль вводить отказались, то при втором отказе ввода
                    // --- просто грузим хранилище как есть (чтобы небыло циклического запроса пароля, если
                    // --- его просто не хотим вводить)
                    getInstance().mIsAlreadyTryDecrypt = true;
                    loadStorage(context, node, false, isOnlyFavorites, isOpenLastNode);
                }
            }
        });
    }

    /**
     * Непосредственная расшифровка (если зашифровано) или чтение данных хранилища.
     *
     * @param node            Зашифрованная ветка, которую нужно открыть после засшифровки.
     * @param isDecrypt       Нужно ли вызвать процесс расшифровки хранилища.
     * @param isOnlyFavorites Нужно ли загружать только избранные записи
     * @param isOpenLastNode  Нужно ли после загрузки открыть ветку, сохраненную в опции getLastNodeId()
     *                        или ветку с избранным (если именно она передана в node)
     */
    public void loadStorage(Context context, TetroidNode node, boolean isDecrypt, boolean isOnlyFavorites, boolean isOpenLastNode) {
        // расшифровуем хранилище только в том случаем, если:
        //  1) не используем проверку ПИН-кода
        //  2) используем проверку ПИН-кода, при этом расшифровуем с открытием конкретной <b>зашифрованной</b> ветки
        //   (или ветки Избранное)
        isDecrypt = isDecrypt
                && (!PINManager.isRequestPINCode(context)
                || PINManager.isRequestPINCode(context) && node != null
                && (node.isCrypted() || node.equals(FavoritesManager.FAVORITES_NODE)));
        if (isDecrypt && isNodesExist()) {
            // расшифровываем уже загруженное хранилище
            getStorageInitCallback().taskStarted(
                    new DecryptStorageTask(getStorageInitCallback(), context, node)
                            .run());
        } else {
            // загружаем хранилище впервые, с расшифровкой
            TetroidLog.logOperStart(context, TetroidLog.Objs.STORAGE, TetroidLog.Opers.LOAD);
            getStorageInitCallback().taskStarted(
                    new ReadStorageTask(getStorageInitCallback(), context, isDecrypt, isOnlyFavorites, isOpenLastNode)
                            .run());
        }
    }

    /**
     * Создание нового хранилища в указанном расположении.
     *
     * @param storagePath //     * @param checkDirIsEmpty
     */
    public static boolean createStorage(Context context, String storagePath/*, boolean checkDirIsEmpty*/) {
        boolean res = (initOrCreateStorage(context, storagePath, true));
        if (res) {
            // сохраняем путь к хранилищу
            SettingsManager.setStoragePath(context, storagePath);
            LogManager.log(context, context.getString(R.string.log_storage_created), ILogger.Types.INFO, Toast.LENGTH_SHORT);
//            TetroidLog.logOperRes(context, TetroidLog.Objs.STORAGE, TetroidLog.Opers.CREATE, "", Toast.LENGTH_SHORT);
        } else {
//            LogManager.log(getString(R.string.log_failed_storage_create) + mStoragePath, LogManager.Types.ERROR, Toast.LENGTH_LONG);
            TetroidLog.logOperErrorMore(context, TetroidLog.Objs.STORAGE, TetroidLog.Opers.CREATE, Toast.LENGTH_LONG);
        }
        return res;
    }

    public static boolean onNodeDecrypt(Context context, TetroidNode node) {
        if (!node.isNonCryptedOrDecrypted()) {
            if (PINManager.isRequestPINCode(context)) {

                // TODO: сначала просто проверяем пароль
                //  затем спрашиваем ПИН,
                //  а потом уже расшифровываем (!)

                //  Т.е. опять все засунуть в decryptStorage() (?)
                /*PINManager.askPINCode(this, true, () -> {
                    // расшифровываем хранилище
                    decryptStorage(node, true, false, false);
                    showNode(node);
                });*/
                Instance.decryptStorage(context, node, true, false, false);
            } else {
                Instance.askPassword(context, node, true, false, false);
            }
            // выходим, т.к. запрос пароля будет в асинхронном режиме
            return true;
        }
        return false;
    }

    public static boolean onRecordDecrypt(Context context, TetroidRecord record) {
        if (record.isFavorite() && !record.isNonCryptedOrDecrypted()) {
            // запрос пароля в асинхронном режиме
            if (PINManager.isRequestPINCode(context)) {
                Instance.decryptStorage(context, FavoritesManager.FAVORITES_NODE, true,
                        SettingsManager.isLoadFavoritesOnly(context), false);
            } else {
                Instance.askPassword(context, FavoritesManager.FAVORITES_NODE, true,
                        SettingsManager.isLoadFavoritesOnly(context), false);
            }
            return true;
        }
        return false;
    }

    /**
     * Открытие активности для первоначального выбора пути хранилища в файловой системе.
     */
    public static void showStorageFolderChooser(Activity activity, boolean isNew) {
        Intent intent = new Intent(activity, FolderPicker.class);
        intent.putExtra(FolderPicker.EXTRA_TITLE, activity.getString(R.string.title_storage_folder));
        intent.putExtra(FolderPicker.EXTRA_LOCATION, SettingsManager.getStoragePath(activity));
        activity.startActivityForResult(intent, (isNew) ? REQUEST_CODE_CREATE_STORAGE : REQUEST_CODE_OPEN_STORAGE);
    }

    /**
     * Отправление запроса на синхронизацию стороннему приложению.
     *
     * @param storagePath
     */
    public static void startStorageSync(Activity activity, String storagePath) {
        String command = SettingsManager.getSyncCommand(activity);
        Intent intent = SyncManager.createCommandSender(activity, storagePath, command);

        LogManager.log(activity, activity.getString(R.string.log_start_storage_sync) + command);
        if (!SettingsManager.isNotRememberSyncApp(activity)) {
            // использовать стандартный механизм запоминания используемого приложения
            activity.startActivityForResult(intent, REQUEST_CODE_SYNC_STORAGE);
        } else { // или спрашивать постоянно
            activity.startActivityForResult(Intent.createChooser(intent,
                    activity.getString(R.string.title_choose_sync_app)), REQUEST_CODE_SYNC_STORAGE);
        }
    }

    /**
     * Задание (параллельный поток), в котором выполняется загрузка хранилища.
     */
    private static class ReadStorageTask extends TetroidTask2<Void,Void,Boolean> {

        boolean mIsDecrypt;
        boolean mIsFavoritesOnly;
        boolean mIsOpenLastNode;

        ReadStorageTask (IStorageInitCallback callback, Context context,
                         boolean isDecrypt, boolean isFavorites, boolean isOpenLastNode) {
            super(callback, context);
            this.mIsDecrypt = isDecrypt;
            this.mIsFavoritesOnly = isFavorites;
            this.mIsOpenLastNode = isOpenLastNode;
        }

        @Override
        protected void onPreExecute() {
            getStorageInitCallback().taskPreExecute(R.string.task_storage_loading);
        }

        @Override
        protected Boolean doInBackground(Void... values) {
            return Instance.readStorage(mContext, mIsDecrypt, mIsFavoritesOnly);
        }

        @Override
        protected void onPostExecute(Boolean res) {
            getStorageInitCallback().taskPostExecute(true);
            if (res) {
                // устанавливаем глобальную переменную
                App.IsLoadedFavoritesOnly = mIsFavoritesOnly;
                String mes = mContext.getString((mIsFavoritesOnly)
                        ? R.string.log_storage_favor_loaded_mask
                        : (mIsDecrypt) ? R.string.log_storage_loaded_decrypted_mask
                        : R.string.log_storage_loaded_mask);
                LogManager.log(mContext, String.format(mes, StorageManager.getStorageName()), Toast.LENGTH_SHORT);
            } else {
                LogManager.log(mContext, mContext.getString(R.string.log_failed_storage_load) + StorageManager.getStoragePath(),
                        ILogger.Types.WARNING, Toast.LENGTH_LONG);
            }
            // инициализация контролов
            getStorageInitCallback().initGUI(res, mIsFavoritesOnly, mIsOpenLastNode);
            // действия после загрузки хранилища
            getStorageInitCallback().afterStorageLoaded(res);
        }

        private IStorageInitCallback getStorageInitCallback() {
            return (IStorageInitCallback) mTaskCallback;
        }
    }

    /**
     * Задание, в котором выполняется расшифровка уже загруженного хранилища.
     */
    private static class DecryptStorageTask extends TetroidTask2<Void,Void,Boolean> {

        boolean mIsDrawerOpened;
        TetroidNode mNode;

        DecryptStorageTask(IStorageInitCallback callback, Context context, TetroidNode node) {
            super(callback, context);
            this.mNode = node;
        }

        @Override
        protected void onPreExecute() {
            this.mIsDrawerOpened = getStorageInitCallback().taskPreExecute(R.string.task_storage_decrypting);
            TetroidLog.logOperStart(mContext, TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT);
        }

        @Override
        protected Boolean doInBackground(Void... values) {
            return Instance.decryptStorage(mContext, false);
        }

        @Override
        protected void onPostExecute(Boolean res) {
            getStorageInitCallback().taskPostExecute(mIsDrawerOpened);
            if (res) {
                LogManager.log(mContext, R.string.log_storage_decrypted, ILogger.Types.INFO, Toast.LENGTH_SHORT);
            } else {
                TetroidLog.logDuringOperErrors(mContext, TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT, Toast.LENGTH_LONG);
            }
            getStorageInitCallback().afterStorageDecrypted(mNode);
        }

        private IStorageInitCallback getStorageInitCallback() {
            return (IStorageInitCallback) mTaskCallback;
        }
    }

}
