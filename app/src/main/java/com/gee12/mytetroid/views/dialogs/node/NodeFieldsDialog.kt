package com.gee12.mytetroid.views.dialogs.node

import android.content.DialogInterface
import android.text.InputType
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.addAfterTextChangedListener
import com.gee12.mytetroid.common.extensions.setSelectionAtEnd
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.views.dialogs.TetroidDialogFragment
import org.koin.java.KoinJavaComponent.get
import java.util.*
import kotlin.math.abs

/**
 * Диалог создания/изменения ветки.
 */
class NodeFieldsDialog(
    private val node: TetroidNode?,
    private val chooseParent: Boolean,
    override var storageId: Int?,
    private val callback: IResult
) : TetroidDialogFragment<StorageViewModel>() {

    interface IResult {
        fun onApply(name: String, parent: TetroidNode)
    }

    private lateinit var etName: EditText


    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_node

    override fun createViewModel() {
        this.viewModel = get(StorageViewModel::class.java)
    }

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        etName = view.findViewById(R.id.edit_text_name)
        val nodeLayout = view.findViewById<RelativeLayout>(R.id.layout_node)
        val etNode = view.findViewById<EditText>(R.id.edit_text_node)
        val bNode = view.findViewById<ImageButton>(R.id.button_node)

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
        val nodeCallback = object : NodeChooserDialog.Result() {
            override fun onApply(node: TetroidNode?) {
                selectedNode = node
                if (node != null) {
                    etNode.setText(node.name)
                    getPositiveButton()?.isEnabled = !TextUtils.isEmpty(etName.text)
                }
            }

            override fun onProblem(code: Int) {
                when (code) {
                    NodeChooserDialog.IResult.LOAD_STORAGE -> {
                        viewModel.showMessage(getString(R.string.log_storage_need_load), LogType.WARNING)
                    }
                    NodeChooserDialog.IResult.LOAD_ALL_NODES -> {
                        viewModel.showMessage(getString(R.string.log_all_nodes_need_load), LogType.WARNING)
                    }
                }
            }
        }
        if (chooseParent) {
            val clickListener = View.OnClickListener {
                NodeChooserDialog(
                    node = if (nodeCallback.selectedNode != null) nodeCallback.selectedNode else parentNode,
                    canCrypted = false,
                    canDecrypted = true,
                    rootOnly = false,
                    storageId = storageId,
                    callback = nodeCallback
                ).showIfPossible(parentFragmentManager)
            }
            etNode.setOnClickListener(clickListener)
            bNode.setOnClickListener(clickListener)
        }

        // кнопки результата
        setPositiveButton(R.string.answer_ok) { _: DialogInterface?, _: Int ->
            callback.onApply(
                etName.text.toString(),
                if (chooseParent && nodeCallback.selectedNode != null) nodeCallback.selectedNode!! else parentNode
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
