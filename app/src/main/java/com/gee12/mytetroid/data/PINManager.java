package com.gee12.mytetroid.data;

import android.content.Context;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.dialogs.AskDialogs;

public class PINManager {

    /**
     * Установка/очистка ПИН-кода
     */
    public static void setupPINCode(Context context) {
        if (SettingsManager.isRequestPINCode()) {

            // проверяем сохраненного пароля
            

            // задаем новый ПИН-код
            AskDialogs.showPINCodeDialog(context, true, false, null, new AskDialogs.IPinInputResult() {
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
        } else {
            // очищаем
            SettingsManager.setPINCode(null);
            LogManager.log(R.string.log_pin_code_clean);
        }
    }

}
