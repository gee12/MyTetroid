package com.gee12.mytetroid.ui.dialogs.node

import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.onFailure
import com.gee12.mytetroid.common.onSuccess
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.ui.dialogs.TetroidStorageDialogFragment
import com.gee12.mytetroid.usecase.node.GetNodesAndRecordsCountUseCase
import com.gee12.mytetroid.viewmodels.StorageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Диалог информации о ветке.
 */
class NodeInfoDialog(
    val node: TetroidNode,
) : TetroidStorageDialogFragment<StorageViewModel>() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = node.isNonCryptedOrDecrypted

    override fun getLayoutResourceId() = R.layout.dialog_node_info

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(node.name)
        setPositiveButton(R.string.answer_ok)

        (view.findViewById<View>(R.id.text_view_id) as TextView).text = node.id
        (view.findViewById<View>(R.id.text_view_crypted) as TextView).setText(
            if (node.isCrypted) R.string.answer_yes else R.string.answer_no
        )

        val getNodesAndRecordsCountUseCase: GetNodesAndRecordsCountUseCase by koinScope.inject()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                getNodesAndRecordsCountUseCase.run(node)
            }.onFailure {
                viewModel.logFailure(it)
            }.onSuccess {
                view.findViewById<TextView>(R.id.text_view_nodes).text = it.nodesCount.toString()
                view.findViewById<TextView>(R.id.text_view_records).text = it.recordsCount.toString()
            }
        }
    }

    companion object {
        const val TAG = "NodeInfoDialog"

    }

}
