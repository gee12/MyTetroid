package com.gee12.mytetroid.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
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
            // меняем расположение хранилища

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