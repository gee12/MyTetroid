package com.gee12.mytetroid.ui.history

import com.gee12.mytetroid.model.HistoryEntity
import com.gee12.mytetroid.ui.base.BaseEvent

sealed class HistoryEvent : BaseEvent() {
    data class LoadData(val items: List<HistoryEntity>) : HistoryEvent()
}