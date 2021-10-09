package com.gee12.mytetroid.views.dialogs;

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
import com.gee12.mytetroid.logs.LogOper;
import com.gee12.mytetroid.model.TetroidFile;
import com.lumyjuwon.richwysiwygeditor.Utils.Keyboard;

import java.util.Random;

@Deprecated
public class FileDialogs {

    public interface IFileFieldsResult {
        void onApply(String name);
    }

    /**
     * TODO: Перенести в DialogFragment.
     * Диалог создания/изменения файла.
     * @param context
     * @param handler
     */
    public static void createFileDialog(Context context, TetroidFile file, IFileFieldsResult handler) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_attach);

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

    public static void deleteFile(Context context, String fileName, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, context.getString(R.string.ask_file_delete_mask, fileName));
    }

    public static void deleteAttachWithoutFile(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_attach_delete_without_file);
    }

    public static void renameAttachWithoutFile(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_delete_attach_without_file);
    }

    public static void renameAttachWithoutDir(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_delete_record_without_dir);
    }

    public static void operWithoutFile(Context context, LogOper oper, final Dialogs.IApplyResult callback) {
        int resId = (oper == LogOper.DELETE) ? R.string.ask_attach_delete_without_file
                : R.string.ask_delete_attach_without_file;
        String mes = context.getString(resId);
        Dialogs.showAlertDialog(context, mes, true, true, callback);
    }

}
