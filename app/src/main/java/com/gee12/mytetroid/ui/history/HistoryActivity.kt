package com.gee12.mytetroid.ui.history

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.addOnSwipeRefreshListener
import com.gee12.mytetroid.common.extensions.buildIntent
import com.gee12.mytetroid.common.extensions.hideKeyboard
import com.gee12.mytetroid.common.extensions.showForcedWithIcons
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.model.HistoryEntity
import com.gee12.mytetroid.model.enums.HistorySortMode
import com.gee12.mytetroid.model.enums.TetroidObjectType
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.TetroidStorageActivity
import com.gee12.mytetroid.ui.base.views.SearchViewXListener
import com.gee12.mytetroid.ui.dialogs.AskDialogs

class HistoryActivity : TetroidStorageActivity<HistoryViewModel>() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private var searchView: SearchView? = null


    // region Create

    override fun getLayoutResourceId() = R.layout.activity_history

    override fun getViewModelClazz() = HistoryViewModel::class.java

    override fun isSingleTitle() = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recyclerView = findViewById(R.id.recycle_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(
            com.gee12.mytetroid.ui.base.views.DividerItemDecoration(
                context = recyclerView.context,
                orientation = DividerItemDecoration.VERTICAL,
                spaceBetweenRes = R.dimen.recycler_view_item_top_spacing,
            )
        )
        recyclerView.addOnSwipeRefreshListener {
            viewModel.loadData()
        }

        adapter = HistoryAdapter(
            context = this,
            resourcesProvider = resourcesProvider,
            dateTimeFormat = settingsManager.checkDateFormatString(),
        )
        adapter.onItemClickListener = { historyEntity, _ ->
            showOpenHistoryItemDialog(historyEntity)
        }
        adapter.onItemLongClickListener = { historyEntity, view ->
            showHistoryItemPopupMenu(view, historyEntity)
            true
        }
        adapter.onItemMenuClickListener = { historyEntity, view ->
            showHistoryItemPopupMenu(view, historyEntity)
        }
        recyclerView.adapter = adapter

        setSubtitle(viewModel.getStorageName())

        viewModel.loadData()
    }

    override fun createDependencyScope() {
        scopeSource = ScopeSource.current
    }

    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is HistoryEvent -> {
                onHistoryEvent(event)
            }
            else -> super.onBaseEvent(event)
        }
    }

    private fun onHistoryEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.LoadData -> {
                loadData(items = event.items)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.action) {
            Intent.ACTION_SEARCH -> {
                // обработка результата голосового поиска
                val query = intent.getStringExtra(SearchManager.QUERY)
                searchView?.setQuery(query, true)
            }
        }
    }

    // endregion Create

    private fun loadData(items: List<HistoryEntity>) {
        adapter.submitList(items)
        findViewById<TextView>(R.id.text_view_empty_history)?.isVisible = items.isEmpty()
    }

    private fun showOpenHistoryItemDialog(historyEntity: HistoryEntity) {
        val typeName = historyEntity.type.getObjectTypeNameForAction()
        val objName = historyEntity.obj.name
        AskDialogs.showYesDialog(
            context = this,
            message = resourcesProvider.getString(R.string.ask_open_object_from_history_mask, typeName, objName),
            onApply = {
                finishWithResult(historyEntity)
            }
        )
    }

    private fun showDeleteHistoryItemDialog(historyEntity: HistoryEntity) {
        val typeName = historyEntity.type.getObjectTypeNameForAction()
        val objName = historyEntity.obj.name
        AskDialogs.showYesDialog(
            context = this,
            message = resourcesProvider.getString(R.string.ask_delete_object_from_history_mask, typeName, objName),
            onApply = {
                viewModel.deleteItemFromHistory(historyEntity)
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.history, menu)
        initSearchView(menu)
        // только так получилось получить view пункта меню для отображения PopupMenu
        Handler().post {
            findViewById<View>(R.id.action_sort)?.also { view ->
                view.setOnClickListener {
                    showTagsSortPopupMenu(view)
                }
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_clear_history -> {
                viewModel.clearStorageHistory()
                return true
            }
            R.id.action_search -> {

                return true
            }
            R.id.action_choice_mode -> {
                // TODO: включаем режим множественного выбора (ActionMode) для RecyclerView
                //ActionModeController(R.menu.storage_actions, ActionMode.TYPE_PRIMARY, a).startActionMode(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("NonConstantResourceId")
    fun showTagsSortPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.history_sort)

        // выделяем текущую сортировку
        val menuItemId = when (viewModel.currentSortMode) {
            HistorySortMode.DATE_ASC -> R.id.action_sort_history_date_asc
            HistorySortMode.DATE_DESC -> R.id.action_sort_history_date_desc
            HistorySortMode.NAME_ASC -> R.id.action_sort_history_name_asc
            HistorySortMode.NAME_DESC -> R.id.action_sort_history_name_desc
            HistorySortMode.TYPE_ASC -> R.id.action_sort_history_type_asc
            HistorySortMode.TYPE_DESC -> R.id.action_sort_history_type_desc
        }
        popupMenu.menu.findItem(menuItemId)?.also { menuItem ->
            val title = SpannableString(menuItem.title)
            val textColor = ContextCompat.getColor(this, R.color.text_1)
            title.setSpan(ForegroundColorSpan(textColor), 0, title.length, 0)
            title.setSpan(StyleSpan(Typeface.BOLD), 0, title.length, 0)
            menuItem.setTitle(title)
        }

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_sort_history_date_asc -> {
                    viewModel.sortData(HistorySortMode.DATE_ASC)
                    true
                }
                R.id.action_sort_history_date_desc -> {
                    viewModel.sortData(HistorySortMode.DATE_DESC)
                    true
                }
                R.id.action_sort_history_name_asc -> {
                    viewModel.sortData(HistorySortMode.NAME_ASC)
                    true
                }
                R.id.action_sort_history_name_desc -> {
                    viewModel.sortData(HistorySortMode.NAME_DESC)
                    true
                }
                R.id.action_sort_history_type_asc -> {
                    viewModel.sortData(HistorySortMode.TYPE_ASC)
                    true
                }
                R.id.action_sort_history_type_desc -> {
                    viewModel.sortData(HistorySortMode.TYPE_DESC)
                    true
                }
                else -> false
            }
        }
        (popupMenu.menu as MenuBuilder).showForcedWithIcons(view)
    }

    private fun initSearchView(menu: Menu) {
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        searchView = menu.findItem(R.id.action_search).actionView as? SearchView
        searchView?.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView?.setIconifiedByDefault(true)
        // добавлять кнопки справа в выпадающем списке предложений (suggestions), чтобы вставить выбранное
        // предложение в строку запроса для дальнейшего уточнения (изменения), а не для поиска по нему
        searchView?.isQueryRefinementEnabled = true
        object : SearchViewXListener(searchView) {
            override fun onSearchClick() {}
            override fun onQuerySubmit(query: String) {
                viewModel.filterByQuery(query, isSaveQuery = true)
                searchView?.hideKeyboard()
            }
            override fun onQueryChange(query: String) {
                viewModel.filterByQuery(query, isSaveQuery = false)
            }
            override fun onSuggestionSelectOrClick(query: String) {
                searchView?.setQuery(query, true)
                searchView?.hideKeyboard()
            }
            override fun onClose() {
                viewModel.filterByQuery(null, isSaveQuery = false)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showHistoryItemPopupMenu(anchorView: View, historyEntity: HistoryEntity) {
        val popupMenu = PopupMenu(this, anchorView)
        popupMenu.inflate(R.menu.history_context)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_open -> {
                    showOpenHistoryItemDialog(historyEntity)
                    true
                }
                R.id.action_delete -> {
                    showDeleteHistoryItemDialog(historyEntity)
                    true
                }
                else -> false
            }
        }
        (popupMenu.menu as MenuBuilder).showForcedWithIcons(anchorView)
    }

    private fun finishWithResult(historyEntity: HistoryEntity) {
        val intent = buildIntent {
            putExtra(EXTRA_OBJECT_TYPE_ID, historyEntity.type.id)
            putExtra(EXTRA_OBJECT_ID, historyEntity.obj.id)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun TetroidObjectType.getObjectTypeNameForAction() = resourcesProvider.getString(
        when (this) {
            TetroidObjectType.NONE -> R.string.enum_tetroid_object_action_none
            TetroidObjectType.RECORD -> R.string.enum_tetroid_object_action_record
            TetroidObjectType.NODE -> R.string.enum_tetroid_object_action_node
            TetroidObjectType.ATTACH -> R.string.enum_tetroid_object_action_attach
            TetroidObjectType.TAG -> R.string.enum_tetroid_object_action_tag
        }
    )

    companion object {

        const val EXTRA_OBJECT_TYPE_ID = "OBJECT_TYPE_ID"
        const val EXTRA_OBJECT_ID = "OBJECT_ID"

        fun start(activity: Activity, requestCode: Int) {
            val intent = buildIntent {
                setClass(activity, HistoryActivity::class.java)
            }
            activity.startActivityForResult(intent, requestCode)
        }
    }
}