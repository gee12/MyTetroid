package com.gee12.mytetroid.ui.views.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.gee12.mytetroid.R;

public class DateTimeFormatPreference extends DialogPreference {

    private String value;

    public DateTimeFormatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setValue(restorePersistedValue ?
                getPersistedString(value) : (String) defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.xml.pref_datetime_layout;
    }

    public void setValue(String value) {
        this.value = value;
        persistString(value);
    }

    public String getValue() {
        return value;
    }
}
