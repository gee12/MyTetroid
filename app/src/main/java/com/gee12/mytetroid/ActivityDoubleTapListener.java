package com.gee12.mytetroid;


import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Обработчик двойного нажатия на экране.
 */
public class ActivityDoubleTapListener extends GestureDetector.SimpleOnGestureListener {

    private AppCompatActivity activity;

    public ActivityDoubleTapListener(AppCompatActivity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        App.toggleFullscreen(activity);
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }
}