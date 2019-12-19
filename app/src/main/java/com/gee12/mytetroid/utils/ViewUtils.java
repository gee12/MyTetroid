package com.gee12.mytetroid.utils;

import android.app.Activity;
import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class ViewUtils {

    public static void setEnabledIfNotNull(MenuItem view, boolean isEnabled) {
        if (view != null)
            view.setEnabled(isEnabled);
    }

    public static void setVisibleIfNotNull(MenuItem view, boolean isVisible) {
        if (view != null) {
            view.setVisible(isVisible);
        }
    }

    public static int getStatusBarHeight(Context context) {
        if (context == null)
            return 0;
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static void hideKeyboard(Context context, View view) {
        if (context == null || view == null)
            return;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
