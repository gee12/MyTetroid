package com.gee12.mytetroid.views.fragments.settings.storage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.gee12.mytetroid.views.fragments.settings.TetroidSettingsFragment
import com.gee12.mytetroid.TetroidTask
import com.gee12.mytetroid.App
import com.gee12.mytetroid.data.PassManager
import com.gee12.mytetroid.data.PINManager
import com.gee12.mytetroid.views.dialogs.AskDialogs
import com.gee12.htmlwysiwygeditor.Dialogs.IApplyCancelResult
import com.gee12.mytetroid.PermissionManager
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.views.dialogs.PassDialogs
import com.gee12.mytetroid.data.ITaskProgress
import com.gee12.mytetroid.logs.TetroidLog.Objs
import com.gee12.mytetroid.logs.TetroidLog.Opers
import com.gee12.mytetroid.logs.TaskStage.Stages
import com.gee12.mytetroid.logs.TaskStage.ITaskStageExecutor
import com.gee12.mytetroid.logs.TaskStage
import com.gee12.mytetroid.logs.TetroidLog
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.viewmodels.StorageSettingsViewModel
import com.gee12.mytetroid.viewmodels.StorageViewModelFactory
import com.gee12.mytetroid.views.Message
import com.gee12.mytetroid.views.activities.SettingsActivity

class StorageEncryptionSettingsFragment : TetroidSettingsFragment() {

    private lateinit var mViewModel: StorageSettingsViewModel
//    private lateinit var mCryptViewModel: CryptViewModel

    private var mCurTask: TetroidTask<*, *, *>? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        mViewModel = ViewModelProvider(activity!!, StorageViewModelFactory(application))
            .get(StorageSettingsViewModel::class.java)
//        mCryptViewModel = ViewModelProvider(this, StoragesViewModelFactory(application))
//            .get(CryptViewModel::class.java)
        // устанавливаем preferenceDataStore после onCreate(), но перед setPreferencesFromResource()
        preferenceManager?.preferenceDataStore = mViewModel.prefsDataStore

        setPreferencesFromResource(R.xml.storage_prefs_encryption, rootKey)
        setTitle(R.string.pref_category_crypt, mViewModel.getStorageName())

        // установка или смена пароля хранилища
        val isStorageReady = mViewModel.isInited() && mViewModel.isLoaded() && !App.IsLoadedFavoritesOnly
        val passPref = findPreference<Preference>(getString(R.string.pref_key_change_pass))
        val isCrypted = mViewModel.isCrypted()
        passPref!!.setTitle(if (isCrypted) R.string.pref_change_pass else R.string.pref_setup_pass)
        passPref.setSummary(if (isCrypted) R.string.pref_change_pass_summ else R.string.pref_setup_pass_summ)
        passPref.isEnabled = isStorageReady
        passPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (checkStorageIsReady(true)) {
                if (isCrypted) {
                    changePass()
                } else {
                    PassManager.setupPass(context)
                }
                // устанавливаем флаг для MainActivity
                val intent = Intent()
                intent.putExtra(Constants.EXTRA_IS_PASS_CHANGED, true)
                activity!!.setResult(Activity.RESULT_OK, intent)
            }
            true
        }

        // сохранение пароля локально
        val prefIsSavePassLocal = findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_save_pass_hash_local))
        prefIsSavePassLocal?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                if (mViewModel.getMiddlePassHash() == null) {
                    // если пароль не задан, то нечего очищать, не задаем вопрос
                    true
                } else {
                    PINManager.askPINCode(context, true) { changeSavePassHashLocal(newValue as Boolean) }
                    false
                }
            }

        // can't access viewLifecycleOwner when getView() is null yet
        mViewModel.updateStorageField.observe(this, { pair ->
            updateSummary(pair.first, pair.second.toString(), "")
        })
    }

    /**
     * Обработка сброса опции локального сохранения пароля.
     */
    private fun changeSavePassHashLocal(newValue: Boolean) {
        if (!newValue && mViewModel.isSaveMiddlePassLocal()) {
            // удалить сохраненный хэш пароля?
            AskDialogs.showYesNoDialog(context, object : IApplyCancelResult {
                override fun onApply() {
                    mViewModel.dropSavedLocalPassHash()
                    // удаляем хэш пароля, если сняли галку
//                    mViewModel.setMiddlePassHash(null)
                    // сбрасываем галку
//                    mViewModel.setIsSaveMiddlePassHashLocal(false)
                    (findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_save_pass_hash_local)))?.isChecked = false
                    // сбрасываем ПИН-код
//                    mViewModel.setIsRequestPINCode(false)
//                    mViewModel.setPINCodeHash(null)
                    (findPreference<CheckBoxPreference>(getString(R.string.pref_key_request_pin_code)))?.isChecked = false
//                    setPINCodePrefAvailability()
                }

                override fun onCancel() {
                    // устанавливаем галку обратно
//                    SettingsManager.setIsSaveMiddlePassHashLocal(true);
                }
            }, R.string.ask_clear_saved_pass_hash)
        }
    }

/*    private fun setPINCodePrefAvailability() {
        if (App.isFullVersion()) {
            findPreference<Preference>(getString(R.string.pref_key_request_pin_code))!!.isEnabled =
                mViewModel.isSaveMiddlePassLocal()
        }
    }*/

    private fun checkStorageIsReady(checkIsFavorMode: Boolean): Boolean {
        if (!mViewModel.isInited()) {
            val mes =
                getString(if (PermissionManager.writeExtStoragePermGranted(context)) R.string.title_need_init_storage else R.string.title_need_perm_init_storage)
            Message.show(context, mes, Toast.LENGTH_SHORT)
            return false
        } else if (!mViewModel.isLoaded()) {
            Message.show(context, getString(R.string.title_need_load_storage), Toast.LENGTH_SHORT)
            return false
        } else if (checkIsFavorMode && App.IsLoadedFavoritesOnly) {
            Message.show(context, getString(R.string.title_need_load_nodes), Toast.LENGTH_SHORT)
            return false
        }
        return true
    }

    fun onBackPressed(): Boolean {
        return (mCurTask != null && mCurTask!!.isRunning)
    }

    /**
     * Смена пароля хранилища.
     */
    fun changePass() {
        LogManager.log(mContext, R.string.log_start_pass_change)
        // вводим пароли (с проверкой на пустоту и равенство)
        PassDialogs.showPassChangeDialog(context) { curPass: String?, newPass: String? ->
            PassManager.checkPass(context, curPass, { res: Boolean ->
                if (res) {
                    mCurTask = ChangePassTask().run(curPass, newPass)
                }
            }, R.string.log_cur_pass_is_incorrect)
        }
    }

    /**
     * Задание (параллельный поток), в котором выполняется перешифровка хранилища.
     */
    inner class ChangePassTask : TetroidTask<String?, String?, Boolean?>(activity) {
        override fun onPreExecute() {
            settingsActivity!!.setProgressVisibility(true, getString(R.string.task_pass_changing))
        }

        override fun doInBackground(vararg values: String?): Boolean {
            val curPass = values[0]
            val newPass = values[1]
            return PassManager.changePass(context, curPass, newPass, object : ITaskProgress {
                override fun nextStage(obj: Objs, oper: Opers, stage: Stages) {
                    setStage(obj, oper, stage)
                }

                override fun nextStage(obj: Objs, oper: Opers, stageExecutor: ITaskStageExecutor): Boolean {
                    setStage(obj, oper, Stages.START)
                    return if (stageExecutor.execute()) {
                        setStage(obj, oper, Stages.SUCCESS)
                        true
                    } else {
                        setStage(obj, oper, Stages.FAILED)
                        false
                    }
                }
            })
        }

        private fun setStage(obj: Objs, oper: Opers, stage: Stages) {
            val taskStage = TaskStage(ChangePassTask::class.java, obj, oper, stage)
            val mes = TetroidLog.logTaskStage(mContext, taskStage)
            publishProgress(mes)
        }

        override fun onProgressUpdate(vararg values: String?) {
            val mes = values[0]
            settingsActivity!!.setProgressVisibility(true, mes)
        }

        override fun onPostExecute(res: Boolean?) {
            settingsActivity!!.setProgressVisibility(false, null)
            if (res == true) {
                LogManager.log(mContext, R.string.log_pass_changed, ILogger.Types.INFO, Toast.LENGTH_SHORT)
            } else {
                LogManager.log(mContext, R.string.log_pass_change_error, ILogger.Types.INFO, Toast.LENGTH_SHORT)
                showSnackMoreInLogs()
            }
        }
    }

    private val settingsActivity: SettingsActivity?
        get() = activity as SettingsActivity?
}