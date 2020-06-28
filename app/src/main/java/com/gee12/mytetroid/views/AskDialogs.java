package com.gee12.mytetroid.views;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.htmlwysiwygeditor.ViewUtils;
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

    public interface IPassChangeResult {
        boolean applyPass(String curPass, String newPass);
    }

    /*public interface IPassCheckResult {
        void onApply(TetroidNode node);
    }*/

    /**
     *
     * @param context
     * @param node
     * @param isNewPass
     * @param passResult
     */
    public static void showPassEnterDialog(Context context, final TetroidNode node, boolean isNewPass, final IPassInputResult passResult) {
        if (isNewPass) {
            showPassSetupDialog(context, node, passResult);
        } else {
            showPassEnterDialog(context, node, passResult);
        }
    }

    /**
     *
     * @param context
     * @param node
     * @param passResult
     */
    public static void showPassEnterDialog(Context context, final TetroidNode node, final IPassInputResult passResult) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.title_password_enter));
        builder.setPositiveButton(R.string.answer_ok, null);
        builder.setNegativeButton(R.string.answer_cancel, null);

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton(R.string.answer_ok, (dialog1, which) -> {
            passResult.applyPass(input.getText().toString(), node);
        });
        builder.setNegativeButton(R.string.answer_cancel, (dialog1, which) -> {
            passResult.cancelPass();
        });

        final AlertDialog dialog = builder.create();
        // проверка на пустоту пароля
        input.addTextChangedListener(new ViewUtils.TextChangedListener(newText -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!(TextUtils.isEmpty(newText)));
        }));

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        });

        dialog.show();
    }

    /**
     *
     * @param context
     * @param node
     * @param passResult
     */
    public static void showPassSetupDialog(Context context, final TetroidNode node, final IPassInputResult passResult) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_pass_setup);
        builder.setTitle(context.getString(R.string.title_password_set));
        builder.setPositiveButton(R.string.answer_ok, null);
        builder.setNegativeButton(R.string.answer_cancel, null);

        final EditText tvPass = builder.getView().findViewById(R.id.edit_text_pass);
        EditText tvConfirmPass = builder.getView().findViewById(R.id.edit_text_confirm_pass);

        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        // проверка на пустоту паролей
        ViewUtils.TextChangedListener listener = new ViewUtils.TextChangedListener(newText -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!(TextUtils.isEmpty(newText)
                    || tvPass.getText().length() == 0
                    || tvConfirmPass.getText().length() == 0));
        });
        tvPass.addTextChangedListener(listener);
        tvConfirmPass.addTextChangedListener(listener);

        dialog.setOnShowListener(dialogInterface -> {
            Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            okButton.setEnabled(false);
            okButton.setOnClickListener(view -> {
                String pass = tvPass.getText().toString();
                String confirmPass = tvConfirmPass.getText().toString();
                // проверка совпадения паролей
                if (!pass.contentEquals(confirmPass)) {
                    Toast.makeText(context, context.getString(R.string.log_pass_confirm_not_match), Toast.LENGTH_SHORT).show();
                    return;
                }
                passResult.applyPass(pass, node);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    /**
     *
     * @param context
     * @param passResult
     */
    public static void showPassChangeDialog(Context context, final IPassChangeResult passResult) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_pass_change);
        builder.setTitle(context.getString(R.string.title_password_change));
        builder.setPositiveButton(R.string.answer_ok, null);
        builder.setNegativeButton(R.string.answer_cancel, null);

        EditText tvCurPass = builder.getView().findViewById(R.id.edit_text_cur_pass);
        EditText tvNewPass = builder.getView().findViewById(R.id.edit_text_new_pass);
        EditText tvConfirmPass = builder.getView().findViewById(R.id.edit_text_confirm_pass);

        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        // проверка на пустоту паролей
        ViewUtils.TextChangedListener listener = new ViewUtils.TextChangedListener(newText -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!(TextUtils.isEmpty(newText)
                || tvCurPass.getText().length() == 0
                || tvNewPass.getText().length() == 0
                || tvConfirmPass.getText().length() == 0));
        });
        tvCurPass.addTextChangedListener(listener);
        tvNewPass.addTextChangedListener(listener);
        tvConfirmPass.addTextChangedListener(listener);

        dialog.setOnShowListener(dialogInterface -> {
            Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            okButton.setEnabled(false);
            okButton.setOnClickListener(view -> {
                String curPass = tvCurPass.getText().toString();
                String newPass = tvNewPass.getText().toString();
                String confirmPass = tvConfirmPass.getText().toString();
                // проверка совпадения паролей
                if (!newPass.contentEquals(confirmPass)) {
                    Toast.makeText(context, context.getString(R.string.log_pass_confirm_not_match), Toast.LENGTH_SHORT).show();
                    return;
                }
                // проверка текущего пароля
                if (passResult.applyPass(curPass, newPass)) {
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    public static void showEmptyPassCheckingFieldDialog(Context context, String fieldName, final IApplyCancelResult applyHandler) {
        Dialogs.showAlertDialog(context, String.format(context.getString(R.string.log_empty_middle_hash_check_data_field), fieldName),
                (dialog, which) -> applyHandler.onApply(),
                (dialog, which) -> applyHandler.onCancel());
    }

    public static void showReloadStorageDialog(Context context, boolean isCreate, final IApplyResult applyHandler) {
        AskDialogs.showYesDialog(context, applyHandler,
                (isCreate) ? R.string.ask_storage_folder_clear : R.string.ask_storage_path_was_changed);
    }

    public static void showSyncDoneDialog(Context context, boolean isSyncSuccess, final IApplyResult applyHandler) {
        int mesRes = (isSyncSuccess) ? R.string.ask_sync_success_dialog_request : R.string.ask_sync_failed_dialog_request;
        AskDialogs.showYesDialog(context, applyHandler, mesRes);
    }

    public static void showSyncRequestDialog(Context context, final IApplyCancelResult applyHandler) {
        AskDialogs.showYesNoDialog(context, applyHandler, R.string.ask_start_sync_dialog_title);
    }

    public static void showExitDialog(Context context, final IApplyResult applyHandler) {
        AskDialogs.showYesDialog(context, applyHandler, R.string.ask_exit_from_app);
    }

    /**
     *
     * @param context
     * @param applyHandler
     * @param messageRes
     */
    public static void showYesNoDialog(Context context, final IApplyCancelResult applyHandler, @StringRes int messageRes) {
        Dialogs.showAlertDialog(context, messageRes,
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
        Dialogs.showAlertDialog(context, messageRes,
                (dialog, which) -> applyHandler.onApply(),
                null);
    }

}