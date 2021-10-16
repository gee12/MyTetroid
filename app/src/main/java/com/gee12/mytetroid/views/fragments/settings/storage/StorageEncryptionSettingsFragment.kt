package com.gee12.mytetroid.views.fragments.settings.storage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.gee12.mytetroid.views.fragments.settings.TetroidSettingsFragment
import com.gee12.mytetroid.views.dialogs.AskDialogs
import com.gee12.htmlwysiwygeditor.Dialogs.IApplyCancelResult
import com.gee12.mytetroid.PermissionManager
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.SettingsManager
import com.gee12.mytetroid.views.dialogs.pass.PassDialogs
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.viewmodels.CallbackParam
import com.gee12.mytetroid.viewmodels.StorageEncryptionViewModel
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory
import com.gee12.mytetroid.views.Message
import com.gee12.mytetroid.views.activities.SettingsActivity
import com.gee12.mytetroid.views.dialogs.pass.PassChangeDialog
import com.gee12.mytetroid.views.dialogs.pin.PINCodeDialog
import com.gee12.mytetroid.views.dialogs.pin.PinCodeLengthDialog

class StorageEncryptionSettingsFragment : TetroidSettingsFragment() {

    lateinit var viewModel: StorageEncryptionViewModel

//    private var mCurTask: TetroidTask<*, *, *>? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        viewModel = ViewModelProvider(this, TetroidViewModelFactory(application))
            .get(StorageEncryptionViewModel::class.java)

        // устанавливаем preferenceDataStore после onCreate(), но перед setPreferencesFromResource()
        preferenceManager?.preferenceDataStore = viewModel.prefsDataStore

        setPreferencesFromResource(R.xml.storage_prefs_encryption, rootKey)
        setTitle(R.string.pref_category_crypt, viewModel.getStorageName())

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
                if (checkStorageIsReady(true)) {
                    if (isCrypted) {
                        changePass()
                    } else {
                        setupPass()
                    }
                    // устанавливаем флаг для MainActivity
                    val intent = Intent()
                    intent.putExtra(Constants.EXTRA_IS_PASS_CHANGED, true)
                    requireActivity().setResult(Activity.RESULT_OK, intent)
                }
                true
            }
        }

        // сохранение пароля локально
        val prefIsSavePassLocal = findPreference<CheckBoxPreference>(getString(R.string.pref_key_is_save_pass_hash_local))
        prefIsSavePassLocal?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                if (viewModel.getMiddlePassHash() == null) {
                    // если пароль не задан, то нечего очищать, не задаем вопрос
                    true
                } else {
                    viewModel.askPinCode( true,
                        CallbackParam(Constants.StorageEvents.SavePassHashLocalChanged, newValue)
                    )
//                    viewModel.askPinCode( true, CallbackParam(Constants.StorageEvents.SavePassHashLocalChanged, newValue)) {
//                        changeSavePassHashLocal(newValue as Boolean)
//                    }
                    false
                }
            }

        // can't access viewLifecycleOwner when getView() is null yet
        viewModel.updateStorageField.observe(this, { pair ->
            updateSummary(pair.first, pair.second.toString(), "")
        })

        viewModel.storageEvent.observe(this, { event ->
            onStorageEvent(event.state, event.data)
        })
    }

    fun onStorageEvent(state: Constants.StorageEvents, data: Any?) {
        when (state) {
            Constants.StorageEvents.SavePassHashLocalChanged -> changeSavePassHashLocal(data as Boolean)
            Constants.StorageEvents.SetupPinCode -> showSetupPinCodeDialog()
            Constants.StorageEvents.DropPinCode -> showDropPinCodeDialog()
            else -> {}
        }
    }

    private fun showSetupPinCodeDialog() {
        // задаем длину ПИН-кода
        PinCodeLengthDialog(SettingsManager.getPINCodeLength(context),
            object : PinCodeLengthDialog.IPinLengthInputResult {
                override fun onApply(length: Int) {
                    viewModel.setupPinCodeLength(length)

                    // задаем новый ПИН-код
                    PINCodeDialog.showDialog(
                        length = length,
                        isSetup = true,
                        fragmentManager = parentFragmentManager,
                        callback = object : PINCodeDialog.IPinInputResult {
                            override fun onApply(pin: String): Boolean {
                                viewModel.setupPinCode(pin)
                                return true
                            }

                            override fun onCancel() {}
                        })
                }

                override fun onCancel() {}
            })
    }

    private fun showDropPinCodeDialog() {
        // сбрасываем имеющийся ПИН-код, предварительнго его запросив
        PINCodeDialog.showDialog(
            length = SettingsManager.getPINCodeLength(context),
            isSetup = false,
            fragmentManager = parentFragmentManager,
            callback = object : PINCodeDialog.IPinInputResult {
                override fun onApply(pin: String): Boolean {
                    // зашифровываем введеный пароль перед сравнением
                    return viewModel.checkAndDropPinCode(pin)
                }

                override fun onCancel() {}
            })
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

    private fun checkStorageIsReady(checkIsFavorMode: Boolean): Boolean {
        when {
            !viewModel.isInited() -> {
                val mes = getString(
                    if (PermissionManager.writeExtStoragePermGranted(context)) R.string.title_need_init_storage
                    else R.string.title_need_perm_init_storage
                )
                Message.show(context, mes, Toast.LENGTH_SHORT)
                return false
            }
            !viewModel.isLoaded() -> {
                Message.show(context, getString(R.string.title_need_load_storage), Toast.LENGTH_SHORT)
                return false
            }
            checkIsFavorMode && viewModel.isLoadedFavoritesOnly() -> {
                Message.show(context, getString(R.string.title_need_load_nodes), Toast.LENGTH_SHORT)
                return false
            }
            else -> return true
        }
    }

    fun onBackPressed(): Boolean {
//        return (mCurTask != null && mCurTask!!.isRunning)
        return viewModel.isBusy
    }

    /**
     * Смена пароля хранилища.
     */
    fun changePass() {
        viewModel.log(R.string.log_start_pass_change)
        // вводим пароли (с проверкой на пустоту и равенство)
        PassChangeDialog(object : PassChangeDialog.IPassChangeResult {
            override fun applyPass(curPass: String?, newPass: String?): Boolean {
                return viewModel.checkPass(curPass, { res: Boolean ->
                    if (res) {
//                        mCurTask = ChangePassTask().run(curPass, newPass)
                        viewModel.startChangePass(curPass!!, newPass!!)
                    }
                }, R.string.log_cur_pass_is_incorrect)
            }
        }).showIfPossible(parentFragmentManager)

    }

    /**
     * Установка пароля хранилища впервые.
     */
    fun setupPass() {
        viewModel.log(R.string.log_start_pass_setup)
        // вводим пароль
        PassDialogs.showPassEnterDialog(
            node = null,
            isNewPass = true,
            fragmentManager = parentFragmentManager,
            passResult = object : PassDialogs.IPassInputResult {
                override fun applyPass(pass: String, node: TetroidNode?) {
                    viewModel.setupPass(pass)
                }

                override fun cancelPass() {}
            })
    }

    private val settingsActivity: SettingsActivity?
        get() = activity as SettingsActivity?
}