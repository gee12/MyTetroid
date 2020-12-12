package com.gee12.mytetroid.fragments.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.SettingsManager;

public class SettingsSyncFragment extends TetroidSettingsFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.prefs_sync, rootKey);

        getActivity().setTitle(R.string.pref_category_sync);

        /*Preference syncPref = findPreference(getString(R.string.pref_key_is_sync_storage));
        syncPref.setOnPreferenceClickListener(pref -> {
            Message.show(getContext(), getString(R.string.log_func_disabled), Toast.LENGTH_SHORT);
            return true;
        });
        syncPref.setEnabled(false);*/

        updateSummary(R.string.pref_key_sync_command, SettingsManager.getSyncCommand(mContext));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_key_sync_command))) {
            updateSummary(R.string.pref_key_sync_command, SettingsManager.getSyncCommand(mContext));
        }
    }
}
