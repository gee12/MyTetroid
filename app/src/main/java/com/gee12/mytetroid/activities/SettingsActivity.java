package com.gee12.mytetroid.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.PermissionManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TaskStage;
import com.gee12.mytetroid.TetroidLog;
import com.gee12.mytetroid.TetroidSuggestionProvider;
import com.gee12.mytetroid.TetroidTask;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.ITaskProgress;
import com.gee12.mytetroid.data.PINManager;
import com.gee12.mytetroid.data.PassManager;
import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.dialogs.AskDialogs;
import com.gee12.mytetroid.dialogs.PassDialogs;
import com.gee12.mytetroid.views.Message;
import com.gee12.mytetroid.views.StorageChooserDialog;

import org.jetbrains.annotations.NotNull;
import org.jsoup.internal.StringUtil;

import lib.folderpicker.FolderPicker;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String EXTRA_IS_REINIT_STORAGE = "EXTRA_IS_REINIT_STORAGE";
    public static final String EXTRA_IS_CREATE_STORAGE = "EXTRA_IS_CREATE_STORAGE";
    public static final String EXTRA_IS_PASS_CHANGED = "EXTRA_IS_PASS_CHANGED";

    public static final int REQUEST_CODE_OPEN_STORAGE_PATH = 1;
    public static final int REQUEST_CODE_CREATE_STORAGE_PATH = 2;
    public static final int REQUEST_CODE_OPEN_TEMP_PATH = 3;
    public static final int REQUEST_CODE_OPEN_LOG_PATH = 4;

    private AppCompatDelegate mDelegate;
    private LinearLayout mLayoutProgress;
    private TextView mTextViewProgress;

    private TetroidTask mCurTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.prefs);

        this.mLayoutProgress = findViewById(R.id.layout_progress);
        this.mTextViewProgress = findViewById(R.id.progress_text);

        Preference storageFolderPicker = findPreference(getString(R.string.pref_key_storage_path));
        storageFolderPicker.setOnPreferenceClickListener(preference -> {
            if (!checkPermission(REQUEST_CODE_OPEN_STORAGE_PATH))
                return true;
            selectStorageFolder();
            return true;
        });

        Preference tempFolderPicker = findPreference(getString(R.string.pref_key_temp_path));
        tempFolderPicker.setOnPreferenceClickListener(preference -> {
            if (!checkPermission(REQUEST_CODE_OPEN_TEMP_PATH))
                return true;
            selectTrashFolder();
            return true;
        });

        Preference logFolderPicker = findPreference(getString(R.string.pref_key_log_path));
        logFolderPicker.setOnPreferenceClickListener(preference -> {
            if (!checkPermission(REQUEST_CODE_OPEN_LOG_PATH))
                return true;
            selectLogsFolder();
            return true;
        });

        findPreference(getString(R.string.pref_key_clear_trash))
                .setOnPreferenceClickListener(preference -> {
                    AskDialogs.showYesDialog(this, () -> {
                        if (DataManager.clearTrashFolder()) {
                            LogManager.log(R.string.title_trash_cleared, LogManager.Types.INFO, Toast.LENGTH_SHORT);
                        } else {
                            LogManager.log(R.string.title_trash_clear_error, LogManager.Types.ERROR, Toast.LENGTH_LONG);
                        }
                    }, R.string.ask_clear_trash);
                    return true;
                });

        findPreference(getString(R.string.pref_key_clear_search_history))
                .setOnPreferenceClickListener(pref -> {
                    AskDialogs.showYesDialog(this, () -> {
                        TetroidSuggestionProvider.clearHistory(this);
                        SettingsManager.clearSearchOptions();
                        LogManager.log(R.string.title_search_history_cleared, LogManager.Types.INFO, Toast.LENGTH_SHORT);
                    }, R.string.ask_clear_search_history);
                    return true;
                });

        Preference passPref = findPreference(getString(R.string.pref_key_change_pass));
        boolean isInited = DataManager.isInited();
        boolean isLoaded = DataManager.isLoaded();
        boolean crypted = DataManager.isCrypted();
        passPref.setTitle(crypted ? R.string.pref_change_pass : R.string.pref_setup_pass);
        passPref.setSummary(crypted ? R.string.pref_change_pass_summ : R.string.pref_setup_pass_summ);
        passPref.setEnabled(isInited && isLoaded && !App.IsLoadedFavoritesOnly);
        passPref.setOnPreferenceClickListener(pref -> {
            if (!isInited) {
                String mes = getString((PermissionManager.writeExtStoragePermGranted(this))
                    ? R.string.title_need_init_storage
                    : R.string.title_need_perm_init_storage);
                Message.show(this, mes, Toast.LENGTH_SHORT);
            } else if (!isLoaded) {
                Message.show(this, getString(R.string.title_need_load_storage), Toast.LENGTH_SHORT);
            } else if (App.IsLoadedFavoritesOnly) {
                Message.show(this, getString(R.string.title_need_load_nodes), Toast.LENGTH_SHORT);
            } else {
                if (crypted) {
                    changePass();
                } else {
                    PassManager.setupPass(this);
                }
                // устанавливаем флаг для MainActivity
                Intent intent = new Intent();
                intent.putExtra(EXTRA_IS_PASS_CHANGED, true);
                setResult(RESULT_OK, intent);
            }
            return true;
        });

//        findPreference(getString(R.string.pref_key_when_ask_password)).
//                setEnabled(!SettingsManager.isSaveMiddlePassHashLocal());

        findPreference(getString(R.string.pref_key_is_save_pass_hash_local)).
                setOnPreferenceChangeListener((preference, newValue) -> {
                    if (SettingsManager.getMiddlePassHash() == null) {
                        // если пароль не задан, то нечего очищать, не задаем вопрос
                        return true;
                    }
                    if (!((boolean)newValue) && SettingsManager.isSaveMiddlePassHashLocal()) {
                        // удалить сохраненный хэш пароля?
                        AskDialogs.showYesNoDialog(this, new Dialogs.IApplyCancelResult() {
                            @Override
                            public void onApply() {
                                // удаляем хэш пароля, если сняли галку
                                SettingsManager.setMiddlePassHash(null);
                                // сбрасываем галку
                                SettingsManager.setIsSaveMiddlePassHashLocal(false);
                                ((CheckBoxPreference)preference).setChecked(false);
                            }

                            @Override
                            public void onCancel() {
                                // устанавливаем галку обратно
//                                SettingsManager.setIsSaveMiddlePassHashLocal(true);
                            }
                        }, R.string.ask_clear_saved_pass_hash);
                        return false;
                    }
                    return true;
                });

        CheckBoxPreference pinCodePref = (CheckBoxPreference) findPreference(getString(R.string.pref_key_request_pin_code));
        disableIfFree(pinCodePref);
        pinCodePref.setOnPreferenceChangeListener((preference, newValue) -> {
            PINManager.setupPINCode(SettingsActivity.this, res -> {
                SettingsManager.setIsRequestPINCode(res);
                pinCodePref.setChecked(res);
            });
            return false;
        });

        Preference askPassPref = findPreference(getString(R.string.pref_key_when_ask_password));
        askPassPref.setOnPreferenceClickListener(pref -> {
            if (SettingsManager.isSaveMiddlePassHashLocal()) {
                Message.show(this, getString(R.string.title_not_avail_when_save_pass), Toast.LENGTH_SHORT);
            }
            return true;
        });

        Preference keepNodePref = findPreference(getString(R.string.pref_key_is_keep_selected_node));
        keepNodePref.setOnPreferenceClickListener(pref -> {
            if (SettingsManager.isLoadFavoritesOnly()) {
                Message.show(this, getString(R.string.title_not_avail_when_favor), Toast.LENGTH_SHORT);
            }
            return true;
        });
        Preference loadFavorPref = findPreference(getString(R.string.pref_key_is_load_favorites));
        disableIfFree(loadFavorPref);

        updateSummary(R.string.pref_key_storage_path, SettingsManager.getStoragePath());
        updateSummary(R.string.pref_key_temp_path, SettingsManager.getTrashPath());
        updateSummary(R.string.pref_key_sync_command, SettingsManager.getSyncCommand());
        updateSummary(R.string.pref_key_log_path, SettingsManager.getLogPath());
    }

    /**
     * Деактивация опции, если версия приложения Free.
     * @param pref
     */
    private void disableIfFree(Preference pref) {
        if (App.isFullVersion()) {
            pref.setEnabled(true);
        } else {
            pref.setEnabled(false);
            // принудительно отключаем
            pref.setOnPreferenceClickListener(pref1 -> {
                Message.show(this, getString(R.string.title_available_in_pro), Toast.LENGTH_SHORT);
                return true;
            });
            pref.setDependency(null);
        }
    }

    private void selectStorageFolder() {
        // спрашиваем: создать или выбрать хранилище ?
        StorageChooserDialog.createDialog(this, isNew -> {
            openFolderPicker(getString(R.string.title_storage_folder),
                    SettingsManager.getStoragePath(),
                    (isNew) ? REQUEST_CODE_CREATE_STORAGE_PATH : REQUEST_CODE_OPEN_STORAGE_PATH);
        });
    }

    private void selectTrashFolder() {
        openFolderPicker(getString(R.string.pref_trash_path),
                SettingsManager.getTrashPath(),
                REQUEST_CODE_OPEN_TEMP_PATH);
    }

    private void selectLogsFolder() {
        openFolderPicker(getString(R.string.pref_log_path),
                SettingsManager.getLogPath(),
                REQUEST_CODE_OPEN_LOG_PATH);
    }

    private boolean checkPermission(int requestCode) {
        return PermissionManager.checkWriteExtStoragePermission(this, requestCode);
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
        intent.putExtra(FolderPicker.EXTRA_TITLE, title);
        intent.putExtra(FolderPicker.EXTRA_LOCATION, path);
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        boolean permGranted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        if (permGranted) {
            LogManager.log(R.string.log_write_ext_storage_perm_granted, LogManager.Types.INFO);
            switch (requestCode) {
                case REQUEST_CODE_OPEN_STORAGE_PATH: selectStorageFolder(); break;
                case REQUEST_CODE_OPEN_TEMP_PATH: selectTrashFolder(); break;
                case REQUEST_CODE_OPEN_LOG_PATH: selectLogsFolder(); break;
            }
        } else {
            LogManager.log(R.string.log_missing_write_ext_storage_permissions, LogManager.Types.WARNING, Toast.LENGTH_SHORT);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
        String folderPath = data.getStringExtra(FolderPicker.EXTRA_DATA);
        boolean isCreate = requestCode == REQUEST_CODE_CREATE_STORAGE_PATH;
        if (requestCode == REQUEST_CODE_OPEN_STORAGE_PATH || isCreate) {
            // уведомляем об изменении каталога, если он действительно изменился, либо если создаем
            boolean pathChanged = !folderPath.equals(SettingsManager.getStoragePath()) || isCreate;
            if (pathChanged) {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_IS_REINIT_STORAGE, true);
                if (isCreate) {
                    intent.putExtra(EXTRA_IS_CREATE_STORAGE, true);
                }
                setResult(RESULT_OK, intent);
            }
            SettingsManager.setStoragePath(folderPath);
            SettingsManager.setLastChoosedFolder(folderPath);
            updateSummary(R.string.pref_key_storage_path, folderPath);
            if (pathChanged) {
                // закрываем настройки для немедленной перезагрузки хранилища
                finish();
            }
        }
        else if (requestCode == REQUEST_CODE_OPEN_TEMP_PATH) {
            SettingsManager.setTrashPath(folderPath);
            SettingsManager.setLastChoosedFolder(folderPath);
            updateSummary(R.string.pref_key_temp_path, folderPath);
        }
        else if (requestCode == REQUEST_CODE_OPEN_LOG_PATH) {
            SettingsManager.setLogPath(folderPath);
            SettingsManager.setLastChoosedFolder(folderPath);
            LogManager.setLogPath(folderPath);
            updateSummary(R.string.pref_key_log_path, folderPath);
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
        /*if (key.equals(getString(R.string.pref_key_is_save_pass_hash_local))) {
            findPreference(getString(R.string.pref_key_when_ask_password)).
                    setEnabled(!SettingsManager.isSaveMiddlePassHashLocal());
//        } else if (key.equals(sizeToString(R.string.pref_key_record_fields_cols))) {
//            // меняем список полей для отображения
        } else*/ if (key.equals(getString(R.string.pref_key_is_highlight_attach))) {
            // включаем/выключаем выделение записей с файлами
            App.IsHighlightAttach = SettingsManager.isHighlightRecordWithAttach();
            setHighlightPrefAvailability();
        } else if (key.equals(getString(R.string.pref_key_is_highlight_crypted_nodes))) {
            // включаем/выключаем выделение зашифрованных веток
            App.IsHighlightCryptedNodes = SettingsManager.isHighlightEncryptedNodes();
            setHighlightPrefAvailability();
        } else if (key.equals(getString(R.string.pref_key_highlight_attach_color))) {
            // меняем цвет выделения записей с файлами
            App.HighlightAttachColor = SettingsManager.getHighlightColor();
        } else if (key.equals(getString(R.string.pref_key_date_format_string))) {
            // меняем формат даты
            App.DateFormatString = SettingsManager.getDateFormatString();
        } else if (key.equals(getString(R.string.pref_key_is_write_log))) {
            // меняем флаг
            LogManager.init(this, SettingsManager.getLogPath(), SettingsManager.isWriteLogToFile());
        } else if (key.equals(getString(R.string.pref_key_sync_command))) {
            updateSummary(R.string.pref_key_sync_command, SettingsManager.getSyncCommand());
        } else if (key.equals(getString(R.string.pref_key_is_save_pass_hash_local))) {
            setPINCodePrefAvailability();
        }
    }

    private void refreshPreferences() {
        setPreferenceScreen(null);
        addPreferencesFromResource(R.xml.prefs);
    }

    private void setPINCodePrefAvailability() {
        if (App.isFullVersion()) {
            findPreference(getString(R.string.pref_key_request_pin_code)).setEnabled(
                    SettingsManager.isSaveMiddlePassHashLocal());
        }
    }

    private void setHighlightPrefAvailability() {
        findPreference(getString(R.string.pref_key_highlight_attach_color)).setEnabled(
                SettingsManager.isHighlightRecordWithAttach()
                || SettingsManager.isHighlightEncryptedNodes());
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


    /**
     * Смена пароля хранилища.
     * @return
     */
    public void changePass() {
        LogManager.log(R.string.log_start_pass_change);
        // вводим пароли (с проверкой на пустоту и равенство)
        PassDialogs.showPassChangeDialog(this, (curPass, newPass) -> {
            // проверяем пароль
            return PassManager.checkPass(this, curPass, (res) -> {
                if (res) {
                    this.mCurTask = new ChangePassTask().run(curPass, newPass);
                }
            }, R.string.log_cur_pass_is_incorrect);
        });
    }

    @Override
    public void onBackPressed() {
        if (mCurTask != null && mCurTask.isRunning()) {
            return;
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Задание (параллельный поток), в котором выполняется перешифровка хранилища.
     */
    public class ChangePassTask extends TetroidTask<String, String, Boolean> {

        public ChangePassTask() {
            super(SettingsActivity.this);
        }

        @Override
        protected void onPreExecute() {
            mTextViewProgress.setText(getString(R.string.task_pass_changing));
            mLayoutProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(String... values) {
            String curPass = values[0];
            String newPass = values[1];
            return PassManager.changePass(curPass, newPass, new ITaskProgress() {

                @Override
                public void nextStage(TetroidLog.Objs obj, TetroidLog.Opers oper, TaskStage.Stages stage) {
                    setStage(obj, oper, stage);
                }

                @Override
                public boolean nextStage(TetroidLog.Objs obj, TetroidLog.Opers oper, TaskStage.ITaskStageExecutor stageExecutor) {
                    setStage(obj, oper, TaskStage.Stages.START);
                    if (stageExecutor.execute()) {
                        setStage(obj, oper, TaskStage.Stages.SUCCESS);
                        return true;
                    } else {
                        setStage(obj, oper, TaskStage.Stages.FAILED);
                        return false;
                    }
                }

            });
        }

        private void setStage(TetroidLog.Objs obj, TetroidLog.Opers oper, TaskStage.Stages stage) {
            TaskStage taskStage = new TaskStage(ChangePassTask.class, obj, oper, stage);
            String mes = TetroidLog.logTaskStage(taskStage);
            publishProgress(mes);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String mes = values[0];
            mTextViewProgress.setText(mes);
        }

        @Override
        protected void onPostExecute(Boolean res) {
            mLayoutProgress.setVisibility(View.INVISIBLE);
            if (res) {
                LogManager.log(R.string.log_pass_changed, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            } else {
                LogManager.log(R.string.log_pass_change_error, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            }
        }
    }
}