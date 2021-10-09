package com.gee12.mytetroid.views.dialogs

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.gee12.htmlwysiwygeditor.Dialogs.AskDialogBuilder
import com.gee12.mytetroid.R
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory

/**
 * Диалог информации о ветке.
 */
class NodeInfoDialog(
    val node: TetroidNode?
) : TetroidDialogFragment() {

    private lateinit var viewModel: StorageViewModel


    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = (node != null && !node.isNonCryptedOrDecrypted)

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        viewModel = ViewModelProvider(requireActivity(), TetroidViewModelFactory(requireActivity().application))
            .get(StorageViewModel::class.java)

        val builder = AskDialogBuilder.create(context, R.layout.dialog_node_info)
        builder.setPositiveButton(R.string.answer_ok, null)
        builder.setTitle(node?.name)

        val view = builder.view
        (view.findViewById<View>(R.id.text_view_id) as TextView).text = node?.id
        (view.findViewById<View>(R.id.text_view_crypted) as TextView).setText(
            if (node?.isCrypted == true) R.string.answer_yes else R.string.answer_no)
        val nodesRecords: IntArray = viewModel.nodesInteractor.getNodesRecordsCount(node!!)
        if (nodesRecords != null) {
            (view.findViewById<View>(R.id.text_view_nodes) as TextView).text = nodesRecords[0].toString()
            (view.findViewById<View>(R.id.text_view_records) as TextView).text = nodesRecords[1].toString()
        }

        return builder.create()
    }

    companion object {
        const val TAG = "NodeInfoDialog"

    }
}
