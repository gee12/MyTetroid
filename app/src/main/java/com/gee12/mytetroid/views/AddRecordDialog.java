package com.gee12.mytetroid.views;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.model.TetroidRecord;
import com.lumyjuwon.richwysiwygeditor.WysiwygUtils.Keyboard;

public class AddRecordDialog {

    public interface INewRecordResult {
        void onApply(String name, String tags, String author, String url);
    }

    /**
     * Диалог создания/изменения записи.
     * @param context
     * @param handler
     */
    public static void createTextSizeDialog(Context context, TetroidRecord record, INewRecordResult handler) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_record);

        EditText etName = builder.getView().findViewById(R.id.edit_text_name);
        EditText etAuthor = builder.getView().findViewById(R.id.edit_text_author);
        EditText etUrl = builder.getView().findViewById(R.id.edit_text_url);
        EditText etTags = builder.getView().findViewById(R.id.edit_text_tags);

        if (record != null) {
            etName.setText(record.getName());
            etName.setSelection(0, etName.getText().length());
            etAuthor.setText(record.getAuthor());
            etUrl.setText(record.getUrl());
//            String tagsString = Jsoup.parse(record.getTagsString()).toString();
            String tagsString = record.getTagsString();
            etTags.setText(tagsString);
        }

        builder.setPositiveButton(R.string.answer_ok, (dialog1, which) -> {
            handler.onApply(etName.getText().toString(),
                    etAuthor.getText().toString(),
                    etUrl.getText().toString(),
                    etTags.getText().toString());
        }).setNegativeButton(R.string.answer_cancel, null);

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialog12 -> {
            // получаем okButton уже после вызова show()
            final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (TextUtils.isEmpty(etName.getText().toString())) {
                okButton.setEnabled(false);
            }
                Keyboard.showKeyboard(etName);
//            Keyboard.showKeyboard(builder.getView());
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
}
