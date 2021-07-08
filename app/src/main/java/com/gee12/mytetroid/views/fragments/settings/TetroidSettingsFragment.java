package com.gee12.mytetroid.views.fragments.settings;

import android.app.Application;
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
import com.gee12.mytetroid.views.activities.TetroidSettingsActivity;

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

    public void onSharedPreferenceChanged(String key) {
        onSharedPreferenceChanged(SettingsManager.getSettings(getContext()), key);
    }

    @Override
    public void onResume() {
        super.onResume();
        SettingsManager.getSettings(getContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        SettingsManager.getSettings(getContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    protected void openFolderPicker(String title, String location, int requestCode) {
        String path = (!StringUtil.isBlank(location)) ? location : DataManager.getLastFolderPathOrDefault(getContext(), true);
        Intent intent = new Intent(getContext(), FolderPicker.class);
        intent.putExtra(FolderPicker.EXTRA_TITLE, title);
        intent.putExtra(FolderPicker.EXTRA_LOCATION, path);
//        Intent intent = new Intent(getContext(), FolderChooser.class);
//        intent.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
//        intent.putExtra(Constants.INITIAL_DIRECTORY, path);
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
        updateSummary(getString(keyStringRes), value);
    }

    protected void updateSummary(String key, String value) {
        if (!StringUtil.isBlank(value)) {
            Preference pref = findPreference(key);
            if (pref != null)
                pref.setSummary(value);
        }
    }

    protected void updateSummary(@StringRes int keyStringRes, String value, String defValue) {
        updateSummary(getString(keyStringRes), value, defValue);
    }

    protected void updateSummary(String key, String value, String defValue) {
        Preference pref = findPreference(key);
        if (pref == null)
            return;
        if (!StringUtil.isBlank(value)) {
            pref.setSummary(value);
        } else {
            pref.setSummary(defValue);
        }
    }

    protected void updateSummaryIfContains(@StringRes int keyStringRes, String value) {
        if (SettingsManager.isContains(getContext(), keyStringRes)) {
            updateSummary(keyStringRes, value);
        }
    }

    protected void refreshPreferences() {
        setPreferenceScreen(null);
        addPreferencesFromResource(R.xml.prefs);
    }

    public void setTitle(int titleResId, String subtitle) {
        TetroidSettingsActivity activity = (TetroidSettingsActivity)getActivity();
        if (activity != null) {
            activity.setTitle(titleResId);
            activity.setSubTitle(subtitle);
        }
    }

    /**
     * Вывод интерактивного уведомления SnackBar "Подробнее в логах".
     */
    protected void showSnackMoreInLogs() {
        Message.showSnackMoreInLogs(this, R.id.layout_coordinator);
    }

    protected Application getApplication() {
        return (Application) getContext().getApplicationContext();
    }
}
