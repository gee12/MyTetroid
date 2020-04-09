package com.gee12.mytetroid.views;

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
import com.gee12.mytetroid.model.TetroidRecord;

import java.util.Random;

public class RecordAskDialogs {

    public interface IRecordFieldsResult {
        void onApply(String name, String tags, String author, String url);
    }

    /**
     * Диалог создания/изменения записи.
     * @param context
     * @param handler
     */
    public static void createRecordFieldsDialog(Context context, TetroidRecord record, IRecordFieldsResult handler) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_record);

        EditText etName = builder.getView().findViewById(R.id.edit_text_name);
        EditText etAuthor = builder.getView().findViewById(R.id.edit_text_author);
        EditText etUrl = builder.getView().findViewById(R.id.edit_text_url);
        EditText etTags = builder.getView().findViewById(R.id.edit_text_tags);

        if (BuildConfig.DEBUG && record == null) {
            Random rand = new Random();
            int num = Math.abs(rand.nextInt());
            etName.setText("record " + num);
            etAuthor.setText("author " + num);
            etUrl.setText("http://url" + num + ".com");
            etTags.setText("new record , tag " + num);
        }

        if (record != null) {
            etName.setText(record.getName());
            etAuthor.setText(record.getAuthor());
            etUrl.setText(record.getUrl());
//            String tagsString = Jsoup.parse(record.getTagsString()).toString();
            String tagsString = record.getTagsString();
            etTags.setText(tagsString);
        }

        builder.setPositiveButton(R.string.answer_ok, (dialog1, which) -> {
            handler.onApply(etName.getText().toString(),
                    etTags.getText().toString(),
                    etAuthor.getText().toString(),
                    etUrl.getText().toString());
        }).setNegativeButton(R.string.answer_cancel, null);

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialog12 -> {
            // получаем okButton уже после вызова show()
            final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (TextUtils.isEmpty(etName.getText().toString())) {
                okButton.setEnabled(false);
//                Keyboard.showKeyboard(etName);
            }
//            etName.setSelection(0, etName.getText().length());
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

    public static void saveRecord(Context context, final AskDialogs.IApplyCancelResult applyHandler) {
        AskDialogs.showYesNoDialog(context, applyHandler, R.string.save_record);
    }

    public static void deleteRecord(Context context, final AskDialogs.IApplyResult applyHandler) {
        AskDialogs.showYesDialog(context, applyHandler, R.string.record_delete);
    }

    public static void deleteRecordWithoutDir(Context context, final AskDialogs.IApplyResult applyHandler) {
        AskDialogs.showYesDialog(context, applyHandler, R.string.log_record_delete_without_dir);
    }
}
