package com.gee12.mytetroid.fragments;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;

import com.gee12.mytetroid.activities.IMainView;
import com.gee12.mytetroid.views.MainPagerAdapter;

public abstract class TetroidFragment extends Fragment implements View.OnTouchListener {

    protected GestureDetectorCompat gestureDetector;
    protected IMainView mainView;
    protected String titleMask;

    public TetroidFragment() {}

    public TetroidFragment(GestureDetectorCompat detector) {
        this.gestureDetector = detector;
    }

    public void setTitleMask(String titleMask) {
        this.titleMask = titleMask;
    }

    public abstract String getTitle();

    public void setMainView(IMainView main) {
        this.mainView = main;
    }

    public void setMainView(Bundle arguments) {
        if (arguments != null) {
            this.mainView = arguments.getParcelable(MainPagerAdapter.KEY_MAIN_VIEW);
        }
    }

    /**
     *
     * @param detector
     */
    public void setGestureDetector(GestureDetectorCompat detector) {
        this.gestureDetector = detector;
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
        if (gestureDetector != null)
            gestureDetector.onTouchEvent(event);
        return false;
    }
}
