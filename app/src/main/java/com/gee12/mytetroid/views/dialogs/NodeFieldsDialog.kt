package com.gee12.mytetroid.views.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.gee12.htmlwysiwygeditor.Dialogs
import com.gee12.htmlwysiwygeditor.Dialogs.AskDialogBuilder
import com.gee12.mytetroid.App
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.addAfterTextChangedListener
import com.gee12.mytetroid.data.TetroidXml
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.viewmodels.factory.StorageViewModelFactory
import com.gee12.mytetroid.views.Message
import com.gee12.mytetroid.views.dialogs.NodeDialogs.INodeChooserResult
import com.gee12.mytetroid.views.dialogs.NodeDialogs.NodeChooserResult
import java.util.*

/**
 * Диалог создания/изменения ветки.
 */
class NodeFieldsDialog(
    val node: TetroidNode?,
    val chooseParent: Boolean,
    val callback: IResult
) : TetroidDialogFragment() {

    interface IResult {
        fun onApply(name: String, parent: TetroidNode)
    }

    private lateinit var viewModel: StorageViewModel

    private lateinit var etName: EditText


    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        viewModel = ViewModelProvider(this, StorageViewModelFactory(requireActivity().application))
            .get(StorageViewModel::class.java)

        val builder = AskDialogBuilder.create(context, R.layout.dialog_node)

        val view = builder.view
        etName = view.findViewById<EditText>(R.id.edit_text_name)
        val nodeLayout = view.findViewById<RelativeLayout>(R.id.layout_node)
        val etNode = view.findViewById<EditText>(R.id.edit_text_node)
        val bNode = view.findViewById<ImageButton>(R.id.button_node)

        if (BuildConfig.DEBUG && node == null) {
            val rand = Random()
            val num = Math.abs(rand.nextInt())
            etName.setText("node $num")
        }

        if (node != null) {
            etName.setText(node.name)
        }

        val parentNode = if (node != null) {
            if (node !== TetroidXml.ROOT_NODE) node.parentNode else TetroidXml.ROOT_NODE
        } else {
            TetroidXml.ROOT_NODE
        }
        //: NodesManager.getQuicklyNode();
        //: NodesManager.getQuicklyNode();
        if (chooseParent) {
            nodeLayout.visibility = View.VISIBLE
            etNode.setText(if (parentNode != null) parentNode.name else getString(R.string.title_select_node))
            etNode.inputType = InputType.TYPE_NULL
        } else {
            nodeLayout.visibility = View.GONE
        }

        val dialog = builder.create()

        // диалог выбора ветки
        val nodeCallback = object : NodeChooserDialog.Result() {
            override fun onApply(node: TetroidNode?) {
                selectedNode = node
                if (node != null) {
                    etNode.setText(node.name)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !TextUtils.isEmpty(etName.text)
                }
            }

            override fun onProblem(code: Int) {
                when (code) {
                    NodeChooserDialog.IResult.LOAD_STORAGE -> {
                        Message.show(context, getString(R.string.log_storage_need_load), Toast.LENGTH_LONG)
                    }
                    NodeChooserDialog.IResult.LOAD_ALL_NODES -> {
                        Message.show(context, getString(R.string.log_all_nodes_need_load), Toast.LENGTH_LONG)
                    }
                }
            }
        }
        if (chooseParent) {
            val clickListener = View.OnClickListener {
//                NodeDialogs.createNodeChooserDialog(
//                    context,
//                    if (nodeCallback.selectedNode != null) nodeCallback.selectedNode else parentNode,
//                    false, true, false, nodeCallback
//                )
                NodeChooserDialog(
                    if (nodeCallback.selectedNode != null) nodeCallback.selectedNode else parentNode,
                    false,
                    true,
                    false,
                    nodeCallback
                ).showIfPossible(parentFragmentManager)
            }
            etNode.setOnClickListener(clickListener)
            bNode.setOnClickListener(clickListener)
        }

        // кнопки результата
        dialog.setButton(
            AlertDialog.BUTTON_POSITIVE, getString(R.string.answer_ok)
        ) { _: DialogInterface?, _: Int ->
            callback.onApply(
                etName.text.toString(),
                if (chooseParent && nodeCallback.selectedNode != null) nodeCallback.selectedNode!! else parentNode
            )
        }
        dialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, getString(R.string.answer_cancel)
        ) { dialog1: DialogInterface, _: Int -> dialog1.cancel() }

        dialog.setOnShowListener {
            // получаем okButton уже после вызова show()
            if (TextUtils.isEmpty(etName.text.toString())) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            }
            etName.setSelection(etName.text.length)
        }

        return dialog
    }

    // FIXME: возможно нужно onStart()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // получаем okButton тут отдельно после вызова show()
        val okButton = (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)
//        etName.addTextChangedListener(object : TextWatcher {
//            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
//            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
//            override fun afterTextChanged(s: Editable) {
//                okButton.isEnabled = !TextUtils.isEmpty(s)
//            }
//        })
        etName.addAfterTextChangedListener { s ->
            okButton?.isEnabled = !TextUtils.isEmpty(s)
        }
    }

    companion object {
        const val TAG = "NodeFieldsDialog"

    }
}
