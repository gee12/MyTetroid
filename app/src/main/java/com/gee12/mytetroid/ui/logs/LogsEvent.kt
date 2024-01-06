package com.gee12.mytetroid.ui.logs

import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.ui.base.BaseEvent

sealed class LogsEvent : BaseEvent() {
    sealed class LoadFromFile : LogsEvent() {
        object InProcess : LoadFromFile()
        data class Success(var data: List<String>) : LoadFromFile()
        object LogPathIsEmpty : LoadFromFile()
        data class Failed(var failure: Failure, val logFullFileName: String) : LoadFromFile()
    }
    sealed class LoadFromBuffer : LogsEvent() {
        object InProcess : LoadFromBuffer()
        data class Success(var data: List<String>) : LoadFromBuffer()
        data class Failed(var failure: Failure) : LoadFromBuffer()
    }
}