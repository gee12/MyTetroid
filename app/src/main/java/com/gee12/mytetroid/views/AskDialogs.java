package com.gee12.mytetroid.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.TetroidNode;

public class AskDialogs {

    public interface IApplyResult {
        void onApply();
    }

    public interface IApplyCancelResult {
        void onApply();
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

        builder.setPositiveButton(R.string.answer_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                passResult.applyPass(input.getText().toString(), node);
            }
        });
        builder.setNegativeButton(R.string.answer_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                passResult.cancelPass();
            }
        });

        builder.show();
    }

    public static void showEmptyPassCheckingFieldDialog(Context context, String fieldName,
                        final TetroidNode node, final IPassCheckResult applyHandler) {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        applyHandler.onApply(node);
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(String.format(context.getString(R.string.empty_middle_hash_check_data_field), fieldName))
                .setPositiveButton(R.string.answer_yes, dialogClickListener)
                .setNegativeButton(R.string.answer_no, dialogClickListener).show();
    }

    public static void showReloadStorageDialog(Context context, final IApplyCancelResult applyHandler) {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        applyHandler.onApply();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        applyHandler.onCancel();
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.storage_path_was_changed)
                .setPositiveButton(R.string.answer_yes, dialogClickListener)
                .setNegativeButton(R.string.answer_no, dialogClickListener).show();
    }

    public static void showSyncDoneDialog(Context context, boolean isSyncSuccess, final IApplyResult applyHandler) {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    applyHandler.onApply();
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        int mesRes = (isSyncSuccess) ? R.string.sync_success_dialog_request : R.string.sync_failed_dialog_request;
        builder.setMessage(mesRes)
                .setPositiveButton(R.string.answer_yes, dialogClickListener)
                .setNegativeButton(R.string.answer_no, dialogClickListener).show();
    }

    public static void showSyncRequestDialog(Context context, final IApplyCancelResult applyHandler) {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    applyHandler.onApply();
                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                    applyHandler.onCancel();
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getString(R.string.start_sync_dialog_title))
                .setPositiveButton(R.string.answer_yes, dialogClickListener)
                .setNegativeButton(R.string.answer_no, dialogClickListener).show();
    }

    public static void showExitDialog(Context context, final IApplyResult applyHandler) {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    applyHandler.onApply();
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.exit_from_app)
                .setPositiveButton(R.string.answer_yes, dialogClickListener)
                .setNegativeButton(R.string.answer_no, dialogClickListener).show();
    }
}