package com.gee12.mytetroid.interactors

import android.content.Context
import android.widget.Toast
import com.gee12.htmlwysiwygeditor.Dialogs.IApplyCancelResult
import com.gee12.htmlwysiwygeditor.Dialogs.IApplyResult
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.data.ini.DatabaseConfig.EmptyFieldException

class PinInteractor(
    val passwordInteractor: PasswordInteractor,
    val encryptionInteractor: EncryptionInteractor,
    val nodesInteractor: NodesInteractor
) {

    fun isCrypted() = nodesInteractor.isExistCryptedNodes(false)

//    /**
//     * Запрос ПИН-кода, если установлена опция.
//     * К этому моменту факт того, что хэш пароля сохранен локально, должен быть уже проверен.
//     * @param context
//     * @param specialFlag Дополнительный признак, указывающий на то, нужно ли спрашивать ПИН-код
//     * конкретно в данный момент.
//     * @param callback Обработчик обратного вызова.
//     */
//    fun askPINCode(context: Context, specialFlag: Boolean, callback: IApplyResult) {
//        if (isRequestPINCode(context) && specialFlag) {
//            // выводим запрос ввода ПИН-кода
//            PassDialogs.showPINCodeDialog(context, SettingsManager.getPINCodeLength(context), false, object : IPinInputResult {
//                override fun onApply(pin: String): Boolean {
//                    // зашифровываем введеный пароль перед сравнением
//                    val pinHash: String = encryptionInteractor.crypter.passToHash(pin)
//                    val res = pinHash == SettingsManager.getPINCodeHash(context)
//                    if (res) {
//                        callback.onApply()
//                        // сбрасываем признак
//                        StorageManager.resetIsPINNeedToEnter()
//                        LogManager.log(context, R.string.log_pin_code_enter)
//                    }
//                    return res
//                }
//
//                override fun onCancel() {}
//            })
//        } else {
//            callback.onApply()
//        }
//    }

//    /**
//     * Установка/очистка ПИН-кода.
//     * Вызывается при установке/снятии опции.
//     * При установке сначала проверяется факт того, что хэш пароля сохранен локально.
//     */
//    fun setupPINCode(context: Context, callback: ICallback) {
//        if (!isRequestPINCode(context)) {
//            // проверяем сохраненный пароль
//            checkPass(context, object : IApplyCancelResult {
//                override fun onApply() {
//
//                    // задаем длину ПИН-кода
//                    PassDialogs.showPinCodeLengthDialog(context, SettingsManager.getPINCodeLength(context),
//                        object : IPinLengthInputResult {
//                            override fun onApply(length: Int) {
//                                SettingsManager.setPINCodeLength(context, length)
//                                LogManager.log(context, context.getString(R.string.log_pin_code_length_setup) + length)
//                                // задаем новый ПИН-код
//                                PassDialogs.showPINCodeDialog(context, length, true, object : IPinInputResult {
//                                    override fun onApply(pin: String): Boolean {
//                                        // зашифровываем пароль перед установкой
//                                        val pinHash: String = encryptionInteractor.crypter.passToHash(pin)
//                                        SettingsManager.setPINCodeHash(context, pinHash)
//                                        callback.run(true)
//                                        // устанавливаем признак
//                                        StorageManager.setIsPINNeedToEnter()
//                                        LogManager.log(context, R.string.log_pin_code_setup, Toast.LENGTH_SHORT)
//                                        return true
//                                    }
//
//                                    override fun onCancel() {}
//                                })
//                            }
//
//                            override fun onCancel() {}
//                        })
//                }
//
//                override fun onCancel() {}
//            })
//        } else {
//            // сбрасываем имеющийся ПИН-код, предварительнго его запросив
//            PassDialogs.showPINCodeDialog(context, SettingsManager.getPINCodeLength(context), false,
//                object : IPinInputResult {
//                    override fun onApply(pin: String): Boolean {
//                        // зашифровываем введеный пароль перед сравнением
//                        val pinHash: String = encryptionInteractor.crypter.passToHash(pin)
//                        val res = pinHash == SettingsManager.getPINCodeHash(context)
//                        if (res) {
//                            // очищаем
//                            SettingsManager.setPINCodeHash(context, null)
//                            callback.run(false)
//                            LogManager.log(context, R.string.log_pin_code_clean, Toast.LENGTH_SHORT)
//                        }
//                        return res
//                    }
//
//                    override fun onCancel() {}
//                })
//        }
//    }

//    /**
//     *
//     * @param context
//     * @param callback
//     */
//    fun checkPass(context: Context, callback: IApplyCancelResult) {
//        var middlePassHash: String?
//        if (SettingsManager.getMiddlePassHash(context).also { middlePassHash = it } != null) {
//            // хэш пароля сохранен "на диске", проверяем
//            try {
//                if (passwordInteractor.checkMiddlePassHash(middlePassHash)) {
//
//                    // задавать не нужно ?
////                    DataManager.initCryptPass(middlePassHash, true);
//                    callback.onApply()
//                } else {
//                    LogManager.log(context, R.string.log_wrong_saved_pass, Toast.LENGTH_LONG)
//                    // спрашиваем пароль
//                    passwordInteractor.askPassword(context, null, callback)
//                }
//            } catch (ex: EmptyFieldException) {
//                // если поля в INI-файле для проверки пустые
//                LogManager.log(context, ex)
////                if (DataManager.isExistsCryptedNodes()) {
//                if (isCrypted()) {
////                    final String hash = middlePassHash;
//                    // спрашиваем "continue anyway?"
//                    PassDialogs.showEmptyPassCheckingFieldDialog(context, ex.fieldName,
//                        object : IApplyCancelResult {
//                            override fun onApply() {
////                              DataManager.initCryptPass(hash, true);
//                                callback.onApply()
//                            }
//
//                            override fun onCancel() {
//                                callback.onCancel()
//                            }
//                        })
//                } else {
//                    // если нет зашифрованных веток, но пароль сохранен
////                    DataManager.initCryptPass(middlePassHash, true);
//                    callback.onApply()
//                }
//            }
////            } else {
////                // пароль не сохранен, вводим
////                askPassword(node, callback);
////            }
//        } else {
//            // спрашиваем или задаем пароль
//            passwordInteractor.askPassword(context, null, callback)
//        }
//    }
}