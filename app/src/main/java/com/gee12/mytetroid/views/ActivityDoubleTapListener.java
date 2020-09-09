package com.gee12.mytetroid.views;


import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Обработчик двойного нажатия на экране.
 */
public class ActivityDoubleTapListener extends GestureDetector.SimpleOnGestureListener {

    public interface IHandler {
        void onDoubleTap();
    }

    private IHandler handler;

    public ActivityDoubleTapListener(IHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (handler != null)
            handler.onDoubleTap();
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }
}