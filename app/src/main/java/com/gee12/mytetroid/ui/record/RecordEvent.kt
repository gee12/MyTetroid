package com.gee12.mytetroid.ui.record

import android.os.Bundle
import androidx.documentfile.provider.DocumentFile
import com.gee12.htmlwysiwygeditor.model.ImageParams
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidImage
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.ui.storage.StorageEvent

sealed class RecordEvent : StorageEvent() {

    // migration
    object NeedMigration : RecordEvent()

    // ui
    object UpdateOptionsMenu : RecordEvent()

    data class UpdateTitle(
        val title: String,
    ) : RecordEvent()

    data class ShowHomeButton(
        val isVisible: Boolean,
    ) : RecordEvent()

    data class LoadFields(
        val record: TetroidRecord,
    ) : RecordEvent()
    data class ShowEditFieldsDialog(
        val resultObj: ResultObject?,
    ) : RecordEvent()
    sealed class SaveFields : RecordEvent() {
        object InProcess : SaveFields()
        object Success : SaveFields()
    }
    data class LoadHtmlTextFromFile(
        val recordText: String,
    ) : RecordEvent()
    object LoadRecordTextFromHtml : RecordEvent()
    data class AskForLoadAllNodes(
        val resultObj: ResultObject,
    ) : RecordEvent()
    data class FileAttached(
        val attach: TetroidFile,
    ) : RecordEvent()
    data class SwitchEditorMode(
        val mode: EditorMode,
    ) : RecordEvent()
    data class AskForSaving(
        val resultObj: ResultObject,
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
    data class EditImage(
        val params: ImageParams,
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
    data class AskToOpenExportedPdf(
        val pdfFile: DocumentFile,
    ) : RecordEvent()
    data class GetHtmlTextAndSaveToFile(
        val obj: ResultObject
    ) : RecordEvent()

    // another
    data class FinishActivityWithResult(
        val resultActionType: Int? = null,
        val bundle: Bundle? = null,
        var isOpenMainActivity: Boolean = true,
    ) : RecordEvent()
    data class OpenRecordNodeInMainView(val nodeId: String) : RecordEvent()
    data class OpenAnotherRecord(val recordId: String) : RecordEvent()
    data class OpenAnotherNode(val nodeId: String) : RecordEvent()
    data class OpenTag(val tagName: String) : RecordEvent()
    data class OpenRecordAttaches(val recordId: String) : RecordEvent()
    data class DeleteRecord(val recordId: String) : RecordEvent()

}