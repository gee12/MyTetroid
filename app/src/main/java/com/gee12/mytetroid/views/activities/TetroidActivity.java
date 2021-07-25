package com.gee12.mytetroid.views.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TetroidTask2;
import com.gee12.mytetroid.common.Constants;
import com.gee12.mytetroid.data.AttachesManager;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.helpers.NetworkHelper;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.logs.TetroidLog;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.UriUtils;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.viewmodels.BaseStorageViewModel;
import com.gee12.mytetroid.viewmodels.ReadDecryptStorageState;
import com.gee12.mytetroid.views.ActivityDoubleTapListener;
import com.gee12.mytetroid.views.Message;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public abstract class TetroidActivity extends AppCompatActivity
        implements View.OnTouchListener/*, StorageManager.IStorageInitCallback*/ {

    public interface IDownloadFileResult {
        void onSuccess(Uri uri);
        void onError(Exception ex);
    }

    protected GestureDetectorCompat gestureDetector;
    protected Menu mOptionsMenu;
    protected Toolbar mToolbar;
    protected TextView tvTitle;
    protected TextView tvSubtitle;
    protected LinearLayout mLayoutProgress;
    protected TextView mTextViewProgress;
    protected TetroidTask2 mCurTask;
    protected Intent mReceivedIntent;
    protected boolean mIsFullScreen;
    protected boolean mIsOnCreateProcessed;
    protected boolean mIsGUICreated;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        this.mReceivedIntent = getIntent();

        this.mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        setVisibilityActionHome(true);

        // обработчик нажатия на экране
        this.gestureDetector = new GestureDetectorCompat(this,
                new ActivityDoubleTapListener(() -> toggleFullscreen(true)));

        this.tvTitle = mToolbar.findViewById(R.id.text_view_title);
        this.tvSubtitle = mToolbar.findViewById(R.id.text_view_subtitle);

        this.mLayoutProgress = findViewById(R.id.layout_progress_bar);
        this.mTextViewProgress = findViewById(R.id.progress_text);

        this.mIsOnCreateProcessed = false;
        this.mIsGUICreated = false;

        if (getViewModel() != null) {
            getViewModel().getReadStorageStateEvent().observe(this, it -> {
                Constants.ActivityEvents status = it.getStatus();
                ReadDecryptStorageState params = it.getData();

                if (status == Constants.ActivityEvents.TaskPreExecute) {
                    int stringResId = (params.isDecrypt()) ? R.string.task_storage_decrypting : R.string.task_storage_loading;
                    int openedDrawer = taskPreExecute(stringResId);
                    params.setOpenedDrawer(openedDrawer);

                } else if (status == Constants.ActivityEvents.TaskPostExecute) {
                    taskPostExecute(params.getOpenedDrawer());

                } else if (status == Constants.ActivityEvents.InitGUI) {
                    initGUI(params.getResult(), params.isFavoritesOnly(), params.isOpenLastNode());
                }
            });
        }
    }

    protected BaseStorageViewModel getViewModel() {
        return null;
    }

    /**
     * Установка пометки, что обработчик OnCreate был вызван, и можно вызвать другие обработчики,
     *  следующие за ним (а не вразнобой на разных устройствах).
     */
    protected void afterOnCreate() {
        this.mIsOnCreateProcessed = true;
        if (mOptionsMenu != null) {
            onCreateOptionsMenu(mOptionsMenu);
            onPrepareOptionsMenu(mOptionsMenu);
        }
    }

    /**
     *
     * @return
     */
    protected abstract int getLayoutResourceId();

    /**
     * Обработчик события, когда создались все элементы интерфейса.
     * Вызывается из onCreateOptionsMenu(), который, в свою очередь, принудительно вызывается после onCreate().
     */
    protected abstract void onGUICreated();

    protected void onPermissionGranted(int permission) {}

    /**
     * Установка заголовка активности.
     * @param title
     */
    @Override
    public void setTitle(CharSequence title) {
        tvTitle.setText(title);
    }

    /**
     * Установка подзаголовка активности.
     * @param title
     */
    public void setSubtitle(CharSequence title) {
        tvSubtitle.setText(title);
    }

//    /**
//     * Если потеряли фокус на активности, то выходим их полноэкранного режима
//     * (например, при нажатии на "физическую" кнопку вызова меню).
//     * @param hasFocus
//     */
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if (!hasFocus) {
//            setFullscreen(false);
//        }
//    }

    /**
     * Включение/отключение полноэкранного режима.
     * Оставлен только в RecordActivity.
     * @param fromDoubleTap
     * @return
     */
    public int toggleFullscreen(boolean fromDoubleTap) {
        if (this instanceof RecordActivity) {
            if (!fromDoubleTap || SettingsManager.isDoubleTapFullscreen(this)) {
                boolean newValue = !mIsFullScreen;
                ViewUtils.setFullscreen(this, newValue);
                this.mIsFullScreen = newValue;
                return (newValue) ? 1 : 0;
            }
        } else {
            return -1;
        }
        return -1;
    }

    /**
     * Загрузка файла по URL в каталог кэша
     * @param url
     * @param callback
     */
    public void downloadFileToCache(String url, IDownloadFileResult callback) {
        if (TextUtils.isEmpty(url)) {
            TetroidLog.log(this, R.string.log_link_is_empty, ILogger.Types.ERROR, Toast.LENGTH_SHORT);
            return;
        }
        setProgressText(R.string.title_file_downloading);
        String fileName = UriUtils.getFileName(url);
        if (TextUtils.isEmpty(fileName)) {
//            Exception ex = new Exception("");
//            if (callback != null) {
//                callback.onError(ex);
//            }
//            TetroidLog.log(TetroidActivity.this,
//                    getString(R.string.log_error_download_file_mask, ex.getMessage()), Toast.LENGTH_LONG);
//            return;
            fileName = DataManager.createDateTimePrefix();
        }
        String outputFileName = getExternalCacheDir() + "/" + fileName;
        NetworkHelper.downloadFileAsync(url, outputFileName, new NetworkHelper.IWebFileResult() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    if (callback != null) {
                        callback.onSuccess(Uri.fromFile(new File(outputFileName)));
                    }
                    setProgressVisibility(false);
                });
            }
            @Override
            public void onError(Exception ex) {
                runOnUiThread(() -> {
                    if (callback != null) {
                        callback.onError(ex);
                    }
                    TetroidLog.log(TetroidActivity.this,
                            getString(R.string.log_error_download_file_mask, ex.getMessage()), Toast.LENGTH_LONG);
                    setProgressVisibility(false);
                });
            }
        });
    }

    /**
     * Переопределяем обработчик нажатия на экране
     * для обработки перехода в полноэкранный режим.
     * @param v
     * @param event
     * @return
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return false;
    }

    public boolean onBeforeCreateOptionsMenu(Menu menu) {
        boolean onCreateCalled = isOnCreateProcessed();
        if (!onCreateCalled) {
            this.mOptionsMenu = menu;
        }
        return onCreateCalled;
    }

    @SuppressLint("RestrictedApi")
    public boolean onAfterCreateOptionsMenu(Menu menu) {
        // запускаем только 1 раз
        if (!mIsGUICreated) {
            // для отображения иконок
            if (menu instanceof MenuBuilder){
                MenuBuilder m = (MenuBuilder) menu;
                m.setOptionalIconsVisible(true);
            }
            // устанавливаем флаг, что стандартные элементы активности созданы
            onGUICreated();
        }
        this.mIsGUICreated = true;
        return true;
    }

/*    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!isOnCreateCalled())
            return true;
        return false;
    }*/

    public void updateOptionsMenu() {
        if (mOptionsMenu != null) {
            onPrepareOptionsMenu(mOptionsMenu);
        } else {
            LogManager.log(this, getString(R.string.log_mOptionsMenu_is_null), ILogger.Types.WARNING, -1);
        }
    }

    /**
     * Обработчик выбора пунктов системного меню.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_fullscreen:
                toggleFullscreen(false);
                return true;
            case R.id.action_about_app:
                ViewUtils.startActivity(this, AboutActivity.class, null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

//    /**
//     * Обработка возвращаемого результата других активностей.
//     *
//     * @param requestCode
//     * @param resultCode
//     * @param data
//     */
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if ((requestCode == Constants.REQUEST_CODE_OPEN_STORAGE
//                /*|| requestCode == Constants.REQUEST_CODE_CREATE_STORAGE*/)
//                && resultCode == RESULT_OK) {
//            String folderPath = data.getStringExtra(FolderPicker.EXTRA_DATA);
//            if (!TextUtils.isEmpty(folderPath)) {
////                boolean isCreate = (requestCode == Constants.REQUEST_CODE_CREATE_STORAGE);
////                openOrCreateStorage(folderPath, isCreate);
//                loadStorage(folderPath);
//            }
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean permGranted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        switch (requestCode) {

            case Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE: {
                if (permGranted) {
                    LogManager.log(this, R.string.log_write_ext_storage_perm_granted, ILogger.Types.INFO);
//                    loadStorage(null);
                    onPermissionGranted(Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE);
                } else {
                    LogManager.log(this, R.string.log_missing_read_ext_storage_permissions, ILogger.Types.WARNING, Toast.LENGTH_SHORT);
                }
            } break;

            case Constants.REQUEST_CODE_PERMISSION_WRITE_TEMP: {
                if (permGranted) {
                    LogManager.log(this, R.string.log_write_ext_storage_perm_granted, ILogger.Types.INFO);
                    onPermissionGranted(Constants.REQUEST_CODE_PERMISSION_WRITE_TEMP);
                } else {
                    LogManager.log(this, R.string.log_missing_write_ext_storage_permissions, ILogger.Types.WARNING, Toast.LENGTH_SHORT);
                }
            } break;

            case Constants.REQUEST_CODE_PERMISSION_TERMUX: {
                if (permGranted) {
                    LogManager.log(this, R.string.log_run_termux_commands_perm_granted, ILogger.Types.INFO);
//                    StorageManager.startStorageSyncAndInit(this);
                    onPermissionGranted(Constants.REQUEST_CODE_PERMISSION_TERMUX);
                } else {
                    LogManager.log(this, R.string.log_missing_run_termux_commands_permissions,
                            ILogger.Types.WARNING, Toast.LENGTH_SHORT);
                }
            } break;
        }
    }

    //TODO: убрано, т.к. createStorage() будет вызываться только в StoragesActivity и StorageSettingsActivity
//    /**
//     * Открытие существующего или создание нового хранилище в указанном каталоге.
//     * @param folderPath
//     * @param isCreate
//     */
//    private void openOrCreateStorage(String folderPath, boolean isCreate) {
//        if (isCreate) {
//            if (FileUtils.isDirEmpty(new File(folderPath))) {
//                createStorage(folderPath/*, true*/);
//            } else {
//                LogManager.log(this, R.string.log_dir_not_empty, ILogger.Types.ERROR, Toast.LENGTH_LONG);
//            }
//        } else {
//            StorageManager.initOrSyncStorage(this, folderPath, true);
//            loadStorage(folderPath);
//        }
//        // сохраняем путь
//        SettingsManager.setLastChoosedFolder(this, folderPath);
//    }

    /**
     * Запуск загрузки хранилища.
     *
     * Будет вызываться при:
     *  - получении Intent из другой активности с командой загрузки хранилища (по Id)
     *      Например, 1) при выборе хранилища в StoragesActivity и переходе в MainActivity
     *  - получении Intent из активности выбора каталога хранилища с получением выбранного пути
     *      Например, ---1) при вызове FolderChooser в StoragesActivity при добавлении хранилища
     *      Например, ---2) при вызове FolderChooser в StorageSettingsActivity при изменении пути хранилища
     *  - загрузки хранилища в RecordActivity для сохранения временной записи, если оно еще не загружено
     *
     */
//    protected void loadStorage(String storagePath) {
////        boolean isLoadLastForced = false;
////        boolean isCheckFavorMode = true;
////        if (storagePath == null) {
////            StorageManager.startInitStorage(this, this, isLoadLastForced, isCheckFavorMode);
////        } else {
////            StorageManager.initOrSyncStorage(this, storagePath, isCheckFavorMode);
////            StorageManager.initOrSyncStorage(this, storagePath, isCheckFavorMode);
////        }
//        if (getViewModel() != null) {
//            getViewModel().startloadStorage(storagePath, false, true);
//        }
//    }

//    /**
//     * Старт создания нового хранилища.
//     * @param storagePath
//     */
//    protected abstract void createStorage(String storagePath);

//    @Override
    public void blockInterface() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

//    @Override
    public void unblockInterface() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

//    @Override
    public void taskStarted(TetroidTask2 task) {
        this.mCurTask = task;
    }

    public boolean isCurTaskRunning() {
        return (mCurTask != null && mCurTask.isRunning());
    }

//    @Override
    public int taskPreExecute(int sRes) {
        setProgressText(sRes);
        ViewUtils.hideKeyboard(this, getWindow().getDecorView());
        return Gravity.NO_GRAVITY;
    }

//    @Override
    public void taskPostExecute(int openedDrawer) {
        setProgressVisibility(false);
    }

//    @Override
    public void initGUI(boolean res, boolean mIsFavoritesOnly, boolean mIsOpenLastNode) {

    }

//    @Override
    public void afterStorageLoaded(boolean res) {

    }

//    @Override
    public void afterStorageDecrypted(TetroidNode node) {

    }

    /**
     * Обработчик, вызываемый перед запуском кода в обработчике onBackPressed().
     * @return true - можно продолжить работу обработчика onBackPressed(), иначе - прервать
     */
    public boolean onBeforeBackPressed() {
        if (isCurTaskRunning()) {
            // если выполняется задание, то не реагируем на нажатие кнопки Back
            return false;
        }
        return true;
    }

    protected void setVisibilityActionHome(boolean isVisible) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(isVisible);
        }
    }

    protected void setProgressVisibility(boolean isVisible) {
        mLayoutProgress.setVisibility(ViewUtils.toVisibility(isVisible));
    }

    protected void setProgressText(int progressTextResId) {
        setProgressText(getString(progressTextResId));
    }

    protected void setProgressText(String progressText) {
        mLayoutProgress.setVisibility(View.VISIBLE);
        mTextViewProgress.setText(progressText);
    }

    /**
     * Установка видимости пункта меню.
     * @param menuItem
     * @param isVisible
     */
    protected void visibleMenuItem(MenuItem menuItem, boolean isVisible) {
        ViewUtils.setVisibleIfNotNull(menuItem, isVisible);
    }

    /**
     * Установка активности пункта меню.
     * @param menuItem
     * @param isEnabled
     */
    protected void enableMenuItem(MenuItem menuItem, boolean isEnabled) {
        ViewUtils.setEnabledIfNotNull(menuItem, isEnabled);
    }

    /**
     * Принудительное отображение иконок у пунктов меню.
     * @param v
     * @param menu
     */
    @SuppressLint("RestrictedApi")
    protected void setForceShowMenuIcons(View v, MenuBuilder menu) {
        MenuPopupHelper menuHelper = new MenuPopupHelper(this, menu, v);
        menuHelper.setForceShowIcon(true);
        menuHelper.show();
    }

    /**
     * Установка подзаголовка активности.
     * @param subtitle
     */
    protected void setSubtitle(String subtitle) {
        tvSubtitle.setVisibility(View.VISIBLE);
        tvSubtitle.setTextSize(16);
        tvSubtitle.setText(subtitle);
    }

    public void showActivityForResult(Class<?> cls, int requestCode) {
        Intent intent = new Intent(this, cls);
        startActivityForResult(intent, requestCode);
    }

    public boolean isOnCreateProcessed() {
        return mIsOnCreateProcessed;
    }

    /**
     * Вывод интерактивного уведомления SnackBar "Подробнее в логах".
     */
    protected void showSnackMoreInLogs() {
        Message.showSnackMoreInLogs(this, R.id.layout_coordinator);
    }

    /**
     * Задание, в котором выполняется прикрепление нового файла к записи.
     */
    public class AttachFileTask extends TetroidTask2<String, Void, TetroidFile> {

        TetroidRecord mRecord;
        boolean mDeleteSrcFile;

        public AttachFileTask(TetroidRecord record, boolean deleteSrcFile) {
            super(TetroidActivity.this, TetroidActivity.this);
            this.mRecord = record;
            this.mDeleteSrcFile = deleteSrcFile;
        }

        @Override
        protected void onPreExecute() {
            taskPreExecute(R.string.task_attach_file);
        }

        @Override
        protected TetroidFile doInBackground(String... values) {
            String fileFullName = values[0];
            return AttachesManager.attachFile(mContext, fileFullName, mRecord, mDeleteSrcFile);
        }

        @Override
        protected void onPostExecute(TetroidFile res) {
            taskPostExecute(Gravity.NO_GRAVITY);
        }
    }

}
