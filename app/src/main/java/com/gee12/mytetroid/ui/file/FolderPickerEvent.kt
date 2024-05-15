package com.gee12.mytetroid.ui.file

import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.ui.base.BaseEvent

sealed class FolderPickerEvent : BaseEvent() {
    data class LoadFolder(
        val path: String,
        val items: List<FileItem>,
        val failures: List<Failure>,
    ) : FolderPickerEvent()
    data class ShowErrorMessage(val message: String) : FolderPickerEvent()
    data class SelectPathAndExit(val path: String) : FolderPickerEvent()
    object Exit : FolderPickerEvent()
}