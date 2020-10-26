package com.gee12.mytetroid.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;

import com.gee12.mytetroid.ILogger;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.SettingsManager;

import lib.folderpicker.FolderPicker;

import static android.app.Activity.RESULT_OK;

public class SettingsOtherFragment extends TetroidSettingsFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.prefs_other, rootKey);

        getActivity().setTitle(R.string.pref_category_other);

        Preference logFolderPicker = findPreference(getString(R.string.pref_key_log_path));
        logFolderPicker.setOnPreferenceClickListener(preference -> {
            if (!checkPermission(SettingsFragment.REQUEST_CODE_OPEN_LOG_PATH))
                return true;
            selectLogsFolder();
            return true;
        });

        updateSummary(R.string.pref_key_log_path, SettingsManager.getLogPath(mContext));
    }

    /**
     * Обработчик изменения настроек.
     * Чтобы работало нужно переопределить onResume() и onPause()
     * и дописать register/unregister настроек.
     * @param sharedPreferences
     * @param key
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_key_is_write_log))) {
            // меняем флаг
            LogManager.init(getContext(), SettingsManager.getLogPath(mContext), SettingsManager.isWriteLogToFile(mContext));
        }
    }

    public void onRequestPermissionsResult(boolean permGranted, int requestCode) {
        if (permGranted) {
            LogManager.log(mContext, R.string.log_write_ext_storage_perm_granted, ILogger.Types.INFO);
            switch (requestCode) {
                case SettingsFragment.REQUEST_CODE_OPEN_LOG_PATH:
                    selectLogsFolder();
                    break;
            }
        } else {
            LogManager.log(mContext, R.string.log_missing_write_ext_storage_permissions, ILogger.Types.WARNING, Toast.LENGTH_SHORT);
        }
    }

    public void onResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            return;
        String folderPath = data.getStringExtra(FolderPicker.EXTRA_DATA);
        if (requestCode == SettingsFragment.REQUEST_CODE_OPEN_LOG_PATH) {
            SettingsManager.setLogPath(mContext, folderPath);
            SettingsManager.setLastChoosedFolder(mContext, folderPath);
            LogManager.setLogPath(mContext, folderPath);
            updateSummary(R.string.pref_key_log_path, folderPath);
        }
    }

    private void selectLogsFolder() {
        openFolderPicker(getString(R.string.pref_log_path),
                SettingsManager.getLogPath(mContext),
                SettingsFragment.REQUEST_CODE_OPEN_LOG_PATH);
    }

    @Override
    public void onResume() {
        super.onResume();
        SettingsManager.getSettings().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        SettingsManager.getSettings().unregisterOnSharedPreferenceChangeListener(this);
    }
}
