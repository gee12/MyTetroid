package com.gee12.mytetroid.views.dialogs.node

import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.R
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.views.dialogs.TetroidDialogFragment

/**
 * Диалог информации о ветке.
 */
class NodeInfoDialog(
    val node: TetroidNode?
) : TetroidDialogFragment<StorageViewModel>() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = (node != null && node.isNonCryptedOrDecrypted)

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun getLayoutResourceId() = R.layout.dialog_node_info

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(node?.name)
        setPositiveButton(R.string.answer_ok)

        (view.findViewById<View>(R.id.text_view_id) as TextView).text = node?.id
        (view.findViewById<View>(R.id.text_view_crypted) as TextView).setText(
            if (node?.isCrypted == true) R.string.answer_yes else R.string.answer_no
        )
        viewModel.nodesInteractor.getNodesRecordsCount(node!!).let {
            (view.findViewById<View>(R.id.text_view_nodes) as TextView).text = it[0].toString()
            (view.findViewById<View>(R.id.text_view_records) as TextView).text = it[1].toString()
        }
    }

    companion object {
        const val TAG = "NodeInfoDialog"

    }

}
