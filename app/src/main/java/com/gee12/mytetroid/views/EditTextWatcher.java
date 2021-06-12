package com.gee12.mytetroid.views;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;

public abstract class EditTextWatcher {

    public EditTextWatcher(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onOnlyAfterTextChanged(s.toString());
            }
        });
    }

    public abstract void onOnlyAfterTextChanged(String text);
}
