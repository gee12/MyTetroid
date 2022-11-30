package com.gee12.mytetroid.ui.dialogs.record

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.ui.dialogs.TetroidDialogFragment
import org.koin.java.KoinJavaComponent.get
import java.util.*

/**
 * Диалог информации о записи.
 */
class RecordInfoDialog(
    val record: TetroidRecord?,
    override var storageId: Int?
) : TetroidDialogFragment<StorageViewModel>() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = (record != null && record.isNonCryptedOrDecrypted)

    override fun getLayoutResourceId() = R.layout.dialog_record_info

    override fun createViewModel() {
        this.viewModel = get(StorageViewModel::class.java)
    }

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(record?.name)
        setPositiveButton(R.string.answer_ok)
    }

    override fun onStorageInited(storage: TetroidStorage) {
        (dialogView.findViewById<View>(R.id.text_view_id) as TextView).text = record?.id
        val tvNode = dialogView.findViewById<View>(R.id.text_view_node) as TextView
        when {
            viewModel.isLoadedFavoritesOnly() -> {
                tvNode.setText(R.string.hint_need_load_all_nodes)
                tvNode.setTextColor(Color.LTGRAY)
            }
            record?.node != null -> {
                tvNode.text = record.node.name
            }
            else -> {
                tvNode.setText(R.string.hint_error)
                tvNode.setTextColor(Color.LTGRAY)
            }
        }
        (dialogView.findViewById<View>(R.id.text_view_crypted) as TextView).setText(
            if (record?.isCrypted == true) R.string.answer_yes else R.string.answer_no
        )
        val dateFormat = getString(R.string.full_date_format_string)
        val created = record?.created
        (dialogView.findViewById<View>(R.id.text_view_created) as TextView).text =
            if (created != null) Utils.dateToString(created, dateFormat) else "-"

        if (viewModel.appBuildHelper.isFullVersion()) {
            dialogView.findViewById<View>(R.id.table_row_edited).visibility = View.VISIBLE
            val edited: Date? = viewModel.recordsInteractor.getEditedDate(requireContext(), record!!)
            (dialogView.findViewById<View>(R.id.text_view_edited) as TextView).text =
                if (edited != null) Utils.dateToString(edited, dateFormat) else "-"
        }
        val path: String = viewModel.getPathToRecordFolder(record!!)
        (dialogView.findViewById<View>(R.id.text_view_path) as TextView).text = path
        var size = viewModel.recordsInteractor.getRecordFolderSize(requireContext(), record)
        val tvSize = dialogView.findViewById<TextView>(R.id.text_view_size)
        if (size == null) {
            size = requireContext().getString(R.string.title_folder_is_missing)
            tvSize.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorDarkRed))
        }
        tvSize.text = size
    }

    companion object {
        const val TAG = "RecordInfoDialog"

    }

}