package com.gee12.mytetroid.fragments;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;

import com.gee12.mytetroid.activities.IMainView;
import com.gee12.mytetroid.adapters.MainPagerAdapter;

public abstract class TetroidFragment extends Fragment implements View.OnTouchListener {

    protected GestureDetectorCompat mGestureDetector;
    protected IMainView mMainView;
    protected String mTitleMask;

    public TetroidFragment() {}

    public TetroidFragment(GestureDetectorCompat detector) {
        this.mGestureDetector = detector;
    }

    public void setTitleMask(String titleMask) {
        this.mTitleMask = titleMask;
    }

    public abstract String getTitle();

    public void setMainView(IMainView main) {
        this.mMainView = main;
    }

    public void setMainView(Bundle arguments) {
        if (arguments != null) {
            this.mMainView = arguments.getParcelable(MainPagerAdapter.KEY_MAIN_VIEW);
        }
    }

    /**
     *
     * @param detector
     */
    public void setGestureDetector(GestureDetectorCompat detector) {
        this.mGestureDetector = detector;
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
        if (mGestureDetector != null)
            mGestureDetector.onTouchEvent(event);
        return false;
    }
}
