package com.gee12.mytetroid.views.dialogs

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.gee12.htmlwysiwygeditor.Dialogs.AskDialogBuilder
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.utils.Utils
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory

/**
 * Диалог информации о прикрепленном файле.
 */
class AttachInfoDialog(
    val attach: TetroidFile?
) : TetroidDialogFragment() {

    private lateinit var viewModel: StorageViewModel

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow(): Boolean {
        if (attach == null || !attach.isNonCryptedOrDecrypted) {
            return false
        }
        if (attach.record == null) {
            LogManager.log(context, getString(R.string.log_file_record_is_null), ILogger.Types.ERROR)
            return false
        }
        return true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        viewModel = ViewModelProvider(requireActivity(), TetroidViewModelFactory(requireActivity().application))
            .get(StorageViewModel::class.java)

        val builder = AskDialogBuilder.create(context, R.layout.dialog_attach_info)
        builder.setPositiveButton(R.string.answer_ok, null)
        builder.setTitle(attach?.name)

        val record = attach?.record

        val view = builder.view
        (view.findViewById<View>(R.id.text_view_id) as TextView).text = attach?.id
        (view.findViewById<View>(R.id.text_view_record) as TextView).text = record?.name
        (view.findViewById<View>(R.id.text_view_crypted) as TextView).setText(
            if (attach?.isCrypted == true) R.string.answer_yes else R.string.answer_no
        )
        val dateFormat = getString(R.string.full_date_format_string)

        if (App.isFullVersion()) {
            view.findViewById<View>(R.id.table_row_edited).visibility = View.VISIBLE
            val edited = viewModel.attachesInteractor.getEditedDate(requireContext(), attach!!)
            (view.findViewById<View>(R.id.text_view_edited) as TextView).text =
                if (edited != null) Utils.dateToString(edited, dateFormat) else "-"
        }
        val path: String = viewModel.getPathToRecordFolder(record!!)
        (view.findViewById<View>(R.id.text_view_path) as TextView).text = path
        var size: String = viewModel.attachesInteractor.getAttachedFileSize(requireContext(), attach!!)
        val tvSize = view.findViewById<TextView>(R.id.text_view_size)
        if (size == null) {
            size = getString(R.string.title_folder_is_missing)
            tvSize.setTextColor(ContextCompat.getColor(requireActivity().applicationContext, R.color.colorDarkRed))
        }
        tvSize.text = size

        return builder.create()
    }

    companion object {
        const val TAG = "AttachInfoDialog"
    }
}