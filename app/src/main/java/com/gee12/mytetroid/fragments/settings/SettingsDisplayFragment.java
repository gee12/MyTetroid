package com.gee12.mytetroid.fragments.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.RecordFieldsSelector;
import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.views.DateTimeFormatDialog;
import com.gee12.mytetroid.views.DateTimeFormatPreference;

public class SettingsDisplayFragment extends TetroidSettingsFragment {

    private static final String DIALOG_FRAGMENT_TAG = "DateTimeFormatPreference";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.prefs_display, rootKey);

        getActivity().setTitle(R.string.pref_category_display);

        setHighlightPrefAvailability();

        updateSummary(R.string.pref_key_show_record_fields, SettingsManager.getShowRecordFields(mContext));
        updateSummary(R.string.pref_key_record_fields_in_list,
                App.RecordFieldsInList.joinToString(getResources().getStringArray(R.array.record_fields_in_list_entries), 0));
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
        } else if (key.equals(getString(R.string.pref_key_show_record_fields))) {
            updateSummary(R.string.pref_key_show_record_fields, SettingsManager.getShowRecordFields(mContext));
        } else if (key.equals(getString(R.string.pref_key_record_fields_in_list))) {
            App.RecordFieldsInList = new RecordFieldsSelector(mContext, SettingsManager.getRecordFieldsInList(mContext));
            updateSummary(R.string.pref_key_record_fields_in_list,
                    App.RecordFieldsInList.joinToString(getResources().getStringArray(R.array.record_fields_in_list_entries), 0));
        }
    }

    private void setHighlightPrefAvailability() {
        findPreference(getString(R.string.pref_key_highlight_attach_color)).setEnabled(
                SettingsManager.isHighlightRecordWithAttach(mContext)
                        || SettingsManager.isHighlightEncryptedNodes(mContext));
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (getParentFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }
        if (preference instanceof DateTimeFormatPreference) {
            final DialogFragment f = DateTimeFormatDialog.newInstance(preference.getKey());
            f.setTargetFragment(this, 0);
            f.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }

    }
}
