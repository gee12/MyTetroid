package com.gee12.mytetroid.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.gee12.mytetroid.*
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getIdString
import com.gee12.mytetroid.common.extensions.isFileExist
import com.gee12.mytetroid.usecase.InitAppUseCase
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.logs.TaskStage
import com.gee12.mytetroid.logs.TaskStage.Stages
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.data.crypt.IEncryptHelper
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.usecase.crypt.ChangePasswordUseCase
import com.gee12.mytetroid.usecase.crypt.CheckStoragePasswordAndDecryptUseCase
import com.gee12.mytetroid.usecase.crypt.CheckStoragePasswordAndAskUseCase
import com.gee12.mytetroid.usecase.node.CreateNodeUseCase
import com.gee12.mytetroid.usecase.node.InsertNodeUseCase
import com.gee12.mytetroid.usecase.crypt.DecryptStorageUseCase
import com.gee12.mytetroid.usecase.storage.CheckStorageFilesExistingUseCase
import com.gee12.mytetroid.usecase.storage.InitOrCreateStorageUseCase
import com.gee12.mytetroid.usecase.storage.ReadStorageUseCase
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.views.activities.TetroidActivity.IDownloadFileResult
import kotlinx.coroutines.*
import java.lang.Exception
import java.util.ArrayList
import java.util.HashMap

class MainViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    commonSettingsProvider: CommonSettingsProvider,
    appBuildHelper: AppBuildHelper,
    storageProvider: IStorageProvider,
    favoritesInteractor: FavoritesInteractor,
    sensitiveDataProvider: ISensitiveDataProvider,
    passInteractor: PasswordInteractor,
    storageCrypter: IEncryptHelper,
    cryptInteractor: EncryptionInteractor,
    recordsInteractor: RecordsInteractor,
    nodesInteractor: NodesInteractor,
    tagsInteractor: TagsInteractor,
    attachesInteractor: AttachesInteractor,
    storagesRepo: StoragesRepo,
    storageDataProcessor: IStorageDataProcessor,
    storagePathHelper: IStoragePathHelper,
    recordPathHelper: IRecordPathHelper,
    dataInteractor: DataInteractor,
    interactionInteractor: InteractionInteractor,
    syncInteractor: SyncInteractor,
    trashInteractor: TrashInteractor,
    private val migrationInteractor: MigrationInteractor,
    private val storageTreeInteractor: StorageTreeInteractor,
    initAppUseCase: InitAppUseCase,
    initOrCreateStorageUseCase: InitOrCreateStorageUseCase,
    readStorageUseCase: ReadStorageUseCase,
    private val createNodeUseCase: CreateNodeUseCase,
    saveStorageUseCase: SaveStorageUseCase,
    checkStoragePasswordUseCase: CheckStoragePasswordAndAskUseCase,
    changePasswordUseCase: ChangePasswordUseCase,
    private val insertNodeUseCase: InsertNodeUseCase,
    decryptStorageUseCase: DecryptStorageUseCase,
    checkStoragePasswordAndDecryptUseCase: CheckStoragePasswordAndDecryptUseCase,
    checkStorageFilesExistingUseCase: CheckStorageFilesExistingUseCase,
): StorageViewModel(
    app,
    resourcesProvider,
    logger,
    notificator,
    failureHandler,
    commonSettingsProvider,
    appBuildHelper,
    storageProvider,
    favoritesInteractor,
    sensitiveDataProvider,
    passInteractor,
    storageCrypter,
    cryptInteractor,
    recordsInteractor,
    nodesInteractor,
    tagsInteractor,
    attachesInteractor,
    storagesRepo,
    storagePathHelper,
    recordPathHelper,
    dataInteractor,
    interactionInteractor,
    syncInteractor,
    trashInteractor,
    initAppUseCase,
    initOrCreateStorageUseCase,
    readStorageUseCase,
    saveStorageUseCase,
    checkStoragePasswordUseCase,
    changePasswordUseCase,
    decryptStorageUseCase,
    checkStoragePasswordAndDecryptUseCase,
    checkStorageFilesExistingUseCase,
) {

    sealed class MainEvent : VMEvent() {
        // migration
        object Migrated : MainEvent()

        sealed class Node(val node: TetroidNode) : MainEvent() {
            class Encrypt(node: TetroidNode) : Node(node)
            class DropEncrypt(node: TetroidNode) : Node(node)
            class Show(node: TetroidNode) : Node(node)
            class Created(node: TetroidNode) : Node(node)
            class Inserted(node: TetroidNode) : Node(node)
            class Renamed(node: TetroidNode) : Node(node)
            class AskForDelete(node: TetroidNode) : Node(node)
            class Cutted(node: TetroidNode) : Node(node)
            class Deleted(node: TetroidNode) : Node(node)
        }
        data class SetCurrentNode(
            val node: TetroidNode?
        ) : MainEvent()
        object UpdateNodes : MainEvent()

        sealed class Record : MainEvent() {
            data class Open(
                val bundle: Bundle,
            ) : MainEvent()
            data class Deleted(
                val record: TetroidRecord,
            ) : MainEvent()
            data class Cutted(
                val record: TetroidRecord,
            ) : MainEvent()
        }
        data class ShowRecords(
            val records: List<TetroidRecord>,
            val viewId: Int,
            val dropSearch: Boolean = true,
        ) : MainEvent()
        data class RecordsFiltered(
            val query: String,
            val records: List<TetroidRecord>,
            val viewId: Int,
        ) : MainEvent()
        object UpdateRecords : MainEvent()

        // tags
        object UpdateTags : MainEvent()

        // attaches
        data class ShowAttaches(
            val attaches: List<TetroidFile>,
        ) : MainEvent()
        data class AttachesFiltered(
            val query: String,
            val attaches: List<TetroidFile>,
            val viewId: Int,
        ) : MainEvent()
        object UpdateAttaches : MainEvent()
        data class AttachDeleted(
            val attach: TetroidFile,
        ) : MainEvent()

        // favorites
        object UpdateFavoritesTitle : MainEvent()

        // global search
        data class GlobalSearchStart(
            val query: String?,
        ) : MainEvent()
        object GlobalResearch : MainEvent()
        data class GlobalSearchFinished(
            val found: HashMap<ITetroidObject, FoundType>,
            val profile: SearchProfile,
        ) : MainEvent()

        // file system
        data class AskForOperationWithoutDir(
            val clipboardParams: ClipboardParams,
        ) : MainEvent()
        data class AskForOperationWithoutFile(
            val clipboardParams: ClipboardParams,
        ) : MainEvent()
        object OpenFilePicker : MainEvent()
        object OpenFolderPicker : MainEvent()

        object Exit : MainEvent()
    }

    companion object {
        private const val MYTETRA_XML_EXISTING_DELAY = 1000L
    }

    var curMainViewId = Constants.MAIN_VIEW_NONE
    var lastMainViewId = 0

    var curNode: TetroidNode? = null
    var curRecord: TetroidRecord? = null
    var curTag: TetroidTag? = null
    var curFile: TetroidFile? = null

    var tempAttachToOpen: TetroidFile? = null
    var isDropRecordsFiltering = true
    var lastSearchProfile: SearchProfile? = null
    var lastFilterQuery: String? = null
    var isFromRecordActivity = false
    private var isStorageTreeChangingHandled = false

    init {
        // FIXME: koin: циклическая зависимость
        storageProvider.init(storageDataProcessor)

        setStorageTreeObserverCallbacks()
    }

    //region Migration

    /**
     * Проверка необходимости миграции и ее запуск.
     * Возвращает true, если миграция была запущена.
     */
    private fun checkAndStartMigration(): Boolean {
        val fromVersion = CommonSettings.getSettingsVersion(getContext())
        var result: Boolean? = null

        if (fromVersion == 0) { // новая установка, миграция не нужна
            CommonSettings.setSettingsCurrentVersion(getContext())
            return false
        }

        // 50
        if (fromVersion < Constants.SETTINGS_VERSION_CURRENT) {
            result = runBlocking { migrateTo50() }
        }
        // ..

        if (result == true) {
            CommonSettings.setSettingsCurrentVersion(getContext())
            launchOnMain {
                sendEvent(MainEvent.Migrated)
            }
        } else if (result == false) {
            logger.logError(R.string.log_error_migration, true)
            showSnackMoreInLogs()
        }
        return result != null
    }

    private suspend fun migrateTo50(): Boolean {
        logger.log(getString(R.string.log_start_migrate_to_version_mask, "5.0"), false)

        // параметры хранилища из SharedPreferences в бд
        if (migrationInteractor.isNeedMigrateStorageFromPrefs()) {
            if (!migrationInteractor.addDefaultStorageFromPrefs()) {
                return false
            }
        }

        return true
    }

    //endregion Migration

    //region Pages

    fun openPage(pageId: Int) {
        launchOnMain {
            sendViewEvent(ViewEvent.OpenPage(pageId))
        }
    }

    fun showMainView(viewId: Int) {
        // сохраняем значение для возврата на старое View
        // (только, если осуществляется переключение на действительно другую вьюшку)
        if (viewId != curMainViewId) {
            this.lastMainViewId = curMainViewId
        }
        launchOnMain {
            sendViewEvent(ViewEvent.ShowMainView(viewId))
        }
        this.curMainViewId = viewId
    }

    fun closeFoundFragment() {
        launchOnMain {
            sendViewEvent(ViewEvent.CloseFoundView)
        }
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
        launchOnMain {
            sendViewEvent(ViewEvent.UpdateToolbar(viewId, title))
        }
    }

    //endregion Pages

    //region Records

    fun getCurNodeName() = curNode?.name ?: ""

    fun showRecords(records: List<TetroidRecord>, viewId: Int, dropSearch: Boolean = true) {
        launchOnMain {
            sendEvent(MainEvent.ShowRecords(records, viewId, dropSearch))
        }
    }

    /**
     * Вставка записи в ветку.
     */
    fun insertRecord() {
        launchOnMain {
            // на всякий случай проверяем тип
            if (!TetroidClipboard.hasObject(FoundType.TYPE_RECORD))
                return@launchOnMain
            // достаем объект из "буфера обмена"
            val clipboard = TetroidClipboard.getInstance()
            // вставляем с попыткой восстановить каталог записи
            val record = clipboard.getObject() as TetroidRecord
            val isCutted = clipboard.isCutted
            val res = insertRecord(record, isCutted, false)
            when (res) {
                -1 -> {
                    sendEvent(MainEvent.AskForOperationWithoutDir(ClipboardParams(LogOper.INSERT, record, isCutted)))
                }
                -2 -> logOperErrorMore(LogObj.RECORD_DIR, LogOper.INSERT)
                else -> onInsertRecordResult(record, res, isCutted)
            }
        }
    }

    private suspend fun insertRecord(record: TetroidRecord, isCutted: Boolean, withoutDir: Boolean): Int {
        if (curNode == null) return -2
        sendViewEvent(
            ViewEvent.ShowProgressText(
                message = getString(if (isCutted) R.string.progress_insert_record else R.string.progress_copy_record)
            )
        )
        val result = recordsInteractor.insertRecord(getContext(), record, isCutted, curNode!!, withoutDir)
        sendViewEvent(ViewEvent.ShowProgress(isVisible = false))
        return result
    }

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
                updateFavoritesTitle(record)
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
        launchOnMain {
            val record = recordsInteractor.createRecord(getContext(), name, tags, author, url, node, isFavor)
            if (record != null) {
                logOperRes(LogObj.RECORD, LogOper.CREATE, record, false)
                updateTags()
                updateNodes()
                if (!updateFavoritesTitle(record)) {
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
        launchOnMain {
            // имя записи
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            var url: String? = null
            if (Build.VERSION.SDK_INT >= 17) {
                url = intent.getStringExtra(Intent.EXTRA_ORIGINATING_URI)
            }
            // создаем запись
            val node = quicklyNode ?: storageProvider.getRootNode()
            val record: TetroidRecord = recordsInteractor.createTempRecord(
                context = getContext(),
                srcName = subject,
                url = url,
                text = text,
                node = node
            ) ?: return@launchOnMain
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
                        if (attachesInteractor.attachFile(uriHelper.getPath(uri), record) == null) {
                            hasError = true
                        }
                    }
                    if (hasError) {
                        logWarning(R.string.log_files_attach_error, true)
                        sendViewEvent(ViewEvent.ShowMoreInLogs)
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
        launchOnMain {
            val res = deleteRecord(record, false)
            if (res == -1) {
                sendEvent(MainEvent.AskForOperationWithoutDir(ClipboardParams(LogOper.DELETE, record)))
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
    private suspend fun onDeleteRecordResult(record: TetroidRecord, res: Int, isCutted: Boolean) {
        if (res > 0) {
            sendEvent(MainEvent.Record.Deleted(record))
            updateTags()
            updateNodes()
            // обновляем избранное
            if (!updateFavoritesTitle(record)) {
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
            sendViewEvent(ViewEvent.ShowMoreInLogs)
        } else {
            logOperErrorMore(LogObj.RECORD, if (isCutted) LogOper.CUT else LogOper.DELETE)
        }
    }

    override fun editRecordFields(record: TetroidRecord, name: String, tags: String, author: String, url: String, node: TetroidNode, isFavor: Boolean) {
        launchOnMain {
            val oldNode = record.node
            if (recordsInteractor.editRecordFields(record, name, tags, author, url, node, isFavor)) {
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
        launchOnMain {
            if (nodeChanged && record != null) {
                showNode(record.node)
            } else {
                updateRecords()
            }
            updateTags()
            updateFavoritesTitle()
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
        launchOnMain {
            val res = if (curMainViewId == Constants.MAIN_VIEW_FAVORITES)
                favoritesInteractor.swap(pos, isUp, true)
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
        launchOnMain {
            // добавляем в "буфер обмена"
            TetroidClipboard.cut(record)
            // удаляем запись из текущей ветки и каталог перемещаем в корзину
            val res: Int = cutRecord(record, false)
            if (res == -1) {
                sendEvent(MainEvent.AskForOperationWithoutDir(ClipboardParams(LogOper.CUT, record)))
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
        launchOnMain {
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
            logError(R.string.log_record_is_null, show = true)
            return
        }
        // проверка нужно ли расшифровать избранную запись перед отображением
        // (т.к. в избранной ветке записи могут быть нерасшифрованные)
        if (!checkAndDecryptRecord(record)) {
            openRecord(record.id)
        }
    }

    /**
     * Открытие записи по Id.
     * @param recordId
     */
    fun openRecord(recordId: String) {
        launchOnMain {
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
        launchOnMain {
            sendEvent(MainEvent.Record.Open(bundle))
        }
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

    private fun updateRecords() {
        launchOnMain {
            sendEvent(MainEvent.UpdateRecords)
        }
    }

    //endregion Records

    //region Nodes

    /**
     * Открытие записей ветки.
     * @param node
     */
    fun showNode(node: TetroidNode?) {
        if (node != null) {
            // проверка нужно ли расшифровать ветку перед отображением
            if (checkAndDecryptNode(node)) return

            launchOnMain {
                log(getString(R.string.log_open_node) + node.getIdString(resourcesProvider))
                curNode = node
                sendEvent(MainEvent.SetCurrentNode(node))
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
            val curNode = if (curMainViewId == Constants.MAIN_VIEW_FAVORITES) FavoritesInteractor.FAVORITES_NODE else curNode
            setLastNodeId(curNode?.id)
            updateStorageAsync()
        }
    }

    fun createNode(name: String, trueParentNode: TetroidNode?) {
        launchOnMain {
            withIo {
                createNodeUseCase.run(
                    CreateNodeUseCase.Params(
                        name = name,
                        parentNode = trueParentNode,
                    )
                )
            }.onFailure { failure ->
                logFailure(failure)
                logOperErrorMore(LogObj.NODE, LogOper.CREATE)
            }.onSuccess { node ->
                sendEvent(MainEvent.Node.Created(node))
            }
        }
    }

    /**
     * Переименование ветки.
     * @param node
     */
    fun renameNode(node: TetroidNode, newName: String) {
        launchOnMain {
            if (nodesInteractor.editNodeFields(node, newName)) {
                logOperRes(LogObj.NODE, LogOper.RENAME)
                updateNodes()
                sendEvent(MainEvent.Node.Renamed(node))
            } else {
                logOperErrorMore(LogObj.NODE, LogOper.RENAME)
            }
        }
    }

    fun setNodeIcon(nodeId: String?, iconPath: String?, isDrop: Boolean) {
        if (nodeId == null) return
        launchOnMain {
            val node = if (curNode?.id == nodeId) curNode else nodesInteractor.getNode(nodeId)
            if (nodesInteractor.setNodeIcon(node, iconPath, isDrop)) {
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
        val isCut = clipboard.isCutted
        val trueParentNode = if (isSubNode) parentNode else parentNode.parentNode

        launchOnMain {
            withIo {
                insertNodeUseCase.run(
                    InsertNodeUseCase.Params(
                        srcNode = node,
                        parentNode = trueParentNode,
                        isCut = isCut,
                    )
                )
            }.onFailure {
                logFailure(it, show = false)
                logOperErrorMore(LogObj.NODE, LogOper.INSERT)
            }.onSuccess { newNode ->
                // ищем вновь созданную ветку - копию node
                //  (даже при вставке ВЫРЕЗАННОЙ ветки вставляется ее копия, а не оригинальная из буфера обмена)
                sendEvent(MainEvent.Node.Inserted(newNode))
            }
        }
    }

    /**
     * Вырезание ветки из родительской ветки.
     * @param node
     */
    fun cutNode(node: TetroidNode, pos: Int) {
        // нельзя вырезать нерасшифрованную ветку
        if (!node.isNonCryptedOrDecrypted()) {
            log(R.string.log_cannot_delete_undecrypted_node, true)
            return
        }
        // нельзя вырезать ветку, у которой есть дочерние нерасшифрованные ветки
        if (nodesInteractor.hasNonDecryptedNodes(node)) {
            log(getString(R.string.log_enter_pass_first), true)
            return
        }
        // добавляем в "буфер обмена"
        TetroidClipboard.cut(node)
        launchOnMain {
            // удаляем ветку из родительской ветки вместе с записями
            val res = nodesInteractor.cutNode(node)
            onDeleteNodeResult(node, res, true)
        }
    }

    /**
     * Удаление ветки.
     * @param node
     */
    fun startDeleteNode(node: TetroidNode?) {
        if (node == null) return
        // нельзя удалить нерасшифрованную ветку
        if (!node.isNonCryptedOrDecrypted) {
            log(R.string.log_cannot_delete_undecrypted_node, true)
            return
        }
        // нельзя удалить последнюю ветку в корне
        if (node.level == 0 && getRootNodes().size == 1) {
            log(R.string.log_cannot_delete_root_node, true)
            return
        }
        launchOnMain {
            sendEvent(MainEvent.Node.AskForDelete(node))
        }
    }

    fun deleteNode(node: TetroidNode) {
        launchOnMain {
            val res = nodesInteractor.deleteNode(node)
            onDeleteNodeResult(node, res, false)
        }
    }

    private fun onDeleteNodeResult(node: TetroidNode, res: Boolean, isCutted: Boolean) {
        if (res) {
            launchOnMain {
                sendEvent(if (isCutted) MainEvent.Node.Cutted(node) else MainEvent.Node.Deleted(node))
            }
            // удаляем элемент внутри списка
//            if (mListAdapterNodes.deleteItem(pos)) {
//                logOperRes(this, LogObj.NODE, if (!isCutted) LogOper.DELETE else LogOper.CUT)
//            } else {
//                LogManager.log(this, getString(R.string.log_node_delete_list_error), ILogger.Types.ERROR, Toast.LENGTH_LONG)
//            }
            // обновляем label с количеством избранных записей
            if (appBuildHelper.isFullVersion()) {
                updateFavoritesTitle()
            }
            // убираем список записей удаляемой ветки
            if (curNode == node || nodesInteractor.isNodeInNode(curNode, node)) {
//                getMainPage().clearView()
                curRecord = null
                curNode = null
                launchOnMain {
                    sendViewEvent(ViewEvent.ClearMainView)
                }
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
            showMessage(R.string.mes_quickly_node_cannot_encrypt)
        } else {
            checkStoragePass(
                callbackEvent = MainEvent.Node.Encrypt(node)
            )
        }
    }

    /**
     * Сброс шифрования ветки.
     * @param node
     */
    fun startDropEncryptNode(node: TetroidNode) {
        checkStoragePass(
            MainEvent.Node.DropEncrypt(node)
        )
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
        launchOnMain {
            sendViewEvent(
                ViewEvent.TaskStarted(
                    if (isEncrypt) R.string.task_node_encrypting else R.string.task_node_drop_crypting
                )
            )

            logOperStart(LogObj.NODE, if (isEncrypt) LogOper.ENCRYPT else LogOper.DROPCRYPT, node)

            val nodeWasEncrypted = node.isCrypted
            val operation = if (isEncrypt) LogOper.ENCRYPT else LogOper.DROPCRYPT

            val result = withIo {
                // сначала расшифровываем хранилище
                if (isStorageCrypted() && !isStorageDecrypted()) {
                    setStage(LogObj.STORAGE, LogOper.DECRYPT, Stages.START)

                    decryptStorageUseCase.run(
                        DecryptStorageUseCase.Params(decryptFiles = false)
                    ).map { result ->
                        setIsDecrypted(result)
                    }.foldResult(
                        onLeft = {
                            logFailure(it)
                            setStage(LogObj.STORAGE, LogOper.DECRYPT, Stages.FAILED)
                            return@withIo -2
                        },
                        onRight = {
                            setStage(LogObj.STORAGE, LogOper.DECRYPT, Stages.SUCCESS)
                        }
                    )
                }

                // зашифровуем, только если хранилище не зашифровано или уже расшифровано
                return@withIo if (isStorageNonEncryptedOrDecrypted()) {
                    setStage(LogObj.NODE, operation, Stages.START)

                    val result = if (isEncrypt) cryptInteractor.encryptNode(node)
                    else cryptInteractor.dropCryptNode(node)

                    if (result && saveStorage()) 1 else -1
                } else 0
            }

            sendViewEvent(ViewEvent.TaskFinished)

            if (result > 0) {
                logOperRes(LogObj.NODE, operation)
                if (!isEncrypt && nodeWasEncrypted) {
                    // проверяем существование зашифрованных веток
                    checkExistenceCryptedNodes()
                }
            } else {
                logOperErrorMore(LogObj.NODE, operation)
            }
            afterStorageDecrypted(null)
        }
    }

    override fun afterStorageDecrypted(node: TetroidNode?) {
        launchOnMain {
            sendStorageEvent(StorageEvent.Decrypted)
            if (node != null) {
                if (node === FavoritesInteractor.FAVORITES_NODE) {
                    showFavorites()
                } else {
                    showNode(node)
                }
            }
            sendViewEvent(ViewEvent.HandleReceivedIntent)
        }
    }

    private fun setStage(obj: LogObj, oper: LogOper, stage: Stages) {
        val taskStage = TaskStage(Constants.TetroidView.Main, obj, oper, stage)
        launchOnMain {
            sendViewEvent(
                ViewEvent.ShowProgressText(
                    message = logger.logTaskStage(taskStage).orEmpty()
                )
            )
        }
    }

    fun swapNodes(nodes: List<TetroidNode>, pos: Int, isUp: Boolean): Int {
        return runBlocking {
            swapTetroidObjects(nodes, pos, isUp, true)
        }
    }

    fun updateNodes() {
        launchOnMain {
            sendEvent(MainEvent.UpdateNodes)
        }
    }

    //endregion Nodes

    //region Tags

    fun getCurTagName() = curTag?.name ?: ""

    fun showTag(tagName: String) {
        val tag = tagsInteractor.getTag(tagName)
        if (tag != null) {
            showTag(tag)
        } else {
            logWarning(getString(R.string.search_tag_not_found_mask, tagName), true)
        }
    }

    /**
     * Отображение записей по метке.
     * @param tag
     */
    fun showTag(tag: TetroidTag?) {
        launchOnMain {
            if (tag != null) {
                curTag = tag
                // сбрасываем текущую ветку
                sendEvent(MainEvent.SetCurrentNode(node = null))
                log(getString(R.string.log_open_tag_records_mask, tag.name))
                showRecords(tag.records, Constants.MAIN_VIEW_TAG_RECORDS)
            } else {
                logError(R.string.log_tag_is_null, true)
            }
        }
    }

    fun renameTag(tag: TetroidTag?, name: String) {
        if (tag == null || tag.name == name) return
        launchOnMain {
            if (tagsInteractor.renameTag(tag, name)) {
                logOperRes(LogObj.TAG, LogOper.RENAME)
                updateTags()
                updateRecords()
            } else {
                logOperErrorMore(LogObj.TAG, LogOper.RENAME)
            }
        }
    }

    fun updateTags() {
        launchOnMain {
            sendEvent(MainEvent.UpdateTags)
        }
    }

    //endregion Tags

    // region Attaches

    fun checkPermissionAndOpenAttach(activity: Activity, attach: TetroidFile) {
        if (attach.isNonCryptedOrDecrypted) {
            openAttach(attach)
        } else {
            if (isDecryptAttachesToTemp()) {
                if (checkWriteExtStoragePermission(activity, Constants.REQUEST_CODE_PERMISSION_WRITE_TEMP)) {
                    openAttach(attach)
                } else {
                    // будет запрос разрешения на запись расшифрованного файла в память
                    tempAttachToOpen = attach
                }
            } else {
                log(R.string.log_viewing_decrypted_not_possible, true)
            }
        }
    }

    fun checkPermissionAndOpenTempAttach(activity: Activity) {
        tempAttachToOpen?.let {
            checkPermissionAndOpenAttach(activity, it)
        }
    }

    fun openAttach(attach: TetroidFile) {
        launchOnMain {
            attachesInteractor.openAttach(getContext(), attach)
        }
    }

    fun showAttaches(attaches: List<TetroidFile>) {
        launchOnMain {
            sendEvent(MainEvent.ShowAttaches(attaches))
        }
    }

    /**
     * Отображение списка прикрепленных файлов.
     * @param record Запись
     */
    fun showRecordAttaches(record: TetroidRecord?, fromRecordActivity: Boolean = false) {
        if (record == null) return
        launchOnMain {
            curNode = record.node
            curRecord = record
            isFromRecordActivity = fromRecordActivity
            showAttaches(record.attachedFiles)
            openPage(Constants.PAGE_MAIN)
        }
    }

    open fun attachFile(fileFullName: String, record: TetroidRecord?, deleteSrcFile: Boolean) {
        launchOnMain {
            sendViewEvent(ViewEvent.TaskStarted(R.string.task_attach_file))
            val attach = withIo {
                attachesInteractor.attachFile(fileFullName, record, deleteSrcFile)
            }
            sendViewEvent(ViewEvent.TaskFinished/*, Gravity.NO_GRAVITY*/)
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
                sendViewEvent(ViewEvent.ShowMoreInLogs)
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
        launchOnMain {
            sendEvent(MainEvent.OpenFilePicker)
        }
    }

    /**
     * Ввод URL для загрузки и прикрепления файла к текущей записи.
     */
    fun downloadAndAttachFile(url: String) {
        launchOnMain {
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
        launchOnMain {
            when (val res: Int = attachesInteractor.deleteAttachedFile(file, false)) {
                -2 -> sendEvent(MainEvent.AskForOperationWithoutFile(ClipboardParams(LogOper.DELETE, file)))
                -1 -> sendEvent(MainEvent.AskForOperationWithoutDir(ClipboardParams(LogOper.DELETE, file)))
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
            launchOnMain {
                sendEvent(MainEvent.AttachDeleted(attach = file))
            }
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
        launchOnMain {
            when (val res = attachesInteractor.editAttachedFileFields(file, name)) {
                -2 -> sendEvent(MainEvent.AskForOperationWithoutFile(ClipboardParams(LogOper.RENAME, file)))
                // TODO: добавить вариант Создать каталог записи
                -1 -> sendEvent(MainEvent.AskForOperationWithoutDir(ClipboardParams(LogOper.RENAME, file)))
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
        launchOnMain {
            when (params.operation) {
                LogOper.RENAME,
                LogOper.DELETE -> {
                    if (params.obj is TetroidFile) {
                        val file = params.obj
                        val res = attachesInteractor.deleteAttachedFile(file, true)
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
        launchOnMain {
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
        launchOnMain {
            sendEvent(MainEvent.OpenFolderPicker)
        }
    }

    /**
     * Сохранение файла по выбранному пути.
     */
    fun saveCurAttachOnDevice(folderPath: String) {
        launchOnMain {
            sendViewEvent(ViewEvent.TaskStarted(R.string.task_file_saving))
            val res = curFile?.let {
                attachesInteractor.saveFile(it, folderPath)
            } ?: false
            sendViewEvent(ViewEvent.TaskFinished/*, Gravity.NO_GRAVITY*/)
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
        launchOnMain {
            sendEvent(MainEvent.UpdateAttaches)
        }
    }

    //endregion Attaches

    //region Favorites

    /**
     * Отображение списка избранных записей.
     */
    fun showFavorites() {
        launchOnMain {
            // выделяем ветку Избранное, только если загружено не одно Избранное
            if (!isLoadedFavoritesOnly()) {
                sendEvent(MainEvent.SetCurrentNode(node = FavoritesInteractor.FAVORITES_NODE))
            }
            showRecords(getFavoriteRecords(), Constants.MAIN_VIEW_FAVORITES, dropSearch = true)

            // сохраняем выбранную ветку
            saveLastSelectedNode()
        }
    }

    fun addToFavorite(record: TetroidRecord) {
        launchOnMain {
            if (favoritesInteractor.add(record)) {
                val mes = getString(R.string.log_added_to_favor)
                showMessage(mes)
                log(mes + ": " + record.getIdString(resourcesProvider), false)
                updateFavoritesTitle(record)
            } else {
                logOperError(LogObj.RECORD, LogOper.ADD, getString(R.string.log_with_id_to_favor_mask, record.id), true, true)
            }
        }
    }

    fun removeFromFavorite(record: TetroidRecord) {
        launchOnMain {
            if (favoritesInteractor.remove(record, true)) {
                val mes = getString(R.string.log_deleted_from_favor)
                showMessage(mes)
                log(mes + ": " + record.getIdString(resourcesProvider), false)
                updateFavoritesTitle()
                updateRecords()
            } else {
                logOperError(LogObj.RECORD, LogOper.DELETE, getString(R.string.log_with_id_from_favor_mask, record.id), true, true)
            }
        }
    }

    fun updateFavoritesTitle(record: TetroidRecord?): Boolean {
        if (record == null || !record.isFavorite) return false
        updateFavoritesTitle()
        updateRecords()
        return true
    }

    private fun updateFavoritesTitle() {
        launchOnMain {
            sendEvent(MainEvent.UpdateFavoritesTitle)
        }
    }

    //endregion Favorites

    //region Global search

    /**
     * Открытие объекта из поисковой выдачи в зависимости от его типа.
     * @param found
     */
    fun openFoundObject(found: ITetroidObject) {
        launchOnMain {
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
        launchOnMain {
            sendEvent(MainEvent.GlobalResearch)
        }
    }

    /**
     * Запуск глобального поиска.
     * @param profile
     */
    fun startGlobalSearch(profile: SearchProfile) {
        this.lastSearchProfile = profile
        // TODO: to UseCase
        val searchInteractor = SearchInteractor(profile, storageProvider, nodesInteractor, recordsInteractor)

        launchOnMain {
            log(getString(R.string.global_search_start, profile.query))
            sendViewEvent(ViewEvent.TaskStarted(R.string.global_searching))
            val found = searchInteractor.globalSearch()
            sendViewEvent(ViewEvent.TaskFinished/*, Gravity.NO_GRAVITY*/)

            if (found == null) {
                log(getString(R.string.log_global_search_return_null), true)
                return@launchOnMain
            } else if (profile.isSearchInNode) {
                profile.node?.let { node ->
                    log(getString(R.string.global_search_by_node_result, node.name), true)
                }
            }

            // уведомляем, если не смогли поискать в зашифрованных ветках
            if (searchInteractor.isExistCryptedNodes) {
                log(R.string.log_found_crypted_nodes, true)
            }
            log(getString(R.string.global_search_end, found.size))

            sendEvent(MainEvent.GlobalSearchFinished(found, profile))
        }
    }

    fun startGlobalSearchFromFilterQuery() {
        launchOnMain {
            sendEvent(MainEvent.GlobalSearchStart(query = lastFilterQuery))
        }
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
        launchOnMain {
            val message = if (viewId == Constants.MAIN_VIEW_NODE_RECORDS)
                getString(R.string.filter_records_in_node_by_query, getCurNodeName(), query)
            else getString(R.string.filter_records_in_tag_by_query, getCurTagName(), query)
            log(message)
            val found = ScanManager.searchInRecordsNames(records, query)
            showRecords(found, viewId, dropSearch = false)
            if (lastFilterQuery.isNullOrEmpty()) {
                lastFilterQuery = query
            }
            sendEvent(MainEvent.RecordsFiltered(query, found, viewId))
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
        launchOnMain {
            log(getString(R.string.filter_files_by_query, record.name, query))
            val found = ScanManager.searchInFiles(record.attachedFiles, query)
            showAttaches(found)
            sendEvent(MainEvent.AttachesFiltered(
                query = query,
                attaches = found,
                viewId = Constants.MAIN_VIEW_RECORD_FILES,
            ))
        }
    }

    fun onRecordsSearchClose() {
        launchOnMain {
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

    private fun setStorageTreeObserverCallbacks() {
        with(storageTreeInteractor) {
            treeChangedCallback = { event ->
                // обработка внешнего изменения дерева записей
                onStorageTreeOutsideChanged(event)
            }
            observerStartedCallback = {
                isStorageTreeChangingHandled = true
            }
            observerStoppedCallback = {
                isStorageTreeChangingHandled = false
            }
        }
    }

    /**
     * Обработчик изменения структуры хранилища извне.
     */
    fun startStorageTreeObserverIfNeeded() {
        if (isCheckOutsideChanging()) {
            // запускаем мониторинг, только если хранилище загружено
            if (isStorageLoaded()) {
                this.isStorageTreeChangingHandled = false

                launchOnMain {
                    storageTreeInteractor.startStorageTreeObserver(
                        storagePath = storagePathHelper.getPathToMyTetraXml()
                    ) /*{ event ->
                        // обработка внешнего изменения дерева записей
                        onStorageTreeOutsideChanged(event)
                    }*/
                }
            }
        } else {
            storageTreeInteractor.stopStorageTreeObserver()
        }
    }

    fun onStorageTreeOutsideChanged(event: TetroidFileObserver.Event) {
        // проверяем, не был ли запущен обработчик второй раз подряд
        if (!isStorageTreeChangingHandled) {
            isStorageTreeChangingHandled = true

            when (event) {
                TetroidFileObserver.Event.Modified -> {
                    log(R.string.log_storage_tree_changed_outside)
                    launchOnMain {
                        this@MainViewModel.sendStorageEvent(StorageEvent.TreeChangedOutside)
                    }
                }
                TetroidFileObserver.Event.Moved,
                TetroidFileObserver.Event.Deleted -> {
                    // проверяем существование mytetra.xml только после задержки (т.к. файл мог быть пересоздан)
                    launchOnMain {
                        withIo {
                            delay(MYTETRA_XML_EXISTING_DELAY)
                        }
                        if (storagePathHelper.getPathToMyTetraXml().isFileExist()) {
                            log(R.string.log_storage_tree_changed_outside)
                            this@MainViewModel.sendStorageEvent(StorageEvent.TreeChangedOutside)
                        } else {
                            log(R.string.log_storage_tree_deleted_outside)
                            this@MainViewModel.sendStorageEvent(StorageEvent.TreeDeletedOutside)
                        }
                    }
                }
            }
        }
    }

    fun dropIsStorageChangingHandled() {
        isStorageTreeChangingHandled = false
    }

    fun saveMytetraXmlFromCurrentStorageTree() {
        launchOnMain {
            if (saveStorage()) {
                showMessage(R.string.mes_storage_tree_saved)
            }
            isStorageTreeChangingHandled = false
        }
    }

    //endregion FileObserver

    //region Main view

    fun onUICreated() {
        launchOnMain {
            if (isStorageLoaded()) {
                // инициализация контролов
                sendViewEvent(
                    ViewEvent.InitGUI(
                        storage = storage!!,
                        isLoadFavoritesOnly = isLoadedFavoritesOnly(),
                        isHandleReceivedIntent = true,
                        result = true
                    )
                )
                // действия после загрузки хранилища
                sendStorageEvent(StorageEvent.Loaded(result = true))
            } else {
                // проверяем необходимость миграции
                val mirgationStarted = withIo {
                    checkAndStartMigration()
                }

                if (!mirgationStarted) {
                    // загружаем хранилище, если еще не загружано
                    startInitStorage()
                }
            }
        }
    }

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
        // очистка корзины перед выходом из приложения
        clearTrashFolderAndExit(true) { result ->
            if (result) {
                // синхронизация перед выходом из приложения
                syncStorageAndExit(activity)
            }
        }
    }

    fun syncStorageAndExit(activity: Activity) {
        syncStorageAndExit(activity) { result ->
            if (result) {
                // выход из приложения
                exitAfterAsks()
            } else {
                // спрашиваем что делать
                launchOnMain {
                    sendStorageEvent(StorageEvent.AskForSyncAfterFailureSyncOnExit)
                }
            }
        }
    }

    fun exitAfterAsks() {
        launchOnMain {
            onExit()
            sendEvent(MainEvent.Exit)
        }
    }

    /**
     * Запуск очистки корзины (если опция включена) и выход из приложения.
     */
    fun clearTrashFolderAndExit(isNeedAsk: Boolean, callback: ICallback?) {
        if (storage == null) {
            callback?.run(true)
            return
        }
        launchOnMain {
            when (trashInteractor.clearTrashFolderBeforeExit(storage!!, isNeedAsk)) {
                TrashInteractor.TrashClearResult.SUCCESS -> {
                    log(R.string.title_trash_cleared, false)
                    callback?.run(true)
                }
                TrashInteractor.TrashClearResult.FAILURE -> {
                    logError(R.string.title_trash_clear_error, true)
                    callback?.run(false)
                }
                TrashInteractor.TrashClearResult.NEED_ASK -> {
                    sendStorageEvent(StorageEvent.AskBeforeClearTrashOnExit(callback))
                }
                TrashInteractor.TrashClearResult.NONE -> {
                    callback?.run(true)
                }
            }
        }
    }

    private fun onExit() {
        log(R.string.log_app_exit)

        // останавливаем отслеживание изменения структуры хранилища
        storageTreeInteractor.stopStorageTreeObserver()

        // удаляем сохраненный хэш пароля из памяти
        sensitiveDataProvider.resetMiddlePassHash()

        // удаляем загруженное хранилище из памяти
        storageProvider.resetStorage()
    }

    //endregion Main view

}

class ClipboardParams(
    val operation: LogOper,
    val obj: TetroidObject,
    val isCutted: Boolean = false
)
