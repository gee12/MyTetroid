package com.gee12.mytetroid.ui.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.R

/**
 * Диалог запроса полного доступа к файловой системе устройства.
 */
class FullFileStoragePermissionDialog(
    private val onSuccess: () -> Unit,
    private val onCancel: () -> Unit,
) : BaseDialogFragment() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_full_file_storage_permission

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        isCancelable = false
        setTitle(R.string.ask_permission_on_all_files_on_device_title)
        setPositiveButton(R.string.answer_ok) { _, _ ->
            onSuccess()
        }
        setNegativeButton(R.string.answer_cancel) { _, _ ->
            onCancel()
        }
    }

    companion object {
        const val TAG = "RecordInfoDialog"
    }

}
