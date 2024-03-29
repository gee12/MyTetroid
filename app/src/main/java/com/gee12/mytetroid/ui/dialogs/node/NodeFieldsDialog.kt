package com.gee12.mytetroid.ui.dialogs.node

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.addAfterTextChangedListener
import com.gee12.mytetroid.common.extensions.setSelectionAtEnd
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.ui.dialogs.TetroidStorageDialogFragment
import com.gee12.mytetroid.ui.storage.StorageViewModel
import java.util.*
import kotlin.math.abs

/**
 * Диалог создания/изменения ветки.
 */
class NodeFieldsDialog(
    private val node: TetroidNode?,
    private val chooseParent: Boolean,
    override var storageId: Int?,
    private val onApply: (name: String, parentNode: TetroidNode) -> Unit,
) : TetroidStorageDialogFragment<StorageViewModel>() {

    private lateinit var etName: EditText


    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_node

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        etName = view.findViewById(R.id.edit_text_name)
        val nodeLayout = view.findViewById<RelativeLayout>(R.id.layout_node)
        val etNode = view.findViewById<EditText>(R.id.edit_text_node)
        val bNode = view.findViewById<ImageButton>(R.id.button_node)

        @SuppressLint("SetTextI18n")
        if (BuildConfig.DEBUG && node == null) {
            val rand = Random()
            val num = abs(rand.nextInt())
            etName.setText("node $num")
        }

        if (node != null) {
            etName.setText(node.name)
        }

        val parentNode = if (node != null && node != viewModel.getRootNode()) {
            node.parentNode
        } else {
            viewModel.getRootNode()
        }

        //: NodesManager.getQuicklyNode();
        if (chooseParent) {
            nodeLayout.visibility = View.VISIBLE
            etNode.setText(if (parentNode != null) parentNode.name else getString(R.string.title_select_node))
            etNode.inputType = InputType.TYPE_NULL
        } else {
            nodeLayout.visibility = View.GONE
        }

        // диалог выбора ветки
        var selectedNode: TetroidNode? = null
        if (chooseParent) {
            val clickListener = View.OnClickListener {
                NodeChooserDialog(
                    node = if (selectedNode != null) selectedNode else parentNode,
                    canCrypted = false,
                    canDecrypted = true,
                    rootOnly = false,
                    storageId = storageId,
                    onApply = { node ->
                        selectedNode = node
                        etNode.setText(node.name)
                        getPositiveButton()?.isEnabled = etName.text.isNotEmpty()
                    },
                    onProblem = { code ->
                        when (code) {
                            NodeChooserDialog.ProblemType.LOAD_STORAGE -> {
                                viewModel.showMessage(getString(R.string.log_storage_need_load), LogType.WARNING)
                            }
                            NodeChooserDialog.ProblemType.LOAD_ALL_NODES -> {
                                viewModel.showMessage(getString(R.string.log_all_nodes_need_load), LogType.WARNING)
                            }
                        }
                    },
                ).showIfPossible(parentFragmentManager)
            }
            etNode.setOnClickListener(clickListener)
            bNode.setOnClickListener(clickListener)
        }

        // кнопки результата
        setPositiveButton(R.string.answer_ok) { _: DialogInterface?, _: Int ->
            onApply(
                etName.text.toString(),
                if (chooseParent && selectedNode != null) selectedNode!! else parentNode
            )
        }
        setNegativeButton(R.string.answer_cancel)

        etName.addAfterTextChangedListener { checkPositiveButtonIsEnabled() }

        etName.setSelectionAtEnd()
        showKeyboard(etName)
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        checkPositiveButtonIsEnabled()
    }

    private fun checkPositiveButtonIsEnabled() {
        getPositiveButton()?.isEnabled = etName.text.isNotEmpty()
    }

    companion object {
        const val TAG = "NodeFieldsDialog"

    }

}
