package com.gee12.mytetroid.views.dialogs.attach

import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.views.dialogs.TetroidDialogFragment
import com.lumyjuwon.richwysiwygeditor.R

class AttachFileByURLDialog(
    val callback: IAttachFileByURLResult
) : TetroidDialogFragment<StorageViewModel>() {

    interface IAttachFileByURLResult {
        fun onApply(url: String?)
    }

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun getLayoutResourceId() = com.lumyjuwon.richwysiwygeditor.R.layout.dialog_insert_web_content

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(com.gee12.mytetroid.R.string.title_download_attach_file)
        val etLink = view.findViewById<EditText>(R.id.edit_text_link)
        view.findViewById<View>(R.id.checkbox_text_only).visibility = View.GONE

        setPositiveButton(com.gee12.mytetroid.R.string.answer_ok) { _, _ -> callback.onApply(etLink.text.toString()) }
        setNegativeButton(com.gee12.mytetroid.R.string.answer_cancel)

        showKeyboard(etLink)
    }

    companion object {
        const val TAG = "AttachFileByURLDialog"
    }
}