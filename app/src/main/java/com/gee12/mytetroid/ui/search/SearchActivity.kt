package com.gee12.mytetroid.ui.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.buildIntent
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.ui.dialogs.node.NodeChooserDialog
import com.gee12.mytetroid.model.SearchProfile
import com.gee12.mytetroid.model.enums.SearchInNodeMode
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.TetroidStorageActivity

/**
 * Аквтивность для настройки параметров глобального поиска.
 */
class SearchActivity : TetroidStorageActivity<SearchViewModel>() {

    private lateinit var etQuery: EditText
    private lateinit var cbText: CheckBox
    private lateinit var cbRecordsNames: CheckBox
    private lateinit var cbAuthor: CheckBox
    private lateinit var cbUrl: CheckBox
    private lateinit var cbTags: CheckBox
    private lateinit var cbRecordFolderNames: CheckBox
    private lateinit var cbNodes: CheckBox
    private lateinit var cbFiles: CheckBox
    private lateinit var cbIds: CheckBox
    private lateinit var spSplitToWords: Spinner
    private lateinit var spInWholeWords: Spinner
    private lateinit var spInNodeMode: Spinner
    private lateinit var etNodeName: EditText
    private lateinit var bNodeChooser: ImageButton

    override fun getLayoutResourceId() = R.layout.activity_search

    override fun getViewModelClazz() = SearchViewModel::class.java


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
        cbRecordFolderNames = findViewById(R.id.check_box_record_folder_names)
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
                val mode = SearchInNodeMode.getById(spInNodeMode.selectedItemPosition) ?: SearchInNodeMode.NONE
                viewModel.selectSearchInNodeMode(searchInNodeMode = mode)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        etQuery.setSelection(etQuery.text?.length ?: 0)

        var nodeId: String? = null

        intent.extras?.let { extras ->
            extras.getString(EXTRA_QUERY)?.let { query ->
                etQuery.setText(query)
            }
            nodeId = extras.getString(EXTRA_CURRENT_NODE_ID)
        }

        viewModel.initStorage(nodeId)
    }

    override fun createDependencyScope() {
        scopeSource = ScopeSource.current
    }

    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is SearchEvent.Init -> {
                initUiFromSearchProfile(searchProfile = event.searchProfile)
            }
            is SearchEvent.ChangeSelectedNode -> {
                onSelectedNodeChanged(node = event.node, searchInCurrentNode = event.searchInNodeMode)
            }
            is SearchEvent.Finish -> {
                setResultAndFinish(searchProfile = event.searchProfile)
            }
            else -> super.onBaseEvent(event)
        }
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

    private fun onSelectedNodeChanged(node: TetroidNode?, searchInCurrentNode: SearchInNodeMode) {
        val name = node?.name ?: resourcesProvider.getString(R.string.title_select_node)
        etNodeName.setText(name)

        val isNodeSelectionMode = searchInCurrentNode == SearchInNodeMode.IN_SELECTED_NODE
        etNodeName.isEnabled = isNodeSelectionMode
        bNodeChooser.isEnabled = isNodeSelectionMode

        initNodeChooser(node)
    }

    private fun initNodeChooser(node: TetroidNode?) {
        etNodeName.inputType = InputType.TYPE_NULL

        // диалог выбора ветки
        val clickListener = View.OnClickListener {
            NodeChooserDialog(
                node = node,
                canCrypted = false,
                canDecrypted = true,
                rootOnly = false,
                storageId = viewModel.getStorageId(),
                onApply = { node ->
                    viewModel.selectNode(node)
                },
                onProblem = { code ->
                    when (code) {
                        NodeChooserDialog.ProblemType.LOAD_STORAGE -> {
                            showMessage(getString(R.string.log_storage_need_load), LogType.ERROR)
                        }
                        NodeChooserDialog.ProblemType.LOAD_ALL_NODES -> {
                            showMessage(getString(R.string.log_all_nodes_need_load), LogType.ERROR)
                        }
                    }
                }
            ).showIfPossibleAndNeeded(supportFragmentManager)
        }
        etNodeName.setOnClickListener(clickListener)
        bNodeChooser.setOnClickListener(clickListener)
    }

    private fun initUiFromSearchProfile(searchProfile: SearchProfile) {
        etQuery.setText(searchProfile.query)
        cbText.isChecked = searchProfile.inRecordText
        cbRecordsNames.isChecked = searchProfile.inRecordName
        cbAuthor.isChecked = searchProfile.inRecordAuthor
        cbUrl.isChecked = searchProfile.inRecordUrl
        cbTags.isChecked = searchProfile.inRecordTags
        cbRecordFolderNames.isChecked = searchProfile.inRecordFolderName
        cbNodes.isChecked = searchProfile.inNodeName
        cbFiles.isChecked = searchProfile.inAttachName
        cbIds.isChecked = searchProfile.inObjectsId
        spSplitToWords.setSelection(if (searchProfile.isSplitToWords) 0 else 1)
        spInWholeWords.setSelection(if (searchProfile.isOnlyWholeWords) 0 else 1)
        spInNodeMode.setSelection(searchProfile.searchInNodeMode.id)

        onSelectedNodeChanged(
            node = searchProfile.node,
            searchInCurrentNode = searchProfile.searchInNodeMode,
        )
    }

    private fun buildSearchProfileFromUi() = SearchProfile(
        query = etQuery.text?.toString().orEmpty(),
        inRecordText = cbText.isChecked,
        inRecordName = cbRecordsNames.isChecked,
        inRecordAuthor = cbAuthor.isChecked,
        inRecordUrl = cbUrl.isChecked,
        inRecordTags = cbTags.isChecked,
        inRecordFolderName = cbRecordFolderNames.isChecked,
        inNodeName = cbNodes.isChecked,
        inAttachName = cbFiles.isChecked,
        inObjectsId = cbIds.isChecked,
        isSplitToWords = spSplitToWords.selectedItemPosition == 0,
        isOnlyWholeWords = spInWholeWords.selectedItemPosition == 0,
        searchInNodeMode = SearchInNodeMode.getById(spInNodeMode.selectedItemPosition) ?: SearchInNodeMode.NONE,
        nodeId = null,
    )

    private fun setResultAndFinish(searchProfile: SearchProfile) {
        val intent = buildIntent {
            putExtra(Constants.EXTRA_SEARCH_PROFILE, searchProfile)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.global_search, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_query_submit) {
            viewModel.checkValuesAndFinish(
                searchProfile = buildSearchProfileFromUi(),
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        private const val EXTRA_QUERY = "QUERY"
        private const val EXTRA_CURRENT_NODE_ID = "CURRENT_NODE_ID"

        fun start(activity: Activity, query: String?, currentNodeId: String?) {
            val intent = Intent(activity, SearchActivity::class.java).apply {
                if (query != null) {
                    putExtra(EXTRA_QUERY, query)
                }
                putExtra(EXTRA_CURRENT_NODE_ID, currentNodeId)
            }
            activity.startActivityForResult(intent, Constants.REQUEST_CODE_SEARCH_ACTIVITY)
        }

    }

}