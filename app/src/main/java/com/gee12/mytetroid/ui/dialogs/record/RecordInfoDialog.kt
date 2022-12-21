package com.gee12.mytetroid.ui.dialogs.record

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.dialogs.TetroidStorageDialogFragment
import com.gee12.mytetroid.ui.storage.StorageViewModel
import kotlinx.coroutines.launch

/**
 * Диалог информации о записи.
 */
class RecordInfoDialog(
    val record: TetroidRecord,
    override var storageId: Int?
) : TetroidStorageDialogFragment<StorageViewModel>() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = record.isNonCryptedOrDecrypted

    override fun getLayoutResourceId() = R.layout.dialog_record_info

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(record.name)
        setPositiveButton(R.string.answer_ok)
    }

    override fun onStorageInited(storage: TetroidStorage) {
        dialogView.findViewById<TextView>(R.id.text_view_id).text = record.id
        val tvNode = dialogView.findViewById<TextView>(R.id.text_view_node)
        when {
            viewModel.isLoadedFavoritesOnly() -> {
                tvNode.setText(R.string.hint_need_load_all_nodes)
                tvNode.setTextColor(Color.LTGRAY)
            }
            record.node != null -> {
                tvNode.text = record.node.name
            }
            else -> {
                tvNode.setText(R.string.hint_error)
                tvNode.setTextColor(Color.LTGRAY)
            }
        }
        dialogView.findViewById<TextView>(R.id.text_view_crypted).setText(
            if (record.isCrypted) R.string.answer_yes else R.string.answer_no
        )
        val dateFormat = getString(R.string.full_date_format_string)
        val created = record.created
        dialogView.findViewById<TextView>(R.id.text_view_created).text =
            if (created != null) Utils.dateToString(created, dateFormat) else "-"

        if (viewModel.buildInfoProvider.isFullVersion()) {
            dialogView.findViewById<View>(R.id.table_row_edited).visibility = View.VISIBLE

            //TODO: использовать события вместо корутины
            lifecycleScope.launch {
                val edited = viewModel.getEditedDate(record)?.let { date ->
                    Utils.dateToString(date, dateFormat)
                } ?: "-"
                dialogView.findViewById<TextView>(R.id.text_view_edited).text = edited
            }
        }
        val path = viewModel.getPathToRecordFolder(record)
        dialogView.findViewById<TextView>(R.id.text_view_path).text = path

        //TODO: использовать события вместо корутины
        lifecycleScope.launch {
            var size = viewModel.getRecordFolderSize(record)
            val tvSize = dialogView.findViewById<TextView>(R.id.text_view_size)
            if (size == null) {
                size = requireContext().getString(R.string.title_folder_is_missing)
                tvSize.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorDarkRed))
            }
            tvSize.text = size
        }
    }

    companion object {
        const val TAG = "RecordInfoDialog"

    }

}
