package com.gee12.mytetroid.ui.logs

import com.gee12.mytetroid.ui.base.BaseEvent

sealed class LogsEvent : BaseEvent() {
    object ShowBufferLogs : LogsEvent()
    sealed class Loading : LogsEvent() {
        object InProcess : Loading()
        data class Success(var data: List<String>) : Loading()
        data class Failed(var text: String) : Loading()
    }
}