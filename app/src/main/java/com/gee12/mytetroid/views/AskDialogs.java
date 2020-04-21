package com.gee12.mytetroid.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.StringRes;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.model.TetroidNode;

public class AskDialogs {

    public interface IApplyResult {
        void onApply();
    }

    public interface IApplyCancelResult extends IApplyResult {
        void onCancel();
    }

    public interface IPassInputResult {
        void applyPass(String pass, TetroidNode node);
        void cancelPass();
    }

    public interface IPassCheckResult {
        void onApply(TetroidNode node);
    }

    public static void showPassDialog(Context context, final TetroidNode node, final IPassInputResult passResult) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.title_pass_input));

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton(R.string.answer_ok, (dialog, which) -> {
            passResult.applyPass(input.getText().toString(), node);
        });
        builder.setNegativeButton(R.string.answer_cancel, (dialog, which) -> {
            dialog.cancel();
            passResult.cancelPass();
        });

        builder.show();
    }

    public static void showEmptyPassCheckingFieldDialog(Context context, String fieldName,
                        final TetroidNode node, final IPassCheckResult applyHandler) {

//        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
//            switch (which){
//                case DialogInterface.BUTTON_POSITIVE:
//                    applyHandler.onApply(node);
//                    break;
//            }
//        };
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setMessage(String.format(context.getString(R.string.log_empty_middle_hash_check_data_field), fieldName))
//                .setPositiveButton(R.string.answer_yes, (dialog, which) -> applyHandler.onApply(node))
//                .setNegativeButton(R.string.answer_no, null).show();
        showAlertDialog(context, String.format(context.getString(R.string.log_empty_middle_hash_check_data_field), fieldName),
                (dialog, which) -> applyHandler.onApply(node),
                null);
    }

    public static void showRequestWriteExtStorageDialog(Context context, final AskDialogs.IApplyResult applyHandler) {
        AskDialogs.showYesDialog(context, applyHandler, R.string.request_write_ext_storage);
    }

    public static void showRequestCameraDialog(Context context, final AskDialogs.IApplyResult applyHandler) {
        AskDialogs.showYesDialog(context, applyHandler, R.string.request_camera);
    }

    public static void showReloadStorageDialog(Context context, final IApplyResult applyHandler) {
        AskDialogs.showYesDialog(context, applyHandler, R.string.storage_path_was_changed);
    }

    public static void showSyncDoneDialog(Context context, boolean isSyncSuccess, final IApplyResult applyHandler) {
        int mesRes = (isSyncSuccess) ? R.string.log_sync_success_dialog_request : R.string.log_sync_failed_dialog_request;
        AskDialogs.showYesDialog(context, applyHandler, mesRes);
    }

    public static void showSyncRequestDialog(Context context, final IApplyCancelResult applyHandler) {
        AskDialogs.showYesNoDialog(context, applyHandler, R.string.start_sync_dialog_title);
    }

    public static void showExitDialog(Context context, final IApplyResult applyHandler) {
        AskDialogs.showYesDialog(context, applyHandler, R.string.exit_from_app);
    }

    /**
     *
     * @param context
     * @param applyHandler
     * @param messageRes
     */
    public static void showYesNoDialog(Context context, final IApplyCancelResult applyHandler, @StringRes int messageRes) {
//        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
//            if (which == DialogInterface.BUTTON_POSITIVE) {
//                applyHandler.onApply();
//            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
//                applyHandler.onCancel();
//            }
//        };
        showAlertDialog(context, messageRes,
                (dialog, which) -> applyHandler.onApply(),
                (dialog, which) -> applyHandler.onCancel());
    }

    /**
     *
     * @param context
     * @param applyHandler
     * @param messageRes
     */
    public static void showYesDialog(Context context, final IApplyResult applyHandler, @StringRes int messageRes) {
//        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
//            if (which == DialogInterface.BUTTON_POSITIVE) {
//                applyHandler.onApply();
//            }
//        };
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setMessage(messageRes)
//                .setPositiveButton(R.string.answer_yes, (dialog, which) -> applyHandler.onApply())
//                .setNegativeButton(R.string.answer_no, null).show();
        showAlertDialog(context, messageRes,
                (dialog, which) -> applyHandler.onApply(),
                null);
    }

    public static void showAlertDialog(Context context, int messageRes,
           DialogInterface.OnClickListener yesListener, DialogInterface.OnClickListener noListerener) {
        showAlertDialog(context, context.getString(messageRes), yesListener, noListerener);
    }

    public static void showAlertDialog(Context context, String message,
           DialogInterface.OnClickListener yesListener, DialogInterface.OnClickListener noListerener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setPositiveButton(R.string.answer_yes, yesListener)
                .setNegativeButton(R.string.answer_no, noListerener).show();
    }

}