package com.gee12.mytetroid.views.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.views.dialogs.pin.PinCodeDialog
import com.gee12.mytetroid.views.dialogs.pin.PinCodeLengthDialog


class SettingsEncryptionFragment : TetroidSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_encryption, rootKey)
        requireActivity().setTitle(R.string.pref_category_crypt)

        // установка ПИН-кода
        findPreference<CheckBoxPreference>(getString(R.string.pref_key_request_pin_code))?.let {
            disableIfFree(it)
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, _: Any? ->
                    if (baseViewModel.isRequestPINCode()) {
                        showDropPinCodeDialog()
                    } else {
                        showSetupPinCodeDialog()
                    }
                    false
                }
        }

        // когда запрашивать пароль
        findPreference<Preference>(getString(R.string.pref_key_when_ask_password))
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (CommonSettings.isSaveMiddlePassHashLocalDef(context)) {
                baseViewModel.showMessage(R.string.title_not_avail_when_save_pass)
            }
            true
        }
        updateSummary(
            R.string.pref_key_when_ask_password,
            getString(R.string.pref_when_ask_password_summ_mask, CommonSettings.getWhenAskPass(context))
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
        if (key == getString(R.string.pref_key_when_ask_password)) {
            updateSummary(
                R.string.pref_key_when_ask_password,
                getString(R.string.pref_when_ask_password_summ_mask, CommonSettings.getWhenAskPass(context))
            )
        }
    }

    private fun setPinCodePrefIsChecked(isChecked: Boolean) {
        findPreference<CheckBoxPreference>(getString(R.string.pref_key_request_pin_code))?.let {
            it.isChecked = isChecked
        }
    }

    private fun showSetupPinCodeDialog() {
        // задаем длину ПИН-кода
        PinCodeLengthDialog(
            CommonSettings.getPinCodeLength(context),
            Constants.MIN_PINCODE_LENGTH,
            Constants.MAX_PINCODE_LENGTH,
            object : PinCodeLengthDialog.IPinLengthInputResult {
                override fun onApply(length: Int) {
                    baseViewModel.setupPinCodeLength(length)

                    // задаем новый ПИН-код
                    PinCodeDialog.showDialog(
                        length = length,
                        isSetup = true,
                        fragmentManager = parentFragmentManager,
                        callback = object : PinCodeDialog.IPinInputResult {
                            override fun onApply(pin: String): Boolean {
                                baseViewModel.setupPinCode(pin)
                                setPinCodePrefIsChecked(true)
                                return true
                            }

                            override fun onCancel() {}
                        })
                }

                override fun onCancel() {}
            }
        ).showIfPossible(parentFragmentManager)
    }

    private fun showDropPinCodeDialog() {
        // сбрасываем имеющийся ПИН-код, предварительнго его запросив
        PinCodeDialog.showDialog(
            length = CommonSettings.getPinCodeLength(context),
            isSetup = false,
            fragmentManager = parentFragmentManager,
            callback = object : PinCodeDialog.IPinInputResult {
                override fun onApply(pin: String): Boolean {
                    return baseViewModel.checkAndDropPinCode(pin).also {
                        if (it) setPinCodePrefIsChecked(false)
                    }
                }

                override fun onCancel() {}
            }
        )
    }

}