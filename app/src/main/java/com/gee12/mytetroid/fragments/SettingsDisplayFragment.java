package com.gee12.mytetroid.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.SettingsManager;

public class SettingsDisplayFragment extends TetroidSettingsFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.prefs_display, rootKey);

        getActivity().setTitle(R.string.pref_category_display);

        setHighlightPrefAvailability();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_key_is_highlight_attach))) {
            // включаем/выключаем выделение записей с файлами
            App.IsHighlightAttach = SettingsManager.isHighlightRecordWithAttach(mContext);
            setHighlightPrefAvailability();
        } else if (key.equals(getString(R.string.pref_key_is_highlight_crypted_nodes))) {
            // включаем/выключаем выделение зашифрованных веток
            App.IsHighlightCryptedNodes = SettingsManager.isHighlightEncryptedNodes(mContext);
            setHighlightPrefAvailability();
        } else if (key.equals(getString(R.string.pref_key_highlight_attach_color))) {
            // меняем цвет выделения записей с файлами
            App.HighlightAttachColor = SettingsManager.getHighlightColor(mContext);
        } else if (key.equals(getString(R.string.pref_key_date_format_string))) {
            // меняем формат даты
            App.DateFormatString = SettingsManager.getDateFormatString(mContext);
        }
    }

    private void setHighlightPrefAvailability() {
        findPreference(getString(R.string.pref_key_highlight_attach_color)).setEnabled(
                SettingsManager.isHighlightRecordWithAttach(mContext)
                        || SettingsManager.isHighlightEncryptedNodes(mContext));
    }
}
