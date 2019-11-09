package com.gee12.mytetroid.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import com.gee12.mytetroid.Message;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.Utils;

public class DateTimeFormatPreference extends DialogPreference {

    private String value;
    private String newValue;

    public DateTimeFormatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.xml.pref_datetime_layout);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);
    }

    @Override
    protected View onCreateDialogView() {
        View view = super.onCreateDialogView();
        EditText et = view.findViewById(R.id.edit_text_value);
        et.setText(value);
        et.setSelection(value.length());
        et.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                DateTimeFormatPreference.this.newValue = s.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        return view;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            this.value = this.getPersistedString(getContext().getString(R.string.def_date_format_string));
        } else {
            // Set default state from the XML attribute
            setValue((String) defaultValue);
        }
        this.newValue = value;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            if (!TextUtils.isEmpty(newValue) && Utils.checkDateFormatString(newValue)) {
                setValue(newValue);
            } else {
                Message.show(getContext(), "Введена некорректная строка формата даты/времени");
            }
        }
    }

    public void setValue(String value) {
        this.value = value;
        persistString(value);
    }

    public String getValue() {
        return value;
    }
}
