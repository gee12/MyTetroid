package com.gee12.mytetroid.ui.main

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.commit
import androidx.viewpager.widget.PagerTabStrip
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.ICallback
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.common.utils.ViewUtils
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.manager.ClipboardManager
import com.gee12.mytetroid.domain.manager.FavoritesManager.Companion.FAVORITES_NODE
import com.gee12.mytetroid.domain.manager.ScanManager
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.model.enums.TetroidPermission
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.TetroidStorageActivity
import com.gee12.mytetroid.ui.base.VMEvent
import com.gee12.mytetroid.ui.base.views.SearchViewXListener
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.dialogs.IntentDialog
import com.gee12.mytetroid.ui.dialogs.node.NodeFieldsDialog
import com.gee12.mytetroid.ui.dialogs.node.NodeInfoDialog
import com.gee12.mytetroid.ui.dialogs.pass.PassDialogs.IPassInputResult
import com.gee12.mytetroid.ui.dialogs.pass.PassDialogs.showPasswordEnterDialog
import com.gee12.mytetroid.ui.dialogs.pin.PinCodeDialog.Companion.showDialog
import com.gee12.mytetroid.ui.dialogs.pin.PinCodeDialog.IPinInputResult
import com.gee12.mytetroid.ui.dialogs.storage.StorageDialogs
import com.gee12.mytetroid.ui.dialogs.tag.TagFieldsDialog
import com.gee12.mytetroid.ui.main.found.FoundPageFragment
import com.gee12.mytetroid.ui.tag.TagsFragment
import com.gee12.mytetroid.ui.node.NodesListAdapter
import com.gee12.mytetroid.ui.node.icon.IconsActivity
import com.gee12.mytetroid.ui.record.RecordActivity
import com.gee12.mytetroid.ui.search.SearchActivity.Companion.start
import com.gee12.mytetroid.ui.settings.SettingsActivity
import com.gee12.mytetroid.ui.storage.StorageEvent
import com.gee12.mytetroid.ui.storage.info.StorageInfoActivity.Companion.start
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.runBlocking
import lib.folderpicker.FolderPicker
import pl.openrnd.multilevellistview.*
import java.util.*

/**
 * Главная активность приложения со списком веток, записей и меток.
 */
class MainActivity : TetroidStorageActivity<MainViewModel>() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var fragmentTags: TagsFragment
    private lateinit var lvNodes: MultiLevelListView
    private lateinit var listAdapterNodes: NodesListAdapter
    private lateinit var tvNodesEmpty: TextView
    private lateinit var searchViewNodes: SearchView
    private lateinit var searchViewTags: SearchView
    private lateinit var searchViewRecords: SearchView
    private lateinit var viewPagerAdapter: MainPagerAdapter
    private lateinit var viewPager: MainViewPager
    private lateinit var titleStrip: PagerTabStrip
    private lateinit var favoritesNode: View
    private lateinit var btnLoadStorageNodes: Button
    private lateinit var btnTagsSort: View
    private lateinit var fabCreateNode: FloatingActionButton

    private var isActivityCreated = false
    private var openedDrawerBeforeLock = 0

    private val mainPage: MainPageFragment
        get() = viewPagerAdapter.mainFragment

    private val foundPage: FoundPageFragment
        get() = viewPagerAdapter.foundFragment

    override fun getLayoutResourceId() = R.layout.activity_main

    override fun getViewModelClazz() = MainViewModel::class.java

    override fun isSingleTitle() = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(null)

        // выдвигающиеся панели
        drawerLayout = findViewById(R.id.drawer_layout)
        // задаем кнопку (стрелку) управления шторкой
        val drawerToggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        ) {
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
            }
        }
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // обработчик нажатия на экране, когда ветка не выбрана
        drawerLayout.setOnTouchListener(this)

        // страницы (главная и найдено)
        viewPagerAdapter = MainPagerAdapter(
            viewModel,
            supportFragmentManager,
            gestureDetector
        )
        viewPager = findViewById(R.id.view_pager)
        viewPager.adapter = viewPagerAdapter
//        mViewPager.setGestureDetector(mGestureDetector);
        titleStrip = viewPager.findViewById(R.id.pager_title_strip)
        setFoundPageVisibility(false)
        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(i: Int, v: Float, i1: Int) {}
            override fun onPageSelected(i: Int) {
                if (isActivityCreated) {
                    changeToolBarByPage(i)
                }
            }
            override fun onPageScrollStateChanged(i: Int) {}
        })

//        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
//        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
//        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        // ветки
        lvNodes = findViewById(R.id.list_view_nodes)
        lvNodes.setOnItemClickListener(onNodeClickListener)
        lvNodes.setOnItemLongClickListener(onNodeLongClickListener)
        val nodesFooter = (getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.list_view_empty_footer, null, false)
        lvNodes.listView.addFooterView(nodesFooter, null, false)
//        registerForContextMenu(mListViewNodes.getListView());
        tvNodesEmpty = findViewById(R.id.nodes_text_view_empty)
        fabCreateNode = findViewById(R.id.button_add_node)
        fabCreateNode.setOnClickListener { v: View? -> createNode() }
        fabCreateNode.hide()
        val nodesNavView = drawerLayout.findViewById<NavigationView>(R.id.nav_view_left)
        val nodesHeader = nodesNavView.getHeaderView(0)
        searchViewNodes = nodesHeader.findViewById(R.id.search_view_nodes)
        searchViewNodes.visibility = View.GONE
        initNodesSearchView(searchViewNodes, nodesHeader)

        // метки
        fragmentTags = TagsFragment(viewModel)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, fragmentTags)
                setReorderingAllowed(true)
            }
        }

        val tagsNavView = drawerLayout.findViewById<NavigationView>(R.id.nav_view_right)
        val vTagsHeader = tagsNavView.getHeaderView(0)
        searchViewTags = vTagsHeader.findViewById(R.id.search_view_tags)
        searchViewTags.visibility = View.GONE
        initTagsSearchView(searchViewTags, vTagsHeader)
        btnTagsSort = vTagsHeader.findViewById(R.id.button_tags_sort)
        btnTagsSort.setOnClickListener { view: View ->
            fragmentTags.showTagsSortPopupMenu(view)
        }

        // избранное
        favoritesNode = findViewById(R.id.node_favorites)
        btnLoadStorageNodes = findViewById(R.id.button_load)
        favoritesNode.visibility = View.GONE
        btnLoadStorageNodes.visibility = View.GONE
        if (viewModel.buildInfoProvider.isFullVersion()) {
            favoritesNode.setOnClickListener { 
                viewModel.showFavorites() 
            }
            val listener = View.OnClickListener { 
                viewModel.loadAllNodes(false) 
            }
            btnLoadStorageNodes.setOnClickListener(listener)
        }

        initNodesListAdapters()
    }

    override fun createDependencyScope() {
        scopeSource = ScopeSource.current
    }

    override fun createViewModel() {
        super.createViewModel()
    }

    override fun initViewModel() {
        super.initViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()

        // TODO ?
        //ScopeSource.main.scope.close()
    }

    /**
     * Обработчик события, когда создался главный фрагмент активности.
     */
    fun onMainPageCreated() {
        // принудительно запускаем создание пунктов меню уже после отработки onCreate
        super.afterOnCreate()
        isActivityCreated = true
    }

    /**
     * Обработчик события, когда создались все элементы интерфейса.
     * Вызывается из onCreateOptionsMenu(), т.к. пункты меню, судя по всему, создаются в последнюю очередь.
     */
    override fun onUICreated(isUICreated: Boolean) {
        if (isUICreated) {
            viewModel.onUICreated()
        }
    }

    /**
     * Обработчик событий UI.
     */
    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is MainEvent -> {
                onMainEvent(event)
            }
            is BaseEvent.InitUI -> {
                initUI(
                    isLoaded = event.result,
                    isOnlyFavorites = event.isLoadFavoritesOnly,
                    isOpenLastNode = event.isHandleReceivedIntent,
                    isAllNodesOpening = event.isAllNodesLoading,
                )
            }
            is BaseEvent.UpdateToolbar -> {
                updateMainToolbar(event.viewId, event.title)
            }
            BaseEvent.HandleReceivedIntent -> {
                checkReceivedIntent(receivedIntent)
            }
            is BaseEvent.Permission.Check -> {
                if (event.permission == TetroidPermission.WriteStorage) {
                    viewModel.checkWriteExtStoragePermission(this)
                }
            }
            is BaseEvent.Permission.Granted -> {
                if (event.permission == TetroidPermission.WriteStorage) {
                    viewModel.syncAndInitStorage(this)
                }
            }
            is BaseEvent.Permission.Canceled -> {}
            is BaseEvent.OpenPage -> viewPager.setCurrent(event.pageId)
            is BaseEvent.ShowMainView -> mainPage.showView(event.viewId)
            BaseEvent.ClearMainView -> mainPage.clearView()
            BaseEvent.CloseFoundView -> closeFoundFragment()
            is BaseEvent.TaskStarted -> {
                openedDrawerBeforeLock = taskMainPreExecute(event.titleResId ?: R.string.task_wait)
            }
            BaseEvent.TaskFinished -> taskMainPostExecute()
            else -> super.onBaseEvent(event)
        }
    }

    /**
     * Обработчик событий хранилища.
     */
    override fun onStorageEvent(event: StorageEvent) {
        when (event) {
            is StorageEvent.FoundInBase -> onStorageUpdated()
            is StorageEvent.Inited -> onStorageInited()
            is StorageEvent.AskBeforeClearTrashOnExit -> {
                showClearTrashDialog(event.callback)
            }
            is StorageEvent.AskOnSync.BeforeInit -> {
                showSyncRequestDialog(event.callback)
            }
            is StorageEvent.AskOnSync.BeforeExit -> {
                showSyncRequestDialog(event.callback)
            }
//            is StorageEvents.AskAfterSyncManually,
            is StorageEvent.AskOnSync.AfterInit -> {
                AskDialogs.showYesDialog(
                    context = this,
                    messageResId = if (event.result) R.string.ask_sync_success_dialog_request else R.string.ask_sync_failed_dialog_request,
                    onApply = {
                        viewModel.initStorageAndLoad()
                    },
                )
            }
            is StorageEvent.AskOnSync.AfterExit -> {
                if (!event.result) {
                    AskDialogs.showYesDialog(
                        context = this,
                        messageResId = R.string.ask_sync_failed_dialog_request,
                        onApply = {},
                    )
                }
            }
            StorageEvent.AskForSyncAfterFailureSyncOnExit -> {
                showSyncRequestDialogAfterFailureSync()
            }
            is StorageEvent.InitFailed -> {
                viewModel.showMessage(R.string.mes_storage_init_error)
                initUI(
                    isLoaded = false,
                    isOnlyFavorites = event.isOnlyFavorites,
                    isOpenLastNode = false,
                    isAllNodesOpening = false
                )
            }
            is StorageEvent.LoadOrDecrypt -> {
                viewModel.loadOrDecryptStorage(event.params)
            }
            is StorageEvent.AskPassword -> showPasswordDialog(event.callbackEvent)
            is StorageEvent.AskPinCode -> showPinCodeDialog(event.callbackEvent)
            is StorageEvent.ChangePassDirectly -> viewModel.startChangePass(
                curPass = event.curPass,
                newPass = event.newPass
            )
            StorageEvent.AskForClearStoragePass -> showNoCryptedNodesLeftDialog()
            is StorageEvent.AskForEmptyPassCheckingField -> {
                showEmptyPassCheckingFieldDialog(
                    fieldName = event.fieldName,
                    passHash = event.passHash,
                    callbackEvent = event.callbackEvent,
                )
            }
            StorageEvent.TreeChangedOutside -> showStorageTreeChangedOutsideDialog()
            StorageEvent.TreeDeletedOutside -> showStorageTreeDeletedOutsideDialog()
            else -> super.onStorageEvent(event)
        }
    }

    /**
     * Обработчик событий в главном окне приложения.
     */
    private fun onMainEvent(event: MainEvent) {
        when (event) {
            MainEvent.Migrated -> showMigrationDialog()
            is MainEvent.Node.Encrypt -> viewModel.encryptNode(event.node)
            is MainEvent.Node.DropEncrypt -> viewModel.dropEncryptNode(event.node)
            is MainEvent.Node.Show -> viewModel.showNode(event.node)
            is MainEvent.Node.Created -> onNodeCreated(event.node)
            is MainEvent.Node.Inserted -> onNodeInserted(event.node)
            is MainEvent.Node.Renamed -> onNodeRenamed(event.node)
            is MainEvent.Node.AskForDelete -> showDeleteNodeDialog(event.node)
            is MainEvent.Node.Cutted -> onNodeDeleted(event.node, true)
            is MainEvent.Node.Deleted -> onNodeDeleted(event.node, false)
            is MainEvent.Node.Reordered -> onNodeReordered(
                flatPosition = event.flatPosition,
                positionInNode = event.positionInNode,
                newPositionInNode = event.newPositionInNode,
            )
            is MainEvent.SetCurrentNode -> setCurNode(event.node)
            MainEvent.UpdateNodes -> updateNodes()
            is MainEvent.Record.Open -> openRecord(event.bundle)
            is MainEvent.Record.Deleted -> mainPage.onDeleteRecordResult(event.record)
            is MainEvent.Record.Cutted -> mainPage.onDeleteRecordResult(event.record)
            is MainEvent.ShowRecords -> {
                showRecords(
                    records = event.records,
                    viewId = event.viewId,
                    dropSearch = event.dropSearch,
                )
            }
            is MainEvent.RecordsFiltered -> {
                mainPage.onRecordsFiltered(
                    query = event.query,
                    found = event.records,
                    viewId = event.viewId
                )
            }
            is MainEvent.UpdateRecordsList -> {
                mainPage.updateRecordList(
                    records = event.records,
                    curMainViewId = event.curMainViewId
                )
            }
            is MainEvent.Tags.UpdateTags -> {
                updateTags()
            }
            is MainEvent.Tags.ReloadTags -> {
                fragmentTags.setTagsDataItems(event.tagsMap)
            }
            is MainEvent.Tags.UpdateSelectedTags -> {
                fragmentTags.updateSelectedTags(
                    selectedTags = event.selectedTags,
                    isMultiTagsMode = event.isMultiTagsMode,
                )
            }
            is MainEvent.ShowAttaches -> {
                mainPage.showAttaches(event.attaches)
            }
            is MainEvent.AttachesFiltered -> {
                mainPage.onAttachesFiltered(event.query, event.attaches)
            }
            is MainEvent.AttachDeleted -> mainPage.onDeleteAttachResult(event.attach)
            MainEvent.UpdateAttaches -> mainPage.updateAttachesList()
            is MainEvent.ReloadAttaches -> mainPage.setAttachesList(event.attaches)
            MainEvent.UpdateFavoritesTitle -> updateFavoritesTitle()
            is MainEvent.GlobalSearchStart -> showGlobalSearchActivity(event.query)
            MainEvent.GlobalResearch -> research()
            is MainEvent.GlobalSearchFinished -> {
                onGlobalSearchFinished(event.found, event.profile)
            }
            is MainEvent.AskForOperationWithoutFolder -> {
                askForOperationWithoutFolder(clipboardParams = event.clipboardParams)
            }
            is MainEvent.AskForOperationWithoutFile -> {
                askForOperationWithoutFile(clipboardParams = event.clipboardParams)
            }
            MainEvent.OpenFilePicker -> openFilePicker()
            MainEvent.OpenFolderPicker -> openFolderPicker()
            MainEvent.Exit -> finish()
        }
    }

    // region UI

    fun initUI(
        isLoaded: Boolean,
        isOnlyFavorites: Boolean,
        isOpenLastNode: Boolean,
        isAllNodesOpening: Boolean
    ) {
        val rootNodes: List<TetroidNode?> = viewModel.getRootNodes()
        val isEmpty = rootNodes.isEmpty()

        // избранные записи
        val isVisibleLoadButtons = isLoaded && isOnlyFavorites
        btnLoadStorageNodes.isVisible = isVisibleLoadButtons
        fragmentTags.setVisibleLoadStorageTags(isVisibleLoadButtons)
        ViewUtils.setFabVisibility(fabCreateNode, isLoaded && !isOnlyFavorites)
        lvNodes.isVisible = !isOnlyFavorites
        favoritesNode.isVisible = isLoaded && viewModel.buildInfoProvider.isFullVersion()
        tvNodesEmpty.isVisible = false
        if (isLoaded && viewModel.buildInfoProvider.isFullVersion()) {
            updateFavoritesTitle()
        }

        // элементы фильтра веток и меток
        searchViewNodes.isVisible = isLoaded && !isOnlyFavorites
        searchViewTags.isVisible = isLoaded && !isOnlyFavorites
        btnTagsSort.isVisible = isLoaded && !isOnlyFavorites

        // обновляем заголовок в шторке
        updateStorageNameLabel(isLoaded)
        if (isOnlyFavorites) {
            // обработка только "ветки" избранных записей
            if (isLoaded) {
                // списки записей, файлов
                mainPage.resetListAdapters()
                viewModel.showFavorites()
                // список меток
                fragmentTags.setTagsEmptyText(R.string.title_load_all_nodes)
                setListEmptyViewState(tvNodesEmpty, true, R.string.title_load_all_nodes)
            } else {
                setEmptyTextViews(R.string.title_storage_not_loaded)
            }
        } else if (isAllNodesOpening) {
            // загрузка всех записей, после того, как уже были загружены избранные записи
            resetNodesTagsListAdapters()
            if (isLoaded) {
                // список веток
                listAdapterNodes.setDataItems(rootNodes)
                setListEmptyViewState(tvNodesEmpty, isEmpty, R.string.title_nodes_is_missing)
                // списки записей, файлов
                mainPage.resetListAdapters()
                // список меток
                reloadTags()
                fragmentTags.setTagsEmptyText(R.string.log_tags_is_missing)
            } else {
                setEmptyTextViews(R.string.title_storage_not_loaded)
            }
        } else {
            resetNodesTagsListAdapters()
            if (isLoaded) {
                // выбираем ветку, выбранную в прошлый раз
                var nodesAdapterInited = false
                var nodeToSelect: TetroidNode? = null
                if (viewModel.isKeepLastNode() && !isEmpty && isOpenLastNode) {
                    val nodeId = viewModel.getLastNodeId()
                    if (nodeId != null) {
                        if (nodeId == FAVORITES_NODE.id) {
                            nodeToSelect = FAVORITES_NODE
                        } else {
                            // TODO: убрать runBlocking
                            nodeToSelect = runBlocking { viewModel.getNode(nodeId) }
                            if (nodeToSelect != null) {
                                val expandNodes = viewModel.createNodesHierarchy(nodeToSelect)
                                listAdapterNodes.setDataItems(rootNodes, expandNodes)
                                nodesAdapterInited = true
                            }
                        }
                    }
                } else {
                    // очищаем заголовок, список
                    mainPage.clearView()
                    drawerLayout.openDrawer(GravityCompat.START)
                }
                if (!nodesAdapterInited) {
                    listAdapterNodes.setDataItems(rootNodes)
                }
                if (!isEmpty) {
                    // списки записей, файлов
                    mainPage.resetListAdapters()
                    if (nodeToSelect != null) {
                        if (nodeToSelect === FAVORITES_NODE) {
                            viewModel.showFavorites()
                        } else {
                            viewModel.showNode(nodeToSelect)
                        }
                    } else {
                        mainPage.clearView()
                    }

                    // список меток
                    reloadTags()
                    fragmentTags.setTagsEmptyText(R.string.log_tags_is_missing)
                }
                setListEmptyViewState(tvNodesEmpty, isEmpty, R.string.title_nodes_is_missing)
            } else {
                mainPage.clearView()
                setEmptyTextViews(R.string.title_storage_not_loaded)
            }
        }
        updateOptionsMenu()
    }

    private fun initNodesListAdapters() {
        // список веток
        listAdapterNodes = NodesListAdapter(
            context = this,
            isHighlightCryptedNodes = viewModel.settingsManager.isHighlightCryptedNodes(),
            highlightColor = viewModel.settingsManager.highlightAttachColor(),
            onClick = { node, pos ->
                if (node.isExpandable && CommonSettings.isExpandEmptyNode(this@MainActivity)) {
                    // если у ветки есть подветки и установлена опция
                    if (node.recordsCount > 0) {
                        // и в ветке есть записи - открываем список записей
                        viewModel.showNode(node)
                    } else {
                        // иначе - разворачиваем/сворачиваем ветку
                        listAdapterNodes.toggleNodeExpand(pos)
                    }
                } else {
                    // сразу открываем список записей (даже если он пуст)
                    viewModel.showNode(node)
                }
            },
            onLongClick = { view, node, pos ->
                showNodePopupMenu(view, node, pos)
                true
            },
        )
        lvNodes.setAdapter(listAdapterNodes)
    }

    private fun resetNodesTagsListAdapters() {
        listAdapterNodes.reset()
        fragmentTags.resetListAdapters()
    }

    /**
     * Диалог с информацией об обновлении.
     */
    private fun showMigrationDialog() {
        if (BuildConfig.VERSION_CODE == Constants.SETTINGS_VERSION_CURRENT) {
            AskDialogs.showOkDialog(
                context = this,
                messageRes = R.string.mes_migration_50,
                applyResId = R.string.answer_ok,
                isCancelable = false,
                onApply = {
                    viewModel.startInitStorage()
                }
            )
        }
    }

    private fun updateStorageNameLabel(isLoaded: Boolean) {
        val title = if (isLoaded) {
            viewModel.getStorageName()
        } else {
            getString(R.string.main_header_title)
        }
        (findViewById<View>(R.id.text_view_app_name) as TextView).text = title
    }

    private fun setEmptyTextViews(@StringRes mesId: Int) {
        mainPage.setRecordsEmptyViewText(getString(mesId))
        setListEmptyViewState(tvNodesEmpty, isVisible = true, mesId)
        fragmentTags.setListEmptyViewState(isVisible = true, mesId)
        drawerLayout.closeDrawers()
    }

    private fun setListEmptyViewState(tvEmpty: TextView, isVisible: Boolean, @StringRes stringId: Int) {
        setListEmptyViewState(tvEmpty, isVisible, getString(stringId))
    }

    private fun setListEmptyViewState(tvEmpty: TextView, isVisible: Boolean, string: String) {
        tvEmpty.visibility = if (isVisible) View.VISIBLE else View.GONE
        tvEmpty.text = string
    }

    /**
     * Изменение ToolBar по текущей странице.
     */
    private fun changeToolBarByPage(curPage: Int) {
        if (curPage == Constants.PAGE_MAIN) {
            viewModel.restoreLastMainToolbarState()
        } else {
            updateMainToolbar(Constants.MAIN_VIEW_GLOBAL_FOUND, null)
        }
    }

    /**
     * Установка заголовка и подзаголовка ToolBar.
     */
    fun updateMainToolbar(viewId: Int, title: String?) {
        val newTitle = when (viewId) {
            Constants.MAIN_VIEW_GLOBAL_FOUND -> getString(R.string.title_global_search)
            Constants.MAIN_VIEW_NONE -> null
            Constants.MAIN_VIEW_NODE_RECORDS -> viewModel.getCurNodeName()
            Constants.MAIN_VIEW_TAG_RECORDS -> viewModel.getSelectedTagsNames()
            Constants.MAIN_VIEW_FAVORITES -> getString(R.string.title_favorites)
            Constants.MAIN_VIEW_RECORD_FILES -> title
            else -> title
        }
        setTitle(newTitle)
        setSubtitle(viewId)
        updateOptionsMenu()
    }

    /**
     * Установка подзаголовка активности, указывающим на тип отображаемого объекта.
     */
    private fun setSubtitle(viewId: Int) {
        if (viewId != Constants.MAIN_VIEW_GLOBAL_FOUND) {
            val titles = resources.getStringArray(R.array.view_type_titles)
            // преобразуем идентификатор view в индекс заголовка
            val titleId = viewId - 1
            if (titleId >= 0 && titleId < titles.size) {
                tvSubtitle.visibility = View.VISIBLE
                tvSubtitle.textSize = 12f
                tvSubtitle.text = if (viewId == Constants.MAIN_VIEW_TAG_RECORDS && viewModel.isMultiTagsMode) {
                    val tagsSearchMode = settingsManager.getTagsSearchMode().getStringValue(resourcesProvider)
                    "${resourcesProvider.getString(R.string.title_multiple_tags)} (${tagsSearchMode})"
                } else {
                    titles[titleId]
                }
            } else  /*if (titleId < 0)*/ {
                tvSubtitle.visibility = View.GONE
            }
        } else if (viewModel.lastSearchProfile != null) {
            setSubtitle("\"${viewModel.lastSearchProfile!!.query}\"")
        } else {
            tvSubtitle.visibility = View.GONE
        }
    }

    private fun askForOperationWithoutFolder(clipboardParams: ClipboardParams) {
        if (clipboardParams.obj is TetroidRecord) {
            val title = getString(
                when (clipboardParams.operation) {
                    LogOper.DELETE -> R.string.title_delete
                    LogOper.CUT -> R.string.title_cut
                    else -> R.string.title_insert
                }
            )
            AskDialogs.showYesNoDialog(
                context = this,
                message = getString(R.string.ask_oper_without_record_dir_mask).format(title),
                isCancelable = true,
                onApply = {
                    viewModel.doOperationWithoutDir(clipboardParams)
                },
                onCancel = {},
            )

        } else if (clipboardParams.obj is TetroidFile) {
            AskDialogs.showYesDialog(
                context = this,
                messageResId = R.string.ask_delete_record_without_dir,
                onApply = {
                    viewModel.doOperationWithoutDir(clipboardParams)
                },
            )
        }
    }

    private fun askForOperationWithoutFile(clipboardParams: ClipboardParams) {
        val oper = clipboardParams.operation
        AskDialogs.showYesNoDialog(
            context = this,
            messageResId = if (oper === LogOper.DELETE) R.string.ask_attach_delete_without_file else R.string.ask_delete_attach_without_file,
            onApply = {
                viewModel.doOperationWithoutFile(clipboardParams)
            },
            onCancel = {},
        )
    }

    // endregion UI

    // region LoadStorage

    /**
     * Обработчик события после загрузки хранилища.
     */
    override fun afterStorageLoaded(res: Boolean) {
        if (res) {
            // проверяем входящий Intent после загрузки
            checkReceivedIntent(receivedIntent)
            // запускаем отслеживание изменения структуры хранилища
            viewModel.startStorageTreeObserverIfNeeded()
        }
    }

    /**
     * Перезагрузка хранилища (при изменении пути в настройках).
     */
    private fun reinitStorage() {
        closeFoundFragment()
        mainPage.clearView()
        viewModel.startReinitStorage()
    }

    /**
     * Перезагрузка хранилища.
     */
    private fun reloadStorageAsk() {
        StorageDialogs.showReloadStorageDialog(
            context = this,
            toCreate = false,
            pathChanged = false,
            onApply = {
                reinitStorage()
            },
        )
    }

    override fun afterStorageDecrypted() {
        updateNodes()
        updateTags()
        // обновляем и записи, т.к. расшифровка могла быть вызвана из Favorites
        viewModel.updateRecordsList()
        checkReceivedIntent(receivedIntent)
    }

    /**
     * Если включен режим только избранных записей, то сначала нужно загрузить все ветки.
     */
    private fun checkIsNeedLoadAllNodes(data: Intent): Boolean {
        if (viewModel.isLoadedFavoritesOnly()) {
            receivedIntent = data
            viewModel.loadAllNodes(true)
            return true
        }
        return false
    }

    /**
     * Если какие-либо свойства хранилища были изменены.
     */
    private fun onStorageUpdated() {
        updateStorageNameLabel(viewModel.isStorageLoaded())
    }

    private fun onStorageInited() {
        closeFoundFragment()
        mainPage.clearView()
    }

    // endregion LoadStorage

    //region Sync

    private fun showSyncRequestDialog(callback: ICallback?) {
        AskDialogs.showYesNoDialog(
            context = this,
            isCancelable = false,
            messageResId = R.string.ask_start_sync_dialog_title,
            onApply = {
                viewModel.startStorageSync(this, callback)
            },
            onCancel = {
                viewModel.cancelStorageSync(callback)
            },
        )
    }

    private fun showSyncRequestDialogAfterFailureSync(/*ICallback callback*/) {
        AskDialogs.showDialog(
            context = this,
            message = getString(R.string.ask_start_sync_or_exit_dialog_title),
            isCancelable = false,
            applyResId = R.string.action_sync,
            cancelResId = R.string.action_exit,
            onApply = {
                viewModel.syncStorageAndExit(this)
            },
            onCancel = {
                viewModel.exitAfterAsks()
            },
        )
    }

    //endregion Sync

    //region Encryption

    private fun showPasswordDialog(callbackEvent: VMEvent) {
        val isSetup = !viewModel.isStorageCrypted()
        showPasswordEnterDialog(
            isSetup = isSetup,
            fragmentManager = supportFragmentManager,
            passResult = object : IPassInputResult {
                override fun applyPass(pass: String) {
                    viewModel.onPasswordEntered(pass, isSetup, callbackEvent)
                }

                override fun cancelPass() {
                    viewModel.onPasswordCanceled(isSetup, callbackEvent)
                }
            })
    }

    private fun showPinCodeDialog(callbackEvent: VMEvent) {
        showDialog(
            length = CommonSettings.getPinCodeLength(this),
            isSetup = !viewModel.isStorageCrypted(),
            fragmentManager = supportFragmentManager,
            callback = object : IPinInputResult {
                override fun onApply(pin: String): Boolean {
                    return viewModel.startCheckPinCode(pin, callbackEvent)
                }

                override fun onCancel() {}
            }
        )
    }

    //endregion Encryption

    //region Nodes

    /**
     * Установка и выделение текущей ветки.
     */
    private fun setCurNode(node: TetroidNode?) {
        listAdapterNodes.curNode = node
        listAdapterNodes.notifyDataSetChanged()
        if (viewModel.buildInfoProvider.isFullVersion()) {
            setFavorIsCurNode(node === FAVORITES_NODE)
        }
    }

    /**
     * Управление подсветкой ветки Избранное.
     */
    private fun setFavorIsCurNode(isCurNode: Boolean) {
        favoritesNode.setBackgroundColor(
            ContextCompat.getColor(
                this,
                if (isCurNode) R.color.colorCurNode else R.color.transparent
            )
        )
    }

    fun updateNodes() {
        listAdapterNodes.notifyDataSetChanged()
    }

    fun updateTags() {
        fragmentTags.updateTags()
    }

    fun reloadTags() {
        fragmentTags.setTagsDataItems(viewModel.getTagsMap())
    }

    /**
     * Обработчик клика на "конечной" ветке (без подветок).
     */
    private val onNodeClickListener: OnItemClickListener = object : OnItemClickListener {
        /**
         * Клик на конечной ветке.
         */
        override fun onItemClicked(parent: MultiLevelListView, view: View, item: Any, itemInfo: ItemInfo) {
            viewModel.showNode(item as TetroidNode)
        }

        /**
         * Клик на родительской ветке.
         */
        override fun onGroupItemClicked(parent: MultiLevelListView, view: View, item: Any, itemInfo: ItemInfo) {
/*          // это событие обрабатывается с помощью OnNodeHeaderClickListener, чтобы разделить клик
            // на заголовке и на стрелке раскрытия/закрытия ветки
            TetroidNode node = (TetroidNode) item;
            if (!node.isNonCryptedOrDecrypted()) {
                decryptStorage(node);
                // как остановить дальнейшее выполнение, чтобы не стабатывал Expander?
//                return;
            }*/
        }
    }

    /**
     * Обработчик долгого клика на ветках.
     */
    private val onNodeLongClickListener = OnItemLongClickListener { 
            parent: MultiLevelListView, view: View, item: Any?, _: ItemInfo?, pos: Int ->
        if (parent != lvNodes) return@OnItemLongClickListener
        val node = item as TetroidNode?
        if (node == null) {
            viewModel.logError(getString(R.string.log_get_item_is_null), true)
            return@OnItemLongClickListener
        }
        showNodePopupMenu(view, node, pos)
    }

    // endregion Nodes

    // region Node

    /**
     * Создание ветки.
     * @param parentNode Родительская ветка
     * @param pos        Позиция в списке родительской ветки
     * @param isSubNode  Если true, значит как подветка, иначе рядом с выделенной веткой
     */
    private fun createNode(parentNode: TetroidNode, pos: Int, isSubNode: Boolean) {
        NodeFieldsDialog(
            node = null,
            chooseParent = false,
            storageId = viewModel.getStorageId(),
            onApply = { name: String, _: TetroidNode ->
                val trueParentNode = if (isSubNode) parentNode else parentNode.parentNode
                viewModel.createNode(name, trueParentNode)
            }
        ).showIfPossible(supportFragmentManager)
    }

    /**
     * Создание ветки.
     */
    private fun createNode() {
        NodeFieldsDialog(
            node = null,
            chooseParent = true,
            storageId = viewModel.getStorageId(),
            onApply = { name: String, parentNode: TetroidNode ->
                viewModel.createNode(name, parentNode)
            }
        ).showIfPossible(supportFragmentManager)
    }

    private fun onNodeCreated(node: TetroidNode) {
        if (listAdapterNodes.addItem(node.parentNode)) {
            viewModel.logOperRes(LogObj.NODE, LogOper.CREATE, node, false)
            viewModel.showNode(node)
        } else {
            viewModel.logError(getString(R.string.log_create_node_list_error), true)
        }
    }

    private fun showNodeInfoDialog(node: TetroidNode) {
        NodeInfoDialog(
            node = node,
        ).showIfPossible(supportFragmentManager)
    }

    /**
     * Копирование ссылки на ветку в буфер обмена.
     */
    private fun copyNodeLink(node: TetroidNode?) {
        if (node != null) {
            val url = node.createUrl()
            Utils.writeToClipboard(this, getString(R.string.link_to_node), url)
            viewModel.log(getString(R.string.title_link_was_copied) + url, true)
        } else {
            viewModel.log(getString(R.string.log_get_item_is_null), true)
        }
    }

    /**
     * Переименование ветки.
     */
    private fun renameNode(node: TetroidNode) {
        NodeFieldsDialog(
            node = node,
            chooseParent = false,
            storageId = viewModel.getStorageId(),
            onApply = { name: String, _: TetroidNode ->
                viewModel.renameNode(node, name)
            }
        ).showIfPossible(supportFragmentManager)
    }

    private fun onNodeRenamed(node: TetroidNode) {
        if (viewModel.curNode == node) {
            title = node.name
        }
    }

    private fun setNodeIcon(node: TetroidNode) {
        IconsActivity.start(this, node, Constants.REQUEST_CODE_NODE_ICON)
    }

    /**
     * Удаление ветки.
     */
    private fun showDeleteNodeDialog(node: TetroidNode) {
        val isQuicklyNode = node.id == viewModel.getQuicklyNodeId()
        val mesId = if (isQuicklyNode) R.string.ask_quickly_node_delete_mask else R.string.ask_node_delete_mask
        AskDialogs.showYesDialog(
            context = this,
            message = getString(mesId, node.name),
            onApply = {
                viewModel.deleteOrCutNode(node, isCutting = false)
            }
        )
    }

    private fun onNodeDeleted(node: TetroidNode, isCutted: Boolean) {
        // удаляем элемент внутри списка
        if (listAdapterNodes.deleteItem(node)) {
            viewModel.logOperRes(LogObj.NODE, if (isCutted) LogOper.CUT else LogOper.DELETE)
        } else {
            viewModel.logError(getString(R.string.log_node_delete_list_error), true)
        }
    }

    private fun onNodeReordered(flatPosition: Int, positionInNode: Int, newPositionInNode: Int) {
        if (listAdapterNodes.swapItems(flatPosition, positionInNode, newPositionInNode)) {
            viewModel.logOperRes(LogObj.NODE, LogOper.REORDER)
        } else {
            viewModel.logError(getString(R.string.log_node_move_list_error), show = true)
        }
    }

    /**
     * Развернуть все подветки у ветки.
     */
    private fun expandSubNodes(pos: Int) {
        listAdapterNodes.extendNodeSubnodes(pos, NestType.MULTIPLE)
    }

    /**
     * Копирование ветки.
     */
    private fun copyNode(node: TetroidNode) {
        if (viewModel.hasNonDecryptedNodes(node)) {
            viewModel.showMessage(getString(R.string.log_enter_pass_first), LogType.WARNING)
            return
        }
        // добавляем в "буфер обмена"
        ClipboardManager.copy(node)
        viewModel.logOperRes(LogObj.NODE, LogOper.COPY)
    }

    private fun onNodeInserted(node: TetroidNode) {
        if (listAdapterNodes.addItem(node)) {
            viewModel.logOperRes(LogObj.NODE, LogOper.INSERT)
        } else {
            viewModel.logError(getString(R.string.log_create_node_list_error), true)
        }
    }

    /**
     * Диалог с вопросом о сбросе пароля в связи с отсутствием зашифрованных веток.
     */
    private fun showNoCryptedNodesLeftDialog() {
        AskDialogs.showYesDialog(
            context = this,
            messageResId = R.string.ask_clear_pass_database_ini,
            onApply = {
                viewModel.clearSavedPass()
            },
        )
    }

    /**
     * Если поля в INI-файле для проверки пустые, спрашиваем "Continue anyway?"
     */
    private fun showEmptyPassCheckingFieldDialog(
        fieldName: String,
        passHash: String,
        callbackEvent: VMEvent
    ) {
        AskDialogs.showYesNoDialog(
            context = this,
            message = getString(R.string.log_empty_middle_hash_check_data_field, fieldName),
            onApply = {
                viewModel.confirmEmptyPassCheckingFieldDialog(
                    passHash = passHash,
                    callbackEvent = callbackEvent,
                )
            },
            onCancel = {
                viewModel.cancelEmptyPassCheckingFieldDialog(callbackEvent)
            },
        )
    }

    // endregion Node

    // region Storage tree
    
    private fun showStorageTreeChangedOutsideDialog() {
        AskDialogs.showYesNoDialog(
            context = this,
            messageResId = R.string.ask_storage_tree_changed_outside,
            onApply = {
                viewModel.startReinitStorage()
            },
            onCancel = {},
            onDismiss = {
                viewModel.dropIsStorageChangingHandled()
            },
        )
    }

    private fun showStorageTreeDeletedOutsideDialog() {
        AskDialogs.showYesNoDialog(
            context = this,
            messageResId = R.string.ask_storage_tree_deleted_outside,
            onApply = {
                viewModel.saveMytetraXmlFromCurrentStorageTree()
            },
            onCancel = {},
            onDismiss =  {
                viewModel.dropIsStorageChangingHandled()
            },
        )
    }

    // endregion Storage tree

    // region Favorites
    
    /**
     * Обновление ветки Избранное.
     */
    fun updateFavoritesTitle() {
        val favorites = viewModel.getFavoriteRecords()
        val size = favorites.size
        val tvName = findViewById<TextView>(R.id.favorites_name)
        tvName.setTextColor(
            ContextCompat.getColor(
                this,
                if (size > 0) R.color.colorBaseText else R.color.colorLightText
            )
        )
        val favoritesCountView = findViewById<TextView>(R.id.favorites_count)
        favoritesCountView.text = "[%d]".format(Locale.getDefault(), size)
    }

    // endregion Favorites

    // region Tag

    /**
     * Переименование метки в записях.
     */
    private fun renameTag(tag: TetroidTag) {
        TagFieldsDialog(
            tag = tag
        ) { name: String -> 
            viewModel.renameTag(tag, name) 
        }
            .showIfPossible(supportFragmentManager)
    }

    /**
     * Копирование ссылки на метку в буфер обмена.
     */
    private fun copyTagLink(tag: TetroidTag?) {
        if (tag != null) {
            val url = tag.createUrl()
            Utils.writeToClipboard(this, getString(R.string.link_to_tag), url)
            viewModel.log(getString(R.string.title_link_was_copied) + url, true)
        } else {
            viewModel.log(getString(R.string.log_get_item_is_null), true)
        }
    }

    // endregion Tag

    // region Records

    /**
     * Отображение списка записей.
     */
    private fun showRecords(records: List<TetroidRecord>, viewId: Int) {
        showRecords(records, viewId, dropSearch = true)
    }

    /**
     * Отображение списка записей.
     * @param dropSearch Нужно ли закрыть фильтрацию SearchView
     */
    private fun showRecords(records: List<TetroidRecord>, viewId: Int, dropSearch: Boolean) {
        // сбрасываем фильтрацию при открытии списка записей
        if (dropSearch && !searchViewRecords.isIconified) {
            // сбрасываем SearchView;
            // но т.к. при этом срабатывает событие onClose, нужно избежать повторной загрузки
            // полного списка записей в его обработчике с помощью проверки mIsDropRecordsFiltering
            viewModel.isDropRecordsFiltering = false
//          mSearchViewRecords.onActionViewCollapsed();
            searchViewRecords.setQuery("", false)
            searchViewRecords.isIconified = true
            viewModel.isDropRecordsFiltering = true
        }
        drawerLayout.closeDrawers()
        viewPager.setCurrent(Constants.PAGE_MAIN)
        mainPage.showRecords(records, viewId)
    }

    // endregion Records

    // region Record
    
    /**
     * Открытие активности RecordActivity.
     */
    private fun openRecord(bundle: Bundle) {
        RecordActivity.start(this, Intent.ACTION_MAIN, Constants.REQUEST_CODE_RECORD_ACTIVITY, bundle)
    }

    // endregion Record

    // region ContextMenus
    
    /**
     * Отображение всплывающего (контексного) меню ветки.
     * FIXME: Заменить на использование AlertDialog ? (чтобы посередине экрана)
     */
    @SuppressLint("RestrictedApi", "NonConstantResourceId")
    private fun showNodePopupMenu(v: View, node: TetroidNode, pos: Int) {
        val popupMenu = PopupMenu(this, v) //, Gravity.CENTER_HORIZONTAL);
        popupMenu.inflate(R.menu.node_context)
        val menu = popupMenu.menu
        val parentNode = node.parentNode
        val isNonCrypted = node.isNonCryptedOrDecrypted
        visibleMenuItem(menu.findItem(R.id.action_expand_node), node.isExpandable && isNonCrypted)
//        visibleMenuItem(menu.findItem(R.id.action_create_node), isNonCrypted);
        visibleMenuItem(menu.findItem(R.id.action_create_subnode), isNonCrypted)
        visibleMenuItem(menu.findItem(R.id.action_rename), isNonCrypted)
//        visibleMenuItem(menu.findItem(R.id.action_collapse_node), node.isExpandable());
        val nodesCount = (if (parentNode != null) parentNode.subNodes else viewModel.getRootNodes()).size
        visibleMenuItem(menu.findItem(R.id.action_move_up), nodesCount > 0)
        visibleMenuItem(menu.findItem(R.id.action_move_down), nodesCount > 0)
        val canInsert = ClipboardManager.hasObject(FoundType.TYPE_NODE)
        visibleMenuItem(menu.findItem(R.id.action_insert), canInsert)
        visibleMenuItem(menu.findItem(R.id.action_insert_subnode), canInsert && isNonCrypted)
        visibleMenuItem(menu.findItem(R.id.action_copy), isNonCrypted)
        val canCutDel = (node.level > 0 || viewModel.getRootNodes().size > 1) && isNonCrypted
        visibleMenuItem(menu.findItem(R.id.action_cut), canCutDel)
        visibleMenuItem(menu.findItem(R.id.action_delete), canCutDel)
        visibleMenuItem(menu.findItem(R.id.action_encrypt_node), !node.isCrypted)
        val canNoCrypt = node.isCrypted && (parentNode == null || !parentNode.isCrypted)
        visibleMenuItem(menu.findItem(R.id.action_drop_encrypt_node), canNoCrypt)
        visibleMenuItem(menu.findItem(R.id.action_info), isNonCrypted)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_open_node -> {
                    viewModel.showNode(node)
                    true
                }
                R.id.action_create_subnode -> {
                    createNode(node, pos, isSubNode = true)
                    true
                }
                R.id.action_create_node -> {
                    createNode(node, pos, isSubNode = false)
                    true
                }
                R.id.action_rename -> {
                    renameNode(node)
                    true
                }
                R.id.action_node_icon -> {
                    setNodeIcon(node)
                    true
                }
                R.id.action_copy_link -> {
                    copyNodeLink(node)
                    true
                }
                R.id.action_encrypt_node -> {
                    viewModel.startEncryptNode(node)
                    true
                }
                R.id.action_drop_encrypt_node -> {
                    viewModel.startDropEncryptNode(node)
                    true
                }
                R.id.action_expand_node -> {
                    expandSubNodes(pos)
                    true
                }
                R.id.action_move_up -> {
                    viewModel.reorderNode(node, pos, isUp = true)
                    true
                }
                R.id.action_move_down -> {
                    viewModel.reorderNode(node, pos, isUp = false)
                    true
                }
                R.id.action_copy -> {
                    copyNode(node)
                    true
                }
                R.id.action_cut -> {
                    viewModel.cutNode(node, pos)
                    true
                }
                R.id.action_insert -> {
                    viewModel.insertNode(node, isSubNode = false)
                    true
                }
                R.id.action_insert_subnode -> {
                    viewModel.insertNode(node, isSubNode = true)
                    true
                }
                R.id.action_info -> {
                    showNodeInfoDialog(node)
                    true
                }
                R.id.action_delete -> {
                    viewModel.startDeleteNode(node)
                    true
                }
                else -> false
            }
        }
        setForceShowMenuIcons(v, menu as MenuBuilder)
    }

    // endregion ContextMenus

    // region OnActivityResult
    
    /**
     * Обработка возвращаемого результата других активностей.
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            Constants.REQUEST_CODE_STORAGES_ACTIVITY -> {
                data?.let { onStoragesActivityResult(data) }
            }
            Constants.REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY -> {
                data?.let { onStorageSettingsActivityResult(data) }
            }
            Constants.REQUEST_CODE_COMMON_SETTINGS_ACTIVITY -> {
                    data?.let { onCommonSettingsActivityResult(data) }
            }
            Constants.REQUEST_CODE_RECORD_ACTIVITY -> {
                onRecordActivityResult(resultCode, data)
            }
            Constants.REQUEST_CODE_SEARCH_ACTIVITY -> {
                if (resultCode == RESULT_OK) {
                    data?.getParcelableExtra<SearchProfile>(Constants.EXTRA_SEARCH_PROFILE)?.let { profile ->
                        viewModel.startGlobalSearch(profile)
                    }
                }
            }
            Constants.REQUEST_CODE_SYNC_STORAGE -> {
                viewModel.onStorageSyncFinished(resultCode == RESULT_OK)
            }
            Constants.REQUEST_CODE_FILE_PICKER -> {
                if (resultCode == RESULT_OK) {
                    data?.getStringExtra(FolderPicker.EXTRA_DATA)?.let { fileFullName ->
                        // сохраняем путь
                        CommonSettings.setLastChoosedFolder(this, FileUtils.getFileFolder(fileFullName))
                        viewModel.attachFileToCurRecord(fileFullName, false)
                    }
                }
            }
            Constants.REQUEST_CODE_FOLDER_PICKER -> {
                if (resultCode == RESULT_OK) {
                    data?.getStringExtra(FolderPicker.EXTRA_DATA)?.let { folderPath ->
                        // сохраняем путь
                        CommonSettings.setLastChoosedFolder(this, folderPath)
                        viewModel.saveCurAttachOnDevice(folderPath)
                    }
                }
            }
            Constants.REQUEST_CODE_NODE_ICON -> {
                if (resultCode == RESULT_OK) {
                    val nodeId = data?.getStringExtra(Constants.EXTRA_NODE_ID)
                    val iconPath = data?.getStringExtra(Constants.EXTRA_NODE_ICON_PATH)
                    val isDrop = data?.getBooleanExtra(Constants.EXTRA_IS_DROP, false) ?: false
                    if (nodeId != null) {
                        viewModel.setNodeIcon(nodeId, iconPath, isDrop)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Обработка возвращаемого результата активности со списком хранилищ.
     */
    private fun onStoragesActivityResult(data: Intent) {
        // проверяем нужно ли отслеживать структуру хранилища
        viewModel.startStorageTreeObserverIfNeeded()

        // скрываем пункт меню Синхронизация, если отключили
        updateOptionsMenu()
        // обновляем списки, могли измениться настройки отображения
        viewModel.updateRecordsList()
        updateNodes()
        onStorageChangedIntent(data)
    }

    /**
     * Обработка возвращаемого результата активности настроек хранилища.
     */
    private fun onStorageSettingsActivityResult(data: Intent) {
        // проверяем нужно ли отслеживать структуру хранилища
        viewModel.startStorageTreeObserverIfNeeded()

        // скрываем пункт меню Синхронизация, если отключили
        updateOptionsMenu()
        onStorageChangedIntent(data)
    }

    /**
     * Обработка возвращаемого результата активности общих настроек приложения.
     */
    private fun onCommonSettingsActivityResult(data: Intent) {
        // обновляем списки, могли измениться настройки отображения
        viewModel.updateRecordsList()
        updateNodes()
    }

    private fun onStorageChangedIntent(data: Intent?) {
        if (data != null) {
            // перезагружаем хранилище, если изменили путь
//            if (data.getBooleanExtra(Constants.EXTRA_IS_REINIT_STORAGE, false)) {
//                boolean toCreate = data.getBooleanExtra(Constants.EXTRA_IS_CREATE_STORAGE, false);
//                AskDialogs.showReloadStorageDialog(this, toCreate, true, () -> {
//                    if (toCreate) {
//                        createStorage(SettingsManager.getStoragePath(this)/*, true*/);
//                    } else {
//                        reinitStorage();
//                    }
//                });
//            } else
            /*if (data.getBooleanExtra(Constants.EXTRA_IS_RELOAD_STORAGE_ENTITY, false)) {
                viewModel.startReloadStorageEntity();
            } else*/
            if (data.getBooleanExtra(Constants.EXTRA_IS_LOAD_STORAGE, false)) {
                val storageId = data.getIntExtra(Constants.EXTRA_STORAGE_ID, 0)
                val isLoadAllNodes = data.getBooleanExtra(Constants.EXTRA_IS_LOAD_ALL_NODES, false)
                viewModel.startInitStorage(storageId, isLoadAllNodes)
            } else {
                if (data.getBooleanExtra(Constants.EXTRA_IS_RELOAD_STORAGE_ENTITY, false)) {
                    // перезагрузить свойства хранилища из базы
                    viewModel.startReloadStorageEntity()
                }
                if (data.getBooleanExtra(Constants.EXTRA_IS_LOAD_ALL_NODES, false)) {
                    viewModel.loadAllNodes(false)
                } else if (data.getBooleanExtra(Constants.EXTRA_IS_PASS_CHANGED, false)) {
                    // обновляем списки, т.к. хранилище должно было расшифроваться
                    updateNodes()
                    updateTags()
                }
            }
        }
    }

    /**
     * Обработка возвращаемого результата активности просмотра содержимого записи.
     */
    private fun onRecordActivityResult(resCode: Int, data: Intent?) {
        if (data == null) {
            // обычное закрытие активности

            // проверяем пора ли запустить диалог оценки приложения
            checkForInAppReviewShowing()
            return
        }
        // проверяем нужно ли отслеживать структуру хранилища
        viewModel.startStorageTreeObserverIfNeeded()
        if (data.getBooleanExtra(Constants.EXTRA_IS_FIELDS_EDITED, false)) {
            // обновляем списки, если редактировали свойства записи
            viewModel.onRecordFieldsUpdated(null, false)
        } else {
            // обновляем список записей, чтобы обновить дату изменения
            if (viewModel.settingsManager.getRecordFieldsSelector().checkIsEditedDate()) {
                viewModel.updateRecordsList()
            }
        }
        when (resCode) {
            Constants.RESULT_REINIT_STORAGE -> 
                /*if (data.getBooleanExtra(Constants.EXTRA_IS_CREATE_STORAGE, false)) {
                    createStorage(SettingsManager.getStoragePath(this)*/
                /*, true*/ /*);
                } else*/ {
                reinitStorage()
                // сбрасываем Intent, чтобы избежать циклической перезагрузки хранилища
                receivedIntent = null
            }
            Constants.RESULT_PASS_CHANGED -> {
                if (data.getBooleanExtra(Constants.EXTRA_IS_PASS_CHANGED, false)) {
                    // обновляем списки, т.к. хранилище должно было расшифроваться
                    updateNodes()
                    updateTags()
                }
            }
            Constants.RESULT_OPEN_RECORD -> {
                if (checkIsNeedLoadAllNodes(data)) return
                data.getStringExtra(Constants.EXTRA_OBJECT_ID)?.let { recordId ->
                    viewModel.openRecord(recordId)
                }
            }
            Constants.RESULT_OPEN_NODE -> {
                if (checkIsNeedLoadAllNodes(data)) return
                data.getStringExtra(Constants.EXTRA_OBJECT_ID)?.let { nodeId ->
                    viewModel.showNode(nodeId)
                }
            }
            Constants.RESULT_SHOW_ATTACHES -> {
                data.getStringExtra(Constants.EXTRA_OBJECT_ID)?.let { recordId ->
                    viewModel.showRecordAttaches(recordId, fromRecordActivity = true)
                }
            }
            Constants.RESULT_SHOW_TAG -> {
                if (checkIsNeedLoadAllNodes(data)) return
                data.getStringExtra(Constants.EXTRA_TAG_NAME)?.let { tagName ->
                    viewModel.showTagRecords(tagName)
                }
            }
            Constants.RESULT_DELETE_RECORD -> {
                data.getStringExtra(Constants.EXTRA_OBJECT_ID)?.let { recordId ->
                    viewModel.deleteRecord(recordId)
                }
            }
        }
    }

    override fun onPermissionGranted(requestCode: Int) {
        when (requestCode) {
            Constants.REQUEST_CODE_PERMISSION_WRITE_TEMP -> viewModel.checkPermissionAndOpenTempAttach(this)
            Constants.REQUEST_CODE_PERMISSION_TERMUX -> viewModel.syncAndInitStorage(this)
            else -> super.onPermissionGranted(requestCode)
        }
    }

    // endregion OnActivityResult

    // region GlobalSearch

    private fun setFoundPageVisibility(isVisible: Boolean) {
        if (!isVisible) {
            viewPager.setCurrent(Constants.PAGE_MAIN)
        }
        viewPager.setPagingEnabled(isVisible)
        titleStrip.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private fun onGlobalSearchFinished(found: Map<ITetroidObject, FoundType>, profile: SearchProfile) {
        foundPage.setFounds(found, profile)
        viewPagerAdapter.notifyDataSetChanged() // для обновления title у страницы
        setFoundPageVisibility(true)
        viewPager.setCurrent(Constants.PAGE_FOUND)
    }

    private fun closeFoundFragment() {
        setFoundPageVisibility(false)
    }

    private fun research() {
        showGlobalSearchActivity(null)
    }

    // endregion GlobalSearch

    // region OnNewIntent
    
    /**
     * Обработка входящего Intent.
     */
    override fun onNewIntent(intent: Intent?) {
        checkReceivedIntent(intent)
        super.onNewIntent(intent)
    }

    /**
     * Проверка входящего Intent.
     */
    private fun checkReceivedIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        when (intent.action) {
            Intent.ACTION_SEARCH -> {
                // обработка результата голосового поиска
                val query = intent.getStringExtra(SearchManager.QUERY)
                searchViewRecords.setQuery(query, true)
            }
            Constants.ACTION_RECORD -> {
                val resCode = intent.getIntExtra(Constants.EXTRA_RESULT_CODE, 0)
                onRecordActivityResult(resCode, intent)
            }
            Constants.ACTION_MAIN_ACTIVITY -> if (intent.hasExtra(Constants.EXTRA_SHOW_STORAGE_INFO)) {
                showStorageInfoActivity()
            }
            Constants.ACTION_STORAGE_SETTINGS -> {
                val storageId = intent.getIntExtra(Constants.EXTRA_STORAGE_ID, 0)
                if (storageId > 0) {
                    val isLoadStorage = intent.getBooleanExtra(Constants.EXTRA_IS_LOAD_STORAGE, false)
                    val isLoadAllNodes = intent.getBooleanExtra(Constants.EXTRA_IS_LOAD_ALL_NODES, false)
                    if (isLoadStorage) {
                        viewModel.startInitStorage(storageId, isLoadAllNodes)
                    } else if (isLoadAllNodes) {
                        viewModel.loadAllNodes(true)
                    }
                }
            }
            Intent.ACTION_SEND -> {

                // прием текста/изображения из другого приложения
                val type = intent.type ?: return
                var text: String? = null
                var isText = false
                val uris = ArrayList<Uri>()
                if (type.startsWith("text/")) {
                    isText = true
                    text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (text == null) {
                        viewModel.logWarning(R.string.log_not_passed_text, true)
                        return
                    }
                    viewModel.log(getString(R.string.log_receiving_intent_text))
                } else if (type.startsWith("image/")) {
                    // изображение
                    val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (imageUri == null) {
                        viewModel.logWarning(R.string.log_not_passed_image_uri, true)
                        return
                    }
                    viewModel.log(getString(R.string.log_receiving_intent_image_mask, imageUri))
                    uris.add(imageUri)
                }
                showIntentDialog(intent, isText, text, uris)
            }
            Intent.ACTION_SEND_MULTIPLE -> {

                // прием нескольких изображений из другого приложения
                val type = intent.type ?: return
                if (type.startsWith("image/")) {
                    val uris = intent.getParcelableArrayListExtra<Uri?>(Intent.EXTRA_STREAM)
                    if (uris == null) {
                        viewModel.logWarning(R.string.log_not_passed_image_uri, true)
                        return
                    }
                    viewModel.log(getString(R.string.log_receiving_intent_images_mask, uris.size))
                    showIntentDialog(intent, false, null, uris)
                }
            }
        }
    }

    /**
     * Отрытие диалога для вставки объектов из других приложений.
     */
    private fun showIntentDialog(intent: Intent, isText: Boolean, text: String?, imagesUri: ArrayList<Uri>) {
        if (viewModel.isLoadedFavoritesOnly()) {
            // если загружено только избранное, то нужно сначала загрузить все ветки,
            // чтобы добавить текст/картинку в одну из записей или в новую запись одной из веток
            val title = getString(
                when {
                    isText -> R.string.word_received_text
                    imagesUri.size > 1 -> R.string.word_received_images
                    else -> R.string.word_received_image
                }
            )
            AskDialogs.showYesNoDialog(
                context = this,
                message = Utils.fromHtml(
                    getString(R.string.text_load_nodes_before_receive_mask, title)
                ),
                onApply = {
                    // сохраняем Intent и загружаем хранилище
                    receivedIntent = intent
                    viewModel.loadAllNodes(false)
                },
                onCancel = {},
            )
        } else {
            IntentDialog(isText) { receivedData: ReceivedData ->
                if (receivedData.isCreate) {
                    viewModel.createRecordFromIntent(intent, isText, text!!, imagesUri, receivedData)
                } else {
                    // TODO: реализовать выбор имеющихся записей
                }
            }
        }
    }

    // endregion OnNewIntent

    // region Search
    
    /**
     * Фильтр меток по названию.
     * @param isSearch Если false, то происходит сброс фильтра.
     */
    private fun searchInTags(query: String?, isSearch: Boolean) {
        val tags = if (isSearch) {
            ScanManager.searchInTags(viewModel.getTagsMap(), query)
        } else {
            viewModel.getTagsMap()
        }
        fragmentTags.setTagsDataItems(tags)
        if (tags.isEmpty()) {
            fragmentTags.setTagsEmptyText(
                if (isSearch) {
                    getString(R.string.search_tags_not_found_mask, query)
                } else {
                    getString(R.string.log_tags_is_missing)
                }
            )
        }
    }

    /**
     * Виджет поиска по записям ветки / прикрепленным к записи файлам.
     */
    private fun initRecordsSearchView(menuItem: MenuItem) {
        // Associate searchable configuration with the SearchView
        searchViewRecords = (menuItem.actionView as SearchView)
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        searchViewRecords.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchViewRecords.setIconifiedByDefault(true)
        searchViewRecords.isQueryRefinementEnabled = true
        object : SearchViewXListener(searchViewRecords) {
            override fun onSearchClick() {}
            override fun onQuerySubmit(query: String) {
                viewModel.filterListInMainPage(query, true)
            }

            override fun onQueryChange(query: String) {
                viewModel.filterListInMainPage(query, false)
            }

            override fun onSuggestionSelectOrClick(query: String) {
                searchViewRecords.setQuery(query, true)
            }

            override fun onClose() {
                viewModel.onRecordsSearchClose()
            }
        }
    }

    /**
     * Настройка элемента для фильтра веток.
     */
    private fun initNodesSearchView(searchView: SearchView, nodesHeader: View) {
        val tvHeader = nodesHeader.findViewById<TextView>(R.id.text_view_nodes_header)
        val ivIcon = nodesHeader.findViewById<ImageView>(R.id.image_view_app_icon)
        object : SearchViewXListener(searchView) {
            override fun onClose() {
                // ничего не делать, если хранилище не было загружено
                if (listAdapterNodes == null) return
                searchInNodesNames(null)
                setListEmptyViewState(
                    tvEmpty = tvNodesEmpty,
                    isVisible = viewModel.getRootNodes().isEmpty(),
                    stringId = R.string.title_nodes_is_missing
                )
                tvHeader.visibility = View.VISIBLE
                ivIcon.visibility = View.VISIBLE
            }

            override fun onSearchClick() {
                tvHeader.visibility = View.GONE
                ivIcon.visibility = View.GONE
            }

            override fun onQuerySubmit(query: String) {
                searchInNodesNames(query)
            }

            override fun onQueryChange(query: String) {
                searchInNodesNames(query)
            }

            override fun onSuggestionSelectOrClick(query: String) {
                searchInNodesNames(query)
            }
        }
    }

    /**
     * Настройка элемента для фильтра меток.
     */
    private fun initTagsSearchView(searchView: SearchView, tagsHeader: View) {
        val tvHeader = tagsHeader.findViewById<TextView>(R.id.text_view_tags_header)
        object : SearchViewXListener(searchView) {
            override fun onClose() {
                searchInTags(null, false)
                tvHeader.visibility = View.VISIBLE
            }

            override fun onSearchClick() {
                tvHeader.visibility = View.GONE
            }

            override fun onQuerySubmit(query: String) {
                searchInTags(query, true)
            }

            override fun onQueryChange(query: String) {
                searchInTags(query, true)
            }

            override fun onSuggestionSelectOrClick(query: String) {
                searchInTags(query, true)
            }
        }
    }

    /**
     * Фильтр веток по названию ветки.
     */
    private fun searchInNodesNames(query: String?) {
        if (listAdapterNodes != null) {
            if (query.isNullOrEmpty()) {
                // просто выводим все ветки
                listAdapterNodes.setDataItems(viewModel.getRootNodes())
                setListEmptyViewState(tvNodesEmpty, false, "")
            } else {
                val found = ScanManager.searchInNodesNames(viewModel.getRootNodes(), query)
                listAdapterNodes.setDataItems(found)
                setListEmptyViewState(
                    tvEmpty = tvNodesEmpty,
                    isVisible = found.isEmpty(),
                    string = getString(R.string.search_nodes_not_found_mask, query)
                )
            }
        }
    }

    private fun closeSearchView(search: SearchView) {
        search.isIconified = true
        search.setQuery("", false)
    }

    // endregion Search

    // region InAppReview
    
    /**
     * Запуск механизма оценки и отзыва о приложении In-App Review.
     *
     * TODO: пока отключено.
     *
     */
    private var recordOpeningCount = 0
    private fun checkForInAppReviewShowing() {
        if (++recordOpeningCount > 2) {
            recordOpeningCount = 0
//            TetroidReview.showInAppReview(this);
        }
    }

    // endregion InAppReview

    // region OptionsMenu
    
    /**
     * Обработчик создания системного меню.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!super.onBeforeCreateOptionsMenu(menu)) return true
        menuInflater.inflate(R.menu.main, menu)
        optionsMenu = menu
        initRecordsSearchView(menu.findItem(R.id.action_search_records))
        mainPage.onCreateOptionsMenu(menu)
        return super.onAfterCreateOptionsMenu(menu)
    }

    /**
     * Обработчик подготовки пунктов системного меню перед отображением.
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (!isOnCreateProcessed) return true
        val curViewId = if (viewPagerAdapter != null) viewModel.curMainViewId else 0
        val canSearchRecords = viewPager != null && viewPager.currentItem == Constants.PAGE_MAIN && (curViewId == Constants.MAIN_VIEW_NODE_RECORDS
                || curViewId == Constants.MAIN_VIEW_TAG_RECORDS)
        menu.findItem(R.id.action_search_records)?.isVisible = canSearchRecords
        menu.findItem(R.id.action_storage_sync)?.isVisible = viewModel.isStorageSyncEnabled()
        val isStorageLoaded = viewModel.isStorageLoaded()
        enableMenuItem(menu.findItem(R.id.action_search_records), isStorageLoaded)
        enableMenuItem(menu.findItem(R.id.action_global_search), isStorageLoaded)
        enableMenuItem(menu.findItem(R.id.action_storage_sync), isStorageLoaded)
        val isStorageNotNull = viewModel.storage != null
        enableMenuItem(menu.findItem(R.id.action_storage_info), isStorageNotNull)
        enableMenuItem(menu.findItem(R.id.action_storage_settings), isStorageNotNull)
        enableMenuItem(menu.findItem(R.id.action_storage_reload), isStorageNotNull)

        mainPage.onPrepareOptionsMenu(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * Обработчик выбора пунктов системного меню.
     */
    @SuppressLint("NonConstantResourceId")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_move_back -> {
                viewModel.moveBackFromAttaches()
                return true
            }
            R.id.action_global_search -> {
                showGlobalSearchActivity(null)
                return true
            }
            R.id.action_storage_sync -> {
                viewModel.syncStorage(this)
                return true
            }
            R.id.action_storage_info -> {
                showStorageInfoActivity()
                return true
            }
            R.id.action_storage_reload -> {
                reloadStorageAsk()
                return true
            }
            R.id.action_storages -> {
                showStoragesActivity()
                return true
            }
            R.id.action_storage_settings -> showStorageSettingsActivity(viewModel.storage)
            R.id.action_settings -> {
                showActivityForResult(SettingsActivity::class.java, Constants.REQUEST_CODE_COMMON_SETTINGS_ACTIVITY)
                return true
            }
            else -> if (mainPage.onOptionsItemSelected(id)) {
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // endregion OptionsMenu

    // region Exit

    /**
     * Обработчик нажатия кнопки Назад.
     */
    override fun onBackPressed() {
        if (!onBeforeBackPressed()) {
            return
        }
        if (CommonSettings.isShowNodesInsteadExit(this)) {
            // показывать левую шторку вместо выхода
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                if (CommonSettings.isConfirmAppExit(this)) {
                    askForExit()
                } else {
                    viewModel.onBeforeExit(this)
                }
            } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                var needToOpenDrawer = true
                val curPage = viewPager.currentItem
                if (curPage == Constants.PAGE_MAIN || curPage == Constants.PAGE_FOUND) {
                    if (curPage == Constants.PAGE_MAIN && mainPage.onBackPressed()
                        || curPage == Constants.PAGE_FOUND && foundPage.onBackPressed()
                    ) {
                        needToOpenDrawer = false
                    }
                }
                // открываем левую шторку, если все проверили
                if (needToOpenDrawer) {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
            }
        } else {
            var needToExit = true
            // выходить, если не отображаются боковые панели
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                val curPage = viewPager.currentItem
                if (curPage == Constants.PAGE_MAIN || curPage == Constants.PAGE_FOUND) {
                    if (curPage == Constants.PAGE_MAIN && !mainPage.onBackPressed()
                        || curPage == Constants.PAGE_FOUND && !foundPage.onBackPressed()
                    ) {
                        if (CommonSettings.isConfirmAppExit(this)) {
                            askForExit()
                            needToExit = false
                        }
                    } else {
                        needToExit = false
                    }
                }
                // выходим, если все проверили
                if (needToExit) {
                    viewModel.onBeforeExit(this)
                }
            }
        }
    }

    override fun onPause() {
        // устанавливаем признак необходимости запроса PIN-кода
        viewModel.setIsPINNeedToEnter()
        super.onPause()
    }

    private fun askForExit() {
        AskDialogs.showYesDialog(
            context = this,
            messageResId = R.string.ask_exit_from_app,
            onApply = {
                viewModel.onBeforeExit(this)
            },
        )
    }

    private fun showClearTrashDialog(callback: ICallback?) {
        AskDialogs.showYesNoDialog(
            context = this,
            isCancelable = false,
            messageResId = R.string.ask_clear_trash,
            onApply = {
                viewModel.clearTrashFolderAndExit(false, callback)
            },
            onCancel = {
                callback?.run(true)
            },
        )
    }

    // endregion Exit

    // region StartActivity

    private fun showStorageInfoActivity() {
        start(this, viewModel.getStorageId())
    }

    private fun showGlobalSearchActivity(query: String?) {
        if (viewModel.isLoadedFavoritesOnly()) {
            viewModel.showMessage(getString(R.string.mes_all_nodes_must_be_loaded), LogType.WARNING)
        } else {
            val curNode = viewModel.curNode
            val curNodeId = if (curNode != null && curNode !== FAVORITES_NODE) curNode.id else null
            start(this, query, curNodeId, viewModel.getStorageId())
        }
    }

    // endregion StartActivity

    // region Tasks

    fun taskMainPreExecute(progressTextResId: Int): Int {
        super.taskPreExecute(progressTextResId)
        val openedDrawer =
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) GravityCompat.START else if (drawerLayout.isDrawerOpen(GravityCompat.END)) GravityCompat.END else Gravity.NO_GRAVITY
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        return openedDrawer
    }

    fun taskMainPostExecute() {
        super.taskPostExecute()
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    //endregion Tasks

    companion object {
        fun start(context: Context, action: String?, bundle: Bundle?) {
            val intent = Intent(context, MainActivity::class.java)
            intent.action = action
            if (bundle != null) {
                intent.putExtras(bundle)
            }
            context.startActivity(intent)
        }
    }

}