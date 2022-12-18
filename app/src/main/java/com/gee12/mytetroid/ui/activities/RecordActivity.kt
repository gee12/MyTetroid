package com.gee12.mytetroid.ui.activities

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.gee12.htmlwysiwygeditor.IImagePicker
import com.gee12.htmlwysiwygeditor.INetworkWorker
import com.gee12.mytetroid.App.checkKeepScreenOn
import com.gee12.mytetroid.R
import com.gee12.mytetroid.TetroidSuggestionProvider
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.focusAndShowKeyboard
import com.gee12.mytetroid.common.extensions.hideKeyboard
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.common.utils.ViewUtils
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.helpers.HtmlHelper
import com.gee12.mytetroid.helpers.TetroidClipboardListener
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.ui.SearchViewXListener
import com.gee12.mytetroid.ui.TetroidImagePicker
import com.gee12.mytetroid.ui.activities.StorageInfoActivity.Companion.start
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.dialogs.record.RecordFieldsDialog
import com.gee12.mytetroid.ui.dialogs.record.RecordInfoDialog
import com.gee12.mytetroid.ui.dialogs.storage.StorageDialogs
import com.gee12.mytetroid.ui.views.TetroidEditText
import com.gee12.mytetroid.ui.views.TetroidEditText.ITetroidEditTextListener
import com.gee12.mytetroid.ui.views.TetroidEditor
import com.gee12.mytetroid.ui.views.TetroidEditor.IEditorListener
import com.gee12.mytetroid.viewmodels.BaseEvent
import com.gee12.mytetroid.viewmodels.RecordViewModel
import com.gee12.mytetroid.viewmodels.RecordViewModel.RecordEvent
import com.gee12.mytetroid.viewmodels.ResultObj
import com.gee12.mytetroid.viewmodels.StorageViewModel.StorageEvent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.lumyjuwon.richwysiwygeditor.IColorPicker
import com.lumyjuwon.richwysiwygeditor.RichEditor.EditableWebView.*
import com.lumyjuwon.richwysiwygeditor.WysiwygEditor
import kotlinx.coroutines.launch
import net.cachapa.expandablelayout.ExpandableLayout

/**
 * Активность просмотра и редактирования содержимого записи.
 */
class RecordActivity : TetroidStorageActivity<RecordViewModel>(),
    IPageLoadListener, 
    ILinkLoadListener, 
    IHtmlReceiveListener, 
    IYoutubeLinkLoadListener, 
    IImagePicker,
    IColorPicker, 
    INetworkWorker, 
    ColorPickerDialogListener, 
    IEditorListener {

    private lateinit var mFieldsExpanderLayout: ExpandableLayout
    private lateinit var mButtonToggleFields: FloatingActionButton
    private lateinit var mButtonFullscreen: FloatingActionButton
    private lateinit var mTextViewTags: TextView
    private lateinit var mScrollViewHtml: ScrollView
    private lateinit var mEditTextHtml: TetroidEditText
    protected lateinit var mHtmlProgressBar: ProgressBar
    lateinit var editor: TetroidEditor
        private set
    private lateinit var mButtonScrollTop: FloatingActionButton
    private lateinit var mButtonFindNext: FloatingActionButton
    private lateinit var mButtonFindPrev: FloatingActionButton
    private lateinit var mFindListener: TextFindListener
    private lateinit var mSearchView: SearchView
    private val imagePicker = TetroidImagePicker(this) { uris: List<Uri>, isCameraMode: Boolean ->
        saveSelectedImages(uris, isCameraMode)
    }

    override fun getLayoutResourceId() = R.layout.activity_record

    override fun getViewModelClazz() = RecordViewModel::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (receivedIntent == null || receivedIntent!!.action == null) {
            finish()
            return
        }
        startInitStorage()
        viewModel.onCreate(receivedIntent!!)
        editor = findViewById(R.id.html_editor)
        editor.setColorPickerListener(this)
        editor.setImagePickerListener(this)
        editor.setNetworkWorkerListener(this)
        editor.setToolBarVisibility(false)
        //        mEditor.setOnTouchListener(this);
        editor.setOnPageLoadListener(this)
        editor.setEditorListener(this)
        val webView = editor.getWebView()
        webView.setOnTouchListener(this)
        webView.setOnUrlLoadListener(this)
        webView.setOnHtmlReceiveListener(this)
        webView.setYoutubeLoadLinkListener(this)
        webView.setClipboardListener(TetroidClipboardListener(this))
        mTextViewTags = findViewById(R.id.text_view_record_tags)
        mTextViewTags.movementMethod = LinkMovementMethod.getInstance()
        mFieldsExpanderLayout = findViewById(R.id.layout_fields_expander)
//        ToggleButton tbRecordFieldsExpander = findViewById(R.id.toggle_button_expander);
//        tbRecordFieldsExpander.setOnCheckedChangeListener((buttonView, isChecked) -> mFieldsExpanderLayout.toggle());
        mButtonToggleFields = findViewById(R.id.button_toggle_fields)
        mButtonToggleFields.setOnClickListener {
            toggleRecordFieldsVisibility()
        }
        setRecordFieldsVisibility(false)
//        ViewUtils.setOnGlobalLayoutListener(mButtonToggleFields, () -> updateScrollButtonLocation());
//        this.mButtonFieldsEdit = findViewById(R.id.button_edit_fields);
//        mButtonFieldsEdit.setOnClickListener(v -> editFields());
        mButtonFullscreen = findViewById(R.id.button_fullscreen)
        mButtonFullscreen.setOnClickListener {
            toggleFullscreen(false)
        }
        val mButtonScrollBottom = findViewById<FloatingActionButton>(R.id.button_scroll_bottom)
        mButtonScrollTop = findViewById(R.id.button_scroll_top)
        // прокидывание кнопок пролистывания в редактор
        editor.initScrollButtons(mButtonScrollBottom, mButtonScrollTop)
        mButtonFindNext = findViewById(R.id.button_find_next)
        mButtonFindNext.setOnClickListener {
            findNext()
        }
        mButtonFindPrev = findViewById(R.id.button_find_prev)
        mButtonFindPrev.setOnClickListener {
            findPrev()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mFindListener = TextFindListener()
            editor.getWebView().setFindListener(mFindListener)
        } else {
            // TODO: что здесь для API=15 ?
        }
        mScrollViewHtml = findViewById(R.id.scroll_html)
        mEditTextHtml = findViewById(R.id.edit_text_html)
        //        mEditTextHtml.setOnTouchListener(this); // работает криво
        mEditTextHtml.setTetroidListener(HtmlEditTextEditTextListener())
        mEditTextHtml.initSearcher(mScrollViewHtml)
        mHtmlProgressBar = findViewById(R.id.progress_bar)

        // не гасим экран, если установлена опция
        checkKeepScreenOn(this)
        afterOnCreate()
    }

    override fun createDependencyScope() {
        scopeSource = ScopeSource.current
    }

    override fun initViewModel() {
        super.initViewModel()

        viewModel.curRecord.observe(this) { record ->
            viewModel.openRecord(record)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // TODO: если открывали активити из виджета, то закрываем главный scope
        //ScopeContainer.current.scope.close()
    }

    /**
     * Обработчик событий UI.
     */
    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is RecordEvent -> {
                onRecordEvent(event)
            }
            is BaseEvent.StartActivity -> startActivity(event.intent)
            is BaseEvent.SetActivityResult -> setResult(event.code, event.intent)
            BaseEvent.FinishActivity -> finish()
            is BaseEvent.FinishWithResult -> finishWithResult(event.code, event.bundle)
            BaseEvent.PermissionCheck,
            is BaseEvent.PermissionGranted ->
                // загружаем параметры хранилища только после проверки разрешения на запись во внешнюю память
                startInitStorage()
            is BaseEvent.PermissionCanceled ->
                // закрываем активити, если нет разрешения на запись во внешнюю память
                // TODO: поведение потребуется изменить, если хранилище загружается только на чтение
                finish()
            is BaseEvent.UpdateTitle -> setTitle(event.title)
            BaseEvent.UpdateOptionsMenu -> updateOptionsMenu()
            is BaseEvent.ShowHomeButton -> setVisibilityActionHome(event.isVisible)
            is BaseEvent.TaskStarted -> taskPreExecute(event.titleResId ?: R.string.task_wait)
            BaseEvent.TaskFinished -> taskPostExecute()
            else -> super.onBaseEvent(event)
        }
    }

    /**
     * Обработчик событий хранилища.
     */
    override fun onStorageEvent(event: StorageEvent) {
        when (event) {
            is StorageEvent.LoadOrDecrypt -> viewModel.loadOrDecryptStorage(event.params)
            else -> super.onStorageEvent(event)
        }
    }

    /**
     * Обработчик событий в окне записи.
     */
    private fun onRecordEvent(event: RecordEvent) {
        when (event) {
            RecordEvent.NeedMigration -> showNeedMigrationDialog()
            RecordEvent.Save -> saveRecord()
            is RecordEvent.LoadFields -> {
                loadFields(event.record)
                expandFieldsIfNeed()
            }
            is RecordEvent.EditFields -> editFields(event.resultObj)
            is RecordEvent.LoadRecordTextFromFile -> loadRecordText(event.recordText)
            RecordEvent.LoadRecordTextFromHtml -> loadRecordTextFromHtmlEditor()
            is RecordEvent.AskForLoadAllNodes -> {
                AskDialogs.showYesDialog(
                    context = this,
                    messageResId = R.string.ask_load_all_nodes_dialog_title,
                    onApply = {
                        val obj = event.resultObj
                        when (obj.type) {
                            ResultObj.OPEN_RECORD -> viewModel.showAnotherRecord(obj.id)
                            ResultObj.OPEN_NODE -> viewModel.showAnotherNodeDirectly(obj.id)
                            ResultObj.OPEN_TAG ->
                                viewModel.openTagDirectly(obj.id) // id - это tagName
                        }
                    },
                )
            }
            is RecordEvent.FileAttached -> onFileAttached(event.attach)
            is RecordEvent.SwitchViews -> switchViews(event.viewMode)
            is RecordEvent.AskForSaving -> askForSaving(event.resultObj)
            RecordEvent.BeforeSaving -> onBeforeSavingAsync()
            is RecordEvent.IsEditedChanged -> editor.setIsEdited(event.isEdited)
            is RecordEvent.EditedDateChanged -> {
                (findViewById<View>(R.id.text_view_record_edited) as TextView).text = event.dateString
            }
            RecordEvent.StartCaptureCamera -> {
                setProgressVisibility(true, getString(R.string.progress_camera_capture))
            }
            RecordEvent.StartLoadImages -> {
                setProgressVisibility(true, getString(R.string.progress_images_loading))
            }
            is RecordEvent.InsertImages -> {
                editor.insertImages(event.images, viewModel.getPathToRecordFolder(viewModel.curRecord.value!!))
                setProgressVisibility(false, null)
            }
            is RecordEvent.OpenWebLink -> openWebLink(event.link)
            is RecordEvent.InsertWebPageContent -> {
                editor.insertWebPageContent(event.content, false)
            }
            is RecordEvent.InsertWebPageText -> {
                editor.insertWebPageContent(event.text, true)
            }
        }
    }

    // region Storage

    private fun startInitStorage() {
        viewModel.checkWriteExtStoragePermission(
            activity = this,
            requestCode = Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE,
            callback =  {
                when (receivedIntent?.action) {
                    Intent.ACTION_MAIN,
                    Constants.ACTION_ADD_RECORD -> {
                        if (!viewModel.initStorage(receivedIntent!!)) {
                            finish()
                        }
                    }
                    else -> {
                        finish()
                    }
                }
            }
        )
    }

    override fun afterStorageInited() {
        viewModel.onStorageInited(receivedIntent)
    }

    override fun afterStorageLoaded(res: Boolean) {
        viewModel.onStorageLoaded(res)
    }

    // endregion Storage

    private fun showNeedMigrationDialog() {
        AskDialogs.showOkDialog(this, R.string.mes_need_migration, R.string.answer_ok, false) { finish() }
    }

    override fun onUICreated(uiCreated: Boolean) {
        viewModel.checkMigration()
    }

    override fun onStart() {
        super.onStart()
        viewModel.isFromAnotherActivity = isFromAnotherActivity()
    }

    // region Open record

    /**
     * Отображние свойств записи.
     */
    private fun loadFields(record: TetroidRecord) {
        var id = R.id.label_record_tags
        // метки
        val tagsHtml = HtmlHelper.createTagsHtmlString(record)
        if (tagsHtml != null) {
            val sequence: CharSequence = Utils.fromHtml(tagsHtml)
            val spannable = SpannableString(sequence)
            val urls = spannable.getSpans(0, sequence.length, URLSpan::class.java)
            for (span in urls) {
                val start = spannable.getSpanStart(span)
                val end = spannable.getSpanEnd(span)
                val clickableSpan: ClickableSpan = object : ClickableSpan() {
                    override fun onClick(view: View) {
                        viewModel.onTagUrlLoad(span.url)
                    }
                }
                spannable.removeSpan(span)
                spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            mTextViewTags.text = spannable
            id = R.id.text_view_record_tags
        }
        // указываем относительно чего теперь выравнивать следующее за метками поле
        // (т.к. метки - многострочный элемент)
        val tvLabelAuthor = findViewById<TextView>(R.id.label_record_author)
        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.BELOW, id)
        tvLabelAuthor.layoutParams = params
        val params2 = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params2.addRule(RelativeLayout.BELOW, id)
        params2.addRule(RelativeLayout.RIGHT_OF, R.id.label_record_author)

        // автор
        val tvAuthor = findViewById<TextView>(R.id.text_view_record_author)
        tvAuthor.layoutParams = params2
        tvAuthor.text = record.author
        // url
        val tvUrl = findViewById<TextView>(R.id.text_view_record_url)
        tvUrl.text = record.url
        // даты
        val dateFormat = getString(R.string.full_date_format_string)
        val tvCreated = findViewById<TextView>(R.id.text_view_record_created)
        val created = record.created
        tvCreated.text = if (created != null) Utils.dateToString(created, dateFormat) else ""
        if (viewModel.buildInfoProvider.isFullVersion()) {
            findViewById<View>(R.id.label_record_edited).visibility = View.VISIBLE
            val tvEdited = findViewById<TextView>(R.id.text_view_record_edited)
            tvEdited.visibility = View.VISIBLE

            //TODO: использовать события вместо корутины
            lifecycleScope.launch {
                val edited = viewModel.getEditedDate(record)?.let { date ->
                    Utils.dateToString(date, dateFormat)
                } ?: "-"
                tvEdited.text = edited
            }
        }
    }

    /**
     * Загрузка html-кода записи из редактора html-кода (fromHtmlEditor) в WebView.
     */
    private fun loadRecordTextFromHtmlEditor() {
        val textHtml = mEditTextHtml.text.toString()
        loadRecordText(textHtml)
    }

    private fun loadRecordText(textHtml: String) {
        mEditTextHtml.reset()
        //mEditor.getWebView().clearAndFocusEditor();
        val baseUrl = viewModel.getUriToRecordFolder()
        editor.webView.loadDataWithBaseURL(
            baseUrl + "/",
            textHtml,
            "text/html",
            "UTF-8",
            null
        )
    }

    // endregion Open record

    // region Load page

    /**
     * Событие запуска загрузки страницы.
     */
    override fun onStartPageLoading() {}

    override fun onPageLoading(progress: Int) {}

    /**
     * Событие окончания загрузки страницы.
     */
    override fun onPageLoaded() {
        viewModel.onPageLoaded()
    }

    /**
     * Событие начала загрузки Javascript-кода для редактирования текста.
     */
    override fun onStartEditorJSLoading() {}

    /**
     * Событие окончания загрузки Javascript-кода для редактирования текста.
     */
    override fun onEditorJSLoaded() {
        viewModel.onEditorJSLoaded(intent)
    }

    override fun onIsEditedChanged(isEdited: Boolean) {
        viewModel.isEdited = isEdited
    }

    /**
     * Обработчик события возврата html-текста заметки из WebView.
     */
    override fun onReceiveEditableHtml(htmlText: String) {
        if (editor.isCalledHtmlRequest) {
            // если этот метод был вызван в результате запроса isCalledHtmlRequest, то:
            editor.setHtmlRequestHandled()
            runOnUiThread { viewModel.onHtmlRequestHandled() }
        } else {
            // метод вызывается в параллельном потоке, поэтому устанавливаем текст в основном
            runOnUiThread {
                mEditTextHtml.setText(htmlText)
                mEditTextHtml.requestFocus()
            }
        }
    }

    // endregion Load page

    // region Load links

    /**
     * Открытие ссылки в тексте.
     */
    override fun onLinkLoad(url: String): Boolean {
        val baseUrl = editor.webView.baseUrl
        viewModel.onLinkLoad(url, baseUrl)
        return true
    }

    override fun onYoutubeLinkLoad(videoId: String) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=$videoId"))
        startActivity(webIntent)
    }

    private fun openWebLink(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        } catch (ex: Exception) {
            viewModel.logError(ex, true)
        }
    }

    // endregion Load links

    // region ColorPicker

    override fun onPickColor(curColor: Int) {
        var defColor = curColor
        if (defColor == 0) {
            // если не передали цвет, то достаем последний из сохраненных
            val savedColors = CommonSettings.getPickedColors(this)
            defColor = if (savedColors != null && savedColors.size > 0) savedColors[savedColors.size - 1] else Color.BLACK
        }
        ColorPickerDialog.newBuilder()
            .setColor(defColor)
            .show(this)
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        CommonSettings.addPickedColor(this, color, WysiwygEditor.MAX_SAVED_COLORS)
        editor.setPickedColor(color)
    }

    override fun getSavedColors(): IntArray {
        return CommonSettings.getPickedColors(this)
    }

    override fun removeSavedColor(index: Int, color: Int) {
        CommonSettings.removePickedColor(this, color)
    }

    override fun onDialogDismissed(dialogId: Int) {}

    // endregion ColorPicker

    // region Image

    override fun startPicker() {
        imagePicker.startPicker()
    }

    override fun startCamera() {
        // проверка разрешения на камеру
        if (!viewModel.checkCameraPermission(this)) {
            return
        }
        imagePicker.startCamera()
    }

    private fun saveSelectedImages(uris: List<Uri>, isCameraMode: Boolean) {
        viewModel.saveImages(uris, isCameraMode)
    }

    fun saveImage(uri: Uri?, deleteSrcFile: Boolean) {
        viewModel.saveImage(uri!!, deleteSrcFile)
    }

    /**
     * Загрузка содержимого Web-страницы по ссылке.
     */
    override fun downloadWebPageContent(url: String, isTextOnly: Boolean) {
        viewModel.downloadWebPageContent(url, isTextOnly)
    }

    /**
     * Загрузка изображение по ссылке.
     */
    override fun downloadImage(url: String) {
        viewModel.downloadImage(url)
    }

    // endregion Image

    // region Attach

    fun attachFile(uri: Uri?, deleteSrcFile: Boolean) {
        viewModel.attachFile(uri, deleteSrcFile)
    }

    private fun onFileAttached(res: TetroidFile) {
        AskDialogs.showYesNoDialog(
            context = this,
            messageResId = R.string.ask_open_attaches,
            onApply = {
                viewModel.openRecordAttaches()
            },
            onCancel = {},
        )
    }

    fun downloadAndAttachFile(uri: Uri?) {
        viewModel.downloadAndAttachFile(uri!!)
    }

    //endregion Attach

    //region Mode

    private fun switchViews(newMode: Int) {
        viewModel.logDebug("switchViews: mode=$newMode")
        when (newMode) {
            Constants.MODE_VIEW -> {
                editor.visibility = View.VISIBLE
                editor.setToolBarVisibility(false)
                mScrollViewHtml!!.visibility = View.GONE
                setRecordFieldsVisibility(true)
                editor.setEditMode(false)
                editor.setScrollButtonsVisibility(true)
                setSubtitle(getString(R.string.subtitle_record_view))
                editor.webView.hideKeyboard()
            }
            Constants.MODE_EDIT -> {
                editor.visibility = View.VISIBLE
                // загружаем Javascript (если нужно)
//                if (!mEditor.getWebView().isEditorJSLoaded()) {
//                    setProgressVisibility(true);
//                }
                editor.webView.loadEditorJSScript(false)
                editor.setToolBarVisibility(true)
                mScrollViewHtml!!.visibility = View.GONE
                setRecordFieldsVisibility(false)
                editor.setEditMode(true)
                editor.setScrollButtonsVisibility(false)
                setSubtitle(getString(R.string.subtitle_record_edit))
                editor.webView.focusEditor()
                editor.webView.focusAndShowKeyboard()
            }
            Constants.MODE_HTML -> {
                editor.visibility = View.GONE
                if (editor.webView.isHtmlRequestMade) {
                    val htmlText = editor.webView.editableHtml
                    mEditTextHtml.setText(htmlText)
                } else {
                    setEditorProgressVisibility(true)
                    // загружаем Javascript (если нужно), и затем делаем запрос на html-текст
                    editor.webView.loadEditorJSScript(true)
                }
                //                mEditor.getWebView().makeEditableHtmlRequest();
                mScrollViewHtml!!.visibility = View.VISIBLE
                setRecordFieldsVisibility(false)
                setSubtitle(getString(R.string.subtitle_record_html))
                mEditTextHtml.focusAndShowKeyboard()
            }
        }
    }

    // endregion Image

    // region Save record

    private fun askForSaving(obj: ResultObj?) {
        AskDialogs.showYesNoDialog(
            context = this,
            messageResId = R.string.ask_save_record,
            onApply = {
                viewModel.saveRecord(obj)
            },
            onCancel = {
                viewModel.onAfterSaving(obj)
            },
        )
    }

    /**
     * Предобработка html-текста перед сохранением.
     * Выполнение кода продолжиться в функции onReceiveEditableHtml().
     */
    private fun onBeforeSavingAsync() {
        editor.beforeSaveAsync(true)
    }

    private fun saveRecord() {
        // если сохраняем запись перед выходом, то учитываем, что можем находиться в режиме HTML
        val htmlText = TetroidEditor.getDocumentHtml(if (viewModel.isHtmlMode()) mEditTextHtml.text.toString() else editor.webView.editableHtml)
        viewModel.saveRecordText(htmlText)
    }

    // endregion Save record

    // region Options record

    /**
     * Отправка записи.
     */
    fun shareRecord() {
        val text = if (viewModel.isHtmlMode()) mEditTextHtml.text.toString() else Utils.fromHtml(
            editor.webView.editableHtml
        ).toString()
        viewModel.shareRecord(text)
    }

    /**
     * Удаление записи.
     */
    fun deleteRecord() {
        AskDialogs.showYesDialog(
            context = this,
            message = getString(R.string.ask_record_delete_mask, viewModel.getRecordName()),
            onApply = {
                viewModel.deleteRecord()
            },
        )
    }

    fun showRecordInfoDialog() {
        RecordInfoDialog(
            viewModel.curRecord.value!!,
            viewModel.getStorageId()
        ).showIfPossible(supportFragmentManager)
    }

    // endregion Options record

    // region Search

    /**
     * Поиск по тексту записи.
     */
    private fun searchInRecordText(query: String) {
        TetroidSuggestionProvider.saveRecentQuery(this, query)
        if (viewModel.isHtmlMode()) {
            val matches = mEditTextHtml.searcher.findAll(query)
            if (matches >= 0) {
                onFindTextResult(matches)
            }
        } else {
            editor.searchText(query)
        }
        // сбрасываем счетчик совпадений от прежнего поиска
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mFindListener!!.reset()
        }
    }

    private fun findPrev() {
        if (viewModel.isHtmlMode()) {
            mEditTextHtml.searcher.prevMatch()
        } else {
            editor.prevMatch()
        }
    }

    private fun findNext() {
        if (viewModel.isHtmlMode()) {
            mEditTextHtml.searcher.nextMatch()
        } else {
            editor.nextMatch()
        }
    }

    private fun stopSearch() {
        if (viewModel.isHtmlMode()) {
            mEditTextHtml.searcher.stopSearch()
        } else {
            editor.stopSearch()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    internal inner class TextFindListener : WebView.FindListener {
        var isStartSearch = true
        override fun onFindResultReceived(activeMatchOrdinal: Int, numberOfMatches: Int, isDoneCounting: Boolean) {
            if (isDoneCounting && isStartSearch) {
                isStartSearch = false
                onFindTextResult(numberOfMatches)
            }
        }

        fun reset() {
            isStartSearch = true
        }
    }

    fun onFindTextResult(matches: Int) {
        if (matches > 0) {
            viewModel.log(getString(R.string.log_n_matches_found, matches), show = true)
        } else {
            viewModel.log(getString(R.string.log_matches_not_found), show = true)
        }
    }

    // endregion Search

    private fun isFromAnotherActivity(): Boolean {
//        ActivityCompat.getReferrer()
        return callingActivity != null
    }

    /**
     * Сохранение записи при любом скрытии активности.
     */
    public override fun onPause() {
        viewModel.onPause()
        super.onPause()
    }

    /**
     * Обработка возвращаемого результата других активностей.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY) {
            if (data != null) {
                if (data.getBooleanExtra(Constants.EXTRA_IS_REINIT_STORAGE, false)) {
                    // хранилище изменено
                    if (viewModel.isStorageLoaded()) {
                        // спрашиваем о перезагрузке хранилище, только если оно уже загружено
                        val isCreate = data.getBooleanExtra(Constants.EXTRA_IS_CREATE_STORAGE, false)
                        StorageDialogs.showReloadStorageDialog(
                            context = this,
                            toCreate = isCreate,
                            pathChanged = true,
                            onApply = {
                                // перезагружаем хранилище в главной активности, если изменили путь,
                                finishWithResult(Constants.RESULT_REINIT_STORAGE, data.extras)
                            },
                        )
                    }
                } else if (data.getBooleanExtra(Constants.EXTRA_IS_PASS_CHANGED, false)) {
                    // пароль изменен
                    finishWithResult(Constants.RESULT_PASS_CHANGED, data.extras)
                }
            }
        } else if (requestCode == Constants.REQUEST_CODE_COMMON_SETTINGS_ACTIVITY) {
            // не гасим экран, если установили опцию
            checkKeepScreenOn(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        checkReceivedIntent(intent)
        super.onNewIntent(intent)
    }

    /**
     * Проверка входящего Intent.
     */
    private fun checkReceivedIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            // обработка результата голосового поиска
            val query = intent.getStringExtra(SearchManager.QUERY)
            mSearchView.setQuery(query, true)
        }
    }
    
    // region Options menu
    
    /**
     * Обработчик создания системного меню.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!super.onBeforeCreateOptionsMenu(menu)) return true
        menuInflater.inflate(R.menu.record, menu)
        optionsMenu = menu
        initSearchView(menu)
        return super.onAfterCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (!isOnCreateProcessed) return true
        // режимы
        val mode = viewModel.curMode
        activateMenuItem(menu.findItem(R.id.action_record_view), mode == Constants.MODE_EDIT)
        activateMenuItem(
            menu.findItem(R.id.action_record_edit), mode == Constants.MODE_VIEW
                    || mode == Constants.MODE_HTML
        )
        activateMenuItem(
            menu.findItem(R.id.action_record_html), mode == Constants.MODE_VIEW
                    || mode == Constants.MODE_EDIT
        )
        activateMenuItem(menu.findItem(R.id.action_record_save), mode == Constants.MODE_EDIT)
        val isLoaded = viewModel.isStorageLoaded()
        val isLoadedFavoritesOnly = viewModel.isLoadedFavoritesOnly()
        val isTemp = viewModel.isRecordTemprorary()
        activateMenuItem(menu.findItem(R.id.action_record_edit_fields), isLoaded && !isLoadedFavoritesOnly, !isTemp)
        activateMenuItem(menu.findItem(R.id.action_record_node), isLoaded && !isLoadedFavoritesOnly, !isTemp)
        activateMenuItem(menu.findItem(R.id.action_delete), isLoaded && !isLoadedFavoritesOnly, !isTemp)
        activateMenuItem(menu.findItem(R.id.action_attached_files), isLoaded, !isTemp)
        activateMenuItem(menu.findItem(R.id.action_cur_record_folder), true, !isTemp)
        activateMenuItem(menu.findItem(R.id.action_info), isLoaded, !isTemp)
        enableMenuItem(menu.findItem(R.id.action_storage_settings), isLoaded)
        return super.onPrepareOptionsMenu(menu)
    }

    private fun activateMenuItem(menuItem: MenuItem, isVisible: Boolean, isEnabled: Boolean) {
        menuItem.isVisible = isVisible
        menuItem.isEnabled = isEnabled
    }

    private fun activateMenuItem(menuItem: MenuItem, isVisible: Boolean) {
        menuItem.isVisible = isVisible
    }

    /**
     * Инициализация элементов для поиска.
     */
    fun initSearchView(menu: Menu) {
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        mSearchView = menu.findItem(R.id.action_search_text).actionView as SearchView
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        mSearchView.setIconifiedByDefault(true)
        // добавлять кнопки справа в выпадающем списке предложений (suggestions), чтобы вставить выбранное
        // предложение в строку запроса для дальнейшего уточнения (изменения), а не для поиска по нему
        mSearchView.isQueryRefinementEnabled = true
        object : SearchViewXListener(mSearchView) {
            override fun onSearchClick() {}
            override fun onQuerySubmit(query: String) {
                searchInRecordText(query)
                setFindButtonsVisibility(true)
            }

            override fun onQueryChange(query: String) {}
            override fun onSuggestionSelectOrClick(query: String) {
                mSearchView.setQuery(query, true)
            }

            override fun onClose() {
                setFindButtonsVisibility(false)
                stopSearch()
            }
        }
    }

    /**
     * Обработчик выбора пунктов системного меню.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_record_view -> {
                viewModel.switchMode(Constants.MODE_VIEW)
                return true
            }
            R.id.action_record_edit -> {
                viewModel.switchMode(Constants.MODE_EDIT)
                return true
            }
            R.id.action_record_html -> {
                viewModel.switchMode(Constants.MODE_HTML)
                return true
            }
            R.id.action_record_save -> {
                viewModel.saveRecord(null)
                return true
            }
            R.id.action_record_edit_fields -> {
                editFields(null)
                return true
            }
            R.id.action_record_node -> {
                viewModel.showRecordNode()
                return true
            }
            R.id.action_attached_files -> {
                viewModel.openRecordAttaches()
                return true
            }
            R.id.action_cur_record_folder -> {
                viewModel.openRecordFolder()
                return true
            }
            R.id.action_share -> {
                shareRecord()
                return true
            }
            R.id.action_delete -> {
                deleteRecord()
                return true
            }
            R.id.action_info -> {
                showRecordInfoDialog()
                return true
            }
            R.id.action_fullscreen -> {
                toggleFullscreen(false)
                return true
            }
            R.id.action_storage_settings -> showStorageSettingsActivity(viewModel.storage)
            R.id.action_settings -> {
                showActivityForResult(SettingsActivity::class.java, Constants.REQUEST_CODE_COMMON_SETTINGS_ACTIVITY)
                return true
            }
            R.id.action_storage_info -> {
                start(this, viewModel.getStorageId())
                return true
            }
            android.R.id.home -> return viewModel.onHomePressed()
        }
        return super.onOptionsItemSelected(item)
    }
    
    // endregion Options menu
    
    // region Record fields
    
    /**
     * Редактирование свойств записи.
     */
    private fun editFields(obj: ResultObj?) {
        RecordFieldsDialog(
            record = viewModel.curRecord.value,
            chooseNode = true,
            node = viewModel.quicklyNode,
            storageId = viewModel.getStorageId()
        ) { name: String, tags: String, author: String, url: String, node: TetroidNode, isFavor: Boolean ->
            viewModel.editFields(
                obj,
                name,
                tags,
                author,
                url,
                node,
                isFavor
            )
        }
            .showIfPossible(supportFragmentManager)
    }

    /**
     * Скрытие/раскрытие панели со свойствами записи.
     */
    private fun toggleRecordFieldsVisibility() {
        /*mFieldsExpanderLayout.toggle();

        if (mFieldsExpanderLayout.isExpanded()) {
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_up_white);
        } else {
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_down_white);
        }*/
        expandRecordFields(!mFieldsExpanderLayout.isExpanded)
        updateScrollButtonLocation()
    }

    private fun expandRecordFields(expand: Boolean) {
        if (expand) {
            mFieldsExpanderLayout.expand()
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_up_white)
        } else {
            mFieldsExpanderLayout.collapse()
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_down_white)
        }
        updateScrollButtonLocation()
    }

    /**
     * Отображение или скрытие панели свойств в зависимости от настроек.
     */
    private fun expandFieldsIfNeed() {
        expandRecordFields(viewModel.isNeedExpandFields())
    }

    /**
     * Управление видимостью панели со свойствами записи.
     * Полностью, вместе с кнопкой управления видимостью панели.
     */
    private fun setRecordFieldsVisibility(isVisible: Boolean) {
        if (isVisible) {
            mFieldsExpanderLayout.visibility = View.VISIBLE
            mButtonToggleFields.show()
        } else {
            mFieldsExpanderLayout.visibility = View.GONE
            mButtonToggleFields.hide()
        }
    }

    // endregion Record fields

    override fun toggleFullscreen(fromDoubleTap: Boolean): Int {
        val res = super.toggleFullscreen(fromDoubleTap)
        if (res >= 0) {
            val isFullscreen = res == 1
            if (isFullscreen) {
                mButtonFullscreen.show()
            } else {
                mButtonFullscreen.hide()
            }
            if (viewModel.isViewMode()) {
                setRecordFieldsVisibility(!isFullscreen)
            }
        }
        return res
    }

    /**
     * Обновление расположения кнопок скроллинга записи вниз/вверх.
     */
    private fun updateScrollButtonLocation() {
        val params = mButtonScrollTop.layoutParams as RelativeLayout.LayoutParams
//        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) scrollTopButton.getLayoutParams();
//        float density = getResources().getDisplayMetrics().density;
//        int fabMargin = (int) (getResources().getDimension(R.dimen.fab_small_margin) / density);
        if (mFieldsExpanderLayout.isExpanded) {
//            params.topMargin = fabMargin;
            params.addRule(RelativeLayout.ALIGN_TOP, R.id.html_editor)
            //            params.topMargin = fabMargin;
        } else {
//            int aboveButtonHeight = (int) (mButtonToggleFields.getMeasuredHeight() / density);
//            params.topMargin = fabMargin + aboveButtonHeight + fabMargin;
//            params.rightMargin = fabMargin; // для совпадения с mButtonToggleFields
            params.addRule(RelativeLayout.BELOW, R.id.button_toggle_fields)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.removeRule(RelativeLayout.ALIGN_TOP)
            }
            //            params.topMargin = 0;
        }
        mButtonScrollTop.layoutParams = params
    }

    /**
     * Установка видимости и позиционирования Fab кнопок при включении/выключении режима поиска.
     */
    fun setFindButtonsVisibility(vis: Boolean) {
        ViewUtils.setFabVisibility(mButtonFindNext, vis)
        ViewUtils.setFabVisibility(mButtonFindPrev, vis)
        setRecordFieldsVisibility(!vis && viewModel.curMode != Constants.MODE_HTML)
        // установим позиционирование кнопки FindNext от правого края
        var params = mButtonFindNext.layoutParams as RelativeLayout.LayoutParams
        if (vis) {
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.removeRule(RelativeLayout.RIGHT_OF)
            }
        } else {
            params.addRule(RelativeLayout.RIGHT_OF, R.id.button_toggle_fields)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            }
        }
        mButtonFindNext.layoutParams = params
        // установим позиционирование кнопки ScrollToTop от кнопки FindNext
        if (vis) {
            params = mButtonScrollTop.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.BELOW, R.id.button_find_next)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.removeRule(RelativeLayout.ALIGN_TOP)
            }
            mButtonScrollTop.layoutParams = params
        } else {
            updateScrollButtonLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        when (requestCode) {
            Constants.REQUEST_CODE_PERMISSION_CAMERA -> {
                if (permGranted) {
                    viewModel.log(R.string.log_camera_perm_granted)
                    startCamera()
                } else {
                    viewModel.logWarning(R.string.log_missing_camera_perm, true)
                }
            }
        }
    }

    override fun showActivityForResult(cls: Class<*>?, requestCode: Int) {
        val intent = Intent(this, cls)
        startActivityForResult(intent, requestCode)
    }

    /**
     * Формирование результата активности и ее закрытие.
     * При необходимости запуск главной активности, если текущая была запущена самостоятельно.
     */
    private fun finishWithResult(resCode: Int, bundle: Bundle?) {
        if (callingActivity != null) {
            if (bundle != null) {
                bundle.putInt(Constants.EXTRA_RESULT_CODE, resCode)
                val intent = Intent(Constants.ACTION_RECORD)
                intent.putExtras(bundle)
                setResult(resCode, intent)
            } else {
                setResult(resCode)
            }
        } else {
            val bundle = bundle ?: Bundle()
            bundle.putInt(Constants.EXTRA_RESULT_CODE, resCode)
            MainActivity.start(this, Constants.ACTION_RECORD, bundle)
        }
        finish()
    }

    /**
     * Обработчик нажатия кнопки Назад.
     */
    override fun onBackPressed() {
        if (!onBeforeBackPressed()) {
            return
        }
        // выполняем родительский метод только если не был запущен асинхронный код
        if (viewModel.isCanBack(isFromAnotherActivity())) {
            super.onBackPressed()
        }
    }

    /**
     * Обработчик события закрытия активности.
     */
    override fun onStop() {
        // отключаем блокировку выключения экрана
        ViewUtils.setKeepScreenOn(this, false)
        super.onStop()
    }

    fun setEditorProgressVisibility(isVisible: Boolean) {
        mHtmlProgressBar.isVisible = isVisible
    }

    /**
     * Обработчк изменения текста в окне HTML-кода записи.
     */
    private inner class HtmlEditTextEditTextListener : ITetroidEditTextListener {
        override fun onAfterTextInited() {
            if (viewModel.isHtmlMode()) {
                setEditorProgressVisibility(false)
            }
        }

        override fun onAfterTextChanged() {
            if (viewModel.isHtmlMode()) {
                editor.setIsEdited()
                viewModel.isEdited = true
            }
        }
    }

    companion object {
        fun start(activity: Activity, action: String?, requestCode: Int, bundle: Bundle?) {
            val intent = Intent(activity, RecordActivity::class.java)
            intent.action = action
            if (bundle != null) {
                intent.putExtras(bundle)
            }
            activity.startActivityForResult(intent, requestCode)
        }
    }

}