package com.gee12.mytetroid.views;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.preference.PreferenceDialogFragmentCompat;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.utils.Utils;

public class DateTimeFormatDialog extends PreferenceDialogFragmentCompat {

    private EditText mEditText;

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        DateTimeFormatPreference pref = (DateTimeFormatPreference) getPreference();
        if (pref == null) {
            return;
        }
        String value = pref.getValue();
        this.mEditText = view.findViewById(R.id.edit_text_value);
        mEditText.setText(value);
        mEditText.setSelection(value.length());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String newValue = mEditText.getText().toString();
            if (!TextUtils.isEmpty(newValue) && Utils.checkDateFormatString(newValue)) {
                // сохраняем значение опции
                DateTimeFormatPreference pref = (DateTimeFormatPreference) getPreference();
                if (pref != null) {
                    pref.setValue(newValue);
                }
            } else {
                Message.show(getContext(), getString(R.string.mes_input_wrong_date_format));
            }
        }
    }

    public static DateTimeFormatDialog newInstance(String key) {
        final DateTimeFormatDialog fragment = new DateTimeFormatDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }
}
