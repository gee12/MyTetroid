package com.gee12.mytetroid.ui.dialogs.storage

import android.content.DialogInterface
import android.view.View
import android.widget.CheckedTextView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.gee12.mytetroid.R
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.dialogs.BaseDialogFragment

class DeleteStorageDialog(
    private val storage: TetroidStorage,
    private val isCurrentStorage: Boolean,
    private val onApply: (Boolean) -> Unit,
) : BaseDialogFragment() {

    private lateinit var cbIsDeleteFiles: CheckedTextView

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_delete_storage

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(R.string.ask_delete_storage_title)
        val message = buildSpannedString {
            if (isCurrentStorage) {
                bold { appendLine(getString(R.string.title_storage_currently_in_use_mask, storage.name)) }
                append(getString(R.string.ask_delete_storage_anyway))
            } else {
                append(getString(R.string.ask_delete_storage_mask, storage.name))
            }
        }
        view.findViewById<TextView>(R.id.text_view_message)?.setText(message)

        cbIsDeleteFiles = view.findViewById(R.id.check_box_is_with_files)
        cbIsDeleteFiles.setOnClickListener { cbIsDeleteFiles.isChecked = !cbIsDeleteFiles.isChecked }
        cbIsDeleteFiles.isChecked = false

        // кнопки результата
        setPositiveButton(R.string.answer_ok) { _: DialogInterface?, _: Int ->
            val deleteWithFiles = cbIsDeleteFiles.isChecked
            onApply(deleteWithFiles)
        }
        setNegativeButton(R.string.answer_cancel)
    }

    companion object {
        const val TAG = "DeleteStorageDialog"
    }
}