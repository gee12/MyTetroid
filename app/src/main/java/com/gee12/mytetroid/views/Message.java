package com.gee12.mytetroid.views;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.activities.LogsActivity;
import com.gee12.mytetroid.utils.ViewUtils;
import com.google.android.material.snackbar.Snackbar;

public class Message {

//    static BaseTransientBottomBar.Behavior behavior = new BaseTransientBottomBar.Behavior();
//
//    static {
//        behavior.setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_ANY);
//    }

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
     *
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
        showSnack(ViewUtils.getRootView(context), context.getString(textResId), duration, context.getString(titleResId), listener);
    }

    public static void showSnack(View view, @StringRes int textResId, int duration, @StringRes int titleResId,
                                 View.OnClickListener listener) {
        if (view == null)
            return;
        Context context = view.getContext();
        showSnack(view, context.getString(textResId), duration, context.getString(titleResId), listener);
    }

//    public static void showSnack(View view, String text, int duration, String title, View.OnClickListener listener) {
//        showSnack(view, text, duration, title, listener);
//    }

//    public static void showSnack(Context context, String text, int duration, String title, View.OnClickListener listener) {
    public static void showSnack(View view, String text, int duration, String title, View.OnClickListener listener) {
        final Snackbar snackbar = Snackbar.make(view, text, duration)
                .setAction(title, listener);
//                .setBehavior(behavior);
//        snackbar.getView().setOnClickListener(view2 -> snackbar.dismiss());
//        snackbar.setAnchorView();
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
                            // Tap or Else
                        }
                        break;
                }
                return false;
            }
        });*/
        snackbar.show();
    }

    /**
     * Вывод интерактивного уведомления SnackBar "Подробнее в логах".
     */
    public static void showSnackMoreInLogs(View view) {
        if (view == null)
            return;
        showSnackMoreInLogs(view.getContext(), view);
    }

    public static void showSnackMoreInLogs(Context context) {
        showSnackMoreInLogs(context, ViewUtils.getRootView(context));
    }

    public static void showSnackMoreInLogs(Context context, @IdRes int rootViewId) {
        showSnackMoreInLogs(context, ViewUtils.getRootView(context, rootViewId));
    }

    public static void showSnackMoreInLogs(Fragment fragment, @IdRes int rootViewId) {
        if (fragment == null)
            return;
        showSnackMoreInLogs(fragment.getContext(), ViewUtils.getRootView(fragment.getActivity(), rootViewId));
    }

    public static void showSnackMoreInLogs(Activity activity, @IdRes int rootViewId) {
        showSnackMoreInLogs(activity, ViewUtils.getRootView(activity, rootViewId));
    }

    public static void showSnackMoreInLogs(Context context, View view) {
        showSnack(view, R.string.title_more_in_logs, Snackbar.LENGTH_LONG,
                R.string.title_open, v -> LogsActivity.startLogsActivity(context));
    }
}
