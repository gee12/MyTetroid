package com.gee12.mytetroid.views.fragments.settings.storage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.gee12.mytetroid.views.dialogs.AskDialogs
import com.gee12.htmlwysiwygeditor.Dialogs.IApplyCancelResult
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.views.dialogs.pass.PassDialogs
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.views.dialogs.pass.PassChangeDialog

class StorageEncryptionSettingsFragment : TetroidStorageSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
    }

    override fun onStorageEvent(event: Constants.StorageEvents, data: Any?) {
        when (event) {
            Constants.StorageEvents.PassChanged,
            Constants.StorageEvents.PassSetuped -> onPasswordChanged()
            Constants.StorageEvents.SavePassHashLocalChanged -> changeSavePassHashLocal(data as Boolean)
//            Constants.StorageEvents.SetupPinCode -> showSetupPinCodeDialog()
//            Constants.StorageEvents.DropPinCode -> showDropPinCodeDialog()
            else -> super.onStorageEvent(event, data)
        }
    }

//    override fun onUpdateStorageFieldEvent(key: String, value: String) {
//        // FIXME: нужно ли это здесь ?
//        updateSummary(key, value, "")
//    }

    override fun onStorageInited(storage: TetroidStorage) {
        setTitle(R.string.pref_category_crypt, storage.name)

        // устанавливаем preferenceDataStore после onCreate(), но перед setPreferencesFromResource()
        preferenceManager?.preferenceDataStore = viewModel.prefsDataStore

        setPreferencesFromResource(R.xml.storage_prefs_encryption, null)

        // сохранение пароля локально
        findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_save_pass_hash_local))
            ?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            return@OnPreferenceChangeListener viewModel.onPassLocalHashLocalParamChanged(newValue)
        }

        updateChangeSetupPasswordPref()
    }

    private fun onPasswordChanged() {
        updateChangeSetupPasswordPref()

        // устанавливаем флаг для MainActivity
        val intent = Intent()
        intent.putExtra(Constants.EXTRA_IS_PASS_CHANGED, true)
        requireActivity().setResult(Activity.RESULT_OK, intent)
    }

    private fun updateChangeSetupPasswordPref() {
        // установка или смена пароля хранилища
        val isStorageReady = viewModel.isInited()
                && viewModel.isLoaded()
                && !viewModel.isLoadedFavoritesOnly()
        findPreference<Preference>(getString(R.string.pref_key_change_pass))?.apply {
            val isCrypted = viewModel.isCrypted()
            setTitle(if (isCrypted) R.string.pref_change_pass else R.string.pref_setup_pass)
            setSummary(if (isCrypted) R.string.pref_change_pass_summ else R.string.pref_setup_pass_summ)
            isEnabled = isStorageReady
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (viewModel.checkStorageIsReady(true)) {
                    if (isCrypted) {
                        changePass()
                    } else {
                        setupPass()
                    }
                }
                true
            }
        }
    }

    /**
     * Обработка сброса опции локального сохранения пароля.
     */
    private fun changeSavePassHashLocal(newValue: Boolean) {
        if (!newValue && viewModel.isSaveMiddlePassLocal()) {
            // удалить сохраненный хэш пароля?
            AskDialogs.showYesNoDialog(context, object : IApplyCancelResult {
                override fun onApply() {
                    viewModel.dropSavedLocalPassHash()
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

    fun onBackPressed(): Boolean {
        return viewModel.isBusy
    }

    /**
     * Смена пароля хранилища.
     */
    fun changePass() {
        viewModel.log(R.string.log_start_pass_change)
        // вводим пароли
        PassChangeDialog(object : PassChangeDialog.IPassChangeResult {
            override fun applyPass(curPass: String, newPass: String): Boolean {
                // проверяем введенный текущий пароль
                return viewModel.checkPass(curPass, { isPassCorrect ->
                    if (isPassCorrect) {
                        viewModel.startChangePass(curPass, newPass)
                    }
                }, R.string.log_cur_pass_is_incorrect)
            }
        }).showIfPossible(parentFragmentManager)
    }

    /**
     * Установка пароля хранилища впервые.
     */
    fun setupPass() {
        PassDialogs.showPassEnterDialog(
//            node = null,
            isSetup = true,
            fragmentManager = parentFragmentManager,
            passResult = object : PassDialogs.IPassInputResult {
                override fun applyPass(pass: String/*, node: TetroidNode?*/) {
                    viewModel.startSetupPass(pass)
                }

                override fun cancelPass() {
                }
            })
    }

}