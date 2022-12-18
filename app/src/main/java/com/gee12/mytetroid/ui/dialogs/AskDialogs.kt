package com.gee12.mytetroid.ui.dialogs;

import android.content.Context;

import androidx.annotation.StringRes;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.R;

public class AskDialogs {

    public static void showLoadStorageDialog(Context context, String storageName, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, context.getString(R.string.ask_load_storage_mask, storageName));
    }

    public static void showCreateStorageFilesDialog(Context context, String storagePath, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, context.getString(R.string.ask_create_new_storage_mask, storagePath));
    }

    public static void showOpenStorageSettingsDialog(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, context.getString(R.string.ask_open_storage_settings));
    }

    public static void showReloadStorageDialog(Context context, boolean toCreate, boolean pathChanged,
                                               final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback,
                (toCreate) ? R.string.ask_create_storage_in_folder :
                        (pathChanged) ? R.string.ask_storage_path_was_changed : R.string.ask_reload_storage);
    }

    public static void showClearTrashDialog(Context context, final Dialogs.IApplyCancelResult callback) {
        AskDialogs.showYesNoDialog(context, callback, false, R.string.ask_clear_trash);
    }

    public static void showSyncDoneDialog(Context context, boolean isSyncSuccess, final Dialogs.IApplyResult callback) {
        int mesRes = (isSyncSuccess) ? R.string.ask_sync_success_dialog_request : R.string.ask_sync_failed_dialog_request;
        AskDialogs.showYesDialog(context, callback, mesRes);
    }

    public static void showSyncFailerBeforeExitDialog(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_sync_failed_dialog_request);
    }

    public static void showSyncRequestDialog(Context context, final Dialogs.IApplyCancelResult callback) {
        AskDialogs.showYesNoDialog(context, callback, false, R.string.ask_start_sync_dialog_title);
    }

    public static void showSyncRequestDialogAfterFailureSync(Context context, final Dialogs.IApplyCancelResult callback) {
        Dialogs.showAlertDialog(context, context.getString(R.string.ask_start_sync_or_exit_dialog_title),
                true, false, R.string.action_sync, R.string.action_exit, callback);
    }

    public static void showLoadAllNodesDialog(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_load_all_nodes_dialog_title);
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
    public static void showYesNoDialog(Context context, final Dialogs.IApplyCancelResult callback, @StringRes int messageRes) {
        showYesNoDialog(context, callback, true, messageRes);
    }

    public static void showYesNoDialog(Context context, final Dialogs.IApplyCancelResult callback, String message) {
        showYesNoDialog(context, callback, true, message);
    }

    public static void showYesNoDialog(Context context, final Dialogs.IApplyCancelResult callback, boolean isCancelable, @StringRes int messageRes) {
        Dialogs.showAlertDialog(context, messageRes, true, isCancelable, callback);
    }

    public static void showYesNoDialog(Context context, final Dialogs.IApplyCancelResult callback, boolean isCancelable, String message) {
        Dialogs.showAlertDialog(context, message, true, isCancelable, callback);
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
     * @param message
     */
    public static void showYesDialog(Context context, final Dialogs.IApplyResult callback, String message) {
        Dialogs.showAlertDialog(context, message, true, true, callback);
    }

    /**
     *
     * @param context
     * @param callback
     * @param messageRes
     */
    public static void showOkDialog(Context context, @StringRes int messageRes, @StringRes int applyRes, boolean isCancelable,
                                    final Dialogs.IApplyResult callback) {
        Dialogs.showAlertDialog(
                context,
                context.getString(messageRes),
                false,
                isCancelable,
                applyRes,
                R.string.cancel,
                Dialogs.getCancelCallback(callback)
        );
    }
}