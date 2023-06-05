package com.gee12.mytetroid.logs

import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.domain.IFailureHandler

abstract class BaseTetroidLogger(
    val failureHandler: IFailureHandler,
) : ITetroidLogger {

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

    override fun logError(ex: Throwable, show: Boolean) {
        log(failureHandler.getExceptionInfo(ex), LogType.ERROR, show)
    }

    override fun logError(s: String, ex: Exception, show: Boolean) {
        log("$s: ${failureHandler.getExceptionInfo(ex)}", LogType.ERROR, show)
    }

    override fun logFailure(failure: Failure, show: Boolean) {
        val message = failureHandler.getFailureMessage(failure).getFullMassage()
        // TODO: сделать многострочные уведомления

        log(message, LogType.ERROR, show)
    }

    //endregion ITetroidLogger

    //region Show message

    abstract fun showMessage(s: String, type: LogType)


    //endregion Show message

    //region String utils

    abstract fun getString(resId: Int, vararg args: Any): String

    //endregion String utils

}