package com.gee12.mytetroid.viewmodels

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.FileObserver
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.lifecycle.viewModelScope
import com.gee12.mytetroid.*
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.helpers.UriHelper
import com.gee12.mytetroid.interactors.SearchInteractor
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.logs.TaskStage
import com.gee12.mytetroid.logs.TaskStage.Stages
import com.gee12.mytetroid.logs.TetroidLog.*
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.services.FileObserverService
import com.gee12.mytetroid.utils.Utils
import com.gee12.mytetroid.views.Message
import com.gee12.mytetroid.views.activities.TetroidActivity.IDownloadFileResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.util.ArrayList
import java.util.HashMap

class MainViewModel(
    app: Application,
    storagesRepo: StoragesRepo
): StorageViewModel/*<Constants.MainEvents>*/(app, storagesRepo) {

//    val objectAction: SingleLiveEvent<ViewModelEvent<Constants.MainEvents, Any>> = SingleLiveEvent()
//
//    fun doAction(action: Constants.MainEvents, param: Any? = null) {
//        objectAction.postValue(ViewModelEvent(action, param))
//    }

    var curMainViewId = Constants.MAIN_VIEW_NONE
    var lastMainViewId = 0

    var curNode: TetroidNode? = null
    var curRecord: TetroidRecord? = null
    var curTag: TetroidTag? = null
    var curFile: TetroidFile? = null

    var tempFileToOpen: TetroidFile? = null
    var isDropRecordsFiltering = true
//    var lastScan: ScanManager? = null
    var lastSearchProfile: SearchProfile? = null
    var lastFilterQuery: String? = null
    var isFromRecordActivity = false
    private var isStorageChangingHandled = false


    //region Pages

    fun openPage(pageId: Int) {
        makeViewEvent(Constants.ViewEvents.OpenPage, pageId)
    }

    fun onMainPageCreated() {
        makeViewEvent(Constants.ViewEvents.MainPageCreated)
    }

    fun showMainView(viewId: Int) {
        // сохраняем значение для возврата на старое View
        // (только, если осуществляется переключение на действительно другую вьюшку)
        if (viewId != curMainViewId) {
            this.lastMainViewId = curMainViewId
        }
        makeViewEvent(Constants.ViewEvents.ShowMainView, viewId)
        this.curMainViewId = viewId
    }

    fun closeFoundFragment() {
        makeViewEvent(Constants.ViewEvents.CloseFoundView)
    }

    /**
     * Восстанавливаем состояние Toolbar при переключении обратно к фрагменту MainPageFragment.
     */
    fun restoreLastMainToolbarState() {
        var title: String? = null
        val restoredViewId = curMainViewId
        if (restoredViewId == Constants.MAIN_VIEW_RECORD_FILES) {
            title = curRecord?.name ?: ""
        }
        updateToolbar(restoredViewId, title)
    }

    fun updateToolbar(viewId: Int, title: String?) {
        makeViewEvent(Constants.ViewEvents.UpdateToolbar, ToolbarParams(viewId, title))
    }

    //endregion Pages

    //region Records

    fun getCurNodeName() = curNode?.name ?: ""

    fun showRecords(records: List<TetroidRecord>, viewId: Int, dropSearch: Boolean = true) {
        makeEvent(Constants.MainEvents.ShowRecords, ObjectsInView(records, viewId, dropSearch))
    }

    /**
     * Вставка записи в ветку.
     */
    fun insertRecord() {
        viewModelScope.launch {
            // на всякий случай проверяем тип
            if (!TetroidClipboard.hasObject(FoundType.TYPE_RECORD))
                return@launch
            // достаем объект из "буфера обмена"
            val clipboard = TetroidClipboard.getInstance()
            // вставляем с попыткой восстановить каталог записи
            val record = clipboard.getObject() as TetroidRecord
            val isCutted = clipboard.isCutted
            val res = insertRecord(record, isCutted, false)
            when (res) {
                -1 -> {
                    makeEvent(Constants.MainEvents.AskForOperationWithoutDir, ClipboardParams(Opers.INSERT, record, isCutted))
                }
                -2 -> logOperErrorMore(getContext(), Objs.RECORD_DIR, Opers.INSERT)
                else -> onInsertRecordResult(record, res, isCutted)
            }
        }
    }

    private suspend fun insertRecord(record: TetroidRecord, isCutted: Boolean, withoutDir: Boolean): Int {
        if (curNode == null) return -2;
        return recordsInteractor.insertRecord(getContext(), record, isCutted, curNode!!, withoutDir)
    }

    private fun onInsertRecordResult(record: TetroidRecord, res: Int, isCutted: Boolean) {
        if (res > 0) {
            logOperRes(getContext(), Objs.RECORD, Opers.INSERT)
            updateRecords()
            updateNodes()
            updateTags()
            if (isCutted) {
                // очищаем "буфер обмена"
                TetroidClipboard.clear()
                // обновляем избранное
                updateFavorites(record)
            }
        } else {
            logOperErrorMore(getContext(), Objs.RECORD, Opers.INSERT)
        }
    }

    /**
     * Создание новой записи.
     */
    fun createRecord(name: String, tags: String, author: String, url: String, node: TetroidNode?, isFavor: Boolean) {
        if (node == null) return
        viewModelScope.launch {
            val record = recordsInteractor.createRecord(getContext(), name, tags, author, url, node, isFavor)
            if (record != null) {
                logOperRes(getContext(), Objs.RECORD, Opers.CREATE, record, false)
                updateTags()
                updateNodes()
                if (!updateFavorites(record)) {
                    updateRecords()
                }
                openRecord(record)
            } else {
                logOperErrorMore(getContext(), Objs.RECORD, Opers.CREATE)
            }
        }
    }

    /**
     * Создание новой записи для вставки объекта из другого приложения.
     * @param intent
     * @param isText
     * @param text
     * @param imagesUri
     * @param receivedData
     */
    fun createRecordFromIntent(intent: Intent, isText: Boolean, text: String, imagesUri: ArrayList<Uri>, receivedData: ReceivedData) {
        viewModelScope.launch {
            // имя записи
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            var url: String? = null
            if (Build.VERSION.SDK_INT >= 17) {
                url = intent.getStringExtra(Intent.EXTRA_ORIGINATING_URI)
            }
            // создаем запись
            val node = if (quicklyNode != null) quicklyNode!! else TetroidXml.ROOT_NODE
            val record: TetroidRecord = recordsInteractor.createTempRecord(getContext(), subject, url, text, node) ?: return@launch
            if (isText) {
                // запускаем активность просмотра записи
                openRecord(record.id)
            } else {
                // загружаем изображения в каталоги записи
                if (!receivedData.isAttach) {
                    // запускаем активность просмотра записи с командой вставки изображений после загрузки
                    openRecordWithImages(record.id, imagesUri)
                } else {
                    // прикрепляем изображения как файлы
                    var hasError = false
                    val uriHelper = UriHelper(getContext())
                    for (uri in imagesUri) {
                        if (attachesInteractor.attachFile(getContext(), uriHelper.getPath(uri), record) == null) {
                            hasError = true
                        }
                    }
                    if (hasError) {
                        LogManager.log(getContext(), R.string.log_files_attach_error, ILogger.Types.WARNING, Toast.LENGTH_LONG)
                        makeViewEvent(Constants.ViewEvents.ShowMoreInLogs)
                    }
                    // запускаем активность записи, к которой уже будут прикреплены файлы
                    openRecordWithAttachedFiles(record.id)
                    // обновляем список файлов
//                mDrawerLayout.closeDrawers();
//                mViewPagerAdapter.getMainFragment().showRecordFiles(record);
                }
            }
        }
    }

    /**
     * Удаление записи.
     */
    fun deleteRecord(record: TetroidRecord?) {
        if (record == null) return
        viewModelScope.launch {
            val res = deleteRecord(record, false)
            if (res == -1) {
                makeEvent(Constants.MainEvents.AskForOperationWithoutDir, ClipboardParams(Opers.DELETE, record))
            } else {
                onDeleteRecordResult(record, res, false)
            }
        }
    }

    /**
     * Обработка результата удаления записи.
     * @param record
     * @param res
     */
    private fun onDeleteRecordResult(record: TetroidRecord, res: Int, isCutted: Boolean) {
        if (res > 0) {
            makeEvent(Constants.MainEvents.RecordDeleted, record)
            updateTags()
            updateNodes()
            // обновляем избранное
            updateFavorites(record)
            curRecord = null
            logOperRes(getContext(), Objs.RECORD, if (isCutted) Opers.CUT else Opers.DELETE)
            // переходим в список записей ветки после удаления
            // (запись может быть удалена при попытке просмотра/изменения файла, например)
            if (curMainViewId != Constants.MAIN_VIEW_NODE_RECORDS && curMainViewId != Constants.MAIN_VIEW_FAVORITES) {
                showMainView(Constants.MAIN_VIEW_NODE_RECORDS)
            }
        } else if (res == -2 && !isCutted) {
            log(getContext(), R.string.log_erorr_move_record_dir_when_del, ILogger.Types.WARNING, Toast.LENGTH_LONG)
            makeViewEvent(Constants.ViewEvents.ShowMoreInLogs)
        } else {
            logOperErrorMore(getContext(), Objs.RECORD, if (isCutted) Opers.CUT else Opers.DELETE)
        }
    }

    override fun editRecordFields(record: TetroidRecord, name: String, tags: String, author: String, url: String, node: TetroidNode, isFavor: Boolean) {
        viewModelScope.launch {
            val oldNode = record.node
            if (recordsInteractor.editRecordFields(getContext(), record, name, tags, author, url, node, isFavor)) {
                onRecordFieldsUpdated(record, oldNode !== record.node)
//                TetroidLog.logOperRes(TetroidLog.Objs.FILE_FIELDS, TetroidLog.Opers.CHANGE);
                log(getContext(), R.string.log_record_fields_changed, ILogger.Types.INFO, Toast.LENGTH_SHORT)
            } else {
                logOperErrorMore(getContext(), Objs.RECORD_FIELDS, Opers.CHANGE)
            }
        }
    }

    /**
     * Обновление списка записей и меток после изменения свойств записи.
     */
    fun onRecordFieldsUpdated(record: TetroidRecord?, nodeChanged: Boolean) {
        if (nodeChanged && record != null) {
            showNode(record.node)
        } else {
            updateRecords()
        }
        makeEvent(Constants.MainEvents.UpdateTags)
        updateFavorites(record)
    }

    /**
     * Копирование записи.
     * @param record
     */
    fun copyRecord(record: TetroidRecord) {
        // добавляем в "буфер обмена"
        TetroidClipboard.copy(record)
        logOperRes(getContext(), Objs.RECORD, Opers.COPY)
    }

    /**
     * Перемещение записи вверх/вниз по списку.
     * @param records
     * @param pos
     * @param isUp
     */
    fun reorderRecord(records: List<TetroidRecord>, pos: Int, isUp: Boolean) {
        val res = if (curMainViewId == Constants.MAIN_VIEW_FAVORITES)
            FavoritesManager.swapRecords(getContext(), pos, isUp, true)
            else swapTetroidObjects(records, pos, isUp,true)
        if (res > 0) {
            updateRecords()
            logOperRes(getContext(), Objs.RECORD, Opers.REORDER)
        } else if (res < 0) {
            logOperErrorMore(getContext(), Objs.RECORD, Opers.REORDER)
        }
    }

    /**
     * Копирование ссылки на запись в буфер обмена.
     * @param record
     */
    fun copyRecordLink(record: TetroidRecord?) {
        if (record != null) {
            val url = record.createUrl()
            Utils.writeToClipboard(getContext(), getString(R.string.link_to_record), url)
            log(getContext(), getString(R.string.title_link_was_copied) + url, ILogger.Types.INFO, Toast.LENGTH_SHORT)
        } else {
            log(getContext(), getString(R.string.log_get_item_is_null), ILogger.Types.ERROR, Toast.LENGTH_LONG)
        }
    }

    /**
     * Вырезание записи из ветки.
     * @param record
     */
    fun cutRecord(record: TetroidRecord) {
        viewModelScope.launch {
            // добавляем в "буфер обмена"
            TetroidClipboard.cut(record)
            // удаляем запись из текущей ветки и каталог перемещаем в корзину
            val res: Int = cutRecord(record, false)
            if (res == -1) {
                makeEvent(Constants.MainEvents.AskForOperationWithoutDir, ClipboardParams(Opers.CUT, record))
//                }
            } else {
                onDeleteRecordResult(record, res, true)
            }
        }
    }

    /**
     * Операции с объектами хранилища, когда каталог записи отсутствует.
     */
    fun doOperationWithoutDir(params: ClipboardParams) {
        viewModelScope.launch {
            when (params.operation) {
                Opers.INSERT -> {
                    val record = params.obj as TetroidRecord
                    val res: Int = insertRecord(record, params.isCutted, true)
                    onInsertRecordResult(record, res, params.isCutted)
                }
                Opers.DELETE -> {
                    if (params.obj is TetroidRecord) {
                        val record = params.obj
                        val res: Int = deleteRecord(record, true)
                        onDeleteRecordResult(record, res, false)
                    } else if (params.obj is TetroidFile) {
                        val record = params.obj.record
                        val res = deleteRecord(record, true)
                        onDeleteRecordResult(record, res, false)
                    }
                }
                Opers.CUT -> {
                    val record = params.obj as TetroidRecord
                    val res: Int = cutRecord(record, true)
                    onDeleteRecordResult(record, res, true)
                }
                else -> {}
            }
        }
    }

    /**
     * Открытие записи.
     * Реализация метода интерфейса IMainView.
     * @param record
     */
    fun openRecord(record: TetroidRecord?) {
        if (record == null) {
            LogManager.log(getContext(), R.string.log_record_is_null, ILogger.Types.ERROR, Toast.LENGTH_LONG)
            return
        }
        // проверка нужно ли расшифровать избранную запись перед отображением
        // (т.к. в избранной ветке записи могут быть нерасшифрованные)
        if (!onRecordDecrypt(record)) {
            openRecord(record.id)
        }
    }

    /**
     * Открытие записи по Id.
     * @param recordId
     */
    fun openRecord(recordId: String) {
        val bundle = Bundle()
        bundle.putString(Constants.EXTRA_OBJECT_ID, recordId)
        openRecord(bundle)
    }

    /**
     * Открытие записи с последующим добавлением в ее содержимое изображений.
     * @param recordId
     * @param imagesUri
     */
    private fun openRecordWithImages(recordId: String, imagesUri: ArrayList<Uri>) {
        val bundle = Bundle()
        bundle.putString(Constants.EXTRA_OBJECT_ID, recordId)
        bundle.putParcelableArrayList(Constants.EXTRA_IMAGES_URI, imagesUri)
        openRecord(bundle)
    }

    /**
     *
     * @param recordId
     */
    private fun openRecordWithAttachedFiles(recordId: String) {
        val bundle = Bundle()
        bundle.putString(Constants.EXTRA_OBJECT_ID, recordId)
        bundle.putString(Constants.EXTRA_ATTACHED_FILES, "")
        openRecord(bundle)
    }

    /**
     * Открытие активности RecordActivity.
     * @param bundle
     */
    private fun openRecord(bundle: Bundle) {
        makeEvent(Constants.MainEvents.OpenRecord, bundle)
    }

    /**
     * Открытие каталога записи.
     * @param record
     */
    fun openRecordFolder(record: TetroidRecord?) {
        if (record == null) return
        if (!recordsInteractor.openRecordFolder(getContext(), record)) {
            LogManager.log(getContext(), R.string.log_missing_file_manager, Toast.LENGTH_LONG)
        }
    }

    private fun updateRecords() {
        makeEvent(Constants.MainEvents.UpdateRecords)
    }

    //endregion Records

    //region Nodes

    /**
     * Открытие записей ветки.
     * @param node
     */
    fun showNode(node: TetroidNode) {
        // проверка нужно ли расшифровать ветку перед отображением
        if (onNodeDecrypt(node)) return
        LogManager.log(getContext(), getString(R.string.log_open_node) + getIdString(getContext(), node))
        curNode = node
        makeEvent(Constants.MainEvents.ShowNode, node)
        showRecords(node.records, Constants.MAIN_VIEW_NODE_RECORDS)

        // сохраняем выбранную ветку
        saveLastSelectedNode()
    }

    /**
     * Открытие ветки записи.
     * Если активен режим "Только избранное", то открытие списка избранных записей.
     */
    fun showRecordNode(record: TetroidRecord?) {
        when {
            isLoadedFavoritesOnly() -> showFavorites()
            record != null -> showNode(record.node)
            curNode != null -> showNode(curNode!!)
        }
    }

    /**
     * Сохранение последней выбранной ветки.
     */
    private fun saveLastSelectedNode() {
        if (isKeepLastNode()) {
            val curNode = if (curMainViewId == Constants.MAIN_VIEW_FAVORITES) FavoritesManager.FAVORITES_NODE else curNode
            SettingsManager.setLastNodeId(getContext(), curNode?.id)
        }
    }

    fun createNode(name: String, trueParentNode: TetroidNode?) {
        viewModelScope.launch {
            val node = nodesInteractor.createNode(getContext(), name, trueParentNode)
            if (node != null) {
                makeEvent(Constants.MainEvents.NodeCreated, node)
            } else {
                logOperErrorMore(getContext(), Objs.NODE, Opers.CREATE)
            }
        }
    }

    /**
     * Переименование ветки.
     * @param node
     */
    fun renameNode(node: TetroidNode, newName: String) {
        viewModelScope.launch {
            if (nodesInteractor.editNodeFields(getContext(), node, newName)) {
                logOperRes(getContext(), Objs.NODE, Opers.RENAME)
//            if (mCurNode === node) {
//                setTitle(name)
//            }
                updateNodes();
                makeEvent(Constants.MainEvents.NodeRenamed, node)
            } else {
                logOperErrorMore(getContext(), Objs.NODE, Opers.RENAME)
            }
        }
    }

    fun setNodeIcon(nodeId: String?, iconPath: String?, isDrop: Boolean) {
        if (nodeId == null) return
        viewModelScope.launch {
            val node = if (curNode?.id == nodeId) curNode else nodesInteractor.getNode(nodeId)
            if (nodesInteractor.setNodeIcon(getContext(), node, iconPath, isDrop)) {
                logOperRes(getContext(), Objs.NODE, Opers.CHANGE)
                updateNodes()
            } else {
                logOperErrorMore(getContext(), Objs.NODE, Opers.CHANGE)
            }
        }
    }

    /**
     * Вставка ветки.
     * @param parentNode Родительская ветка
     * @param pos        Позиция в списке родительской ветки
     * @param isSubNode  Если true, значит как подветка, иначе рядом с выделенной веткой
     */
    fun insertNode(parentNode: TetroidNode, isSubNode: Boolean) {
        // на всякий случай проверяем тип
        if (!TetroidClipboard.hasObject(FoundType.TYPE_NODE)) return
        // достаем объект из "буфера обмена"
        val clipboard = TetroidClipboard.getInstance()
        // вставляем с попыткой восстановить каталог записи
        val node = clipboard.getObject() as TetroidNode
        val isCutted = clipboard.isCutted
        val trueParentNode = if (isSubNode) parentNode else parentNode.parentNode

        viewModelScope.launch {
            if (nodesInteractor.insertNode(getContext(), node, trueParentNode, isCutted)) {
                makeEvent(Constants.MainEvents.NodeInserted, node)
            } else {
                logOperErrorMore(getContext(), Objs.NODE, Opers.INSERT)
            }
        }
    }

    /**
     * Вырезание ветки из родительской ветки.
     * @param node
     */
    fun cutNode(node: TetroidNode, pos: Int) {
        if (nodesInteractor.hasNonDecryptedNodes(node)) {
            Message.show(getContext(), getString(R.string.log_enter_pass_first), Toast.LENGTH_LONG)
            return
        }
        // добавляем в "буфер обмена"
        TetroidClipboard.cut(node)
        viewModelScope.launch {
            // удаляем ветку из родительской ветки вместе с записями
            val res = nodesInteractor.cutNode(getContext(), node)
            onDeleteNodeResult(node, res, true)
        }
    }

    /**
     * Удаление ветки.
     * @param node
     */
    fun startDeleteNode(node: TetroidNode?) {
        if (node == null) return
        // запрет на удаление последней ветки в корне
        if (node.level == 0 && getRootNodes().size == 1) {
            LogManager.log(getContext(), R.string.log_cannot_delete_root_node, ILogger.Types.INFO, Toast.LENGTH_SHORT)
            return
        }
        makeEvent(Constants.MainEvents.AskForDeleteNode, node)
    }

    fun deleteNode(node: TetroidNode) {
        viewModelScope.launch {
            val res = nodesInteractor.deleteNode(getContext(), node)
            onDeleteNodeResult(node, res, false)
        }
    }

    private fun onDeleteNodeResult(node: TetroidNode, res: Boolean, isCutted: Boolean) {
        if (res) {
            makeEvent(if (isCutted) Constants.MainEvents.NodeCutted else Constants.MainEvents.NodeDeleted, node)
            // удаляем элемент внутри списка
//            if (mListAdapterNodes.deleteItem(pos)) {
//                logOperRes(this, Objs.NODE, if (!isCutted) Opers.DELETE else Opers.CUT)
//            } else {
//                LogManager.log(this, getString(R.string.log_node_delete_list_error), ILogger.Types.ERROR, Toast.LENGTH_LONG)
//            }
            // обновляем label с количеством избранных записей
            if (App.isFullVersion()) {
                makeEvent(Constants.MainEvents.UpdateFavorites)
            }
            // убираем список записей удаляемой ветки
            if (curNode == node || nodesInteractor.isNodeInNode(curNode, node)) {
//                getMainPage().clearView()
                curRecord = null
                curNode = null
                makeViewEvent(Constants.ViewEvents.ClearMainView)
            }
            if (node.isCrypted) {
                // проверяем существование зашифрованных веток
                checkExistenceCryptedNodes()
            }
        } else {
            logOperErrorMore(getContext(), Objs.NODE, if (!isCutted) Opers.DELETE else Opers.CUT)
        }
    }

    fun encryptNode(node: TetroidNode) {
        startEncryptDecryptNode(node, true)
    }

    fun decryptNode(node: TetroidNode) {
        startEncryptDecryptNode(node, false)
    }

    /**
     * Длительная операция по зашифровке/сбросе шифровки веток.
     */
    private fun startEncryptDecryptNode(node: TetroidNode, isEncrypt: Boolean) {
        makeViewEvent(Constants.ViewEvents.TaskStarted,
            if (isEncrypt) R.string.task_node_encrypting else R.string.task_node_drop_crypting)
        logOperStart(getContext(), Objs.NODE, if (isEncrypt) Opers.ENCRYPT else Opers.DROPCRYPT, node)

        val nodeWasEncrypted = node.isCrypted
        val operation = if (isEncrypt) Opers.ENCRYPT else Opers.DROPCRYPT

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                // сначала расшифровываем хранилище
                if (isCrypted()) {
                    setStage(Objs.STORAGE, Opers.DECRYPT, Stages.START)
                    if (cryptInteractor.decryptStorage(getContext(), false)) {
                        setStage(Objs.STORAGE, Opers.DECRYPT, Stages.SUCCESS)
                    } else {
                        setStage(Objs.STORAGE, Opers.DECRYPT, Stages.FAILED)
                        return@withContext -2
                    }
                }
                // только если хранилище расшифровано
                return@withContext if (isDecrypted()) {
                    setStage(Objs.NODE, operation, Stages.START)
                    val res = if (isEncrypt) cryptInteractor.encryptNode(getContext(), node)
                    else cryptInteractor.dropCryptNode(getContext(), node)
                    if (res) 1 else -1
                } else 0
            }

            makeViewEvent(Constants.ViewEvents.TaskFinished)
            if (result > 0) {
                logOperRes(getContext(), Objs.NODE, operation)
                if (!isEncrypt && nodeWasEncrypted) {
                    // проверяем существование зашифрованных веток
                    checkExistenceCryptedNodes()
                }
            } else {
                logOperErrorMore(getContext(), Objs.NODE, operation)
            }
            afterStorageDecrypted(node)
        }
    }

    override fun afterStorageDecrypted(node: TetroidNode?) {
        super.afterStorageDecrypted(node)
        if (node != null) {
            if (node === FavoritesManager.FAVORITES_NODE) {
                showFavorites()
            } else {
                showNode(node)
            }
        }
        makeViewEvent(Constants.ViewEvents.HandleReceivedIntent)
    }

    //    private void reloadStorage() {
    //        reinitStorage();
    //    }
    //    /**
    //     * Создание нового хранилища в указанном расположении.
    //     *
    //     * @param storagePath
    ////     * @param checkDirIsEmpty
    //     */
    //    @Override
    //    protected void createStorage(String storagePath/*, boolean checkDirIsEmpty*/) {
    //        if (StorageManager.createStorage(this, storagePath)) {
    //            closeFoundFragment();
    //            getMainPage().clearView();
    //            mDrawerLayout.openDrawer(Gravity.LEFT);
    //            // сохраняем путь к хранилищу
    ////            if (SettingsManager.isLoadLastStoragePath()) {
    //            /*SettingsManager.setStoragePath(storagePath);*/
    ////            }
    //            initGUI(DataManager.createDefault(this), false, false);
    ////            LogManager.log(getString(R.string.log_storage_created) + mStoragePath, LogManager.Types.INFO, Toast.LENGTH_SHORT);
    //        } else {
    //            mDrawerLayout.openDrawer(Gravity.LEFT);
    //            initGUI(false, false, false);
    ////            LogManager.log(getString(R.string.log_failed_storage_create) + mStoragePath, LogManager.Types.ERROR, Toast.LENGTH_LONG);
    //        }
    //    }
    // TODO: почему нигде не используется ??
    //    private void initStorage(String storagePath) {
    //        // читаем установленную опцию isLoadFavoritesOnly только при первой загрузке
    //        boolean isFavorites = !DataManager.isLoaded() && SettingsManager.isLoadFavoritesOnlyDef(this)
    //                || (DataManager.isLoaded() && DataManager.isFavoritesMode());
    //
    //        if (StorageManager.initStorage(this, storagePath)) {
    //            mDrawerLayout.openDrawer(Gravity.LEFT);
    //        } else {
    //            mDrawerLayout.openDrawer(Gravity.LEFT);
    //            initGUI(false, isFavorites, false);
    //        }
    //    }

    private fun setStage(obj: Objs, oper: Opers, stage: Stages) {
        val taskStage = TaskStage(Constants.TetroidView.Main, obj, oper, stage)
        val mes = logTaskStage(getContext(), taskStage)
        makeViewEvent(Constants.ViewEvents.ShowProgressText, mes)
    }

    fun updateNodes() {
        makeEvent(Constants.MainEvents.UpdateNodes)
    }

    //endregion Nodes

    //region Tags

    fun getCurTagName() = curTag?.name ?: ""

    fun showTag(tagName: String?) {
        val tag = tagsInteractor.getTag(tagName)
        if (tag != null) {
            showTag(tag)
        } else {
            LogManager.log(getContext(), getString(R.string.search_tag_not_found_mask).format(tagName), ILogger.Types.WARNING, Toast.LENGTH_LONG)
        }
    }

    /**
     * Отображение записей по метке.
     * @param tag
     */
    fun showTag(tag: TetroidTag?) {
        if (tag != null) {
            curTag = tag
            // сбрасываем текущую ветку
            makeEvent(Constants.MainEvents.ShowNode, null)
            LogManager.log(getContext(), getString(R.string.log_open_tag_records) + tag)
//            showRecords(tag.records, MainPageFragment.MAIN_VIEW_TAG_RECORDS)
            showRecords(tag.records, Constants.MAIN_VIEW_TAG_RECORDS)
        } else {
            LogManager.log(getContext(), R.string.log_tag_is_null, ILogger.Types.ERROR, Toast.LENGTH_LONG)
        }
    }

    fun renameTag(tag: TetroidTag?, name: String) {
        if (tag == null || tag.name == name) return
        viewModelScope.launch {
            if (tagsInteractor.renameTag(getContext(), tag, name)) {
                logOperRes(getContext(), Objs.TAG, Opers.RENAME)
                makeEvent(Constants.MainEvents.UpdateTags)
                makeEvent(Constants.MainEvents.UpdateRecords)
            } else {
                logOperErrorMore(getContext(), Objs.TAG, Opers.RENAME)
            }
        }
    }

    fun updateTags() {
        makeEvent(Constants.MainEvents.UpdateTags)
    }

    //endregion Tags

    // region Attaches

    fun checkPermissionAndOpenAttach(attach: TetroidFile?) {
        if (isCrypted() && !SettingsManager.isDecryptFilesInTempDef(getContext())) {
            log(getContext(), R.string.log_viewing_decrypted_not_possible, Toast.LENGTH_LONG)
        } else {
            makeEvent(Constants.MainEvents.CheckPermissionAndOpenAttach, attach)
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun openAttach(attach: TetroidFile) {
        viewModelScope.launch {
            attachesInteractor.openAttach(getContext(), attach)
        }
    }

    fun showAttaches(attaches: List<TetroidFile>) {
        makeEvent(Constants.MainEvents.ShowAttaches, ObjectsInView(attaches))
    }

//    fun showRecordAttaches(record: TetroidRecord) {
//        doAction(Constants.TetroidEvents.ShowRecordAttaches, record)
//    }

    /**
     * Отображение списка прикрепленных файлов.
     * @param record Запись
     */
    fun showRecordAttaches(record: TetroidRecord?, fromRecordActivity: Boolean = false) {
        if (record == null) return
        curNode = record.node
        curRecord = record
        isFromRecordActivity = fromRecordActivity
        showAttaches(record.attachedFiles)
        openPage(Constants.PAGE_MAIN)
    }

    open fun attachFile(fileFullName: String?, record: TetroidRecord?, deleteSrcFile: Boolean) {
        makeViewEvent(Constants.ViewEvents.TaskStarted, R.string.task_attach_file)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val attach = attachesInteractor.attachFile(getContext(), fileFullName, record, deleteSrcFile)
                makeViewEvent(Constants.ViewEvents.TaskFinished, Gravity.NO_GRAVITY)
                if (attach != null) {
                    log(getContext(), getString(R.string.log_file_was_attached), ILogger.Types.INFO, Toast.LENGTH_SHORT)
                    makeEvent(Constants.MainEvents.UpdateAttaches)
                    // обновляем список записей для обновления иконки о наличии прикрепляемых файлов у записи,
                    // если был прикреплен первый файл
                    if ((record?.attachedFilesCount ?: 0) == 1) {
                        updateRecords()
                    }
                } else {
                    log(getContext(), getString(R.string.log_file_attaching_error), ILogger.Types.ERROR, Toast.LENGTH_LONG)
                    makeViewEvent(Constants.ViewEvents.ShowMoreInLogs)
                }
            }
        }
    }

    /**
     * Прикрепление нового файла к текущей записи.
     */
    fun attachFileToCurRecord(uri: Uri) {
        val uriHelper = UriHelper(getContext())
        attachFileToCurRecord(uriHelper.getPath(uri), true)
    }

    fun attachFileToCurRecord(fullFileName: String?, deleteSrcFile: Boolean) {
        attachFile(fullFileName, curRecord, deleteSrcFile)
    }

    /**
     * Выбор файла в файловой системе устройства и прикрепление к текущей записи.
     */
    fun pickAndAttachFile() {
        makeEvent(Constants.MainEvents.OpenFilePicker)
    }

    /**
     * Ввод URL для загрузки и прикрепления файла к текущей записи.
     */
    fun downloadAndAttachFile(url: String) {
        viewModelScope.launch {
            downloadFileToCache(url, object : IDownloadFileResult {
                override fun onSuccess(uri: Uri) {
                    attachFileToCurRecord(uri)
                }
                override fun onError(ex: Exception) {}
            })
        }
    }

    /**
     * Удаление прикрепленного файла.
     * @param file
     */
    fun deleteAttach(file: TetroidFile) {
        viewModelScope.launch {
            val res: Int = attachesInteractor.deleteAttachedFile(getContext(), file, false)
            when (res) {
                -2 -> makeEvent(Constants.MainEvents.AskForOperationWithoutFile, ClipboardParams(Opers.DELETE, file))
                -1 -> makeEvent(Constants.MainEvents.AskForOperationWithoutDir, ClipboardParams(Opers.DELETE, file))
                else -> onDeleteAttachResult(file, res)
            }
        }
    }

    /**
     * Обработка результата удаления файла.
     * @param file
     * @param res
     */
    private fun onDeleteAttachResult(file: TetroidFile, res: Int) {
        if (res > 0) {
            makeEvent(Constants.MainEvents.AttachDeleted, file)
            // обновляем список записей для удаления иконки о наличии прикрепляемых файлов у записи,
            // если был удален единственный файл
            if ((curRecord?.attachedFilesCount ?: 0) <= 0) {
                updateRecords()
            }
            logOperRes(getContext(), Objs.FILE, Opers.DELETE)
        } else {
            logOperErrorMore(getContext(), Objs.FILE, Opers.DELETE)
        }
    }

    /**
     * Переименование прикрепленного файла.
     * @param file
     */
    fun renameAttach(file: TetroidFile, name: String) {
        viewModelScope.launch {
            val res = attachesInteractor.editAttachedFileFields(getContext(), file, name)
            when (res) {
                -2 -> makeEvent(Constants.MainEvents.AskForOperationWithoutFile, ClipboardParams(Opers.RENAME, file))
                // TODO: добавить вариант Создать каталог записи
                -1 -> makeEvent(Constants.MainEvents.AskForOperationWithoutDir, ClipboardParams(Opers.RENAME, file))
                else -> onRenameAttachResult(res)
            }
        }
    }

    /**
     * Обработка результата переименования файла.
     * @param res
     */
    private fun onRenameAttachResult(res: Int) {
        if (res > 0) {
            logOperRes(getContext(), Objs.FILE, Opers.RENAME)
            makeEvent(Constants.MainEvents.UpdateAttaches)
        } else {
            logOperErrorMore(getContext(), Objs.FILE, Opers.RENAME)
        }
    }

    /**
     * Операции с объектами хранилища, когда файл отсутствует.
     */
    fun doOperationWithoutFile(params: ClipboardParams) {
        viewModelScope.launch {
            when (params.operation) {
                Opers.RENAME,
                Opers.DELETE -> {
                    if (params.obj is TetroidFile) {
                        val file = params.obj
                        val res = attachesInteractor.deleteAttachedFile(getContext(), file, true)
                        onDeleteAttachResult(file, res)
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * Перемещение файла вверх/вниз по списку.
     * @param pos
     * @param isUp
     */
    fun reorderAttach(files: List<TetroidFile>, pos: Int, isUp: Boolean) {
        val res = swapTetroidObjects(files, pos, isUp,true)
        if (res > 0) {
            makeEvent(Constants.MainEvents.UpdateAttaches)
            logOperRes(getContext(), Objs.FILE, Opers.REORDER)
        } else if (res < 0) {
            logOperErrorMore(getContext(), Objs.FILE, Opers.REORDER)
        }
    }

    fun saveAttachOnDevice(file: TetroidFile) {
        curFile = file
        makeEvent(Constants.MainEvents.OpenFolderPicker)
    }

    /**
     * Сохранение файла по выбранному пути.
     */
    fun saveCurAttachOnDevice(folderPath: String?) {
        viewModelScope.launch {
            makeViewEvent(Constants.ViewEvents.TaskStarted, R.string.task_file_saving)
            val res = attachesInteractor.saveFile(getContext(), curFile, folderPath)
            makeViewEvent(Constants.ViewEvents.TaskFinished, Gravity.NO_GRAVITY)
            onSaveFileResult(res)
        }
    }

    private fun onSaveFileResult(res: Boolean) {
        if (res) {
            logOperRes(getContext(), Objs.FILE, Opers.SAVE, "", Toast.LENGTH_SHORT)
        } else {
            logOperErrorMore(getContext(), Objs.FILE, Opers.SAVE)
        }
    }

    /**
     * Получение размера прикрепленного файла.
     * @param attach
     * @return
     */
    fun getAttachSize(attach: TetroidFile): String {
        return attachesInteractor.getAttachedFileSize(getContext(), attach)
    }

    fun moveBackFromAttaches() {
        if (isFromRecordActivity) {
            openRecord(curRecord)
            isFromRecordActivity = false
        } else {
            showRecordNode(curRecord)
        }
    }

    //endregion Attaches

    //region Favorites

    /**
     * Отображение списка избранных записей.
     */
    fun showFavorites() {
        // проверка нужно ли расшифровать ветку перед отображением
        /*if (FavoritesManager.isCryptedAndNonDecrypted()) {
            // запрос пароля в асинхронном режиме
            askPassword(FavoritesManager.FAVORITES_NODE);
        } else*/
//        run {

        // выделяем ветку Избранное, только если загружено не одно Избранное
//            if (!isLoadedFavoritesOnly()) {
//                setCurNode(null)
//                setFavorIsCurNode(true)
//            }
//            showRecords(FavoritesManager.getFavoritesRecords(), MainPageFragment.MAIN_VIEW_FAVORITES)

        makeEvent(Constants.MainEvents.ShowFavorites)
        // сохраняем выбранную ветку
        saveLastSelectedNode()
//        }
    }

    fun addFavorite(context: Context?, record: TetroidRecord) {
        if (FavoritesManager.add(context, record)) {
            val mes = getString(R.string.log_added_to_favor)
            Message.show(getContext(), mes, Toast.LENGTH_SHORT)
            log(getContext(), mes + ": " + getIdString(getContext(), record), ILogger.Types.INFO, -1)
            updateFavorites(record)
        } else {
            logOperError(getContext(), Objs.RECORD, Opers.ADD, getString(R.string.log_with_id_to_favor_mask).format(record.id), true, Toast.LENGTH_LONG)
        }
    }

    fun removeFavorite(context: Context?, record: TetroidRecord) {
        if (FavoritesManager.remove(context, record, true)) {
            val mes = getString(R.string.log_deleted_from_favor)
            Message.show(getContext(), mes, Toast.LENGTH_SHORT)
            log(getContext(), mes + ": " + getIdString(getContext(), record), ILogger.Types.INFO, -1)
            makeEvent(Constants.MainEvents.UpdateFavorites)
            updateRecords()
        } else {
            logOperError(getContext(), Objs.RECORD, Opers.DELETE, getString(R.string.log_with_id_from_favor_mask).format(record.id), true, Toast.LENGTH_LONG)
        }
    }

    fun updateFavorites(record: TetroidRecord?): Boolean {
        if (record == null || !record.isFavorite) return false
        makeEvent(Constants.MainEvents.UpdateFavorites)
        updateRecords()
        return true
    }

    //endregion Favorites

    //region Global search

    /**
     * Открытие объекта из поисковой выдачи в зависимости от его типа.
     * @param found
     */
    fun openFoundObject(found: ITetroidObject) {
        val type = found.type
        when (type) {
            FoundType.TYPE_RECORD -> openRecord(found as TetroidRecord)
            FoundType.TYPE_FILE -> showRecordAttaches((found as TetroidFile).record, false)
            FoundType.TYPE_NODE -> showNode(found as TetroidNode)
            FoundType.TYPE_TAG -> showTag(found as TetroidTag)
        }
        if (type != FoundType.TYPE_RECORD) {
            openPage(Constants.PAGE_MAIN)
        }
    }

    fun research() {
        makeEvent(Constants.MainEvents.GlobalResearch)
    }

    /**
     * Запуск глобального поиска.
     * @param profile
     */
    fun startGlobalSearch(profile: SearchProfile?) {
        if (profile == null) return

        this.lastSearchProfile = profile
        val searchInteractor = SearchInteractor(profile, storageInteractor, nodesInteractor, recordsInteractor)

        viewModelScope.launch {
            makeViewEvent(Constants.ViewEvents.TaskStarted, R.string.global_searching)
            LogManager.log(getContext(), getString(R.string.global_search_start).format(profile.query))
            val found = searchInteractor.globalSearch(getContext())
            makeViewEvent(Constants.ViewEvents.TaskFinished, Gravity.NO_GRAVITY)
            if (found == null) {
                LogManager.log(getContext(), getString(R.string.log_global_search_return_null), Toast.LENGTH_SHORT)
                return@launch
            } else if (profile.isSearchInNode && profile.node != null) {
                LogManager.log(getContext(), getString(R.string.global_search_by_node_result).format(profile.node?.name), Toast.LENGTH_SHORT)
            }
            // уведомляем, если не смогли поискать в зашифрованных ветках
            if (searchInteractor.isExistCryptedNodes) {
                LogManager.log(getContext(), R.string.log_found_crypted_nodes, Toast.LENGTH_SHORT)
            }
            LogManager.log(getContext(), String.format(getString(R.string.global_search_end), found.size))

            makeEvent(Constants.MainEvents.GlobalSearchFinished, GlobalSearchParams(found, profile))
        }
    }

    fun startGlobalSearchFromFilterQuery() {
        makeEvent(Constants.MainEvents.GlobalSearchStart, lastFilterQuery)
    }

    //endregion Global search

    //region Filter

    /**
     * Фильтр записей, меток или файлов (смотря какой список активен в данный момент).
     * @param query
     */
    fun filterListInMainPage(query: String, isSaveQuery: Boolean) {
        if (isSaveQuery) {
            TetroidSuggestionProvider.saveRecentQuery(getContext(), query)
        }
        filterListInMainPage(query, curMainViewId)
    }

    private fun filterListInMainPage(query: String, viewId: Int) {
        when (viewId) {
            Constants.MAIN_VIEW_NODE_RECORDS -> filterNodeRecords(query)
            Constants.MAIN_VIEW_TAG_RECORDS -> filterTagRecords(query)
            Constants.MAIN_VIEW_RECORD_FILES -> filterRecordAttaches(query)
        }
    }

    private fun filterNodeRecords(query: String) {
        if (curNode != null) {
            filterRecords(query, curNode!!.records, Constants.MAIN_VIEW_NODE_RECORDS)
        } else {
            LogManager.log(getContext(), R.string.search_records_search_select_node, Toast.LENGTH_LONG)
        }
    }

    private fun filterTagRecords(query: String) {
        if (curTag != null) {
            filterRecords(query, curTag!!.records, Constants.MAIN_VIEW_TAG_RECORDS)
        } else {
            LogManager.log(getContext(), R.string.search_records_select_tag, Toast.LENGTH_LONG)
        }
    }

    private fun filterRecords(query: String, records: List<TetroidRecord>, viewId: Int) {
        val message = if (viewId == Constants.MAIN_VIEW_NODE_RECORDS)
            getString(R.string.filter_records_in_node_by_query).format(getCurNodeName(), query)
        else getString(R.string.filter_records_in_tag_by_query).format(getCurTagName(), query)
        LogManager.log(getContext(), message)
        val found = ScanManager.searchInRecordsNames(records, query)
        showRecords(found, viewId, false)
        if (lastFilterQuery.isNullOrEmpty()) {
            lastFilterQuery = query
        }
        makeEvent(Constants.MainEvents.RecordsFiltered, FilteredObjectsInView(query, found, viewId))
    }

    private fun filterRecordAttaches(query: String) {
        if (curRecord != null) {
            filterAttaches(query, curRecord!!) }
        else {
            LogManager.log(getContext(), getString(R.string.log_cur_record_is_not_set), ILogger.Types.ERROR, Toast.LENGTH_LONG)
        }
    }

    private fun filterAttaches(query: String, record: TetroidRecord) {
        LogManager.log(getContext(), String.format(getString(R.string.filter_files_by_query), record.name, query))
        val found = ScanManager.searchInFiles(record.attachedFiles, query)
        showAttaches(found)
        makeEvent(Constants.MainEvents.AttachesFiltered, FilteredObjectsInView(query, found, Constants.MAIN_VIEW_RECORD_FILES))
    }

    fun onRecordsSearchClose() {
        // "сбрасываем" фильтрацию, но не для только что открытых списков записей
        // (т.к. при открытии списка записей вызывается setIconified=false, при котором вызывается это событие,
        // что приводит к повторному открытию списка записей)
        if (isDropRecordsFiltering) {
            when (curMainViewId) {
                Constants.MAIN_VIEW_NODE_RECORDS -> if (curNode != null) {
                    showRecords(curNode!!.records, Constants.MAIN_VIEW_NODE_RECORDS, false)
                }
                Constants.MAIN_VIEW_TAG_RECORDS -> if (curTag != null) {
                    showRecords(curTag!!.records, Constants.MAIN_VIEW_TAG_RECORDS, false)
                }
            }
        }
    }

    //endregion Filter

    //region FileObserver

    /**
     * Обработчик изменения структуры хранилища извне.
     */
    fun startStorageTreeObserver() {
        if (isCheckOutsideChanging()) {
            // запускаем мониторинг, только если хранилище загружено
            if (isLoaded()) {
                this.isStorageChangingHandled = false
                val bundle = Bundle()
                bundle.putInt(FileObserverService.EXTRA_ACTION_ID, FileObserverService.ACTION_START)
//                bundle.putString(FileObserverService.EXTRA_FILE_PATH, StorageManager.getStoragePath() + "/" + DataManager.MYTETRA_XML_FILE_NAME);
                bundle.putString(FileObserverService.EXTRA_FILE_PATH, storageInteractor.getPathToMyTetraXml())
                bundle.putInt(FileObserverService.EXTRA_EVENT_MASK, FileObserver.MODIFY)

                makeEvent(Constants.MainEvents.StartFileObserver, bundle)
            }
        } else {
            makeEvent(Constants.MainEvents.StopFileObserver)
        }
    }

    fun onStorageOutsideChanged() {
        // проверяем, не был ли запущен обработчик второй раз подряд
        if (!isStorageChangingHandled) {
            isStorageChangingHandled = true
            LogManager.log(getContext(), R.string.ask_storage_changed_outside, ILogger.Types.INFO)
            makeStorageEvent(Constants.StorageEvents.ChangedOutside)
        }
    }

    fun dropIsStorageChangingHandled() {
        isStorageChangingHandled = false
    }

    //endregion FileObserver

    fun onMainViewBackPressed(curView: Int): Boolean {
        var res = false
        if (curView == Constants.MAIN_VIEW_RECORD_FILES) {
            res = true
            when (lastMainViewId) {
                Constants.MAIN_VIEW_NODE_RECORDS,
                Constants.MAIN_VIEW_TAG_RECORDS,
                // Constants.VIEW_FOUND_RECORDS,
                Constants.MAIN_VIEW_FAVORITES ->
                    showMainView(lastMainViewId)
                else -> showMainView(Constants.MAIN_VIEW_NONE)
            }
        }
        return res
    }

}

class ClipboardParams(
    val operation: Opers,
    val obj: TetroidObject,
    val isCutted: Boolean = false
)

class GlobalSearchParams(
    val found: HashMap<ITetroidObject, FoundType>,
    val profile: SearchProfile
)

class ToolbarParams(
    val viewId: Int,
    val title: String?
)