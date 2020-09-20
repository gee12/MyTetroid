package com.gee12.mytetroid.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.PermissionManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TaskStage;
import com.gee12.mytetroid.TetroidLog;
import com.gee12.mytetroid.TetroidSuggestionProvider;
import com.gee12.mytetroid.TetroidTask;
import com.gee12.mytetroid.activities.SettingsActivity;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.ITaskProgress;
import com.gee12.mytetroid.data.NodesManager;
import com.gee12.mytetroid.data.PINManager;
import com.gee12.mytetroid.data.PassManager;
import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.dialogs.AskDialogs;
import com.gee12.mytetroid.dialogs.PassDialogs;
import com.gee12.mytetroid.views.DateTimeFormatDialog;
import com.gee12.mytetroid.views.DateTimeFormatPreference;
import com.gee12.mytetroid.views.Message;
import com.gee12.mytetroid.views.StorageChooserDialog;

import org.jsoup.internal.StringUtil;

import lib.folderpicker.FolderPicker;

import static android.app.Activity.RESULT_OK;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String DIALOG_FRAGMENT_TAG = "DateTimeFormatPreference";
    public static final String EXTRA_IS_REINIT_STORAGE = "EXTRA_IS_REINIT_STORAGE";
    public static final String EXTRA_IS_CREATE_STORAGE = "EXTRA_IS_CREATE_STORAGE";
    public static final String EXTRA_IS_PASS_CHANGED = "EXTRA_IS_PASS_CHANGED";

    public static final int REQUEST_CODE_OPEN_STORAGE_PATH = 1;
    public static final int REQUEST_CODE_CREATE_STORAGE_PATH = 2;
    public static final int REQUEST_CODE_OPEN_TEMP_PATH = 3;
    public static final int REQUEST_CODE_OPEN_LOG_PATH = 4;

    private TetroidTask mCurTask;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs, rootKey);

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
                    AskDialogs.showYesDialog(getContext(), () -> {
                        if (DataManager.clearTrashFolder()) {
                            LogManager.log(R.string.title_trash_cleared, LogManager.Types.INFO, Toast.LENGTH_SHORT);
                        } else {
                            LogManager.log(R.string.title_trash_clear_error, LogManager.Types.ERROR, Toast.LENGTH_LONG);
                        }
                    }, R.string.ask_clear_trash);
                    return true;
                });

        findPreference(getString(R.string.pref_key_quickly_node_id))
                .setOnPreferenceClickListener(preference -> {

                    // TODO:
                    /*SettingsManager.setQuicklyNode(node);
                    DataManager.setQuicklyNode(node);*/
                    updateSummary(R.string.pref_key_quickly_node_id, SettingsManager.getQuicklyNodeName());
                    return true;
                });
        NodesManager.updateQuicklyNode();

        findPreference(getString(R.string.pref_key_clear_search_history))
                .setOnPreferenceClickListener(pref -> {
                    AskDialogs.showYesDialog(getContext(), () -> {
                        TetroidSuggestionProvider.clearHistory(getContext());
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
                String mes = getString((PermissionManager.writeExtStoragePermGranted(getContext()))
                        ? R.string.title_need_init_storage
                        : R.string.title_need_perm_init_storage);
                Message.show(getContext(), mes, Toast.LENGTH_SHORT);
            } else if (!isLoaded) {
                Message.show(getContext(), getString(R.string.title_need_load_storage), Toast.LENGTH_SHORT);
            } else if (App.IsLoadedFavoritesOnly) {
                Message.show(getContext(), getString(R.string.title_need_load_nodes), Toast.LENGTH_SHORT);
            } else {
                if (crypted) {
                    changePass();
                } else {
                    PassManager.setupPass(getContext());
                }
                // устанавливаем флаг для MainActivity
                Intent intent = new Intent();
                intent.putExtra(EXTRA_IS_PASS_CHANGED, true);
                getActivity().setResult(RESULT_OK, intent);
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
                    } else {
                        PINManager.askPINCode(getContext(), true, () ->
                                changeSavePassHashLocal((boolean) newValue));
                        return false;
                    }
                });

        CheckBoxPreference pinCodePref = findPreference(getString(R.string.pref_key_request_pin_code));
        disableIfFree(pinCodePref);
        pinCodePref.setOnPreferenceChangeListener((preference, newValue) -> {
            PINManager.setupPINCode(getContext(), res -> {
                SettingsManager.setIsRequestPINCode(res);
                pinCodePref.setChecked(res);
            });
            return false;
        });
        setPINCodePrefAvailability();

        Preference askPassPref = findPreference(getString(R.string.pref_key_when_ask_password));
        askPassPref.setOnPreferenceClickListener(pref -> {
            if (SettingsManager.isSaveMiddlePassHashLocal()) {
                Message.show(getContext(), getString(R.string.title_not_avail_when_save_pass), Toast.LENGTH_SHORT);
            }
            return true;
        });

        Preference keepNodePref = findPreference(getString(R.string.pref_key_is_keep_selected_node));
        keepNodePref.setOnPreferenceClickListener(pref -> {
            if (SettingsManager.isLoadFavoritesOnly()) {
                Message.show(getContext(), getString(R.string.title_not_avail_when_favor), Toast.LENGTH_SHORT);
            }
            return true;
        });
        Preference loadFavorPref = findPreference(getString(R.string.pref_key_is_load_favorites));
        disableIfFree(loadFavorPref);

        updateSummary(R.string.pref_key_when_ask_password, (SettingsManager.isSaveMiddlePassHashLocal())
                ? getString(R.string.pref_when_ask_password_summ) : SettingsManager.getWhenAskPass());
        updateSummary(R.string.pref_key_storage_path, SettingsManager.getStoragePath());
        updateSummary(R.string.pref_key_temp_path, SettingsManager.getTrashPath());
        updateSummary(R.string.pref_key_quickly_node_id, SettingsManager.getQuicklyNodeName());
        updateSummary(R.string.pref_key_sync_command, SettingsManager.getSyncCommand());
        updateSummary(R.string.pref_key_log_path, SettingsManager.getLogPath());

        setHighlightPrefAvailability();
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
        if (key.equals(getString(R.string.pref_key_is_save_pass_hash_local))) {
            setPINCodePrefAvailability();
            updateSummary(R.string.pref_key_when_ask_password, (SettingsManager.isSaveMiddlePassHashLocal())
                    ? getString(R.string.pref_when_ask_password_summ) : SettingsManager.getWhenAskPass());
        } else if (key.equals(getString(R.string.pref_key_when_ask_password))) {
            updateSummary(R.string.pref_key_when_ask_password, SettingsManager.getWhenAskPass());
        } else if (key.equals(getString(R.string.pref_key_is_highlight_attach))) {
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
            LogManager.init(getContext(), SettingsManager.getLogPath(), SettingsManager.isWriteLogToFile());
        } else if (key.equals(getString(R.string.pref_key_sync_command))) {
            updateSummary(R.string.pref_key_sync_command, SettingsManager.getSyncCommand());
        }
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

    public void onRequestPermissionsResult(boolean permGranted, int requestCode) {
        if (permGranted) {
            LogManager.log(R.string.log_write_ext_storage_perm_granted, LogManager.Types.INFO);
            switch (requestCode) {
                case REQUEST_CODE_OPEN_STORAGE_PATH:
                    selectStorageFolder();
                    break;
                case REQUEST_CODE_OPEN_TEMP_PATH:
                    selectTrashFolder();
                    break;
                case REQUEST_CODE_OPEN_LOG_PATH:
                    selectLogsFolder();
                    break;
            }
        } else {
            LogManager.log(R.string.log_missing_write_ext_storage_permissions, LogManager.Types.WARNING, Toast.LENGTH_SHORT);
        }
    }

    public void onResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
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
                getActivity().setResult(RESULT_OK, intent);
            }
            SettingsManager.setStoragePath(folderPath);
            SettingsManager.setLastChoosedFolder(folderPath);
            updateSummary(R.string.pref_key_storage_path, folderPath);
            if (pathChanged) {
                // закрываем настройки для немедленной перезагрузки хранилища
                getActivity().finish();
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

    private boolean checkPermission(int requestCode) {
        return PermissionManager.checkWriteExtStoragePermission(getActivity(), requestCode);
    }

    private void selectStorageFolder() {
        // спрашиваем: создать или выбрать хранилище ?
        StorageChooserDialog.createDialog(getContext(), isNew -> {
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

    private void openFolderPicker(String title, String location, int requestCode) {
        String path = (!StringUtil.isBlank(location)) ? location : DataManager.getLastFolderOrDefault(getContext(), true);
        Intent intent = new Intent(getContext(), FolderPicker.class);
        intent.putExtra(FolderPicker.EXTRA_TITLE, title);
        intent.putExtra(FolderPicker.EXTRA_LOCATION, path);
        getActivity().startActivityForResult(intent, requestCode);
    }

    /**
     * Обработка сброса опции сохранения хэша пароля локально.
     * @param newValue
     * @return
     */
    private void changeSavePassHashLocal(boolean newValue) {
        if (!newValue && SettingsManager.isSaveMiddlePassHashLocal()) {
            // удалить сохраненный хэш пароля?
            AskDialogs.showYesNoDialog(getContext(), new Dialogs.IApplyCancelResult() {
                @Override
                public void onApply() {
                    // удаляем хэш пароля, если сняли галку
                    SettingsManager.setMiddlePassHash(null);
                    // сбрасываем галку
                    SettingsManager.setIsSaveMiddlePassHashLocal(false);
                    ((CheckBoxPreference)findPreference(getString(R.string.pref_key_is_save_pass_hash_local)))
                            .setChecked(false);
                    // сбрасываем ПИН-код
                    SettingsManager.setIsRequestPINCode(false);
                    SettingsManager.setPINCodeHash(null);
                    ((CheckBoxPreference)findPreference(getString(R.string.pref_key_request_pin_code)))
                            .setChecked(false);
                    setPINCodePrefAvailability();
                }

                @Override
                public void onCancel() {
                    // устанавливаем галку обратно
//                    SettingsManager.setIsSaveMiddlePassHashLocal(true);
                }
            }, R.string.ask_clear_saved_pass_hash);
        }
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
                Message.show(getContext(), getString(R.string.title_available_in_pro), Toast.LENGTH_SHORT);
                return true;
            });
            pref.setDependency(null);
        }
    }

    private void updateSummary(@StringRes int keyStringRes, String value) {
        if (!StringUtil.isBlank(value)) {
            Preference pref = findPreference(getString(keyStringRes));
            if (pref != null)
                pref.setSummary(value);
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
    public void onResume() {
        super.onResume();
        SettingsManager.getSettings().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        SettingsManager.getSettings().unregisterOnSharedPreferenceChangeListener(this);
    }

    public boolean onBackPressed() {
        if (mCurTask != null && mCurTask.isRunning()) {
            return true;
        }
        return false;
    }

    /**
     * Смена пароля хранилища.
     * @return
     */
    public void changePass() {
        LogManager.log(R.string.log_start_pass_change);
        // вводим пароли (с проверкой на пустоту и равенство)
        PassDialogs.showPassChangeDialog(getContext(), (curPass, newPass) -> {
            // проверяем пароль
            return PassManager.checkPass(getContext(), curPass, (res) -> {
                if (res) {
                    this.mCurTask = new ChangePassTask().run(curPass, newPass);
                }
            }, R.string.log_cur_pass_is_incorrect);
        });
    }

    /**
     * Задание (параллельный поток), в котором выполняется перешифровка хранилища.
     */
    public class ChangePassTask extends TetroidTask<String, String, Boolean> {

        public ChangePassTask() {
            super(getActivity());
        }

        @Override
        protected void onPreExecute() {
//            mTextViewProgress.setText(getString(R.string.task_pass_changing));
//            mLayoutProgress.setVisibility(View.VISIBLE);
            getSettingsActivity().setProgressVisibility(true, getString(R.string.task_pass_changing));
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
//            mTextViewProgress.setText(mes);
            getSettingsActivity().setProgressVisibility(true, mes);
        }

        @Override
        protected void onPostExecute(Boolean res) {
//            mLayoutProgress.setVisibility(View.INVISIBLE);
            getSettingsActivity().setProgressVisibility(false, null);
            if (res) {
                LogManager.log(R.string.log_pass_changed, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            } else {
                LogManager.log(R.string.log_pass_change_error, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            }
        }
    }

    private SettingsActivity getSettingsActivity() {
        return (SettingsActivity) getActivity();
    }
}
