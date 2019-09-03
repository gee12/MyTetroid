package com.gee12.mytetroid.views;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import com.gee12.mytetroid.R;

public class DateFormatPreference extends DialogPreference {

    private String dateTimeFormat;

    public DateFormatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.xml.pref_datetime_layout);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            this.dateTimeFormat = this.getPersistedString("???");
        } else {
            // Set default state from the XML attribute
            this.dateTimeFormat = (String) defaultValue;
            persistString(dateTimeFormat);
        }
    }

//    @Override
//    protected Object onGetDefaultValue(TypedArray a, int index) {
//        return a.getInteger(index, DEFAULT_VALUE);
//    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            persistString(dateTimeFormat);
        }
    }

    public void setDateTimeFormat(String dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
        persistString(dateTimeFormat);
    }

    public String getDateTimeFormat() {
        return dateTimeFormat;
    }
}
