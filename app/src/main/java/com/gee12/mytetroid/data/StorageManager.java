package com.gee12.mytetroid.data;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.PermissionManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TetroidLog;
import com.gee12.mytetroid.TetroidTask2;
import com.gee12.mytetroid.dialogs.AskDialogs;
import com.gee12.mytetroid.dialogs.PassDialogs;
import com.gee12.mytetroid.model.TetroidNode;
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
    private IStorageInitCallback mStorageInitCallback;

    /**
     * Первоначальная инициализация.
     * @param storageInitCallback
     */
    public static void setStorageCallback(IStorageInitCallback storageInitCallback) {
        getInstance().mStorageInitCallback = storageInitCallback;
    }

    protected static IStorageInitCallback getStorageInitCallback() {
        return getInstance().mStorageInitCallback;
    }

    /**
     * Загрузка параметров из файла database.ini и инициализация переменных.
     * @param storagePath
     * @return
     */
    public static boolean initStorage(String storagePath, boolean isNew) {
        DataManager inst = getInstance();
        inst.mStoragePath = storagePath;
        DataManager.DatabaseConfig = new DatabaseConfig(storagePath + SEPAR + DATABASE_INI_FILE_NAME);
        boolean res;
        try {
            File storageDir = new File(storagePath);
            if (isNew) {
                LogManager.log(context.getString(R.string.log_start_storage_creating) + storagePath, LogManager.Types.DEBUG);
                if (storageDir.exists()) {
                    // очищаем каталог
                    LogManager.log(R.string.log_clear_storage_dir, LogManager.Types.INFO);
                    FileUtils.clearDir(storageDir);
                    // проверяем, пуст ли каталог
                } else {
                    LogManager.log(R.string.log_dir_is_missing, LogManager.Types.ERROR);
                    return false;
                }
                // сохраняем новый database.ini
                res = DatabaseConfig.saveDefault();
                // создаем каталог base
                File baseDir = new File(storagePath, BASE_FOLDER_NAME);
                if (!baseDir.mkdir()) {
                    return false;
                }
                // добавляем корневую ветку
                inst.init();
                inst.mIsStorageLoaded = true;
            }  else {
                // загружаем database.ini
                res = DatabaseConfig.load();
            }
            inst.mStorageName = storageDir.getName();
        } catch (Exception ex) {
            LogManager.log(ex);
            return false;
        }
        inst.mIsStorageInited = res;
        return res;
    }

    /**
     *
     * @param activity
     * @param storageInitCallback
     */
    /*public static void initOrShowStorage(Activity activity, IStorageInitCallback storageInitCallback) {
        StorageManager.setStorageCallback(storageInitCallback);
        if (!StorageManager.isLoaded() *//*|| getStorageInitCallback() == null*//*) {
            // загружаем хранилище
            StorageManager.startInitStorage(activity, false);
        } else {
            // инициализация контролов
            getStorageInitCallback().initGUI(true, SettingsManager.isLoadFavoritesOnly(), SettingsManager.isKeepLastNode());
            // действия после загрузки хранилища
            getStorageInitCallback().afterStorageLoaded(true);
        }
    }*/

    /**
     * Начало загрузки хранилища.
     *
     * @isLoadLastForced Загружать по сохраненнному пути, даже если не установлена опция isLoadLastStoragePath
     */
    public static void startInitStorage(Activity activity, boolean isLoadLastForced) {
        // сначала проверяем разрешение на запись во внешнюю память
        if (!PermissionManager.checkWriteExtStoragePermission(activity, StorageManager.REQUEST_CODE_PERMISSION_WRITE_STORAGE)) {
            return;
        }

        String storagePath = SettingsManager.getStoragePath();
        if (storagePath != null && SettingsManager.isLoadLastStoragePath() || isLoadLastForced) {
            initOrSyncStorage(activity, storagePath);
        } else {
            StorageChooserDialog.createDialog(activity, isNew -> StorageManager.showStorageFolderChooser(activity, isNew));
        }
    }

    /**
     * Проверка нужно ли синхронизировать хранилище перед загрузкой.
     *
     * @param storagePath
     */
    public static void initOrSyncStorage(Activity activity, final String storagePath) {
        if (SettingsManager.isSyncStorage() && SettingsManager.isSyncBeforeInit()) {
            // спрашиваем о необходимости запуска синхронизации, если установлена опция
            if (SettingsManager.isAskBeforeSync()) {
                AskDialogs.showSyncRequestDialog(activity, new Dialogs.IApplyCancelResult() {
                    @Override
                    public void onApply() {
                        getInstance().mIsLoadStorageAfterSync = true;
                        startStorageSync(activity, storagePath);
                    }

                    @Override
                    public void onCancel() {
                        initStorage(activity, storagePath);
                    }
                });
            } else {
                getInstance().mIsLoadStorageAfterSync = true;
                startStorageSync(activity, storagePath);
            }
        } else {
            initStorage(activity, storagePath);
        }
    }

    /**
     * Загрузка хранилища по указанному пути.
     *
     * @param storagePath Путь хранилища
     */
    public static boolean initStorage(Context context, String storagePath) {
        getInstance().mIsAlreadyTryDecrypt = false;
        // читаем установленную опцию isLoadFavoritesOnly только при первой загрузке
        boolean isFavorites = !DataManager.isLoaded() && SettingsManager.isLoadFavoritesOnly()
                || (DataManager.isLoaded() && DataManager.isFavoritesMode());

        boolean res = initStorage(storagePath, false);
        if (res) {
            LogManager.log(context.getString(R.string.log_storage_settings_inited) + storagePath);
            /*mDrawerLayout.openDrawer(Gravity.LEFT);*/
            // сохраняем путь к хранилищу
//            if (SettingsManager.isLoadLastStoragePath()) {
            SettingsManager.setStoragePath(storagePath);
//            }
            if (DataManager.isCrypted() /*&& !isFavorites*/) {
                // сначала устанавливаем пароль, а потом загружаем (с расшифровкой)
                //decryptStorage(null);
                decryptStorage(context, null, false, isFavorites, true);
            } else {
                // загружаем
                initStorage(context, null, false, isFavorites, true);
            }
        } else {
            LogManager.log(context.getString(R.string.log_failed_storage_init) + storagePath,
                    LogManager.Types.ERROR, Toast.LENGTH_LONG);
            /*mDrawerLayout.openDrawer(Gravity.LEFT);*/
            getStorageInitCallback().initGUI(false, isFavorites, false);
        }
        return res;
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
    public static void decryptStorage(Context context, TetroidNode node, boolean isNodeOpening,
                                       boolean isOnlyFavorites, boolean isOpenLastNode) {
        String middlePassHash;
        // пароль сохранен локально?
        if (SettingsManager.isSaveMiddlePassHashLocal()
                && (middlePassHash = SettingsManager.getMiddlePassHash()) != null) {
            // проверяем
            try {
                if (PassManager.checkMiddlePassHash(middlePassHash)) {
                    DataManager.initCryptPass(middlePassHash, true);
                    // запрос ПИН-кода
                    PINManager.askPINCode(context, isNodeOpening, () -> {
                        initStorage(context, node, true, isOnlyFavorites, isOpenLastNode);
                    });

                } else if (isNodeOpening) {
                    // спрашиваем пароль
                    askPassword(context, node, isNodeOpening, isOnlyFavorites, isOpenLastNode);
                } else {
                    LogManager.log(R.string.log_wrong_saved_pass, Toast.LENGTH_LONG);
                    if (!getInstance().mIsAlreadyTryDecrypt) {
                        getInstance().mIsAlreadyTryDecrypt = true;
                        initStorage(context, node, false, isOnlyFavorites, isOpenLastNode);
                    }
                }
            } catch (DatabaseConfig.EmptyFieldException ex) {
                // если поля в INI-файле для проверки пустые
                LogManager.log(ex);
                // спрашиваем "continue anyway?"
                PassDialogs.showEmptyPassCheckingFieldDialog(context, ex.getFieldName(), new Dialogs.IApplyCancelResult() {
                            @Override
                            public void onApply() {
                                DataManager.initCryptPass(middlePassHash, true);
                                // запрос ПИН-кода
                                PINManager.askPINCode(context, isNodeOpening, () -> {
                                    initStorage(context, node, true, isOnlyFavorites, isOpenLastNode);
                                });
                            }

                            @Override
                            public void onCancel() {
                                if (!isNodeOpening) {
                                    // загружаем хранилище без пароля
                                    initStorage(context, node, false, isOnlyFavorites, isOpenLastNode);
                                }
                            }
                        }
                );
            }
        } else if (SettingsManager.getWhenAskPass().equals(context.getString(R.string.pref_when_ask_password_on_start))
                || isNodeOpening) {
            // если пароль не сохранен, то спрашиваем его, когда также:
            //      * если нужно расшифровывать хранилище сразу на старте
            //      * если функция вызвана во время открытия зашифрованной ветки
            askPassword(context, node, isNodeOpening, isOnlyFavorites, isOpenLastNode);
        } else {
            // тогда просто загружаем хранилище без расшифровки, если:
            //      * не сохранен пароль
            //      * пароль не нужно спрашивать на старте
            //      * функция не вызвана во время открытия зашифрованной ветки
            initStorage(context, node, false, isOnlyFavorites, isOpenLastNode);
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
    public static void askPassword(Context context, final TetroidNode node, boolean isNodeOpening,
                                    boolean isOnlyFavorites, boolean isOpenLastNode) {
        LogManager.log(R.string.log_show_pass_dialog);
        // выводим окно с запросом пароля в асинхронном режиме
        PassDialogs.showPassEnterDialog(context, node, false, new PassDialogs.IPassInputResult() {
            @Override
            public void applyPass(final String pass, TetroidNode node) {
                // подтверждение введенного пароля
                PassManager.checkPass(context, pass, (res) -> {
                    if (res) {
                        PassManager.initPass(pass);
                        PINManager.askPINCode(context, isNodeOpening, () -> {
                            initStorage(context, node, true, isOnlyFavorites, isOpenLastNode);
                        });
                    } else {
                        // повторяем запрос
                        askPassword(context, node, isNodeOpening, isOnlyFavorites, isOpenLastNode);
                    }
                }, R.string.log_pass_is_incorrect);
            }

            @Override
            public void cancelPass() {
                // Если при первой загрузке хранилища установлена текущей зашифрованная ветка (node),
                // и пароль не сохраняли, то нужно его спросить.
                // Но если пароль вводить отказались, то просто грузим хранилище как есть
                // (только в первый раз, затем перезагружать не нужно)
                if (!getInstance().mIsAlreadyTryDecrypt && !DataManager.isLoaded()) {
                    getInstance().mIsAlreadyTryDecrypt = true;
                    initStorage(context, node, false, isOnlyFavorites, isOpenLastNode);
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
    public static void initStorage(Context context, TetroidNode node, boolean isDecrypt, boolean isOnlyFavorites, boolean isOpenLastNode) {
        // расшифровуем хранилище только в том случаем, если:
        //  1) не используем проверку ПИН-кода
        //  2) используем проверку ПИН-кода, при этом расшифровуем с открытием конкретной <b>зашифрованной</b> ветки
        //   (или ветки Избранное)
        isDecrypt = isDecrypt
                && (!PINManager.isRequestPINCode()
                || PINManager.isRequestPINCode() && node != null
                && (node.isCrypted() || node.equals(FavoritesManager.FAVORITES_NODE)));
        if (isDecrypt && DataManager.isNodesExist()) {
            // расшифровываем уже загруженное хранилище
            getStorageInitCallback().taskStarted(
                    new DecryptStorageTask(getStorageInitCallback(), context, node)
                            .run());
        } else {
            // загружаем хранилище впервые, с расшифровкой
            TetroidLog.logOperStart(TetroidLog.Objs.STORAGE, TetroidLog.Opers.LOAD);
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
        /*if (checkDirIsEmpty) {
            if (!FileUtils.isDirEmpty(new File(storagePath))) {
                AskDialogs.showYesDialog(this, () -> {
                    createStorage(storagePath, false);
                }, R.string.ask_dir_not_empty);
                return;
            }
        }*/
        boolean res = (initStorage(storagePath, true));
        if (res) {
            /*closeFoundFragment();
            mViewPagerAdapter.getMainFragment().clearView();
            mDrawerLayout.openDrawer(Gravity.LEFT);*/
            // сохраняем путь к хранилищу
//            if (SettingsManager.isLoadLastStoragePath()) {
            SettingsManager.setStoragePath(storagePath);
//            }
            /*initGUI(DataManager.createDefault(), false, false);*/
//            LogManager.log(getString(R.string.log_storage_created) + mStoragePath, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            TetroidLog.logOperRes(TetroidLog.Objs.STORAGE, TetroidLog.Opers.CREATE, "", Toast.LENGTH_SHORT);
        } else {
            /*mDrawerLayout.openDrawer(Gravity.LEFT);
            initGUI(false, false, false);*/
//            LogManager.log(getString(R.string.log_failed_storage_create) + mStoragePath, LogManager.Types.ERROR, Toast.LENGTH_LONG);
            TetroidLog.logOperErrorMore(TetroidLog.Objs.STORAGE, TetroidLog.Opers.CREATE, Toast.LENGTH_LONG);
        }
        return res;
    }

    /**
     * Открытие активности для первоначального выбора пути хранилища в файловой системе.
     */
    public static void showStorageFolderChooser(Activity activity, boolean isNew) {
        Intent intent = new Intent(activity, FolderPicker.class);
        intent.putExtra(FolderPicker.EXTRA_TITLE, activity.getString(R.string.title_storage_folder));
        intent.putExtra(FolderPicker.EXTRA_LOCATION, SettingsManager.getStoragePath());
        activity.startActivityForResult(intent, (isNew) ? REQUEST_CODE_CREATE_STORAGE : REQUEST_CODE_OPEN_STORAGE);
    }

    /**
     * Отправление запроса на синхронизацию стороннему приложению.
     *
     * @param storagePath
     */
    public static void startStorageSync(Activity activity, String storagePath) {
        String command = SettingsManager.getSyncCommand();
        Intent intent = SyncManager.createCommandSender(activity, storagePath, command);

        LogManager.log(activity.getString(R.string.log_start_storage_sync) + command);
        if (!SettingsManager.isNotRememberSyncApp()) {
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
            return DataManager.readStorage(mIsDecrypt, mIsFavoritesOnly);
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
                LogManager.log(String.format(mes, DataManager.getStorageName()), Toast.LENGTH_SHORT);
            } else {
                LogManager.log(mContext.getString(R.string.log_failed_storage_load) + DataManager.getStoragePath(),
                        LogManager.Types.WARNING, Toast.LENGTH_LONG);
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
            TetroidLog.logOperStart(TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT);
        }

        @Override
        protected Boolean doInBackground(Void... values) {
            return DataManager.decryptStorage(false);
        }

        @Override
        protected void onPostExecute(Boolean res) {
            getStorageInitCallback().taskPostExecute(mIsDrawerOpened);
            if (res) {
                LogManager.log(R.string.log_storage_decrypted, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            } else {
                TetroidLog.logDuringOperErrors(TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT, Toast.LENGTH_LONG);
            }
            getStorageInitCallback().afterStorageDecrypted(mNode);
        }

        private IStorageInitCallback getStorageInitCallback() {
            return (IStorageInitCallback) mTaskCallback;
        }
    }

}
