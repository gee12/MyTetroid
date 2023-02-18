package com.gee12.mytetroid.ui.storages

import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.base.BaseEvent

sealed class StoragesEvent : BaseEvent() {
    data class AddedNewStorage(
        val storage: TetroidStorage,
    ) : StoragesEvent()
}