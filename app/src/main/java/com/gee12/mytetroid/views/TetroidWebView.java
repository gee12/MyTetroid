package com.gee12.mytetroid.views;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;
import androidx.core.view.GestureDetectorCompat;

public class TetroidWebView extends WebView /*implements View.OnTouchListener*/ {

    private GestureDetectorCompat gestureDetector;

    public TetroidWebView(Context context) {
        super(context);
        init();
    }

    public TetroidWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TetroidWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TetroidWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


//    @Override
//    public boolean onTouch(View v, MotionEvent event) {
//        return gestureDetector.onTouchEvent(event);
//    }

    void init() {
        this.gestureDetector = new GestureDetectorCompat(getContext(), new TetroidGestureDetector());
//        setOnTouchListener(this);
    }

    private class TetroidGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            HitTestResult htResult = getHitTestResult();
            if (htResult == null || htResult.getExtra() == null)
            {
                return false;
            }
            return false;
        }

//        @Override
//        public boolean onSingleTapConfirmed(MotionEvent e) {
//            HitTestResult htResult = getHitTestResult();
//            if (htResult == null || htResult.getExtra() == null)
//            {
//            }
//            return false;
//        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }
    }
}
