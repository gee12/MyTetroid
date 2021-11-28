package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Context
import androidx.annotation.MainThread
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.ICallback
import com.gee12.mytetroid.data.ITaskProgress
import com.gee12.mytetroid.data.CommonSettings
import com.gee12.mytetroid.data.TetroidXml
import com.gee12.mytetroid.data.crypt.TetroidCrypter
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.interactors.PasswordInteractor
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.logs.TaskStage
import com.gee12.mytetroid.logs.TetroidLogger
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.utils.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

open class StorageEncryptionViewModel(
    app: Application,
    logger: TetroidLogger?,
    storagesRepo: StoragesRepo,
    xmlLoader: TetroidXml,
    crypter: TetroidCrypter?
) : StorageSettingsViewModel(
    app,
    logger,
    storagesRepo,
    xmlLoader,
    crypter
) {

    val databaseConfig = DatabaseConfig(logger)

    val passInteractor = PasswordInteractor(this.logger, /*storage,*/ databaseConfig, cryptInteractor, storageInteractor, nodesInteractor)
//    val pinInteractor = PinInteractor(passInteractor, cryptInteractor, nodesInteractor)

    var isPinNeedEnter = false


    override fun isCrypted(): Boolean {
        var iniFlag = false
        try {
            iniFlag = databaseConfig.isCryptMode
        } catch (ex: Exception) {
            logError(ex)
        }
        /*return (iniFlag == 1 && instance.mIsExistCryptedNodes) ? true
                : (iniFlag != 1 && !instance.mIsExistCryptedNodes) ? false
                : (iniFlag == 1 && !instance.mIsExistCryptedNodes) ? true
                : (iniFlag == 0 && instance.mIsExistCryptedNodes) ? true : false;*/
        return iniFlag || nodesInteractor.isExistCryptedNodes(false)
    }

    //region Password

    /**
     * Асинхронная проверка - имеется ли сохраненный пароль, и его запрос при необходимости.
     * Используется:
     *      * когда хранилище уже загружено (зашифровка/сброс шифровки ветки)
     *      * либо когда загрузка хранилища не требуется (установка/сброс ПИН-кода)
     * @param callback
     */
    fun checkStoragePass(callback: EventCallbackParams) {

        var middlePassHash: String?
        when {
            crypter.middlePassHash.also { middlePassHash = it } != null -> {
                // хэш пароля сохранен в оперативной памяти (вводили до этого и проверяли)
                cryptInteractor.initCryptPass(middlePassHash!!, true)
                // запрос ПИН-кода
                askPinCode(true, callback)
            }
            isSaveMiddlePassLocal() && storage?.middlePassHash.also { middlePassHash = it } != null -> {
                // хэш пароля сохранен локально, проверяем
                try {
                    if (passInteractor.checkMiddlePassHash(middlePassHash)) {
                        cryptInteractor.initCryptPass(middlePassHash!!, true)
                        // запрос ПИН-кода
                        askPinCode(true, callback)
                    } else {
                        logger.log(R.string.log_wrong_saved_pass, true)
                        // спрашиваем пароль
                        askPassword(/*node,*/ callback)
                    }
                } catch (ex: DatabaseConfig.EmptyFieldException) {
                    // если поля в INI-файле для проверки пустые
                    logError(ex)
                    // if (DataManager.isExistsCryptedNodes()) {
                    if (isCrypted()) {
                        // спрашиваем "continue anyway?"
                        postStorageEvent(Constants.StorageEvents.AskForEmptyPassCheckingField,
                            EmptyPassCheckingFieldCallbackParams(ex.fieldName, middlePassHash!!, callback)
                        )
                    } else {
                        // если нет зашифрованных веток, но пароль сохранен
                        cryptInteractor.initCryptPass(middlePassHash!!, true)
                        askPinCode(true, callback)
                    }
                }
            }
            else -> {
                // спрашиваем или задаем пароль
                askPassword(callback)
            }
        }
    }

    fun confirmEmptyPassCheckingFieldDialog(callback: EmptyPassCheckingFieldCallbackParams) {
        cryptInteractor.initCryptPass(callback.passHash, true)
        askPinCode(true, callback)
    }

    /**
     * Отображения запроса пароля от хранилища.
     * @param callback
     */
    fun askPassword(callback: EventCallbackParams) {
        logger.log(R.string.log_show_pass_dialog, false)
        // выводим окно с запросом пароля в асинхронном режиме
        postStorageEvent(Constants.StorageEvents.AskPassword, callback)
    }

    fun onPasswordEntered(pass: String, isSetup: Boolean, callback: EventCallbackParams) {
        if (isSetup) {
            launch {
                setupPass(pass)
                postEventFromCallbackParam(callback)
            }
        } else {
            checkPass(pass, { res: Boolean ->
                launch {
                    if (res) {
                        initPass(pass)
                        postEventFromCallbackParam(callback)
                    } else {
                        // повторяем запрос
                        askPassword(callback)
                    }
                }
            }, R.string.log_pass_is_incorrect)
        }
    }

    open fun onPasswordCanceled(isSetup: Boolean, callback: EventCallbackParams) {
        if (!isSetup) {
            (callback.data as? StorageParams)?.let {
                if (!it.isNodeOpening) {
//                    isAlreadyTryDecrypt = true
                    it.isDecrypt = false
                    postEventFromCallbackParam(callback)
                }
            }
        }
    }

    fun startSetupPass(pass: String) {
        launch {
            setupPass(pass)
        }
    }

    @MainThread
    suspend fun setupPass(pass: String) {
        log(R.string.log_start_pass_setup)
        setViewEvent(Constants.ViewEvents.TaskStarted, getString(R.string.task_pass_setting))
        isBusy = true
        val result = withContext(Dispatchers.IO) {
            passInteractor.setupPass(storage!!, pass).also {
                // сохраняем хэш пароля в бд (если установлена соответствующая опция)
                updateStorage(storage!!)
            }
        }
        isBusy = false
        setViewEvent(Constants.ViewEvents.TaskFinished)
        if (result) {
            setStorageEvent(Constants.StorageEvents.PassSetuped)
        }
    }

    suspend fun initPass(pass: String) {
        passInteractor.initPass(storage!!, pass)
        updateStorage(storage!!)
    }

    /**
     * Каркас проверки введенного пароля.
     * @param context
     * @param pass
     * @param callback
     * @param wrongPassRes
     */
    fun checkPass(pass: String, callback: ICallback, wrongPassRes: Int): Boolean {
        try {
            if (passInteractor.checkPass(pass)) {
                callback.run(true)
            } else {
                logger.logError(wrongPassRes, true)
                callback.run(false)
                return false
            }
        } catch (ex: DatabaseConfig.EmptyFieldException) {
            // если поля в INI-файле для проверки пустые
            logger.logError(ex)
            // спрашиваем "continue anyway?"
            postStorageEvent(
                Constants.StorageEvents.AskForEmptyPassCheckingField,
                // TODO: тут спрашиваем нормально ли расшифровались данные
                callback
            )
        }
        return true
    }

    fun startChangePass(curPass: String, newPass: String) {
        launch {
            setViewEvent(Constants.ViewEvents.TaskStarted, getString(R.string.task_pass_changing))
            isBusy = true
            val result = changePass(getContext(), curPass, newPass, taskProgressHandler)
            isBusy = false
            setViewEvent(Constants.ViewEvents.TaskFinished)
            if (result) {
                setStorageEvent(Constants.StorageEvents.PassChanged)
                logger.log(R.string.log_pass_changed, true)
            } else {
                logger.logError(R.string.log_pass_change_error, true)
                setViewEvent(Constants.ViewEvents.ShowMoreInLogs)
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
        passInteractor.savePassCheckData(newPass)

        return true
    }

    //endregion Password

    //region Pin

    /**
     * Проверка использования ПИН-кода с учетом версии приложения.
     * @return
     */
    fun isRequestPINCode(): Boolean {
        return (App.isFullVersion()
                && CommonSettings.isRequestPINCode(getContext())
                && isPinNeedEnter)
    }

    /**
     * Запрос ПИН-кода, если установлена опция.
     * К этому моменту факт того, что хэш пароля сохранен локально, должен быть уже проверен.
     * @param specialFlag Дополнительный признак, указывающий на то, нужно ли спрашивать ПИН-код
     * конкретно в данный момент.
     * @param callback Обработчик обратного вызова.
     */
    fun askPinCode(specialFlag: Boolean, callback: EventCallbackParams /*callback: Dialogs.IApplyResult*/) {
        if (isRequestPINCode() && specialFlag) {
            // выводим запрос ввода ПИН-кода
            postStorageEvent(Constants.StorageEvents.AskPinCode, callback)
        } else {
            postEventFromCallbackParam(callback)
        }
    }

    fun startCheckPinCode(pin: String, callback: EventCallbackParams): Boolean {
        // зашифровываем введеный пароль перед сравнением
        val res = checkPinCode(pin)
        if (res) {
            postEventFromCallbackParam(callback)
            // сбрасываем признак
            isPinNeedEnter = false
            logger.log(R.string.log_pin_code_enter)
        }
        return res
    }

    /**
     * Установка/очистка ПИН-кода.
     * Вызывается при установке/снятии опции.
     * При установке сначала проверяется факт того, что хэш пароля сохранен локально.
     */
    fun startSetupOrDropPinCode(callback: EventCallbackParams) {
        if (!isRequestPINCode()) {
            checkStoragePass(EventCallbackParams(Constants.StorageEvents.SetupPinCode, callback))
        } else {
            checkStoragePass(EventCallbackParams(Constants.StorageEvents.DropPinCode, callback))
        }
    }

    fun setupPinCodeLength(length: Int) {
        CommonSettings.setPINCodeLength(getContext(), length)
        logger.log(getString(R.string.log_pin_code_length_setup) + length)
    }

    fun setupPinCode(pin: String) {
        // зашифровываем пароль перед установкой
        val pinHash: String = crypter.passToHash(pin)
        CommonSettings.setPINCodeHash(getContext(), pinHash)
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
        val pinHash = crypter.passToHash(pin)
        return (pinHash == CommonSettings.getPINCodeHash(getContext()))
    }

    protected fun dropPinCode() {
        CommonSettings.setPINCodeHash(getContext(), null)
        logger.log(R.string.log_pin_code_clean, true)
    }

    //endregion Pin


    fun onPassLocalHashLocalParamChanged(newValue: Any): Boolean {
        return if (getMiddlePassHash() != null) {
            askPinCode(
                true,
                EventCallbackParams(Constants.StorageEvents.SavePassHashLocalChanged, newValue)
            )
            false
        } else {
            // если пароль не задан, то нечего очищать, не задаем вопрос
            true
        }
    }

    /**
     * Зашифровка или расшифровка файла записи и прикрепленных файлов при необходимости.
     * @param record
     * @param isEncrypt
     */
    override suspend fun cryptRecordFiles(context: Context, record: TetroidRecord, isCrypted: Boolean, isEncrypt: Boolean): Boolean {
        // файл записи
        val recordFolderPath = getPathToRecordFolder(record)
        var file = File(recordFolderPath, record.fileName)
        if (cryptInteractor.encryptOrDecryptFile(context, file, isCrypted, isEncrypt) < 0) {
            return false
        }
        // прикрепленные файлы
        if (record.attachedFilesCount > 0) {
            for (attach in record.attachedFiles) {
                file = File(recordFolderPath, attach.idName)
                if (!file.exists()) {
                    logger.logWarning(context.getString(R.string.log_file_is_missing) + StringUtils.getIdString(context, attach))
                    continue
                }
                if (cryptInteractor.encryptOrDecryptFile(context, file, isCrypted, isEncrypt) < 0) {
                    return false
                }
            }
        }
        return true
    }


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
            val mes = this@StorageEncryptionViewModel.logger.logTaskStage(taskStage)
            postViewEvent(Constants.ViewEvents.ShowProgressText, mes)
        }
    }

}

open class EventCallbackParams(
    val event: Any,
    val data: Any?
)

class EmptyPassCheckingFieldCallbackParams(
    val fieldName: String,
    val passHash: String,
    callback: EventCallbackParams
) : EventCallbackParams(callback.event, callback.data)
