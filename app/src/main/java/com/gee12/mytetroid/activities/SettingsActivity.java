package com.gee12.mytetroid.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.Utils;

import lib.folderpicker.FolderPicker;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int REQUEST_CODE_OPEN_STORAGE_PATH = 1;
    public static final int REQUEST_CODE_OPEN_TEMP_PATH = 2;
    public static final int REQUEST_CODE_OPEN_LOG_PATH = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);

        Preference storageFolderPicker = findPreference(getString(R.string.pref_key_storage_path));
        storageFolderPicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String path = SettingsManager.getStoragePath();
                if (Utils.isNullOrEmpty(path)) {
                    path = Utils.getExtPublicDocumentsDir();
                }
                SettingsManager.isAskReloadStorage = false;
                openFolderPicker(getString(R.string.folder_chooser_title),
                        path,
                        REQUEST_CODE_OPEN_STORAGE_PATH);
                return true;
            }
        });

        Preference tempFolderPicker = findPreference(getString(R.string.pref_key_temp_path));
        tempFolderPicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                openFolderPicker(getString(R.string.temp_path),
                        SettingsManager.getTempPath(),
                        REQUEST_CODE_OPEN_TEMP_PATH);
                return true;
            }
        });

        Preference logFolderPicker = findPreference(getString(R.string.pref_key_log_path));
        logFolderPicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                openFolderPicker(getString(R.string.log_path),
                        SettingsManager.getLogPath(),
                        REQUEST_CODE_OPEN_LOG_PATH);
                return true;
            }
        });
    }

    private void openFolderPicker(String title, String location, int requestCode) {
        Intent intent = new Intent(SettingsActivity.this, FolderPicker.class);
        intent.putExtra("title", title);
        intent.putExtra("location", location);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;
        String folderFullName = data.getStringExtra("data");
        if (requestCode == REQUEST_CODE_OPEN_STORAGE_PATH) {
            if (!folderFullName.equals(SettingsManager.getStoragePath())) {
                SettingsManager.isAskReloadStorage = true;
            }
            SettingsManager.setStoragePath(folderFullName);
        } else if (requestCode == REQUEST_CODE_OPEN_TEMP_PATH) {
            SettingsManager.setTempPath(folderFullName);
        } else if (requestCode == REQUEST_CODE_OPEN_LOG_PATH) {
            SettingsManager.setLogPath(folderFullName);
            LogManager.setLogPath(folderFullName);
        }
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
        if (key.equals(getString(R.string.pref_key_storage_path))) {
            // вызывается, когда задаем
            // удаляем хэш пароля, если поменяли хранилище
//            String newStoragePath = SettingsManager.getStoragePath();
//            if (Utils.isNullOrEmpty(newStoragePath) || !newStoragePath.equals(SettingsManager.LastStoragePath)) {
//                SettingsManager.setMiddlePassHash(null);
//            }

        } else if (key.equals(getString(R.string.pref_key_is_save_pass_hash_local))) {
            // удаляем хэш пароля, если сняли галку
            SettingsManager.setMiddlePassHash(null);
//        } else if (key.equals(getString(R.string.pref_key_record_fields_cols))) {
//            // меняем список полей для отображения

        } else if (key.equals(getString(R.string.pref_key_is_highlight_attach))) {
            // включаем/выключаем выделение записей с файлами
            SettingsManager.IsHighlightAttachCache = SettingsManager.isHighlightRecordWithAttach();

        } else if (key.equals(getString(R.string.pref_key_highlight_attach_color))) {
            // меняем цвет выделения записей с файлами
            SettingsManager.HighlightAttachColorCache = SettingsManager.getHighlightAttachColor();
        } else if (key.equals(getString(R.string.pref_key_date_format_string))) {
            // меняем формат даты
            SettingsManager.DateFormatStringCache = SettingsManager.getDateFormatString();
        } else if (key.equals(getString(R.string.pref_key_is_write_log))) {
            // меняем флаг
            LogManager.init(this, SettingsManager.getLogPath(), SettingsManager.isWriteLog());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SettingsManager.getSettings().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SettingsManager.getSettings().unregisterOnSharedPreferenceChangeListener(this);
    }
}