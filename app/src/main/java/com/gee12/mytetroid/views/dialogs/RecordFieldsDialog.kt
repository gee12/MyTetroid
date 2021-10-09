package com.gee12.mytetroid.views.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.gee12.htmlwysiwygeditor.Dialogs.AskDialogBuilder
import com.gee12.mytetroid.App
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.addAfterTextChangedListener
import com.gee12.mytetroid.data.TetroidXml
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory
import com.gee12.mytetroid.views.Message
import java.util.*

/**
 * Диалог создания/изменения записи.
 */
class RecordFieldsDialog(
    val record: TetroidRecord?,
    val chooseNode: Boolean,
    val node: TetroidNode?,
    val callback: IResult
) : TetroidDialogFragment() {

    interface IResult {
        fun onApply(name: String, tags: String, author: String, url: String, node: TetroidNode?, isFavor: Boolean)
    }

    private lateinit var viewModel: StorageViewModel

    private lateinit var etName: EditText

    private var recordNode: TetroidNode? = null
    private lateinit var nodeCallback: NodeChooserDialog.Result


    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        viewModel = ViewModelProvider(requireActivity(), TetroidViewModelFactory(requireActivity().application))
            .get(StorageViewModel::class.java)

        val builder = AskDialogBuilder.create(context, R.layout.dialog_record)

        val view: View = builder.view
        etName = view.findViewById(R.id.edit_text_name)
        val etAuthor = view.findViewById<EditText>(R.id.edit_text_author)
        val etUrl = view.findViewById<EditText>(R.id.edit_text_url)
        val etTags = view.findViewById<EditText>(R.id.edit_text_tags)
        val layoutNode = view.findViewById<RelativeLayout>(R.id.layout_node)
        val etNode = view.findViewById<EditText>(R.id.edit_text_node)
        val bNode = view.findViewById<ImageButton>(R.id.button_node)
        val ctvFavor = view.findViewById<CheckedTextView>(R.id.check_box_favor)

        if (BuildConfig.DEBUG && record == null) {
            val rand = Random()
            val num = Math.abs(rand.nextInt())
            etName.setText("record $num")
            etAuthor.setText("author $num")
            etUrl.setText("http://url$num.com")
            etTags.setText("new record , tag $num")
        }

        val curRecordNode = record?.node
        recordNode = if (curRecordNode != null && curRecordNode !== TetroidXml.ROOT_NODE) curRecordNode
        else node ?: viewModel.quicklyNode
        if (record != null) {
            etName.setText(record.name)
            etAuthor.setText(record.author)
            etUrl.setText(record.url)
            val tagsString = record.tagsString
            etTags.setText(tagsString)
        }
        if (chooseNode) {
            layoutNode.visibility = View.VISIBLE
            etNode.setText(if (recordNode != null) recordNode?.name else getString(R.string.title_select_node))
            etNode.inputType = InputType.TYPE_NULL
        }
        if (App.isFullVersion()) {
            ctvFavor.visibility = View.VISIBLE
            ctvFavor.isChecked = record != null && record.isFavorite
            ctvFavor.setOnClickListener { ctvFavor.isChecked = !ctvFavor.isChecked }
        }

        val dialog = builder.create()

        // диалог выбора ветки
        nodeCallback = object : NodeChooserDialog.Result() {
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
        if (chooseNode) {
            val clickListener = View.OnClickListener { v: View? ->
//                NodeDialogs.createNodeChooserDialog(
//                    context,
//                    if (nodeCallback.selectedNode != null) nodeCallback.selectedNode else recordNode,
//                    false, true, false, nodeCallback
//                )
                NodeChooserDialog(
                    if (nodeCallback.selectedNode != null) nodeCallback.selectedNode else recordNode,
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
                etTags.text.toString(),
                etAuthor.text.toString(),
                etUrl.text.toString(),
                if (chooseNode && nodeCallback.selectedNode != null) nodeCallback.selectedNode else recordNode,
                ctvFavor.isChecked
            )
        }
        dialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, getString(R.string.answer_cancel)
        ) { dialog1: DialogInterface, _: Int -> dialog1.cancel() }

        // обработчик отображения
        dialog.setOnShowListener {
            // получаем okButton уже после вызова show()
            if (TextUtils.isEmpty(etName.text) || recordNode == null) {
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
        etName.addAfterTextChangedListener { s ->
            val curNode = if (nodeCallback.selectedNode != null) nodeCallback.selectedNode else recordNode
            okButton?.isEnabled = !TextUtils.isEmpty(s) && curNode != null
        }

    }

    companion object {
        const val TAG = "RecordFieldsDialog"

//        fun newInstance(): RecordFieldsDialog {
//            return RecordFieldsDialog()
//        }
    }
}
