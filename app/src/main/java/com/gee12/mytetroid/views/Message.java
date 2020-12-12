package com.gee12.mytetroid.views;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.gee12.mytetroid.utils.ViewUtils;
import com.google.android.material.behavior.SwipeDismissBehavior;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

public class Message {

    static BaseTransientBottomBar.Behavior behavior = new BaseTransientBottomBar.Behavior();

    static {
        behavior.setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_ANY);
    }

    /**
     * Отображение уведомления в виде Toast.
     * @param context
     * @param text
     */
    public static void show(Context context, String text) {
        show(context, text, Toast.LENGTH_LONG);
    }

    public static void show(Context context, String text, int duration) {
        Toast.makeText(context, text, duration).show();
    }

    /**
     * Отображение уведомления в виде Snackbar.
     * @param view
     * @param text
     */
    public static void showSnack(View view, String text) {
        showSnack(view, text, Snackbar.LENGTH_LONG);
    }

    public static void showSnack(View view, String text, int duration) {
        showSnack(view, text, duration, null, null);
    }

    public static void showSnack(Context context, @StringRes int textResId, int duration, @StringRes int titleResId,
                                 View.OnClickListener listener) {
        showSnack(context, context.getString(textResId), duration, context.getString(titleResId), listener);
    }

    public static void showSnack(View view, String text, int duration, String title, View.OnClickListener listener) {
        showSnack(view, text, duration, title, listener);
    }

    public static void showSnack(Context context, String text, int duration, String title, View.OnClickListener listener) {
        final Snackbar snackbar = Snackbar.make(ViewUtils.getRootView(context), text, duration)
                .setAction(title, listener);
//                .setBehavior(behavior);
        snackbar.getView().setOnClickListener(view -> snackbar.dismiss());
        /*Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
        layout.setOnTouchListener(new View.OnTouchListener() {
            private float x1, x2;
            static final int MIN_DISTANCE = 150;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1 = event.getX();
                        break;
                    case MotionEvent.ACTION_UP:
                        x2 = event.getX();
                        float deltaX = x2 - x1;
                        if (Math.abs(deltaX) > MIN_DISTANCE) {
                            // Left to Right swipe action
                            if (x2 > x1) {
                                snackbar.dismiss();
                            } else {
                            // Right to left swipe action
                                snackbar.dismiss();
                            }
                        }
                        else {
//                            // Tap or Else
                        }
                        break;
                }
                return false;
            }
        });*/
        snackbar.show();
    }
}
