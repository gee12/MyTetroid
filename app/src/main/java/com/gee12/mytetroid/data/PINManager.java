package com.gee12.mytetroid.data;

import android.content.Context;
import android.widget.Toast;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.crypt.CryptManager;
import com.gee12.mytetroid.dialogs.PassDialogs;

public class PINManager {

    /**
     * Проверка использования ПИН-кода с учетом версии приложения.
     * @return
     */
    public static boolean isRequestPINCode() {
        return App.isFullVersion() && SettingsManager.isRequestPINCode();
    }

    /**
     * Запрос ПИН-кода, если установлена опция.
     * К этому моменту факт того, что хэш пароля сохранен локально, должен быть уже проверен.
     * @param context
     */
    public static void askPINCode(Context context, boolean isNodeOpening, Dialogs.IApplyResult callback) {
        if (isRequestPINCode() && isNodeOpening) {
            // выводим запрос ввода ПИН-кода
            PassDialogs.showPINCodeDialog(context, SettingsManager.getPINCodeLength(), false, new PassDialogs.IPinInputResult() {
                @Override
                public boolean onApply(String pin) {
                    // зашифровываем введеный пароль перед сравнением
                    String pinHash = CryptManager.passToHash(pin);
                    boolean res = pinHash.equals(SettingsManager.getPINCodeHash());
                    if (res) {
                        callback.onApply();
                        LogManager.log(R.string.log_pin_code_enter);
                    }
                    return res;
                }

                @Override
                public void onCancel() {
                }
            });
        } else {
            callback.onApply();
        }
    }

    /**
     * Установка/очистка ПИН-кода.
     * Вызывается при установке/снятии опции.
     * При установке сначала проверяется факт того, что хэш пароля сохранен локально.
     */
    public static void setupPINCode(Context context, ICallback callback) {
        if (!isRequestPINCode()) {
            // проверяем сохраненный пароль
            checkPass(context, new Dialogs.IApplyCancelResult() {
                @Override
                public void onApply() {
                    // задаем длину ПИН-кода
                    PassDialogs.showPinCodeLengthDialog(context, SettingsManager.getPINCodeLength(), new PassDialogs.IPinLengthInputResult() {
                        @Override
                        public void onApply(int length) {
                            SettingsManager.setPINCodeLength(length);
                            LogManager.log(context.getString(R.string.log_pin_code_length_setup) + length);
                            // задаем новый ПИН-код
                            PassDialogs.showPINCodeDialog(context, length, true, new PassDialogs.IPinInputResult() {
                                @Override
                                public boolean onApply(String pin) {
                                    // зашифровываем пароль перед установкой
                                    String pinHash = CryptManager.passToHash(pin);
                                    SettingsManager.setPINCodeHash(pinHash);
                                    callback.run(true);
                                    LogManager.log(R.string.log_pin_code_setup, Toast.LENGTH_SHORT);
                                    return true;
                                }
                                @Override
                                public void onCancel() {
//                                    callback.run(false);
                                }
                            });
                        }
                        @Override
                        public void onCancel() {
//                            callback.run(false);
                        }
                    });
                }
                @Override
                public void onCancel() {
//                    callback.run(false);
                }
            });
        } else {
            // сбрасываем имеющийся ПИН-код, предварительнго его запросив
            PassDialogs.showPINCodeDialog(context, SettingsManager.getPINCodeLength(), false, new PassDialogs.IPinInputResult() {
                @Override
                public boolean onApply(String pin) {
                    // зашифровываем введеный пароль перед сравнением
                    String pinHash = CryptManager.passToHash(pin);
                    boolean res = pinHash.equals(SettingsManager.getPINCodeHash());
                    if (res) {
                        // очищаем
                        SettingsManager.setPINCodeHash(null);
                        callback.run(false);
                        LogManager.log(R.string.log_pin_code_clean, Toast.LENGTH_SHORT);
                    }
                    return res;
                }
                @Override
                public void onCancel() {
//                    callback.run(true);
                }
            });
        }
    }

    /**
     *
     * @param context
     * @param callback
     */
    public static void checkPass(Context context, Dialogs.IApplyCancelResult callback) {
        String middlePassHash;
        if ((middlePassHash = SettingsManager.getMiddlePassHash()) != null) {
            // хэш пароля сохранен "на диске", проверяем
            try {
                if (PassManager.checkMiddlePassHash(middlePassHash)) {

                    // задавать не нужно ?
//                    DataManager.initCryptPass(middlePassHash, true);

                    callback.onApply();
                } else {
                    LogManager.log(R.string.log_wrong_saved_pass, Toast.LENGTH_LONG);
                    // спрашиваем пароль
                    PassManager.askPassword(context, null, callback);
                }
            } catch (DatabaseConfig.EmptyFieldException ex) {
                // если поля в INI-файле для проверки пустые
                LogManager.log(ex);
                //                if (DataManager.isExistsCryptedNodes()) {
                if (DataManager.isCrypted()) {
//                    final String hash = middlePassHash;
                    // спрашиваем "continue anyway?"
                    PassDialogs.showEmptyPassCheckingFieldDialog(context, ex.getFieldName(),
                            new Dialogs.IApplyCancelResult() {
                                @Override
                                public void onApply() {

//                                    DataManager.initCryptPass(hash, true);

                                    callback.onApply();
                                }
                                @Override
                                public void onCancel() {
                                    callback.onCancel();
                                }
                            });
                } else {
                    // если нет зашифрованных веток, но пароль сохранен
//                    DataManager.initCryptPass(middlePassHash, true);

                    callback.onApply();
                }
            }
//            } else {
//                // пароль не сохранен, вводим
//                askPassword(node, callback);
//            }
        } else {
            // спрашиваем или задаем пароль
            PassManager.askPassword(context, null, callback);
        }
    }
}
