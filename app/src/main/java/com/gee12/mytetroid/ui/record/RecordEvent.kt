package com.gee12.mytetroid.ui.record

import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidImage
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.ui.storage.StorageEvent

sealed class RecordEvent : StorageEvent() {
    object NeedMigration : RecordEvent()
    data class LoadFields(
        val record: TetroidRecord,
    ) : RecordEvent()
    data class EditFields(
        val resultObj: ResultObj?,
    ) : RecordEvent()
    data class LoadRecordTextFromFile(
        val recordText: String,
    ) : RecordEvent()
    object LoadRecordTextFromHtml : RecordEvent()
    data class AskForLoadAllNodes(
        val resultObj: ResultObj,
    ) : RecordEvent()
    data class FileAttached(
        val attach: TetroidFile,
    ) : RecordEvent()
    data class SwitchViews(
        val viewMode: Int,
    ) : RecordEvent()
    data class AskForSaving(
        val resultObj: ResultObj?,
    ) : RecordEvent()
    object BeforeSaving : RecordEvent()
    data class IsEditedChanged(
        val isEdited: Boolean,
    ) : RecordEvent()
    data class EditedDateChanged(
        val dateString: String,
    ) : RecordEvent()
    object StartLoadImages : RecordEvent()
    object StartCaptureCamera : RecordEvent()
    data class InsertImages(
        val images: List<TetroidImage>,
    ) : RecordEvent()
    data class InsertWebPageContent(
        val content: String,
    ) : RecordEvent()
    data class InsertWebPageText(
        val text: String,
    ) : RecordEvent()
    data class OpenWebLink(
        val link: String,
    ) : RecordEvent()
    object Save : RecordEvent()
}