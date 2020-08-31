package com.gee12.mytetroid.data;

import android.content.Context;
import android.widget.Toast;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TaskStage;
import com.gee12.mytetroid.TetroidLog;
import com.gee12.mytetroid.crypt.Base64;
import com.gee12.mytetroid.crypt.CryptManager;
import com.gee12.mytetroid.crypt.Crypter;
import com.gee12.mytetroid.dialogs.PassDialogs;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.utils.Utils;

public class PassManager extends DataManager {

    /**
     * Асинхронная проверка имеется ли сохраненный пароль и его запрос при необходимости.
     *
     * TODO: добавить запрос на ввод ПИН-кода, если включено сохранение пароля
     *
     * @param node
     * @param callback Действие после проверки пароля
     */
    public static void checkStoragePass(Context context, TetroidNode node, Dialogs.IApplyCancelResult callback) {
        //if (SettingsManager.isSaveMiddlePassHashLocal()) {
        String middlePassHash;
        if ((middlePassHash = CryptManager.getMiddlePassHash()) != null) {
            // хэш пароля сохранен в оперативной памяти (вводили до этого и проверяли)
            initCryptPass(middlePassHash, true);
            callback.onApply();
        } else if ((middlePassHash = SettingsManager.getMiddlePassHash()) != null) {
            // хэш пароля сохранен "на диске", проверяем
            try {
                if (checkMiddlePassHash(middlePassHash)) {
                    initCryptPass(middlePassHash, true);
                    callback.onApply();
                } else {
                    LogManager.log(R.string.log_wrong_saved_pass, Toast.LENGTH_LONG);
                    // спрашиваем пароль
                    askPassword(context, node, callback);
                }
            } catch (DatabaseConfig.EmptyFieldException ex) {
                // если поля в INI-файле для проверки пустые
                LogManager.log(ex);
                //                if (DataManager.isExistsCryptedNodes()) {
                if (isCrypted()) {
                    final String hash = middlePassHash;
                    // спрашиваем "continue anyway?"
                    PassDialogs.showEmptyPassCheckingFieldDialog(context, ex.getFieldName(),
                            new Dialogs.IApplyCancelResult() {
                                @Override
                                public void onApply() {
                                    initCryptPass(hash, true);
                                    callback.onApply();
                                }
                                @Override
                                public void onCancel() {
                                }
                            });
                } else {
                    // если нет зашифрованных веток, но пароль сохранен
                    initCryptPass(middlePassHash, true);
                    callback.onApply();
                }
            }
//            } else {
//                // пароль не сохранен, вводим
//                askPassword(node, callback);
//            }
        } else {
            // спрашиваем или задаем пароль
            askPassword(context, node, callback);
        }
    }

    /**
     * Отображения запроса пароля от хранилища.
     * @param node
     */
    public static void askPassword(Context context, final TetroidNode node, Dialogs.IApplyCancelResult callback) {
        LogManager.log(R.string.log_show_pass_dialog);
        boolean isNewPass = !isCrypted();
        // выводим окно с запросом пароля в асинхронном режиме
        PassDialogs.showPassEnterDialog(context, node, isNewPass, new PassDialogs.IPassInputResult() {
            @Override
            public void applyPass(final String pass, TetroidNode node) {
                if (isNewPass) {
                    LogManager.log(R.string.log_start_pass_setup);
                    setupPass(pass);
                    callback.onApply();
                } else {
                    checkPass(context, pass, (res) -> {
                        if (res) {
                            initPass(pass);
                            callback.onApply();
                        } else {
                            // повторяем запрос
                            askPassword(context, node, callback);
                        }
                    }, R.string.log_pass_is_incorrect);
                }
            }

            @Override
            public void cancelPass() {
                callback.onCancel();
            }
        });
    }

    /**
     * Проверка введенного пароля с сохраненным проверочным хэшем.
     * @param pass
     * @return
     * @throws DatabaseConfig.EmptyFieldException
     */
    public static boolean checkPass(String pass) throws DatabaseConfig.EmptyFieldException {
        String salt = databaseConfig.getCryptCheckSalt();
        String checkHash = databaseConfig.getCryptCheckHash();
        return CryptManager.checkPass(pass, salt, checkHash);
    }

    /**
     * Проверка сохраненного хэша пароля с помощью сохраненных зашифрованных данных.
     * @param passHash
     * @return
     * @throws DatabaseConfig.EmptyFieldException
     */
    public static boolean checkMiddlePassHash(String passHash) throws DatabaseConfig.EmptyFieldException {
        String checkData = databaseConfig.getMiddleHashCheckData();
        return CryptManager.checkMiddlePassHash(passHash, checkData);
    }

    /**
     * Каркас проверки введенного пароля.
     * @param context
     * @param pass
     * @param callback
     * @param wrongPassRes
     */
    public static boolean checkPass(Context context, String pass, ICallback callback, int wrongPassRes) {
        try {
            if (checkPass(pass)) {
                callback.run(true);
            } else {
                LogManager.log(wrongPassRes, Toast.LENGTH_LONG);
                callback.run(false);
                return false;
            }
        } catch (DatabaseConfig.EmptyFieldException ex) {
            // если поля в INI-файле для проверки пустые
            LogManager.log(ex);
            // спрашиваем "continue anyway?"
            PassDialogs.showEmptyPassCheckingFieldDialog(context, ex.getFieldName(), new Dialogs.IApplyCancelResult() {
                @Override
                public void onApply() {
                    // TODO: тут спрашиваем нормально ли расшифровались данные
                    //  ...
                    if (callback != null) {
                        callback.run(true);
                    }
                }
                @Override
                public void onCancel() {
                }
            });
        }
        return true;
    }

    /**
     * Сохранение пароля в настройках и его установка для шифрования.
     * @param pass
     */
    public static void initPass(String pass) {
        String passHash = CryptManager.passToHash(pass);
        if (SettingsManager.isSaveMiddlePassHashLocal()) {
            // сохраняем хэш пароля
            SettingsManager.setMiddlePassHash(passHash);
            // записываем проверочную строку
            saveMiddlePassCheckData(passHash);
        } else {
            // сохраняем хэш пароля в оперативную память, может еще понадобится
            CryptManager.setMiddlePassHash(passHash);
        }
        // здесь, по идее, можно сохранять сразу passHash (с параметром isMiddleHash=true),
        // но сделал так
        DataManager.initCryptPass(pass, false);
    }

    /**
     * Установка пароля хранилища впервые.
     * @return
     */
    public static void setupPass(Context context) {
        LogManager.log(R.string.log_start_pass_setup);
        // вводим пароль
        PassDialogs.showPassEnterDialog(context, null, true, new PassDialogs.IPassInputResult() {
            @Override
            public void applyPass(String pass, TetroidNode node) {
                setupPass(pass);
            }
            @Override
            public void cancelPass() {
            }
        });
    }

    /**
     * Установка пароля хранилища впервые.
     * @param pass
     */
    public static void setupPass(String pass) {
        // сохраняем в database.ini
        if (savePassCheckData(pass)) {
            LogManager.log(R.string.log_pass_setted, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            initPass(pass);
        } else {
            LogManager.log(R.string.log_pass_set_error, LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    public static boolean changePass(String curPass, String newPass, ITaskProgress taskProgress) {
        // сначала устанавливаем текущий пароль
        taskProgress.nextStage(TetroidLog.Objs.CUR_PASS, TetroidLog.Opers.SET, TaskStage.Stages.START);
        initPass(curPass);
        // и расшифровываем хранилище
        if (!taskProgress.nextStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT, () ->
                DataManager.decryptStorage(true)))
            return false;
        // теперь устанавливаем новый пароль
        taskProgress.nextStage(TetroidLog.Objs.NEW_PASS, TetroidLog.Opers.SET, TaskStage.Stages.START);
        initPass(newPass);
        // и перешифровываем зашифрованные ветки
        if (!taskProgress.nextStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.REENCRYPT, () ->
                DataManager.reencryptStorage()))
            return false;
        // сохраняем mytetra.xml
        taskProgress.nextStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.SAVE, () ->
                DataManager.saveStorage());
        // сохраняем данные в database.ini
        taskProgress.nextStage(TetroidLog.Objs.NEW_PASS, TetroidLog.Opers.SAVE, TaskStage.Stages.START);
        savePassCheckData(newPass);
        return true;
    }

    /**
     * Сброс сохраненного хэша пароля и его проверочных данных.
     */
    public static void clearSavedPass() {
        SettingsManager.setMiddlePassHash(null);
        CryptManager.setMiddlePassHash(null);
        clearPassCheckData();
        clearMiddlePassCheckData();
    }

    /**
     * Сохранение проверочного хэша пароля и сопутствующих данных в database.ini.
     * @param newPass
     * @return
     */
    public static boolean savePassCheckData(String newPass) {
        byte[] salt = Utils.createRandomBytes(32);
        byte[] passHash = null;
        try {
            passHash = Crypter.calculatePBKDF2Hash(newPass, salt);
        } catch (Exception ex) {
            LogManager.log(ex);
            return false;
        }
        return databaseConfig.savePass(Base64.encodeToString(passHash, false),
                Base64.encodeToString(salt, false), true);
    }

    /**
     * Сохранение проверочной строки промежуточного хэша пароля в database.ini.
     * @param passHash
     * @return
     */
    public static boolean saveMiddlePassCheckData(String passHash) {
        String checkData = Crypter.createMiddlePassHashCheckData(passHash);
        return databaseConfig.saveCheckData(checkData);
    }

    /**
     * Очистка сохраненного проверочнго хэша пароля.
     * @return
     */
    public static boolean clearPassCheckData() {
        SettingsManager.setMiddlePassHash(null);
        return databaseConfig.savePass(null, null, false);
    }

    /**
     * Очистка сохраненной проверочной строки промежуточного хэша пароля.
     * @return
     */
    public static boolean clearMiddlePassCheckData() {
        return databaseConfig.saveCheckData(null);
    }

}
