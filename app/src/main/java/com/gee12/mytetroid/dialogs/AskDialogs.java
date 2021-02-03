package com.gee12.mytetroid.dialogs;

import android.content.Context;

import androidx.annotation.StringRes;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.R;

public class AskDialogs {

    public static void showReloadStorageDialog(Context context, boolean toCreate, boolean pathChanged,
                                               final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback,
                (toCreate) ? R.string.ask_create_storage_in_folder :
                        (pathChanged) ? R.string.ask_storage_path_was_changed : R.string.ask_reload_storage);
    }

    public static void showSyncDoneDialog(Context context, boolean isSyncSuccess, final Dialogs.IApplyResult callback) {
        int mesRes = (isSyncSuccess) ? R.string.ask_sync_success_dialog_request : R.string.ask_sync_failed_dialog_request;
        AskDialogs.showYesDialog(context, callback, mesRes);
    }

    public static void showSyncRequestDialog(Context context, final Dialogs.IApplyCancelResult callback) {
        AskDialogs.showYesNoDialog(context, callback, R.string.ask_start_sync_dialog_title);
    }

    public static void showExitDialog(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_exit_from_app);
    }

    /**
     *
     * @param context
     * @param callback
     * @param messageRes
     */
    public static void showYesNoDialog(Context context, final Dialogs.IApplyCancelResult callback,
                                       @StringRes int messageRes) {
        showYesNoDialog(context, callback, true, messageRes);
    }

    public static void showYesNoDialog(Context context, final Dialogs.IApplyCancelResult callback,
                                       boolean isCancelable, @StringRes int messageRes) {
        Dialogs.showAlertDialog(context, messageRes, true, isCancelable, callback);
    }

    /**
     *
     * @param context
     * @param callback
     * @param messageRes
     */
    public static void showYesDialog(Context context, final Dialogs.IApplyResult callback, @StringRes int messageRes) {
        Dialogs.showAlertDialog(context, context.getString(messageRes), true, true, callback);
    }

    /**
     *
     * @param context
     * @param callback
     * @param messageRes
     */
    public static void showOkDialog(Context context, final Dialogs.IApplyResult callback, @StringRes int messageRes) {
        Dialogs.showAlertDialog(context, messageRes, false, true, callback);
    }
}