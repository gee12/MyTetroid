package com.gee12.mytetroid.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.crypt.CryptManager;
import com.gee12.mytetroid.data.DataManager;

import org.jsoup.internal.StringUtil;

import lib.folderpicker.FolderPicker;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int REQUEST_CODE_OPEN_STORAGE_PATH = 1;
    public static final int REQUEST_CODE_OPEN_TEMP_PATH = 2;
    public static final int REQUEST_CODE_OPEN_LOG_PATH = 3;

    private AppCompatDelegate mDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.prefs);

        Preference storageFolderPicker = findPreference(getString(R.string.pref_key_storage_path));
        storageFolderPicker.setOnPreferenceClickListener(preference -> {
//            String path = SettingsManager.getStoragePath();
//            if (StringUtil.isBlank(path)) {
//                path = FileUtils.getExternalPublicDocsDir();
//            }
            SettingsManager.isAskReloadStorage = false;
            openFolderPicker(getString(R.string.title_storage_folder),
                    SettingsManager.getStoragePath(),
                    REQUEST_CODE_OPEN_STORAGE_PATH);
            return true;
        });

        Preference tempFolderPicker = findPreference(getString(R.string.pref_key_temp_path));
        tempFolderPicker.setOnPreferenceClickListener(preference -> {
            openFolderPicker(getString(R.string.temp_path),
                    SettingsManager.getTempPath(),
                    REQUEST_CODE_OPEN_TEMP_PATH);
            return true;
        });

        Preference logFolderPicker = findPreference(getString(R.string.pref_key_log_path));
        logFolderPicker.setOnPreferenceClickListener(preference -> {
            openFolderPicker(getString(R.string.log_path),
                    SettingsManager.getLogPath(),
                    REQUEST_CODE_OPEN_LOG_PATH);
            return true;
        });

        updateSummary(R.string.pref_key_storage_path, SettingsManager.getStoragePath());
        updateSummary(R.string.pref_key_temp_path, SettingsManager.getTempPath());
        updateSummary(R.string.pref_key_sync_command, SettingsManager.getSyncCommand());
        updateSummary(R.string.pref_key_log_path, SettingsManager.getLogPath());
    }

    private void updateSummary(@StringRes int keyStringRes, String value) {
        if (!StringUtil.isBlank(value)) {
            Preference pref = findPreference(getString(keyStringRes));
            if (pref != null)
                pref.setSummary(value);
        }
    }

    private void openFolderPicker(String title, String location, int requestCode) {
        String path = (!StringUtil.isBlank(location)) ? location : DataManager.getLastFolderOrDefault(this, true);
        Intent intent = new Intent(SettingsActivity.this, FolderPicker.class);
        intent.putExtra("title", title);
        intent.putExtra("location", path);
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
            SettingsManager.setLastChoosedFolder(folderFullName);
            updateSummary(R.string.pref_key_storage_path, folderFullName);
        }
        else if (requestCode == REQUEST_CODE_OPEN_TEMP_PATH) {
            SettingsManager.setTempPath(folderFullName);
            SettingsManager.setLastChoosedFolder(folderFullName);
            updateSummary(R.string.pref_key_temp_path, folderFullName);
        }
        else if (requestCode == REQUEST_CODE_OPEN_LOG_PATH) {
            SettingsManager.setLogPath(folderFullName);
            SettingsManager.setLastChoosedFolder(folderFullName);
            LogManager.setLogPath(folderFullName);
            updateSummary(R.string.pref_key_log_path, folderFullName);
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
        /*if (key.equals(getString(R.string.pref_key_storage_path))) {
            // вызывается, когда задаем
            // удаляем хэш пароля, если поменяли хранилище
//            String newStoragePath = SettingsManager.getStoragePath();
//            if (TextUtils.isEmpty(newStoragePath) || !newStoragePath.equals(SettingsManager.LastStoragePath)) {
//                SettingsManager.setMiddlePassHash(null);
//            }

        } else*/ if (key.equals(getString(R.string.pref_key_is_save_pass_hash_local))) {
            if (SettingsManager.isSaveMiddlePassHashLocal()) {
                // сохраняем хеш пароля локально в настройках
                SettingsManager.setMiddlePassHash(CryptManager.getMiddlePassHash());
            }
            else  {
                // удаляем хэш пароля, если сняли галку
                SettingsManager.setMiddlePassHash(null);
            }
//        } else if (key.equals(sizeToString(R.string.pref_key_record_fields_cols))) {
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
        } else if (key.equals(getString(R.string.pref_key_sync_command))) {
            updateSummary(R.string.pref_key_sync_command, SettingsManager.getSyncCommand());
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Этот метод и методы ниже для реализации ToolBar в PreferenceActivity
     * @param savedInstanceState
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    private void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Nullable
    public ActionBar getSupportActionBar() {
        return this.getDelegate().getSupportActionBar();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }
}