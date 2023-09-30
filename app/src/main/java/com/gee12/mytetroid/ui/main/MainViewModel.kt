package com.gee12.mytetroid.ui.main

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.child
import com.gee12.mytetroid.*
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getIdString
import com.gee12.mytetroid.common.extensions.isFileExist
import com.gee12.mytetroid.common.extensions.orZero
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.logs.TaskStage
import com.gee12.mytetroid.logs.TaskStage.Stages
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.domain.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.domain.repo.StoragesRepo
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.TetroidActivity
import com.gee12.mytetroid.ui.storage.StorageEvent
import com.gee12.mytetroid.domain.interactor.*
import com.gee12.mytetroid.domain.manager.*
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.domain.usecase.GlobalSearchUseCase
import com.gee12.mytetroid.domain.usecase.SwapObjectsInListUseCase
import com.gee12.mytetroid.domain.usecase.crypt.*
import com.gee12.mytetroid.domain.usecase.file.GetFileModifiedDateInStorageUseCase
import com.gee12.mytetroid.domain.usecase.file.GetFolderSizeInStorageUseCase
import com.gee12.mytetroid.domain.usecase.node.CreateNodeUseCase
import com.gee12.mytetroid.domain.usecase.node.GetNodeByIdUseCase
import com.gee12.mytetroid.domain.usecase.attach.*
import com.gee12.mytetroid.domain.usecase.node.*
import com.gee12.mytetroid.domain.usecase.node.icon.LoadNodeIconUseCase
import com.gee12.mytetroid.domain.usecase.node.icon.SetNodeIconUseCase
import com.gee12.mytetroid.domain.usecase.record.*
import com.gee12.mytetroid.domain.usecase.storage.*
import com.gee12.mytetroid.domain.usecase.tag.GetTagByNameUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import com.gee12.mytetroid.domain.usecase.tag.RenameTagInRecordsUseCase
import com.gee12.mytetroid.model.enums.TagsSearchMode
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.ui.storage.StorageViewModel
import kotlinx.coroutines.*
import java.lang.Exception
import java.util.ArrayList

class MainViewModel(
    app: Application,
    buildInfoProvider: BuildInfoProvider,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,

    settingsManager: CommonSettingsManager,
    appPathProvider: IAppPathProvider,
    storageProvider: IStorageProvider,
    favoritesManager: FavoritesManager,
    sensitiveDataProvider: ISensitiveDataProvider,
    storagePathProvider: IStoragePathProvider,
    recordPathProvider: IRecordPathProvider,
    dataNameProvider: IDataNameProvider,
    private val storageSettingsProvider: IStorageSettingsProvider,

    storagesRepo: StoragesRepo,
    cryptManager: IStorageCryptManager,
    storageDataProcessor: IStorageDataProcessor,

    interactionManager: InteractionManager,
    syncInteractor: SyncInteractor,
    private val storageTreeInteractor: StorageTreeObserver,

    private val globalSearchUseCase: GlobalSearchUseCase,
    getFileModifiedDateUseCase: GetFileModifiedDateInStorageUseCase,
    getFolderSizeUseCase: GetFolderSizeInStorageUseCase,
    private val swapObjectsInListUseCase: SwapObjectsInListUseCase,

    initOrCreateStorageUseCase: InitOrCreateStorageUseCase,
    readStorageUseCase: ReadStorageUseCase,
    saveStorageUseCase: SaveStorageUseCase,
    decryptStorageUseCase: DecryptStorageUseCase,
    checkStorageFilesExistingUseCase: CheckStorageFilesExistingUseCase,
    clearStorageTrashFolderUseCase: ClearStorageTrashFolderUseCase,
    checkPasswordOrPinAndDecryptUseCase: CheckPasswordOrPinAndDecryptUseCase,
    checkPasswordOrPinUseCase: CheckPasswordOrPinAndAskUseCase,
    changePasswordUseCase: ChangePasswordUseCase,
    setupPasswordUseCase: SetupPasswordUseCase,
    private val initPasswordUseCase: InitPasswordUseCase,
    private val checkPasswordUseCase: CheckPasswordUseCase,
    private val dropAllPasswordDataUseCase: DropAllPasswordDataUseCase,

    getNodeByIdUseCase: GetNodeByIdUseCase,
    private val createNodeUseCase: CreateNodeUseCase,
    private val insertNodeUseCase: InsertNodeUseCase,
    private val cutOrDeleteNodeUseCase: CutOrDeleteNodeUseCase,
    private val editNodeFieldsUseCase: EditNodeFieldsUseCase,
    private val loadNodeIconUseCase: LoadNodeIconUseCase,
    private val setNodeIconUseCase: SetNodeIconUseCase,

    getRecordByIdUseCase: GetRecordByIdUseCase,
    private val insertRecordUseCase: InsertRecordUseCase,
    private val createRecordUseCase: CreateRecordUseCase,
    private val createTempRecordUseCase: CreateTempRecordUseCase,
    private val editRecordFieldsUseCase: EditRecordFieldsUseCase,
    private val cutOrDeleteRecordUseCase: CutOrDeleteRecordUseCase,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,

    private val getUriFromAttachUseCase: PrepareAttachForOpenUseCase,
    private val createAttachToRecordUseCase: AttachFileToRecordUseCase,
    private val deleteAttachUseCase: DeleteAttachUseCase,
    private val editAttachFieldsUseCase: EditAttachFieldsUseCase,
    private val saveAttachToFileUseCase: SaveAttachToFileUseCase,

    private val getTagByNameUseCase: GetTagByNameUseCase,
    private val renameTagInRecordsUseCase: RenameTagInRecordsUseCase,

    cryptRecordFilesIfNeedUseCase: CryptRecordFilesIfNeedUseCase,
    parseRecordTagsUseCase: ParseRecordTagsUseCase,
): StorageViewModel(
    app = app,
    buildInfoProvider = buildInfoProvider,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,

    settingsManager = settingsManager,
    appPathProvider = appPathProvider,
    storageProvider = storageProvider,
    sensitiveDataProvider = sensitiveDataProvider,
    storagePathProvider = storagePathProvider,
    recordPathProvider = recordPathProvider,
    dataNameProvider = dataNameProvider,

    storagesRepo = storagesRepo,
    cryptManager = cryptManager,

    favoritesManager = favoritesManager,
    interactionManager = interactionManager,
    syncInteractor = syncInteractor,

    getFileModifiedDateUseCase = getFileModifiedDateUseCase,
    getFolderSizeUseCase = getFolderSizeUseCase,

    initOrCreateStorageUseCase = initOrCreateStorageUseCase,
    readStorageUseCase = readStorageUseCase,
    saveStorageUseCase = saveStorageUseCase,
    decryptStorageUseCase = decryptStorageUseCase,
    checkStorageFilesExistingUseCase = checkStorageFilesExistingUseCase,
    clearStorageTrashFolderUseCase = clearStorageTrashFolderUseCase,
    checkPasswordOrPinAndDecryptUseCase = checkPasswordOrPinAndDecryptUseCase,
    checkPasswordOrPinUseCase = checkPasswordOrPinUseCase,
    changePasswordUseCase = changePasswordUseCase,
    setupPasswordUseCase = setupPasswordUseCase,

    getNodeByIdUseCase = getNodeByIdUseCase,
    getRecordByIdUseCase = getRecordByIdUseCase,
) {

    companion object {
        private const val MYTETRA_XML_EXISTING_DELAY = 1000L
    }

    var currentMainViewType: MainViewType = MainViewType.NONE
        private set
    private var lastMainViewType: MainViewType = MainViewType.NONE

    var curNode: TetroidNode? = null
        private set
    var curRecord: TetroidRecord? = null
        private set
    var curAttach: TetroidFile? = null
        private set

    private val curRecords: MutableList<TetroidRecord> = mutableListOf()
    private val curAttaches: MutableList<TetroidFile> = mutableListOf()
    private var selectedTags: MutableList<TetroidTag> = mutableListOf()

    private var tempAttachToOpen: TetroidFile? = null
    var isDropRecordsFiltering = true
    var lastSearchProfile: SearchProfile? = null
        private set
    private var lastFilterQuery: String? = null
    private var isFromRecordActivity = false
    private var isStorageTreeChangingHandled = false
    var isMultiTagsMode = false
        private set

    init {
        // FIXME: koin: из-за циклической зависимости вместо инжекта storageDataProcessor в конструкторе,
        //  задаем его вручную позже
        storageProvider.init(storageDataProcessor)
        cryptManager.init(
            cryptRecordFilesIfNeedUseCase,
            parseRecordTagsUseCase,
        )

        setStorageTreeObserverCallbacks()
    }

    // region Migration

    // endregion Migration

    // region Pages

    fun openPage(page: PageType) {
        launchOnMain {
            sendEvent(MainEvent.OpenPage(page))
        }
    }

    fun showMainView(viewType: MainViewType) {
        // сохраняем значение для возврата на старое View
        // (только, если осуществляется переключение на действительно другую вьюшку)
        if (viewType != currentMainViewType) {
            this.lastMainViewType = currentMainViewType
        }
        launchOnMain {
            sendEvent(MainEvent.ShowMainView(viewType))
        }
        this.currentMainViewType = viewType
    }

    fun closeFoundFragment() {
        launchOnMain {
            sendEvent(MainEvent.CloseFoundView)
        }
    }

    /**
     * Восстанавливаем состояние Toolbar при переключении обратно к фрагменту MainPageFragment.
     */
    fun restoreLastToolbarState() {
        var title: String? = null
        val restoredView = currentMainViewType
        if (restoredView == MainViewType.RECORD_ATTACHES) {
            title = curRecord?.name.orEmpty()
        }
        updateToolbar(page = PageType.MAIN, viewType = restoredView, title)
    }

    fun updateToolbar(page: PageType, viewType: MainViewType, title: String?) {
        launchOnMain {
            sendEvent(MainEvent.UpdateToolbar(page, viewType, title))
        }
    }

    // endregion Pages

    // region Password

    /**
     * Асинхронная проверка - имеется ли сохраненный пароль, и его запрос при необходимости.
     * Используется:
     * * когда хранилище уже загружено (зашифровка/сброс шифровки ветки)
     * * либо когда загрузка хранилища не требуется (установка/сброс ПИН-кода)
     */
    fun checkStoragePassword(callbackEvent: BaseEvent) {
        launchOnMain {
            withIo {
                checkPasswordOrPinUseCase.run(
                    CheckPasswordOrPinAndAskUseCase.Params(
                        isStorageEncrypted = isStorageEncrypted(),
                    )
                )
            }.onFailure { failure ->
                logFailure(failure)
            }.onSuccess { result ->
                when (result) {
                    is CheckPasswordOrPinAndAskUseCase.Result.PasswordNotSet -> {
                        sendEvent(callbackEvent)
                    }
                    is CheckPasswordOrPinAndAskUseCase.Result.AskPassword -> {
                        askPassword(callbackEvent)
                    }
                    is CheckPasswordOrPinAndAskUseCase.Result.AskPin -> {
                        askPinCode(true, callbackEvent)
                    }
                    is CheckPasswordOrPinAndAskUseCase.Result.AskForEmptyPassCheckingField -> {
                        sendEvent(
                            StorageEvent.AskForEmptyPassCheckingField(
                                fieldName = result.fieldName,
                                passHash = result.passHash,
                                callbackEvent = callbackEvent,
                            )
                        )
                    }
                }
            }
        }
    }

    fun confirmEmptyPasswordCheckingFieldDialog(passHash: String, callbackEvent: BaseEvent) {
        cryptManager.setKeyFromMiddleHash(passHash)
        askPinCode(true, callbackEvent)
    }

    fun cancelEmptyPasswordCheckingFieldDialog(callbackEvent: BaseEvent) {
        when (callbackEvent) {
            is StorageEvent.LoadOrDecrypt -> {
                if (!callbackEvent.params.isNodeOpening) {
                    // загружаем хранилище без расшифровки
                    callbackEvent.params.isDecrypt = false
                    loadOrDecryptStorage(callbackEvent.params)
                }
            }
            else -> {}
        }
    }

    fun onPasswordEntered(password: String, isSetup: Boolean, callbackEvent: BaseEvent) {
        if (isSetup) {
            launchOnMain {
                setupPassword(password, callbackEvent)
            }
        } else {
            launchOnMain {
                withIo {
                    checkPasswordUseCase.run(
                        CheckPasswordUseCase.Params(password)
                    )
                }.onFailure {
                    logFailure(it)
                }.onSuccess { result ->
                    when (result) {
                        is CheckPasswordUseCase.Result.PasswordNotSet -> {
                            sendEvent(callbackEvent)
                        }
                        is CheckPasswordUseCase.Result.Success -> {
                            launchOnMain {
                                withIo {
                                    initPasswordUseCase.run(
                                        InitPasswordUseCase.Params(
                                            password = password,
                                        )
                                    )
                                }.onFailure {
                                    logFailure(it)
                                }.onSuccess {
                                    sendEvent(callbackEvent)
                                }
                            }
                        }
                        is CheckPasswordUseCase.Result.NotMatched -> {
                            logError(R.string.log_pass_is_incorrect, show = true)
                            askPassword(callbackEvent)
                        }
                        is CheckPasswordUseCase.Result.AskForEmptyPassCheckingField -> {
                            launchOnMain {
                                sendEvent(
                                    StorageEvent.AskForEmptyPassCheckingField(
                                        fieldName = result.fieldName,
                                        passHash = "",
                                        callbackEvent = callbackEvent,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun onPasswordCanceled(isSetup: Boolean, callbackEvent: BaseEvent) {
        if (!isSetup) {
            isAlreadyTryDecrypt = true
            //super.onPasswordCanceled(isSetup, callback)
            // FIXME: не уверен в решении ..
            when (callbackEvent) {
                is StorageEvent.LoadOrDecrypt -> {
                    if (!callbackEvent.params.isNodeOpening) {
                        //isAlreadyTryDecrypt = true
                        callbackEvent.params.isDecrypt = false
                        launchOnMain {
                            sendEvent(callbackEvent)
                        }
                    }
                }
            }
        }
    }

    fun dropAllPasswordData() {
        launchOnMain {
            withIo {
                dropAllPasswordDataUseCase.run(
                    DropAllPasswordDataUseCase.Params
                )
            }.onFailure {
                logFailure(it)
            }
        }
    }

    // endregion Password

    // region Records

    fun setCurrentRecords(records: List<TetroidRecord>) {
        curRecords.clear()
        curRecords.addAll(records)
    }

    fun showRecords(records: List<TetroidRecord>, page: MainViewType, dropSearch: Boolean = true) {
        launchOnMain {
            setCurrentRecords(records)
            sendEvent(MainEvent.ShowRecords(records, page, dropSearch))
        }
    }

    /**
     * Вставка записи в ветку.
     */
    fun insertRecord() {
        // на всякий случай проверяем тип
        if (!ClipboardManager.hasObject(FoundType.TYPE_RECORD))
            return
        // достаем объект из "буфера обмена"
        val clipboard = ClipboardManager.getInstance()
        // вставляем с попыткой восстановить каталог записи
        val record = clipboard.getObject() as TetroidRecord
        val isCutting = clipboard.isCutting
        insertRecord(record, isCutting, withoutDir = false)
    }

    private fun insertRecord(record: TetroidRecord, isCutting: Boolean, withoutDir: Boolean) {
        launchOnMain {
            sendEvent(MainEvent.Record.Insert.InProcess(record))

            withIo {
                insertRecordUseCase.run(
                    InsertRecordUseCase.Params(
                        record = record,
                        node = curNode!!,
                        isCutting = isCutting,
                        withoutDir = withoutDir,
                    )
                )
            }.onComplete {
                hideProgress()
            }.onFailure { failure ->
                sendEvent(MainEvent.Record.Insert.Failed(record, failure))
                when (failure) {
                    is Failure.File -> {
                        logFailure(failure)
                    }
                    is Failure.Folder -> {
                        sendEvent(MainEvent.AskForOperationWithoutFolder(ClipboardParams(LogOper.INSERT, record, isCutting)))
                    }
                    else -> {
                        logFailure(failure)
                    }
                }
            }.onSuccess { insertedRecord ->
                logOperRes(LogObj.RECORD, LogOper.INSERT)
                sendEvent(MainEvent.Record.Insert.Success(record))

                showNode(insertedRecord.node)
                updateNodes()
                reloadTags()
                if (isCutting) {
                    // очищаем "буфер обмена"
                    ClipboardManager.clear()
                    updateFavoritesNodeTitleAndListIfNeed(insertedRecord)
                }
            }
        }
    }

    /**
     * Создание новой записи.
     */
    fun createRecord(
        name: String,
        tags: String,
        author: String,
        url: String,
        node: TetroidNode,
        isFavorite: Boolean,
    ) {
        launchOnMain {
            sendEvent(MainEvent.Record.Create.InProcess(name))

            withIo {
                createRecordUseCase.run(
                    CreateRecordUseCase.Params(
                        name = name,
                        tagsString = tags,
                        author = author,
                        url = url,
                        node = node,
                        isFavor = isFavorite,
                    )
                )
            }.onComplete {
                hideProgress()
            }.onFailure { failure ->
                logFailure(failure)
                sendEvent(MainEvent.Record.Create.Failed(name, failure))
            }.onSuccess { record ->
                logOperRes(LogObj.RECORD, LogOper.CREATE, record, show = false)
                sendEvent(MainEvent.Record.Create.Success(record))
                reloadTags()
                updateNodes()
                updateFavoritesNodeTitleAndListIfNeed(record)
                showNode(record.node)
                openRecord(record)
            }
        }
    }

    /**
     * Создание новой записи для вставки объекта из другого приложения.
     */
    fun createRecordFromIntent(
        intent: Intent,
        isText: Boolean,
        text: String,
        imagesUri: ArrayList<Uri>,
        receivedData: ReceivedData
    ) {
        launchOnMain {
            // имя записи
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            var url: String? = null
            if (Build.VERSION.SDK_INT >= 17) {
                url = intent.getStringExtra(Intent.EXTRA_ORIGINATING_URI)
            }
            // создаем запись
            withIo {
                createTempRecordUseCase.run(
                    CreateTempRecordUseCase.Params(
                        srcName = subject,
                        url = url,
                        text = text,
                        node = getQuicklyOrRootNode(),
                    )
                )
            }.onFailure {
                logFailure(it)
            }.onSuccess { record ->
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
                        for (uri in imagesUri) {
                            createAttachToRecordUseCase.run(
                                AttachFileToRecordUseCase.Params(
                                    fileUri = uri,
                                    record = record,
                                )
                            ).onFailure {
                                hasError = true
                            }
                        }
                        if (hasError) {
                            logWarning(R.string.log_files_attach_error, true)
                            sendEvent(BaseEvent.ShowMoreInLogs)
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
    }

    /**
     * Удаление записи.
     */
    fun deleteRecord(recordId: String) {
        launchOnMain {
            withIo {
                getRecordByIdUseCase.run(recordId)
            }.onFailure {
                logFailure(it)
            }.onSuccess {
                deleteRecord(it)
            }
        }
    }

    fun deleteRecord(record: TetroidRecord) {
        cutOrDeleteRecord(record, isCutting = false, withoutDir = false)
    }

    fun editRecordFields(
        record: TetroidRecord,
        name: String,
        tags: String,
        author: String,
        url: String,
        node: TetroidNode,
        isFavor: Boolean,
    ) {
        val oldNode = record.node
        launchOnMain {
            withIo {
                editRecordFieldsUseCase.run(
                    EditRecordFieldsUseCase.Params(
                        record = record,
                        name = name,
                        tagsString = tags,
                        author = author,
                        url = url,
                        node = node,
                        isFavor = isFavor,
                    )
                )
            }.onFailure {
                logFailure(it)
            }.onSuccess {
                onRecordFieldsUpdated(record, oldNode !== record.node)
                log(R.string.log_record_fields_changed, true)
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
                updateRecordsList()
            }
            reloadTags()
            updateFavoritesNodeTitle()
        }
    }

    /**
     * Копирование записи.
     * @param record
     */
    fun copyRecord(record: TetroidRecord) {
        // добавляем в "буфер обмена"
        ClipboardManager.copy(record)
        logOperRes(LogObj.RECORD, LogOper.COPY)
    }

    /**
     * Перемещение записи вверх/вниз по списку.
     * @param records
     * @param pos
     * @param isUp
     */
    fun reorderRecords(pos: Int, isUp: Boolean) {
        if (currentMainViewType == MainViewType.FAVORITES) {
            reorderFavorites(pos, isUp)
        } else {
            reorderNodeRecords(pos, isUp)
        }
    }

    private fun reorderFavorites(pos: Int, isUp: Boolean) {
        launchOnMain {
            withIo {
                favoritesManager.swap(pos, isUp, through = true)
            }.onFailure {
                logFailure(it)
                logOperErrorMore(LogObj.RECORD, LogOper.REORDER)
            }.onSuccess { result ->
                if (result) {
                    logOperRes(LogObj.RECORD, LogOper.REORDER)
                    setCurrentRecords(getFavoriteRecords())
                    updateRecordsList()
                }
            }
        }
    }

    private fun reorderNodeRecords(pos: Int, isUp: Boolean) {
        curNode?.records?.let { records ->
            launchOnMain {
                withIo {
                    swapObjectsInStorageTree(records, pos, isUp, through = true)
                }.onFailure {
                    logFailure(it)
                    logOperErrorMore(LogObj.RECORD, LogOper.REORDER)
                }.onSuccess { result ->
                    if (result) {
                        logOperRes(LogObj.RECORD, LogOper.REORDER)
                        setCurrentRecords(records)
                        updateRecordsList()
                    }
                }
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
        // добавляем в "буфер обмена"
        ClipboardManager.cut(record)
        // удаляем запись из текущей ветки и каталог перемещаем в корзину
        cutOrDeleteRecord(record, isCutting = true, withoutDir = false)
    }

    private fun cutOrDeleteRecord(record: TetroidRecord, isCutting: Boolean, withoutDir: Boolean) {
        launchOnMain {
            sendEvent(if (isCutting) MainEvent.Record.Cut.InProcess(record) else MainEvent.Record.Delete.InProcess(record))

            withIo {
                cutOrDeleteRecordUseCase.run(
                    CutOrDeleteRecordUseCase.Params(
                        record = record,
                        withoutDir = withoutDir,
                        isCutting = isCutting,
                    )
                )
            }.onFailure { failure ->
                sendEvent(if (isCutting) MainEvent.Record.Cut.Failed(record, failure) else MainEvent.Record.Delete.Failed(record, failure))
                when (failure) {
                    is Failure.File -> {
                        logFailure(failure)
                        sendEvent(BaseEvent.ShowMoreInLogs)
                    }
                    is Failure.Folder -> {
                        if (isCutting) {
                            sendEvent(MainEvent.AskForOperationWithoutFolder(ClipboardParams(LogOper.CUT, record)))
                        } else {
                            sendEvent(MainEvent.AskForOperationWithoutFolder(ClipboardParams(LogOper.DELETE, record)))
                        }
                    }
                    else -> {
                        logFailure(failure)
                    }
                }
            }.onSuccess {
                curRecords.remove(record)
                sendEvent(if (isCutting) MainEvent.Record.Cut.Success(record) else MainEvent.Record.Delete.Success(record))
                reloadTags()
                updateNodes()
                updateFavoritesNodeTitleAndListIfNeed(record)
                updateRecordsList()
                curRecord = null
                logOperRes(LogObj.RECORD, if (isCutting) LogOper.CUT else LogOper.DELETE)
                // переходим в список записей ветки после удаления
                // (запись может быть удалена при попытке просмотра/изменения файла, например)
                if (currentMainViewType !in arrayOf(MainViewType.NODE_RECORDS, MainViewType.FAVORITES)) {
                    showMainView(MainViewType.NODE_RECORDS)
                }
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
                    insertRecord(record, isCutting = params.isCutting, withoutDir = true)
                }
                LogOper.DELETE -> {
                    if (params.obj is TetroidRecord) {
                        val record = params.obj
                        cutOrDeleteRecord(record, isCutting = false, withoutDir = true)
                    } else if (params.obj is TetroidFile) {
                        val record = params.obj.record
                        cutOrDeleteRecord(record, isCutting = false, withoutDir = true)
                    }
                }
                LogOper.CUT -> {
                    val record = params.obj as TetroidRecord
                    cutOrDeleteRecord(record, isCutting = true, withoutDir = true)
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
    fun openRecord(record: TetroidRecord) {
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
            openRecord(recordId, bundle)
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
        openRecord(recordId, bundle)
    }

    /**
     *
     * @param recordId
     */
    private fun openRecordWithAttachedFiles(recordId: String) {
        val bundle = Bundle()
        bundle.putString(Constants.EXTRA_OBJECT_ID, recordId)
        bundle.putString(Constants.EXTRA_ATTACHED_FILES, "")
        openRecord(recordId, bundle)
    }

    /**
     * Открытие активности RecordActivity.
     * @param bundle
     */
    private fun openRecord(recordId: String, bundle: Bundle) {
        bundle.putInt(Constants.EXTRA_STORAGE_ID, storage?.id.orZero())
        launchOnMain {
            sendEvent(MainEvent.Record.Open(recordId, bundle))
        }
    }

    /**
     * Открытие каталога записи.
     */
    fun openCurrentRecordFolder(activity: Activity) {
        curRecord?.also {
            openRecordFolder(
                activity = activity,
                record = it,
            )
        }
    }

    fun openRecordFolder(activity: Activity, record: TetroidRecord) {
        logger.logDebug(resourcesProvider.getString(R.string.log_start_record_folder_opening) + record.id)
        launchOnMain {
            withIo {
                getRecordFolderUseCase.run(
                    GetRecordFolderUseCase.Params(
                        record = record,
                        createIfNeed = false,
                        inTrash = record.isTemp,
                        showMessage = true,
                    )
                )
            }.onFailure {
                logFailure(it)
            }.onSuccess { recordFolder ->
                val uri = recordFolder.uri
                if (!interactionManager.openFolder(activity, uri)) {
                    Utils.writeToClipboard(getContext(), resourcesProvider.getString(R.string.title_record_folder_uri), uri.toString())
                    logWarning(R.string.log_missing_file_manager, show = true)
                }
            }
        }
    }

    fun updateRecordsList() {
        launchOnMain {
            sendEvent(
                MainEvent.UpdateRecordsList(
                    records = curRecords,
                    currentViewType = currentMainViewType,
                )
            )
        }
    }

    // endregion Records

    // region Nodes

    private fun setCurrentNode(node: TetroidNode?) {
        curNode = node
        launchOnMain {
            sendEvent(MainEvent.SetCurrentNode(node))
        }
    }

    fun getCurNodeName() = curNode?.name.orEmpty()

    fun showNode(nodeId: String) {
        launchOnMain {
            withIo {
                getNodeByIdUseCase.run(
                    GetNodeByIdUseCase.Params(nodeId)
                )
            }.onFailure {
                logFailure(it)
            }.onSuccess { node ->
                showNode(node)
            }
        }
    }

    /**
     * Открытие записей ветки.
     * @param node
     */
    fun showNode(node: TetroidNode) {
        // проверка нужно ли расшифровать ветку перед отображением
        if (checkAndDecryptNode(node)) return

        launchOnMain {
            log(getString(R.string.log_open_node) + node.getIdString(resourcesProvider))
            setCurrentNode(node)
            showRecords(node.records, MainViewType.NODE_RECORDS)

            // сохраняем выбранную ветку
            saveLastSelectedNode(nodeId = node.id)
        }
    }

    /**
     * Открытие ветки записи.
     * Если активен режим "Только избранное", то открытие списка избранных записей.
     */
    fun showRecordNode() {
        when {
            isLoadedFavoritesOnly() -> showFavorites()
            curRecord != null -> showNode(curRecord!!.node)
            curNode != null -> showNode(curNode!!)
        }
    }

    /**
     * Сохранение последней выбранной ветки.
     */
    private fun saveLastSelectedNode(nodeId: String) {
        if (isKeepLastNode()) {
            launchOnIo {
                setLastNodeIdAndSaveStorageInDb(nodeId)
            }
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
            withIo {
                editNodeFieldsUseCase.run(
                    EditNodeFieldsUseCase.Params(
                        node = node,
                        name = newName,
                    )
                )
            }.onFailure {
                logFailure(it)
//                logOperErrorMore(LogObj.NODE, LogOper.RENAME)
            }.onSuccess {

                logOperRes(LogObj.NODE, LogOper.RENAME)
                updateNodes()
                sendEvent(MainEvent.Node.Renamed(node))
            }
        }
    }

    fun setNodeIcon(nodeId: String, iconPath: String?, isDrop: Boolean) {
        launchOnMain {
            withIo {
                val node = if (curNode?.id == nodeId) {
                    curNode
                } else {
                    getNode(nodeId)
                }
                node?.let {
                    setNodeIconUseCase.run(
                        SetNodeIconUseCase.Params(
                            node = node,
                            iconFileName = if (!isDrop) iconPath else null,
                        )
                    )
                }
            }?.onFailure {
                //logOperErrorMore(LogObj.NODE, LogOper.CHANGE)
                logFailure(it)
            }?.onSuccess {
                logOperRes(LogObj.NODE, LogOper.CHANGE)
                updateNodes()
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
        if (!ClipboardManager.hasObject(FoundType.TYPE_NODE)) return
        // достаем объект из "буфера обмена"
        val clipboard = ClipboardManager.getInstance()
        // вставляем с попыткой восстановить каталог записи
        val node = clipboard.getObject() as TetroidNode
        val isCutting = clipboard.isCutting
        val trueParentNode = if (isSubNode) parentNode else parentNode.parentNode

        launchOnMain {
            sendEvent(MainEvent.Node.Insert.InProcess(node))

            withIo {
                insertNodeUseCase.run(
                    InsertNodeUseCase.Params(
                        srcNode = node,
                        parentNode = trueParentNode,
                        isCutting = isCutting,
                    )
                )
            }.onFailure { failure ->
                logFailure(failure, show = false)
                sendEvent(MainEvent.Node.Insert.Failed(node, failure))
                logOperErrorMore(LogObj.NODE, LogOper.INSERT)
            }.onSuccess { newNode ->
                // ищем вновь созданную ветку - копию node
                //  (даже при вставке ВЫРЕЗАННОЙ ветки вставляется ее копия, а не оригинальная из буфера обмена)
                sendEvent(MainEvent.Node.Insert.Success(newNode))
            }
        }
    }

    /**
     * Вырезание ветки из родительской ветки.
     * @param node
     */
    fun cutNode(node: TetroidNode, pos: Int) {
        // нельзя вырезать нерасшифрованную ветку
        if (!node.isNonCryptedOrDecrypted) {
            log(R.string.log_cannot_delete_undecrypted_node, true)
            return
        }
        // нельзя вырезать ветку, у которой есть дочерние нерасшифрованные ветки
        if (hasNonDecryptedNodes(node)) {
            log(getString(R.string.log_enter_pass_first), true)
            return
        }
        // добавляем в "буфер обмена"
        ClipboardManager.cut(node)
        // удаляем ветку из родительской ветки вместе с записями
        deleteOrCutNode(node, isCutting = true)
    }

    /**
     * Удаление ветки.
     * @param node
     */
    fun startDeleteNode(node: TetroidNode) {
        // нельзя удалить нерасшифрованную ветку
        when {
            !node.isNonCryptedOrDecrypted -> {
                log(R.string.log_cannot_delete_undecrypted_node, true)
            }
            // нельзя удалить последнюю ветку в корне
            node.level == 0 && getRootNodes().size == 1 -> {
                log(R.string.log_cannot_delete_root_node, true)
            }
            else -> {
                launchOnMain {
                    sendEvent(MainEvent.Node.AskForDelete(node))
                }
            }
        }
    }

    fun deleteOrCutNode(node: TetroidNode, isCutting: Boolean) {
        launchOnMain {
            sendEvent(if (isCutting) MainEvent.Node.Cut.InProcess(node) else MainEvent.Node.Delete.InProcess(node))

            withIo {
                cutOrDeleteNodeUseCase.run(
                    CutOrDeleteNodeUseCase.Params(
                        node = node,
                        isCutting = isCutting,
                    )
                )
            }.onFailure { failure ->
                logFailure(failure)
                sendEvent(if (isCutting) MainEvent.Node.Cut.Failed(node, failure) else MainEvent.Node.Delete.Failed(node, failure))
            }.onSuccess {
                sendEvent(if (isCutting) MainEvent.Node.Cut.Success(node) else MainEvent.Node.Delete.Success(node))
                // обновляем label с количеством избранных записей
                if (buildInfoProvider.isFullVersion()) {
                    updateFavoritesNodeTitle()
                }
                // убираем список записей удаляемой ветки
                if (curNode == node || isNodeInNode(curNode, node)) {
                    curRecord = null
                    curNode = null
                    launchOnMain {
                        sendEvent(MainEvent.ClearMainView)
                    }
                }
                if (node.isCrypted) {
                    // проверяем существование зашифрованных веток
                    checkExistenceCryptedNodes()
                }
            }
        }
    }

    /**
     * Зашифровка ветки.
     * @param node
     */
    fun startEncryptNode(node: TetroidNode) {
        val callbackEvent = MainEvent.Node.Encrypt(node)

        if (node.id == getQuicklyNodeId()) {
            showMessage(R.string.mes_quickly_node_cannot_encrypt)
        } else if (!isStorageEncrypted()) {
            launchOnMain {
                sendEvent(StorageEvent.AskPassword(callbackEvent))
            }
        } else {
            checkStoragePassword(callbackEvent)
        }
    }

    /**
     * Сброс шифрования ветки.
     * @param node
     */
    fun startDropEncryptNode(node: TetroidNode) {
        val callbackEvent = MainEvent.Node.DropEncrypt(node)
        checkStoragePassword(callbackEvent)
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
            sendEvent(
                BaseEvent.TaskStarted(
                    if (isEncrypt) R.string.task_node_encrypting else R.string.task_node_drop_crypting
                )
            )

            logOperStart(LogObj.NODE, if (isEncrypt) LogOper.ENCRYPT else LogOper.DROPCRYPT, node)

            val nodeWasEncrypted = node.isCrypted
            val operation = if (isEncrypt) LogOper.ENCRYPT else LogOper.DROPCRYPT

            val result = withIo {
                // сначала расшифровываем хранилище
                if (isStorageEncrypted() && !isStorageDecrypted()) {
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

                    val result = if (isEncrypt) cryptManager.encryptNode(node, isReencrypt = false)
                    else dropCryptNode(node)

                    if (result && saveStorage()) 1 else -1
                } else 0
            }

            sendEvent(BaseEvent.TaskFinished)

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

    /**
     * Расшифровка ветки с подветками (постоянная).
     */
    private suspend fun dropCryptNode(node: TetroidNode): Boolean {
        return cryptManager.decryptNode(
            node = node,
            isDecryptSubNodes = true,
            isDecryptRecords = true,
            loadIconCallback = {
                loadNodeIconUseCase.execute(
                    LoadNodeIconUseCase.Params(node)
                ).onFailure {
                    logger.logFailure(it, show = false)
                }
            },
            isDropCrypt = true,
            isDecryptFiles = false
        )
    }

    override fun afterStorageDecrypted(node: TetroidNode?) {
        launchOnMain {
            sendEvent(StorageEvent.Decrypted)
            if (node != null) {
                if (node === FavoritesManager.FAVORITES_NODE) {
                    showFavorites()
                } else {
                    showNode(node)
                }
            }
            sendEvent(MainEvent.HandleReceivedIntent)
        }
    }

    private fun setStage(obj: LogObj, oper: LogOper, stage: Stages) {
        val taskStage = TaskStage(Constants.TetroidView.Main, obj, oper, stage)
        launchOnMain {
            showProgressWithText(message = logger.logTaskStage(taskStage).orEmpty())
        }
    }

    /**
     * Перемещение ветки вверх/вниз по списку.
     */
    fun reorderNode(
        node: TetroidNode,
        position: Int,
        isUp: Boolean,
    ) {
        launchOnMain {
            val subNodes = node.parentNode.subNodes ?: getRootNodes()

            if (subNodes.isNotEmpty()) {
                val positionInNode = subNodes.indexOf(node)
                withIo {
                    swapObjectsInStorageTree(
                        list = subNodes,
                        pos = positionInNode,
                        isUp = isUp,
                        through = true
                    )
                }.onFailure {
                    logFailure(it)
                    logOperErrorMore(LogObj.NODE, LogOper.REORDER)
                }.onSuccess { result ->
                    if (result) {
                        // меняем местами элементы внутри списка
                        val newPositionInNode = when {
                            isUp -> {
                                if (positionInNode == 0) subNodes.size - 1 else positionInNode - 1
                            }
                            positionInNode == subNodes.size - 1 -> {
                                0
                            }
                            else -> {
                                positionInNode + 1
                            }
                        }
                        sendEvent(MainEvent.Node.Reordered(
                            node = node,
                            flatPosition = position,
                            positionInNode = positionInNode,
                            newPositionInNode = newPositionInNode
                        ))
                    }
                }
            }
        }
    }

    fun updateNodes() {
        launchOnMain {
            sendEvent(MainEvent.UpdateNodes)
        }
    }

    fun hasNonDecryptedNodes(node: TetroidNode): Boolean {
        if (!node.isNonCryptedOrDecrypted) return true
        if (node.subNodesCount > 0) {
            for (subnode in node.subNodes) {
                if (hasNonDecryptedNodes(subnode)) return true
            }
        }
        return false
    }

    fun isNodeInNode(node: TetroidNode?, nodeAsParent: TetroidNode?): Boolean {
        if (node == null || nodeAsParent == null) return false
        return if (node.parentNode != null) {
            if (node.parentNode == nodeAsParent) true else isNodeInNode(node.parentNode, nodeAsParent)
        } else false
    }

    // endregion Nodes

    // region Tags

    fun isTagSelected(tag: TetroidTag): Boolean {
        return selectedTags.any { it.name == tag.name }
    }

    fun onTagClick(tag: TetroidTag) {
        if (isMultiTagsMode) {
            if (isTagSelected(tag)) {
                unselectTag(tag)
            } else {
                selectTag(tag)
            }
        } else {
            showTagRecords(tag)
        }
    }

    fun selectTag(tag: TetroidTag) {
        launchOnMain {
            isMultiTagsMode = true
            if (!selectedTags.contains(tag)) {
                selectedTags.add(tag)
            }
            sendEvent(MainEvent.Tags.UpdateSelectedTags(selectedTags, isMultiTagsMode))
        }
    }

    fun unselectTag(tag: TetroidTag) {
        launchOnMain {
            if (selectedTags.contains(tag)) {
                selectedTags.remove(tag)
                isMultiTagsMode = selectedTags.isNotEmpty()
                sendEvent(MainEvent.Tags.UpdateSelectedTags(selectedTags, isMultiTagsMode))
            }
        }
    }

    fun unselectAllTags() {
        launchOnMain {
            selectedTags.clear()
            isMultiTagsMode = false
            sendEvent(MainEvent.Tags.UpdateSelectedTags(selectedTags, isMultiTagsMode))
        }
    }

    fun getSelectedTagsNames(): String {
        return selectedTags.joinToString(separator = ", ") {
            if (it.isEmpty) {
                resourcesProvider.getString(R.string.title_empty_tag)
            } else {
                it.name
            }
        }
    }

    /**
     * Отображение записей по имени метки.
     */
    fun showTagRecords(tagName: String) {
        launchOnMain {
            withIo {
                getTagByNameUseCase.run(tagName)
            }.onFailure {
                logFailure(it)
            }.onSuccess { tag ->
                showTagRecords(tag)
            }
        }
    }

    /**
     * Отображение записей по метке.
     */
    fun showTagRecords(tag: TetroidTag) {
        launchOnMain {
            isMultiTagsMode = false
            selectedTags.clear()
            selectedTags.add(tag)
            sendEvent(MainEvent.Tags.UpdateSelectedTags(selectedTags, isMultiTagsMode))
            // сбрасываем текущую ветку
            setCurrentNode(node = null)
            log(getString(R.string.log_open_tag_records_mask, tag.name))
            showRecords(tag.records, MainViewType.TAG_RECORDS)
        }
    }

    /**
     * Отображение записей по меткам.
     */
    fun showTagsRecords(tags: List<TetroidTag>) {
        launchOnMain {
            selectedTags.clear()
            selectedTags.addAll(tags)
            sendEvent(MainEvent.Tags.UpdateSelectedTags(selectedTags, isMultiTagsMode))
            showTagsRecordsFromSelected()
        }
    }

    fun showTagsRecordsFromSelected() {
        launchOnMain {
            // сбрасываем текущую ветку
            setCurrentNode(node = null)
            log(getString(R.string.log_open_tag_records_mask, getSelectedTagsNames()))
            val tagsRecords = getTagsRecords(selectedTags)
            showRecords(tagsRecords, MainViewType.TAG_RECORDS)
        }
    }

    private fun getTagsRecords(tags: List<TetroidTag>): List<TetroidRecord> {
        val allTagsRecords = tags.map { it.records }.flatten().distinct()

        return when (settingsManager.getTagsSearchMode()) {
            TagsSearchMode.OR -> {
                allTagsRecords
            }
            TagsSearchMode.AND -> {
                allTagsRecords.filter { record ->
                    tags.all { tag ->
                        record.tags.any { it.name == tag.name }
                    }
                }
            }
        }
    }

    fun renameTag(tag: TetroidTag, name: String) {
        if (tag.name == name) {
            return
        }
        launchOnMain {
            withIo {
                renameTagInRecordsUseCase.run(
                    RenameTagInRecordsUseCase.Params(
                        tag = tag,
                        newName = name,
                    )
                )
            }.onFailure {
                logFailure(it)
                //logOperErrorMore(LogObj.TAG, LogOper.RENAME)
            }.onSuccess {
                logOperRes(LogObj.TAG, LogOper.RENAME)
                reloadTags()
                updateRecordsList()
            }
        }
    }

    fun updateTags() {
        launchOnMain {
            sendEvent(MainEvent.Tags.UpdateTags)
        }
    }

    fun reloadTags() {
        launchOnMain {
            sendEvent(MainEvent.Tags.ReloadTags(tagsMap = getTagsMap()))
        }
    }

    // endregion Tags

    // region Attaches

    fun setCurrentAttaches(attaches: List<TetroidFile>) {
        curAttaches.clear()
        curAttaches.addAll(attaches)
    }

    fun checkPermissionIfNeedAndOpenAttach(activity: Activity, attach: TetroidFile) {
        if (!attach.isCrypted) {
            openAttach(activity, attach)
        } else {
            if (storageSettingsProvider.isDecryptAttachesToTempFolder()) {
                // будет запрос разрешения на запись расшифрованного файла в память
                tempAttachToOpen = attach

                val attachFile = storageFolder?.child(
                    context = getContext(),
                    path = recordPathProvider.getRelativePathToRecordAttach(attach),
                    requiresWriteAccess = true,
                )
                attachFile?.also {
                    checkAndRequestWriteFileStoragePermission(
                        uri = it.uri,
                        requestCode = PermissionRequestCode.OPEN_ATTACH_FILE,
                    )
                }
            } else {
                launchOnMain {
                    sendEvent(MainEvent.Attach.Open.RequestToEnableDecryptAttachesToTempFolder(attach))
                }
            }
        }
    }

    fun openTempAttachAfterCheckPermission(activity: Activity) {
        tempAttachToOpen?.let {
            openAttach(activity, it)
        }
    }

    private fun openAttach(activity: Activity, attach: TetroidFile) {
        launchOnMain {
            sendEvent(MainEvent.Attach.Open.InProcess(attach))

            withIo {
                getUriFromAttachUseCase.run(
                    PrepareAttachForOpenUseCase.Params(
                        attach = attach,
                    )
                )
            }.onFailure { failure ->
                logFailure(failure)
                sendEvent(MainEvent.Attach.Open.Failed(attach, failure))
            }.onSuccess { fileUri ->
                sendEvent(MainEvent.Attach.Open.Success(attach))
                interactionManager.openFile(activity, fileUri)
            }
        }
    }

    fun enableDecryptAttachesToTempFolderAndOpen(activity: Activity, attach: TetroidFile) {
        launchOnIo {
            setIsDecryptToTempAndSaveStorageInDb(value = true)
            checkPermissionIfNeedAndOpenAttach(activity, attach)
        }
    }

    fun showAttaches(attaches: List<TetroidFile>) {
        launchOnMain {
            setCurrentAttaches(attaches)
            sendEvent(MainEvent.ShowAttaches(attaches))
        }
    }

    /**
     * Отображение списка прикрепленных файлов.
     * @param record Запись
     */
    fun showRecordAttaches(recordId: String, fromRecordActivity: Boolean = false) {
        launchOnMain {
            withIo {
                getRecordByIdUseCase.run(recordId)
            }.onFailure {
                logFailure(it)
            }.onSuccess {
                showRecordAttaches(it, fromRecordActivity)
            }
        }
    }

    fun showRecordAttaches(record: TetroidRecord, fromRecordActivity: Boolean = false) {
        launchOnMain {
            curNode = record.node
            curRecord = record
            isFromRecordActivity = fromRecordActivity
            showAttaches(record.attachedFiles)
            openPage(PageType.MAIN)
        }
    }

    private fun attachFile(uri: Uri, record: TetroidRecord, deleteSrcFile: Boolean) {
        launchOnMain {
            sendEvent(BaseEvent.TaskStarted(R.string.task_attach_file))
            withIo {
                createAttachToRecordUseCase.run(
                    AttachFileToRecordUseCase.Params(
                        fileUri = uri,
                        record = record,
                        deleteSrcFile = deleteSrcFile,
                    )
                )
            }.onComplete {
                sendEvent(BaseEvent.TaskFinished/*, Gravity.NO_GRAVITY*/)
            }.onFailure {
                logFailure(it)
            }.onSuccess {
                log(R.string.log_file_was_attached, true)
                updateAttaches()
                // обновляем список записей для обновления иконки о наличии прикрепляемых файлов у записи,
                // если был прикреплен первый файл
                if (record.attachedFilesCount == 1) {
                    updateRecordsList()
                }
            }
        }
    }

    /**
     * Прикрепление нового файла к текущей записи.
     */
    fun attachFileToCurrentRecord(fileUri: Uri, deleteSrcFile: Boolean) {
        curRecord?.let { record ->
            attachFile(fileUri, record, deleteSrcFile)
        }
    }

    /**
     * Выбор файла в файловой системе устройства и прикрепление к текущей записи.
     */
    fun pickAndAttachFile() {
        launchOnMain {
            sendEvent(MainEvent.Attach.OpenPicker)
        }
    }

    /**
     * Ввод URL для загрузки и прикрепления файла к текущей записи.
     */
    fun downloadAndAttachFile(url: String) {
        launchOnMain {
            downloadFileToCache(
                url = url,
                callback = object : TetroidActivity.IDownloadFileResult {
                    override fun onSuccess(uri: Uri) {
                        // прикрепляем и удаляем файл из кэша
                        attachFileToCurrentRecord(uri, deleteSrcFile = true)
                    }
                    override fun onError(ex: Exception) {
                        logError(ex, show = true)
                    }
                }
            )
        }
    }

    /**
     * Удаление прикрепленного файла.
     */
    fun deleteAttach(attach: TetroidFile, withoutFile: Boolean = false) {
        launchOnMain {
            sendEvent(MainEvent.Attach.Delete.InProcess(attach))

            withIo {
                deleteAttachUseCase.run(
                    DeleteAttachUseCase.Params(
                        attach = attach,
                        withoutFile = withoutFile,
                    )
                )
            }.onFailure { failure ->
                sendEvent(MainEvent.Attach.Delete.Failed(attach, failure))
                when (failure) {
                    is Failure.File.NotExist -> {
                        sendEvent(MainEvent.AskForOperationWithoutFile(ClipboardParams(LogOper.DELETE, attach)))
                    }
                    is Failure.Folder.NotExist -> {
                        sendEvent(MainEvent.AskForOperationWithoutFolder(ClipboardParams(LogOper.DELETE, attach)))
                    }
                    else -> {
                        logFailure(failure)
                    }
                }
            }.onSuccess {
                curAttaches.remove(attach)
                launchOnMain {
                    sendEvent(MainEvent.Attach.Delete.Success(attach))
                }
                // обновляем список записей для удаления иконки о наличии прикрепляемых файлов у записи,
                // если был удален единственный файл
                if ((curRecord?.attachedFilesCount ?: 0) <= 0) {
                    updateRecordsList()
                }
                logOperRes(LogObj.FILE, LogOper.DELETE)
            }
        }
    }

    /**
     * Переименование прикрепленного файла.
     */
    fun renameAttach(attach: TetroidFile, name: String) {
        launchOnMain {
            withIo {
                editAttachFieldsUseCase.run(
                    EditAttachFieldsUseCase.Params(
                        attach = attach,
                        name = name,
                    )
                )
            }.onFailure {
                when (it) {
                    is Failure.File.NotExist -> {
                        sendEvent(MainEvent.AskForOperationWithoutFile(ClipboardParams(LogOper.RENAME, attach)))
                    }
                    is Failure.Folder.NotExist -> {
                        // TODO: добавить вариант Создать каталог записи
                        sendEvent(MainEvent.AskForOperationWithoutFolder(ClipboardParams(LogOper.RENAME, attach)))
                    }
                    else -> {
                        logFailure(it)
                    }
                }
            }.onSuccess {
                logOperRes(LogObj.FILE, LogOper.RENAME)
                updateAttaches()
            }
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
                        deleteAttach(
                            attach = params.obj,
                            withoutFile = true,
                        )
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * Перемещение файла вверх/вниз по списку.
     */
    fun reorderAttaches(pos: Int, isUp: Boolean) {
        launchOnMain {
            curRecord?.attachedFiles?.let { attaches ->
                withIo {
                    swapObjectsInStorageTree(attaches, pos, isUp, through = true)
                }.onFailure {
                    logFailure(it)
                    logOperErrorMore(LogObj.FILE, LogOper.REORDER)
                }.onSuccess {
                    logOperRes(LogObj.FILE, LogOper.REORDER)
                    setCurrentAttaches(attaches)
                    reloadAttaches()
                }
            }
        }
    }

    fun pickFolderAndSaveAttachToFile(attach: TetroidFile) {
        curAttach = attach
        launchOnMain {
            sendEvent(MainEvent.PickFolderForAttach(attach))
        }
    }

    /**
     * Сохранение файла по выбранному пути.
     */
    fun saveAttachToFolder(folder: DocumentFile) {
        curAttach?.let {
            launchOnMain {
                sendEvent(BaseEvent.TaskStarted(R.string.task_file_saving))
                withIo {
                    saveAttachToFileUseCase.run(
                        SaveAttachToFileUseCase.Params(
                            attach = it,
                            destFolder = folder,
                        )
                    )
                }.onComplete {
                    sendEvent(BaseEvent.TaskFinished/*, Gravity.NO_GRAVITY*/)
                }.onFailure {
                    logFailure(it)
                }.onSuccess {
                    logOperRes(LogObj.FILE, LogOper.SAVE, "", true)
                }
            }
        }
    }

    fun moveBackFromAttaches() {
        if (isFromRecordActivity) {
            curRecord?.let { record ->
                openRecord(record)
                isFromRecordActivity = false
            }
        } else {
            showRecordNode()
        }
    }

    private fun updateAttaches() {
        launchOnMain {
            sendEvent(MainEvent.UpdateAttaches)
        }
    }

    private fun reloadAttaches() {
        launchOnMain {
            sendEvent(MainEvent.ReloadAttaches(curAttaches))
        }
    }

    // endregion Attaches

    // region Favorites

    /**
     * Отображение списка избранных записей.
     */
    fun showFavorites() {
        launchOnMain {
            val node = FavoritesManager.FAVORITES_NODE
            // выделяем ветку Избранное, только если загружено не одно Избранное
            if (!isLoadedFavoritesOnly()) {
                setCurrentNode(node)
            }
            showRecords(getFavoriteRecords(), MainViewType.FAVORITES, dropSearch = true)

            // сохраняем выбранную ветку
            saveLastSelectedNode(nodeId = node.id)
        }
    }

    fun addToFavorite(record: TetroidRecord) {
        launchOnMain {
            if (favoritesManager.add(record)) {
                val mes = getString(R.string.log_added_to_favor)
                showMessage(mes)
                log(mes + ": " + record.getIdString(resourcesProvider), false)
                updateFavoritesNodeTitle()
                updateRecordsList()
            } else {
                logOperError(LogObj.RECORD, LogOper.ADD, getString(R.string.log_with_id_to_favor_mask, record.id), true, true)
            }
        }
    }

    fun removeFromFavorite(record: TetroidRecord) {
        launchOnMain {
            if (favoritesManager.remove(record, resetFlag = true)) {
                val mes = getString(R.string.log_deleted_from_favor)
                showMessage(mes)
                log(mes + ": " + record.getIdString(resourcesProvider), false)
                if (!record.isFavorite && currentMainViewType == MainViewType.FAVORITES) {
                    curRecords.remove(record)
                }
                updateFavoritesNodeTitle()
                updateRecordsList()
            } else {
                logOperError(LogObj.RECORD, LogOper.DELETE, getString(R.string.log_with_id_from_favor_mask, record.id), true, true)
            }
        }
    }

    fun updateFavoritesNodeTitleAndListIfNeed(record: TetroidRecord?) {
        if (record?.isFavorite == true) {
            updateFavoritesNodeTitle()
        }
    }

    private fun updateFavoritesNodeTitle() {
        launchOnMain {
            sendEvent(MainEvent.UpdateFavoritesNodeTitle)
        }
    }

    // endregion Favorites

    // region Global search

    /**
     * Открытие объекта из поисковой выдачи в зависимости от его типа.
     * @param found
     */
    fun openFoundObject(found: ITetroidObject) {
        launchOnMain {
            val type = found.type
            when (type) {
                FoundType.TYPE_RECORD -> (found as? TetroidRecord)?.let { openRecord(it) }
                FoundType.TYPE_FILE -> (found as? TetroidFile)?.let { showRecordAttaches(it.record, false) }
                FoundType.TYPE_NODE -> (found as? TetroidNode)?.let { showNode(it) }
                FoundType.TYPE_TAG -> (found as? TetroidTag)?.let { showTagRecords(it) }
            }
            if (type != FoundType.TYPE_RECORD) {
                openPage(PageType.MAIN)
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
        launchOnMain {
            log(getString(R.string.global_search_start, profile.query))
            sendEvent(BaseEvent.TaskStarted(R.string.global_searching))
            withIo {
                globalSearchUseCase.run(
                    GlobalSearchUseCase.Params(profile)
                )
            }.onComplete {
                sendEvent(BaseEvent.TaskFinished/*, Gravity.NO_GRAVITY*/)
            }.onFailure {
                logFailure(it)
            }.onSuccess { result ->
                if (profile.isSearchInNode) {
                    profile.node?.let { node ->
                        log(getString(R.string.global_search_by_node_result, node.name), show = true)
                    }
                }

                // уведомляем, если не смогли поискать в зашифрованных ветках
                if (result.isExistCryptedNodes) {
                    log(R.string.log_found_crypted_nodes, show = true)
                }
                log(getString(R.string.global_search_end, result.foundObjects.size))

                sendEvent(MainEvent.GlobalSearchFinished(result.foundObjects, profile))
            }
        }
    }

    fun startGlobalSearchFromFilterQuery() {
        launchOnMain {
            sendEvent(MainEvent.GlobalSearchStart(query = lastFilterQuery))
        }
    }

    // endregion Global search

    // region Filter

    /**
     * Фильтр записей, меток или файлов (смотря какой список активен в данный момент).
     * @param query
     */
    fun filterListInMainPage(query: String, isSaveQuery: Boolean) {
        if (isSaveQuery) {
            TetroidSuggestionProvider.saveRecentQuery(getContext(), query)
        }
        filterListInMainPage(query, currentMainViewType)
    }

    private fun filterListInMainPage(query: String, viewType: MainViewType) {
        when (viewType) {
            MainViewType.NODE_RECORDS -> filterNodeRecords(query)
            MainViewType.TAG_RECORDS -> filterTagRecords(query)
            MainViewType.RECORD_ATTACHES -> filterRecordAttaches(query)
            else -> Unit
        }
    }

    private fun filterNodeRecords(query: String) {
        if (curNode != null) {
            filterRecords(query, curNode!!.records, MainViewType.NODE_RECORDS)
        } else {
            log(R.string.search_records_search_select_node, show = true)
        }
    }

    private fun filterTagRecords(query: String) {
        if (selectedTags.isNotEmpty()) {
            val tagsRecords = getTagsRecords(selectedTags)
            filterRecords(query, tagsRecords, MainViewType.TAG_RECORDS)
        } else {
            log(R.string.search_records_select_tag, show = true)
        }
    }

    private fun filterRecords(query: String, records: List<TetroidRecord>, viewType: MainViewType) {
        launchOnMain {
            val message = if (viewType == MainViewType.NODE_RECORDS)
                getString(R.string.filter_records_in_node_by_query, getCurNodeName(), query)
            else getString(R.string.filter_records_in_tag_by_query, getSelectedTagsNames(), query)
            log(message)
            val found = ScanManager.searchInRecordsNames(records, query)
            showRecords(found, viewType, dropSearch = false)
            if (lastFilterQuery.isNullOrEmpty()) {
                lastFilterQuery = query
            }
            sendEvent(MainEvent.RecordsFiltered(query, found, viewType))
        }
    }

    private fun filterRecordAttaches(query: String) {
        if (curRecord != null) {
            filterAttaches(query, curRecord!!) }
        else {
            logError(getString(R.string.log_cur_record_is_not_set), show = true)
        }
    }

    private fun filterAttaches(query: String, record: TetroidRecord) {
        launchOnMain {
            log(getString(R.string.filter_files_by_query, record.name, query))
            val found = ScanManager.searchInFiles(record.attachedFiles, query)
            showAttaches(found)
            sendEvent(
                MainEvent.AttachesFiltered(
                    query = query,
                    attaches = found,
                    viewType = MainViewType.RECORD_ATTACHES,
                )
            )
        }
    }

    fun onRecordsSearchClose() {
        launchOnMain {
            // "сбрасываем" фильтрацию, но не для только что открытых списков записей
            // (т.к. при открытии списка записей вызывается setIconified=false, при котором вызывается это событие,
            // что приводит к повторному открытию списка записей)
            if (isDropRecordsFiltering) {
                when (currentMainViewType) {
                    MainViewType.NODE_RECORDS -> if (curNode != null) {
                        showRecords(curNode!!.records, MainViewType.NODE_RECORDS, dropSearch = false)
                    }
                    MainViewType.TAG_RECORDS -> if (selectedTags.isNotEmpty()) {
                        val tagsRecords = getTagsRecords(selectedTags)
                        showRecords(tagsRecords, MainViewType.TAG_RECORDS, dropSearch = false)
                    }
                    else -> Unit
                }
            }
        }
    }

    // endregion Filter

    // region FileObserver

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
                    storageTreeInteractor.startObserver(
                        storagePath = storagePathProvider.getPathToMyTetraXml()
                    )
                }
            }
        } else {
            storageTreeInteractor.stopObserver()
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
                        sendEvent(StorageEvent.TreeChangedOutside)
                    }
                }
                TetroidFileObserver.Event.Moved,
                TetroidFileObserver.Event.Deleted -> {
                    // проверяем существование mytetra.xml только после задержки (т.к. файл мог быть пересоздан)
                    launchOnMain {
                        withIo {
                            delay(MYTETRA_XML_EXISTING_DELAY)
                        }
                        if (storagePathProvider.getPathToMyTetraXml().isFileExist()) {
                            log(R.string.log_storage_tree_changed_outside)
                            sendEvent(StorageEvent.TreeChangedOutside)
                        } else {
                            log(R.string.log_storage_tree_deleted_outside)
                            sendEvent(StorageEvent.TreeDeletedOutside)
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

    // endregion FileObserver

    // region Main view

    fun initStorageOrUi() {
        launchOnMain {
            if (isStorageLoaded()) {
                // инициализация контролов
                // действия после загрузки хранилища
                sendEvent(StorageEvent.Loaded(
                    isLoaded = true,
                    isLoadedFavoritesOnly = isLoadedFavoritesOnly(),
                    isHandleReceivedIntent = true,
                ))
            } else {
                // загружаем хранилище, если еще не загружано
                checkPermissionsAndInitDefaultStorage()
            }
        }
    }

    fun onMainViewBackPressed(mainPageIndex: Int): Boolean {
        var res = false
        if (mainPageIndex == MainViewType.RECORD_ATTACHES.index) {
            res = true
            when (lastMainViewType) {
                MainViewType.NODE_RECORDS,
                MainViewType.TAG_RECORDS,
                // Constants.VIEW_FOUND_RECORDS,
                MainViewType.FAVORITES ->
                    showMainView(lastMainViewType)
                else -> showMainView(MainViewType.NONE)
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
                    sendEvent(StorageEvent.AskForSyncAfterFailureSyncOnExit)
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
        storage?.also { storage ->
            when {
                !storage.isClearTrashBeforeExit -> {
                    callback?.run(true)
                }
                storage.isAskBeforeClearTrashBeforeExit && isNeedAsk -> {
                    launchOnMain {
                        sendEvent(StorageEvent.AskBeforeClearTrashOnExit(callback))
                    }
                }
                else -> {
                    launchOnMain {
                        withIo {
                            clearStorageTrashFolderUseCase.run(
                                ClearStorageTrashFolderUseCase.Params(storage)
                            )
                        }.onFailure {
                            logError(R.string.title_trash_clear_error, show = true)
                            logFailure(it, show = false)
                            callback?.run(false)
                        }.onSuccess {
                            log(R.string.title_trash_cleared, show = false)
                            callback?.run(true)
                        }
                    }
                }
            }
        } ?: run {
            callback?.run(true)
        }
    }

    private fun onExit() {
        log(R.string.log_app_exit)

        // останавливаем отслеживание изменения структуры хранилища
        storageTreeInteractor.stopObserver()

        // удаляем загруженные данные хранилища из памяти
        clearStorageDataFromMemory()
    }

    // endregion Main view

    private suspend fun swapObjectsInStorageTree(
        list: List<Any>,
        pos: Int,
        isUp: Boolean,
        through: Boolean
    ): Either<Failure, Boolean> {
        return swapObjectsInListUseCase.run(
            SwapObjectsInListUseCase.Params(
                list = list,
                position = pos,
                isUp = isUp,
                through = through,
            )
        ).flatMap { result ->
            saveStorageUseCase.run()
                .map { result }
        }
    }

}