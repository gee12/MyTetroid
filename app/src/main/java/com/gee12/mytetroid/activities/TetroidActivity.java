package com.gee12.mytetroid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TetroidTask2;
import com.gee12.mytetroid.data.StorageManager;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.ActivityDoubleTapListener;

public abstract class TetroidActivity extends AppCompatActivity
        implements View.OnTouchListener, StorageManager.IStorageInitCallback {

    protected GestureDetectorCompat gestureDetector;
    protected Toolbar mToolbar;
    protected TextView tvTitle;
    protected TextView tvSubtitle;
    protected LinearLayout mLayoutProgress;
    protected TextView mTextViewProgress;
    protected TetroidTask2 mCurTask;
    protected Intent mReceivedIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        this.mReceivedIntent = getIntent();

        this.mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // обработчик нажатия на экране
        this.gestureDetector = new GestureDetectorCompat(this,
                new ActivityDoubleTapListener(() -> toggleFullscreen(true)));

        this.tvTitle = mToolbar.findViewById(R.id.text_view_title);
        this.tvSubtitle = mToolbar.findViewById(R.id.text_view_subtitle);

        this.mLayoutProgress = findViewById(R.id.layout_progress);
        this.mTextViewProgress = findViewById(R.id.progress_text);

    }

    protected abstract int getLayoutResourceId();

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
    public boolean toggleFullscreen(boolean fromDoubleTap) {
        if (this instanceof RecordActivity) {
            return App.toggleFullscreen(this, fromDoubleTap);
        } else {
            return false;
        }
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
    public boolean taskPreExecute(int sRes) {
        mTextViewProgress.setText(sRes);
        mLayoutProgress.setVisibility(View.VISIBLE);
        return true;
    }

    @Override
    public void taskPostExecute(boolean isDrawerOpened) {
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
}
