package com.gee12.mytetroid.views.dialogs

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.gee12.htmlwysiwygeditor.Dialogs.AskDialogBuilder
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants.StorageEvents
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.utils.Utils
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory
import java.util.*

/**
 * Диалог информации о записи.
 */
class RecordInfoDialog(
    val record: TetroidRecord?
) : TetroidDialogFragment() {

    private lateinit var viewModel: StorageViewModel
    private lateinit var dialogView: View


    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = (record != null && record.isNonCryptedOrDecrypted)

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        initViewModel()

        val builder = AskDialogBuilder.create(context, R.layout.dialog_record_info)
        builder.setPositiveButton(R.string.answer_ok, null)
        builder.setTitle(record?.name)

        dialogView = builder.view
        return builder.create()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(requireActivity(), TetroidViewModelFactory(requireActivity().application))
            .get(StorageViewModel::class.java)

        viewModel.storageEvent.observe(this, { (state, data) -> onStorageEvent(state, data) })

        viewModel.initStorageFromLastStorageId()
    }

    private fun onStorageEvent(event: StorageEvents?, data: Any?) {
        when (event) {
            StorageEvents.Inited -> onStorageInited()
        }
    }

    private fun onStorageInited() {
        val view = dialogView

        (view.findViewById<View>(R.id.text_view_id) as TextView).text = record?.id
        val tvNode = view.findViewById<View>(R.id.text_view_node) as TextView
        when {
            viewModel.isLoadedFavoritesOnly() -> {
                tvNode.setText(R.string.hint_load_all_nodes)
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
        (view.findViewById<View>(R.id.text_view_crypted) as TextView).setText(
            if (record?.isCrypted == true) R.string.answer_yes else R.string.answer_no
        )
        val dateFormat = getString(R.string.full_date_format_string)
        val created = record?.created
        (view.findViewById<View>(R.id.text_view_created) as TextView).text =
            if (created != null) Utils.dateToString(created, dateFormat) else "-"

        if (App.isFullVersion()) {
            view.findViewById<View>(R.id.table_row_edited).visibility = View.VISIBLE
            val edited: Date? = viewModel.recordsInteractor.getEditedDate(requireContext(), record!!)
            (view.findViewById<View>(R.id.text_view_edited) as TextView).text =
                if (edited != null) Utils.dateToString(edited, dateFormat) else "-"
        }
        val path: String = viewModel.getPathToRecordFolder(record!!)
        (view.findViewById<View>(R.id.text_view_path) as TextView).text = path
        var size = viewModel.recordsInteractor.getRecordFolderSize(requireContext(), record)
        val tvSize = view.findViewById<TextView>(R.id.text_view_size)
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
