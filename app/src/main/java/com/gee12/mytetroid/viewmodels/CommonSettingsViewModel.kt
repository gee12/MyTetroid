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
                /*&& isPinNeedEnter*/
    }

//    fun isPinCodeSetup(): Boolean {
//        return CommonSettings.getPINCodeHash(getContext()) != null
//    }

//    /**
//     * Запрос ПИН-кода, если установлена опция.
//     * К этому моменту факт того, что хэш пароля сохранен локально, должен быть уже проверен.
//     * @param specialFlag Дополнительный признак, указывающий на то, нужно ли спрашивать ПИН-код
//     * конкретно в данный момент.
//     * @param callback Обработчик обратного вызова.
//     */
//    fun askPinCode(specialFlag: Boolean, callback: EventCallbackParams /*callback: Dialogs.IApplyResult*/) {
//        if (isRequestPINCode() && specialFlag) {
//            // выводим запрос ввода ПИН-кода
//            postStorageEvent(Constants.StorageEvents.AskPinCode, callback)
//        } else {
//            postEventFromCallbackParam(callback)
//        }
//    }

//    fun startCheckPinCode(pin: String, callback: EventCallbackParams): Boolean {
//        // зашифровываем введеный пароль перед сравнением
//        val res = checkPinCode(pin)
//        if (res) {
//            postEventFromCallbackParam(callback)
//            // сбрасываем признак
////            isPinNeedEnter = false
//            logger.log(R.string.log_pin_code_enter)
//        }
//        return res
//    }

//    /**
//     * Установка/очистка ПИН-кода.
//     * Вызывается при установке/снятии опции.
//     * ---При установке сначала проверяется факт того, что хэш пароля сохранен локально.
//     */
//    fun startSetupOrDropPinCode(callback: EventCallbackParams) {
//        if (!isRequestPINCode()) {
//            checkStoragePass(EventCallbackParams(Constants.StorageEvents.SetupPinCode, callback))
//        } else {
//            checkStoragePass(EventCallbackParams(Constants.StorageEvents.DropPinCode, callback))
//        }
//    }

    fun setupPinCodeLength(length: Int) {
        CommonSettings.setPINCodeLength(getContext(), length)
        logger.log(getString(R.string.log_pin_code_length_setup) + length, false)
    }

    fun setupPinCode(pin: String) {
        // сохраняем хеш
//        val pinHash: String = crypter.passToHash(pin)
        val pinHash: String = Utils.toMD5Hex(pin)
        CommonSettings.setPINCodeHash(getContext(), pinHash)
        // устанавливаем признак
//        isPinNeedEnter = true
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