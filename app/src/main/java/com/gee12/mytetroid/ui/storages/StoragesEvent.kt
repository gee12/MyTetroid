package com.gee12.mytetroid.ui.storages

import androidx.documentfile.provider.DocumentFile
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.base.BaseEvent

sealed class StoragesEvent : BaseEvent() {
    data class ShowAddStorageDialog(val isNew: Boolean) : StoragesEvent()
    data class SetStorageFolder(val folder: DocumentFile) : StoragesEvent()
    data class AddedNewStorage(
        val storage: TetroidStorage,
    ) : StoragesEvent()
}