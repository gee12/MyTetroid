package com.gee12.mytetroid.ui.dialogs.attach

import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.ui.storage.StorageViewModel
import com.gee12.mytetroid.ui.dialogs.TetroidStorageDialogFragment
import com.lumyjuwon.richwysiwygeditor.R

class AttachFileByUrlDialog(
    val onApply: (url: String) -> Unit,
) : TetroidStorageDialogFragment<StorageViewModel>() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_insert_web_content

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(com.gee12.mytetroid.R.string.title_download_attach_file)
        val etLink = view.findViewById<EditText>(R.id.edit_text_link)
        view.findViewById<View>(R.id.checkbox_text_only).visibility = View.GONE

        setPositiveButton(com.gee12.mytetroid.R.string.answer_ok) { _, _ -> onApply(etLink.text.toString()) }
        setNegativeButton(com.gee12.mytetroid.R.string.answer_cancel)

        showKeyboard(etLink)
    }

    companion object {
        const val TAG = "AttachFileByURLDialog"
    }
}