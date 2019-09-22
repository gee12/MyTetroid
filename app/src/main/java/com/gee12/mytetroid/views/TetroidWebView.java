package com.gee12.mytetroid.views;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;

public class TetroidWebView extends WebView {

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

    private void init() {
    }
}
