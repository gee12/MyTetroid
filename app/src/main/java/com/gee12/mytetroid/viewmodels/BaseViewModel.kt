package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.logs.Message

open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    val logger = LogManager.createLogger(getContext())

    val message = MutableLiveData<Message>()

    //region Log

    fun log(resId: Int, show: Boolean = false) {
        log(getString(resId), show)
    }

    fun log(resId: Int, type: ILogger.Types, show: Boolean = false) {
        log(getString(resId), type, show)
    }

    fun log(mes: String, show: Boolean = false) {
        log(mes, ILogger.Types.INFO, show)
    }

    fun logError(resId: Int, show: Boolean = false) {
        logError(getString(resId), show)
    }

    fun logError(mes: String, show: Boolean = false) {
        log(mes, ILogger.Types.ERROR, show)
    }

    /**
     * TODO: обработать ex
     */
    fun logError(ex: Exception, show: Boolean = false) {
        LogManager.log(getContext(), ex)
    }

    fun log(mes: String, type: ILogger.Types, show: Boolean = false) {
        LogManager.log(getContext(), mes, type)
        if (show) {
            message.postValue(Message(mes, type))
        }
    }

    //endregion Log

    //region Context

    //    fun getString(resId: Int) = (getApplication() as Context).getString(resId)
    fun getString(resId: Int) = getApplication<Application>().resources.getString(resId)
    fun getString(resId: Int, vararg params: Any) = getApplication<Application>().resources.getString(resId, params)

    fun getContext(): Context = getApplication()

    //endregion Context
}

data class ViewModelException<E>(val event: E, val throwable: Throwable)

data class ViewModelEvent<S, D>(var state: S, var data: D?) {
    constructor(state: S): this(state, null)
}