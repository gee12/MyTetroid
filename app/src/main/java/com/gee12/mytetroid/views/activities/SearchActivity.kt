package com.gee12.mytetroid.views.activities

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.SettingsManager
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.views.dialogs.node.NodeChooserDialog
import com.gee12.mytetroid.model.SearchProfile
import com.gee12.mytetroid.views.Message

/**
 * Аквтивность для настройки параметров глобального поиска.
 */
class SearchActivity : TetroidActivity<StorageViewModel>() {

    private var etQuery: EditText? = null
    private var cbText: CheckBox? = null
    private var cbRecordsNames: CheckBox? = null
    private var cbAuthor: CheckBox? = null
    private var cbUrl: CheckBox? = null
    private var cbTags: CheckBox? = null
    private var cbNodes: CheckBox? = null
    private var cbFiles: CheckBox? = null
    private var cbIds: CheckBox? = null
    private var spSplitToWords: Spinner? = null
    private var spInWholeWords: Spinner? = null
    private var spInNodeMode: Spinner? = null
    private var etNodeName: EditText? = null
    private var bNodeChooser: ImageButton? = null

    //    private boolean isCurNodeNotNull;
    private var nodeId: String? = null

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun getLayoutResourceId() = R.layout.activity_search

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

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

        initSpinner(spSplitToWords, R.array.search_split_to_words)
        initSpinner(spInWholeWords, R.array.search_in_whole_words)
        initSpinner(spInNodeMode, R.array.search_in_cur_node)

        spInNodeMode?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                updateNodeChooser()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<View>(R.id.button_clear).setOnClickListener { etQuery?.setText("") }

        readSearchPrefs()

        etQuery?.setSelection(etQuery?.text?.length ?: 0)

        intent.extras?.let { extras ->
//            this.isCurNodeNotNull = extras.getBoolean(MainActivity.EXTRA_CUR_NODE_IS_NOT_NULL);
            if (SettingsManager.getSearchInNodeMode(this) == 1) {
                nodeId = extras.getString(Constants.EXTRA_CUR_NODE_ID)
            }
            extras.getString(Constants.EXTRA_QUERY)?.let { query ->
                etQuery?.setText(query)
            }
        }

        viewModel.initStorageFromLastStorageId()
    }

    override fun initViewModel() {
        super.initViewModel()
        viewModel.storageEvent.observe(this, { (state, data) -> onStorageEvent(state, data) })

    }

    override fun onStorageEvent(event: Constants.StorageEvents?, data: Any?) {
        when (event) {
            Constants.StorageEvents.Inited -> onStorageInited()
        }
    }

    private fun onStorageInited() {
        initNodeChooser()
        updateNodeChooser()
    }

    /**
     *
     */
    private fun readSearchPrefs() {
        etQuery?.setText(SettingsManager.getSearchQuery(this))
        cbText?.isChecked = SettingsManager.isSearchInText(this)
        cbRecordsNames?.isChecked = SettingsManager.isSearchInRecordsNames(this)
        cbAuthor?.isChecked = SettingsManager.isSearchInAuthor(this)
        cbUrl?.isChecked = SettingsManager.isSearchInUrl(this)
        cbTags?.isChecked = SettingsManager.isSearchInTags(this)
        cbNodes?.isChecked = SettingsManager.isSearchInNodes(this)
        cbFiles?.isChecked = SettingsManager.isSearchInFiles(this)
        cbIds?.isChecked = SettingsManager.isSearchInIds(this)
        spSplitToWords?.setSelection(if (SettingsManager.isSearchSplitToWords(this)) 0 else 1)
        spInWholeWords?.setSelection(if (SettingsManager.isSearchInWholeWords(this)) 0 else 1)
//        spInNodeMode.setSelection(SettingsManager.isSearchInCurNode(this) ? 1 : 0);
        spInNodeMode?.setSelection(SettingsManager.getSearchInNodeMode(this))
        nodeId = SettingsManager.getSearchNodeId(this)
    }

    /**
     *
     * @param spinner
     * @param arrayId
     */
    private fun initSpinner(spinner: Spinner?, arrayId: Int) {
        val adapter = ArrayAdapter.createFromResource(
            this,
            arrayId,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner?.adapter = adapter
    }

    /**
     *
     */
    private fun initNodeChooser() {
        etNodeName = findViewById(R.id.edit_text_node)
        bNodeChooser = findViewById(R.id.button_node)
        var node: TetroidNode? = null
        if (viewModel.isLoaded()) {
            if (nodeId != null) {
                node = viewModel.getNode(nodeId!!)
                if (node == null) {
                    // очищаем, если такой ветки нет
                    nodeId = null
                }
            }
        } else {
            viewModel.logError(R.string.title_storage_not_loaded, true)
            finish()
            return
        }
        etNodeName?.setText(if (node != null) node.name else getString(R.string.title_select_node))
        etNodeName?.inputType = InputType.TYPE_NULL

        // диалог выбора ветки
        val nodeCallback: NodeChooserDialog.Result = object : NodeChooserDialog.Result() {
            override fun onApply(node: TetroidNode?) {
                selectedNode = node
                if (node != null) {
                    etNodeName?.setText(node.name)
                    nodeId = node.id
                }
            }

            override fun onProblem(code: Int) {
                when (code) {
                    NodeChooserDialog.IResult.LOAD_STORAGE -> {
                        viewModel.showError(getString(R.string.log_storage_need_load))
                    }
                    NodeChooserDialog.IResult.LOAD_ALL_NODES -> {
                        viewModel.showError(getString(R.string.log_all_nodes_need_load))
                    }
                }
            }
        }
        val clickListener = View.OnClickListener {
//            NodeDialogs.createNodeChooserDialog(this,
//                    (nodeCallback.getSelectedNode() != null) ? nodeCallback.getSelectedNode() : node,
//                    false, true, false, nodeCallback);
            NodeChooserDialog(
                node = if (nodeCallback.selectedNode != null) nodeCallback.selectedNode else node,
                canCrypted = false,
                canDecrypted = true,
                rootOnly = false,
                callback = nodeCallback
            ).showIfPossible(supportFragmentManager)
        }
        etNodeName?.setOnClickListener(clickListener)
        bNodeChooser?.setOnClickListener(clickListener)
    }

    /**
     *
     */
    private fun updateNodeChooser() {
        if (spInNodeMode?.selectedItemPosition == 1) {
            intent.extras?.let { extras ->
                nodeId = extras.getString(Constants.EXTRA_CUR_NODE_ID)
            }
        }
        val isNodeSelectionMode = spInNodeMode?.selectedItemPosition == 2
        etNodeName?.isEnabled = isNodeSelectionMode
        bNodeChooser?.isEnabled = isNodeSelectionMode
    }

    /**
     *
     * @return
     */
    private fun buildSearchProfile(): SearchProfile {
        return SearchProfile(
            query = etQuery?.text?.toString() ?: "",
            inText = cbText?.isChecked ?: false,
            inRecordsNames = cbRecordsNames?.isChecked ?: false,
            inAuthor = cbAuthor?.isChecked ?: false,
            inUrl = cbUrl?.isChecked ?: false,
            inTags = cbTags?.isChecked ?: false,
            inNodes = cbNodes?.isChecked ?: false,
            inFiles = cbFiles?.isChecked ?: false,
            inIds = cbIds?.isChecked ?: false,
            isSplitToWords = spSplitToWords?.selectedItemPosition == 0,
            isOnlyWholeWords = spInWholeWords?.selectedItemPosition == 0,
            isSearchInNode = spInNodeMode?.selectedItemPosition != 0,
            nodeId = nodeId
        )
    }

    /**
     *
     */
    private fun startSearch() {
        // сохраняем параметры поиск
        saveSearchPrefs()
        // запускаем поиск и выводим результат
        val intent = Intent()
        intent.putExtra(Constants.EXTRA_SEARCH_PROFILE, buildSearchProfile())
        setResult(RESULT_OK, intent)
        finish()
    }

    /**
     *
     */
    private fun saveSearchPrefs() {
        SettingsManager.setSearchQuery(this, etQuery?.text.toString())
        SettingsManager.setSearchInText(this, cbText?.isChecked == true)
        SettingsManager.setSearchInRecordsNames(this, cbRecordsNames?.isChecked == true)
        SettingsManager.setSearchInAuthor(this, cbAuthor?.isChecked == true)
        SettingsManager.setSearchInUrl(this, cbUrl?.isChecked == true)
        SettingsManager.setSearchInTags(this, cbTags?.isChecked == true)
        SettingsManager.setSearchInNodes(this, cbNodes?.isChecked == true)
        SettingsManager.setSearchInFiles(this, cbFiles?.isChecked == true)
        SettingsManager.setSearchInIds(this, cbIds?.isChecked == true)
        SettingsManager.setSearchSplitToWords(this, spSplitToWords?.selectedItemPosition == 0)
        SettingsManager.setSearchInWholeWords(this, spInWholeWords?.selectedItemPosition == 0)
//        SettingsManager.setSearchInCurNode(this, spInCurrentNode.getSelectedItemPosition() == 1);
        SettingsManager.setSearchInNodeMode(this, spInNodeMode?.selectedItemPosition ?: 0)
        SettingsManager.setSearchNodeId(this, nodeId)
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
                TextUtils.isEmpty(etQuery?.text.toString()) -> {
                    Message.show(this, getString(R.string.title_enter_query))
                }
                spInNodeMode?.selectedItemPosition == 1 && TextUtils.isEmpty(nodeId) -> {
                    Message.show(this, getString(R.string.log_cur_node_is_not_selected))
                }
                spInNodeMode?.selectedItemPosition == 2 && TextUtils.isEmpty(nodeId) -> {
                    Message.show(this, getString(R.string.log_select_node_to_search))
                }
                else -> {
                    startSearch()
                }
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}