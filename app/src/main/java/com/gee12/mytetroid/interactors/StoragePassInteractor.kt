package com.gee12.mytetroid.interactors

/**
 * Перенос кода из PassManager: сюда и в PassViewModel (CryptViewModel)
 */
class StoragePassInteractor {

//    /**
//     * Асинхронная проверка имеется ли сохраненный пароль и его запрос при необходимости.
//     * @param node
//     * @param callback Действие после проверки пароля
//     */
//    fun checkStoragePass(context: Context?, node: TetroidNode?, callback: IApplyCancelResult) {
//        //if (SettingsManager.isSaveMiddlePassHashLocal()) {
//        var middlePassHash: String?
//        if (DataManager.Instance.mCrypter.middlePassHash.also { middlePassHash = it } != null) {
//            // хэш пароля сохранен в оперативной памяти (вводили до этого и проверяли)
//            DataManager.Instance.initCryptPass(middlePassHash, true)
//            callback.onApply()
//        } else if (SettingsManager.getMiddlePassHash(context).also { middlePassHash = it } != null) {
//            // хэш пароля сохранен "на диске", проверяем
//            try {
//                if (checkMiddlePassHash(middlePassHash)) {
//                    DataManager.Instance.initCryptPass(middlePassHash, true)
//                    //                    callback.onApply();
//                    PINManager.askPINCode(context, true) { callback.onApply() }
//                } else {
//                    LogManager.log(context, R.string.log_wrong_saved_pass, Toast.LENGTH_LONG)
//                    // спрашиваем пароль
//                    askPassword(context, node, callback)
//                }
//            } catch (ex: EmptyFieldException) {
//                // если поля в INI-файле для проверки пустые
//                LogManager.log(context, ex)
//                //                if (DataManager.isExistsCryptedNodes()) {
//                if (DataManager.isCrypted(context)) {
//                    val hash = middlePassHash
//                    // спрашиваем "continue anyway?"
//                    PassDialogs.showEmptyPassCheckingFieldDialog(context, ex.fieldName,
//                        object : IApplyCancelResult {
//                            override fun onApply() {
//                                DataManager.Instance.initCryptPass(hash, true)
//                                //                                    callback.onApply();
//                                PINManager.askPINCode(context, true) { callback.onApply() }
//                            }
//
//                            override fun onCancel() {}
//                        })
//                } else {
//                    // если нет зашифрованных веток, но пароль сохранен
//                    DataManager.Instance.initCryptPass(middlePassHash, true)
//                    //                    callback.onApply();
//                    PINManager.askPINCode(context, true) { callback.onApply() }
//                }
//            }
//            //            } else {
////                // пароль не сохранен, вводим
////                askPassword(node, callback);
////            }
//        } else {
//            // спрашиваем или задаем пароль
//            askPassword(context, node, callback)
//        }
//    }
//
////    private static void checkPINAndInitPass(Context context, Dialogs.IApplyCancelResult callback) {
////        PINManager.askPINCode(context, true, () -> {
////            callback.onApply();
////        });
////    }
//
//    //    private static void checkPINAndInitPass(Context context, Dialogs.IApplyCancelResult callback) {
//    //        PINManager.askPINCode(context, true, () -> {
//    //            callback.onApply();
//    //        });
//    //    }
//    /**
//     * Отображения запроса пароля от хранилища.
//     * @param node
//     */
//    fun askPassword(context: Context?, node: TetroidNode?, callback: IApplyCancelResult) {
//        LogManager.log(context, R.string.log_show_pass_dialog)
//        val isNewPass = !DataManager.isCrypted(context)
//        // выводим окно с запросом пароля в асинхронном режиме
//        PassDialogs.showPassEnterDialog(context, node, isNewPass, object : IPassInputResult {
//            override fun applyPass(pass: String, node: TetroidNode) {
//                if (isNewPass) {
//                    LogManager.log(context, R.string.log_start_pass_setup)
//                    setupPass(context, pass)
//                    //                    callback.onApply();
//                    PINManager.askPINCode(context, node != null) { callback.onApply() }
//                } else {
//                    checkPass(context, pass, { res: Boolean ->
//                        if (res) {
//                            initPass(context, pass)
//                            //                            callback.onApply();
//                            PINManager.askPINCode(context, node != null) { callback.onApply() }
//                        } else {
//                            // повторяем запрос
//                            askPassword(context, node, callback)
//                        }
//                    }, R.string.log_pass_is_incorrect)
//                }
//            }
//
//            override fun cancelPass() {
//                callback.onCancel()
//            }
//        })
//    }
//
//    /**
//     * Проверка введенного пароля с сохраненным проверочным хэшем.
//     * @param pass
//     * @return
//     * @throws DatabaseConfig.EmptyFieldException
//     */
//    @Throws(EmptyFieldException::class)
//    fun checkPass(pass: String?): Boolean {
//        val salt = DataManager.Instance.mDatabaseConfig.cryptCheckSalt
//        val checkHash = DataManager.Instance.mDatabaseConfig.cryptCheckHash
//        return DataManager.Instance.mCrypter.checkPass(pass, salt, checkHash)
//    }
//
//    /**
//     * Проверка сохраненного хэша пароля с помощью сохраненных зашифрованных данных.
//     * @param passHash
//     * @return
//     * @throws DatabaseConfig.EmptyFieldException
//     */
//    @Throws(EmptyFieldException::class)
//    fun checkMiddlePassHash(passHash: String?): Boolean {
//        val checkData = DataManager.Instance.mDatabaseConfig.middleHashCheckData
//        return DataManager.Instance.mCrypter.checkMiddlePassHash(passHash, checkData)
//    }
//
//    /**
//     * Каркас проверки введенного пароля.
//     * @param context
//     * @param pass
//     * @param callback
//     * @param wrongPassRes
//     */
//    fun checkPass(context: Context?, pass: String?, callback: ICallback?, wrongPassRes: Int): Boolean {
//        try {
//            if (checkPass(pass)) {
//                callback!!.run(true)
//            } else {
//                LogManager.log(context, wrongPassRes, Toast.LENGTH_LONG)
//                callback!!.run(false)
//                return false
//            }
//        } catch (ex: EmptyFieldException) {
//            // если поля в INI-файле для проверки пустые
//            LogManager.log(context, ex)
//            // спрашиваем "continue anyway?"
//            PassDialogs.showEmptyPassCheckingFieldDialog(context, ex.fieldName, object : IApplyCancelResult {
//                override fun onApply() {
//                    // TODO: тут спрашиваем нормально ли расшифровались данные
//                    //  ...
//                    callback?.run(true)
//                }
//
//                override fun onCancel() {}
//            })
//        }
//        return true
//    }
//
//    /**
//     * Сохранение пароля в настройках и его установка для шифрования.
//     * @param pass
//     */
//    fun initPass(context: Context?, pass: String?) {
//        val passHash = DataManager.Instance.mCrypter.passToHash(pass)
//        if (SettingsManager.isSaveMiddlePassHashLocal(context)) {
//            // сохраняем хэш пароля
//            SettingsManager.setMiddlePassHash(context, passHash)
//            // записываем проверочную строку
//            saveMiddlePassCheckData(passHash)
//        } else {
//            // сохраняем хэш пароля в оперативную память, может еще понадобится
//            DataManager.Instance.mCrypter.middlePassHash = passHash
//        }
//        // здесь, по идее, можно сохранять сразу passHash (с параметром isMiddleHash=true),
//        // но сделал так
//        DataManager.Instance.initCryptPass(pass, false)
//    }
//
//    /**
//     * Установка пароля хранилища впервые.
//     * @return
//     */
//    fun setupPass(context: Context?) {
//        LogManager.log(context, R.string.log_start_pass_setup)
//        // вводим пароль
//        PassDialogs.showPassEnterDialog(context, null, true, object : IPassInputResult {
//            override fun applyPass(pass: String, node: TetroidNode) {
//                setupPass(context, pass)
//            }
//
//            override fun cancelPass() {}
//        })
//    }
//
//    /**
//     * Установка пароля хранилища впервые.
//     * @param pass
//     */
//    fun setupPass(context: Context?, pass: String?) {
//        // сохраняем в database.ini
//        if (savePassCheckData(context, pass)) {
//            LogManager.log(context, R.string.log_pass_setted, ILogger.Types.INFO, Toast.LENGTH_SHORT)
//            initPass(context, pass)
//        } else {
//            LogManager.log(context, R.string.log_pass_set_error, ILogger.Types.ERROR, Toast.LENGTH_LONG)
//        }
//    }
//
//    fun changePass(context: Context?, curPass: String?, newPass: String?, taskProgress: ITaskProgress): Boolean {
//        // сначала устанавливаем текущий пароль
//        taskProgress.nextStage(TetroidLog.Objs.CUR_PASS, TetroidLog.Opers.SET, TaskStage.Stages.START)
//        initPass(context, curPass)
//        // и расшифровываем хранилище
//        if (!taskProgress.nextStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT) { DataManager.Instance.decryptStorage(context, true) }) return false
//        // теперь устанавливаем новый пароль
//        taskProgress.nextStage(TetroidLog.Objs.NEW_PASS, TetroidLog.Opers.SET, TaskStage.Stages.START)
//        initPass(context, newPass)
//        // и перешифровываем зашифрованные ветки
//        if (!taskProgress.nextStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.REENCRYPT) { DataManager.Instance.reencryptStorage(context) }) return false
//        // сохраняем mytetra.xml
//        taskProgress.nextStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.SAVE) { DataManager.Instance.saveStorage(context) }
//        // сохраняем данные в database.ini
//        taskProgress.nextStage(TetroidLog.Objs.NEW_PASS, TetroidLog.Opers.SAVE, TaskStage.Stages.START)
//        savePassCheckData(context, newPass)
//        return true
//    }
//
//    /**
//     * Сброс сохраненного хэша пароля и его проверочных данных.
//     */
//    fun clearSavedPass(context: Context?) {
//        SettingsManager.setMiddlePassHash(context, null)
//        DataManager.Instance.mCrypter.middlePassHash = null
//        clearPassCheckData(context)
//        clearMiddlePassCheckData()
//    }
//
//    /**
//     * Сохранение проверочного хэша пароля и сопутствующих данных в database.ini.
//     * @param newPass
//     * @return
//     */
//    fun savePassCheckData(context: Context?, newPass: String?): Boolean {
//        val salt = Utils.createRandomBytes(32)
//        var passHash: ByteArray? = null
//        passHash = try {
//            Crypter.calculatePBKDF2Hash(newPass, salt)
//        } catch (ex: Exception) {
//            LogManager.log(context, ex)
//            return false
//        }
//        return DataManager.Instance.mDatabaseConfig.savePass(
//            Base64.encodeToString(passHash, false),
//            Base64.encodeToString(salt, false), true
//        )
//    }
//
//    /**
//     * Сохранение проверочной строки промежуточного хэша пароля в database.ini.
//     * @param passHash
//     * @return
//     */
//    fun saveMiddlePassCheckData(passHash: String?): Boolean {
//        val checkData = DataManager.Instance.mCrypter.createMiddlePassHashCheckData(passHash)
//        return DataManager.Instance.mDatabaseConfig.saveCheckData(checkData)
//    }
//
//    /**
//     * Очистка сохраненного проверочнго хэша пароля.
//     * @return
//     */
//    fun clearPassCheckData(context: Context?): Boolean {
//        SettingsManager.setMiddlePassHash(context, null)
//        return DataManager.Instance.mDatabaseConfig.savePass(null, null, false)
//    }
//
//    /**
//     * Очистка сохраненной проверочной строки промежуточного хэша пароля.
//     * @return
//     */
//    fun clearMiddlePassCheckData(): Boolean {
//        return DataManager.Instance.mDatabaseConfig.saveCheckData(null)
//    }
}