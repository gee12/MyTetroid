package com.gee12.mytetroid.common.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.annotation.IdRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class ViewUtils {

    public static void setFabVisibility(FloatingActionButton fab, boolean isVisible) {
        if (fab == null)
            return;
        if (isVisible) fab.show();
        else fab.hide();
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
     * Установка полноэкранного режима.
     * @param isFullscreen Если true, то mToolbar исчезает и в опциях DecorView устанавливаются нужные флаги
     *                     для полноэкранного режима, иначе все флаги сбрасываются.
     */
    public static void setFullscreen(AppCompatActivity activity, boolean isFullscreen) {
        int newUiOptions = 0;

        if (isFullscreen) {
            newUiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();

            // отображение контента под системными панелями
            if (Build.VERSION.SDK_INT >= 16) {
                newUiOptions ^= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            }

            // скрытие панели навигации. Для совместимости с Ice Cream Sandwich.
            if (Build.VERSION.SDK_INT >= 14) {
                newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }

            // скрытие панели состояния. Для совместимости с Jellybean
            if (Build.VERSION.SDK_INT >= 16) {
                newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
            }

            // "липкий" полноэкранный режим
            if (Build.VERSION.SDK_INT >= 19) {
                newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
//            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE;
            }
        }
        activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);

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
    }

    public static void hideSystemUI(AppCompatActivity activity) {
        View mDecorView = activity.getWindow().getDecorView();
        mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    public static void showSystemUI(AppCompatActivity activity) {
        View mDecorView = activity.getWindow().getDecorView();
        mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    /**
     * Установка блокировки выключения экрана.
     * @param activity
     * @param isKeep
     */
    public static void setKeepScreenOn(Activity activity, boolean isKeep) {
        if (isKeep)
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     *
     * @param view
     * @param runnable
     */
    public static void setOnGlobalLayoutListener(View view, Runnable runnable) {
        final ViewTreeObserver vto = view.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (view.getMeasuredWidth() > 0) {
                        // заново получаем ViewTreeObserver, т.к.  vto != newVto
                        ViewTreeObserver newVto = view.getViewTreeObserver();
                        if (Build.VERSION.SDK_INT < 16) {
                            newVto.removeGlobalOnLayoutListener(this);
                        } else {
                            newVto.removeOnGlobalLayoutListener(this);
                        }
                        // запускаем свой код
                        runnable.run();
                    }
                }
            });
        }
    }

    /**
     * Makes sure the view (and any children) get the enabled state changed.
     */
    public static void setEnabledStateOnViews(View v, boolean enabled) {
        if (v == null)
            return;
        v.setEnabled(enabled);

        if (v instanceof ViewGroup) {
            final ViewGroup vg = (ViewGroup) v;
            for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                setEnabledStateOnViews(vg.getChildAt(i), enabled);
            }
        }
    }

    /**
     * Get root view of activity from context.
     * @param context
     * @return
     */
    public static View getRootView(Context context) {
        final Activity activity = (Activity) context;
        if (activity == null) {
            return null;
        }
        View rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) {
            rootView = activity.getWindow().getDecorView().getRootView();
        }
        return rootView;
    }

    public static View getRootView(Context context, @IdRes int resId) {
        final Activity activity = (Activity) context;
        if (activity == null) {
            return null;
        }
        View rootView = activity.findViewById(resId);
        if (rootView == null) {
            rootView = getRootView(context);
        }
        return rootView;
    }
}
