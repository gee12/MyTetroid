package com.gee12.mytetroid.ui.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.buildIntent
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.ui.storage.StorageViewModel
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.ui.dialogs.node.NodeChooserDialog
import com.gee12.mytetroid.model.SearchProfile
import com.gee12.mytetroid.ui.base.TetroidStorageActivity
import com.gee12.mytetroid.ui.storage.StorageEvent
import kotlinx.coroutines.launch

/**
 * Аквтивность для настройки параметров глобального поиска.
 */
// TODO: создать SearchViewModel
class SearchActivity : TetroidStorageActivity<StorageViewModel>() {

    private lateinit var etQuery: EditText
    private lateinit var cbText: CheckBox
    private lateinit var cbRecordsNames: CheckBox
    private lateinit var cbAuthor: CheckBox
    private lateinit var cbUrl: CheckBox
    private lateinit var cbTags: CheckBox
    private lateinit var cbNodes: CheckBox
    private lateinit var cbFiles: CheckBox
    private lateinit var cbIds: CheckBox
    private lateinit var spSplitToWords: Spinner
    private lateinit var spInWholeWords: Spinner
    private lateinit var spInNodeMode: Spinner
    private lateinit var etNodeName: EditText
    private lateinit var bNodeChooser: ImageButton

    private var nodeId: String? = null
    private var node: TetroidNode? = null

    override fun getLayoutResourceId() = R.layout.activity_search

    override fun getViewModelClazz() = StorageViewModel::class.java


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        etQuery = findViewById(R.id.edit_text_query)
        cbText = findViewById(R.id.check_box_records_text)
        cbRecordsNames = findViewById(R.id.check_box_records_names)
        cbAuthor = findViewById(R.id.check_box_author)
        cbUrl = findViewById(R.id.check_box_url)
        cbTags = findViewById(R.id.check_box_tags)
        cbNodes = findViewById(R.id.check_box_nodes)
        cbFiles = findViewById(R.id.check_box_files)
        cbIds = findViewById(R.id.check_box_ids)
        spSplitToWords = findViewById(R.id.spinner_split_to_words)
        spInWholeWords = findViewById(R.id.spinner_in_whole_words)
        spInNodeMode = findViewById(R.id.spinner_in_cur_node)
        etNodeName = findViewById(R.id.edit_text_node)
        bNodeChooser = findViewById(R.id.button_node)

        initSpinner(spSplitToWords, R.array.search_split_to_words)
        initSpinner(spInWholeWords, R.array.search_in_whole_words)
        initSpinner(spInNodeMode, R.array.search_in_cur_node)

        spInNodeMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                updateNode()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<View>(R.id.button_clear).setOnClickListener { etQuery.setText("") }

        etQuery.setSelection(etQuery.text?.length ?: 0)

        var storageId: Int? = null

        intent.extras?.let { extras ->
            extras.getInt(Constants.EXTRA_STORAGE_ID).takeIf { it > 0 } ?.let {
                storageId = it
            }
            if (CommonSettings.getSearchInNodeMode(this) == 1) {
                nodeId = extras.getString(Constants.EXTRA_CUR_NODE_ID)
            }
            extras.getString(Constants.EXTRA_QUERY)?.let { query ->
                etQuery.setText(query)
            }
        }

        viewModel.startInitStorageFromBase(storageId ?: settingsManager.getLastStorageId())
    }

    override fun onStorageEvent(event: StorageEvent) {
        when (event) {
            is StorageEvent.Inited -> onStorageInited()
            else -> {}
        }
    }

    private fun onStorageInited() {
        updateNode()
        initNodeChooser()
        readSearchPrefs()
    }

    private fun readSearchPrefs() {
        etQuery.setText(CommonSettings.getSearchQuery(this))
        cbText.isChecked = CommonSettings.isSearchInText(this)
        cbRecordsNames.isChecked = CommonSettings.isSearchInRecordsNames(this)
        cbAuthor.isChecked = CommonSettings.isSearchInAuthor(this)
        cbUrl.isChecked = CommonSettings.isSearchInUrl(this)
        cbTags.isChecked = CommonSettings.isSearchInTags(this)
        cbNodes.isChecked = CommonSettings.isSearchInNodes(this)
        cbFiles.isChecked = CommonSettings.isSearchInFiles(this)
        cbIds.isChecked = CommonSettings.isSearchInIds(this)
        spSplitToWords.setSelection(if (CommonSettings.isSearchSplitToWords(this)) 0 else 1)
        spInWholeWords.setSelection(if (CommonSettings.isSearchInWholeWords(this)) 0 else 1)
//        spInNodeMode.setSelection(SettingsManager.isSearchInCurNode(this) ? 1 : 0);
        spInNodeMode.setSelection(CommonSettings.getSearchInNodeMode(this))
//        nodeId = SettingsManager.getSearchNodeId(this)
    }

    private fun initSpinner(spinner: Spinner, arrayId: Int) {
        val adapter = ArrayAdapter.createFromResource(
            this,
            arrayId,
            R.layout.list_item_spinner
        )
        adapter.setDropDownViewResource(R.layout.list_item_spinner_dropdown)
        spinner.adapter = adapter
    }

    private fun updateNode() {
        nodeId = when (spInNodeMode.selectedItemPosition) {
            1 -> intent.extras?.getString(Constants.EXTRA_CUR_NODE_ID)
            else -> CommonSettings.getSearchNodeId(this)
        }
        nodeId?.let {
            lifecycleScope.launch {
                node = viewModel.getNode(it)?.also { node ->
                    etNodeName.setText(node.name)
                } ?: run {
                    nodeId = null
                    etNodeName.setText(R.string.title_select_node)
                    // не отображаем сообщение, т.к. ветка могла быть из другого хранилища
                    viewModel.logWarning(getString(R.string.error_node_not_found_with_id_mask, it), false)
                    null
                }
            }
        } ?: run {
            if (spInNodeMode.selectedItemPosition == 1) {
                etNodeName.setText(R.string.title_select_node)
                viewModel.showMessage(getString(R.string.log_cur_node_is_not_selected), LogType.WARNING)
            }
        }
        val isNodeSelectionMode = spInNodeMode.selectedItemPosition == 2
        etNodeName.isEnabled = isNodeSelectionMode
        bNodeChooser.isEnabled = isNodeSelectionMode
    }

    private fun initNodeChooser() {
        etNodeName.inputType = InputType.TYPE_NULL

        // диалог выбора ветки
        var selectedNode: TetroidNode? = null
        val clickListener = View.OnClickListener {
            NodeChooserDialog(
                node = if (selectedNode != null) selectedNode else node,
                canCrypted = false,
                canDecrypted = true,
                rootOnly = false,
                storageId = viewModel.getStorageId(),
                onApply = { node ->
                    selectedNode = node
                    etNodeName.setText(node.name)
                    nodeId = node.id
                },
                onProblem = { code ->
                    when (code) {
                        NodeChooserDialog.ProblemType.LOAD_STORAGE -> {
                            viewModel.showError(getString(R.string.log_storage_need_load))
                        }
                        NodeChooserDialog.ProblemType.LOAD_ALL_NODES -> {
                            viewModel.showError(getString(R.string.log_all_nodes_need_load))
                        }
                    }
                }
            ).showIfPossible(supportFragmentManager)
        }
        etNodeName.setOnClickListener(clickListener)
        bNodeChooser.setOnClickListener(clickListener)
    }

    private fun buildSearchProfile(): SearchProfile {
        return SearchProfile(
            query = etQuery.text?.toString().orEmpty(),
            inText = cbText.isChecked,
            inRecordsNames = cbRecordsNames.isChecked,
            inAuthor = cbAuthor.isChecked,
            inUrl = cbUrl.isChecked,
            inTags = cbTags.isChecked,
            inNodes = cbNodes.isChecked,
            inFiles = cbFiles.isChecked,
            inIds = cbIds.isChecked,
            isSplitToWords = spSplitToWords.selectedItemPosition == 0,
            isOnlyWholeWords = spInWholeWords.selectedItemPosition == 0,
            isSearchInNode = spInNodeMode.selectedItemPosition != 0,
            nodeId = nodeId
        )
    }

    private fun startSearch() {
        // сохраняем параметры поиск
        saveSearchPrefs()
        // запускаем поиск и выводим результат
        val intent = buildIntent {
            putExtra(Constants.EXTRA_SEARCH_PROFILE, buildSearchProfile())
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun saveSearchPrefs() {
        CommonSettings.setSearchQuery(this, etQuery.text.toString())
        CommonSettings.setSearchInText(this, cbText.isChecked)
        CommonSettings.setSearchInRecordsNames(this, cbRecordsNames.isChecked)
        CommonSettings.setSearchInAuthor(this, cbAuthor.isChecked)
        CommonSettings.setSearchInUrl(this, cbUrl.isChecked)
        CommonSettings.setSearchInTags(this, cbTags.isChecked)
        CommonSettings.setSearchInNodes(this, cbNodes.isChecked)
        CommonSettings.setSearchInFiles(this, cbFiles.isChecked)
        CommonSettings.setSearchInIds(this, cbIds.isChecked)
        CommonSettings.setSearchSplitToWords(this, spSplitToWords.selectedItemPosition == 0)
        CommonSettings.setSearchInWholeWords(this, spInWholeWords.selectedItemPosition == 0)
//        SettingsManager.setSearchInCurNode(this, spInCurrentNode.getSelectedItemPosition() == 1);
        CommonSettings.setSearchInNodeMode(this, spInNodeMode.selectedItemPosition)
        CommonSettings.setSearchNodeId(this, nodeId)
    }

    /**
     * Обработчик создания системного меню
     * @param menu
     * @return
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.global_search, menu)
        return true
    }

    /**
     * Обработчик выбора пунктов системного меню
     * @param item
     * @return
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_query_submit) {
            when {
                TextUtils.isEmpty(etQuery.text.toString()) -> {
                    viewModel.showMessage(R.string.title_enter_query)
                }
                spInNodeMode.selectedItemPosition == 1 && TextUtils.isEmpty(nodeId) -> {
                    viewModel.showMessage(R.string.log_cur_node_is_not_selected)
                }
                spInNodeMode.selectedItemPosition == 2 && TextUtils.isEmpty(nodeId) -> {
                    viewModel.showMessage(R.string.log_select_node_to_search)
                }
                else -> {
                    startSearch()
                }
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        fun start(activity: Activity, query: String?, currentNodeId: String?, storageId: Int?) {
            val intent = Intent(activity, SearchActivity::class.java).apply {
                if (storageId != null) {
                    putExtra(Constants.EXTRA_STORAGE_ID, storageId)
                }
                if (query != null) {
                    putExtra(Constants.EXTRA_QUERY, query)
                }
                putExtra(Constants.EXTRA_CUR_NODE_ID, currentNodeId)
            }
            activity.startActivityForResult(intent, Constants.REQUEST_CODE_SEARCH_ACTIVITY)
        }

    }

}