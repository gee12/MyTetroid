package com.gee12.mytetroid.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TetroidTask2;
import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.data.StorageManager;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.utils.FileUtils;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.ActivityDoubleTapListener;
import com.gee12.mytetroid.views.Message;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import lib.folderpicker.FolderPicker;

public abstract class TetroidActivity extends AppCompatActivity
        implements View.OnTouchListener, StorageManager.IStorageInitCallback {

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        this.mReceivedIntent = getIntent();

        this.mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

    /**
     * Обработка возвращаемого результата других активностей.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == StorageManager.REQUEST_CODE_OPEN_STORAGE
                || requestCode == StorageManager.REQUEST_CODE_CREATE_STORAGE)
                && resultCode == RESULT_OK) {
            String folderPath = data.getStringExtra(FolderPicker.EXTRA_DATA);
            if (!TextUtils.isEmpty(folderPath)) {
                boolean isCreate = (requestCode == StorageManager.REQUEST_CODE_CREATE_STORAGE);
                openOrCreateStorage(folderPath, isCreate);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean permGranted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        switch (requestCode) {
            case StorageManager.REQUEST_CODE_PERMISSION_WRITE_STORAGE: {
                if (permGranted) {
                    LogManager.log(this, R.string.log_write_ext_storage_perm_granted, ILogger.Types.INFO);
                    loadStorage(null);
                } else {
                    LogManager.log(this, R.string.log_missing_read_ext_storage_permissions, ILogger.Types.WARNING, Toast.LENGTH_SHORT);
                }
            } break;
            case StorageManager.REQUEST_CODE_PERMISSION_TERMUX: {
                if (permGranted) {
                    LogManager.log(this, R.string.log_run_termux_commands_perm_granted, ILogger.Types.INFO);
                    StorageManager.startStorageSyncAndInit(this);
                } else {
                    LogManager.log(this, R.string.log_missing_run_termux_commands_permissions,
                            ILogger.Types.WARNING, Toast.LENGTH_SHORT);
                }
            } break;
        }
    }

    /**
     * Открытие существующего или создание нового хранилище в указанном каталоге.
     * @param folderPath
     * @param isCreate
     */
    private void openOrCreateStorage(String folderPath, boolean isCreate) {
        if (isCreate) {
            if (FileUtils.isDirEmpty(new File(folderPath))) {
                createStorage(folderPath/*, true*/);
            } else {
                LogManager.log(this, R.string.log_dir_not_empty, ILogger.Types.ERROR, Toast.LENGTH_LONG);
            }
        } else {
//            StorageManager.initOrSyncStorage(this, folderPath, true);
            loadStorage(folderPath);
        }
        // сохраняем путь
        SettingsManager.setLastChoosedFolder(this, folderPath);
    }

    /**
     * Старт загрузки существующего хранилища.
     */
    protected abstract void loadStorage(String folderPath);

    /**
     * Старт создания нового хранилища.
     * @param storagePath
     */
    protected abstract void createStorage(String storagePath);

    @Override
    public void blockInterface() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    @Override
    public void unblockInterface() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    @Override
    public void taskStarted(TetroidTask2 task) {
        this.mCurTask = task;
    }

    public boolean isCurTaskRunning() {
        return (mCurTask != null && mCurTask.isRunning());
    }

    @Override
    public int taskPreExecute(int sRes) {
        mTextViewProgress.setText(sRes);
        mLayoutProgress.setVisibility(View.VISIBLE);
        ViewUtils.hideKeyboard(this, getWindow().getDecorView());
        return Gravity.NO_GRAVITY;
    }

    @Override
    public void taskPostExecute(int openedDrawer) {
        mLayoutProgress.setVisibility(View.INVISIBLE);
    }

    @Override
    public void initGUI(boolean res, boolean mIsFavoritesOnly, boolean mIsOpenLastNode) {

    }

    @Override
    public void afterStorageLoaded(boolean res) {

    }

    @Override
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

    protected void setVisibilityActionHome(boolean isVis) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(isVis);
        }
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
}
