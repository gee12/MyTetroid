package com.gee12.mytetroid.ui.main

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import com.gee12.mytetroid.R
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.manager.ClipboardManager
import com.gee12.mytetroid.model.FoundType
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.ui.attach.FilesListAdapter
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.dialogs.attach.AttachFieldsDialog
import com.gee12.mytetroid.ui.dialogs.attach.AttachFileByUrlDialog
import com.gee12.mytetroid.ui.dialogs.attach.AttachInfoDialog
import com.gee12.mytetroid.ui.dialogs.record.RecordFieldsDialog
import com.gee12.mytetroid.ui.dialogs.record.RecordInfoDialog
import com.gee12.mytetroid.ui.base.TetroidFragment
import com.gee12.mytetroid.ui.main.records.RecordsListAdapter
import com.github.clans.fab.FloatingActionMenu
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainPageFragment : TetroidFragment<MainViewModel>, MainPage {
    
    private lateinit var viewFlipperMain: ViewFlipper
    private lateinit var lvRecords: ListView
    private lateinit var lvAttaches: ListView
    private lateinit var tvRecordsEmpty: TextView
    private lateinit var tvFilesEmpty: TextView
    private lateinit var fabAddRecord: FloatingActionButton
    private lateinit var fabAddAttach: FloatingActionMenu
    private lateinit var btnUseGlobalSearch: Button
    private lateinit var listAdapterRecords: RecordsListAdapter
    private lateinit var listAdapterAttaches: FilesListAdapter

    val viewModel: MainViewModel
        get() = (requireActivity() as MainActivity).viewModel

    constructor(gestureDetector: GestureDetectorCompat) : super(gestureDetector)

    constructor() : super()

    override fun getLayoutResourceId() = R.layout.fragment_main

    override fun getViewModelClazz() = MainViewModel::class.java

    override fun createDependencyScope() {
        scopeSource = ScopeSource.current
    }

    override fun getTitle(): String {
        return resourcesProvider.getString(R.string.title_main_fragment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        
        viewFlipperMain = view.findViewById(R.id.view_flipper_main)
        // обработка нажатия на пустом месте экрана, когда записей в ветке нет
        viewFlipperMain.setOnTouchListener(this)

        // список записей
        lvRecords = view.findViewById(R.id.list_view_records)

        // обработка нажатия на пустом месте списка записей
        lvRecords.setOnTouchListener(this)
        //
        lvRecords.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long -> 
            showRecord(position) 
        }
        tvRecordsEmpty = view.findViewById(R.id.text_view_empty_records)
        btnUseGlobalSearch = view.findViewById(R.id.button_global_search)
        btnUseGlobalSearch.setOnClickListener {
            viewModel.startGlobalSearchFromFilterQuery() 
        }
        lvRecords.emptyView = tvRecordsEmpty

        // пустое пространство под списками
        val recordsFooter = (requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.list_view_empty_footer, null, false)
        lvRecords.addFooterView(recordsFooter, null, false)
        registerForContextMenu(lvRecords)
        /*mListViewRecords.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });*/

        // список файлов
        lvAttaches = view.findViewById(R.id.list_view_files)
        // обработка нажатия на пустом месте списка файлов
        lvAttaches.setOnTouchListener(this)
        lvAttaches.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long -> 
            openAttach(position) 
        }
        tvFilesEmpty = view.findViewById(R.id.text_view_empty_files)
        lvAttaches.emptyView = tvFilesEmpty

        // пустое пространство под списками
        val filesFooter = (requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.list_view_empty_footer, null, false)
        lvAttaches.addFooterView(filesFooter, null, false)
        registerForContextMenu(lvAttaches)
        fabAddRecord = view.findViewById(R.id.button_add_record)
        fabAddRecord.setOnClickListener {
            createRecord()
        }
        fabAddAttach = view.findViewById(R.id.fab_add_attach)
        fabAddAttach.setClosedOnTouchOutside(true)
        val fabAddLocalFile = view.findViewById<com.github.clans.fab.FloatingActionButton>(R.id.fab_attach_local_file)
        fabAddLocalFile.setOnClickListener {
            fabAddAttach.close(true)
            viewModel.pickAndAttachFile()
        }
        val fabAttachFileByLink = view.findViewById<com.github.clans.fab.FloatingActionButton>(R.id.fab_attach_file_by_link)
        fabAttachFileByLink.setOnClickListener {
            fabAddAttach.close(true)
            AttachFileByUrlDialog(
                onApply = { url: String ->
                    viewModel.downloadAndAttachFile(url)
                }
            ).showIfPossible(parentFragmentManager)
        }

        initListAdapters()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showListIfFragmentCreated()
    }

    private fun showListIfFragmentCreated() {
        if (isAdded) {
            showView(viewType = viewModel.currentMainViewType)
            viewModel.updateRecordsList()
        }
    }

    private fun initListAdapters() {
        // список записей
        val settingsProvider = viewModel.settingsManager
        listAdapterRecords = RecordsListAdapter(
            requireContext(),
            resourcesProvider = viewModel.resourcesProvider,
            buildInfoProvider = buildInfoProvider,
            dateTimeFormat = settingsProvider.checkDateFormatString(),
            isHighlightAttach = settingsProvider.isHighlightRecordWithAttach(),
            highlightAttachColor = settingsProvider.highlightAttachColor(),
            fieldsSelector = settingsProvider.getRecordFieldsSelector(),
            getEditedDateCallback = { record ->
                viewModel.getEditedDate(record)
            },
            onClick = { record ->
                viewModel.showRecordAttaches(record)
            }
        )
        lvRecords.adapter = listAdapterRecords

        // список файлов
        listAdapterAttaches = FilesListAdapter(
            context = requireContext(),
            getAttachedFileSize = { attach ->
                viewModel.getAttachFileSize(requireContext(), attach)
            }
        )
        lvAttaches.adapter = listAdapterAttaches
    }

    fun resetListAdapters() {
        listAdapterRecords.reset()
        listAdapterAttaches.reset()
    }

    fun showView(viewType: MainViewType) {
        fabAddRecord.hide()
        fabAddAttach.hideMenu(true)
        var whichChild = viewType
        var title: String? = null
        when (viewType) {
            MainViewType.NONE,
            MainViewType.TAG_RECORDS -> {
                whichChild = MainViewType.NODE_RECORDS
            }
            MainViewType.NODE_RECORDS -> {
                fabAddRecord.show()
            }
            MainViewType.RECORD_ATTACHES -> {
                if (!viewModel.isLoadedFavoritesOnly()) {
                    fabAddAttach.showMenu(true)
                }
                title = viewModel.curRecord?.name.orEmpty()
            }
            MainViewType.FAVORITES -> Unit
        }
        viewModel.updateToolbar(page = PageType.MAIN, viewType = viewType, title)
        viewFlipperMain.displayedChild = whichChild.index - 1
    }

    /**
     * Возврат фрагмента в первоначальное состояние.
     */
    fun clearView() {
        viewModel.showMainView(MainViewType.NONE)
        lvRecords.adapter = null
        lvAttaches.adapter = null
        tvRecordsEmpty.setText(R.string.title_select_the_node)
        showGlobalSearchButton(false)
    }

    // region Records

    /**
     * Отображение списка записей.
     */
    fun showRecords(records: List<TetroidRecord>, viewType: MainViewType) {
        viewModel.showMainView(viewType)
        tvRecordsEmpty.setText(
            if (viewType == MainViewType.FAVORITES) {
                R.string.title_favors_is_missing
            } else {
                R.string.title_records_is_missing
            }
        )
        showGlobalSearchButton(false)
        listAdapterRecords.setDataItems(records, viewType)
        lvRecords.adapter = listAdapterRecords
    }

    fun scrollToRecord(record: TetroidRecord) {
        val position = listAdapterRecords.getItemPositionById(recordId = record.id)
        if (position > -1) {
            lvRecords.postDelayed(
                { lvRecords.smoothScrollToPosition(position) }, 300
            )
        }
    }

    /**
     * Обновление списка записей.
     */
    fun updateRecordList(records: List<TetroidRecord>, currentViewType: MainViewType) {
        listAdapterRecords.setDataItems(records, viewType = currentViewType)
    }

    fun onRecordsFiltered(query: String?, found: List<TetroidRecord>, viewType: MainViewType) {
        if (found.isEmpty()) {
            val emptyText = if (viewType == MainViewType.NODE_RECORDS) {
                getString(R.string.search_records_in_node_not_found_mask, query, viewModel.getCurNodeName())
            } else {
                getString(R.string.search_records_in_tag_not_found_mask, query, viewModel.getSelectedTagsNames())
            }
            setRecordsEmptyViewText(emptyText)
            showGlobalSearchButton(true)
        } else {
            showGlobalSearchButton(false)
        }
    }

    // endregion Records

    // region Record

    /**
     * Отображение записи
     * @param position Индекс записи в списке записей
     */
    private fun showRecord(position: Int) {
        val record = listAdapterRecords.getItem(position) as TetroidRecord
        showRecord(record)
    }

    fun showRecord(record: TetroidRecord) {
        viewModel.openRecord(record)
    }

    fun onRecordOpened(recordId: String) {
        listAdapterRecords.lastOpenedRecordId = recordId
        listAdapterRecords.notifyDataSetChanged()
    }

    /**
     * Создание новой записи.
     */
    fun createRecord() {
        RecordFieldsDialog(
            record = null,
            chooseNode = true,
            node = viewModel.curNode,
            storageId = viewModel.getStorageId(),
            onApply = { name, tags, author, url, node, isFavorite ->
                viewModel.createRecord(
                    name = name,
                    tags = tags,
                    author = author,
                    url = url,
                    node = node,
                    isFavorite = isFavorite
                )
            },
        ).showIfPossible(parentFragmentManager)
    }

    /**
     * Удаление записи.
     * @param record
     */
    private fun deleteRecord(record: TetroidRecord) {
        AskDialogs.showYesDialog(
            context = requireContext(),
            message = getString(R.string.ask_record_delete_mask, record.name),
            onApply = {
                viewModel.deleteRecord(record)
            },
        )
    }

    /**
     * Обработка результата удаления записи.
     * @param record
     */
    fun onDeleteRecordResult(record: TetroidRecord) {
        lvAttaches.adapter = null
    }

    /**
     * Перемещение записи вверх/вниз по списку.
     * @param pos
     * @param isUp
     */
    private fun reorderRecord(pos: Int, isUp: Boolean) {
        viewModel.reorderRecords(
            pos = pos,
            isUp = isUp
        )
    }

    /**
     * Редактирование свойств записи.
     * @param record
     */
    private fun editRecordFields(record: TetroidRecord) {
        RecordFieldsDialog(
            record = record,
            chooseNode = true,
            node = viewModel.curNode,
            storageId = viewModel.getStorageId()
        ) { name: String, tags: String, author: String, url: String, node: TetroidNode, isFavor: Boolean ->
            viewModel.editRecordFields(
                record = record,
                name = name,
                tags = tags,
                author = author,
                url = url,
                node = node,
                isFavor = isFavor
            )
        }.showIfPossible(parentFragmentManager)
    }

    private fun showRecordInfoDialog(record: TetroidRecord) {
        RecordInfoDialog(
            record = record,
            storageId = viewModel.getStorageId()
        ).showIfPossible(parentFragmentManager)
    }

    // endregion Record

    // region Attaches

    fun showAttaches(files: List<TetroidFile>) {
        viewModel.showMainView(MainViewType.RECORD_ATTACHES)
        setAttachesList(files)
        lvAttaches.adapter = listAdapterAttaches
    }

    // endregion Attaches
    
    // region Attach
    
    /**
     * Открытие прикрепленного файла.
     * @param position Индекс файла в списке прикрепленных файлов записи
     */
    private fun openAttach(position: Int) {
        viewModel.curRecord?.attachedFiles?.getOrNull(position)?.also { attach ->
            viewModel.checkPermissionIfNeedAndOpenAttach(activity = requireActivity(), attach)
        }
    }

    /**
     * Перемещение файла вверх/вниз по списку.
     * @param pos
     * @param isUp
     */
    private fun reorderAttach(pos: Int, isUp: Boolean) {
        viewModel.reorderAttaches(
            pos = pos,
            isUp = isUp
        )
    }

    /**
     * Удаление прикрепленного файла.
     * @param attach
     */
    private fun deleteAttach(attach: TetroidFile) {
        AskDialogs.showYesDialog(
            context = requireContext(),
            message = getString(R.string.ask_file_delete_mask, attach.name),
            onApply = {
                viewModel.deleteAttach(attach)
            },
        )
    }

    /**
     * Обработка результата удаления файла.
     * @param attach
     */
    fun onDeleteAttachResult(attach: TetroidFile) {
        listAdapterAttaches.notifyDataSetChanged()
    }

    /**
     * Переименование прикрепленного файла.
     * @param attach
     */
    private fun renameAttach(attach: TetroidFile) {
        AttachFieldsDialog(
            file = attach
        ) { name: String ->
            viewModel.renameAttach(attach, name)
        }
            .showIfPossible(parentFragmentManager)
    }

    fun showAttachInfoDialog(attach: TetroidFile) {
        AttachInfoDialog(
            attach = attach,
            storageId = viewModel.getStorageId()
        ).showIfPossible(parentFragmentManager)
    }

    fun updateAttachesList() {
        listAdapterAttaches.notifyDataSetChanged()
    }

    fun setAttachesList(data: List<TetroidFile>) {
        listAdapterAttaches.reset(data)
    }

    // endregion Attach

    // region ContextMenu

    /**
     * Обработчик создания контекстного меню при долгом тапе на записи.
     * @param menu
     * @param v
     * @param menuInfo
     */
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val viewId = v.id
        val inflater = requireActivity().menuInflater
        val adapterMenuInfo = menuInfo as? AdapterView.AdapterContextMenuInfo
        if (viewId == R.id.list_view_records) {
            inflater.inflate(R.menu.record_context, menu)
            prepareRecordsContextMenu(menu, adapterMenuInfo)
        } else if (viewId == R.id.list_view_files) {
            inflater.inflate(R.menu.attach_context, menu)
            prepareFilesContextMenu(menu, adapterMenuInfo)
        }
    }

    /**
     * @param menu
     * @param menuInfo
     */
    private fun prepareRecordsContextMenu(menu: Menu, menuInfo: AdapterView.AdapterContextMenuInfo?) {
        if (menuInfo == null) return
        val isPro = viewModel.buildInfoProvider.isFullVersion()
        val isLoadedFavoritesOnly = viewModel.isLoadedFavoritesOnly()
        val isFavoritesView = viewModel.currentMainViewType == MainViewType.FAVORITES
        var isNonCrypted = false
        val record = listAdapterRecords.getItem(menuInfo.position) as? TetroidRecord
        if (record != null) {
            isNonCrypted = record.isNonCryptedOrDecrypted
            if (!isNonCrypted) {
                activateMenuItem(menu.findItem(R.id.action_copy), false)
                activateMenuItem(menu.findItem(R.id.action_cut), false)
                activateMenuItem(menu.findItem(R.id.action_attached_files), false)
                activateMenuItem(menu.findItem(R.id.action_open_record_folder), false)
                activateMenuItem(menu.findItem(R.id.action_copy_link), false)
                activateMenuItem(menu.findItem(R.id.action_info), false)
            }
            val isFavorite = record.isFavorite
            activateMenuItem(menu.findItem(R.id.action_add_favorite), isPro && !isFavoritesView && !isFavorite)
            activateMenuItem(menu.findItem(R.id.action_remove_favorite), isPro && isFavoritesView || isFavorite)
        }
        activateMenuItem(menu.findItem(R.id.action_record_edit_fields), !isLoadedFavoritesOnly && isNonCrypted)
        activateMenuItem(menu.findItem(R.id.action_delete), !isLoadedFavoritesOnly)
        activateMenuItem(
            menu.findItem(R.id.action_record_node),
            !isLoadedFavoritesOnly && isFavoritesView && isNonCrypted
        )
        activateMenuItem(
            menu.findItem(R.id.action_insert),
            !isFavoritesView && ClipboardManager.hasObject(FoundType.TYPE_RECORD)
        )
        val recordsCount = listAdapterRecords.count
        activateMenuItem(menu.findItem(R.id.action_move_up), recordsCount > 0)
        activateMenuItem(menu.findItem(R.id.action_move_down), recordsCount > 0)
    }

    /**
     * @param menu
     * @param menuInfo
     */
    private fun prepareFilesContextMenu(menu: Menu, menuInfo: AdapterView.AdapterContextMenuInfo?) {
        if (menuInfo == null) return
        val isLoadedFavoritesOnly = viewModel.isLoadedFavoritesOnly()
        activateMenuItem(menu.findItem(R.id.action_rename), !isLoadedFavoritesOnly)
        val filesCount = listAdapterAttaches.count
        activateMenuItem(menu.findItem(R.id.action_move_up), !isLoadedFavoritesOnly && filesCount > 0)
        activateMenuItem(menu.findItem(R.id.action_move_down), !isLoadedFavoritesOnly && filesCount > 0)
        val file = listAdapterAttaches.getItem(menuInfo.position) as? TetroidFile
        val attachFileSize = file?.let { viewModel.getAttachFileSize(requireContext(), it) }
        activateMenuItem(menu.findItem(R.id.action_save_as), !attachFileSize.isNullOrEmpty())
        activateMenuItem(menu.findItem(R.id.action_delete), !isLoadedFavoritesOnly)
    }

    private fun activateMenuItem(menuItem: MenuItem, isActivate: Boolean) {
        menuItem.setVisible(isActivate)
    }

    /**
     * Обработчик выбора пунктов контекстного меню записи.
     * @param item
     * @return
     */
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val id = item.itemId
        val pos = info.position
        if (info.targetView.parent === lvRecords) {
             onContextRecordItemSelected(id, pos)
        } else if (info.targetView.parent === lvAttaches) {
            return onContextFileItemSelected(id, pos)
        }
        return super.onContextItemSelected(item)
    }

    private fun onContextRecordItemSelected(id: Int, pos: Int): Boolean {
        val record = listAdapterRecords.getItem(pos) as? TetroidRecord
        if (record == null) {
            viewModel.logError(getString(R.string.log_get_item_is_null), true)
            return true
        }
        return when (id) {
            R.id.action_open_record -> {
                showRecord(record)
                true
            }
            R.id.action_record_edit_fields -> {
                editRecordFields(record)
                true
            }
            R.id.action_record_node -> {
                viewModel.showNode(record.node)
                true
            }
            R.id.action_copy -> {
                viewModel.copyRecord(record)
                true
            }
            R.id.action_cut -> {
                viewModel.cutRecord(record)
                true
            }
            R.id.action_insert -> {
                viewModel.insertRecord()
                true
            }
            R.id.action_attached_files -> {
                viewModel.showRecordAttaches(record)
                true
            }
            R.id.action_open_record_folder -> {
                viewModel.openRecordFolder(activity = requireActivity(), record)
                true
            }
            R.id.action_copy_link -> {
                viewModel.copyRecordLink(record)
                true
            }
            R.id.action_move_up -> {
                reorderRecord(pos, true)
                true
            }
            R.id.action_move_down -> {
                reorderRecord(pos, false)
                true
            }
            R.id.action_add_favorite -> {
                viewModel.addToFavorite(record)
                true
            }
            R.id.action_remove_favorite -> {
                viewModel.removeFromFavorite(record)
                true
            }
            R.id.action_info -> {
                showRecordInfoDialog(record)
                true
            }
            R.id.action_delete -> {
                deleteRecord(record)
                true
            }
            else -> false
        }
    }

    private fun onContextFileItemSelected(id: Int, pos: Int): Boolean {
        val attach = listAdapterAttaches.getItem(pos) as? TetroidFile
        if (attach == null) {
            viewModel.logError(getString(R.string.log_get_item_is_null), true)
            return true
        }
        return when (id) {
            R.id.action_open_file -> {
                openAttach(pos)
                true
            }
            R.id.action_rename -> {
                renameAttach(attach)
                true
            }
            R.id.action_copy_link ->
                // TODO: реализовать ссылки на файлы
//                viewModel.copyAttachLink();
                true
            R.id.action_save_as -> {
                viewModel.pickFolderAndSaveAttachToFile(attach)
                true
            }
            R.id.action_move_up -> {
                reorderAttach(pos, true)
                true
            }
            R.id.action_move_down -> {
                reorderAttach(pos, false)
                true
            }
            R.id.action_info -> {
                showAttachInfoDialog(attach)
                true
            }
            R.id.action_delete -> {
                deleteAttach(attach)
                true
            }
            else -> false
        }
    }

    // endregion ContextMenu

    fun onProgressVisibilityChanged(isVisible: Boolean) {
        tvRecordsEmpty.isVisible = !isVisible
    }

    fun onBackPressed(): Boolean {
        val curView = viewFlipperMain.displayedChild + 1
        return viewModel.onMainViewBackPressed(curView)
    }

    fun setRecordsEmptyViewText(text: String?) {
        tvRecordsEmpty.text = text
    }

    fun showGlobalSearchButton(isVisible: Boolean) {
        btnUseGlobalSearch.isVisible = isVisible
    }

    fun onAttachesFiltered(query: String?, found: List<TetroidFile?>) {
        if (found.isEmpty()) {
            tvFilesEmpty.text = getString(R.string.search_files_not_found_mask, query)
        }
    }

}