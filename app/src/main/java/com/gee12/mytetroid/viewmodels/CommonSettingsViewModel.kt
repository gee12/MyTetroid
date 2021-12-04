package com.gee12.mytetroid.viewmodels

import android.app.Application
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.CommonSettings
import com.gee12.mytetroid.repo.CommonSettingsRepo
import com.gee12.mytetroid.utils.Utils

class CommonSettingsViewModel(
    application: Application,
    settingsRepo: CommonSettingsRepo
) : BaseViewModel(application, settingsRepo) {


    //region Pin

    /**
     * Проверка использования ПИН-кода с учетом версии приложения.
     * @return
     */
    fun isRequestPINCode(): Boolean {
        return App.isFullVersion()
                && CommonSettings.isRequestPINCode(getContext())
                && CommonSettings.getPINCodeHash(getContext()) != null
    }

    fun setupPinCodeLength(length: Int) {
        CommonSettings.setPINCodeLength(getContext(), length)
        logger.log(getString(R.string.log_pin_code_length_setup) + length, false)
    }

    fun setupPinCode(pin: String) {
        // сохраняем хеш
//        val pinHash: String = crypter.passToHash(pin)
        val pinHash: String = Utils.toMD5Hex(pin)
        CommonSettings.setPINCodeHash(getContext(), pinHash)
        logger.log(R.string.log_pin_code_setup, true)
    }

    fun checkAndDropPinCode(pin: String): Boolean {
        return checkPinCode(pin).also {
            if (it) dropPinCode()
        }
    }

    fun checkPinCode(pin: String): Boolean {
        // сравниваем хеши
//        val pinHash = crypter.passToHash(pin)
        val pinHash = Utils.toMD5Hex(pin)
        return (pinHash == CommonSettings.getPINCodeHash(getContext()))
    }

    protected fun dropPinCode() {
        CommonSettings.setPINCodeHash(getContext(), null)
        logger.log(R.string.log_pin_code_dropped, true)
    }

    //endregion Pin

}