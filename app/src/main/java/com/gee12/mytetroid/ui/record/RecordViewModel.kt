package com.gee12.mytetroid.ui.record

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
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
import com.gee12.mytetroid.common.extensions.buildIntent
import com.gee12.mytetroid.domain.NetworkHelper.IWebImageResult
import com.gee12.mytetroid.domain.NetworkHelper.IWebPageContentResult
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.ui.main.MainActivity
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.domain.*
import com.gee12.mytetroid.domain.interactor.*
import com.gee12.mytetroid.domain.manager.*
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.domain.repo.StoragesRepo
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.TetroidActivity
import com.gee12.mytetroid.ui.storage.StorageViewModel
import com.gee12.mytetroid.domain.usecase.InitAppUseCase
import com.gee12.mytetroid.domain.usecase.attach.AttachFileToRecordUseCase
import com.gee12.mytetroid.domain.usecase.crypt.*
import com.gee12.mytetroid.domain.usecase.file.GetFileModifiedDateInStorageUseCase
import com.gee12.mytetroid.domain.usecase.file.GetFolderSizeInStorageUseCase
import com.gee12.mytetroid.domain.usecase.node.GetNodeByIdUseCase
import com.gee12.mytetroid.domain.usecase.record.*
import com.gee12.mytetroid.domain.usecase.image.SaveImageFromBitmapUseCase
import com.gee12.mytetroid.domain.usecase.image.SaveImageFromUriUseCase
import com.gee12.mytetroid.domain.usecase.storage.*
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.model.permission.TetroidPermission


class RecordViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,

    settingsManager: CommonSettingsManager,
    appPathProvider: IAppPathProvider,
    buildInfoProvider: BuildInfoProvider,
    storageProvider: IStorageProvider,
    sensitiveDataProvider: ISensitiveDataProvider,
    storagePathProvider: IStoragePathProvider,
    recordPathProvider: IRecordPathProvider,
    dataNameProvider: IDataNameProvider,

    storagesRepo: StoragesRepo,
    cryptManager: IStorageCryptManager,

    favoritesManager: FavoritesManager,
    interactionManager: InteractionManager,
    syncInteractor: SyncInteractor,

    initAppUseCase: InitAppUseCase,
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
    initPasswordUseCase : InitPasswordUseCase,
    clearSavedPasswordUseCase: ClearSavedPasswordUseCase,

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
): StorageViewModel(
    app = app,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,

    settingsManager = settingsManager,
    appPathProvider = appPathProvider,
    buildInfoProvider = buildInfoProvider,
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

    initAppUseCase = initAppUseCase,
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
    initPasswordUseCase = initPasswordUseCase,
    clearSavedPasswordUseCase = clearSavedPasswordUseCase,

    getNodeByIdUseCase = getNodeByIdUseCase,
    getRecordByIdUseCase = getRecordByIdUseCase,
) {

    var curRecord = MutableLiveData<TetroidRecord>()
    var curMode = 0
    var isFirstLoad = true
    var isFieldsEdited = false
    var isReceivedImages = false
    var modeToSwitch = -1
    var isSaveTempAfterStorageLoaded = false
    var resultObj: ResultObj? = null
    var isEdited = false
    var isFromAnotherActivity = false


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

    fun onStorageInited(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_MAIN -> {
                // открытие или создание записи из главной активности
                initRecordFromStorage(intent)
            }
            Constants.ACTION_ADD_RECORD -> {
    //            viewModel.initApp()
                initRecordFromWidget()
            }
            else -> {
                launchOnMain {
                    sendEvent(BaseEvent.FinishActivity)
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
                    sendEvent(RecordEvent.EditFields(resultObj))
                }
            }
        }
    }

    //endregion Storage

    // region Load page

    fun onCreate(receivedIntent: Intent) {
        // проверяем передавались ли изображения
        isReceivedImages = receivedIntent.hasExtra(Constants.EXTRA_IMAGES_URI)
    }

    /**
     * Событие окончания загрузки страницы.
     */
    fun onPageLoaded() {
        if (isFirstLoad) {
            isFirstLoad = false
            // переключаем режим отображения
            val defMode = if (
                curRecord.value!!.isNew
                || isReceivedImages
                || CommonSettings.isRecordEditMode(getContext())
            ) Constants.MODE_EDIT
            else Constants.MODE_VIEW

            // сбрасываем флаг, т.к. уже воспользовались
            curRecord.value!!.setIsNew(false)
            switchMode(defMode)

            /*if (defMode == MODE_EDIT) {
            ViewUtils.showKeyboard(this, mEditor.getWebView(), false);
//                Keyboard.showKeyboard(mEditor);
        }*/
        } else {
            // переключаем только views
            launchOnMain {
                sendEvent(RecordEvent.SwitchViews(viewMode = curMode))
            }
        }
    }

    fun onEditorJSLoaded(receivedIntent: Intent) {
        // вставляем переданные изображения
        if (isReceivedImages) {
            receivedIntent.getParcelableArrayListExtra<Uri>(Constants.EXTRA_IMAGES_URI)?.let { uris ->
                saveImages(uris, false)
            }
        }
    }

    @UiThread
    fun onHtmlRequestHandled() {
        // теперь сохраняем текст заметки без вызова предварительных методов
        saveRecord(false, resultObj)
        // переключаем режим, если асинхронное сохранение было вызвано в процессе переключения режима
        if (modeToSwitch > 0) {
            switchMode(modeToSwitch, false)
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
        var url = srcUrl
        try {
            url = URLDecoder.decode(url, "UTF-8")
        } catch (e: UnsupportedEncodingException) {

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
                        openAnotherRecord(ResultObj(ResultObj.OPEN_RECORD, obj.id), true)
                    } else {
                        launchOnMain {
                            val record = getRecord(obj.id)
                            if (record != null) {
                                openAnotherRecord(ResultObj(record), true)
                            } else {
                                logWarning(getString(R.string.log_not_found_record) + obj.id)
                            }
                        }
                    }
                }
                FoundType.TYPE_NODE ->
                    if (isLoadedFavoritesOnly()) {
                        openAnotherNode(ResultObj(ResultObj.OPEN_NODE, obj.id), true)
                    } else {
                        launchOnMain {
                            val node = getNode(obj.id)
                            if (node != null) {
                                openAnotherNode(ResultObj(node), true)
                            } else {
                                logWarning(getString(R.string.error_node_not_found_with_id_mask, obj.id))
                            }
                        }
                    }
                FoundType.TYPE_TAG -> {
                    val tag = obj.id
                    if (tag.isNotEmpty()) {
                        openTag(tag, true)
                    } else {
                        logWarning(getString(R.string.title_tag_name_is_empty))
                    }
                }
                FoundType.TYPE_AUTHOR, FoundType.TYPE_FILE -> {
                }
                else -> logWarning(getString(R.string.log_link_to_obj_parsing_error))
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
            return
        }
        if (decodedUrl.startsWith(TetroidTag.LINKS_PREFIX)) {
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
     * @return
     */
    private fun initRecordFromStorage(intent: Intent) {
        launchOnMain {
            // получаем переданную запись
            val recordId = intent.getStringExtra(Constants.EXTRA_OBJECT_ID)
            if (recordId != null) {
                // получаем запись
                val record = getRecord(recordId)
                if (record != null) {
                    curRecord.postValue(record!!)
                    setTitle(record.name)
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
                    sendEvent(BaseEvent.FinishActivity)
                }
            } else {
                logError(getString(R.string.log_not_transferred_record_id), true)
                sendEvent(BaseEvent.FinishActivity)
            }
        }
    }

    /**
     * Создание новой временной записи, т.к. активность была запущена из виджета AddRecordWidget.
     * @return
     */
    private fun initRecordFromWidget() {
        launchOnMain {
            sendEvent(BaseEvent.ShowHomeButton(isVisible = false))

            // создаем временную запись
            withIo {
                createTempRecordUseCase.run(
                    CreateTempRecordUseCase.Params(
                        srcName = null,
                        url = null,
                        text = null,
                        node = quicklyNode ?: storageProvider.getRootNode(),
                    )
                )
            }.onFailure {
                logFailure(it)
                sendEvent(BaseEvent.FinishActivity)
            }.onSuccess { record ->
                curRecord.postValue(record)
                setTitle(record.name)
            }
        }
    }

    /**
     * Отображение записи (свойств и текста).
     */
    fun checkPermissionsAndLoadRecord(record: TetroidRecord) {
        val htmlFilePath = recordPathProvider.getRelativePathToFileInRecordFolder(record, record.fileName)
        storageFolder?.child(
            context = getContext(),
            path = htmlFilePath,
            requiresWriteAccess = isStorageReadOnly()
        )?.let { htmlFile ->
            checkAndRequestFileStoragePermission(
                storage = storage!!,
                uri = htmlFile.uri,
                requestCode = PermissionRequestCode.OPEN_RECORD_FILE,
            )
        } ?: logFailure(Failure.File.Get(FilePath.File(storageFolderPath, htmlFilePath)))
    }

    fun loadRecordAfterPermissionsGranted() {
        launchOnMain {
            val record = curRecord.value!!
            log(getString(R.string.log_record_loading) + record.id)
            sendEvent(RecordEvent.LoadFields(record))
            // текст
            loadRecordTextFromFile(record)
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
                RecordEvent.LoadRecordTextFromFile(
                    recordText = text.orEmpty()
                )
            )
        }
    }

    //endregion Open record

    //region Open another objects

    /**
     * Открытие другой записи по внутренней ссылке.
     * @param resObj
     * @param isAskForSave
     */
    private fun openAnotherRecord(resObj: ResultObj?, isAskForSave: Boolean) {
        if (resObj == null) return
        if (onSaveRecord(isAskForSave, resObj)) return
        if (isLoadedFavoritesOnly()) {
//            AskDialogs.showLoadAllNodesDialog(this,
//                IApplyResult { showAnotherRecord(resObj.id) })
            launchOnMain {
                sendEvent(RecordEvent.AskForLoadAllNodes(resObj))
            }
        } else {
            showAnotherRecord(resObj.id)
        }
    }

    fun showAnotherRecord(id: String?) {
        val bundle = Bundle()
        bundle.putString(Constants.EXTRA_OBJECT_ID, id)
        if (isFieldsEdited) {
            bundle.putBoolean(Constants.EXTRA_IS_FIELDS_EDITED, true)
        }
//        finishWithResult(RESULT_OPEN_RECORD, bundle)
        launchOnMain {
            sendEvent(
                BaseEvent.FinishWithResult(
                    code = Constants.RESULT_OPEN_RECORD,
                    bundle = bundle
                )
            )
        }
    }

    /**
     * Открытие другой ветки по внутренней ссылке.
     * @param resObj
     * @param isAskForSave
     */
    private fun openAnotherNode(resObj: ResultObj?, isAskForSave: Boolean) {
        if (resObj == null) return
        if (onSaveRecord(isAskForSave, ResultObj(resObj))) return
        if (isLoadedFavoritesOnly()) {
//            AskDialogs.showLoadAllNodesDialog(this,
//                IApplyResult { showAnotherNodeDirectly(resObj.id) })
            launchOnMain {
                sendEvent(RecordEvent.AskForLoadAllNodes(resObj))
            }
        } else {
            showAnotherNodeDirectly(resObj.id)
        }
    }

    fun showAnotherNodeDirectly(id: String?) {
        val bundle = Bundle()
        bundle.putString(Constants.EXTRA_OBJECT_ID, id)
//        finishWithResult(RESULT_OPEN_NODE, bundle)
        launchOnMain {
            sendEvent(
                BaseEvent.FinishWithResult(
                    code = Constants.RESULT_OPEN_NODE, bundle = bundle
                )
            )
        }
    }

    /**
     * Открытие записей метки в главной активности.
     * @param tagName
     * @param isAskForSave
     */
    private fun openTag(tagName: String, isAskForSave: Boolean) {
        if (onSaveRecord(isAskForSave, ResultObj(tagName))) return
//        if (StorageManager.isFavoritesMode()) {
        if (isLoadedFavoritesOnly()) {
//            AskDialogs.showLoadAllNodesDialog(this,
//                IApplyResult { openTagDirectly(tagName) })
            launchOnMain {
                sendEvent(
                    RecordEvent.AskForLoadAllNodes(
                        ResultObj(ResultObj.OPEN_TAG, tagName)
                    )
                )
            }
        } else {
            openTagDirectly(tagName)
        }
    }

    fun openTagDirectly(tagName: String?) {
        val bundle = Bundle()
        bundle.putString(Constants.EXTRA_TAG_NAME, tagName)
        if (isFieldsEdited) {
            bundle.putBoolean(Constants.EXTRA_IS_FIELDS_EDITED, true)
        }
//        finishWithResult(RESULT_SHOW_TAG, bundle)
        launchOnMain {
            sendEvent(
                BaseEvent.FinishWithResult(
                    code = Constants.RESULT_SHOW_TAG, bundle = bundle
                )
            )
        }
    }

    /**
     * Открытие ветки записи.
     */
    fun showRecordNode() {
        openAnotherNode(ResultObj(curRecord.value!!.node), true)
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
//                logOperError(LogObj.IMAGE, LogOper.SAVE, show = true)
                sendEvent(BaseEvent.ShowMoreInLogs)
            }.onSuccess { image ->
                sendEvent(RecordEvent.InsertImages(images = listOf(image)))
            }
        }
    }

    fun saveImage(bitmap: Bitmap) {
        launchOnMain {
            withIo {
                saveImageFromBitmapUseCase.run(
                    SaveImageFromBitmapUseCase.Params(
                        record = curRecord.value!!,
                        bitmap = bitmap,
                    )
                )
            }.onFailure {
                logFailure(it)
//                logOperError(LogObj.IMAGE, LogOper.SAVE, show = true)
                sendEvent(BaseEvent.ShowMoreInLogs)
            }.onSuccess { image ->
                sendEvent(RecordEvent.InsertImages(images = listOf(image)))
            }
        }
    }

    fun downloadWebPageContent(url: String?, isTextOnly: Boolean) {
        launchOnMain {
            sendEvent(
                BaseEvent.ShowProgressWithText(
                    message = getString(R.string.title_page_downloading)
                )
            )
            NetworkHelper.downloadWebPageContentAsync(url, isTextOnly, object : IWebPageContentResult {
                override fun onSuccess(content: String, isTextOnly: Boolean) {
                    launchOnMain {
//                    mEditor.insertWebPageContent(content, isTextOnly)
                        sendEvent(
                            if (isTextOnly) RecordEvent.InsertWebPageText(text = content)
                            else RecordEvent.InsertWebPageContent(content = content)
                        )
                        sendEvent(BaseEvent.HideProgress)
                    }
                }

                override fun onError(ex: java.lang.Exception) {
                    launchOnMain {
                        logError(getString(R.string.log_error_download_web_page_mask, ex.message!!), true)
                        sendEvent(BaseEvent.HideProgress)
                    }
                }
            })
        }
    }

    fun downloadImage(url: String?) {
        launchOnMain {
            sendEvent(
                BaseEvent.ShowProgressWithText(
                    message = getString(R.string.title_image_downloading)
                )
            )
            NetworkHelper.downloadImageAsync(url, object : IWebImageResult {
                override fun onSuccess(bitmap: Bitmap) {
                    launchOnMain {
                        saveImage(bitmap)
                        sendEvent(BaseEvent.HideProgress)
                    }
                }

                override fun onError(ex: java.lang.Exception) {
                    launchOnMain {
                        logError(getString(R.string.log_error_download_image_mask, ex.message!!), true)
                        sendEvent(BaseEvent.HideProgress)
                    }
                }
            })
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
                log(getString(R.string.log_file_was_attached), true)
                sendEvent(RecordEvent.FileAttached(attach))
            }
        }
    }

    fun downloadAndAttachFile(uri: Uri) {
        launchOnMain {
            super.downloadFileToCache(
                url = uri.toString(),
                callback = object : TetroidActivity.IDownloadFileResult {
                    override fun onSuccess(uri: Uri) {
                        // прикрепляем и удаляем файл из кэша
                        attachFile(uri, deleteSrcFile = true)
                    }
                    override fun onError(ex: Exception) {
                        logError(ex, show = true)
                    }
                }
            )
        }
    }

    /**
     * Открытие списка прикрепленных файлов записи.
     * @param isAskForSave
     */
    private fun openRecordAttaches(record: TetroidRecord?, isAskForSave: Boolean) {
        if (record == null) return
        if (onSaveRecord(isAskForSave, ResultObj(ResultObj.OPEN_FILE))) return
        val bundle = Bundle()
        bundle.putString(Constants.EXTRA_OBJECT_ID, record.id)
//        finishWithResult(RESULT_SHOW_ATTACHES, bundle)
        launchOnMain {
            sendEvent(
                BaseEvent.FinishWithResult(
                    code = Constants.RESULT_SHOW_ATTACHES, bundle = bundle
                )
            )
        }
    }

    /**
     * Открытие списка прикрепленных файлов записи.
     */
    fun openRecordAttaches() {
        openRecordAttaches(curRecord.value, true)
    }

    //endregion Attach

    //region Mode

    /**
     * Переключение режима отображения содержимого записи.
     * @param newMode
     */
    @UiThread
    fun switchMode(newMode: Int) {
        switchMode(newMode, true)
    }

    @UiThread
    private fun switchMode(newMode: Int, isNeedSave: Boolean) {
        modeToSwitch = -1
        val oldMode = curMode
        // сохраняем
//        onSaveRecord();
        var runBeforeSaving = false
        if (isNeedSave
            && CommonSettings.isRecordAutoSave(getContext())
            && !curRecord.value!!.isTemp
        ) {
            // автоматически сохраняем текст записи, если:
            //  * есть изменения
            //  * не находимся в режиме HTML (сначала нужно перейти в режим EDIT (WebView), а уже потом можно сохранять)
            //  * запись не временная
            if (isEdited && curMode != Constants.MODE_HTML) {
                runBeforeSaving = saveRecord(null)
            }
        }
        // если асинхронно запущена предобработка сохранения, то выходим
        if (runBeforeSaving) {
            modeToSwitch = newMode
            return
        }
        launchOnMain {
            // перезагружаем html-текст записи в webView, если был режим редактирования HTML
            if (oldMode == Constants.MODE_HTML) {
                sendEvent(RecordEvent.LoadRecordTextFromHtml)
            } else {
                sendEvent(RecordEvent.SwitchViews(viewMode = newMode))
            }
            curMode = newMode
            sendEvent(BaseEvent.UpdateOptionsMenu)
        }
    }

    //endregion Mode

    //region SaveRecord

    /**
     * Сохранение изменений при скрытии или выходе из активности.
     * @param showAskDialog
     * @param obj если null - закрываем активность, иначе - выполняем действия с объектом
     * @return false - можно продолжать действие (н-р, закрывать активность), true - начатое
     * действие нужно прервать, чтобы дождаться результата из диалога
     */
    private fun onSaveRecord(showAskDialog: Boolean, obj: ResultObj?): Boolean {
        if (isEdited) {
            if (CommonSettings.isRecordAutoSave(getContext()) && !curRecord.value!!.isTemp) {
                // сохраняем без запроса
                return saveRecord(obj)
            } else if (showAskDialog) {
                launchOnMain {
                    sendEvent(RecordEvent.AskForSaving(obj))
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
    fun saveRecord(obj: ResultObj?): Boolean {
        val runBeforeSaving = CommonSettings.isFixEmptyParagraphs(getContext())
        if (runBeforeSaving) {
            resultObj = obj
        }
        return saveRecord(runBeforeSaving, obj) || runBeforeSaving
    }

    /**
     * Сохранение html-текста записи в файл с предобработкой.
     * @param callBefore Нужно ли перед сохранением совершить какие-либы манипуляции с html ?
     * @return true - запущен ли код в асинхронном режиме.
     */
    private fun saveRecord(callBefore: Boolean, obj: ResultObj?): Boolean {
        if (callBefore) {
            launchOnMain {
                sendEvent(RecordEvent.BeforeSaving)
            }
            return true
        } else {
            if (curRecord.value!!.isTemp) {
                if (isStorageLoaded()) {
                    launchOnMain {
                        sendEvent(RecordEvent.EditFields(obj))
                    }
                } else {
                    isSaveTempAfterStorageLoaded = true
                    loadStorage()
                }
                return true
            } else {
                saveRecord()
                onAfterSaving(obj)
            }
        }
        return false
    }

    private fun saveRecord() {
        // запрашиваем у View html-текст записи и сохраняем
        launchOnMain {
            sendEvent(RecordEvent.Save)
        }
    }

    /**
     * Получение актуального html-текста записи из WebView и непосредственное сохранение в файл.
     */
    fun saveRecordText(htmlText: String) {
        launchOnMain {
            curRecord.value?.let { record ->
                log(resourcesProvider.getString(R.string.log_start_record_file_saving) + record.id)
                sendEvent(BaseEvent.ShowProgressWithText(message = resourcesProvider.getString(R.string.progress_save_record)))
                withIo {
                    saveRecordHtmlTextUseCase.run(
                        SaveRecordHtmlTextUseCase.Params(
                            record = record,
                            html = htmlText,
                        )
                    )
                }.onComplete {
                    sendEvent(BaseEvent.HideProgress)
                }.onFailure {
                    logFailure(it)
                }.onSuccess {
                    log(R.string.log_record_saved, true)
                    // сбрасываем пометку изменения записи
                    dropIsEdited()
                    updateEditedDate()
                }
            }
        }
    }

    /**
     * Обновление поля последнего изменения записи.
     */
    private fun updateEditedDate() {
        val record = curRecord.value!!
        if (buildInfoProvider.isFullVersion() && !record.isNew && !record.isTemp) {
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
     * Обработчик события после сохранения записи, вызванное при ответе на запрос сохранения в диалоге.
     */
    fun onAfterSaving(srcResObj: ResultObj?) {
        var resObj = srcResObj
        if (resObj == null) {
            resObj = ResultObj(null)
        }
        when (resObj.type) {
            ResultObj.EXIT,
            ResultObj.START_MAIN_ACTIVITY ->
                if (!onRecordFieldsIsEdited(resObj.type == ResultObj.START_MAIN_ACTIVITY)) {
                    launchOnMain {
                        sendEvent(BaseEvent.FinishActivity)
                    }
                }
            ResultObj.OPEN_RECORD -> openAnotherRecord(resObj, false)
            ResultObj.OPEN_NODE -> openAnotherNode(resObj, false)
            ResultObj.OPEN_FILE -> openRecordAttaches(curRecord.value, false)
            ResultObj.OPEN_TAG -> openTag((resObj.obj as String?)!!, false)
            ResultObj.NONE -> if (resObj.needReloadText) {
                // перезагружаем baseUrl в WebView
//                loadRecordText(mRecord, false)
                loadRecordTextFromFile(curRecord.value!!)
            }
        }
    }

    // endregion Search

    /**
     * Действия перед закрытием активности, если свойства записи были изменены.
     * @param startMainActivity
     * @return false - можно продолжать действие (н-р, закрывать активность), true - начатое
     * действие нужно прервать, чтобы дождаться результата из диалога
     */
    private fun onRecordFieldsIsEdited(startMainActivity: Boolean): Boolean {
        if (isFieldsEdited) {
            when {
//                ActivityCompat.getReferrer()
                isFromAnotherActivity -> {
                    launchOnMain {
                        // закрываем активность, возвращая результат:
                        // указываем родительской активности, что нужно обновить список записей
                        val intent = buildIntent {
                            putExtra(Constants.EXTRA_IS_FIELDS_EDITED, true)
                        }
                        sendEvent(
                            BaseEvent.SetActivityResult(
                                code = Activity.RESULT_OK,
                                intent = intent
                            )
                        )
                    }
                }
                startMainActivity -> {
                    launchOnMain {
                        // запускаем главную активность, помещая результат
//                bundle.putString(EXTRA_OBJECT_ID, mRecord.getId());
                        if (curRecord.value!!.node != null) {
                            openRecordNodeInMainView()
                            sendEvent(BaseEvent.FinishActivity)
                        } else {
                            showMessage(getString(R.string.log_record_node_is_empty), LogType.WARNING)
                        }
                    }
                    return true
                }
                else -> {
                    launchOnMain {
                        sendEvent(BaseEvent.FinishActivity)
                    }
                    return true
                }
            }
        }
        return false
    }

    private fun openRecordNodeInMainView() {
        val bundle = Bundle().apply {
            putInt(Constants.EXTRA_RESULT_CODE, Constants.RESULT_OPEN_NODE)
            putString(Constants.EXTRA_OBJECT_ID, curRecord.value!!.node.id)
        }
        val intent = Intent(getContext(), MainActivity::class.java).apply {
            putExtras(bundle)
            action = Constants.ACTION_RECORD
        }
        launchOnMain {
            sendEvent(BaseEvent.StartActivity(intent))
        }
    }

    //region Interaction

    /**
     * Открытие каталога записи.
     */
    fun openRecordFolder(activity: Activity) {
        val record = curRecord.value!!
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
                    Utils.writeToClipboard(getContext(), resourcesProvider.getString(R.string.title_record_folder_path), uri.path)
                    logWarning(R.string.log_missing_file_manager, show = true)
                }
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
                sendEvent(BaseEvent.ShowProgressWithText(resourcesProvider.getString(R.string.state_export_to_pdf)))
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
                    sendEvent(BaseEvent.HideProgress)
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
        if (curRecord.value?.isTemp == false) {
            onSaveRecord(false, null)
        }
    }

    /**
     *
     */
    fun onHomePressed(): Boolean {
        return if (!onSaveRecord(true, ResultObj(ResultObj.START_MAIN_ACTIVITY))) {
            // не был запущен асинхронный код при сохранении, поэтому
            //  можем выполнить стандартный обработчик кнопки home (должен быть возврат false),
            //  но после проверки изменения свойств (если изменены, то включится другой механизм
            //  выхода из активности)
            onRecordFieldsIsEdited(true)
        } else true
        // был запущен асинхронный код при сохранении,
        //  поэтому не выполняем стандартный обработчик кнопки home
    }

    fun isCanBack(isFromAnotherActivity: Boolean): Boolean {
        // выполняем родительский метод только если не был запущен асинхронный код
        if (!onSaveRecord(true, ResultObj(ResultObj.EXIT))) {
            if (!onRecordFieldsIsEdited(false)) {
                return true
            }
        }
        return false
    }

    /**
     * Удаление записи.
     */
    fun deleteRecord() {
        val bundle = Bundle()
        bundle.putString(Constants.EXTRA_OBJECT_ID, curRecord.value!!.id)
        launchOnMain {
            sendEvent(
                BaseEvent.FinishWithResult(
                    code = Constants.RESULT_DELETE_RECORD, bundle = bundle
                )
            )
        }
    }

    private fun dropIsEdited() {
        isEdited = false
        launchOnMain {
            sendEvent(RecordEvent.IsEditedChanged(isEdited = false))
        }
    }

    fun editFields(
        obj: ResultObj?,
        name: String,
        tags: String,
        author: String,
        url: String,
        node: TetroidNode,
        isFavor: Boolean
    ) {
        launchOnMain {
            val wasTemp = curRecord.value!!.isTemp
            withIo {
                editRecordFieldsUseCase.run(
                    EditRecordFieldsUseCase.Params(
                        record = curRecord.value!!,
                        name = name,
                        tagsString = tags,
                        author = author,
                        url = url,
                        node = node,
                        isFavor = isFavor
                    )
                )
            }.onFailure {
                logFailure(it)
//                if (wasTemp) {
//                    // все равно сохраняем текст записи
//                    logOperErrorMore(LogObj.TEMP_RECORD, LogOper.SAVE)
//                } else {
//                    logOperErrorMore(LogObj.RECORD_FIELDS, LogOper.CHANGE)
//                }
            }.onSuccess {
                isFieldsEdited = true
                setTitle(name)
                sendEvent(RecordEvent.LoadFields(record = curRecord.value!!))
                if (wasTemp) {
                    // сохраняем текст записи
                    val resObj = if (obj == null) {
                        ResultObj(null).apply {
                            // baseUrl изменился, нужно перезагрузить в WebView
                            needReloadText = true
                        }
                    } else {
                        null
                    }
                    saveRecord(resObj)
                    // показываем кнопку Home для возврата в ветку записи
                    sendEvent(BaseEvent.ShowHomeButton(isVisible = true))
                } else {
                    log(R.string.log_record_fields_changed, true)
                }
            }
        }
    }

    fun getUriToRecordFolder(): Uri {
        return recordPathProvider.getUriToRecordFolder(curRecord.value!!)
    }

    /**
     * Отображение или скрытие панели свойств в зависимости от настроек.
     */
    fun isNeedExpandFields(): Boolean {
        return if (
            curRecord.value!!.isNew
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
            sendEvent(BaseEvent.UpdateTitle(title))
        }
    }

    fun getRecordName() = curRecord.value?.name

    fun isRecordTemprorary() = curRecord.value?.isTemp ?: true

    fun isViewMode() = curMode == Constants.MODE_VIEW

    fun isEditMode() = curMode == Constants.MODE_EDIT

    fun isHtmlMode() = curMode == Constants.MODE_HTML

}

