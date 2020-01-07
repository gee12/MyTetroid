package com.gee12.mytetroid.utils;

import android.app.Activity;
import android.content.Context;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;


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
        if (context == null || view == null)
            return;
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     *
     * @param context
     * @param view
     */
    public static void hideKeyboard(Context context, View view) {
        if (context == null || view == null)
            return;
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     *
     * @param editText
     */
    public static void disableCopyAndPaste(EditText editText) {
        if (editText == null)
            return;
        if (android.os.Build.VERSION.SDK_INT < 11) {
            editText.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                public void onCreateContextMenu(ContextMenu menu, View v,
                                                ContextMenu.ContextMenuInfo menuInfo) {
                    menu.clear();
                }
            });
        } else {
            editText.setCustomSelectionActionModeCallback(new ActionMode.Callback() {

                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                public void onDestroyActionMode(ActionMode mode) {
                }

                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                public boolean onActionItemClicked(ActionMode mode,
                                                   MenuItem item) {
                    return false;
                }
            });
        }
    }
}
