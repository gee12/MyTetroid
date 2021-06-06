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
import com.gee12.mytetroid.model.TetroidTag;
import com.lumyjuwon.richwysiwygeditor.Utils.Keyboard;

import java.util.Random;

public class TagDialogs {

    public interface ITagFieldsResult {
        void onApply(String name);
    }

    /**
     * Диалог изменения имени метки.
     * @param context
     * @param handler
     */
    public static void createTagDialog(Context context, TetroidTag tag, ITagFieldsResult handler) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_tag);

        EditText etName = builder.getView().findViewById(R.id.edit_text_name);

        if (BuildConfig.DEBUG && tag == null) {
            Random rand = new Random();
            int num = Math.abs(rand.nextInt());
            etName.setText("tag " + num + ".test");
        }

        if (tag != null) {
            etName.setText(tag.getName().toLowerCase());
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
