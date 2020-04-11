package com.gee12.mytetroid.activities;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.ActivityDoubleTapListener;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.R;

public abstract class TetroidActivity extends AppCompatActivity implements View.OnTouchListener {

    protected GestureDetectorCompat gestureDetector;
    protected Toolbar mToolbar;
    protected TextView tvTitle;
    protected TextView tvSubtitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        this.mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // обработчик нажатия на экране
        this.gestureDetector = new GestureDetectorCompat(this, new ActivityDoubleTapListener(this::toggleFullscreen));

        this.tvTitle = mToolbar.findViewById(R.id.text_view_title);
        this.tvSubtitle = mToolbar.findViewById(R.id.text_view_subtitle);

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

    public boolean toggleFullscreen() {
        return App.toggleFullscreen(this);
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
}
