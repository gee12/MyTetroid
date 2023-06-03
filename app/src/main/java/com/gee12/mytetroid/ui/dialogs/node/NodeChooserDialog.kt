package com.gee12.mytetroid.ui.dialogs.node

import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.manager.ScanManager
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.storage.StorageViewModel
import com.gee12.mytetroid.ui.node.NodesListAdapter
import com.gee12.mytetroid.ui.dialogs.TetroidStorageDialogFragment
import pl.openrnd.multilevellistview.MultiLevelListView

/**
 * Диалог выбора ветки.
 */
class NodeChooserDialog(
    val node: TetroidNode?,
    val canCrypted: Boolean, // разрешены ли зашифрованные ветки
    val canDecrypted: Boolean, // разрешены ли уже расшифрованные ветки
    val rootOnly: Boolean, // разрешены ли только ветки в корне дерева
    override var storageId: Int? = null,
    val onApply: (TetroidNode) -> Unit,
    val onProblem: (ProblemType) -> Unit,
) : TetroidStorageDialogFragment<StorageViewModel>() {

    enum class ProblemType {
        LOAD_STORAGE,
        LOAD_ALL_NODES,
    }

    private lateinit var adapter: NodesListAdapter
    private lateinit var searchView: SearchView
    private lateinit var tvNoticeBottom: TextView


    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    fun isStorageLoaded(): Boolean {
        // проверяем загружено ли хранилище
        if (!viewModel.isStorageLoaded()) {
            onProblem(ProblemType.LOAD_STORAGE)
            return false
        }
        // проверяем загружены ли все ветки
        if (viewModel.isLoadedFavoritesOnly()) {
            onProblem(ProblemType.LOAD_ALL_NODES)
            return false
        }
        return true
    }

    override fun getLayoutResourceId() = R.layout.dialog_nodes

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        dialog.setTitle(R.string.title_choose_node)

        adapter = NodesListAdapter(
            context = requireContext(),
            isHighlightCryptedNodes = viewModel.settingsManager.isHighlightCryptedNodes(),
            highlightColor = viewModel.settingsManager.highlightAttachColor(),
            onClick = { node, _ ->
                onSelectNode(node)
            },
            onLongClick = { _, node, _ ->
                onSelectNode(node)
                true
            },
        )

        // список веток
        val listView = dialogView.findViewById<MultiLevelListView>(R.id.list_view_nodes)
        listView.setAdapter(adapter)

        // уведомление
        tvNoticeBottom = dialogView.findViewById(R.id.text_view_notice_bottom)

        // строка поиска
        val tvEmpty = dialogView.findViewById<TextView>(R.id.nodes_text_view_empty)
        searchView = dialogView.findViewById(R.id.search_view_nodes) ?: return
        searchView.isIconified = false
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            private fun searchNodes(query: String) {
                if (TextUtils.isEmpty(query)) {
                    adapter.setDataItems(viewModel.getRootNodes())
                    tvEmpty.visibility = View.GONE
                } else {
                    val found = ScanManager.searchInNodesNames(viewModel.getRootNodes(), query)
                    adapter.setDataItems(found)
                    if (found.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = getString(R.string.search_nodes_not_found_mask, query)
                    } else {
                        tvEmpty.visibility = View.GONE
                    }
                }
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                searchNodes(query)
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                searchNodes(query)
                return true
            }
        })

        // Catch event on [x] button inside search view
//        int searchCloseButtonId = searchView.getContext().getResources()
//                .getIdentifier("app:id/search_close_btn", null, null);
//        AppCompatImageView closeButton = searchView.findViewById(searchCloseButtonId);
        val closeButton = searchView.findViewById<View>(androidx.appcompat.R.id.search_close_btn)
        closeButton?.setOnClickListener { searchView.setQuery("", true) }

        // обработчик результата
        setPositiveButton(R.string.answer_ok) { _, _ ->
            adapter.curNode?.also {
                onApply(it)
            }
        }
        setNegativeButton(R.string.answer_cancel)
    }

    override fun onStorageInited(storage: TetroidStorage) {
        // проверяем уже после загрузки хранилища
        if (!isStorageLoaded()) {
            dismiss()
            return
        }

        adapter.curNode = node
        adapter.setDataItems(viewModel.getRootNodes())

        showKeyboard(searchView)
    }

    private fun onSelectNode(node: TetroidNode) {
        val okButton = getPositiveButton()
        val isCryptedWarning = !canCrypted && node.isCrypted && !node.isDecrypted
        val isDecryptedWarning = !canDecrypted && node.isCrypted && node.isDecrypted
        val isNotRootWarning = rootOnly && node.level > 0

        if (isCryptedWarning || isDecryptedWarning || isNotRootWarning) {
            var mes = when {
                isCryptedWarning -> getString(R.string.mes_select_non_encrypted_node)
                isDecryptedWarning -> getString(R.string.mes_select_decrypted_node)
                else -> null
            }
            if (isNotRootWarning) {
                mes = (mes?.plus("\n").orEmpty()) + getString(R.string.mes_select_first_level_node)
            }
            tvNoticeBottom.text = mes
            tvNoticeBottom.visibility = View.VISIBLE
            okButton?.isEnabled = false
        } else {
            tvNoticeBottom.visibility = View.GONE
            okButton?.isEnabled = true
        }
        adapter.curNode = node
        adapter.notifyDataSetChanged()
        hideKeyboard(dialogView)
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        getPositiveButton()?.isEnabled = adapter.curNode != null
    }

    companion object {
        const val TAG = "NodeChooserDialog"
    }

}