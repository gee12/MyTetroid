package com.gee12.mytetroid.views.dialogs.record

import android.content.DialogInterface
import android.text.InputType
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.App
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.addAfterTextChangedListener
import com.gee12.mytetroid.common.extensions.setSelectionAtEnd
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.views.dialogs.TetroidDialogFragment
import com.gee12.mytetroid.views.dialogs.node.NodeChooserDialog
import java.util.*
import kotlin.math.abs

/**
 * Диалог создания/изменения записи.
 */
class RecordFieldsDialog(
    private val record: TetroidRecord?,
    private val chooseNode: Boolean,
    private val node: TetroidNode?,
    override var storageId: Int?,
    private val callback: IResult
) : TetroidDialogFragment<StorageViewModel>() {

    interface IResult {
        fun onApply(name: String, tags: String, author: String, url: String, node: TetroidNode, isFavor: Boolean)
    }

    private lateinit var etName: EditText

    private var recordNode: TetroidNode? = null
    private lateinit var nodeCallback: NodeChooserDialog.Result


    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun getLayoutResourceId() = R.layout.dialog_record

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        etName = dialogView.findViewById(R.id.edit_text_name)
        val etAuthor = dialogView.findViewById<EditText>(R.id.edit_text_author)
        val etUrl = dialogView.findViewById<EditText>(R.id.edit_text_url)
        val etTags = dialogView.findViewById<EditText>(R.id.edit_text_tags)
        val layoutNode = dialogView.findViewById<RelativeLayout>(R.id.layout_node)
        val etNode = dialogView.findViewById<EditText>(R.id.edit_text_node)
        val bNode = dialogView.findViewById<ImageButton>(R.id.button_node)
        val ctvFavor = dialogView.findViewById<CheckedTextView>(R.id.check_box_favor)

        if (BuildConfig.DEBUG && record == null) {
            val rand = Random()
            val num = abs(rand.nextInt())
            etName.setText("record $num")
            etAuthor.setText("author $num")
            etUrl.setText("http://url$num.com")
            etTags.setText("new record , tag $num")
        }

        val curRecordNode = record?.node
        recordNode = if (curRecordNode != null && curRecordNode !== viewModel.storageDataProcessor.getRootNode()) curRecordNode
            else node
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

        // диалог выбора ветки
        nodeCallback = object : NodeChooserDialog.Result() {
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
        if (chooseNode) {
            val clickListener = View.OnClickListener {
                NodeChooserDialog(
                    node = if (nodeCallback.selectedNode != null) nodeCallback.selectedNode else recordNode,
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
            val selectedNode = if (chooseNode && nodeCallback.selectedNode != null) nodeCallback.selectedNode!! else recordNode ?: return@setPositiveButton
            callback.onApply(
                etName.text.toString(),
                etTags.text.toString(),
                etAuthor.text.toString(),
                etUrl.text.toString(),
                selectedNode,
                ctvFavor.isChecked
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
        val node = nodeCallback.selectedNode ?: recordNode
        getPositiveButton()?.isEnabled = etName.text.isNotEmpty() && node != null
    }

    companion object {
        const val TAG = "RecordFieldsDialog"

    }

}
