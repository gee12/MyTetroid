package com.gee12.mytetroid.data;

import android.content.Context;
import android.widget.Toast;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.dialogs.AskDialogs;

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
            AskDialogs.showPINCodeDialog(context, false, new AskDialogs.IPinInputResult() {
                @Override
                public void onApply(String code) {
                    callback.onApply();
                    LogManager.log(R.string.log_pin_code_enter);
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
    public static void setupPINCode(Context context) {
        if (isRequestPINCode()) {
            // проверяем сохраненный пароль
            checkPass(context, () -> {
                // задаем новый ПИН-код
                AskDialogs.showPINCodeDialog(context, true, new AskDialogs.IPinInputResult() {
                    @Override
                    public void onApply(String code) {
                        SettingsManager.setPINCode(code);
                        LogManager.log(R.string.log_pin_code_setup);
                    }

                    @Override
                    public void onCancel() {
                        SettingsManager.setIsRequestPINCode(false);
                    }
                });
            });
        } else {
            // очищаем
            SettingsManager.setPINCode(null);
            LogManager.log(R.string.log_pin_code_clean);
        }
    }

    /**
     *
     * @param context
     * @param callback
     */
    public static void checkPass(Context context, Dialogs.IApplyResult callback) {
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
                    AskDialogs.showEmptyPassCheckingFieldDialog(context, ex.getFieldName(),
                            new Dialogs.IApplyCancelResult() {
                                @Override
                                public void onApply() {

//                                    DataManager.initCryptPass(hash, true);

                                    callback.onApply();
                                }
                                @Override
                                public void onCancel() {
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
