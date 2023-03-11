package com.gee12.mytetroid.ui.dialogs.storage

import android.content.DialogInterface
import android.view.View
import android.widget.CheckedTextView
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.R
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.dialogs.TetroidDialogFragment
import com.gee12.mytetroid.ui.storage.StorageViewModel

class DeleteStorageDialog(
    private val storage: TetroidStorage,
    private val onApply: (Boolean) -> Unit,
) : TetroidDialogFragment<StorageViewModel>() {

    private lateinit var cbIsDeleteFiles: CheckedTextView

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_delete_storage

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(getString(R.string.ask_delete_storage_mask, storage.name))

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