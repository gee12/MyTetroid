package com.gee12.mytetroid.logs

import com.gee12.mytetroid.model.TetroidObject

interface ITetroidLogger {
    fun logWithoutShow(s: String, type: LogType)

    fun log(s: String, type: LogType, show: Boolean = true)
    fun log(resId: Int, type: LogType, show: Boolean = true)
    fun log(s: String, show: Boolean = true)
    fun log(resId: Int, show: Boolean = true)

    fun logDebug(s: String, show: Boolean = false)
    fun logDebug(resId: Int, show: Boolean = false)

    fun logWarning(s: String, show: Boolean = true)
    fun logWarning(resId: Int, show: Boolean = true)

    fun logOperStart(obj: LogObj, oper: LogOper, tetroidObj: TetroidObject?): String
    fun logOperStart(obj: LogObj, oper: LogOper, add: String = ""): String
    fun logOperCancel(obj: LogObj, oper: LogOper): String
    fun logOperRes(obj: LogObj, oper: LogOper, tetroidObj: TetroidObject?, show: Boolean): String
    fun logOperRes(obj: LogObj, oper: LogOper, add: String = "", show: Boolean = true): String

    fun logError(s: String, show: Boolean = true)
    fun logError(resId: Int, show: Boolean = true)
    fun logError(ex: Exception, show: Boolean = true)
    fun logError(s: String, ex: Exception, show: Boolean = true)
    fun logOperError(obj: LogObj, oper: LogOper, show: Boolean): String
    fun logOperError(obj: LogObj, oper: LogOper, add: String?, more: Boolean, show: Boolean): String
    fun logOperErrorMore(obj: LogObj, oper: LogOper, show: Boolean): String

    fun logEmptyParams(methodName: String)
}

enum class LogType(val tag: String) {
    INFO("INFO"),
    DEBUG("DEBUG"),
    WARNING("WARNING"),
    ERROR("ERROR")
}
