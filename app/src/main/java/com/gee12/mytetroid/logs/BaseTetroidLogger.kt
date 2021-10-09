package com.gee12.mytetroid.logs

abstract class BaseTetroidLogger : ITetroidLogger {

    //region ITetroidLogger

    override fun log(s: String, type: LogType, show: Boolean) {
        logWithoutShow(s, type)
        if (show) {
            showMessage(s, type)
        }
    }

    override fun log(resId: Int, type: LogType, show: Boolean) {
        log(getString(resId), type, show)
    }

    override fun log(s: String, show: Boolean) {
        log(s, LogType.INFO, show)
    }

    override fun log(resId: Int, show: Boolean) {
        log(getString(resId), LogType.INFO, show)
    }

    override fun logDebug(s: String, show: Boolean) {
        log(s, LogType.DEBUG, show)
    }

    override fun logDebug(resId: Int, show: Boolean) {
        log(getString(resId), LogType.DEBUG, show)
    }

    override fun logWarning(s: String, show: Boolean) {
        log(s, LogType.WARNING, show)
    }

    override fun logWarning(resId: Int, show: Boolean) {
        log(getString(resId), LogType.WARNING, show)
    }

    override fun logError(s: String, show: Boolean) {
        log(s, LogType.ERROR, show)
    }

    override fun logError(resId: Int, show: Boolean) {
        log(getString(resId), LogType.ERROR, show)
    }

    override fun logError(ex: Exception, show: Boolean) {
        log(FileTetroidLogger.getExceptionInfo(ex), LogType.ERROR, show)
    }

    override fun logError(s: String, ex: Exception, show: Boolean) {
        log("$s: ${FileTetroidLogger.getExceptionInfo(ex)}", LogType.ERROR, show)
    }

    //endregion ITetroidLogger

    //region Show message

    abstract fun showMessage(s: String, type: LogType)

    //endregion Show message

    //region String utils

    abstract fun getString(resId: Int): String

    //endregion String utils

}