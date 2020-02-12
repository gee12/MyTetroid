package com.gee12.mytetroid.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;


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

    /**
     *
     * @param context
     * @param view
     */
    public static void showKeyboard(Context context, View view) {
        if (context == null)
            return;
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (view != null)
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        else
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    }

    /**
     *
     * @param context
     * @param view
     */
    public static void hideKeyboard(Context context, View view) {
        if (context == null)
            return;
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (view != null)
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        else
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    /**
     * Установка полноэкранного режима.
     * @param isFullscreen Если true, то mToolbar исчезает и в опциях SystemUiVisibility устанавливаются нужные флаги
     *                     для полноэкранного режима, иначе все флаги сбрасываются.
     */
    public static void setFullscreen(AppCompatActivity activity, boolean isFullscreen) {
//        this.isFullscreen = isFullscreen;

        // StatusBar
        View decorView = activity.getWindow().getDecorView();
        int visibility = (isFullscreen)
                ? View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                : 0;
        decorView.setSystemUiVisibility(
                visibility);
        // ToolBar
        try {
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                if (isFullscreen)
                    actionBar.hide();
                else
                    actionBar.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // панель с полями записи
//        viewPagerAdapter.getMainFragment().setFullscreen(isFullscreen);
    }

    /**
     *
     * @param activity
     * @param cls
     * @param bundle
     */
    public static void startActivity(Activity activity, Class<?> cls, Bundle bundle, String action, int flags, Integer requestCode) {
        Intent intent = new Intent(activity, cls);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        if (action != null) {
            intent.setAction(action);
        }
        if (flags != 0) {
            intent.setFlags(flags);
        }
        if (requestCode == null)
            activity.startActivity(intent);
        else
            activity.startActivityForResult(intent, requestCode);
    }

    public static void startActivity(Activity activity, Class<?> cls, Bundle bundle, int requestCode) {
        startActivity(activity, cls, bundle, null, 0, requestCode);
    }

    public static void startActivity(Activity activity, Class<?> cls, Bundle bundle) {
        startActivity(activity, cls, bundle, null, 0, null);
    }

    /**
     *
     * @param context
     * @param cls
     * @param bundle
     */
    public static Intent createIntent(Context context, Class<?> cls, Bundle bundle, String action) {
        Intent intent = new Intent(context, cls);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        if (action != null) {
            intent.setAction(action);
        }
        return intent;
    }

}
