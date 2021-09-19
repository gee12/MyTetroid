package com.gee12.mytetroid.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.esafirm.imagepicker.features.ImagePicker
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.SingleLiveEvent
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.helpers.NetworkHelper
import com.gee12.mytetroid.helpers.NetworkHelper.IWebImageResult
import com.gee12.mytetroid.helpers.NetworkHelper.IWebPageContentResult
import com.gee12.mytetroid.helpers.UriHelper
import com.gee12.mytetroid.interactors.ImagesInteractor
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.logs.TetroidLog
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.utils.Utils
import com.gee12.mytetroid.views.Message
import com.gee12.mytetroid.views.activities.MainActivity
import com.gee12.mytetroid.views.activities.TetroidActivity.IDownloadFileResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*

class RecordViewModel(
    app: Application,
    private val storagesRepo: StoragesRepo
): StorageViewModel/*<Constants.RecordEvents>*/(app, storagesRepo) {

    val objectAction: SingleLiveEvent<ViewModelEvent<Constants.RecordEvents, Any>> = SingleLiveEvent()

    fun doAction(action: Constants.RecordEvents, param: Any? = null) {
        objectAction.postValue(ViewModelEvent(action, param))
    }

    protected val imagesInteractor = ImagesInteractor(dataInteractor, recordsInteractor)

    var mRecord: TetroidRecord? = null
    var mCurMode = 0
    var mIsFirstLoad = true
    var mIsFieldsEdited = false
    var mIsReceivedImages = false
    var mModeToSwitch = -1
    var mIsSaveTempAfterStorageLoaded = false
    var mResultObj: ResultObj? = null
    var isEdited = false
    var isFromAnotherActivity = false

    fun onCreate(receivedIntent: Intent) {

        // проверяем передавались ли изображения
        mIsReceivedImages = receivedIntent.hasExtra(Constants.EXTRA_IMAGES_URI)

    }

    fun onGUICreated() {
        try {
            // загрузка записи выполняется в последнюю очередь (после создания пунктов меню)
            openRecord(mRecord)
        } catch (ex: Exception) {
            LogManager.log(getContext(), ex)
        }
    }

    // region LoadPage

    /**
     * Событие окончания загрузки страницы.
     */
    fun onPageLoaded() {
        if (mIsFirstLoad) {
            mIsFirstLoad = false
            // переключаем режим отображения
            val defMode = if (
                mRecord!!.isNew
                || mIsReceivedImages
                || SettingsManager.isRecordEditMode(getContext())
            ) Constants.MODE_EDIT else Constants.MODE_VIEW

            // сбрасываем флаг, т.к. уже воспользовались
            mRecord!!.setIsNew(false)
            switchMode(defMode)

            /*if (defMode == MODE_EDIT) {
                ViewUtils.showKeyboard(this, mEditor.getWebView(), false);
//                Keyboard.showKeyboard(mEditor);
            }*/
        } else {
            // переключаем только views
//            switchViews(mCurMode)
            doAction(Constants.RecordEvents.SwitchViews, mCurMode)
        }
    }

    fun onEditorJSLoaded(receivedIntent: Intent) {
        // вставляем переданные изображения
        if (mIsReceivedImages) {
            val uris = receivedIntent.getParcelableArrayListExtra<Uri>(Constants.EXTRA_IMAGES_URI)
            saveImages(uris, false)
        }
    }

    fun onHtmlRequestHandled() {
        // теперь сохраняем текст заметки без вызова предварительных методов
        saveRecord(false, mResultObj)
        // переключаем режим, если асинхронное сохранение было вызвано в процессе переключения режима
        if (mModeToSwitch > 0) {
            switchMode(mModeToSwitch, false)
        }
    }

    // endregion LoadPage

    // region LoadLinks

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
        var obj: TetroidObject
        if (TetroidObject.parseUrl(url).also { obj = it } != null) {
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
                            LogManager.log(getContext(), getString(R.string.log_not_found_record) + obj.id, ILogger.Types.WARNING, Toast.LENGTH_LONG)
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
                            LogManager.log(getContext(), getString(R.string.log_not_found_node_id) + obj.id, ILogger.Types.WARNING, Toast.LENGTH_LONG)
                        }
                    }
                FoundType.TYPE_TAG -> {
                    val tag = obj.id
                    if (!TextUtils.isEmpty(tag)) {
                        openTag(tag, true)
                    } else {
                        LogManager.log(getContext(), getString(R.string.title_tag_name_is_empty), ILogger.Types.WARNING, Toast.LENGTH_LONG)
                    }
                }
                FoundType.TYPE_AUTHOR, FoundType.TYPE_FILE -> {
                }
                else -> LogManager.log(getContext(), getString(R.string.log_link_to_obj_parsing_error), ILogger.Types.WARNING, Toast.LENGTH_LONG)
            }
        } else {
            // обрабатываем внешнюю ссылку
            doAction(Constants.RecordEvents.OpenWebLink, url)
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
            LogManager.log(getContext(), getString(R.string.log_url_decode_error) + url, ex)
            return
        }
        if (decodedUrl.startsWith(TetroidTag.LINKS_PREFIX)) {
            // избавляемся от приставки "tag:"
            val tagName = decodedUrl.substring(TetroidTag.LINKS_PREFIX.length)
            openTag(tagName, true)
        } else {
            LogManager.log(getContext(), getString(R.string.log_wrong_tag_link_format), ILogger.Types.WARNING, Toast.LENGTH_LONG)
        }
    }

    //endregion LoadLinks

    //region OpenRecord

    /**
     * Получение записи из хранилища, т.к. актиность была запущена из MainActivity.
     * @param intent
     * @return
     */
    fun initRecordFromMain(intent: Intent) {
        // получаем переданную запись
        val recordId = intent.getStringExtra(Constants.EXTRA_OBJECT_ID)
        if (recordId != null) {
            // получаем запись
            mRecord = recordsInteractor.getRecord(recordId)
            if (mRecord == null) {
                LogManager.log(getContext(), getString(R.string.log_not_found_record) + recordId, ILogger.Types.ERROR, Toast.LENGTH_LONG)
                updateViewState(Constants.ViewEvents.FinishActivity)
            } else {
                setTitle(mRecord!!.name)
                //                setVisibilityActionHome(!mRecord.isTemp());
                if (intent.hasExtra(Constants.EXTRA_ATTACHED_FILES)) {
                    // временная запись создана для прикрепления файлов
                    val filesCount = mRecord!!.attachedFilesCount
                    if (filesCount > 0) {
                        val mes = if (filesCount == 1) java.lang.String.format(
                            getString(R.string.mes_attached_file_mask),
                            mRecord!!.attachedFiles[0].name
                        ) else String.format(getString(R.string.mes_attached_files_mask), filesCount)
                        Message.show(getContext(), mes, Toast.LENGTH_LONG)
                    }
                }
            }
        } else {
            LogManager.log(getContext(), getString(R.string.log_not_transferred_record_id), ILogger.Types.ERROR, Toast.LENGTH_LONG)
            updateViewState(Constants.ViewEvents.FinishActivity)
        }
    }

    /**
     * Создание новой временной записи, т.к. активность была запущена из виджета AddRecordWidget.
     * @return
     */
    fun initRecordFromWidget() {
        updateViewState(Constants.ViewEvents.ShowHomeButton)
        viewModelScope.launch {
            // создаем временную запись
            val node = if (quicklyNode != null) quicklyNode!! else TetroidXml.ROOT_NODE
            mRecord = recordsInteractor.createTempRecord(getContext(), null, null, null, node)
            if (mRecord != null) {
                setTitle(mRecord!!.name)
            } else {
                TetroidLog.logOperError(getContext(), TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE, Toast.LENGTH_LONG)
                updateViewState(Constants.ViewEvents.FinishActivity)
            }
        }
    }

    /**
     * Отображение записи (свойств и текста).
     * @param record
     */
    fun openRecord(record: TetroidRecord?) {
        if (record == null) return
        mRecord = record
        LogManager.log(getContext(), getString(R.string.log_record_loading) + record.id, ILogger.Types.INFO)
        doAction(Constants.RecordEvents.LoadFields, record)
        // FIXME: сделал вызов после обработки LoadFields. Проверить
//        expandFieldsIfNeed()
        // текст
        loadRecordTextFromFile(record)
    }

    fun loadRecordTextFromFile(record: TetroidRecord) {
        if (!record.isNew) {
            viewModelScope.launch {
                val text = recordsInteractor.getRecordHtmlTextDecrypted(getContext(), record, Toast.LENGTH_LONG)
                if (text != null) {
                    doAction(Constants.RecordEvents.LoadRecordTextFromFile, text)
                } else {
                    if (record.isCrypted && cryptInteractor.crypter.errorCode > 0) {
                        LogManager.log(getContext(), R.string.log_error_record_file_decrypting, ILogger.Types.ERROR, Toast.LENGTH_LONG)
                        updateViewState(Constants.ViewEvents.ShowMoreInLogs)
                    }
                }
            }
        }
    }

    //endregion OpenRecord

    //region OpenAnotherObjects

    /**
     * Открытие другой записи по внутренней ссылке.
     * @param resObj
     * @param isAskForSave
     */
    fun openAnotherRecord(resObj: ResultObj?, isAskForSave: Boolean) {
        if (resObj == null) return
        if (onSaveRecord(isAskForSave, resObj)) return
        if (isLoadedFavoritesOnly()) {
//            AskDialogs.showLoadAllNodesDialog(this,
//                IApplyResult { showAnotherRecord(resObj.id) })
            doAction(Constants.RecordEvents.AskForLoadAllNodes, resObj)
        } else {
            showAnotherRecord(resObj.id)
        }
    }

    fun showAnotherRecord(id: String?) {
        val bundle = Bundle()
        bundle.putString(Constants.EXTRA_OBJECT_ID, id)
        if (mIsFieldsEdited) {
            bundle.putBoolean(Constants.EXTRA_IS_FIELDS_EDITED, true)
        }
//        finishWithResult(RESULT_OPEN_RECORD, bundle)
        updateViewState(Constants.ViewEvents.FinishWithResult, ActivityResult(Constants.RESULT_OPEN_RECORD, bundle))
    }

    /**
     * Открытие другой ветки по внутренней ссылке.
     * @param resObj
     * @param isAskForSave
     */
    fun openAnotherNode(resObj: ResultObj?, isAskForSave: Boolean) {
        if (resObj == null) return
        if (onSaveRecord(isAskForSave, ResultObj(resObj))) return
        if (isLoadedFavoritesOnly()) {
//            AskDialogs.showLoadAllNodesDialog(this,
//                IApplyResult { showAnotherNodeDirectly(resObj.id) })
            doAction(Constants.RecordEvents.AskForLoadAllNodes, resObj)
        } else {
            showAnotherNodeDirectly(resObj.id)
        }
    }

    fun showAnotherNodeDirectly(id: String?) {
        val bundle = Bundle()
        bundle.putString(Constants.EXTRA_OBJECT_ID, id)
//        finishWithResult(RESULT_OPEN_NODE, bundle)
        updateViewState(Constants.ViewEvents.FinishWithResult, ActivityResult(Constants.RESULT_OPEN_NODE, bundle))
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
            doAction(Constants.RecordEvents.AskForLoadAllNodes, ResultObj(ResultObj.OPEN_TAG, tagName))
        } else {
            openTagDirectly(tagName)
        }
    }

    fun openTagDirectly(tagName: String?) {
        val bundle = Bundle()
        bundle.putString(Constants.EXTRA_TAG_NAME, tagName)
        if (mIsFieldsEdited) {
            bundle.putBoolean(Constants.EXTRA_IS_FIELDS_EDITED, true)
        }
//        finishWithResult(RESULT_SHOW_TAG, bundle)
        updateViewState(Constants.ViewEvents.FinishWithResult, ActivityResult(Constants.RESULT_SHOW_TAG, bundle))
    }

    /**
     * Открытие ветки записи.
     */
    fun showRecordNode() {
        openAnotherNode(ResultObj(mRecord!!.node), true)
    }

    //endregion OpenAnotherObjects

    //region Image

    /**
     * Обработка выбранных изображений.
     * @param data
     */
    fun saveSelectedImages(data: Intent, isCamera: Boolean) {
        val images = ImagePicker.getImages(data)
        if (images == null) {
            LogManager.log(getContext(), getString(R.string.log_selected_files_is_missing), ILogger.Types.ERROR, Toast.LENGTH_LONG)
            return
        }
        val uris: MutableList<Uri> = ArrayList()
        for (image in images) {
            uris.add(Uri.fromFile(File(image.path)))
        }
        saveImages(uris, isCamera)
    }

    fun saveImages(imageUris: List<Uri>?, deleteSrcFile: Boolean) {
        if (imageUris == null) return
        var errorCount = 0
        val savedImages: MutableList<TetroidImage> = ArrayList()
        for (uri in imageUris) {
            val savedImage = imagesInteractor.saveImage(getContext(), mRecord, uri, deleteSrcFile)
            if (savedImage != null) {
                savedImages.add(savedImage)
            } else {
                errorCount++
            }
        }
        if (errorCount > 0) {
            LogManager.log(
                getContext(), String.format(getString(R.string.log_failed_to_save_images_mask), errorCount),
                ILogger.Types.WARNING, Toast.LENGTH_LONG
            )
            updateViewState(Constants.ViewEvents.ShowMoreInLogs)
        }
        if (!savedImages.isEmpty()) {
//            mEditor.insertImages(savedImages)
            doAction(Constants.RecordEvents.InsertImages, savedImages)
        }
    }

    fun saveImage(imageUri: Uri?, deleteSrcFile: Boolean) {
        val savedImage = imagesInteractor.saveImage(getContext(), mRecord, imageUri, deleteSrcFile)
        saveImage(savedImage)
    }

    fun saveImage(bitmap: Bitmap?) {
        val savedImage = imagesInteractor.saveImage(getContext(), mRecord, bitmap)
        saveImage(savedImage)
    }

    fun saveImage(image: TetroidImage?) {
        if (image != null) {
//            mEditor.insertImage(image)
            doAction(Constants.RecordEvents.InsertImages, listOf(image))
        } else {
            TetroidLog.logOperError(getContext(), TetroidLog.Objs.IMAGE, TetroidLog.Opers.SAVE, Toast.LENGTH_LONG)
            updateViewState(Constants.ViewEvents.ShowMoreInLogs)
        }
    }

    fun downloadWebPageContent(url: String?, isTextOnly: Boolean) {
        viewModelScope.launch {
            updateViewState(Constants.ViewEvents.ShowProgress, getString(R.string.title_page_downloading))
            NetworkHelper.downloadWebPageContentAsync(url, isTextOnly, object : IWebPageContentResult {
                override fun onSuccess(content: String?, isTextOnly: Boolean) {
//                    mEditor.insertWebPageContent(content, isTextOnly)
                    if (isTextOnly) doAction(Constants.RecordEvents.InsertWebPageText)
                    else doAction(Constants.RecordEvents.InsertWebPageContent)
                    updateViewState(Constants.ViewEvents.ShowProgress, false)
                }

                override fun onError(ex: java.lang.Exception) {
                    TetroidLog.log(getContext(), getString(R.string.log_error_download_web_page_mask, ex.message!!), Toast.LENGTH_LONG)
                    updateViewState(Constants.ViewEvents.ShowProgress, false)
                }
            })
        }
    }

    fun downloadImage(url: String?) {
        viewModelScope.launch {
            updateViewState(Constants.ViewEvents.ShowProgress, getString(R.string.title_image_downloading))
            NetworkHelper.downloadImageAsync(url, object : IWebImageResult {
                override fun onSuccess(bitmap: Bitmap?) {
                    saveImage(bitmap)
                    updateViewState(Constants.ViewEvents.ShowProgress, false)
                }

                override fun onError(ex: java.lang.Exception) {
                    TetroidLog.log(getContext(), getString(R.string.log_error_download_image_mask, ex.message!!), Toast.LENGTH_LONG)
                    updateViewState(Constants.ViewEvents.ShowProgress, false)
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
        val uriHelper = UriHelper(getContext())
//        AttachFileFromRecordTask(mRecord, deleteSrcFile).run(uriHelper.getPath(uri))
        attachFile(uriHelper.getPath(uri), mRecord, deleteSrcFile)
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

    fun attachFile(fileFullName: String?, record: TetroidRecord?, deleteSrcFile: Boolean) {
        updateViewState(Constants.ViewEvents.TaskStarted, R.string.task_attach_file)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val attach = attachesInteractor.attachFile(getContext(), fileFullName, record, deleteSrcFile)
                updateViewState(Constants.ViewEvents.TaskFinished)
                if (attach != null) {
                    TetroidLog.log(getContext(), getString(R.string.log_file_was_attached), ILogger.Types.INFO, Toast.LENGTH_SHORT)
                    doAction(Constants.RecordEvents.FileAttached, attach)
                } else {
                    TetroidLog.log(getContext(), getString(R.string.log_files_attach_error), ILogger.Types.ERROR, Toast.LENGTH_LONG)
                    updateViewState(Constants.ViewEvents.ShowMoreInLogs)
                }
            }
        }
    }

    fun downloadAndAttachFile(uri: Uri) {
        viewModelScope.launch {
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
        updateViewState(Constants.ViewEvents.FinishWithResult, ActivityResult(Constants.RESULT_SHOW_ATTACHES, bundle))
    }

    /**
     * Открытие списка прикрепленных файлов записи.
     */
    fun openRecordAttaches() {
        openRecordAttaches(mRecord, true)
    }

    //endregion Attach

    //region Mode

    /**
     * Переключение режима отображения содержимого записи.
     * @param newMode
     */
    fun switchMode(newMode: Int) {
        switchMode(newMode, true)
    }

    fun switchMode(newMode: Int, isNeedSave: Boolean) {
        mModeToSwitch = -1
        val oldMode = mCurMode
        // сохраняем
//        onSaveRecord();
        var runBeforeSaving = false
        if (isNeedSave
            && SettingsManager.isRecordAutoSave(getContext())
            && !mRecord!!.isTemp) {
            // автоматически сохраняем текст записи, если:
            //  * есть изменения
            //  * не находимся в режиме HTML (сначала нужно перейти в режим EDIT (WebView), а уже потом можно сохранять)
            //  * запись не временная
            if (isEdited && mCurMode != Constants.MODE_HTML) {
                runBeforeSaving = saveRecord(null)
            }
        }
        // если асинхронно запущена предобработка сохранения, то выходим
        if (runBeforeSaving) {
            mModeToSwitch = newMode
            return
        }
        // перезагружаем html-текст записи в webView, если был режим редактирования HTML
        if (oldMode == Constants.MODE_HTML) {
//            loadRecordText(mRecord, true)
            doAction(Constants.RecordEvents.LoadRecordTextFromHtml)
        } else {
//            switchViews(newMode)
            doAction(Constants.RecordEvents.SwitchViews, newMode)
        }
        mCurMode = newMode
//        updateOptionsMenu()
        updateViewState(Constants.ViewEvents.UpdateOptionsMenu)
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
        // TODO: слушать изменение у WysiwygEditor
        if (isEdited) {
            if (SettingsManager.isRecordAutoSave(getContext()) && !mRecord!!.isTemp) {
                // сохраняем без запроса
                return saveRecord(obj)
            } else if (showAskDialog) {
                doAction(Constants.RecordEvents.AskForSaving, obj)
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
        val runBeforeSaving = SettingsManager.isFixEmptyParagraphs(getContext())
        if (runBeforeSaving) {
            mResultObj = obj
        }
        return saveRecord(runBeforeSaving, obj) || runBeforeSaving
    }

    /**
     * Сохранение html-текста записи в файл с предобработкой.
     * @param callBefore Нужно ли перед сохранением совершить какие-либы манипуляции с html ?
     * @return true - запущен ли код в асинхронном режиме.
     */
    fun saveRecord(callBefore: Boolean, obj: ResultObj?): Boolean {
        if (callBefore) {
//            onBeforeSavingAsync()
            doAction(Constants.RecordEvents.BeforeSaving)
            return true
        } else {
            if (mRecord!!.isTemp) {
                if (isLoaded()) {
//                    this.runOnUiThread(Runnable { editFields(obj) })
                    doAction(Constants.RecordEvents.EditFields, obj)
                } else {
                    mIsSaveTempAfterStorageLoaded = true
//                    this.runOnUiThread(Runnable { loadStorage() })
                    loadStorage()
                }
                return true
            } else {
                save()
//                this.runOnUiThread(Runnable { onAfterSaving(obj) })
                onAfterSaving(obj)
            }
        }
        return false
    }

    private fun save() {
        // запрашиваем у View html-текст записи и сохраняем
        doAction(Constants.RecordEvents.Save)
    }

    /**
     * Получение актуального html-текста записи из WebView и непосредственное сохранение в файл.
     */
    fun save(htmlText: String) {
        LogManager.log(getContext(), getString(R.string.log_before_record_save) + mRecord!!.id, ILogger.Types.INFO)
//        val htmlText: String = mEditor.getDocumentHtml()
        //        String htmlText = (mCurMode == MODE_HTML)
//                ? TetroidEditor.getDocumentHtml(mEditTextHtml.getText().toString()) : mEditor.getDocumentHtml();
        if (recordsInteractor.saveRecordHtmlText(getContext(), mRecord!!, htmlText)) {
//            TetroidLog.logOperRes(TetroidLog.Objs.RECORD, TetroidLog.Opers.SAVE);
            LogManager.log(getContext(), R.string.log_record_saved, ILogger.Types.INFO, Toast.LENGTH_SHORT)
            // сбрасываем пометку изменения записи
            dropIsEdited();
            updateEditedDate()
        } else {
            TetroidLog.logOperErrorMore(getContext(), TetroidLog.Objs.RECORD, TetroidLog.Opers.SAVE)
        }
    }

    /**
     * Обновление поля последнего изменения записи.
     */
    private fun updateEditedDate() {
        if (App.isFullVersion()) {
            val dateFormat = getString(R.string.full_date_format_string)
            val edited = recordsInteractor.getEditedDate(getContext(), mRecord!!)
//            (findViewById<View>(R.id.text_view_record_edited) as TextView).text =
            val editedDate = if (edited != null) Utils.dateToString(edited, dateFormat) else ""
            doAction(Constants.RecordEvents.EditedDateChanged, editedDate)
        }
    }

    /**
     * Обработчик события после сохранения записи, вызванное при ответе на запрос сохранения в диалоге.
     * @param resObj
     */
    fun onAfterSaving(resObj: ResultObj?) {
        var resObj = resObj
        if (resObj == null) {
            resObj = ResultObj(null)
        }
        when (resObj.type) {
            ResultObj.EXIT,
            ResultObj.START_MAIN_ACTIVITY ->
                if (!onRecordFieldsIsEdited(resObj.type == ResultObj.START_MAIN_ACTIVITY)) {
                    updateViewState(Constants.ViewEvents.FinishActivity)
                }
            ResultObj.OPEN_RECORD -> openAnotherRecord(resObj, false)
            ResultObj.OPEN_NODE -> openAnotherNode(resObj, false)
            ResultObj.OPEN_FILE -> openRecordAttaches(mRecord, false)
            ResultObj.OPEN_TAG -> openTag((resObj.obj as String?)!!, false)
            ResultObj.NONE -> if (resObj.needReloadText) {
                // перезагружаем baseUrl в WebView
//                loadRecordText(mRecord, false)
                loadRecordTextFromFile(mRecord!!)
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
        if (mIsFieldsEdited) {
//                ActivityCompat.getReferrer()
            if (isFromAnotherActivity) {
                // закрываем активность, возвращая результат:
                // указываем родительской активности, что нужно обновить список записей
                val intent = Intent()
                intent.putExtra(Constants.EXTRA_IS_FIELDS_EDITED, true)
                //            intent.setAction(ACTION_ADD_RECORD);
//                setResult(Activity.RESULT_OK, intent)
                updateViewState(Constants.ViewEvents.SetActivityResult, ActivityResult(Activity.RESULT_OK, intent = intent))
            } else if (startMainActivity) {
                // запускаем главную активность, помещая результат
                //                bundle.putString(EXTRA_OBJECT_ID, mRecord.getId());
                if (mRecord!!.node != null) {

//                    finish()
                    updateViewState(Constants.ViewEvents.FinishActivity)
                } else {
                    Message.show(getContext(), getString(R.string.log_record_node_is_empty), Toast.LENGTH_LONG)
                }
                return true
            } else {
//                finish()
                updateViewState(Constants.ViewEvents.FinishActivity)
                return true
            }
        }
        return false
    }

    fun openRecordNodeInMainView() {
        val bundle = Bundle().apply {
            putInt(Constants.EXTRA_RESULT_CODE, Constants.RESULT_OPEN_NODE)
            putString(Constants.EXTRA_OBJECT_ID, mRecord!!.node.id)
        }
        val intent = Intent(getContext(), MainActivity::class.java).apply {
            putExtras(bundle)
            action = Constants.ACTION_RECORD
        }
//        ViewUtils.startActivity(
//            this,
//            MainActivity::class.java, bundle, Constants.ACTION_RECORD, 0, null
//        )
        updateViewState(Constants.ViewEvents.StartActivity, intent)
    }

    //endregion SaveRecord

    //region Storage

//    /**
//     *
//     */
//    @Override
//    protected void createStorage(String storagePath) {
//        boolean res = StorageManager.createStorage(this, storagePath);
//        initGUI(res && DataManager.createDefault(this), false, false);
//    }

    // endregion SaveRecord
    // region Storage
    //    /**
    //     *
    //     */
    //    @Override
    //    protected void createStorage(String storagePath) {
    //        boolean res = StorageManager.createStorage(this, storagePath);
    //        initGUI(res && DataManager.createDefault(this), false, false);
    //    }

    /**
     * Старт загрузки хранилища.
     */
    //    @Override
    //    protected void loadStorage(String storagePath) {
    fun loadStorage() {
//        boolean isLoadLastForced = false;
//        boolean isCheckFavorMode = !mRecord.isTemp();
//        if (storagePath == null) {
//            StorageManager.startInitStorage(this, this, isLoadLastForced, isCheckFavorMode);
//        } else {
//            StorageManager.initOrSyncStorage(this, storagePath, isCheckFavorMode);
//        }
        startInitStorage(false, !mRecord!!.isTemp)
    }

    fun afterStorageLoaded() {
        if (mIsSaveTempAfterStorageLoaded) {
            mIsSaveTempAfterStorageLoaded = false
            // сохраняем временную запись
//            editFields(mResultObj)
            doAction(Constants.RecordEvents.EditFields, mResultObj)
        }
    }

    //endregion Storage

    //region Interaction

    /**
     * Открытие каталога записи.
     */
    fun openRecordFolder() {
        if (!recordsInteractor.openRecordFolder(getContext(), mRecord!!)) {
            LogManager.log(getContext(), R.string.log_missing_file_manager, Toast.LENGTH_LONG)
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
        interactionInteractor.shareText(getContext(), mRecord!!.name, text)
    }

    //endregion Interaction

    /**
     * Сохранение записи при любом скрытии активности.
     */
    fun onPause() {
        if (!mRecord!!.isTemp) {
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
        bundle.putString(Constants.EXTRA_OBJECT_ID, mRecord!!.id)
        updateViewState(Constants.ViewEvents.FinishWithResult, ActivityResult(Constants.RESULT_DELETE_RECORD, bundle))
    }

    fun dropIsEdited() {
        isEdited = false
        doAction(Constants.RecordEvents.IsEditedChanged, false)
    }

    fun editFields(obj: ResultObj?, name: String, tags: String, author: String, url: String, node: TetroidNode, isFavor: Boolean) {
        viewModelScope.launch {
            val wasTemp = mRecord!!.isTemp
            if (recordsInteractor.editRecordFields(getContext(), mRecord, name, tags, author, url, node, isFavor)) {
                mIsFieldsEdited = true
                setTitle(name)
//                loadFields(mRecord)
                doAction(Constants.RecordEvents.LoadFields, mRecord)
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
                    //                    TetroidLog.logOperRes(TetroidLog.Objs.TEMP_RECORD, TetroidLog.Opers.SAVE);
                    // показываем кнопку Home для возврата в ветку записи
                    updateViewState(Constants.ViewEvents.ShowHomeButton)
                } else {
//                    TetroidLog.logOperRes(TetroidLog.Objs.RECORD_FIELDS, TetroidLog.Opers.CHANGE);
                    LogManager.log(getContext(), R.string.log_record_fields_changed, ILogger.Types.INFO, Toast.LENGTH_SHORT)
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
                    TetroidLog.logOperErrorMore(getContext(), TetroidLog.Objs.TEMP_RECORD, TetroidLog.Opers.SAVE)
                } else {
                    TetroidLog.logOperErrorMore(getContext(), TetroidLog.Objs.RECORD_FIELDS, TetroidLog.Opers.CHANGE)
                }
            }
        }
    }

    /**
     * Отображение или скрытие панели свойств в зависимости от настроек.
     */
    fun isNeedExpandFields(): Boolean {
        return if (
            mRecord!!.isNew
            || mIsReceivedImages
            || SettingsManager.isRecordEditMode(getContext())
        ) {
            false
        } else {
            val option = SettingsManager.getShowRecordFields(getContext())
            when (option) {
                getString(R.string.pref_show_record_fields_no) -> false
                getString(R.string.pref_show_record_fields_yes) -> true
                else -> {
                    mRecord != null
                            && (!TextUtils.isEmpty(mRecord!!.tagsString)
                                || !TextUtils.isEmpty(mRecord!!.author)
                                || !TextUtils.isEmpty(mRecord!!.url))
                }
            }
        }
    }

    fun getUriToRecordFolder(record: TetroidRecord) = recordsInteractor.getUriToRecordFolder(getContext(), record)

    fun getRecordEditedDate(record: TetroidRecord) = recordsInteractor.getEditedDate(getContext(), record)

    fun getRecordName() = mRecord?.name

    fun isRecordTemprorary() = mRecord?.isTemp ?: true

    fun isViewMode() = mCurMode == Constants.MODE_VIEW

    fun isEditMode() = mCurMode == Constants.MODE_EDIT

    fun isHtmlMode() = mCurMode == Constants.MODE_HTML

    fun setTitle(title: String) {
        updateViewState(Constants.ViewEvents.UpdateTitle, title)
    }
}

/**
 *
 */
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