package com.gee12.mytetroid.dialogs;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.BuildConfig;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.model.TetroidFile;
import com.lumyjuwon.richwysiwygeditor.WysiwygUtils.Keyboard;

import java.util.Random;

public class FileAskDialogs {

    public interface IFileFieldsResult {
        void onApply(String name);
    }

    /**
     * Диалог создания/изменения файла.
     * @param context
     * @param handler
     */
    public static void createFileDialog(Context context, TetroidFile file, IFileFieldsResult handler) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_file);

        EditText etName = builder.getView().findViewById(R.id.edit_text_name);

        if (BuildConfig.DEBUG && file == null) {
            Random rand = new Random();
            int num = Math.abs(rand.nextInt());
            etName.setText("file " + num + ".test");
        }

        if (file != null) {
            etName.setText(file.getName());
        }

        builder.setPositiveButton(R.string.answer_ok, (dialog1, which) -> {
            handler.onApply(etName.getText().toString());
        }).setNegativeButton(R.string.answer_cancel, null);

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialog12 -> {
            // получаем okButton уже после вызова show()
            final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (TextUtils.isEmpty(etName.getText().toString())) {
                okButton.setEnabled(false);
                Keyboard.showKeyboard(etName);
            }
            etName.setSelection(etName.getText().length());
//            Keyboard.showKeyboard(etName);
        });
        dialog.show();

        // получаем okButton тут отдельно после вызова show()
        final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) {
                okButton.setEnabled(!TextUtils.isEmpty(s));
            }
        });
    }

    public static void deleteFile(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_file_delete);
    }

/*    public static void deleteAttachWithoutDir(Context context, TetroidLog.Opers oper, final AskDialogs.IApplyResult callback) {
        int resId = (oper == TetroidLog.Opers.DELETE) ? R.string.title_delete
                : (oper == TetroidLog.Opers.CUT) ? R.string.title_cut
                : R.string.title_insert;
        String mes = String.format(context.getString(R.string.ask_record_oper_without_dir_mask),
                context.getString(resId));
//        AskDialogs.showYesDialog(context, callback, R.string.log_record_delete_without_dir);
        AskDialogs.showAlertDialog(context, mes,
                (dialog, which) -> callback.onApply(),
                null);
    }*/

    public static void deleteAttachWithoutFile(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_attach_delete_without_file);
    }

    public static void renameAttachWithoutDir(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_delete_record_without_dir);
    }

    public static void renameAttachWithoutFile(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_delete_attach_without_file);
    }
}
