package com.gee12.mytetroid.viewmodels

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.FileObserver
import android.view.Gravity
import androidx.annotation.MainThread
import androidx.annotation.RequiresPermission
import com.gee12.mytetroid.*
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.data.crypt.TetroidCrypter
import com.gee12.mytetroid.helpers.UriHelper
import com.gee12.mytetroid.interactors.SearchInteractor
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.logs.TaskStage
import com.gee12.mytetroid.logs.TaskStage.Stages
import com.gee12.mytetroid.logs.TetroidLogger
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.services.FileObserverService
import com.gee12.mytetroid.utils.StringUtils
import com.gee12.mytetroid.utils.Utils
import com.gee12.mytetroid.views.activities.TetroidActivity.IDownloadFileResult
import kotlinx.coroutines.*
import java.lang.Exception
import java.util.ArrayList
import java.util.HashMap

class MainViewModel(
    app: Application,
    logger: TetroidLogger?,
    storagesRepo: StoragesRepo?,
    xmlLoader: TetroidXml?,
    crypter: TetroidCrypter?
): StorageViewModel(
    app,
    logger,
    storagesRepo,
    xmlLoader,
    crypter
) {

    var curMainViewId = Constants.MAIN_VIEW_NONE
    var lastMainViewId = 0

    var curNode: TetroidNode? = null
    var curRecord: TetroidRecord? = null
    var curTag: TetroidTag? = null
    var curFile: TetroidFile? = null

    var tempFileToOpen: TetroidFile? = null
    var isDropRecordsFiltering = true
    var lastSearchProfile: SearchProfile? = null
    var lastFilterQuery: String? = null
    var isFromRecordActivity = false
    private var isStorageChangingHandled = false

    init {
//        CommonSettings.init(app)

        this.xmlLoader.setStorageLoadHelper(this)
    }

    //region Pages

    @MainThread
    fun openPage(pageId: Int) {
        setViewEvent(Constants.ViewEvents.OpenPage, pageId)
    }

//    fun onMainPageCreated() {
//        launch {
//            withContext(Dispatchers.IO) {
//                makeViewEvent(Constants.ViewEvents.MainPageCreated)
//            }
//        }
//    }

    @MainThread
    fun showMainView(viewId: Int) {
        // сохраняем значение для возврата на старое View
        // (только, если осуществляется переключение на действительно другую вьюшку)
        if (viewId != curMainViewId) {
            this.lastMainViewId = curMainViewId
        }
        setViewEvent(Constants.ViewEvents.ShowMainView, viewId)
        this.curMainViewId = viewId
    }

    @MainThread
    fun closeFoundFragment() {
        setViewEvent(Constants.ViewEvents.CloseFoundView)
    }

    /**
     * Восстанавливаем состояние Toolbar при переключении обратно к фрагменту MainPageFragment.
     */
    @MainThread
    fun restoreLastMainToolbarState() {
        var title: String? = null
        val restoredViewId = curMainViewId
        if (restoredViewId == Constants.MAIN_VIEW_RECORD_FILES) {
            title = curRecord?.name ?: ""
        }
        updateToolbar(restoredViewId, title)
    }

    fun updateToolbar(viewId: Int, title: String?) {
        postViewEvent(Constants.ViewEvents.UpdateToolbar, ToolbarParams(viewId, title))
    }

    //endregion Pages

    //region Records

    fun getCurNodeName() = curNode?.name ?: ""

    fun showRecords(records: List<TetroidRecord>, viewId: Int, dropSearch: Boolean = true) {
        setEvent(Constants.MainEvents.ShowRecords, ObjectsInView(records, viewId, dropSearch))
    }

    /**
     * Вставка записи в ветку.
     */
    fun insertRecord() {
        launch {
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
                    postEvent(Constants.MainEvents.AskForOperationWithoutDir, ClipboardParams(LogOper.INSERT, record, isCutted))
                }
                -2 -> logOperErrorMore(LogObj.RECORD_DIR, LogOper.INSERT)
                else -> onInsertRecordResult(record, res, isCutted)
            }
        }
    }

    private suspend fun insertRecord(record: TetroidRecord, isCutted: Boolean, withoutDir: Boolean): Int {
        if (curNode == null) return -2
        return recordsInteractor.insertRecord(getContext(), record, isCutted, curNode!!, withoutDir)
    }

    @MainThread
    private fun onInsertRecordResult(record: TetroidRecord, res: Int, isCutted: Boolean) {
        if (res > 0) {
            logOperRes(LogObj.RECORD, LogOper.INSERT)
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
            logOperErrorMore(LogObj.RECORD, LogOper.INSERT)
        }
    }

    /**
     * Создание новой записи.
     */
    fun createRecord(name: String, tags: String, author: String, url: String, node: TetroidNode?, isFavor: Boolean) {
        if (node == null) return
        launch {
            val record = recordsInteractor.createRecord(getContext(), name, tags, author, url, node, isFavor)
            if (record != null) {
                logOperRes(LogObj.RECORD, LogOper.CREATE, record, false)
                updateTags()
                updateNodes()
                if (!updateFavorites(record)) {
                    updateRecords()
                }
                openRecord(record)
            } else {
                logOperErrorMore(LogObj.RECORD, LogOper.CREATE)
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
        launch {
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
                        logWarning(R.string.log_files_attach_error, true)
                        postViewEvent(Constants.ViewEvents.ShowMoreInLogs)
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
        launch {
            val res = deleteRecord(record, false)
            if (res == -1) {
                setEvent(Constants.MainEvents.AskForOperationWithoutDir, ClipboardParams(LogOper.DELETE, record))
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
    @MainThread
    private suspend fun onDeleteRecordResult(record: TetroidRecord, res: Int, isCutted: Boolean) {
        if (res > 0) {
            setEvent(Constants.MainEvents.RecordDeleted, record)
            updateTags()
            updateNodes()
            // обновляем избранное
            if (!updateFavorites(record)) {
                updateRecords()
            }
            curRecord = null
            delay(10)
            logOperRes(LogObj.RECORD, if (isCutted) LogOper.CUT else LogOper.DELETE)
            // переходим в список записей ветки после удаления
            // (запись может быть удалена при попытке просмотра/изменения файла, например)
            if (curMainViewId != Constants.MAIN_VIEW_NODE_RECORDS && curMainViewId != Constants.MAIN_VIEW_FAVORITES) {
                showMainView(Constants.MAIN_VIEW_NODE_RECORDS)
            }
        } else if (res == -2 && !isCutted) {
            logWarning(R.string.log_erorr_move_record_dir_when_del, true)
            setViewEvent(Constants.ViewEvents.ShowMoreInLogs)
        } else {
            logOperErrorMore(LogObj.RECORD, if (isCutted) LogOper.CUT else LogOper.DELETE)
        }
    }

    override fun editRecordFields(record: TetroidRecord, name: String, tags: String, author: String, url: String, node: TetroidNode, isFavor: Boolean) {
        launch {
            val oldNode = record.node
            if (recordsInteractor.editRecordFields(getContext(), record, name, tags, author, url, node, isFavor)) {
                onRecordFieldsUpdated(record, oldNode !== record.node)
//                TetroidLog.logOperRes(TetroidLog.LogObj.FILE_FIELDS, TetroidLog.LogOper.CHANGE);
                log(R.string.log_record_fields_changed, true)
            } else {
                logOperErrorMore(LogObj.RECORD_FIELDS, LogOper.CHANGE)
            }
        }
    }

    /**
     * Обновление списка записей и меток после изменения свойств записи.
     */
    fun onRecordFieldsUpdated(record: TetroidRecord?, nodeChanged: Boolean) {
        launch {
            if (nodeChanged && record != null) {
                showNode(record.node)
            } else {
                updateRecords()
            }
            updateTags()
            updateFavorites()
        }
    }

    /**
     * Копирование записи.
     * @param record
     */
    fun copyRecord(record: TetroidRecord) {
        // добавляем в "буфер обмена"
        TetroidClipboard.copy(record)
        logOperRes(LogObj.RECORD, LogOper.COPY)
    }

    /**
     * Перемещение записи вверх/вниз по списку.
     * @param records
     * @param pos
     * @param isUp
     */
    fun reorderRecords(records: List<TetroidRecord>, pos: Int, isUp: Boolean) {
        launch {
            val res = if (curMainViewId == Constants.MAIN_VIEW_FAVORITES)
                FavoritesManager.swapRecords(getContext(), pos, isUp, true)
            else swapTetroidObjects(records, pos, isUp, true)
            if (res > 0) {
                updateRecords()
                logOperRes(LogObj.RECORD, LogOper.REORDER)
            } else if (res < 0) {
                logOperErrorMore(LogObj.RECORD, LogOper.REORDER)
            }
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
            log(getString(R.string.title_link_was_copied) + url, true)
        } else {
            logError(getString(R.string.log_get_item_is_null), true)
        }
    }

    /**
     * Вырезание записи из ветки.
     * @param record
     */
    fun cutRecord(record: TetroidRecord) {
        launch {
            // добавляем в "буфер обмена"
            TetroidClipboard.cut(record)
            // удаляем запись из текущей ветки и каталог перемещаем в корзину
            val res: Int = cutRecord(record, false)
            if (res == -1) {
                postEvent(Constants.MainEvents.AskForOperationWithoutDir, ClipboardParams(LogOper.CUT, record))
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
        launch {
            when (params.operation) {
                LogOper.INSERT -> {
                    val record = params.obj as TetroidRecord
                    val res: Int = insertRecord(record, params.isCutted, true)
                    onInsertRecordResult(record, res, params.isCutted)
                }
                LogOper.DELETE -> {
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
                LogOper.CUT -> {
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
            logError(R.string.log_record_is_null, true)
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
        launch(Dispatchers.Main) {
            val bundle = Bundle()
            bundle.putString(Constants.EXTRA_OBJECT_ID, recordId)
            openRecord(bundle)
        }
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
        postEvent(Constants.MainEvents.OpenRecord, bundle)
    }

    /**
     * Открытие каталога записи.
     * @param record
     */
    fun openRecordFolder(record: TetroidRecord?) {
        if (record == null) return
        if (!recordsInteractor.openRecordFolder(getContext(), record)) {
            logWarning(R.string.log_missing_file_manager, true)
        }
    }

    @MainThread
    private fun updateRecords() {
        setEvent(Constants.MainEvents.UpdateRecords)
    }

    //endregion Records

    //region Nodes

    /**
     * Открытие записей ветки.
     * @param node
     */
    fun showNode(node: TetroidNode?) {
        if (node != null) {
            launch {
                // проверка нужно ли расшифровать ветку перед отображением
                if (checkAndDecryptNode(node)) return@launch
                log(getString(R.string.log_open_node) + StringUtils.getIdString(getContext(), node))
                curNode = node
                setEvent(Constants.MainEvents.SetCurrentNode, node)
                showRecords(node.records, Constants.MAIN_VIEW_NODE_RECORDS)

                // сохраняем выбранную ветку
                saveLastSelectedNode()
            }
        } else {
            logError(R.string.log_node_is_null, true)
        }
    }

    /**
     * Открытие ветки записи.
     * Если активен режим "Только избранное", то открытие списка избранных записей.
     */
    fun showRecordNode(record: TetroidRecord?) {
        when {
            isLoadedFavoritesOnly() -> showFavorites()
            record != null -> showNode(record.node)
            curNode != null -> showNode(curNode)
        }
    }

    /**
     * Сохранение последней выбранной ветки.
     */
    private fun saveLastSelectedNode() {
        if (isKeepLastNode()) {
            val curNode = if (curMainViewId == Constants.MAIN_VIEW_FAVORITES) FavoritesManager.FAVORITES_NODE else curNode
            setLastNodeId(curNode?.id)
            updateStorage()
        }
    }

    fun createNode(name: String, trueParentNode: TetroidNode?) {
        launch {
            val node = nodesInteractor.createNode(getContext(), name, trueParentNode)
            if (node != null) {
                setEvent(Constants.MainEvents.NodeCreated, node)
            } else {
                logOperErrorMore(LogObj.NODE, LogOper.CREATE)
            }
        }
    }

    /**
     * Переименование ветки.
     * @param node
     */
    fun renameNode(node: TetroidNode, newName: String) {
        launch {
            if (nodesInteractor.editNodeFields(getContext(), node, newName)) {
                logOperRes(LogObj.NODE, LogOper.RENAME)
                updateNodes()
                setEvent(Constants.MainEvents.NodeRenamed, node)
            } else {
                logOperErrorMore(LogObj.NODE, LogOper.RENAME)
            }
        }
    }

    fun setNodeIcon(nodeId: String?, iconPath: String?, isDrop: Boolean) {
        if (nodeId == null) return
        launch {
            val node = if (curNode?.id == nodeId) curNode else nodesInteractor.getNode(nodeId)
            if (nodesInteractor.setNodeIcon(getContext(), node, iconPath, isDrop)) {
                logOperRes(LogObj.NODE, LogOper.CHANGE)
                updateNodes()
            } else {
                logOperErrorMore(LogObj.NODE, LogOper.CHANGE)
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

        launch {
            if (nodesInteractor.insertNode(getContext(), node, trueParentNode, isCutted)) {
                // ищем вновь созданную ветку - копию node
                //  (даже при вставке ВЫРЕЗАННОЙ ветки вставляется ее копия, а не оригинальная из буфера обмена)
                val insertedNode = nodesInteractor.getNode(node.id)
                setEvent(Constants.MainEvents.NodeInserted, insertedNode)
            } else {
                logOperErrorMore(LogObj.NODE, LogOper.INSERT)
            }
        }
    }

    /**
     * Вырезание ветки из родительской ветки.
     * @param node
     */
    fun cutNode(node: TetroidNode, pos: Int) {
        if (nodesInteractor.hasNonDecryptedNodes(node)) {
            showMessage(getString(R.string.log_enter_pass_first))
            return
        }
        // добавляем в "буфер обмена"
        TetroidClipboard.cut(node)
        launch {
            // удаляем ветку из родительской ветки вместе с записями
            val res = nodesInteractor.cutNode(getContext(), node)
            onDeleteNodeResult(node, res, true)
        }
    }

    /**
     * Удаление ветки.
     * @param node
     */
    @MainThread
    fun startDeleteNode(node: TetroidNode?) {
        if (node == null) return
        // запрет на удаление последней ветки в корне
        if (node.level == 0 && getRootNodes().size == 1) {
            log(R.string.log_cannot_delete_root_node, true)
            return
        }
        postEvent(Constants.MainEvents.AskForDeleteNode, node)
    }

    fun deleteNode(node: TetroidNode) {
        launch {
            val res = nodesInteractor.deleteNode(getContext(), node)
            onDeleteNodeResult(node, res, false)
        }
    }

    @MainThread
    private fun onDeleteNodeResult(node: TetroidNode, res: Boolean, isCutted: Boolean) {
        if (res) {
            setEvent(if (isCutted) Constants.MainEvents.NodeCutted else Constants.MainEvents.NodeDeleted, node)
            // удаляем элемент внутри списка
//            if (mListAdapterNodes.deleteItem(pos)) {
//                logOperRes(this, LogObj.NODE, if (!isCutted) LogOper.DELETE else LogOper.CUT)
//            } else {
//                LogManager.log(this, getString(R.string.log_node_delete_list_error), ILogger.Types.ERROR, Toast.LENGTH_LONG)
//            }
            // обновляем label с количеством избранных записей
            if (App.isFullVersion()) {
                updateFavorites()
            }
            // убираем список записей удаляемой ветки
            if (curNode == node || nodesInteractor.isNodeInNode(curNode, node)) {
//                getMainPage().clearView()
                curRecord = null
                curNode = null
                setViewEvent(Constants.ViewEvents.ClearMainView)
            }
            if (node.isCrypted) {
                // проверяем существование зашифрованных веток
                checkExistenceCryptedNodes()
            }
        } else {
            logOperErrorMore(LogObj.NODE, if (!isCutted) LogOper.DELETE else LogOper.CUT)
        }
    }

    /**
     * Зашифровка ветки.
     * @param node
     */
    fun startEncryptNode(node: TetroidNode) {
        if (node == quicklyNode) {
            showMessage(getString(R.string.mes_quickly_node_cannot_encrypt))
        } else {
//        PassManager.checkStoragePass(this, node, new Dialogs.IApplyCancelResult() {
            checkStoragePass(
                EventCallbackParams(Constants.MainEvents.EncryptNode, node)
            )
        /*new Dialogs.IApplyCancelResult() {
            @Override
            public void onApply() {
//                mCurTask = new CryptNodeTask(node, true).run();
                viewModel.encryptNode(node);
            }
            @Override
            public void onCancel() {
            }
        }*/
        }
    }

    /**
     * Сброс шифрования ветки.
     * @param node
     */
    fun startDropEncryptNode(node: TetroidNode) {
//        PassManager.checkStoragePass(this, node, new Dialogs.IApplyCancelResult() {
        checkStoragePass(
            EventCallbackParams(Constants.MainEvents.DropEncryptNode, node)
        )
        /*new Dialogs.IApplyCancelResult() {
            @Override
            public void onApply() {
                viewModel.decryptNode(node);
            }
            @Override
            public void onCancel() {
            }
        }*/
    }

    fun encryptNode(node: TetroidNode) {
        startEncryptDecryptNode(node, true)
    }

    fun dropEncryptNode(node: TetroidNode) {
        startEncryptDecryptNode(node, false)
    }

    /**
     * Длительная операция по зашифровке/сбросе шифровки веток.
     */
    private fun startEncryptDecryptNode(node: TetroidNode, isEncrypt: Boolean) {
        postViewEvent(
            Constants.ViewEvents.TaskStarted,
            if (isEncrypt) R.string.task_node_encrypting else R.string.task_node_drop_crypting
        )
        logOperStart(LogObj.NODE, if (isEncrypt) LogOper.ENCRYPT else LogOper.DROPCRYPT, node)

        val nodeWasEncrypted = node.isCrypted
        val operation = if (isEncrypt) LogOper.ENCRYPT else LogOper.DROPCRYPT

        launch {
            val result = withContext(Dispatchers.IO) {
                // сначала расшифровываем хранилище
                if (isCrypted() && !isDecrypted()) {
                    setStage(LogObj.STORAGE, LogOper.DECRYPT, Stages.START)
                    if (cryptInteractor.decryptStorage(getContext(), false)) {
                        setStage(LogObj.STORAGE, LogOper.DECRYPT, Stages.SUCCESS)
                    } else {
                        setStage(LogObj.STORAGE, LogOper.DECRYPT, Stages.FAILED)
                        return@withContext -2
                    }
                }
                // зашифровуем, только если хранилище не зашифровано или уже расшифровано
                return@withContext if (isNonEncryptedOrDecrypted()) {
                    setStage(LogObj.NODE, operation, Stages.START)

                    val result = if (isEncrypt) cryptInteractor.encryptNode(getContext(), node)
                    else cryptInteractor.dropCryptNode(getContext(), node)

                    if (result) 1 else -1
                } else 0
            }

            setViewEvent(Constants.ViewEvents.TaskFinished)

            if (result > 0) {
                logOperRes(LogObj.NODE, operation)
                if (!isEncrypt && nodeWasEncrypted) {
                    // проверяем существование зашифрованных веток
                    checkExistenceCryptedNodes()
                }
            } else {
                logOperErrorMore(LogObj.NODE, operation)
            }
            afterStorageDecrypted(node)
        }
    }

    override fun afterStorageDecrypted(node: TetroidNode?) {
        super.afterStorageDecrypted(node)
        launch {
            if (node != null) {
                if (node === FavoritesManager.FAVORITES_NODE) {
                    showFavorites()
                } else {
                    showNode(node)
                }
            }
            setViewEvent(Constants.ViewEvents.HandleReceivedIntent)
        }
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

    private fun setStage(obj: LogObj, oper: LogOper, stage: Stages) {
        val taskStage = TaskStage(Constants.TetroidView.Main, obj, oper, stage)
        val mes = logger.logTaskStage(taskStage)
        postViewEvent(Constants.ViewEvents.ShowProgressText, mes)
    }

    fun swapNodes(nodes: List<TetroidNode>, pos: Int, isUp: Boolean): Int {
        return runBlocking {
            swapTetroidObjects(nodes, pos, isUp, true)
        }
    }

    @MainThread
    fun updateNodes() {
        setEvent(Constants.MainEvents.UpdateNodes)
    }

    //endregion Nodes

    //region Tags

    fun getCurTagName() = curTag?.name ?: ""

    fun showTag(tagName: String?) {
        val tag = tagsInteractor.getTag(tagName)
        if (tag != null) {
            showTag(tag)
        } else {
            logWarning(getString(R.string.search_tag_not_found_mask).format(tagName), true)
        }
    }

    /**
     * Отображение записей по метке.
     * @param tag
     */
    fun showTag(tag: TetroidTag?) {
        launch {
            if (tag != null) {
                curTag = tag
                // сбрасываем текущую ветку
                setEvent(Constants.MainEvents.SetCurrentNode, null)
                log(getString(R.string.log_open_tag_records_mask).format(tag.name))
                showRecords(tag.records, Constants.MAIN_VIEW_TAG_RECORDS)
            } else {
                logError(R.string.log_tag_is_null, true)
            }
        }
    }

    fun renameTag(tag: TetroidTag?, name: String) {
        if (tag == null || tag.name == name) return
        launch {
            if (tagsInteractor.renameTag(getContext(), tag, name)) {
                logOperRes(LogObj.TAG, LogOper.RENAME)
                updateTags()
                updateRecords()
            } else {
                logOperErrorMore(LogObj.TAG, LogOper.RENAME)
            }
        }
    }

    @MainThread
    fun updateTags() {
        setEvent(Constants.MainEvents.UpdateTags)
    }

    //endregion Tags

    // region Attaches

    fun checkPermissionAndOpenAttach(attach: TetroidFile?) {
        if (isCrypted() && !CommonSettings.isDecryptFilesInTempDef(getContext())) {
            log(R.string.log_viewing_decrypted_not_possible, true)
        } else {
            postEvent(Constants.MainEvents.CheckPermissionAndOpenAttach, attach)
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun openAttach(attach: TetroidFile) {
        launch {
            attachesInteractor.openAttach(getContext(), attach)
        }
    }

    @MainThread
    fun showAttaches(attaches: List<TetroidFile>) {
        setEvent(Constants.MainEvents.ShowAttaches, ObjectsInView(attaches))
    }

    /**
     * Отображение списка прикрепленных файлов.
     * @param record Запись
     */
    fun showRecordAttaches(record: TetroidRecord?, fromRecordActivity: Boolean = false) {
        if (record == null) return
        launch {
            curNode = record.node
            curRecord = record
            isFromRecordActivity = fromRecordActivity
            showAttaches(record.attachedFiles)
            openPage(Constants.PAGE_MAIN)
        }
    }

    open fun attachFile(fileFullName: String, record: TetroidRecord?, deleteSrcFile: Boolean) {
        launch {
            setViewEvent(Constants.ViewEvents.TaskStarted, R.string.task_attach_file)
            val attach = withContext(Dispatchers.IO) {
                attachesInteractor.attachFile(getContext(), fileFullName, record, deleteSrcFile)
            }
            setViewEvent(Constants.ViewEvents.TaskFinished, Gravity.NO_GRAVITY)
            if (attach != null) {
                log(R.string.log_file_was_attached, true)
                updateAttaches()
                // обновляем список записей для обновления иконки о наличии прикрепляемых файлов у записи,
                // если был прикреплен первый файл
                if ((record?.attachedFilesCount ?: 0) == 1) {
                    updateRecords()
                }
            } else {
                logError(R.string.log_file_attaching_error, true)
                setViewEvent(Constants.ViewEvents.ShowMoreInLogs)
            }
        }
    }

    /**
     * Прикрепление нового файла к текущей записи.
     */
    fun attachFileToCurRecord(uri: Uri) {
        UriHelper(getContext()).getPath(uri)?.let {
            attachFileToCurRecord(it, true)
        }
    }

    fun attachFileToCurRecord(fullFileName: String, deleteSrcFile: Boolean) {
        attachFile(fullFileName, curRecord, deleteSrcFile)
    }

    /**
     * Выбор файла в файловой системе устройства и прикрепление к текущей записи.
     */
    fun pickAndAttachFile() {
        postEvent(Constants.MainEvents.OpenFilePicker)
    }

    /**
     * Ввод URL для загрузки и прикрепления файла к текущей записи.
     */
    fun downloadAndAttachFile(url: String) {
        launch {
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
        launch {
            when (val res: Int = attachesInteractor.deleteAttachedFile(getContext(), file, false)) {
                -2 -> postEvent(Constants.MainEvents.AskForOperationWithoutFile, ClipboardParams(LogOper.DELETE, file))
                -1 -> postEvent(Constants.MainEvents.AskForOperationWithoutDir, ClipboardParams(LogOper.DELETE, file))
                else -> onDeleteAttachResult(file, res)
            }
        }
    }

    /**
     * Обработка результата удаления файла.
     * @param file
     * @param res
     */
    @MainThread
    private fun onDeleteAttachResult(file: TetroidFile, res: Int) {
        if (res > 0) {
            setEvent(Constants.MainEvents.AttachDeleted, file)
            // обновляем список записей для удаления иконки о наличии прикрепляемых файлов у записи,
            // если был удален единственный файл
            if ((curRecord?.attachedFilesCount ?: 0) <= 0) {
                updateRecords()
            }
            logOperRes(LogObj.FILE, LogOper.DELETE)
        } else {
            logOperErrorMore(LogObj.FILE, LogOper.DELETE)
        }
    }

    /**
     * Переименование прикрепленного файла.
     * @param file
     */
    fun renameAttach(file: TetroidFile, name: String) {
        launch {
            when (val res = attachesInteractor.editAttachedFileFields(getContext(), file, name)) {
                -2 -> postEvent(Constants.MainEvents.AskForOperationWithoutFile, ClipboardParams(LogOper.RENAME, file))
                // TODO: добавить вариант Создать каталог записи
                -1 -> postEvent(Constants.MainEvents.AskForOperationWithoutDir, ClipboardParams(LogOper.RENAME, file))
                else -> onRenameAttachResult(res)
            }
        }
    }

    /**
     * Обработка результата переименования файла.
     * @param res
     */
    @MainThread
    private fun onRenameAttachResult(res: Int) {
        if (res > 0) {
            logOperRes(LogObj.FILE, LogOper.RENAME)
            updateAttaches()
        } else {
            logOperErrorMore(LogObj.FILE, LogOper.RENAME)
        }
    }

    /**
     * Операции с объектами хранилища, когда файл отсутствует.
     */
    fun doOperationWithoutFile(params: ClipboardParams) {
        launch {
            when (params.operation) {
                LogOper.RENAME,
                LogOper.DELETE -> {
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
    fun reorderAttaches(files: List<TetroidFile>, pos: Int, isUp: Boolean) {
        launch {
            val res = swapTetroidObjects(files, pos, isUp, true)
            if (res > 0) {
                updateAttaches()
                logOperRes(LogObj.FILE, LogOper.REORDER)
            } else if (res < 0) {
                logOperErrorMore(LogObj.FILE, LogOper.REORDER)
            }
        }
    }

    fun saveAttachOnDevice(file: TetroidFile) {
        curFile = file
        postEvent(Constants.MainEvents.OpenFolderPicker)
    }

    /**
     * Сохранение файла по выбранному пути.
     */
    fun saveCurAttachOnDevice(folderPath: String?) {
        launch {
            postViewEvent(Constants.ViewEvents.TaskStarted, R.string.task_file_saving)
            val res = attachesInteractor.saveFile(getContext(), curFile, folderPath)
            postViewEvent(Constants.ViewEvents.TaskFinished, Gravity.NO_GRAVITY)
            onSaveFileResult(res)
        }
    }

    private fun onSaveFileResult(res: Boolean) {
        if (res) {
            logOperRes(LogObj.FILE, LogOper.SAVE, "", true)
        } else {
            logOperErrorMore(LogObj.FILE, LogOper.SAVE)
        }
    }

    /**
     * Получение размера прикрепленного файла.
     * @param attach
     * @return
     */
    fun getAttachSize(attach: TetroidFile): String? {
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

    private fun updateAttaches() {
        setEvent(Constants.MainEvents.UpdateAttaches)
    }

    //endregion Attaches

    //region Favorites

    /**
     * Отображение списка избранных записей.
     */
    fun showFavorites() {
        launch {
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


            // проверка нужно ли расшифровать ветку перед отображением
            /*if (FavoritesManager.isCryptedAndNonDecrypted()) {
            // запрос пароля в асинхронном режиме
            askPassword(FavoritesManager.FAVORITES_NODE);
        } else*/
//        {
            // выделяем ветку Избранное, только если загружено не одно Избранное
//            if (!App.IsLoadedFavoritesOnly) {
            if (!isLoadedFavoritesOnly()) {
//            setCurNode(null)
                setEvent(Constants.MainEvents.SetCurrentNode, FavoritesManager.FAVORITES_NODE)
//            setFavorIsCurNode(true)
//        postEvent(Constants.MainEvents.ShowFavorites)
            }
            showRecords(FavoritesManager.getFavoritesRecords(), Constants.MAIN_VIEW_FAVORITES, true)

            // сохраняем выбранную ветку
            saveLastSelectedNode()
//        }
        }
    }

    @MainThread
    fun addFavorite(context: Context?, record: TetroidRecord) {
        if (FavoritesManager.add(context, record)) {
            val mes = getString(R.string.log_added_to_favor)
            showMessage(mes)
            log(mes + ": " + StringUtils.getIdString(getContext(), record), false)
            updateFavorites(record)
        } else {
            logOperError(LogObj.RECORD, LogOper.ADD, getString(R.string.log_with_id_to_favor_mask).format(record.id), true, true)
        }
    }

    @MainThread
    fun removeFavorite(context: Context?, record: TetroidRecord) {
        if (FavoritesManager.remove(context, record, true)) {
            val mes = getString(R.string.log_deleted_from_favor)
            showMessage(mes)
            log(mes + ": " + StringUtils.getIdString(getContext(), record), false)
            updateFavorites()
            updateRecords()
        } else {
            logOperError(LogObj.RECORD, LogOper.DELETE, getString(R.string.log_with_id_from_favor_mask).format(record.id), true, true)
        }
    }

    @MainThread
    fun updateFavorites(record: TetroidRecord?): Boolean {
        if (record == null || !record.isFavorite) return false
        updateFavorites()
        updateRecords()
        return true
    }

    private fun updateFavorites() {
        setEvent(Constants.MainEvents.UpdateFavorites)
    }

    //endregion Favorites

    //region Global search

    /**
     * Открытие объекта из поисковой выдачи в зависимости от его типа.
     * @param found
     */
    fun openFoundObject(found: ITetroidObject) {
        launch {
            val type = found.type
            when (type) {
                FoundType.TYPE_RECORD -> openRecord(found as? TetroidRecord)
                FoundType.TYPE_FILE -> showRecordAttaches((found as? TetroidFile)?.record, false)
                FoundType.TYPE_NODE -> showNode(found as? TetroidNode)
                FoundType.TYPE_TAG -> showTag(found as? TetroidTag)
            }
            if (type != FoundType.TYPE_RECORD) {
                openPage(Constants.PAGE_MAIN)
            }
        }
    }

    fun research() {
        postEvent(Constants.MainEvents.GlobalResearch)
    }

    /**
     * Запуск глобального поиска.
     * @param profile
     */
    fun startGlobalSearch(profile: SearchProfile?) {
        if (profile == null) return

        this.lastSearchProfile = profile
        val searchInteractor = SearchInteractor(profile, storageInteractor, nodesInteractor, recordsInteractor)

        launch {
            log(getString(R.string.global_search_start).format(profile.query))
            setViewEvent(Constants.ViewEvents.TaskStarted, R.string.global_searching)
            val found = searchInteractor.globalSearch(getContext())
            setViewEvent(Constants.ViewEvents.TaskFinished, Gravity.NO_GRAVITY)

            if (found == null) {
                log(getString(R.string.log_global_search_return_null), true)
                return@launch
            } else if (profile.isSearchInNode && profile.node != null) {
                log(getString(R.string.global_search_by_node_result).format(profile.node?.name), true)
            }

            // уведомляем, если не смогли поискать в зашифрованных ветках
            if (searchInteractor.isExistCryptedNodes) {
                log(R.string.log_found_crypted_nodes, true)
            }
            log(String.format(getString(R.string.global_search_end), found.size))

            setEvent(Constants.MainEvents.GlobalSearchFinished, GlobalSearchParams(found, profile))
        }
    }

    fun startGlobalSearchFromFilterQuery() {
        postEvent(Constants.MainEvents.GlobalSearchStart, lastFilterQuery)
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
            log(R.string.search_records_search_select_node, true)
        }
    }

    private fun filterTagRecords(query: String) {
        if (curTag != null) {
            filterRecords(query, curTag!!.records, Constants.MAIN_VIEW_TAG_RECORDS)
        } else {
            log(R.string.search_records_select_tag, true)
        }
    }

    private fun filterRecords(query: String, records: List<TetroidRecord>, viewId: Int) {
        launch {
            val message = if (viewId == Constants.MAIN_VIEW_NODE_RECORDS)
                getString(R.string.filter_records_in_node_by_query).format(getCurNodeName(), query)
            else getString(R.string.filter_records_in_tag_by_query).format(getCurTagName(), query)
            log(message)
            val found = ScanManager.searchInRecordsNames(records, query)
            showRecords(found, viewId, false)
            if (lastFilterQuery.isNullOrEmpty()) {
                lastFilterQuery = query
            }
            setEvent(Constants.MainEvents.RecordsFiltered, FilteredObjectsInView(query, found, viewId))
        }
    }

    private fun filterRecordAttaches(query: String) {
        if (curRecord != null) {
            filterAttaches(query, curRecord!!) }
        else {
            logError(getString(R.string.log_cur_record_is_not_set), true)
        }
    }

    private fun filterAttaches(query: String, record: TetroidRecord) {
        launch {
            log(String.format(getString(R.string.filter_files_by_query), record.name, query))
            val found = ScanManager.searchInFiles(record.attachedFiles, query)
            showAttaches(found)
            setEvent(Constants.MainEvents.AttachesFiltered, FilteredObjectsInView(query, found, Constants.MAIN_VIEW_RECORD_FILES))
        }
    }

    fun onRecordsSearchClose() {
        launch {
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

                postEvent(Constants.MainEvents.StartFileObserver, bundle)
            }
        } else {
            postEvent(Constants.MainEvents.StopFileObserver)
        }
    }

    fun onStorageOutsideChanged() {
        // проверяем, не был ли запущен обработчик второй раз подряд
        if (!isStorageChangingHandled) {
            isStorageChangingHandled = true
            log(R.string.ask_storage_changed_outside)
            postStorageEvent(Constants.StorageEvents.ChangedOutside)
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

    fun onBeforeExit(activity: Activity) {
        // синхронизация перед выходом из приложения
//        viewModel.syncStorageAndRunCallback(this, {
//            onExit()
//            finish()
//        }))
        syncStorageAndExit(activity) {
            launch {
                onExit(activity)
                setEvent(Constants.MainEvents.Exit)
            }
        }
    }

    private fun onExit(activity: Activity) {
        log(R.string.log_app_exit)

        // останавливаем отслеживание изменения структуры хранилища
        FileObserverService.sendCommand(activity, FileObserverService.ACTION_STOP)
        FileObserverService.stop(activity)

        // удаляем загруженное хранилище из памяти
        App.destruct()
    }
}

class ClipboardParams(
    val operation: LogOper,
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