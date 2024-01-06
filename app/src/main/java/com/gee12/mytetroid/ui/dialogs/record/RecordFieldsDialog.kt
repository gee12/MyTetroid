package com.gee12.mytetroid.ui.dialogs.record

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.addAfterTextChangedListener
import com.gee12.mytetroid.common.extensions.setSelectionAtEnd
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.model.QuicklyNode
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.dialogs.TetroidStorageDialogFragment
import com.gee12.mytetroid.ui.dialogs.node.NodeChooserDialog
import com.gee12.mytetroid.ui.storage.StorageViewModel
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.math.abs

/**
 * Диалог создания/изменения записи.
 */
class RecordFieldsDialog(
    private val record: TetroidRecord?,
    private val chooseNode: Boolean,
    private val node: TetroidNode?,
    var quicklyNode: QuicklyNode? = null,
    override var storageId: Int?,
    private val onLoadAllNodes: (() -> Unit)? = null,
    private val onApply: (name: String, tags: String, author: String, url: String, node: TetroidNode, isFavor: Boolean) -> Unit,
) : TetroidStorageDialogFragment<StorageViewModel>() {

    val resourcesProvider: IResourcesProvider by inject()

    private lateinit var layoutProgress: LinearLayout
    private lateinit var tvProgress: TextView

    private lateinit var etName: EditText
    private lateinit var etNode: EditText
    private lateinit var layoutNode: RelativeLayout

    private var recordNode: TetroidNode? = null
    private var selectedNode: TetroidNode? = null


    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_record

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        layoutProgress = dialogView.findViewById(R.id.layout_progress_bar)
        tvProgress = dialogView.findViewById(R.id.progress_text)

        etName = dialogView.findViewById(R.id.edit_text_name)
        val etAuthor = dialogView.findViewById<EditText>(R.id.edit_text_author)
        val etUrl = dialogView.findViewById<EditText>(R.id.edit_text_url)
        val etTags = dialogView.findViewById<EditText>(R.id.edit_text_tags)
        layoutNode = dialogView.findViewById(R.id.layout_node)
        etNode = dialogView.findViewById(R.id.edit_text_node)
        val bNode = dialogView.findViewById<ImageButton>(R.id.button_node)
        val ctvFavor = dialogView.findViewById<CheckedTextView>(R.id.check_box_favor)

        @SuppressLint("SetTextI18n")
        if (BuildConfig.DEBUG && record == null) {
            val rand = Random()
            val num = abs(rand.nextInt())
            etName.setText("record $num")
            etAuthor.setText("author $num")
            etUrl.setText("http://url$num.com")
            etTags.setText("new record , tag $num")
        }

        val currentRecordNode = record?.node
        recordNode = when {
            currentRecordNode != null && currentRecordNode != viewModel.getRootNode() -> {
                currentRecordNode
            }
            node != null -> {
                node
            }
            else -> {
                quicklyNode?.getLoadedNodeOrNull()
            }
        }
        if (record != null) {
            etName.setText(record.name)
            etAuthor.setText(record.author)
            etUrl.setText(record.url)
            val tagsString = record.tagsString
            etTags.setText(tagsString)
        }
        updateNodeView()
        if (viewModel.buildInfoProvider.isFullVersion()) {
            ctvFavor.visibility = View.VISIBLE
            ctvFavor.isChecked = record != null && record.isFavorite
            ctvFavor.setOnClickListener { ctvFavor.isChecked = !ctvFavor.isChecked }
        }

        // диалог выбора ветки
        if (chooseNode) {
            val clickListener = View.OnClickListener {
                NodeChooserDialog(
                    node = if (selectedNode != null) selectedNode else recordNode,
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
                                AskDialogs.showYesNoDialog(
                                    context = requireContext(),
                                    isCancelable = true,
                                    messageResId = R.string.ask_load_all_nodes,
                                    onApply = {
                                        onLoadAllNodes?.invoke()
                                    },
                                    onCancel = {},
                                )
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
            val node = getResultNode()
            node?.also {
                onApply(
                    etName.text.toString(),
                    etTags.text.toString(),
                    etAuthor.text.toString(),
                    etUrl.text.toString(),
                    node,
                    ctvFavor.isChecked
                )
            }
        }
        setNegativeButton(R.string.answer_cancel)

        etName.addAfterTextChangedListener { checkPositiveButtonIsEnabled() }

        etName.setSelectionAtEnd()
        showKeyboard(etName)
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        checkPositiveButtonIsEnabled()
    }

    fun onStorageStartLoading() {
        showProgress(getString(R.string.state_loading))
    }

    fun onStorageLoaded() {
        hideProgress()
        updateNodeView()
        checkPositiveButtonIsEnabled()
    }

    private fun updateNodeView() {
        if (chooseNode) {
            layoutNode.visibility = View.VISIBLE
            val nodeName = when {
                recordNode != null -> recordNode?.name
                selectedNode != null -> selectedNode?.name
                quicklyNode != null -> quicklyNode?.getName(resourcesProvider)
                else -> getString(R.string.title_select_node)
            }
            etNode.setText(nodeName)
            etNode.inputType = InputType.TYPE_NULL
        }
    }

    private fun checkPositiveButtonIsEnabled() {
        val node = getResultNode()
        getPositiveButton()?.isEnabled = etName.text.isNotEmpty() && node != null
    }

    private fun getResultNode(): TetroidNode? {
        return selectedNode ?: recordNode ?: quicklyNode?.getLoadedNodeOrNull()
    }

    private fun showProgress(progressText: String? = null) {
        tvProgress.text = progressText
        layoutProgress.isVisible = true
    }

    private fun hideProgress() {
        layoutProgress.isVisible = false
    }

    companion object {
        const val TAG = "RecordFieldsDialog"

    }

}
