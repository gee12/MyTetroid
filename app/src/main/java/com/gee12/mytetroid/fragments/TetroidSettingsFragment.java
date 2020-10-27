package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.PermissionManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.views.Message;

import org.jsoup.internal.StringUtil;

import lib.folderpicker.FolderPicker;

public class TetroidSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    protected Context mContext;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        this.mContext = getContext();
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

    protected void openFolderPicker(String title, String location, int requestCode) {
        String path = (!StringUtil.isBlank(location)) ? location : DataManager.getLastFolderOrDefault(getContext(), true);
        Intent intent = new Intent(getContext(), FolderPicker.class);
        intent.putExtra(FolderPicker.EXTRA_TITLE, title);
        intent.putExtra(FolderPicker.EXTRA_LOCATION, path);
        getActivity().startActivityForResult(intent, requestCode);
    }

    protected boolean checkPermission(int requestCode) {
        return PermissionManager.checkWriteExtStoragePermission(getActivity(), requestCode);
    }

    /**
     * Деактивация опции, если версия приложения Free.
     * @param pref
     */
    protected void disableIfFree(Preference pref) {
        if (App.isFullVersion()) {
            pref.setEnabled(true);
        } else {
            pref.setEnabled(false);
            // принудительно отключаем
            pref.setOnPreferenceClickListener(pref1 -> {
                Message.show(getContext(), getString(R.string.title_available_in_pro), Toast.LENGTH_SHORT);
                return true;
            });
            pref.setDependency(null);
        }
    }

    protected void updateSummary(@StringRes int keyStringRes, String value) {
        if (!StringUtil.isBlank(value)) {
            Preference pref = findPreference(getString(keyStringRes));
            if (pref != null)
                pref.setSummary(value);
        }
    }

    protected void refreshPreferences() {
        setPreferenceScreen(null);
        addPreferencesFromResource(R.xml.prefs);
    }
}
