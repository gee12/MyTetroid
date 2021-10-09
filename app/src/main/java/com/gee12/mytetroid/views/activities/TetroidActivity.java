package com.gee12.mytetroid.views.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TetroidTask2;
import com.gee12.mytetroid.common.Constants;
import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.viewmodels.BaseStorageViewModel;
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory;
import com.gee12.mytetroid.views.ActivityDoubleTapListener;
import com.gee12.mytetroid.views.Message;

import org.jetbrains.annotations.NotNull;

import lib.folderpicker.FolderPicker;

public abstract class TetroidActivity<VM extends BaseStorageViewModel> extends AppCompatActivity
        implements View.OnTouchListener {

    public interface IDownloadFileResult {
        void onSuccess(Uri uri);
        void onError(Exception ex);
    }

    protected GestureDetectorCompat gestureDetector;
    protected Menu optionsMenu;
    protected Toolbar toolbar;
    protected TextView tvTitle;
    protected TextView tvSubtitle;
    protected LinearLayout layoutProgress;
    protected TextView tvProgress;
    protected TetroidTask2 curTask;
    protected Intent receivedIntent;
    protected boolean isFullScreen;
    protected boolean isOnCreateProcessed;
    protected boolean isGUICreated;

    protected VM viewModel;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        this.receivedIntent = getIntent();

        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setVisibilityActionHome(true);

        // обработчик нажатия на экране
        this.gestureDetector = new GestureDetectorCompat(this,
                new ActivityDoubleTapListener(() -> toggleFullscreen(true)));

        this.tvTitle = toolbar.findViewById(R.id.text_view_title);
        this.tvSubtitle = toolbar.findViewById(R.id.text_view_subtitle);

        this.layoutProgress = findViewById(R.id.layout_progress_bar);
        this.tvProgress = findViewById(R.id.progress_text);

        this.isOnCreateProcessed = false;
        this.isGUICreated = false;

        initViewModel();
    }

    protected void initViewModel() {
        this.viewModel = new ViewModelProvider(this, new TetroidViewModelFactory(getApplication()))
                .get(getViewModelClazz());

        viewModel.getViewEvent().observe(this, it -> onViewEvent(it.getState(), it.getData()));
        viewModel.getStorageEvent().observe(this, it -> onStorageEvent(it.getState(), it.getData()));
        viewModel.getMessageObservable().observe(this, this::onMessage);
    }

    protected abstract Class<VM> getViewModelClazz();

    /**
     * Установка пометки, что обработчик OnCreate был вызван, и можно вызвать другие обработчики,
     *  следующие за ним (а не вразнобой на разных устройствах).
     */
    protected void afterOnCreate() {
        this.isOnCreateProcessed = true;
        if (optionsMenu != null) {
            onCreateOptionsMenu(optionsMenu);
            onPrepareOptionsMenu(optionsMenu);
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

    /**
     *
     * @param event
     * @param data
     */
    protected void onViewEvent(Constants.ViewEvents event, Object data) {
        switch (event) {
//            case TaskStarted:
//                int stringResId = (int) data;
//                taskPreExecute(stringResId);
//                break;
//            case TaskFinished:
//                taskPostExecute();
//                break;
            case ShowProgress:
                setProgressVisibility((boolean) data);
                break;
            case ShowProgressText:
                setProgressText((String) data);
                break;
        }
    }

    /**
     * Обработчик изменения состояния хранилища.
     * @param state
     * @param data
     */
    protected void onStorageEvent(Constants.StorageEvents state, Object data) {
        switch (state) {
            case Loaded:
                afterStorageLoaded((boolean) data);
                break;
            case Decrypted:
                afterStorageDecrypted((TetroidNode) data);
                break;
        }
    }

    public void afterStorageLoaded(boolean res) {}

    public void afterStorageDecrypted(TetroidNode node) {}

    protected void onPermissionGranted(int permission) {}

    // region FileFolderPicker

    public void openFilePicker() {
        openFileFolderPicker(true);
    }

    public void openFolderPicker() {
        openFileFolderPicker(false);
    }

    /**
     * Открытие активности для выбора файла или каталога в файловой системе.
     */
    public void openFileFolderPicker(boolean isPickFile) {
        Intent intent = new Intent(this, FolderPicker.class);
        intent.putExtra(FolderPicker.EXTRA_TITLE, (isPickFile) ? getString(R.string.title_select_file_to_upload) : getString(R.string.title_save_file_to));
//        if (getViewModel() != null) {
//            intent.putExtra(FolderPicker.EXTRA_LOCATION, getViewModel().getLastFolderPathOrDefault(false));
//        }
        intent.putExtra(FolderPicker.EXTRA_LOCATION, viewModel.getLastFolderPathOrDefault(false));
        intent.putExtra(FolderPicker.EXTRA_PICK_FILES, isPickFile);
        startActivityForResult(intent, (isPickFile) ? Constants.REQUEST_CODE_FILE_PICKER : Constants.REQUEST_CODE_FOLDER_PICKER);
    }

    // endregion FileFolderPicker

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
                boolean newValue = !isFullScreen;
                ViewUtils.setFullscreen(this, newValue);
                this.isFullScreen = newValue;
                return (newValue) ? 1 : 0;
            }
        } else {
            return -1;
        }
        return -1;
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
            this.optionsMenu = menu;
        }
        return onCreateCalled;
    }

    @SuppressLint("RestrictedApi")
    public boolean onAfterCreateOptionsMenu(Menu menu) {
        // запускаем только 1 раз
        if (!isGUICreated) {
            // для отображения иконок
            if (menu instanceof MenuBuilder){
                MenuBuilder m = (MenuBuilder) menu;
                m.setOptionalIconsVisible(true);
            }
            // устанавливаем флаг, что стандартные элементы активности созданы
            onGUICreated();
        }
        this.isGUICreated = true;
        return true;
    }

/*    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!isOnCreateCalled())
            return true;
        return false;
    }*/

    public void updateOptionsMenu() {
        if (optionsMenu != null) {
            onPrepareOptionsMenu(optionsMenu);
        } else {
            viewModel.logWarning(getString(R.string.log_mOptionsMenu_is_null), false);
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
                    viewModel.log(R.string.log_write_ext_storage_perm_granted);
//                    loadStorage(null);
                    onPermissionGranted(Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE);
                } else {
                    viewModel.logWarning(R.string.log_missing_read_ext_storage_permissions, true);
                }
            } break;

            case Constants.REQUEST_CODE_PERMISSION_WRITE_TEMP: {
                if (permGranted) {
                    viewModel.log(R.string.log_write_ext_storage_perm_granted);
                    onPermissionGranted(Constants.REQUEST_CODE_PERMISSION_WRITE_TEMP);
                } else {
                    viewModel.logWarning(R.string.log_missing_write_ext_storage_permissions, true);
                }
            } break;

            case Constants.REQUEST_CODE_PERMISSION_TERMUX: {
                if (permGranted) {
                    viewModel.log(R.string.log_run_termux_commands_perm_granted);
//                    StorageManager.startStorageSyncAndInit(this);
                    onPermissionGranted(Constants.REQUEST_CODE_PERMISSION_TERMUX);
                } else {
                    viewModel.logWarning(R.string.log_missing_run_termux_commands_permissions, true);
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
//                LogManager.log(R.string.log_dir_not_empty, ILogger.Types.ERROR, Toast.LENGTH_LONG);
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
////        }
//        if (getViewModel() != null) {
//            getViewModel().startloadStorage(storagePath, false, true);
//        }
//    }

    public void taskStarted(TetroidTask2 task) {
        this.curTask = task;
    }

    public boolean isCurTaskRunning() {
        return (curTask != null && curTask.isRunning());
    }

    public void taskPreExecute(int progressTextResId) {
        blockInterface();
        setProgressText(progressTextResId);
        ViewUtils.hideKeyboard(this, getWindow().getDecorView());
    }

    public void taskPostExecute() {
        unblockInterface();
        setProgressVisibility(false);
    }

    public void blockInterface() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    public void unblockInterface() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
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
        layoutProgress.setVisibility(ViewUtils.toVisibility(isVisible));
    }

    protected void setProgressText(int progressTextResId) {
        setProgressText(getString(progressTextResId));
    }

    protected void setProgressText(String progressText) {
        layoutProgress.setVisibility(View.VISIBLE);
        tvProgress.setText(progressText);
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
        return isOnCreateProcessed;
    }

    /**
     * Публикация сообщений.
     * @param message
     */
    protected void onMessage(com.gee12.mytetroid.logs.Message message) {
        switch (message.getType()) {
            case INFO:
                Toast.makeText(this, message.getMessage(), Toast.LENGTH_SHORT).show();
                break;
            case WARNING:
                Toast.makeText(this, message.getMessage(), Toast.LENGTH_LONG).show();
                break;
            case ERROR:
                Toast.makeText(this, message.getMessage(), Toast.LENGTH_LONG).show();
//                Message.showSnackMoreInLogs(this, R.id.layout_coordinator);
                break;
            default:
                // TODO: ?
                Toast.makeText(this, message.getMessage(), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * Вывод интерактивного уведомления SnackBar "Подробнее в логах".
     */
    protected void showSnackMoreInLogs() {
        Message.showSnackMoreInLogs(this, R.id.layout_coordinator);
    }

}
