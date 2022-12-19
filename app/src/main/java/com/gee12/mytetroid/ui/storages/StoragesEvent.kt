package com.gee12.mytetroid.ui.storages

import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.base.BaseEvent

sealed class StoragesEvent : BaseEvent() {
    object ShowAddNewStorageDialog : StoragesEvent()
    data class AddedNewStorage(
        val storage: TetroidStorage,
    ) : StoragesEvent()
}