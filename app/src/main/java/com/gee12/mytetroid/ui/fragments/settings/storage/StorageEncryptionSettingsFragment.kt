package com.gee12.mytetroid.ui.fragments.settings.storage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.htmlwysiwygeditor.Dialogs.IApplyCancelResult
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.ui.dialogs.pass.PassDialogs
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.viewmodels.StorageViewModel.StorageEvent
import com.gee12.mytetroid.ui.dialogs.pass.PassChangeDialog

class StorageEncryptionSettingsFragment : TetroidStorageSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
    }

    override fun onStorageEvent(event: StorageEvent) {
        when (event) {
            StorageEvent.PassChanged,
            StorageEvent.PassSetuped -> onPasswordChanged()
            is StorageEvent.SavePassHashLocalChanged -> changeSavePassHashLocal(event.isSaveLocal)
            else -> super.onStorageEvent(event)
        }
    }

    override fun onStorageFoundInBase(storage: TetroidStorage) {
        setTitle(R.string.pref_category_crypt, storage.name)

        // устанавливаем preferenceDataStore после onCreate(), но перед setPreferencesFromResource()
        preferenceManager?.preferenceDataStore = viewModel.prefsDataStore

        setPreferencesFromResource(R.xml.storage_prefs_encryption, null)

        // сохранение пароля локально
        findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_save_pass_hash_local))
            ?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            return@OnPreferenceChangeListener viewModel.onPassLocalHashLocalParamChanged(isSaveLocal = newValue as Boolean)
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
        findPreference<Preference>(getString(R.string.pref_key_change_pass))?.apply {
            val isCrypted = if (viewModel.isStorageInited()) viewModel.isStorageCrypted() else false
            setTitle(if (isCrypted) R.string.pref_change_pass else R.string.pref_setup_pass)
            setSummary(if (isCrypted) R.string.pref_change_pass_summ else R.string.pref_setup_pass_summ)
            isEnabled = viewModel.checkStorageIsReady(
                checkIsFavorMode = true,
                showMessage = false
            )
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (viewModel.checkStorageIsReady(
                        checkIsFavorMode = true,
                        showMessage = true
                    )) {
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
     * Обработка изменения опции локального сохранения пароля.
     */
    private fun changeSavePassHashLocal(newValue: Boolean) {
        if (newValue) {
            viewModel.saveMiddlePassHashLocalIfCached()
            (findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_save_pass_hash_local)))?.isChecked = true
        } else if (viewModel.isSaveMiddlePassLocal() && !viewModel.getMiddlePassHash().isNullOrEmpty()) {
            // удалить сохраненный хэш пароля?
            AskDialogs.showYesNoDialog(
                context,
                object : IApplyCancelResult {
                    override fun onApply() {
                        viewModel.dropSavedLocalPassHash()
                        (findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_save_pass_hash_local)))?.isChecked = false
                    }

                    override fun onCancel() {
                        // устанавливаем галку обратно
    //                    SettingsManager.setIsSaveMiddlePassHashLocal(true);
                    }
                },
                R.string.ask_clear_saved_pass_hash
            )
        }
    }

    override fun onBackPressed(): Boolean {
        return viewModel.isBusy
    }

    /**
     * Смена пароля хранилища.
     */
    private fun changePass() {
        viewModel.log(R.string.log_start_pass_change)
        // вводим пароли
        PassChangeDialog { curPass, newPass ->
            // проверяем введенный текущий пароль
            viewModel.checkPassAndChange(
                curPass = curPass,
                newPass = newPass,
            )
        }.showIfPossible(parentFragmentManager)
    }

    /**
     * Установка пароля хранилища впервые.
     */
    private fun setupPass() {
        PassDialogs.showPasswordEnterDialog(
            isSetup = true,
            fragmentManager = parentFragmentManager,
            passResult = object : PassDialogs.IPassInputResult {
                override fun applyPass(pass: String) {
                    viewModel.startSetupPass(pass)
                }

                override fun cancelPass() {
                }
            }
        )
    }

}