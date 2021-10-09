package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Context
import android.widget.Toast
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.ICallback
import com.gee12.mytetroid.data.ITaskProgress
import com.gee12.mytetroid.data.SettingsManager
import com.gee12.mytetroid.data.TetroidXml
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.interactors.PasswordInteractor
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.logs.TaskStage
import com.gee12.mytetroid.repo.StoragesRepo
import kotlinx.coroutines.launch

open class StorageEncryptionViewModel(
    app: Application,
    storagesRepo: StoragesRepo,
    xmlLoader: TetroidXml
) : StorageSettingsViewModel(app, storagesRepo, xmlLoader) {

    val databaseConfig = DatabaseConfig(logger)

    val passInteractor = PasswordInteractor(logger, storage, databaseConfig, cryptInteractor, storageInteractor, nodesInteractor)
//    val pinInteractor = PinInteractor(passInteractor, cryptInteractor, nodesInteractor)

    var isPinNeedEnter = false


    //region Password

    /**
     * Асинхронная проверка имеется ли сохраненный пароль и его запрос при необходимости.
     * @param node
     */
    fun checkStoragePass(callback: CallbackParam//node: TetroidNode?, event: Any
        /*, callback: Dialogs.IApplyCancelResult*/) {
//        val callbackParam = CallbackParam(event, node)

        //if (SettingsManager.isSaveMiddlePassHashLocal()) {
        var middlePassHash: String?
        when {
            cryptInteractor.crypter.middlePassHash.also { middlePassHash = it } != null -> {
                // хэш пароля сохранен в оперативной памяти (вводили до этого и проверяли)
                cryptInteractor.initCryptPass(middlePassHash!!, true)
                // callback.onApply()
                postEvent(callback)
            }
            storage?.middlePassHash.also { middlePassHash = it } != null -> {
                // хэш пароля сохранен "на диске", проверяем
                try {
                    if (passInteractor.checkMiddlePassHash(middlePassHash)) {
                        cryptInteractor.initCryptPass(middlePassHash!!, true)
                        // callback.onApply();
//                        askPinCode(true) {
////                            callback.onApply()
//                            doAction(callback)
//                        }
                        askPinCode(true, callback)
                    } else {
                        logger.log(R.string.log_wrong_saved_pass, true)
                        // спрашиваем пароль
                        postEvent(callback)
                    }
                } catch (ex: DatabaseConfig.EmptyFieldException) {
                    // если поля в INI-файле для проверки пустые
                    logger.logError(ex)
                    // if (DataManager.isExistsCryptedNodes()) {
                    if (isCrypted()) {
//                        val hash = middlePassHash
                        // спрашиваем "continue anyway?"
                        /*PassDialogs.showEmptyPassCheckingFieldDialog(context, ex.fieldName,
                            object : Dialogs.IApplyCancelResult {
                                override fun onApply() {
                                    cryptInteractor.initCryptPass(hash!!, true)
                                    // callback.onApply();
//                                    askPinCode(true) {
////                                        callback.onApply()
//                                        doAction(callback)
//                                    }
                                    askPinCode(true, callback)
                                }

                                override fun onCancel() {}
                            })*/
//                        confirmEmptyPassCheckingFieldDialog(
//                            EmptyPassCheckingFieldCallbackParam(middlePassHash!!, callback)
//                        )
                        postStorageEvent(Constants.StorageEvents.AskForEmptyPassCheckingField,
                            EmptyPassCheckingFieldCallbackParam(ex.fieldName, middlePassHash!!, callback)
                        )
                    } else {
                        // если нет зашифрованных веток, но пароль сохранен
                        cryptInteractor.initCryptPass(middlePassHash!!, true)
                        // callback.onApply();
//                        askPinCode(true) {
////                            callback.onApply()
//                            doAction(callback)
//                        }
                        askPinCode(true, callback)
                    }
                }
//            } else {
//                // пароль не сохранен, вводим
//                askPassword(node, callback);
//            }
            }
            else -> {
                // спрашиваем или задаем пароль
                askPassword(/*node,*/ callback)
            }
        }
    }

    fun confirmEmptyPassCheckingFieldDialog(callback: EmptyPassCheckingFieldCallbackParam) {
        cryptInteractor.initCryptPass(callback.passHash, true)
        // callback.onApply();
//                askPinCode(true) {
////                callback.onApply()
//                    doAction(callback)
//                }
        askPinCode(true, callback)
    }

//    private static void checkPINAndInitPass(Context context, Dialogs.IApplyCancelResult callback) {
//        PINManager.askPINCode(context, true, () -> {
//            callback.onApply();
//        });
//    }

    /**
     * Отображения запроса пароля от хранилища.
     * @param node
     */
    fun askPassword(/*node: TetroidNode?,*/ callback: CallbackParam /*callback: Dialogs.IApplyCancelResult*/) {
        logger.log(R.string.log_show_pass_dialog)
//        val isNewPass = !isCrypted()
        // выводим окно с запросом пароля в асинхронном режиме
        postStorageEvent(Constants.StorageEvents.AskPassword, callback)
//        PassDialogs.showPassEnterDialog(context, node, isNewPass, object : PassDialogs.IPassInputResult {
//            override fun applyPass(pass: String, node: TetroidNode?) {
//                if (isNewPass) {
//                    logger.log(R.string.log_start_pass_setup)
//                    passInteractor.setupPass(getContext(), pass)
//                    // callback.onApply();
//                    askPINCode(node != null) {
////                        callback.onApply()
//                        doAction(callback)
//                    }
//                } else {
//                    passInteractor.checkPass(getContext(), pass, { res: Boolean ->
//                        if (res) {
//                            passInteractor.initPass(getContext(), pass)
//                            //                            callback.onApply();
//                            askPINCode(node != null) {
////                                callback.onApply()
//                                doAction(callback)
//                            }
//                        } else {
//                            // повторяем запрос
//                            askPassword(node, callback)
//                        }
//                    }, R.string.log_pass_is_incorrect)
//                }
//            }
//
//            override fun cancelPass() {
////                callback.onCancel()
//            }
//        })
    }

    fun onPasswordAsked(pass: String, isNewPass: Boolean, callback: CallbackParam) {
        if (isNewPass) {
            launch {
                logger.log(R.string.log_start_pass_setup)
                setupPass(pass)
                // callback.onApply();
//            askPINCode(node != null) {
////                        callback.onApply()
//                doAction(callback)
//            }
                askPinCode(true, callback)
            }
        } else {
            checkPass(pass, { res: Boolean ->
                launch {
                    if (res) {
                        initPass(pass)
//                            callback.onApply();
//                    askPINCode(node != null) {
////                                callback.onApply()
//                        doAction(callback)
//                    }
                        askPinCode(true, callback)
                    } else {
                        // повторяем запрос
                        askPassword(callback)
                    }
                }
            }, R.string.log_pass_is_incorrect)
        }
    }

    fun setupPass(pass: String) {
        launch {
            isBusy = true
            passInteractor.setupPass(getContext(), pass)
            updateStorage(storage!!)
            isBusy = false
        }
    }

    suspend fun initPass(pass: String) {
        passInteractor.initPass(getContext(), pass)
        updateStorage(storage!!)
    }

    /**
     * Каркас проверки введенного пароля.
     * @param context
     * @param pass
     * @param callback
     * @param wrongPassRes
     */
    fun checkPass(pass: String?, callback: ICallback?, wrongPassRes: Int): Boolean {
        try {
            if (passInteractor.checkPass(pass)) {
                callback!!.run(true)
            } else {
                logger.logError(wrongPassRes, true)
                callback!!.run(false)
                return false
            }
        } catch (ex: DatabaseConfig.EmptyFieldException) {
            // если поля в INI-файле для проверки пустые
            logger.logError(ex)
            // спрашиваем "continue anyway?"
//            PassDialogs.showEmptyPassCheckingFieldDialog(context, ex.fieldName, object : Dialogs.IApplyCancelResult {
//                override fun onApply() {
//                    // TODO: тут спрашиваем нормально ли расшифровались данные
//                    //  ...
//                    callback?.run(true)
//                }
//
//                override fun onCancel() {}
//            })
            postStorageEvent(Constants.StorageEvents.AskForEmptyPassCheckingField,
                // TODO: тут спрашиваем нормально ли расшифровались данные
                //  ...
                callback!!.run(true)
            )
        }
        return true
    }

    // FIXME: метод почему-то нигде не используется
//    fun checkPassword(pass: String, callback: CallbackParam) {
//        if (!isCrypted()) {
//            // хранилище не зашифровано, значит задаем новый пароль
//            logger.log(R.string.log_start_pass_setup)
//            passInteractor.setupPass(getContext(), pass)
//            // callback.onApply();
////            askPinCode(node != null) {
//////                        callback.onApply()
////                doAction(callback)
////            }
//            askPinCode(callback.data != null, callback)
//        } else {
//            // проверяем пароль с имеющимся
//            passInteractor.checkPass(getContext(), pass, { res: Boolean ->
//                if (res) {
//                    passInteractor.initPass(getContext(), pass)
//                    // callback.onApply();
////                    askPinCode(node != null) {
//////                                callback.onApply()
////                        doAction(callback)
////                    }
//                    askPinCode(callback.data != null, callback)
//                } else {
//                    // повторяем запрос
//                    askPassword(/*node,*/ callback)
//                }
//            }, R.string.log_pass_is_incorrect)
//        }
//    }

    fun startChangePass(curPass: String, newPass: String) {
        postViewEvent(Constants.ViewEvents.ShowProgressText, getString(R.string.task_pass_changing))
        launch {
            isBusy = true
            val res = changePass(getContext(), curPass, newPass, taskProgressHandler)
            isBusy = false
            postViewEvent(Constants.ViewEvents.ShowProgress, false)
            if (res) {
                logger.log(R.string.log_pass_changed, true)
            } else {
                logger.logError(R.string.log_pass_change_error, true)
                postViewEvent(Constants.ViewEvents.ShowMoreInLogs)
            }
        }
    }

    suspend fun changePass(context: Context, curPass: String, newPass: String, taskProgress: ITaskProgress): Boolean {
        // сначала устанавливаем текущий пароль
        taskProgress.nextStage(LogObj.CUR_PASS, LogOper.SET, TaskStage.Stages.START)
        initPass(curPass)

        // и расшифровываем хранилище
        if (!taskProgress.nextStage(LogObj.STORAGE, LogOper.DECRYPT) {
                    cryptInteractor.decryptStorage(context, true)
            }) return false

        // теперь устанавливаем новый пароль
        taskProgress.nextStage(LogObj.NEW_PASS, LogOper.SET, TaskStage.Stages.START)
        initPass(newPass)

        // и перешифровываем зашифрованные ветки
        if (!taskProgress.nextStage(LogObj.STORAGE, LogOper.REENCRYPT) {
                cryptInteractor.reencryptStorage(context)
            }) return false

        // сохраняем mytetra.xml
        taskProgress.nextStage(LogObj.STORAGE, LogOper.SAVE) {
            storageInteractor.saveStorage(context)
        }

        // сохраняем данные в database.ini
        taskProgress.nextStage(LogObj.NEW_PASS, LogOper.SAVE, TaskStage.Stages.START)
        passInteractor.savePassCheckData(context, newPass)

        return true
    }

//    /**
//     * TODO: Переписать на корутины.
//     * Задание (параллельный поток), в котором выполняется перешифровка хранилища.
//     */
//    inner class ChangePassTask : TetroidTask<String?, String?, Boolean?>(activity) {
//        override fun onPreExecute() {
//            settingsActivity!!.setProgressVisibility(true, getString(R.string.task_pass_changing))
//        }
//
//        override fun doInBackground(vararg values: String?): Boolean {
//            val curPass = values[0]
//            val newPass = values[1]
//            return viewModel.changePass(context, curPass, newPass, object : ITaskProgress {
//                override fun nextStage(obj: LogObj, oper: LogOper, stage: TaskStage.Stages) {
//                    setStage(obj, oper, stage)
//                }
//
//                override fun nextStage(obj: LogObj, oper: LogOper, stageExecutor: TaskStage.ITaskStageExecutor): Boolean {
//                    setStage(obj, oper, TaskStage.Stages.START)
//                    return if (stageExecutor.execute()) {
//                        setStage(obj, oper, TaskStage.Stages.SUCCESS)
//                        true
//                    } else {
//                        setStage(obj, oper, TaskStage.Stages.FAILED)
//                        false
//                    }
//                }
//            })
//        }
//
//        private fun setStage(obj: LogObj, oper: LogOper, stage: TaskStage.Stages) {
//            val taskStage = TaskStage(Constants.TetroidView.Settings, obj, oper, stage)
//            val mes = TetroidLog.logTaskStage(mContext, taskStage)
//            publishProgress(mes)
//        }
//
//        override fun onProgressUpdate(vararg values: String?) {
//            val mes = values[0]
//            settingsActivity!!.setProgressVisibility(true, mes)
//        }
//
//        override fun onPostExecute(res: Boolean?) {
//            settingsActivity!!.setProgressVisibility(false, null)
//            if (res == true) {
//                LogManager.log(mContext, R.string.log_pass_changed, ILogger.Types.INFO, Toast.LENGTH_SHORT)
//            } else {
//                LogManager.log(mContext, R.string.log_pass_change_error, ILogger.Types.INFO, Toast.LENGTH_SHORT)
//                showSnackMoreInLogs()
//            }
//        }
//    }

//    /**
//     * Установка пароля хранилища впервые.
//     * @return
//     */
//    fun setupPass(context: Context) {
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

    //endregion Password

    //region Pin

    /**
     * Проверка использования ПИН-кода с учетом версии приложения.
     * @return
     */
    fun isRequestPINCode(): Boolean {
        return (App.isFullVersion()
                && SettingsManager.isRequestPINCode(getContext())
                && isPinNeedEnter)
    }

    /**
     * Запрос ПИН-кода, если установлена опция.
     * К этому моменту факт того, что хэш пароля сохранен локально, должен быть уже проверен.
     * @param specialFlag Дополнительный признак, указывающий на то, нужно ли спрашивать ПИН-код
     * конкретно в данный момент.
     * @param callback Обработчик обратного вызова.
     */
    fun askPinCode(specialFlag: Boolean, callback: CallbackParam /*callback: Dialogs.IApplyResult*/) {
        if (isRequestPINCode() && specialFlag) {
            // выводим запрос ввода ПИН-кода
            postStorageEvent(Constants.StorageEvents.AskPinCode, callback)
           /* PassDialogs.showPINCodeDialog(context, SettingsManager.getPINCodeLength(context), false, object : PassDialogs.IPinInputResult {
                override fun onApply(pin: String): Boolean {
//                    // зашифровываем введеный пароль перед сравнением
//                    val pinHash: String = cryptInteractor.crypter.passToHash(pin)
//                    val res = pinHash == SettingsManager.getPINCodeHash(context)
//                    if (res) {
//                        callback.onApply()
//                        // сбрасываем признак
//                        StorageManager.resetIsPINNeedToEnter()
//                        LogManager.log(context, R.string.log_pin_code_enter)
//                    }
//                    return res
                    return checkPinCode(pin)
                }

                override fun onCancel() {}
            })*/
        } else {
//            callback.onApply()
            postEvent(callback)
        }
    }

    fun startCheckPinCode(pin: String): Boolean {
        // зашифровываем введеный пароль перед сравнением
        val res = checkPinCode(pin)
        if (res) {
//            callback.onApply()
            // сбрасываем признак
//            StorageManager.resetIsPINNeedToEnter()
            isPinNeedEnter = false
            logger.log(R.string.log_pin_code_enter)
        }
        postStorageEvent(Constants.StorageEvents.PinChecked, res)
        return res
    }

    /**
     * Установка/очистка ПИН-кода.
     * Вызывается при установке/снятии опции.
     * При установке сначала проверяется факт того, что хэш пароля сохранен локально.
     */
    fun startSetupDropPinCode(callback: CallbackParam) {
        if (!isRequestPINCode()) {
            checkStoragePass(CallbackParam(Constants.StorageEvents.SetupPinCode, callback))
        } else {
            checkStoragePass(CallbackParam(Constants.StorageEvents.DropPinCode, callback))
        }
    }

    fun setupPinCodeLength(length: Int) {
        SettingsManager.setPINCodeLength(getContext(), length)
        logger.log(getString(R.string.log_pin_code_length_setup) + length)
    }

    fun setupPinCode(pin: String) {
        // зашифровываем пароль перед установкой
        val pinHash: String = cryptInteractor.crypter.passToHash(pin)
        SettingsManager.setPINCodeHash(getContext(), pinHash)
        // устанавливаем признак
        isPinNeedEnter = true
        logger.log(R.string.log_pin_code_setup, true)
    }

    fun checkAndDropPinCode(pin: String): Boolean {
        // зашифровываем введеный пароль перед сравнением
        val res = checkPinCode(pin)
        if (res) {
            // очищаем
            dropPinCode()
        }
        return res
    }

    fun checkPinCode(pin: String): Boolean {
        val pinHash = cryptInteractor.crypter.passToHash(pin)
        return (pinHash == SettingsManager.getPINCodeHash(getContext()))
    }

    protected fun dropPinCode() {
        SettingsManager.setPINCodeHash(getContext(), null)
//        callback.run(false)
        logger.log(R.string.log_pin_code_clean, true)
    }

    // FIXME: из PinManager: по сути, дубликат метода из PassManager
//    /**
//     *
//     * @param context
//     * @param callback
//     */
//    fun checkPass(context: Context, callback: Dialogs.IApplyCancelResult) {
//        var middlePassHash: String?
//        if (SettingsManager.getMiddlePassHash(context).also { middlePassHash = it } != null) {
//            // хэш пароля сохранен "на диске", проверяем
//            try {
//                if (passInteractor.checkMiddlePassHash(middlePassHash)) {
//
//                    // задавать не нужно ?
////                    DataManager.initCryptPass(middlePassHash, true);
//                    callback.onApply()
//                } else {
//                    LogManager.log(context, R.string.log_wrong_saved_pass, Toast.LENGTH_LONG)
//                    // спрашиваем пароль
//                    askPassword(callback)
//                }
//            } catch (ex: DatabaseConfig.EmptyFieldException) {
//                // если поля в INI-файле для проверки пустые
//                LogManager.log(context, ex)
////                if (DataManager.isExistsCryptedNodes()) {
//                if (isCrypted()) {
////                    final String hash = middlePassHash;
//                    // спрашиваем "continue anyway?"
//                    PassDialogs.showEmptyPassCheckingFieldDialog(context, ex.fieldName,
//                        object : Dialogs.IApplyCancelResult {
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
//            askPassword(callback)
//        }
//    }

    //endregion Pin

    private val taskProgressHandler = object : ITaskProgress {
        override suspend fun nextStage(obj: LogObj, oper: LogOper, stage: TaskStage.Stages) {
            setStage(obj, oper, stage)
        }

        override suspend fun nextStage(obj: LogObj, oper: LogOper, stageExecutor: suspend () -> Boolean): Boolean {
            setStage(obj, oper, TaskStage.Stages.START)
            return if (stageExecutor.invoke()) {
                setStage(obj, oper, TaskStage.Stages.SUCCESS)
                true
            } else {
                setStage(obj, oper, TaskStage.Stages.FAILED)
                false
            }
        }

        private fun setStage(obj: LogObj, oper: LogOper, stage: TaskStage.Stages) {
            val taskStage = TaskStage(Constants.TetroidView.Settings, obj, oper, stage)
            val mes = logger.logTaskStage(taskStage)
            postViewEvent(Constants.ViewEvents.ShowProgressText, mes)
        }
    }

}

open class CallbackParam(
    val event: Any,
    val data: Any?
)

class EmptyPassCheckingFieldCallbackParam(
    val fieldName: String,
    val passHash: String,
    callback: CallbackParam
) : CallbackParam(callback.event, callback.data)
