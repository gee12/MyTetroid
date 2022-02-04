package com.gee12.mytetroid.views.activities;

import static com.gee12.mytetroid.common.extensions.ViewExtensionsKt.hideKeyboard;

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

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.ViewModelProvider;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.common.Constants;
import com.gee12.mytetroid.data.settings.CommonSettings;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidStorage;
import com.gee12.mytetroid.common.utils.ViewUtils;
import com.gee12.mytetroid.viewmodels.BaseStorageViewModel;
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory;
import com.gee12.mytetroid.views.ActivityDoubleTapListener;
import com.gee12.mytetroid.views.IViewEventListener;
import com.gee12.mytetroid.views.TetroidMessage;
import com.gee12.mytetroid.views.dialogs.storage.StorageDialogs;

import org.jetbrains.annotations.NotNull;

import lib.folderpicker.FolderPicker;

public abstract class TetroidActivity<VM extends BaseStorageViewModel> extends AppCompatActivity
        implements View.OnTouchListener, IViewEventListener {

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
    protected Intent receivedIntent;
    protected boolean isFullScreen;
    protected boolean isOnCreateProcessed;
    protected boolean isGUICreated;

    protected VM viewModel;

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
        viewModel = new ViewModelProvider(this, new TetroidViewModelFactory(getApplication(), getStorageId()))
                .get(getViewModelClazz());
        viewModel.logDebug(getString(R.string.log_activity_opened_mask, getClass().getSimpleName()));

        viewModel.getViewEvent().observe(this, it -> onViewEvent(it.getState(), it.getData()));
        viewModel.getStorageEvent().observe(this, it -> onStorageEvent(it.getState(), it.getData()));
        viewModel.getObjectAction().observe(this, it -> onObjectEvent(it.getState(), it.getData()));
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

    protected abstract int getLayoutResourceId();

    protected Integer getStorageId() {
        return null;
    }

    /**
     * Обработчик события, когда создались все элементы интерфейса.
     * Вызывается из onCreateOptionsMenu(), который, в свою очередь, принудительно вызывается после onCreate().
     */
    protected void onUICreated(boolean uiCreated) {}

    /**
     * Обработчик изменения состояния View.
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
                boolean isVisible = !(data instanceof Boolean) || (boolean) data;
                setProgressVisibility(isVisible, null);
                break;
            case ShowProgressText:
                setProgressText((String) data);
                break;
        }
    }

    /**
     * Обработчик изменения состояния хранилища.
     * @param event
     * @param data
     */
    protected void onStorageEvent(Constants.StorageEvents event, Object data) {
        switch (event) {
            case NoDefaultStorage:
                StorageDialogs.INSTANCE.askForDefaultStorageNotSpecified(this, () -> showStoragesActivity());
                break;
            case Inited:
                afterStorageInited();
                break;
            case Loaded:
                afterStorageLoaded((boolean) data);
                break;
            case Decrypted:
                afterStorageDecrypted((TetroidNode) data);
                break;
        }
    }

    /**
     * Обработчик изменения состояния объекта.
     * @param event
     * @param data
     */
    protected void onObjectEvent(Object event, Object data) {

    }

    public void afterStorageInited() {}

    public void afterStorageLoaded(boolean res) {}

    public void afterStorageDecrypted(TetroidNode node) {}

    protected void onPermissionGranted(int requestCode) {
        // по-умолчанию обрабатываем результат разрешения во ViewModel,
        //  но можем переопределить onPermissionGranted и в активити
        viewModel.onPermissionGranted(requestCode);
    }

    protected void onPermissionCanceled(int requestCode) {
        viewModel.onPermissionCanceled(requestCode);
    }

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
            if (!fromDoubleTap || CommonSettings.isDoubleTapFullscreen(this)) {
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
        }
        // устанавливаем флаг, что стандартные элементы активности созданы
        onUICreated(!isGUICreated);
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
            viewModel.logWarning("TetroidActivity.updateOptionsMenu(): optionsMenu is null", false);
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
                AboutActivity.start(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGranted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        switch (requestCode) {

            case Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE: {
                if (isGranted) {
                    viewModel.log(R.string.log_write_ext_storage_perm_granted);
                } else {
                    viewModel.logWarning(R.string.log_missing_read_ext_storage_permissions, true);
                }
            } break;

            case Constants.REQUEST_CODE_PERMISSION_WRITE_TEMP: {
                if (isGranted) {
                    viewModel.log(R.string.log_write_ext_storage_perm_granted);
                } else {
                    viewModel.logWarning(R.string.log_missing_write_ext_storage_permissions, true);
                }
            } break;

            case Constants.REQUEST_CODE_PERMISSION_TERMUX: {
                if (isGranted) {
                    viewModel.log(R.string.log_run_termux_commands_perm_granted);
                } else {
                    viewModel.logWarning(R.string.log_missing_run_termux_commands_permissions, true);
                }
            } break;

            default:
                return;
        }

        if (isGranted) {
            onPermissionGranted(requestCode);
        } else {
            onPermissionCanceled(requestCode);
        }
    }

    public void taskPreExecute(int progressTextResId) {
        blockInterface();
        setProgressText(progressTextResId);
        hideKeyboard(getWindow().getDecorView());
    }

    public void taskPostExecute() {
        unblockInterface();
        setProgressVisibility(false, null);
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
        // если выполняется задание, то не реагируем на нажатие кнопки Back
        return !viewModel.isBusy();
    }

    protected void setVisibilityActionHome(boolean isVisible) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(isVisible);
        }
    }

    @Override
    public void setProgressVisibility(boolean isVisible, String text) {
        if (isVisible) {
            tvProgress.setText(text);
            layoutProgress.setVisibility(View.VISIBLE);
        } else {
            layoutProgress.setVisibility(View.GONE);
        }
        layoutProgress.setVisibility(ViewUtils.toVisibility(isVisible));
    }

    @Override
    public void setProgressText(int progressTextResId) {
        setProgressText(getString(progressTextResId));
    }

    @Override
    public void setProgressText(String progressText) {
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

    protected void showStoragesActivity() {
        StoragesActivity.start(this, Constants.REQUEST_CODE_STORAGES_ACTIVITY);
    }

    protected void showStorageSettingsActivity(TetroidStorage storage) {
        if (storage == null) return;
        startActivityForResult(StorageSettingsActivity.newIntent(this, storage), Constants.REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY);
    }

    public boolean isOnCreateProcessed() {
        return isOnCreateProcessed;
    }

    /**
     * Публикация сообщений.
     * @param message
     */
    protected void onMessage(com.gee12.mytetroid.logs.Message message) {
        TetroidMessage.show(this, message);
    }

    /**
     * Вывод интерактивного уведомления SnackBar "Подробнее в логах".
     */
    @Override
    public void showSnackMoreInLogs() {
        TetroidMessage.showSnackMoreInLogs(this, R.id.layout_coordinator);
    }

}
