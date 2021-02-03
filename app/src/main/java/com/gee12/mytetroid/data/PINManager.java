package com.gee12.mytetroid.data;

import android.content.Context;
import android.widget.Toast;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.ini.DatabaseConfig;
import com.gee12.mytetroid.dialogs.PassDialogs;
import com.gee12.mytetroid.logs.LogManager;

import static com.gee12.mytetroid.data.DataManager.Instance;

public class PINManager {

    /**
     * Проверка использования ПИН-кода с учетом версии приложения.
     * @return
     */
    public static boolean isRequestPINCode(Context context) {
        return App.isFullVersion() && SettingsManager.isRequestPINCode(context)
                && StorageManager.isPINNeedToEnter();
    }

    /**
     * Запрос ПИН-кода, если установлена опция.
     * К этому моменту факт того, что хэш пароля сохранен локально, должен быть уже проверен.
     * @param context
     * @param specialFlag Дополнительный признак, указывающий на то, нужно ли спрашивать ПИН-код
     *                    конкретно в данный момент.
     * @param callback Обработчик обратного вызова.
     */
    public static void askPINCode(Context context, boolean specialFlag, Dialogs.IApplyResult callback) {
        if (isRequestPINCode(context) && specialFlag) {
            // выводим запрос ввода ПИН-кода
            PassDialogs.showPINCodeDialog(context, SettingsManager.getPINCodeLength(context), false, new PassDialogs.IPinInputResult() {
                @Override
                public boolean onApply(String pin) {
                    // зашифровываем введеный пароль перед сравнением
                    String pinHash = Instance.mCrypter.passToHash(pin);
                    boolean res = pinHash.equals(SettingsManager.getPINCodeHash(context));
                    if (res) {
                        callback.onApply();
                        // сбрасываем признак
                        StorageManager.resetIsPINNeedToEnter();
                        LogManager.log(context, R.string.log_pin_code_enter);
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
        if (!isRequestPINCode(context)) {
            // проверяем сохраненный пароль
            checkPass(context, new Dialogs.IApplyCancelResult() {
                @Override
                public void onApply() {
                    // задаем длину ПИН-кода
                    PassDialogs.showPinCodeLengthDialog(context, SettingsManager.getPINCodeLength(context),
                            new PassDialogs.IPinLengthInputResult() {
                        @Override
                        public void onApply(int length) {
                            SettingsManager.setPINCodeLength(context, length);
                            LogManager.log(context, context.getString(R.string.log_pin_code_length_setup) + length);
                            // задаем новый ПИН-код
                            PassDialogs.showPINCodeDialog(context, length, true, new PassDialogs.IPinInputResult() {
                                @Override
                                public boolean onApply(String pin) {
                                    // зашифровываем пароль перед установкой
                                    String pinHash = Instance.mCrypter.passToHash(pin);
                                    SettingsManager.setPINCodeHash(context, pinHash);
                                    callback.run(true);
                                    // устанавливаем признак
                                    StorageManager.setIsPINNeedToEnter();
                                    LogManager.log(context, R.string.log_pin_code_setup, Toast.LENGTH_SHORT);
                                    return true;
                                }
                                @Override
                                public void onCancel() {
                                }
                            });
                        }
                        @Override
                        public void onCancel() {
                        }
                    });
                }
                @Override
                public void onCancel() {
                }
            });
        } else {
            // сбрасываем имеющийся ПИН-код, предварительнго его запросив
            PassDialogs.showPINCodeDialog(context, SettingsManager.getPINCodeLength(context), false,
                    new PassDialogs.IPinInputResult() {
                @Override
                public boolean onApply(String pin) {
                    // зашифровываем введеный пароль перед сравнением
                    String pinHash = Instance.mCrypter.passToHash(pin);
                    boolean res = pinHash.equals(SettingsManager.getPINCodeHash(context));
                    if (res) {
                        // очищаем
                        SettingsManager.setPINCodeHash(context, null);
                        callback.run(false);
                        LogManager.log(context, R.string.log_pin_code_clean, Toast.LENGTH_SHORT);
                    }
                    return res;
                }
                @Override
                public void onCancel() {
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
        if ((middlePassHash = SettingsManager.getMiddlePassHash(context)) != null) {
            // хэш пароля сохранен "на диске", проверяем
            try {
                if (PassManager.checkMiddlePassHash(middlePassHash)) {

                    // задавать не нужно ?
//                    DataManager.initCryptPass(middlePassHash, true);

                    callback.onApply();
                } else {
                    LogManager.log(context, R.string.log_wrong_saved_pass, Toast.LENGTH_LONG);
                    // спрашиваем пароль
                    PassManager.askPassword(context, null, callback);
                }
            } catch (DatabaseConfig.EmptyFieldException ex) {
                // если поля в INI-файле для проверки пустые
                LogManager.log(context, ex);
                //                if (DataManager.isExistsCryptedNodes()) {
                if (DataManager.isCrypted(context)) {
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
