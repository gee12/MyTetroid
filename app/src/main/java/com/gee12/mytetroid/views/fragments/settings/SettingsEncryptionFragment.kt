package com.gee12.mytetroid.views.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.CommonSettings

class SettingsEncryptionFragment : TetroidSettingsFragment() {

//    private TetroidTask mCurTask;

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_encryption, rootKey)
        requireActivity().setTitle(R.string.pref_category_crypt)

//        boolean isStorageReady = DataManager.isInited() && DataManager.isLoaded() && !App.IsLoadedFavoritesOnly;

        // установка или смена пароля хранилища
//        Preference passPref = findPreference(getString(R.string.pref_key_change_pass));
//        boolean isCrypted = DataManager.isCrypted(mContext);
//        passPref.setTitle(isCrypted ? R.string.pref_change_pass : R.string.pref_setup_pass);
//        passPref.setSummary(isCrypted ? R.string.pref_change_pass_summ : R.string.pref_setup_pass_summ);
//        passPref.setEnabled(isStorageReady);
//        passPref.setOnPreferenceClickListener(pref -> {
//            if (checkStorageIsReady(true)) {
//                if (isCrypted) {
//                    changePass();
//                } else {
//                    PassManager.setupPass(getContext());
//                }
//                // устанавливаем флаг для MainActivity
//                Intent intent = new Intent();
//                intent.putExtra(SettingsFragment.EXTRA_IS_PASS_CHANGED, true);
//                getActivity().setResult(RESULT_OK, intent);
//            }
//            return true;
//        });

        // сохранение пароля локально
//        findPreference(getString(R.string.pref_key_is_save_pass_hash_local)).
//                setOnPreferenceChangeListener((preference, newValue) -> {
//                    if (SettingsManager.getMiddlePassHash(mContext) == null) {
//                        // если пароль не задан, то нечего очищать, не задаем вопрос
//                        return true;
//                    } else {
//                        PINManager.askPINCode(getContext(), true, () ->
//                                changeSavePassHashLocal((boolean) newValue));
//                        return false;
//                    }
//                });

        // установка ПИН-кода
//        CheckBoxPreference pinCodePref = findPreference(getString(R.string.pref_key_request_pin_code));
//        disableIfFree(pinCodePref);
//        // проверять готовность хранилища достаточно только при установке кода
//        //  (а при сбросе не требуется)
//        pinCodePref.setEnabled(SettingsManager.isRequestPINCode(getContext()) || isStorageReady);
//        pinCodePref.setOnPreferenceChangeListener((preference, newValue) -> {
//            if (SettingsManager.isRequestPINCode(getContext()) || checkStorageIsReady(true)) {
//                PINManager.setupPINCode(getContext(), res -> {
//                    SettingsManager.setIsRequestPINCode(mContext, res);
//                    pinCodePref.setChecked(res);
//                });
//            }
//            return false;
//        });
//        setPINCodePrefAvailability();

        // когда запрашивать пароль
        findPreference<Preference>(getString(R.string.pref_key_when_ask_password))
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (CommonSettings.isSaveMiddlePassHashLocalDef(context)) {
                baseViewModel.showMessage(getString(R.string.title_not_avail_when_save_pass))
            }
            true
        }
        updateSummary(
            R.string.pref_key_when_ask_password,
            if (CommonSettings.isSaveMiddlePassHashLocalDef(context)) getString(R.string.pref_when_ask_password_summ)
            else CommonSettings.getWhenAskPass(context)
        )
    }

    /**
     * Обработчик изменения настроек.
     * Чтобы работало нужно переопределить onResume() и onPause()
     * и дописать register/unregister настроек.
     * @param sharedPreferences
     * @param key
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.pref_key_is_save_pass_hash_local)) {
//            setPINCodePrefAvailability();
            updateSummary(
                R.string.pref_key_when_ask_password,
                if (CommonSettings.isSaveMiddlePassHashLocalDef(context)) getString(R.string.pref_when_ask_password_summ)
                else CommonSettings.getWhenAskPass(context)
            )
        } else if (key == getString(R.string.pref_key_when_ask_password)) {
            updateSummary(R.string.pref_key_when_ask_password, CommonSettings.getWhenAskPass(context))
        }
    }

//    /**
//     * Обработка сброса опции сохранения хэша пароля локально.
//     * @param newValue
//     * @return
//     */
    //    private void changeSavePassHashLocal(boolean newValue) {
    //        if (!newValue && SettingsManager.isSaveMiddlePassHashLocal(mContext)) {
    //            // удалить сохраненный хэш пароля?
    //            AskDialogs.showYesNoDialog(getContext(), new Dialogs.IApplyCancelResult() {
    //                @Override
    //                public void onApply() {
    //                    // удаляем хэш пароля, если сняли галку
    //                    SettingsManager.setMiddlePassHash(mContext, null);
    //                    // сбрасываем галку
    //                    SettingsManager.setIsSaveMiddlePassHashLocal(mContext, false);
    //                    ((CheckBoxPreference)findPreference(getString(R.string.pref_key_is_save_pass_hash_local)))
    //                            .setChecked(false);
    //                    // сбрасываем ПИН-код
    //                    SettingsManager.setIsRequestPINCode(mContext, false);
    //                    SettingsManager.setPINCodeHash(mContext, null);
    //                    ((CheckBoxPreference)findPreference(getString(R.string.pref_key_request_pin_code)))
    //                            .setChecked(false);
    //                    setPINCodePrefAvailability();
    //                }
    //
    //                @Override
    //                public void onCancel() {
    //                    // устанавливаем галку обратно
    ////                    SettingsManager.setIsSaveMiddlePassHashLocal(true);
    //                }
    //            }, R.string.ask_clear_saved_pass_hash);
    //        }
    //    }
    //    private void setPINCodePrefAvailability() {
    //        if (App.isFullVersion()) {
    //            findPreference(getString(R.string.pref_key_request_pin_code)).setEnabled(
    //                    SettingsManager.isSaveMiddlePassHashLocal(mContext));
    //        }
    //    }
    //    private boolean checkStorageIsReady(boolean checkIsFavorMode) {
    //        if (!DataManager.isInited()) {
    //            String mes = getString((PermissionManager.writeExtStoragePermGranted(getContext()))
    //                    ? R.string.title_need_init_storage
    //                    : R.string.title_need_perm_init_storage);
    //            Message.show(getContext(), mes, Toast.LENGTH_SHORT);
    //            return false;
    //        } else if (!DataManager.isLoaded()) {
    //            Message.show(getContext(), getString(R.string.title_need_load_storage), Toast.LENGTH_SHORT);
    //            return false;
    //        } else if (checkIsFavorMode && App.IsLoadedFavoritesOnly) {
    //            Message.show(getContext(), getString(R.string.title_need_load_nodes), Toast.LENGTH_SHORT);
    //            return false;
    //        }
    //        return true;
    //    }
    //    public boolean onBackPressed() {
    //        if (mCurTask != null && mCurTask.isRunning()) {
    //            return true;
    //        }
    //        return false;
    //    }

//    /**
//     * Смена пароля хранилища.
//     * @return
//     */
    //    public void changePass() {
    //        LogManager.log(mContext, R.string.log_start_pass_change);
    //        // вводим пароли (с проверкой на пустоту и равенство)
    //        PassDialogs.showPassChangeDialog(getContext(), (curPass, newPass) -> {
    //            // проверяем пароль
    //            return PassManager.checkPass(getContext(), curPass, (res) -> {
    //                if (res) {
    //                    this.mCurTask = new ChangePassTask().run(curPass, newPass);
    //                }
    //            }, R.string.log_cur_pass_is_incorrect);
    //        });
    //    }

//    /**
//     * Задание (параллельный поток), в котором выполняется перешифровка хранилища.
//     */
    //    public class ChangePassTask extends TetroidTask<String, String, Boolean> {
    //
    //        public ChangePassTask() {
    //            super(getActivity());
    //        }
    //
    //        @Override
    //        protected void onPreExecute() {
    ////            mTextViewProgress.setText(getString(R.string.task_pass_changing));
    ////            mLayoutProgress.setVisibility(View.VISIBLE);
    //            getSettingsActivity().setProgressVisibility(true, getString(R.string.task_pass_changing));
    //        }
    //
    //        @Override
    //        protected Boolean doInBackground(String... values) {
    //            String curPass = values[0];
    //            String newPass = values[1];
    //            return PassManager.changePass(getContext(), curPass, newPass, new ITaskProgress() {
    //
    //                @Override
    //                public void nextStage(TetroidLog.Objs obj, TetroidLog.Opers oper, TaskStage.Stages stage) {
    //                    setStage(obj, oper, stage);
    //                }
    //
    //                @Override
    //                public boolean nextStage(TetroidLog.Objs obj, TetroidLog.Opers oper, TaskStage.ITaskStageExecutor stageExecutor) {
    //                    setStage(obj, oper, TaskStage.Stages.START);
    //                    if (stageExecutor.execute()) {
    //                        setStage(obj, oper, TaskStage.Stages.SUCCESS);
    //                        return true;
    //                    } else {
    //                        setStage(obj, oper, TaskStage.Stages.FAILED);
    //                        return false;
    //                    }
    //                }
    //
    //            });
    //        }
    //
    //        private void setStage(TetroidLog.Objs obj, TetroidLog.Opers oper, TaskStage.Stages stage) {
    //            TaskStage taskStage = new TaskStage(ChangePassTask.class, obj, oper, stage);
    //            String mes = TetroidLog.logTaskStage(mContext, taskStage);
    //            publishProgress(mes);
    //        }
    //
    //        @Override
    //        protected void onProgressUpdate(String... values) {
    //            String mes = values[0];
    ////            mTextViewProgress.setText(mes);
    //            getSettingsActivity().setProgressVisibility(true, mes);
    //        }
    //
    //        @Override
    //        protected void onPostExecute(Boolean res) {
    ////            mLayoutProgress.setVisibility(View.INVISIBLE);
    //            getSettingsActivity().setProgressVisibility(false, null);
    //            if (res) {
    //                LogManager.log(mContext, R.string.log_pass_changed, ILogger.Types.INFO, Toast.LENGTH_SHORT);
    //            } else {
    //                LogManager.log(mContext, R.string.log_pass_change_error, ILogger.Types.INFO, Toast.LENGTH_SHORT);
    //                showSnackMoreInLogs();
    //            }
    //        }
    //    }
    //    private SettingsActivity getSettingsActivity() {
    //        return (SettingsActivity) getActivity();
    //    }
}