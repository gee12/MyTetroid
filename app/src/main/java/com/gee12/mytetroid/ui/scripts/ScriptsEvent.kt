package com.gee12.mytetroid.ui.scripts

import android.net.Uri
import com.gee12.mytetroid.model.ITetroidObject
import com.gee12.mytetroid.model.TetroidScript
import com.gee12.mytetroid.ui.base.BaseEvent

sealed class ScriptsEvent : BaseEvent() {

    data class LoadScripts(
        val tetroidObject: ITetroidObject?,
        val scripts: List<TetroidScript>,
    ) : ScriptsEvent()

    object ShowRequestForDefaultScripts : ScriptsEvent()

    data class ShowScriptDialog(
        val script: TetroidScript,
        val scriptText: String,
        val isNew: Boolean,
    ) : ScriptsEvent()

    data class OpenScriptFile(
        val uri: Uri,
        val mimeType: String,
    ) : ScriptsEvent()

}