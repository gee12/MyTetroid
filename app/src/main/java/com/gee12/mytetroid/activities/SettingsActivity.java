package com.gee12.mytetroid.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.gee12.mytetroid.SettingsManager;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_all);
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
        if (key.equals(getString(R.string.pref_key_is_save_last_date))) {
            // обнуляем последнюю дату, если сняли галочку
            if (!SettingsManager.isSaveLastDate()) {
                SettingsManager.setLastDate(0);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SettingsManager.getPrefs().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SettingsManager.getPrefs().unregisterOnSharedPreferenceChangeListener(this);
    }
}