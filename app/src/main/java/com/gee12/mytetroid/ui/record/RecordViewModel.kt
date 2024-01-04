package com.gee12.mytetroid.ui.record

import android.app.Activity
import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentToFileUseCase
import android.speech.SpeechRecognizer
import android.text.TextUtils
import androidx.annotation.UiThread
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getFileName
import com.gee12.mytetroid.common.extensions.orFalse
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.common.utils.Utils
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.domain.*
import com.gee12.mytetroid.domain.interactor.*
import com.gee12.mytetroid.domain.manager.*
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.domain.repo.StoragesRepo
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.storage.StorageViewModel
import com.gee12.mytetroid.domain.usecase.attach.AttachFileToRecordUseCase
import com.gee12.mytetroid.domain.usecase.crypt.*
import com.gee12.mytetroid.domain.usecase.file.GetFileModifiedDateInStorageUseCase
import com.gee12.mytetroid.domain.usecase.file.GetFolderSizeInStorageUseCase
import com.gee12.mytetroid.domain.usecase.node.GetNodeByIdUseCase
import com.gee12.mytetroid.domain.usecase.record.*
import com.gee12.mytetroid.domain.usecase.image.SaveImageFromBitmapUseCase
import com.gee12.mytetroid.domain.usecase.image.SaveImageFromUriUseCase
import com.gee12.mytetroid.domain.usecase.network.DownloadFileFromWebUseCase
import com.gee12.mytetroid.domain.usecase.network.DownloadImageFromWebUseCase
import com.gee12.mytetroid.domain.usecase.network.DownloadWebPageContentUseCase
import com.gee12.mytetroid.domain.usecase.storage.*
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.model.permission.TetroidPermission
import com.gee12.mytetroid.ui.storage.StorageEvent
import java.io.File


class RecordViewModel(
    app: Application,
    buildInfoProvider: BuildInfoProvider,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,

    settingsManager: CommonSettingsManager,
    appPathProvider: IAppPathProvider,
    storageProvider: IStorageProvider,
    sensitiveDataProvider: ISensitiveDataProvider,
    storagePathProvider: IStoragePathProvider,
    recordPathProvider: IRecordPathProvider,
    dataNameProvider: IDataNameProvider,

    storagesRepo: StoragesRepo,
    cryptManager: IStorageCryptManager,
    storageDataProcessor: IStorageDataProcessor,

    favoritesManager: FavoritesManager,
    interactionManager: InteractionManager,
    syncInteractor: SyncInteractor,

    getFileModifiedDateUseCase : GetFileModifiedDateInStorageUseCase,
    getFolderSizeUseCase: GetFolderSizeInStorageUseCase,

    initOrCreateStorageUseCase: InitOrCreateStorageUseCase,
    readStorageUseCase: ReadStorageUseCase,
    saveStorageUseCase: SaveStorageUseCase,
    decryptStorageUseCase: DecryptStorageUseCase,
    checkStorageFilesExistingUseCase: CheckStorageFilesExistingUseCase,
    clearStorageTrashFolderUseCase: ClearStorageTrashFolderUseCase,
    checkStoragePasswordAndDecryptUseCase: CheckPasswordOrPinAndDecryptUseCase,
    checkStoragePasswordUseCase: CheckPasswordOrPinAndAskUseCase,
    changePasswordUseCase: ChangePasswordUseCase,
    setupPasswordUseCase : SetupPasswordUseCase,

    getNodeByIdUseCase: GetNodeByIdUseCase,
    getRecordByIdUseCase: GetRecordByIdUseCase,

    private val createTempRecordUseCase: CreateTempRecordUseCase,
    private val getRecordHtmlTextDecryptedUseCase : GetRecordHtmlTextUseCase,
    private val saveRecordHtmlTextUseCase : SaveRecordHtmlTextUseCase,
    private val attachFileToRecordUseCase : AttachFileToRecordUseCase,
    private val saveImageFromUriUseCase : SaveImageFromUriUseCase,
    private val saveImageFromBitmapUseCase : SaveImageFromBitmapUseCase,
    private val editRecordFieldsUseCase : EditRecordFieldsUseCase,
    private val getRecordFolderUseCase : GetRecordFolderUseCase,
    private val printDocumentToFileUseCase : PrintDocumentToFileUseCase,
    private val downloadWebPageContentUseCase : DownloadWebPageContentUseCase,
    private val downloadImageFromWebUseCase : DownloadImageFromWebUseCase,
    private val downloadFileFromWebUseCase: DownloadFileFromWebUseCase,

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
    checkPasswordOrPinAndDecryptUseCase = checkStoragePasswordAndDecryptUseCase,
    checkPasswordOrPinUseCase = checkStoragePasswordUseCase,
    checkStorageFilesExistingUseCase = checkStorageFilesExistingUseCase,
    clearStorageTrashFolderUseCase = clearStorageTrashFolderUseCase,
    changePasswordUseCase = changePasswordUseCase,
    setupPasswordUseCase = setupPasswordUseCase,

    getNodeByIdUseCase = getNodeByIdUseCase,
    getRecordByIdUseCase = getRecordByIdUseCase,
) {

    var curRecord = MutableLiveData<TetroidRecord>()
    private var recordFolder: DocumentFile? = null
    val recordState = RecordState(
        recordId = "",
        isSavedFromTemporary = false,
        isFieldsEdited = false,
        isTextEdited = false,
    )
    var editorMode = EditorMode.VIEW
    private var editorModeToSwitch: EditorMode? = null
    private var isFirstLoad = true
    private var isReceivedImages = false
    private var isSaveTempAfterStorageLoaded = false
    private var resultObj: ResultObject = ResultObject.None

    init {
        storageProvider.init(storageDataProcessor)
        cryptManager.init(
            cryptRecordFilesIfNeedUseCase,
            parseRecordTagsUseCase,
        )
    }

    fun init(intent: Intent) {
        // проверяем передавались ли изображения
        isReceivedImages = intent.hasExtra(Constants.EXTRA_IMAGES_URI)

        initStorage(intent)
    }


    //region Migration

    fun checkMigration() {
        if (isNeedMigration()) {
            launchOnMain {
                sendEvent(RecordEvent.NeedMigration)
            }
        }
    }

    //endregion Migration

    //region Storage

    private fun initStorage(intent: Intent) {
        launchOnMain {
            when (intent.action) {
                Intent.ACTION_MAIN -> {
                    initStorageFromIntent(intent)
                }
                Constants.ACTION_ADD_RECORD -> {
                    initDefaultStorage()
                }
                else -> {
                    sendEvent(RecordEvent.FinishActivityWithResult())
                }
            }
        }
    }
    /**
     * Инициализация хранилища по ID, переданному в Intent.
     */
    private suspend fun initStorageFromIntent(intent: Intent) {
        var storageId = intent.getIntExtra(Constants.EXTRA_STORAGE_ID, 0)

        if (storageId > 0) {
            if (storage?.let { it.id == storageId } == true) {
                storage?.also {
                    sendEvent(StorageEvent.FoundInBase(it))
                    sendEvent(StorageEvent.Inited(it))
                }
            } else {
                startInitStorageFromBase(storageId)
            }
        } else {
            // если id хранилища не передано, пытаемся загрузить последнее используемое
            storageId = settingsManager.getLastStorageId()
            if (storageId > 0) {
                checkPermissionsAndInitStorageById(storageId)
            } else {
                logError(getString(R.string.log_not_transferred_storage_id), show = true)
                sendEvent(RecordEvent.FinishActivityWithResult())
            }
        }
    }

    private suspend fun initDefaultStorage() {
        // инициализация хранилища по ID хранилища, загруженному в последний раз.
        var storageId = withIo { storagesRepo.getDefaultStorageId() }
        if (storageId > 0) {
            if (storage?.let { it.id == storageId } == true) {
                storage?.also {
                    sendEvent(StorageEvent.FoundInBase(it))
                    sendEvent(StorageEvent.Inited(it))
                }
            } else {
                checkPermissionsAndInitStorageById(storageId)
            }
        } else {
            logWarning(R.string.log_not_set_default_storage_and_try_load_last)
            // если основное хранилище не указано, пытаемся загрузить последнее используемое
            storageId = settingsManager.getLastStorageId()
            if (storageId > 0) {
                checkPermissionsAndInitStorageById(storageId)
            } else {
                logError(getString(R.string.log_not_transferred_storage_id), show = true)
            }
        }
    }

    fun onStorageInited(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_MAIN -> {
                // открытие или создание записи из главной активности
                initRecordFromStorage(intent)
            }
            Constants.ACTION_ADD_RECORD -> {
                initRecordFromWidget()
            }
            else -> {
                launchOnMain {
                    sendEvent(RecordEvent.FinishActivityWithResult())
                }
            }
        }
    }

    /**
     * Старт загрузки хранилища.
     */
    private fun loadStorage() {
        startLoadStorage(isLoadFavoritesOnly = false)
    }

    fun onStorageLoaded(isLoaded: Boolean) {
        if (isLoaded) {
            if (isSaveTempAfterStorageLoaded) {
                isSaveTempAfterStorageLoaded = false
                // сохраняем временную запись
                launchOnMain {
                    sendEvent(RecordEvent.ShowEditFieldsDialog(resultObj))
                }
            }
        }
    }

    //endregion Storage

    // region Load page

    /**
     * Событие окончания загрузки страницы.
     */
    fun onPageLoaded() {
        if (isFirstLoad) {
            isFirstLoad = false
            // переключаем режим отображения
            val mode = if (
                isRecordNew()
                || isRecordTemporary()
                || isReceivedImages
                || CommonSettings.isRecordEditMode(getContext())
            ) {
                EditorMode.EDIT
            } else {
                EditorMode.VIEW
            }

            // сбрасываем флаг, т.к. уже воспользовались
            curRecord.value!!.setIsNew(false)
            switchMode(mode)
        } else {
            // переключаем только views
            launchOnMain {
                sendEvent(RecordEvent.SwitchEditorMode(mode = editorMode))
            }
        }
    }

    fun onEditorJSLoaded(receivedIntent: Intent) {
        // вставляем переданные изображения
        if (isReceivedImages) {
            receivedIntent.getParcelableArrayListExtra<Uri>(Constants.EXTRA_IMAGES_URI)?.let { uris ->
                saveImages(uris, isCamera = false)
            }
        }
    }

    @UiThread
    fun onHtmlRequestHandled() {
        // теперь сохраняем текст заметки без вызова предварительных методов
        saveRecord(false, resultObj)
        // переключаем режим, если асинхронное сохранение было вызвано в процессе переключения режима
        editorModeToSwitch?.also {
            switchMode(newMode = it, isNeedSave = false)
        }
    }

    fun loadPageFile(uri: Uri): DocumentFile? {
        return uri
            .takeIf { it.scheme in arrayOf(ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE) }
            ?.getFileName(getContext())
            ?.let { fileName ->
                val recordFolderPath = recordFolder?.getAbsolutePath(getContext())
                    ?: return null
                val filePath = FilePath.File(recordFolderPath, fileName)

                recordFolder?.child(
                    context = getContext(),
                    path = filePath.fileName,
                    requiresWriteAccess = !storage?.isReadOnly.orFalse()
                )
            }
    }

    // endregion Load page

    // region Load links

    /**
     * Открытие ссылки в тексте.
     * @param srcUrl
     */
    fun onLinkLoad(srcUrl: String, baseUrl: String?) {
        // раскодируем url, т.к. он может содержать кириллицу и прочее
        var url = try {
            URLDecoder.decode(srcUrl, "UTF-8")
        } catch (ex: UnsupportedEncodingException) {
            srcUrl
        }
        // удаляем baseUrl из строки адреса
        if (baseUrl != null && url.startsWith(baseUrl)) {
            url = url.replace(baseUrl, "")
        }
        TetroidObject.parseUrl(url)?.let { obj ->
            // обрабатываем внутреннюю ссылку
            when (obj.type) {
                FoundType.TYPE_RECORD -> {
                    if (isLoadedFavoritesOnly()) {
                        openAnotherRecord(recordId = obj.id, isAskForSave = true)
                    } else {
                        launchOnMain {
                            val record = getRecord(obj.id)
                            if (record != null) {
                                openAnotherRecord(recordId = record.id, isAskForSave = true)
                            } else {
                                logWarning(getString(R.string.log_not_found_record) + obj.id)
                            }
                        }
                    }
                }
                FoundType.TYPE_NODE ->
                    if (isLoadedFavoritesOnly()) {
                        openAnotherNode(nodeId = obj.id, isAskForSave = true)
                    } else {
                        launchOnMain {
                            val node = getNode(obj.id)
                            if (node != null) {
                                openAnotherNode(nodeId = node.id, isAskForSave = true)
                            } else {
                                logWarning(getString(R.string.error_node_not_found_with_id_mask, obj.id))
                            }
                        }
                    }
                FoundType.TYPE_TAG -> {
                    val tag = obj.id
                    if (tag.isNotEmpty()) {
                        openTag(tagName = tag, isAskForSave = true)
                    } else {
                        logWarning(getString(R.string.title_tag_name_is_empty))
                    }
                }
                FoundType.TYPE_AUTHOR,
                FoundType.TYPE_FILE -> {
                }
                else -> {
                    logWarning(getString(R.string.log_link_to_obj_parsing_error))
                }
            }
        } ?: run {
            // обрабатываем внешнюю ссылку
            launchOnMain {
                sendEvent(RecordEvent.OpenWebLink(link = url))
            }
        }
    }

    /**
     * Открытие метки по ссылке.
     *
     * TODO: Заменить на TetroidObject.parseUrl(url)
     *
     * @param url
     */
    fun onTagUrlLoad(url: String) {
        // декодируем url
        val decodedUrl = try {
            URLDecoder.decode(url, "UTF-8")
        } catch (ex: UnsupportedEncodingException) {
            logError(getString(R.string.log_url_decode_error) + url, ex)
            null
        }
        if (decodedUrl?.startsWith(TetroidTag.LINKS_PREFIX) == true) {
            // избавляемся от приставки "tag:"
            val tagName = decodedUrl.substring(TetroidTag.LINKS_PREFIX.length)
            openTag(tagName, true)
        } else {
            logWarning(getString(R.string.log_wrong_tag_link_format))
        }
    }

    //endregion Load links

    //region Open record

    /**
     * Получение записи из хранилища.
     * @param intent
     */
    private fun initRecordFromStorage(intent: Intent) {
        launchOnMain {
            // получаем переданную запись
            val recordId = intent.getStringExtra(Constants.EXTRA_RECORD_ID)
            if (recordId != null) {
                // получаем запись
                val record = getRecord(recordId)
                if (record != null) {
                    initRecord(record)

//                setVisibilityActionHome(!mRecord.isTemp());
                    if (intent.hasExtra(Constants.EXTRA_ATTACHED_FILES)) {
                        // временная запись создана для прикрепления файлов
                        val filesCount = record.attachedFilesCount
                        if (filesCount > 0) {
                            val mes = if (filesCount == 1)
                                getString(R.string.mes_attached_file_mask, record.attachedFiles[0].name)
                            else getString(R.string.mes_attached_files_mask, filesCount)
                            showMessage(mes)
                        }
                    }
                } else {
                    logError(getString(R.string.log_not_found_record) + recordId, true)
                    sendEvent(RecordEvent.FinishActivityWithResult())
                }
            } else {
                logError(getString(R.string.log_not_transferred_record_id), true)
                sendEvent(RecordEvent.FinishActivityWithResult())
            }
        }
    }

    /**
     * Создание новой временной записи, т.к. активность была запущена из виджета AddRecordWidget.
     */
    private fun initRecordFromWidget() {
        launchOnMain {
            sendEvent(RecordEvent.ShowHomeButton(isVisible = false))

            // создаем временную запись
            withIo {
                createTempRecordUseCase.run(
                    CreateTempRecordUseCase.Params(
                        srcName = null,
                        url = null,
                        text = null,
                        node = getQuicklyOrRootNode(),
                    )
                )
            }.onFailure {
                logFailure(it)
                sendEvent(RecordEvent.FinishActivityWithResult())
            }.onSuccess { record ->
                initRecord(record)
            }
        }
    }

    private suspend fun initRecordFolder(record: TetroidRecord) {
        recordFolder = withIo {
            getRecordFolderUseCase.run(
                GetRecordFolderUseCase.Params(
                    record = record,
                    createIfNeed = false,
                    inTrash = record.isTemp,
                    showMessage = true,
                )
            )
        }.foldResult(
            onLeft = {
                logFailure(it, show = true)
                null
            },
            onRight = { it }
        )
    }

    /**
     * Отображение записи (свойств и текста).
     */
    fun checkPermissionsAndLoadRecord(record: TetroidRecord) {
        launchOnIo {
            val htmlFilePath = recordPathProvider.getPathToFileInRecordFolder(record, record.fileName)
            recordFolder?.child(
                context = getContext(),
                path = record.fileName,
                requiresWriteAccess = isStorageReadOnly()
            )?.let { htmlFile ->
                checkAndRequestFileStoragePermission(
                    storage = storage!!,
                    uri = htmlFile.uri,
                    requestCode = PermissionRequestCode.OPEN_RECORD_FILE,
                )
            } ?: logFailure(Failure.File.Get(htmlFilePath))
        }
    }

    fun loadRecordAfterPermissionsGranted() {
        launchOnMain {
            curRecord.value?.also { record ->
                log(getString(R.string.log_record_loading) + record.id)
                sendEvent(RecordEvent.LoadFields(record))
                // текст
                loadRecordTextFromFile(record)
            }
        }
    }

    private fun loadRecordTextFromFile(record: TetroidRecord) {
        launchOnMain {
            var text: String? = null
            if (!record.isNew) {
                text = withIo {
                    getRecordHtmlTextDecryptedUseCase.run(
                        GetRecordHtmlTextUseCase.Params(
                            record = record,
                            recordFolder = recordFolder,
                            showMessage = true,
                        )
                    ).foldResult(
                        onLeft = {
                            logFailure(it)
                            null
                        },
                        onRight = { it }
                    )
                }
                if (text == null) {
                    if (record.isCrypted && cryptManager.getErrorCode() > 0) {
                        logError(R.string.log_error_record_file_decrypting)
                        sendEvent(BaseEvent.ShowMoreInLogs)
                    }
                }
            }
            sendEvent(
                RecordEvent.LoadHtmlTextFromFile(
                    recordText = text.orEmpty()
                )
            )
        }
    }

    //endregion Open record

    //region Open another objects

    /**
     * Открытие другой записи по внутренней ссылке.
     */
    private fun openAnotherRecord(recordId: String, isAskForSave: Boolean) {
        val resObj = ResultObject.OpenRecord(recordId)
        if (!onSaveRecord(resObj, isAskForSave)) {
            if (isLoadedFavoritesOnly()) {
                launchOnMain {
                    sendEvent(RecordEvent.AskForLoadAllNodes(resObj))
                }
            } else {
                launchOnMain {
                    sendEvent(RecordEvent.OpenAnotherRecord(recordId = recordId))
                }
            }
        }
    }

    /**
     * Открытие другой ветки по внутренней ссылке.
     */
    private fun openAnotherNode(nodeId: String, isAskForSave: Boolean) {
        val resultObj = ResultObject.OpenNode(nodeId)
        if (!onSaveRecord(resultObj, isAskForSave)) {
            if (isLoadedFavoritesOnly()) {
                launchOnMain {
                    sendEvent(RecordEvent.AskForLoadAllNodes(resultObj))
                }
            } else {
                launchOnMain {
                    sendEvent(RecordEvent.OpenAnotherNode(nodeId = nodeId))
                }
            }
        }
    }

    /**
     * Открытие записей метки в главной активности.
     */
    private fun openTag(tagName: String, isAskForSave: Boolean) {
        val resultObj = ResultObject.OpenTag(tagName)
        if (!onSaveRecord(resultObj, isAskForSave)) {
            if (isLoadedFavoritesOnly()) {
                launchOnMain {
                    sendEvent(
                        RecordEvent.AskForLoadAllNodes(resultObj)
                    )
                }
            } else {
                launchOnMain {
                    sendEvent(RecordEvent.OpenTag(tagName = tagName))
                }
            }
        }
    }

    /**
     * Открытие ветки записи.
     */
    fun showRecordNode() {
        openAnotherNode(nodeId = curRecord.value!!.node.id, isAskForSave = true)
    }

    //endregion Open another objects

    //region Image

    fun saveImages(imageUris: List<Uri>, isCamera: Boolean) {
        if (imageUris.isEmpty()) return

        launchOnMain {
            sendEvent(if (isCamera) RecordEvent.StartCaptureCamera else RecordEvent.StartLoadImages)

            var errorCount = 0
            val savedImages: MutableList<TetroidImage> = ArrayList()
            for (uri in imageUris) {
                withIo {
                    saveImageFromUriUseCase.run(
                        SaveImageFromUriUseCase.Params(
                            record = curRecord.value!!,
                            srcImageUri = uri,
                            deleteSrcImageFile = isCamera,
                        )
                    )
                }.onFailure {
                    errorCount++
                }.onSuccess { image ->
                    savedImages.add(image)
                }
            }
            if (errorCount > 0) {
                logWarning(getString(R.string.log_failed_to_save_images_mask, errorCount))
                sendEvent(BaseEvent.ShowMoreInLogs)
            } else {
                // сбрасываем флаг, чтобы небыло зацикливания
                isReceivedImages = false
            }

            sendEvent(RecordEvent.InsertImages(images = savedImages))
        }
    }

    fun saveImage(imageUri: Uri, deleteSrcFile: Boolean) {
        launchOnMain {
            withIo {
                saveImageFromUriUseCase.run(
                    SaveImageFromUriUseCase.Params(
                        record = curRecord.value!!,
                        srcImageUri = imageUri,
                        deleteSrcImageFile = deleteSrcFile,
                    )
                )
            }.onFailure {
                logFailure(it)
                sendEvent(BaseEvent.ShowMoreInLogs)
            }.onSuccess { image ->
                sendEvent(RecordEvent.InsertImages(images = listOf(image)))
            }
        }
    }

    private fun saveImage(bitmap: Bitmap) {
        launchOnMain {
            showProgressWithText(R.string.state_image_saving)
            withIo {
                saveImageFromBitmapUseCase.run(
                    SaveImageFromBitmapUseCase.Params(
                        record = curRecord.value!!,
                        bitmap = bitmap,
                    )
                )
            }.onComplete {
                hideProgress()
            }.onFailure {
                logFailure(it, show = true)
                sendEvent(BaseEvent.ShowMoreInLogs)
            }.onSuccess { image ->
                sendEvent(RecordEvent.InsertImages(images = listOf(image)))
            }
        }
    }

    fun downloadWebPageContent(url: String, isTextOnly: Boolean) {
        launchOnMain {
            showProgressWithText(R.string.title_page_downloading)
            withIo {
                downloadWebPageContentUseCase.run(
                    DownloadWebPageContentUseCase.Params(
                        url = url,
                        isTextOnly = isTextOnly,
                    )
                )
            }.onComplete {
                hideProgress()
            }.onSuccess { content ->
                sendEvent(
                    if (isTextOnly) {
                        RecordEvent.InsertWebPageText(text = content)
                    }
                    else {
                        RecordEvent.InsertWebPageContent(content = content)
                    }
                )
            }.onFailure {
                logFailure(it, show = true)
            }
        }
    }

    fun downloadImage(url: String) {
        launchOnMain {
            showProgressWithText(R.string.state_image_downloading)
            withIo {
                downloadImageFromWebUseCase.run(
                    DownloadImageFromWebUseCase.Params(
                        url = url,
                    )
                )
            }.onComplete {
                hideProgress()
            }.onSuccess { bitmap ->
                saveImage(bitmap)
            }.onFailure {
                logFailure(it, show = true)
            }
        }
    }

    //endregion Image

    //region Attach

    /**
     * Прикрепление нового файла к записи.
     */
    fun attachFile(fileUri: Uri, deleteSrcFile: Boolean) {
        curRecord.value?.let { record ->
            attachFile(fileUri, record, deleteSrcFile)
        }
    }

    private fun attachFile(uri: Uri, record: TetroidRecord, deleteSrcFile: Boolean) {
        launchOnMain {
            sendEvent(BaseEvent.TaskStarted(R.string.task_attach_file))
            withIo {
                attachFileToRecordUseCase.run(
                    AttachFileToRecordUseCase.Params(
                        fileUri = uri,
                        record = record,
                        deleteSrcFile = deleteSrcFile,
                    )
                )
            }.onComplete {
                sendEvent(BaseEvent.TaskFinished)
            }.onFailure {
                logFailure(it)
                sendEvent(BaseEvent.ShowMoreInLogs)
            }.onSuccess { attach ->
                log(getString(R.string.log_file_was_attached), show = true)
                sendEvent(RecordEvent.FileAttached(attach))
            }
        }
    }

    fun downloadAndAttachFile(uri: Uri) {
        launchOnMain {
            showProgressWithText(R.string.state_file_downloading)
            withIo {
                downloadFileFromWebUseCase.run(
                    DownloadFileFromWebUseCase.Params(
                        url = uri.toString(),
                    )
                )
            }.onComplete {
                hideProgress()
            }.onFailure {
                logFailure(it, show = true)
            }.onSuccess { uri ->
                // прикрепляем и удаляем файл из кэша
                attachFile(fileUri = uri, deleteSrcFile = true)
            }
        }
    }

    /**
     * Открытие списка прикрепленных файлов записи.
     */
    private fun openRecordAttaches(recordId: String, isAskForSave: Boolean) {
        val resultObj = ResultObject.OpenAttaches(recordId)
        if (!onSaveRecord(resultObj, isAskForSave)) {
            launchOnMain {
                sendEvent(RecordEvent.OpenRecordAttaches(recordId))
            }
        }
    }

    /**
     * Открытие списка прикрепленных файлов записи.
     */
    fun openRecordAttaches() {
        openRecordAttaches(recordId = curRecord.value!!.id, isAskForSave = true)
    }

    //endregion Attach

    //region Mode

    /**
     * Переключение режима отображения содержимого записи.
     * @param newMode
     */
    @UiThread
    fun switchMode(newMode: EditorMode) {
        switchMode(newMode, true)
    }

    @UiThread
    private fun switchMode(newMode: EditorMode, isNeedSave: Boolean) {
        editorModeToSwitch = null
        val oldMode = editorMode
        // сохраняем
        var runBeforeSaving = false
        if (isNeedSave
            && CommonSettings.isRecordAutoSave(getContext())
            && !curRecord.value!!.isTemp
        ) {
            // автоматически сохраняем текст записи, если:
            //  * есть изменения
            //  * не находимся в режиме HTML (сначала нужно перейти в режим EDIT (WebView), а уже потом можно сохранять)
            //  * запись не временная
            if (recordState.isTextEdited && editorMode != EditorMode.HTML) {
                runBeforeSaving = saveRecord(ResultObject.None)
            }
        }
        if (runBeforeSaving) {
            // если асинхронно запущена предобработка сохранения, то выходим
            editorModeToSwitch = newMode
        } else {
            launchOnMain {
                // перезагружаем html-текст записи в webView, если был режим редактирования HTML
                if (oldMode == EditorMode.HTML) {
                    sendEvent(RecordEvent.LoadRecordTextFromHtml)
                } else {
                    sendEvent(RecordEvent.SwitchEditorMode(mode = newMode))
                }
                editorMode = newMode
                sendEvent(RecordEvent.UpdateOptionsMenu)
            }
        }
    }

    //endregion Mode

    //region SaveRecord

    /**
     * Сохранение изменений при скрытии или выходе из активности.
     * @param isAskForSave
     * @param resultObj если null - закрываем активность, иначе - выполняем действия с объектом
     * @return false - можно продолжать действие (н-р, закрывать активность), true - начатое
     * действие нужно прервать, чтобы дождаться результата из диалога
     */
    private fun onSaveRecord(resultObj: ResultObject, isAskForSave: Boolean): Boolean {
        if (recordState.isTextEdited || isRecordTemporary()) {
            if (CommonSettings.isRecordAutoSave(getContext()) && !isRecordTemporary()) {
                // сохраняем без запроса
                return saveRecord(resultObj)
            } else if (isAskForSave) {
                launchOnMain {
                    sendEvent(RecordEvent.AskForSaving(resultObj))
                }
                return true
            }
        }
        return false
    }

    /**
     * Сохранение html-текста записи в файл.
     * @return true - запущена ли перед сохранением предобработка в асинхронном режиме.
     */
    fun saveRecord(resultObj: ResultObject): Boolean {
        val runBeforeSaving = CommonSettings.isFixEmptyParagraphs(getContext())
        if (runBeforeSaving) {
            this.resultObj = resultObj
        }
        return saveRecord(runBeforeSaving, resultObj) || runBeforeSaving
    }

    /**
     * Сохранение html-текста записи в файл с предобработкой.
     * @param callBefore Нужно ли перед сохранением совершить какие-либы манипуляции с html ?
     * @return true - запущен ли код в асинхронном режиме.
     */
    private fun saveRecord(callBefore: Boolean, resultObj: ResultObject): Boolean {
        return if (callBefore) {
            launchOnMain {
                sendEvent(RecordEvent.BeforeSaving)
            }
            true
        } else {
            if (isRecordTemporary()) {
                if (isStorageLoaded()) {
                    launchOnMain {
                        sendEvent(RecordEvent.ShowEditFieldsDialog(resultObj))
                    }
                } else {
                    isSaveTempAfterStorageLoaded = true
                    loadStorage()
                }
                true
            } else {
                launchOnMain {
                    // запрашиваем у View html-текст записи и сохраняем
                    sendEvent(RecordEvent.GetHtmlTextAndSaveToFile(resultObj))
                }
                false
            }
        }
    }

    /**
     * Получение актуального html-текста записи из WebView и непосредственное сохранение в файл.
     */
    fun saveRecordText(htmlText: String, obj: ResultObject) {
        launchOnMain {
            curRecord.value?.let { record ->
                log(resourcesProvider.getString(R.string.log_start_record_file_saving_mask, record.id))
                showProgressWithText(R.string.state_record_saving)
                withIo {
                    saveRecordHtmlTextUseCase.run(
                        SaveRecordHtmlTextUseCase.Params(
                            record = record,
                            html = htmlText,
                        )
                    )
                }.onComplete {
                    hideProgress()
                }.onFailure {
                    logFailure(it)
                }.onSuccess {
                    log(R.string.log_record_saved, true)
                    // сбрасываем пометку изменения записи
                    dropIsEdited()
                    updateEditedDate()
                    onAfterSaving(obj)
                }
            }
        }
    }

    /**
     * Обновление поля последнего изменения записи.
     */
    private fun updateEditedDate() {
        val record = curRecord.value!!
        if (buildInfoProvider.isFullVersion() && !isRecordNew() && !isRecordTemporary()) {
            launchOnMain {
                withIo {
                    getFileModifiedDateUseCase.run(
                        GetFileModifiedDateInStorageUseCase.Params(
                            fileRelativePath = recordPathProvider.getRelativePathToFileInRecordFolder(record, record.fileName),
                        )
                    )
                }.onFailure {
                    logFailure(it)
                }.map { date ->
                    Utils.dateToString(date, getString(R.string.full_date_format_string))
                }.onSuccess { dateString ->
                    sendEvent(RecordEvent.EditedDateChanged(dateString = dateString))
                }
            }
        }
    }

    /**
     * Обработчик отмены сохранения записи.
     */
    fun onRecordSavingCanceled(resultObj: ResultObject) {
        if (isRecordTemporary()) {
            // удаляем временную запись из ветки
            curRecord.value?.also { record ->
                record.node.deleteRecord(record)
            }
        }
        onAfterSaving(resultObj)
    }

    /**
     * Обработчик события после сохранения записи, вызванное при ответе на запрос сохранения в диалоге.
     */
    private fun onAfterSaving(resultObj: ResultObject) {
        when (resultObj) {
            is ResultObject.Finish -> {
                finishRequest(isOpenMainActivity = resultObj.isOpenMainActivity)
            }
            is ResultObject.OpenRecord -> {
                openAnotherRecord(recordId = resultObj.recordId, isAskForSave = false)
            }
            is ResultObject.OpenNode -> {
                openAnotherNode(nodeId = resultObj.nodeId, isAskForSave = false)
            }
            is ResultObject.OpenAttaches -> {
                openRecordAttaches(recordId = resultObj.recordId, isAskForSave = false)
            }
            is ResultObject.OpenTag -> {
                openTag(tagName = resultObj.tagName, isAskForSave = false)
            }
            ResultObject.None -> {
                if (resultObj.needReloadText) {
                    // перезагружаем baseUrl в WebView
                    loadRecordTextFromFile(record = curRecord.value!!)
                }
            }
        }
    }

    // endregion Search

    /**
     * Действия перед закрытием активности, если свойства записи были изменены.
     * @return false - можно продолжать действие (н-р, закрывать активность), true - начатое
     * действие нужно прервать, чтобы дождаться результата из диалога
     */
    private fun finishRequest(isOpenMainActivity: Boolean) {
        launchOnMain {
            if (recordState.isFieldsEdited) {
                if (isOpenMainActivity) {
                    // запускаем главную активность, помещая результат
                    val node = curRecord.value!!.node
                    if (node != null) {
                        sendEvent(RecordEvent.OpenRecordNodeInMainView(nodeId = node.id))
                    } else {
                        showMessage(getString(R.string.log_record_node_is_empty), LogType.WARNING)
                    }
                }
                else {
                    sendEvent(RecordEvent.FinishActivityWithResult(isOpenMainActivity = false))
                }
            } else {
                sendEvent(RecordEvent.FinishActivityWithResult(isOpenMainActivity = isOpenMainActivity))
            }
        }
    }

    //region Interaction

    /**
     * Открытие каталога записи.
     */
    fun openRecordFolder(activity: Activity) {
        val record = curRecord.value!!
        logger.logDebug(resourcesProvider.getString(R.string.log_start_record_folder_opening_mask, record.id))

        recordFolder?.also {
            val uri = it.uri
            if (!interactionManager.openFolder(activity, uri)) {
                Utils.writeToClipboard(getContext(), resourcesProvider.getString(R.string.title_record_folder_uri), uri.toString())
                logWarning(R.string.log_missing_file_manager, show = true)
            }
        }
    }

    /**
     * Отправка записи.
     */
    fun shareRecord(srcText: String) {
        var text = srcText
        // FIXME: если получать текст из html-кода ВСЕЙ страницы,
        //  то Html.fromHtml() неверно обрабатывает код стиля в шапке:
        //  <header><style> p, li { white-space: pre-wrap; } </style></header>
        //  добавляя его в результат
        val except = "p, li { white-space: pre-wrap; } "
        if (text.startsWith(except)) {
            text = text.substring(except.length)
        }
        interactionManager.shareText(getContext(), curRecord.value!!.name, text)
    }

    fun exportRecordTextToPdfFile(
        folder: DocumentFile,
        pdfFileName: String,
        printAdapter: PrintDocumentAdapter,
        printAttributes: PrintAttributes,
    ) {
        val filePath = FilePath.File(folder.getAbsolutePath(getContext()), pdfFileName)
        val pdfFile = folder.makeFile(
            context = getContext(),
            name = filePath.fileName,
            mimeType = MimeType.getMimeTypeFromExtension("pdf"),
            mode = CreateMode.CREATE_NEW,
        )
        if (pdfFile != null) {
            launchOnMain {
                showProgressWithText(R.string.state_export_to_pdf)
                // специально в Main
                withMain {
                    printDocumentToFileUseCase.run(
                        PrintDocumentToFileUseCase.Params(
                            printAdapter = printAdapter,
                            printAttributes = printAttributes,
                            uri = pdfFile.uri,
                        )
                    )
                }.onComplete {
                    hideProgress()
                }.onFailure {
                    logFailure(it)
                }.onSuccess {
                    sendEvent(RecordEvent.AskToOpenExportedPdf(pdfFile))
                }
            }
        } else {
            logFailure(Failure.File.Create(filePath))
        }
    }

    //endregion Interaction

    //region Voice input

    fun startVoiceInput() {
        launchOnMain {
            sendEvent(BaseEvent.Permission.Check(
                permission = TetroidPermission.RecordAudio,
                requestCode = PermissionRequestCode.RECORD_AUDIO,
            ))
        }
    }

    fun onVoiceInputError(errorCode: Int) {
        val message = when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "unknown error"
        }
        if (errorCode in arrayOf(SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_NO_MATCH)) {
            showMessage(getString(R.string.error_recognize_text))
        } else /*if (viewModel.buildInfoProvider.isDebug)*/ {
            logError(message, show = true)
        }
    }

    //endregion Voice input

    /**
     * Сохранение записи при любом скрытии активности.
     */
    fun onPause() {
        if (recordState.isTextEdited && !isRecordTemporary() && CommonSettings.isRecordAutoSave(getContext())) {
            saveRecord(resultObj = ResultObject.None)
        }
    }

    /**
     * Проверка, следует ли выполнять стандартный обработчик кнопки Home в Toolbar.
     * @return true - отработает стандартный обработчик кнопки Home в Toolbar.
     */
    fun isCanGoHome(): Boolean {
        return if (!onSaveRecord(isAskForSave = true, resultObj = ResultObject.Finish(isOpenMainActivity = true))) {
            // не был запущен асинхронный код при сохранении, поэтому
            //  можем выполнить стандартный обработчик кнопки home (должен быть возврат false),
            //  но после проверки изменения свойств (если изменены, то включится другой механизм
            //  выхода из активности)
            if (recordState.isFieldsEdited) {
                // запускаем главную активность, помещая результат
                val node = curRecord.value!!.node
                if (node != null) {
                    launchOnMain {
                        sendEvent(RecordEvent.OpenRecordNodeInMainView(nodeId = node.id))
                    }
                } else {
                    showMessage(getString(R.string.log_record_node_is_empty), LogType.WARNING)
                }
                false
            } else {
                true
            }
        } else {
            false
        }
    }

    /**
     * Проверка, слудуте ли выполнять стандартный обработчик кнопки Back.
     *  При этом, если возвращаться некуда (isFromAnotherActivity=false), то можем просто закрывать приложение.
     * @return true - отработает стандартный обработчик кнопки Back.
     */
    fun isCanBack(): Boolean {
        // выполняем родительский метод только если не был запущен асинхронный код
        return if (!onSaveRecord(isAskForSave = true, resultObj = ResultObject.Finish(isOpenMainActivity = false))) {
            if (recordState.isFieldsEdited) {
                launchOnMain {
                    sendEvent(RecordEvent.FinishActivityWithResult(isOpenMainActivity = false))
                }
                false
            } else {
                true
            }
        } else {
            false
        }
    }

    /**
     * Удаление записи.
     */
    fun deleteRecord() {
        launchOnMain {
            sendEvent(RecordEvent.DeleteRecord(recordId = curRecord.value!!.id))
        }
    }

    private fun dropIsEdited() {
        recordState.isTextEdited = false
        launchOnMain {
            sendEvent(RecordEvent.IsEditedChanged(isEdited = false))
        }
    }

    fun editFields(
        obj: ResultObject?,
        name: String,
        tags: String,
        author: String,
        url: String,
        node: TetroidNode,
        isFavorite: Boolean
    ) {
        launchOnMain {
            sendEvent(RecordEvent.SaveFields.InProcess)

            val record = curRecord.value!!
            val wasTemporary = record.isTemp
            withIo {
                editRecordFieldsUseCase.run(
                    EditRecordFieldsUseCase.Params(
                        record = record,
                        name = name,
                        tagsString = tags,
                        author = author,
                        url = url,
                        node = node,
                        isFavor = isFavorite
                    )
                )
            }.onFailure {
                logFailure(it)
            }.onSuccess {
                recordState.isFieldsEdited = true
                recordState.isSavedFromTemporary = wasTemporary
                sendEvent(RecordEvent.SaveFields.Success)

                if (wasTemporary) {
                    // обновляем путь к записи, если изначально она была временная (в корзине)
                    initRecordFolder(record)
                }
                setTitle(name)
                sendEvent(RecordEvent.LoadFields(record = record))
                if (wasTemporary) {
                    // сохраняем текст записи
                    val resultObj = ResultObject.None.apply {
                        if (obj == null) {
                            // baseUrl изменился, нужно перезагрузить в WebView
                            needReloadText = true
                        }
                    }
                    saveRecord(resultObj)
                    // показываем кнопку Home для возврата в ветку записи
                    sendEvent(RecordEvent.ShowHomeButton(isVisible = true))
                } else {
                    log(R.string.log_record_fields_changed, true)
                }
            }
        }
    }

    fun getFileUriToRecordFolder(): Uri? {
        return recordFolder?.getAbsolutePath(getContext())
            ?.let { Uri.fromFile(File(it)) }
    }

    /**
     * Отображение или скрытие панели свойств в зависимости от настроек.
     */
    fun isNeedExpandFields(): Boolean {
        return if (isRecordNew()
            || isReceivedImages
            || CommonSettings.isRecordEditMode(getContext())
        ) {
            false
        } else {
            when (CommonSettings.getShowRecordFields(getContext())) {
                getString(R.string.pref_show_record_fields_no) -> false
                getString(R.string.pref_show_record_fields_yes) -> true
                else -> {
                    curRecord.value != null
                            && (!TextUtils.isEmpty(curRecord.value!!.tagsString)
                                || !TextUtils.isEmpty(curRecord.value!!.author)
                                || !TextUtils.isEmpty(curRecord.value!!.url))
                }
            }
        }
    }

    fun setTitle(title: String) {
        launchOnMain {
            sendEvent(RecordEvent.UpdateTitle(title))
        }
    }

    private suspend fun initRecord(record: TetroidRecord) {
        initRecordFolder(record)

        curRecord.postValue(record)
        recordState.recordId = record.id

        setTitle(record.name)
    }

    fun setTextIsEdited(isEdited: Boolean) {
        recordState.isTextEdited = isEdited
    }

    fun getRecordName() = curRecord.value?.name

    fun isRecordNew() = curRecord.value?.isNew ?: true

    fun isRecordTemporary() = curRecord.value?.isTemp ?: true

    fun isViewMode() = editorMode == EditorMode.VIEW

    fun isEditMode() = editorMode == EditorMode.EDIT

    fun isHtmlMode() = editorMode == EditorMode.HTML

}

