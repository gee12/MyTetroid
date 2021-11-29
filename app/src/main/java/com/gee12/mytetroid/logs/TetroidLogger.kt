package com.gee12.mytetroid.logs

import android.text.TextUtils
import androidx.annotation.StringRes
import com.gee12.mytetroid.model.TetroidObject
import kotlin.jvm.JvmOverloads
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.logs.TaskStage.Stages


abstract class TetroidLogger : FileTetroidLogger() {
    
    companion object {
        const val PRESENT_SIMPLE = 0
        const val PAST_PERFECT = 1
        const val PRESENT_CONTINUOUS = 2
    }

    //region Operation start

    override fun logOperStart(obj: LogObj, oper: LogOper, tetroidObj: TetroidObject?): String {
        return logOperStart(obj, oper, addIdName(tetroidObj))
    }

    @JvmOverloads
    override fun logOperStart(obj: LogObj, oper: LogOper, add: String): String {
        // меняем местами существительное и глагол в зависимости от языка
        val first = if (App.isRusLanguage()) oper.getString(PRESENT_CONTINUOUS, ::getStringArray) else obj.getString(PRESENT_CONTINUOUS, ::getStringArray)
        val second = if (App.isRusLanguage()) obj.getString(PRESENT_CONTINUOUS, ::getStringArray) else oper.getString(PRESENT_CONTINUOUS, ::getStringArray)
        val mes = String.format(getString(R.string.log_oper_start_mask), first, second) + add
        log(mes, LogType.INFO)
        return mes
    }

    //endregion Operation start

    //region Operation cancel

    override fun logOperCancel(obj: LogObj, oper: LogOper): String {
        val mes = String.format(
            getString(R.string.log_oper_cancel_mask),
            obj.getString(PRESENT_CONTINUOUS, ::getStringArray), oper.getString(PRESENT_CONTINUOUS, ::getStringArray)
        )
        log(mes, LogType.DEBUG)
        return mes
    }

    //endregion Operation cancel

    //region Operation result

//    fun logOperRes(obj: LogObj, oper: LogOper, o: TetroidObject?, showAdd: Boolean): String {
//        return logOperRes(obj, oper, addIdName(o), true, showAdd)
//    }

    override fun logOperRes(obj: LogObj, oper: LogOper, tetroidObj: TetroidObject?, show: Boolean): String {
        return logOperRes(obj, oper, addIdName(tetroidObj), show)
    }

//    override fun logOperRes(obj: LogObj, oper: LogOper, add: String = "", show: Boolean = true): String {
    override fun logOperRes(obj: LogObj, oper: LogOper, add: String, show: Boolean): String {
        val mes = obj.getString(PAST_PERFECT, ::getStringArray) + oper.getString(PAST_PERFECT, ::getStringArray) + add
        log(mes, LogType.INFO, show)
        return mes
    }

    fun logOperRes(obj: LogObj, oper: LogOper, add: String, show: Boolean, showAdd: Boolean): String {
        var show = show
        var mes = obj.getString(PAST_PERFECT, ::getStringArray) + oper.getString(PAST_PERFECT, ::getStringArray)
        if (!showAdd) {
            showMessage(mes, LogType.INFO)
            show = false
        }
        mes += add
        log(mes, LogType.INFO, show)
        return mes
    }

    //endregion Operation result

    //region Operation error

    fun logOperErrorMore(obj: LogObj, oper: LogOper): String {
        return logOperError(obj, oper, true)
    }

    override fun logOperErrorMore(obj: LogObj, oper: LogOper, show: Boolean): String {
        return logOperError(obj, oper, null, true, show)
    }

    override fun logOperError(obj: LogObj, oper: LogOper, show: Boolean): String {
        return logOperError(obj, oper, null, show, show)
    }

    override fun logOperError(obj: LogObj, oper: LogOper, add: String?, more: Boolean, show: Boolean): String {
        val mes = String.format(
            getString(R.string.log_oper_error_mask),
            oper.getString(PRESENT_SIMPLE, ::getStringArray), obj.getString(PRESENT_SIMPLE, ::getStringArray),
            add ?: ""
        )
//                (more) ? context.getString(R.string.log_more_in_logs) : "");
        log(mes, LogType.ERROR, show)
        if (more) {
            showSnackMoreInLogs()
        }
        return mes
    }

    fun logDuringOperErrors(obj: LogObj, oper: LogOper, show: Boolean): String {
        val mes = String.format(
            getString(R.string.log_during_oper_errors_mask),
            oper.getString(PRESENT_CONTINUOUS, ::getStringArray), obj.getString(PRESENT_CONTINUOUS, ::getStringArray)
        )
        log(mes, LogType.ERROR, show)
        showSnackMoreInLogs()
        return mes
    }

    /**
     * Сообщение о параметре=null
     * @param methodName
     */
    override fun logEmptyParams(methodName: String) {
        val start = if (!TextUtils.isEmpty(methodName)) "$methodName: " else ""
        logWarning(start + "Some required parameter(s) is null")
    }

    //endregion Operation error

    //region Task stage

    fun logTaskStage(stage: TaskStage): String? {
        when (stage.stage) {
            Stages.START -> {
//                if (stage.task == StorageEncryptionSettingsFragment.ChangePassTask.class) {
                if (stage.viewType === Constants.TetroidView.Settings) {
                    return when (stage.oper) {
                        LogOper.CHECK -> logTaskStage(stage, R.string.stage_pass_checking, LogType.INFO)
                        LogOper.SET -> logTaskStage(
                            stage,
                            if (stage.obj == LogObj.CUR_PASS) R.string.log_set_cur_pass else R.string.log_set_new_pass,
                            LogType.INFO
                        )
                        LogOper.DECRYPT -> logTaskStage(stage, R.string.stage_old_pass_decrypting, LogType.INFO)
                        LogOper.REENCRYPT -> logTaskStage(stage, R.string.stage_new_pass_reencrypting, LogType.INFO)
                        LogOper.SAVE -> logTaskStage(
                            stage,
                            if (stage.obj == LogObj.STORAGE) R.string.stage_storage_saving else R.string.log_save_pass,
                            LogType.INFO
                        )
                        else -> logOperStart(stage.obj, stage.oper)
                    }
//                } else if (stage.viewType == MainActivity.CryptNodeTask.class) {
                } else if (stage.viewType === Constants.TetroidView.Main) {
                    return when (stage.oper) {
                        LogOper.DECRYPT -> logTaskStage(stage, R.string.stage_storage_decrypting, LogType.INFO)
                        LogOper.ENCRYPT -> logTaskStage(stage, R.string.task_node_encrypting, LogType.INFO)
                        LogOper.DROPCRYPT -> logTaskStage(stage, R.string.task_node_drop_crypting, LogType.INFO)
                        else -> logOperStart(stage.obj, stage.oper)
                    }
                }
                return logOperStart(stage.obj, stage.oper)
            }
            Stages.SUCCESS -> return logOperRes(stage.obj, stage.oper, "", false)
            Stages.FAILED -> return logDuringOperErrors(stage.obj, stage.oper, false)
        }
        return null
    }

    fun logTaskStage(taskStage: TaskStage, resId: Int, type: LogType): String {
        val mes = getString(resId)
        if (taskStage.writeLog) {
            log(mes, type)
        }
        return mes
    }

    //endregion Task stage

    //region Show message

    /**
     * Вывод интерактивного уведомления SnackBar "Подробнее в логах".
     */
    abstract fun showSnackMoreInLogs()

    //endregion Show message

    //region String utils

    /**
     * Формирование строки с идентификатором объекта хранилища.
     */
    fun addIdName(obj: TetroidObject?): String {
        return obj?.let { getIdString(obj) } ?: ""
    }

    /**
     * Формирование строки с именем и id объекта хранилища.
     */
    fun getIdNameString(obj: TetroidObject): String {
        return getStringFormat(R.string.log_obj_id_name_mask, obj.id, obj.name)
    }

    /**
     * Формирование строки с id объекта хранилища.
     */
    fun getIdString(obj: TetroidObject): String {
        return getStringFormat(R.string.log_obj_id_mask, obj.id)
    }

    fun getStringFormat(@StringRes formatRes: Int, vararg args: String?): String {
//        return Utils.getStringFormat(formatRes, if (args != null && args.size > 1) args as Array<Any?> else args)
        return getString(formatRes).format(*args)
    }

    abstract fun getStringArray(resId: Int): Array<String>

    //endregion String utils

}