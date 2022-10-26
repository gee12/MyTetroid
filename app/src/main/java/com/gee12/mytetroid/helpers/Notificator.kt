package com.gee12.mytetroid.helpers

import com.gee12.mytetroid.logs.LogType

interface INotificator {
    fun showMessage(message: String, type: LogType)
    fun showSnackMoreInLogs()

    var showMessageCallback: ((String, LogType) -> Unit)?
    var showSnackMoreInLogsCallback: (() -> Unit)?
}

class Notificator(

) : INotificator {

    override var showMessageCallback: ((String, LogType) -> Unit)? = null
    override var showSnackMoreInLogsCallback: (() -> Unit)? = null

    override fun showMessage(message: String, type: LogType) {
        showMessageCallback?.invoke(message, type)
    }

    override fun showSnackMoreInLogs() {
        showSnackMoreInLogsCallback?.invoke()
    }

}