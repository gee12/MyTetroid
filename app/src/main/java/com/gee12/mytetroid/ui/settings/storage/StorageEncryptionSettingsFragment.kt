package com.gee12.mytetroid.ui.settings.storage

import android.app.Activity
import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.buildIntent
import com.gee12.mytetroid.ui.dialogs.pass.PassDialogs
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.storage.StorageEvent
import com.gee12.mytetroid.ui.dialogs.pass.PassChangeDialog
import com.gee12.mytetroid.ui.base.TetroidStorageSettingsFragment
import com.gee12.mytetroid.ui.base.views.prefs.DisabledPreference

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

        // отключаем опцию пока еще не проинициализировано хранилище
        findPreference<DisabledPreference>(getString(R.string.pref_key_change_pass))?.also {
            it.isEnabled = false
        }

        // сохранение пароля локально
        findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_save_pass_hash_local))?.also {
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                viewModel.onPasswordLocalHashLocalParamChanged(isSaveLocal = newValue as Boolean)
            }
        }

        updateChangeSetupPasswordPref()
    }

    override fun onStorageInited(storage: TetroidStorage) {
        updateChangeSetupPasswordPref()
    }

    private fun onPasswordChanged() {
        updateChangeSetupPasswordPref()

        // устанавливаем флаг для MainActivity
        val intent = buildIntent {
            putExtra(Constants.EXTRA_IS_PASS_CHANGED, true)
        }
        requireActivity().setResult(Activity.RESULT_OK, intent)
    }

    private fun updateChangeSetupPasswordPref() {
        // установка или смена пароля хранилища
        findPreference<DisabledPreference>(getString(R.string.pref_key_change_pass))?.also {
            val isEncrypted = if (viewModel.isStorageInited()) viewModel.isStorageEncrypted() else false
            setTitle(if (isEncrypted) R.string.pref_change_pass else R.string.pref_setup_pass)
            it.setSummary(if (isEncrypted) R.string.pref_change_pass_summ else R.string.pref_setup_pass_summ)
            it.isEnabled = viewModel.checkStorageIsReady(
                checkIsFavorMode = true,
                showMessage = false
            )
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (viewModel.checkStorageIsReady(
                        checkIsFavorMode = true,
                        showMessage = true
                    )
                ) {
                    if (isEncrypted) {
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
    private fun changeSavePassHashLocal(isSavePasswordHashLocal: Boolean) {
        val pref = findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_save_pass_hash_local))
        when {
            isSavePasswordHashLocal -> {
                viewModel.saveMiddlePassHashLocalIfCached()
                pref?.isChecked = true
            }
            viewModel.getMiddlePassHash().isNullOrEmpty() -> {
                pref?.isChecked = false
            }
            viewModel.isSaveMiddlePassLocal() -> {
                // удалить сохраненный хэш пароля?
                AskDialogs.showYesNoDialog(
                    context = requireContext(),
                    messageResId = R.string.ask_clear_saved_pass_hash,
                    onApply = {
                        viewModel.clearSavedPasswordHash(
                            isDropSavePasswordLocal = true,
                        )
                        pref?.isChecked = false
                    },
                    onCancel = {},
                )
            }
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
            viewModel.checkPasswordAndChange(
                curPassword = curPass,
                newPassword = newPass,
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
                    viewModel.startSetupPassword(pass)
                }
                override fun cancelPass() {
                }
            }
        )
    }

}