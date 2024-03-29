package com.gee12.mytetroid.ui.dialogs.attach

import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.storage.StorageViewModel
import com.gee12.mytetroid.ui.dialogs.TetroidStorageDialogFragment

/**
 * Диалог информации о прикрепленном файле.
 */
class AttachInfoDialog(
    val attach: TetroidFile?,
    override var storageId: Int?
) : TetroidStorageDialogFragment<StorageViewModel>() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = attach != null && attach.isNonCryptedOrDecrypted

    override fun getLayoutResourceId() = R.layout.dialog_attach_info

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        dialog.setTitle(attach?.name)
        setPositiveButton(R.string.answer_ok)
    }

    override fun onStorageInited(storage : TetroidStorage) {
        // проверяем уже после загрузки хранилища
        if (attach?.record == null) {
            viewModel.logError(getString(R.string.log_file_record_is_null), true)
            dismiss()
            return
        }
        val record = attach.record

        (dialogView.findViewById<View>(R.id.text_view_id) as TextView).text = attach.id
        (dialogView.findViewById<View>(R.id.text_view_record) as TextView).text = record?.name
        (dialogView.findViewById<View>(R.id.text_view_crypted) as TextView).setText(
            if (attach.isCrypted) R.string.answer_yes else R.string.answer_no
        )
        val dateFormat = getString(R.string.full_date_format_string)

        if (viewModel.buildInfoProvider.isFullVersion()) {
            dialogView.findViewById<View>(R.id.table_row_edited)?.visibility = View.VISIBLE
            val edited = viewModel.getAttachEditedDate(requireContext(), attach)
            (dialogView.findViewById<View>(R.id.text_view_edited) as TextView).text =
                if (edited != null) Utils.dateToString(edited, dateFormat) else "-"
        }
        val path = viewModel.getPathToRecordFolder(record!!)
        (dialogView.findViewById<View>(R.id.text_view_path) as TextView).text = path.fullPath
        var sizeString = viewModel.getAttachFileSize(requireContext(), attach)
        val tvSize = dialogView.findViewById<TextView>(R.id.text_view_size)
        if (sizeString == null) {
            sizeString = getString(R.string.title_file_is_missing)
            tvSize?.setTextColor(ContextCompat.getColor(requireActivity().applicationContext, R.color.error_2))
        }
        tvSize?.text = sizeString
    }

    companion object {
        const val TAG = "AttachInfoDialog"
    }
}