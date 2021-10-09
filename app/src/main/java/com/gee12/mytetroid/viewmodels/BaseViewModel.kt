package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.SingleLiveEvent
import com.gee12.mytetroid.logs.*
import com.gee12.mytetroid.model.TetroidObject
import com.gee12.mytetroid.utils.StringUtils
import java.util.*

open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    val viewEvent = SingleLiveEvent<ViewModelEvent<Constants.ViewEvents, Any>>()

    // FIXME: почему open ?
    open var logger: TetroidLogger = BaseLogger().apply {
//        init(SettingsManager.getLogPath(getContext()), SettingsManager.isWriteLogToFile(getContext()))
    }

    val messageObservable = MutableLiveData<Message>()


    //region View event

    fun postViewEvent(event: Constants.ViewEvents, param: Any? = null) {
        Log.i("MYTETROID", "postViewEvent(): state=$event param=$param")
        viewEvent.postValue(ViewModelEvent(event, param))
    }

    fun setViewEvent(event: Constants.ViewEvents, param: Any? = null) {
        Log.i("MYTETROID", "setViewEvent(): state=$event param=$param")
        viewEvent.value = ViewModelEvent(event, param)
    }

    //endregion View event

    //region Log

    // info
    @JvmOverloads
    fun log(mes: String, show: Boolean = false) {
        logger.log(mes, show)
    }

    @JvmOverloads
    fun log(resId: Int, show: Boolean = false) {
        logger.log(resId, show)
    }

    // debug
    @JvmOverloads
    fun logDebug(mes: String, show: Boolean = false) {
        logger.logDebug(mes, show)
    }

    @JvmOverloads
    fun logDebug(resId: Int, show: Boolean = false) {
        logger.logDebug(resId, show)
    }

    // warning
    @JvmOverloads
    fun logWarning(mes: String, show: Boolean = true) {
        logger.logWarning(mes, show)
    }

    @JvmOverloads
    fun logWarning(resId: Int, show: Boolean = true) {
        logger.logWarning(resId, show)
    }

    // error
    @JvmOverloads
    fun logError(mes: String, show: Boolean = true) {
        logger.logError(mes, show)
    }

    @JvmOverloads
    fun logError(resId: Int, show: Boolean = true) {
        logger.logError(resId, show)
    }

    @JvmOverloads
    fun logError(ex: Exception, show: Boolean = true) {
        logger.logError(ex, show)
    }

    @JvmOverloads
    fun logError(s: String, ex: Exception, show: Boolean = true) {
        logger.logError(s, ex, show)
    }

    // common
    @JvmOverloads
    fun log(mes: String, type: LogType, show: Boolean = false) {
        logger.log(mes, type)
        if (show) {
            showMessage(mes, type)
        }
    }

    @JvmOverloads
    fun log(resId: Int, type: LogType, show: Boolean = false) {
        logger.log(resId, type, show)
    }

    private fun addIdName(obj: TetroidObject?): String {
        return logger.addIdName(obj)
    }

//    open fun getIdString(obj: TetroidObject): String? {
////        return getStringFormat(R.string.log_obj_id_mask, obj.id)
//        return getString(R.string.log_obj_id_mask).format(obj.id)
//    }

//    open fun getStringFormat(@StringRes formatRes: Int, vararg args: String?): String? {
//        return StringUtils.getStringFormat(getContext(), formatRes, if (args != null && args.size > 1) args as Array<Any?> else args)
//    }

    // operation result
    open fun logOperRes(obj: LogObj, oper: LogOper): String {
        return logger.logOperRes(obj, oper, "", true)
    }

    open fun logOperRes(obj: LogObj, oper: LogOper, tetroidObj: TetroidObject?, show: Boolean): String {
        return logger.logOperRes(obj, oper, StringUtils.getIdNameString(getContext(), tetroidObj), show)
    }

    open fun logOperRes(obj: LogObj, oper: LogOper, add: String, show: Boolean): String {
        return logger.logOperRes(obj, oper, add, show)
    }

    // operation start
    open fun logOperStart(obj: LogObj, oper: LogOper): String {
        return logger.logOperStart(obj, oper, "")
    }

    open fun logOperStart(obj: LogObj, oper: LogOper, tetroidObj: TetroidObject?): String {
        return logger.logOperStart(obj, oper, addIdName(tetroidObj))
    }

    open fun logOperStart(obj: LogObj, oper: LogOper, add: String): String {
        return logger.logOperStart(obj, oper, add)
    }

    open fun logOperError(obj: LogObj, oper: LogOper, add: String?, more: Boolean, show: Boolean): String? {
        return logger.logOperError(obj, oper, add, more, show)
    }

    open fun logOperError(obj: LogObj, oper: LogOper, show: Boolean = false): String? {
        return logger.logOperError(obj, oper, null, false, show)
    }

    @JvmOverloads
    open fun logOperErrorMore(obj: LogObj, oper: LogOper, show: Boolean = false): String? {
        return logger.logOperError(obj, oper, null, true, show)
    }

    open fun logDuringOperErrors(obj: LogObj, oper: LogOper, show: Boolean): String? {
        return logger.logDuringOperErrors(obj, oper, show)
    }

    //endregion Log

    //region Message

    fun showMessage(message: String) {
        showMessage(message, LogType.INFO)
    }

    fun showWarning(message: String) {
        showMessage(message, LogType.WARNING)
    }

    fun showError(message: String) {
        showMessage(message, LogType.ERROR)
    }

    fun showMessage(message: String, type: LogType) {
        messageObservable.postValue(Message(message, type))
    }

    fun showSnackMoreInLogs() {
        postViewEvent(Constants.ViewEvents.ShowMoreInLogs)
    }

    //endregion Message

    //region Context

//    fun getString(resId: Int) = (getApplication() as Context).getString(resId)
    fun getString(resId: Int) = getApplication<Application>().resources.getString(resId)
    fun getString(resId: Int, vararg params: Any) = getApplication<Application>().resources.getString(resId, params)
    fun getStringArray(resId: Int) = getApplication<Application>().resources.getStringArray(resId)

    fun getContext(): Context = getApplication()

    //endregion Context


    inner class BaseLogger : TetroidLogger() {

        override fun showMessage(s: String, type: LogType) = this@BaseViewModel.showMessage(s, type)

        override fun showSnackMoreInLogs() = this@BaseViewModel.showSnackMoreInLogs()

        override fun getString(resId: Int) = this@BaseViewModel.getString(resId)

        override fun getStringArray(resId: Int) = this@BaseViewModel.getStringArray(resId)
    }

}

data class ViewModelException<E>(val event: E, val throwable: Throwable)

data class ViewModelEvent<S, D>(var state: S, var data: D?) {
    constructor(state: S): this(state, null)
}