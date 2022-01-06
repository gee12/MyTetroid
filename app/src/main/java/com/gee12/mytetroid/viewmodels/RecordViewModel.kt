package com.gee12.mytetroid.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import com.esafirm.imagepicker.features.ImagePicker
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.helpers.NetworkHelper
import com.gee12.mytetroid.helpers.NetworkHelper.IWebImageResult
import com.gee12.mytetroid.helpers.NetworkHelper.IWebPageContentResult
import com.gee12.mytetroid.helpers.UriHelper
import com.gee12.mytetroid.interactors.ImagesInteractor
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.utils.Utils
import com.gee12.mytetroid.views.activities.MainActivity
import com.gee12.mytetroid.views.activities.TetroidActivity.IDownloadFileResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*
import androidx.annotation.MainThread
import com.gee12.mytetroid.data.crypt.TetroidCrypter
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.data.xml.TetroidXml
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.repo.CommonSettingsRepo


class RecordViewModel(
    app: Application,
    /*logger: TetroidLogger?,*/
    storagesRepo: StoragesRepo?,
    settingsRepo: CommonSettingsRepo?,
    xmlLoader: TetroidXml?,
    crypter: TetroidCrypter?
): StorageViewModel(
    app,
    /*logger,*/
    storagesRepo,
    settingsRepo,
    xmlLoader,
    crypter
) {

    private val imagesInteractor = ImagesInteractor(this.logger, dataInteractor, recordsInteractor)

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

    @MainThread
    fun checkMigration() {
        if (isNeedMigration()) {
            setEvent(RecordEvents.NeedMigration)
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
                postViewEvent(Constants.ViewEvents.FinishActivity)
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
    @UiThread
    fun onPageLoaded() {
        launch {
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
                setEvent(RecordEvents.SwitchViews, curMode)
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
                        val record = recordsInteractor.getRecord(obj.id)
                        if (record != null) {
                            openAnotherRecord(ResultObj(record), true)
                        } else {
                            logWarning(getString(R.string.log_not_found_record) + obj.id)
                        }
                    }
                }
                FoundType.TYPE_NODE ->
                    if (isLoadedFavoritesOnly()) {
                        openAnotherNode(ResultObj(ResultObj.OPEN_NODE, obj.id), true)
                    } else {
                        val node = nodesInteractor.getNode(obj.id)
                        if (node != null) {
                            openAnotherNode(ResultObj(node), true)
                        } else {
                            logWarning(getString(R.string.log_not_found_node_id) + obj.id)
                        }
                    }
                FoundType.TYPE_TAG -> {
                    val tag = obj.id
                    if (!TextUtils.isEmpty(tag)) {
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
            postEvent(RecordEvents.OpenWebLink, url)
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
        launch {
            // загружаем параметры хранилища из бд
            if (!initStorage(intent)) return@launch

            // получаем переданную запись
            val recordId = intent.getStringExtra(Constants.EXTRA_OBJECT_ID)
            if (recordId != null) {
                // получаем запись
                val record = withContext(Dispatchers.IO) { recordsInteractor.getRecord(recordId) }
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
                    setViewEvent(Constants.ViewEvents.FinishActivity)
                }
            } else {
                logError(getString(R.string.log_not_transferred_record_id), true)
                setViewEvent(Constants.ViewEvents.FinishActivity)
            }
        }
    }

    /**
     * Создание новой временной записи, т.к. активность была запущена из виджета AddRecordWidget.
     * @return
     */
    private fun initRecordFromWidget() {
        postViewEvent(Constants.ViewEvents.ShowHomeButton)
        launch {
            // создаем временную запись
            val node = if (quicklyNode != null) quicklyNode!! else TetroidXml.ROOT_NODE
            val record = withContext(Dispatchers.IO) {
                recordsInteractor.createTempRecord(getContext(), null, null, null, node)
            }
            if (record != null) {
                curRecord.postValue(record!!)
                setTitle(record.name)
            } else {
                logOperError(LogObj.RECORD, LogOper.CREATE, true)
                setViewEvent(Constants.ViewEvents.FinishActivity)
            }
        }
    }

    /**
     * Отображение записи (свойств и текста).
     * @param record
     */
    fun openRecord(record: TetroidRecord) {
        log(getString(R.string.log_record_loading) + record.id)
        setEvent(RecordEvents.LoadFields, record)
        // текст
        loadRecordTextFromFile(record)
    }

    private fun loadRecordTextFromFile(record: TetroidRecord) {
        launch {
            var text: String? = null
            if (!record.isNew) {
                text = withContext(Dispatchers.IO) {
                    recordsInteractor.getRecordHtmlTextDecrypted(getContext(), record, true)
                }
                if (text == null) {
                    if (record.isCrypted && crypter.errorCode > 0) {
                        logError(R.string.log_error_record_file_decrypting)
                        setViewEvent(Constants.ViewEvents.ShowMoreInLogs)
                    }
                }
            }
            setEvent(RecordEvents.LoadRecordTextFromFile, text)
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
            postEvent(RecordEvents.AskForLoadAllNodes, resObj)
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
        postViewEvent(Constants.ViewEvents.FinishWithResult, ActivityResult(Constants.RESULT_OPEN_RECORD, bundle))
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
            postEvent(RecordEvents.AskForLoadAllNodes, resObj)
        } else {
            showAnotherNodeDirectly(resObj.id)
        }
    }

    fun showAnotherNodeDirectly(id: String?) {
        val bundle = Bundle()
        bundle.putString(Constants.EXTRA_OBJECT_ID, id)
//        finishWithResult(RESULT_OPEN_NODE, bundle)
        postViewEvent(Constants.ViewEvents.FinishWithResult, ActivityResult(Constants.RESULT_OPEN_NODE, bundle))
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
            postEvent(RecordEvents.AskForLoadAllNodes, ResultObj(ResultObj.OPEN_TAG, tagName))
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
        postViewEvent(Constants.ViewEvents.FinishWithResult, ActivityResult(Constants.RESULT_SHOW_TAG, bundle))
    }

    /**
     * Открытие ветки записи.
     */
    fun showRecordNode() {
        openAnotherNode(ResultObj(curRecord.value!!.node), true)
    }

    //endregion Open another objects

    //region Image

    /**
     * Обработка выбранных изображений.
     * @param data
     */
    fun saveSelectedImages(data: Intent, isCamera: Boolean) {
        postEvent(if (isCamera) RecordEvents.StartCaptureCamera else RecordEvents.StartLoadImages)

        // FIXME: Устарело, переделать на callback
        val images = ImagePicker.getImages(data)
        if (images == null) {
            logError(getString(R.string.log_selected_files_is_missing))
            return
        }
        val uris: MutableList<Uri> = ArrayList()
        for (image in images) {
            uris.add(Uri.fromFile(File(image.path)))
        }
        saveImages(uris, isCamera)
    }

    private fun saveImages(imageUris: List<Uri>, deleteSrcFile: Boolean) {
        launch {
            var errorCount = 0
            val savedImages: MutableList<TetroidImage> = ArrayList()
            for (uri in imageUris) {
                val savedImage = imagesInteractor.saveImage(getContext(), curRecord.value!!, uri, deleteSrcFile)
                if (savedImage != null) {
                    savedImages.add(savedImage)
                } else {
                    errorCount++
                }
            }
            if (errorCount > 0) {
                logWarning(String.format(getString(R.string.log_failed_to_save_images_mask), errorCount))
                postViewEvent(Constants.ViewEvents.ShowMoreInLogs)
            }
            if (!savedImages.isEmpty()) {
//            mEditor.insertImages(savedImages)
                postEvent(RecordEvents.InsertImages, savedImages)
            }
        }
    }

    fun saveImage(imageUri: Uri, deleteSrcFile: Boolean) {
        launch {
            val savedImage = imagesInteractor.saveImage(getContext(), curRecord.value!!, imageUri, deleteSrcFile)
            saveImage(savedImage)
        }
    }

    fun saveImage(bitmap: Bitmap) {
        launch {
            val savedImage = imagesInteractor.saveImage(getContext(), curRecord.value!!, bitmap)
            saveImage(savedImage)
        }
    }

    private fun saveImage(image: TetroidImage?) {
        if (image != null) {
//            mEditor.insertImage(image)
            postEvent(RecordEvents.InsertImages, listOf(image))
        } else {
            logOperError(LogObj.IMAGE, LogOper.SAVE, true)
            postViewEvent(Constants.ViewEvents.ShowMoreInLogs)
        }
    }

    fun downloadWebPageContent(url: String?, isTextOnly: Boolean) {
        launch {
            postViewEvent(Constants.ViewEvents.ShowProgressText, getString(R.string.title_page_downloading))
            NetworkHelper.downloadWebPageContentAsync(url, isTextOnly, object : IWebPageContentResult {
                override fun onSuccess(content: String?, isTextOnly: Boolean) {
//                    mEditor.insertWebPageContent(content, isTextOnly)
                    if (isTextOnly) postEvent(RecordEvents.InsertWebPageText)
                    else postEvent(RecordEvents.InsertWebPageContent)
                    postViewEvent(Constants.ViewEvents.ShowProgress, false)
                }

                override fun onError(ex: java.lang.Exception) {
                    logError(getString(R.string.log_error_download_web_page_mask, ex.message!!), true)
                    postViewEvent(Constants.ViewEvents.ShowProgress, false)
                }
            })
        }
    }

    fun downloadImage(url: String?) {
        launch {
            postViewEvent(Constants.ViewEvents.ShowProgressText, getString(R.string.title_image_downloading))
            NetworkHelper.downloadImageAsync(url, object : IWebImageResult {
                override fun onSuccess(bitmap: Bitmap) {
                    saveImage(bitmap)
                    postViewEvent(Constants.ViewEvents.ShowProgress, false)
                }

                override fun onError(ex: java.lang.Exception) {
                    logError(getString(R.string.log_error_download_image_mask, ex.message!!), true)
                    postViewEvent(Constants.ViewEvents.ShowProgress, false)
                }
            })
        }
    }

    //endregion Image

    //region Attach

    /**
     * TODO: добавить удаление исходного файла
     * @param uri
     */
    fun attachFile(uri: Uri?, deleteSrcFile: Boolean) {
        UriHelper(getContext()).getPath(uri)?.let {
//        AttachFileFromRecordTask(mRecord, deleteSrcFile).run(uriHelper.getPath(uri))
            attachFile(it, curRecord.value, deleteSrcFile)
        }
    }

//    /**
//     * Задание, в котором выполняется прикрепление нового файла в записи.
//     */
//    public class AttachFileFromRecordTask extends AttachFileTask {
//        public AttachFileFromRecordTask(TetroidRecord record, boolean deleteSrcFile) {
//            super(record, deleteSrcFile);
//        }
//        @Override
//        protected void onPostExecute(TetroidFile res) {
//            taskPostExecute(Gravity.NO_GRAVITY);
//            onFileAttached(res);
//        }
//    }

    private fun attachFile(fileFullName: String, record: TetroidRecord?, deleteSrcFile: Boolean) {
        postViewEvent(Constants.ViewEvents.TaskStarted, R.string.task_attach_file)
        launch {
            withContext(Dispatchers.IO) {
                val attach = attachesInteractor.attachFile(getContext(), fileFullName, record, deleteSrcFile)
                postViewEvent(Constants.ViewEvents.TaskFinished)
                if (attach != null) {
                    log(getString(R.string.log_file_was_attached), true)
                    postEvent(RecordEvents.FileAttached, attach)
                } else {
                    logError(getString(R.string.log_files_attach_error), true)
                    postViewEvent(Constants.ViewEvents.ShowMoreInLogs)
                }
            }
        }
    }

    fun downloadAndAttachFile(uri: Uri) {
        launch {
            super.downloadFileToCache(uri.toString(), object : IDownloadFileResult {
                override fun onSuccess(uri: Uri) {
                    attachFile(uri, true)
                }
                override fun onError(ex: java.lang.Exception) {}
            })
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
        postViewEvent(Constants.ViewEvents.FinishWithResult, ActivityResult(Constants.RESULT_SHOW_ATTACHES, bundle))
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
            && !curRecord.value!!.isTemp) {
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
        // перезагружаем html-текст записи в webView, если был режим редактирования HTML
        if (oldMode == Constants.MODE_HTML) {
            setEvent(RecordEvents.LoadRecordTextFromHtml)
        } else {
            setEvent(RecordEvents.SwitchViews, newMode)
        }
        curMode = newMode
        setViewEvent(Constants.ViewEvents.UpdateOptionsMenu)
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
                postEvent(RecordEvents.AskForSaving, obj)
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
            postEvent(RecordEvents.BeforeSaving)
            return true
        } else {
            if (curRecord.value!!.isTemp) {
                if (isLoaded()) {
                    postEvent(RecordEvents.EditFields, obj)
                } else {
                    isSaveTempAfterStorageLoaded = true
                    loadStorage()
                }
                return true
            } else {
                save()
                onAfterSaving(obj)
            }
        }
        return false
    }

    private fun save() {
        // запрашиваем у View html-текст записи и сохраняем
        postEvent(RecordEvents.Save)
    }

    /**
     * Получение актуального html-текста записи из WebView и непосредственное сохранение в файл.
     */
    fun save(htmlText: String) {
        log(getString(R.string.log_before_record_save) + curRecord.value!!.id)
//        val htmlText: String = mEditor.getDocumentHtml()
        //        String htmlText = (mCurMode == MODE_HTML)
//                ? TetroidEditor.getDocumentHtml(mEditTextHtml.getText().toString()) : mEditor.getDocumentHtml();
        if (recordsInteractor.saveRecordHtmlText(getContext(), curRecord.value!!, htmlText)) {
            log(R.string.log_record_saved, true)
            // сбрасываем пометку изменения записи
            dropIsEdited()
            updateEditedDate()
        } else {
            logOperErrorMore(LogObj.RECORD, LogOper.SAVE)
        }
    }

    /**
     * Обновление поля последнего изменения записи.
     */
    private fun updateEditedDate() {
        if (App.isFullVersion()) {
            val dateFormat = getString(R.string.full_date_format_string)
            val edited = recordsInteractor.getEditedDate(getContext(), curRecord.value!!)
//            (findViewById<View>(R.id.text_view_record_edited) as TextView).text =
            val editedDate = if (edited != null) Utils.dateToString(edited, dateFormat) else ""
            postEvent(RecordEvents.EditedDateChanged, editedDate)
        }
    }

    /**
     * Обработчик события после сохранения записи, вызванное при ответе на запрос сохранения в диалоге.
     * @param resObj
     */
    fun onAfterSaving(srcResObj: ResultObj?) {
        var resObj = srcResObj
        if (resObj == null) {
            resObj = ResultObj(null)
        }
        launch {
            when (resObj.type) {
                ResultObj.EXIT,
                ResultObj.START_MAIN_ACTIVITY ->
                    if (!onRecordFieldsIsEdited(resObj.type == ResultObj.START_MAIN_ACTIVITY)) {
                        postViewEvent(Constants.ViewEvents.FinishActivity)
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
    }

    // endregion Search
    /**
     * Действия перед закрытием активности, если свойства записи были изменены.
     * @param startMainActivity
     * @return false - можно продолжать действие (н-р, закрывать активность), true - начатое
     * действие нужно прервать, чтобы дождаться результата из диалога
     */
    @MainThread
    private fun onRecordFieldsIsEdited(startMainActivity: Boolean): Boolean {
        if (isFieldsEdited) {
            when {
//                ActivityCompat.getReferrer()
                isFromAnotherActivity -> {
                    // закрываем активность, возвращая результат:
                    // указываем родительской активности, что нужно обновить список записей
                    val intent = Intent()
                    intent.putExtra(Constants.EXTRA_IS_FIELDS_EDITED, true)
                    setViewEvent(Constants.ViewEvents.SetActivityResult, ActivityResult(Activity.RESULT_OK, intent = intent))
                }
                startMainActivity -> {
                    // запускаем главную активность, помещая результат
//                bundle.putString(EXTRA_OBJECT_ID, mRecord.getId());
                    if (curRecord.value!!.node != null) {
                        openRecordNodeInMainView();
                        setViewEvent(Constants.ViewEvents.FinishActivity)
                    } else {
                        showMessage(getString(R.string.log_record_node_is_empty), LogType.WARNING)
                    }
                    return true
                }
                else -> {
                    setViewEvent(Constants.ViewEvents.FinishActivity)
                    return true
                }
            }
        }
        return false
    }

    @MainThread
    private fun openRecordNodeInMainView() {
        val bundle = Bundle().apply {
            putInt(Constants.EXTRA_RESULT_CODE, Constants.RESULT_OPEN_NODE)
            putString(Constants.EXTRA_OBJECT_ID, curRecord.value!!.node.id)
        }
        val intent = Intent(getContext(), MainActivity::class.java).apply {
            putExtras(bundle)
            action = Constants.ACTION_RECORD
        }
//        ViewUtils.startActivity(this, MainActivity::class.java, bundle, Constants.ACTION_RECORD, 0, null)
        setViewEvent(Constants.ViewEvents.StartActivity, intent)
    }

    //endregion SaveRecord

    //region Storage

    /**
     * Старт загрузки хранилища.
     */
    private fun loadStorage() {
//        boolean isLoadLastForced = false;
//        boolean isCheckFavorMode = !mRecord.isTemp();
//        if (storagePath == null) {
//            StorageManager.startInitStorage(this, this, isLoadLastForced, isCheckFavorMode);
//        } else {
//            StorageManager.initOrSyncStorage(this, storagePath, isCheckFavorMode);
//        }
        startInitStorage(false, !curRecord.value!!.isTemp)
    }

    fun afterStorageLoaded() {
        if (isSaveTempAfterStorageLoaded) {
            isSaveTempAfterStorageLoaded = false
            // сохраняем временную запись
//            editFields(mResultObj)
            postEvent(RecordEvents.EditFields, resultObj)
        }
    }

    //endregion Storage

    //region Interaction

    /**
     * Открытие каталога записи.
     */
    fun openRecordFolder() {
        if (!recordsInteractor.openRecordFolder(getContext(), curRecord.value!!)) {
            log(R.string.log_missing_file_manager, true)
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
        interactionInteractor.shareText(getContext(), curRecord.value!!.name, text)
    }

    //endregion Interaction

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
        postViewEvent(Constants.ViewEvents.FinishWithResult, ActivityResult(Constants.RESULT_DELETE_RECORD, bundle))
    }

    private fun dropIsEdited() {
        isEdited = false
        postEvent(RecordEvents.IsEditedChanged, false)
    }

    fun editFields(obj: ResultObj?, name: String, tags: String, author: String, url: String, node: TetroidNode, isFavor: Boolean) {
        launch {
            val wasTemp = curRecord.value!!.isTemp
            if (recordsInteractor.editRecordFields(getContext(), curRecord.value, name, tags, author, url, node, isFavor)) {
                isFieldsEdited = true
                setTitle(name)
//                loadFields(mRecord)
                postEvent(RecordEvents.LoadFields, curRecord.value)
                if (wasTemp) {
                    // сохраняем текст записи
//                    save();
                    val resObj: ResultObj?
                    if (obj == null) {
                        resObj = ResultObj(null)
                        // baseUrl изменился, нужно перезагрузить в WebView
                        resObj.needReloadText = true
                    } else {
                        resObj = null
                    }
                    saveRecord(resObj)
                    //                    TetroidLog.logOperRes(LogObj.TEMP_RECORD, LogOper.SAVE);
                    // показываем кнопку Home для возврата в ветку записи
                    postViewEvent(Constants.ViewEvents.ShowHomeButton)
                } else {
//                    TetroidLog.logOperRes(LogObj.RECORD_FIELDS, LogOper.CHANGE);
                    log(R.string.log_record_fields_changed, true)
                }
            } else {
                if (wasTemp) {
                    /*// все равно сохраняем текст записи
//                    save();
                    if (obj == null) {
                        // baseUrl изменился, нужно перезагрузить в WebView
                        obj.needReloadText = true;
                    }
                    saveRecord(obj);*/
                    logOperErrorMore(LogObj.TEMP_RECORD, LogOper.SAVE)
                } else {
                    logOperErrorMore(LogObj.RECORD_FIELDS, LogOper.CHANGE)
                }
            }
        }
    }

    fun getUriToRecordFolder(record: TetroidRecord) = recordsInteractor.getUriToRecordFolder(record)

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
            val option = CommonSettings.getShowRecordFields(getContext())
            when (option) {
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

    fun getRecordEditedDate(record: TetroidRecord) = recordsInteractor.getEditedDate(getContext(), record)

    fun getRecordName() = curRecord.value?.name

    fun isRecordTemprorary() = curRecord.value?.isTemp ?: true

    fun isViewMode() = curMode == Constants.MODE_VIEW

    fun isEditMode() = curMode == Constants.MODE_EDIT

    fun isHtmlMode() = curMode == Constants.MODE_HTML

    fun setTitle(title: String) {
        setViewEvent(Constants.ViewEvents.UpdateTitle, title)
    }

    enum class RecordEvents {
        NeedMigration,
        LoadFields,
        EditFields,
        LoadRecordTextFromFile,
        LoadRecordTextFromHtml,
        AskForLoadAllNodes,
        FileAttached,
        SwitchViews,
        AskForSaving,
        BeforeSaving,
        IsEditedChanged,
        EditedDateChanged,
        StartLoadImages,
        StartCaptureCamera,
        InsertImages,
        InsertWebPageContent,
        InsertWebPageText,
        OpenWebLink,
        Save,
    }

}

class ResultObj {
    var type = 0
    var id: String? = null
    var obj: Any? = null
    var needReloadText = false

    internal constructor(type: Int) {
        init(type, null, null)
    }

    internal constructor(type: Int, id: String?) {
        // если есть id, значит obj использоваться не будет
        init(type, id, null)
    }

    internal constructor(obj: Any?) {
        this.obj = obj
        when (obj) {
            is TetroidRecord -> {
                type = OPEN_RECORD
                id = obj.id
            }
            is TetroidNode -> {
                type = OPEN_NODE
                id = obj.id
            }
            is TetroidFile -> {
                type = OPEN_FILE
                id = obj.id
            }
            is String -> {
                type = OPEN_TAG
            }
            else -> {
                type = NONE
            }
        }
    }

    private fun init(type: Int, id: String?, obj: Any?) {
        this.type = type
        this.id = id
        this.obj = obj
    }

    companion object {
        const val NONE = 0
        const val EXIT = 1
        const val START_MAIN_ACTIVITY = 2
        const val OPEN_RECORD = 3
        const val OPEN_NODE = 4
        const val OPEN_FILE = 5
        const val OPEN_TAG = 6
    }
}

class ActivityResult(
    val code: Int,
    val bundle: Bundle? = null,
    var intent: Intent? = null
)