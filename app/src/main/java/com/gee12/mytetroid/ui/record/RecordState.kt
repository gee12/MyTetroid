package com.gee12.mytetroid.ui.record

data class RecordState(
    var recordId: String,
    var isSavedFromTemporary: Boolean,
    var isFieldsEdited: Boolean,
    var isTextEdited: Boolean,
)
