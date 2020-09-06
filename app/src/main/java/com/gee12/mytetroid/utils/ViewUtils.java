package com.gee12.mytetroid.utils;

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
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class ViewUtils {

    public static int toVisibility(boolean isVisible) {
        return (isVisible) ? View.VISIBLE : View.GONE;
    }

    public static void setFabVisibility(FloatingActionButton fab, boolean isVisible) {
        if (fab == null)
            return;
        if (isVisible) fab.show();
        else fab.hide();
    }

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
     * Отображение клавиатуры.
     * @param context
     * @param view
     */
    public static void showKeyboard(Context context, View view, boolean isForced) {
        if (context == null)
            return;
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        int flags = (isForced) ? InputMethodManager.SHOW_FORCED : InputMethodManager.SHOW_IMPLICIT;
        if (view != null) {
            imm.showSoftInput(view, flags);
        } else
            imm.toggleSoftInput(flags, 0);
    }

    /**
     * Скрытие клавиатуры.
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
        // StatusBar
        /*View decorView = activity.getWindow().getDecorView();
        int visibility = (isFullscreen)
                ? View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                : 0;
        decorView.setSystemUiVisibility(
                visibility);*/
        int newUiOptions = 0;

        if (isFullscreen) {
            // The UI options currently enabled are represented by a bitfield.
            // getSystemUiVisibility() gives us that bitfield.
            int uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
            newUiOptions = uiOptions;

            // Navigation bar hiding:  Backwards compatible to ICS.
            if (Build.VERSION.SDK_INT >= 14) {
                newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }

            // Status bar hiding: Backwards compatible to Jellybean
            if (Build.VERSION.SDK_INT >= 16) {
                newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
            }

            // Immersive mode: Backward compatible to KitKat.
            // Note that this flag doesn't do anything by itself, it only augments the behavior
            // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
            // all three flags are being toggled together.
            // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
            // Sticky immersive mode differs in that it makes the navigation and status bars
            // semi-transparent, and the UI flag does not get cleared when the user interacts with
            // the screen.
            if (Build.VERSION.SDK_INT >= 18) {
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
}
