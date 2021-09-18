package com.gee12.mytetroid.views.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;

import com.esafirm.imagepicker.features.ImagePicker;
import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.htmlwysiwygeditor.IImagePicker;
import com.gee12.htmlwysiwygeditor.INetworkWorker;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.PermissionManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TetroidSuggestionProvider;
import com.gee12.mytetroid.common.Constants;
import com.gee12.mytetroid.helpers.HtmlHelper;
import com.gee12.mytetroid.data.SettingsManager;
//import com.gee12.mytetroid.data.StorageManager;
import com.gee12.mytetroid.viewmodels.StorageViewModel;
import com.gee12.mytetroid.viewmodels.StorageViewModelFactory;
import com.gee12.mytetroid.views.dialogs.AskDialogs;
import com.gee12.mytetroid.views.dialogs.RecordDialogs;
import com.gee12.mytetroid.helpers.TetroidClipboardListener;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.services.FileObserverService;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.ImgPicker;
import com.gee12.mytetroid.views.Message;
import com.gee12.mytetroid.views.SearchViewXListener;
import com.gee12.mytetroid.views.TetroidEditText;
import com.gee12.mytetroid.views.TetroidEditor;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.lumyjuwon.richwysiwygeditor.IColorPicker;
import com.lumyjuwon.richwysiwygeditor.RichEditor.EditableWebView;
import com.lumyjuwon.richwysiwygeditor.WysiwygEditor;

import net.cachapa.expandablelayout.ExpandableLayout;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

/**
 * Активность просмотра и редактирования содержимого записи.
 */
public class RecordActivity extends TetroidActivity implements
        EditableWebView.IPageLoadListener,
        EditableWebView.ILinkLoadListener,
        EditableWebView.IHtmlReceiveListener,
        EditableWebView.IYoutubeLinkLoadListener,
        IImagePicker,
        IColorPicker,
        INetworkWorker,
        ColorPickerDialogListener,
        TetroidEditor.IEditorListener {

    /**
     *
     */
    private static class ResultObj {

        static final int NONE = 0;
        static final int EXIT = 1;
        static final int START_MAIN_ACTIVITY = 2;
        static final int OPEN_RECORD = 3;
        static final int OPEN_NODE = 4;
        static final int OPEN_FILE = 5;
        static final int OPEN_TAG = 6;

        int type;
        String id;
        Object obj;
        boolean needReloadText;

        ResultObj(int type) {
            init(type, null, null);
        }

        ResultObj(int type, String id) {
            // если есть id, значит obj использоваться не будет
            init(type, id, null);
        }

        ResultObj(Object obj) {
            this.obj = obj;
            if (obj instanceof TetroidRecord) {
                this.type = OPEN_RECORD;
                this.id = ((TetroidRecord)obj).getId();
            } else if (obj instanceof TetroidNode) {
                this.type = OPEN_NODE;
                this.id = ((TetroidNode)obj).getId();
            } else if (obj instanceof TetroidFile) {
                this.type = OPEN_FILE;
                this.id = ((TetroidFile)obj).getId();
            } else if (obj instanceof String) {
                this.type = OPEN_TAG;
            } else {
                this.type = NONE;
            }
        }

        private void init(int type, String id, Object obj) {
            this.type = type;
            this.id = id;
            this.obj = obj;
        }
    }

    public static final int MODE_VIEW = 1;
    public static final int MODE_EDIT = 2;
    public static final int MODE_HTML = 3;

    public static final int RESULT_REINIT_STORAGE = 1;
    public static final int RESULT_PASS_CHANGED = 2;
    public static final int RESULT_OPEN_RECORD = 3;
    public static final int RESULT_OPEN_NODE = 4;
    public static final int RESULT_SHOW_ATTACHES = 5;
    public static final int RESULT_SHOW_TAG = 6;
    public static final int RESULT_DELETE_RECORD = 7;

    private ExpandableLayout mFieldsExpanderLayout;
    private FloatingActionButton mButtonToggleFields;
    private FloatingActionButton mButtonFullscreen;
    private TextView mTextViewTags;
    private ScrollView mScrollViewHtml;
    private TetroidEditText mEditTextHtml;
    protected ProgressBar mHtmlProgressBar;
    private TetroidEditor mEditor;
    private FloatingActionButton mButtonScrollTop;
    private FloatingActionButton mButtonFindNext;
    private FloatingActionButton mButtonFindPrev;

    private TetroidRecord mRecord;
    private int mCurMode;
    private boolean mIsFirstLoad = true;
    private boolean mIsFieldsEdited;
    private TextFindListener mFindListener;
    private SearchView mSearchView;
    private boolean mIsReceivedImages;
    private int mModeToSwitch = -1;
    private boolean mIsSaveTempAfterStorageLoaded;
    private ResultObj mResultObj;

    private StorageViewModel mStorageViewModel;


    public RecordActivity() {
        super();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_record;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mStorageViewModel = new ViewModelProvider(this, new StorageViewModelFactory(getApplication()))
                .get(StorageViewModel.class);

        String action;
        if (receivedIntent == null || (action = receivedIntent.getAction()) == null) {
            finish();
            return;
        }
        if (action.equals(Intent.ACTION_MAIN)) {
            // открытие или создание записи из главной активности
            if (!initRecordFromMain(receivedIntent)) {
                finish();
                return;
            }
        } else if (action.equals(Constants.ACTION_ADD_RECORD)) {
            // создание записи из виджета
            // сначала инициализируем службы
            App.init(this);
            if (!initRecordFromWidget()) {
                finish();
                return;
            }
        /*} else if (action.equals(Intent.ACTION_SEND)) {
            // прием текста из другого приложения
            String type = mReceivedIntent.getType();
            if (type != null && type.startsWith("text/")) {
                String text = mReceivedIntent.getStringExtra(Intent.EXTRA_TEXT);
                if (text == null) {
                    LogManager.log(this, R.string.log_not_passed_text, ILogger.Types.WARNING, Toast.LENGTH_LONG);
                    finish();
                    return;
                }
                LogManager.log(this, getString(R.string.log_receiving_intent_text), ILogger.Types.INFO);
                showIntentDialog(mReceivedIntent, text);
            } else {
                finish();
                return;
            }*/
        } else {
            finish();
            return;
        }

        // проверяем передавались ли изображения
        this.mIsReceivedImages = receivedIntent.hasExtra(Constants.EXTRA_IMAGES_URI);

        this.mEditor = findViewById(R.id.html_editor);
        mEditor.setColorPickerListener(this);
        mEditor.setImagePickerListener(this);
        mEditor.setNetworkWorkerListener(this);
        mEditor.setToolBarVisibility(false);
//        mEditor.setOnTouchListener(this);
        mEditor.setOnPageLoadListener(this);
        mEditor.setEditorListener(this);
        EditableWebView webView = mEditor.getWebView();
        webView.setOnTouchListener(this);
        webView.setOnUrlLoadListener(this);
        webView.setOnHtmlReceiveListener(this);
        webView.setYoutubeLoadLinkListener(this);
        webView.setClipboardListener(new TetroidClipboardListener(this));

        this.mTextViewTags = findViewById(R.id.text_view_record_tags);
        mTextViewTags.setMovementMethod(LinkMovementMethod.getInstance());

        this.mFieldsExpanderLayout = findViewById(R.id.layout_fields_expander);
//        ToggleButton tbRecordFieldsExpander = findViewById(R.id.toggle_button_expander);
//        tbRecordFieldsExpander.setOnCheckedChangeListener((buttonView, isChecked) -> mFieldsExpanderLayout.toggle());
        this.mButtonToggleFields = findViewById(R.id.button_toggle_fields);
        mButtonToggleFields.setOnClickListener(v -> toggleRecordFieldsVisibility());
        setRecordFieldsVisibility(false);
//        ViewUtils.setOnGlobalLayoutListener(mButtonToggleFields, () -> updateScrollButtonLocation());
//        this.mButtonFieldsEdit = findViewById(R.id.button_edit_fields);
//        mButtonFieldsEdit.setOnClickListener(v -> editFields());
        this.mButtonFullscreen = findViewById(R.id.button_fullscreen);
        mButtonFullscreen.setOnClickListener(v -> toggleFullscreen(false));

        FloatingActionButton mButtonScrollBottom = findViewById(R.id.button_scroll_bottom);
        this.mButtonScrollTop = findViewById(R.id.button_scroll_top);
        // прокидывание кнопок пролистывания в редактор
        mEditor.initScrollButtons(mButtonScrollBottom, mButtonScrollTop);

        this.mButtonFindNext = findViewById(R.id.button_find_next);
        mButtonFindNext.setOnClickListener(v -> findNext());
        this.mButtonFindPrev = findViewById(R.id.button_find_prev);
        mButtonFindPrev.setOnClickListener(v -> findPrev());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            this.mFindListener = new TextFindListener();
            mEditor.getWebView().setFindListener(mFindListener);
        } else {
            // TODO: что здесь для API=15 ?
        }

        this.mScrollViewHtml = findViewById(R.id.scroll_html);
        this.mEditTextHtml = findViewById(R.id.edit_text_html);
//        mEditTextHtml.setOnTouchListener(this); // работает криво
        mEditTextHtml.setTetroidListener(new HtmlEditTextEditTextListener());
        mEditTextHtml.initSearcher(mScrollViewHtml);

        this.mHtmlProgressBar = findViewById(R.id.progress_bar);

        // не гасим экран, если установлена опция
        App.checkKeepScreenOn(this);

        afterOnCreate();

        // TODO: перенести в TetroidActivity (с использованием Generic типов ?)
        mStorageViewModel.getMessage().observe(this, message -> {
            if (message.getType() == ILogger.Types.INFO) {
                Toast.makeText(RecordActivity.this, message.getMes(), Toast.LENGTH_SHORT).show();
            } else if (message.getType() == ILogger.Types.ERROR) {

                // TODO: переделать
                Toast.makeText(RecordActivity.this, message.getMes(), Toast.LENGTH_SHORT).show();
                Message.showSnackMoreInLogs(RecordActivity.this, R.id.layout_coordinator);
            } else {

                // TODO: ?
                Toast.makeText(RecordActivity.this, message.getMes(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onGUICreated() {
        try {
            // загрузка записи выполняется в последнюю очередь (после создания пунктов меню)
            openRecord(mRecord);
        } catch (Exception ex) {
            LogManager.log(this, ex);
        }
    }


    // region OpenRecord

    /**
     * Получение записи из хранилища, т.к. актиность была запущена из MainActivity.
     * @param intent
     * @return
     */
    private boolean initRecordFromMain(Intent intent) {
        // получаем переданную запись
        String recordId = intent.getStringExtra(Constants.EXTRA_OBJECT_ID);
        if (recordId != null) {
            // получаем запись
            this.mRecord = RecordsManager.getRecord(recordId);
            if (mRecord == null) {
                LogManager.log(this, getString(R.string.log_not_found_record) + recordId, ILogger.Types.ERROR, Toast.LENGTH_LONG);
                return false;
            } else {
                setTitle(mRecord.getName());
//                setVisibilityActionHome(!mRecord.isTemp());

                if (intent.hasExtra(Constants.EXTRA_ATTACHED_FILES)) {
                    // временная запись создана для прикрепления файлов
                    int filesCount = mRecord.getAttachedFilesCount();
                    if (filesCount > 0) {
                        String mes = (filesCount == 1)
                                ? String.format(getString(R.string.mes_attached_file_mask), mRecord.getAttachedFiles().get(0).getName())
                                : String.format(getString(R.string.mes_attached_files_mask), filesCount);
                        Message.show(this, mes, Toast.LENGTH_LONG);
                    }
                }
            }
        } else {
            LogManager.log(this, getString(R.string.log_not_transferred_record_id), ILogger.Types.ERROR, Toast.LENGTH_LONG);
            return false;
        }
        return true;
    }

    /**
     * Создание новой временной записи, т.к. активность была запущена из виджета AddRecordWidget.
     * @return
     */
    private boolean initRecordFromWidget() {
        setVisibilityActionHome(false);
        // создаем временную запись
        this.mRecord = RecordsManager.createTempRecord(this, null, null, null);
        if (mRecord != null) {
            setTitle(mRecord.getName());
        } else {
            TetroidLog.logOperError(this, TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE, Toast.LENGTH_LONG);
            return false;
        }
        return true;
    }

    /**
     * Отображение записи (свойств и текста).
     * @param record
     */
    public void openRecord(TetroidRecord record) {
        if (record == null)
            return;
        LogManager.log(this, getString(R.string.log_record_loading) + record.getId(), ILogger.Types.INFO);
        loadFields(record);
        expandFieldsIfNeed();
        // текст
        loadRecordText(record, false);
    }

    /**
     * Отображние свойств записи.
     * @param record
     */
    private void loadFields(TetroidRecord record) {
        if (record == null)
            return;

        int id = R.id.label_record_tags;
        // метки
        String tagsHtml = HtmlHelper.createTagsHtmlString(record);
        if (tagsHtml != null) {
            CharSequence sequence = Utils.fromHtml(tagsHtml);
            SpannableString spannable = new SpannableString(sequence);
            URLSpan[] urls = spannable.getSpans(0, sequence.length(), URLSpan.class);
            for (URLSpan span : urls) {
                int start = spannable.getSpanStart(span);
                int end = spannable.getSpanEnd(span);
                ClickableSpan clickableSpan = new ClickableSpan() {
                    public void onClick(View view) {
                        viewModel.onTagUrlLoad(span.getURL());
                    }
                };
                spannable.removeSpan(span);
                spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            mTextViewTags.setText(spannable);
            id = R.id.text_view_record_tags;
        }
        // указываем относительно чего теперь выравнивать следующее за метками поле
        // (т.к. метки - многострочный элемент)
        TextView tvLabelAuthor = findViewById(R.id.label_record_author);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, id);
        tvLabelAuthor.setLayoutParams(params);

        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params2.addRule(RelativeLayout.BELOW, id);
        params2.addRule(RelativeLayout.RIGHT_OF, R.id.label_record_author);

        // автор
        TextView tvAuthor = findViewById(R.id.text_view_record_author);
        tvAuthor.setLayoutParams(params2);
        tvAuthor.setText(record.getAuthor());
        // url
        TextView tvUrl = findViewById(R.id.text_view_record_url);
        tvUrl.setText(record.getUrl());
        // даты
        String dateFormat = getString(R.string.full_date_format_string);
        TextView tvCreated = findViewById(R.id.text_view_record_created);
        Date created = record.getCreated();
        tvCreated.setText((created != null) ? Utils.dateToString(created, dateFormat) : "");

        if (App.isFullVersion()) {
            (findViewById(R.id.label_record_edited)).setVisibility(View.VISIBLE);
            TextView tvEdited = findViewById(R.id.text_view_record_edited);
            tvEdited.setVisibility(View.VISIBLE);
            Date edited = RecordsManager.getEditedDate(this, record);
            tvEdited.setText((edited != null) ? Utils.dateToString(edited, dateFormat) : "-");
        }
    }

    /**
     * Загрузка html-кода записи в WebView.
     * @param record
     */
    private void loadRecordText(TetroidRecord record, boolean fromHtmlEditor) {
        // 1) если только что вернулись из редактора html-кода (fromHtmlEditor)
        // /*-------и не используется авто-сохранение изменений,-------*/
        // то загружаем html-код из редактора
        String textHtml = null;
        if (fromHtmlEditor /*&& !SettingsManager.isRecordAutoSave()*/) {
            textHtml = mEditTextHtml.getText().toString();
        } else {
            // 2) если нет, то загружаем html-код из файла записи, только если он не новый
            if (!record.isNew()) {
                textHtml = RecordsManager.getRecordHtmlTextDecrypted(this, record, Toast.LENGTH_LONG);
                if (textHtml == null) {
                    if (record.isCrypted() && DataManager.getInstance().getCrypterManager().getErrorCode() > 0) {
                        LogManager.log(this, R.string.log_error_record_file_decrypting, ILogger.Types.ERROR, Toast.LENGTH_LONG);
                        showSnackMoreInLogs();
                    }
                }
            }
        }
        mEditTextHtml.reset();
        //mEditor.getWebView().clearAndFocusEditor();
        final String text = textHtml;
//        mEditor.getWebView().loadDataWithBaseURL(RecordsManager.getUriToRecordFolder(record),
        mEditor.getWebView().loadDataWithBaseURL(RecordsManager.getUriToRecordFolder(this, record),
                text, "text/html", "UTF-8", null);
    }

    // endregion OpenRecord


    // region LoadPage

    /**
     * Событие запуска загрузки страницы.
     */
    @Override
    public void onStartPageLoading() {
    }

    @Override
    public void onPageLoading(int progress) {
    }

    /**
     * Событие окончания загрузки страницы.
     */
    @Override
    public void onPageLoaded() {
       viewModel.onPageLoaded();
    }

    /**
     * Событие начала загрузки Javascript-кода для редактирования текста.
     */
    @Override
    public void onStartEditorJSLoading() {
//        setProgressVisibility(true);
    }

    /**
     * Событие окончания загрузки Javascript-кода для редактирования текста.
     */
    @Override
    public void onEditorJSLoaded() {
//        setProgressVisibility(false);
        viewModel.onEditorJSLoaded(getIntent());
    }

    @Override
    public void onIsEditedChanged(boolean isEdited) {
        viewModel.setEdited(isEdited);
    }

    /**
     * Обработчик события возврата html-текста заметки из WebView.
     * @param htmlText
     */
    @Override
    public void onReceiveEditableHtml(String htmlText) {
        if (mEditor.isCalledHtmlRequest()) {
            // если этот метод был вызван в результате запроса isCalledHtmlRequest, то:
            mEditor.setHtmlRequestHandled();
            runOnUiThread(() -> {
                viewModel.onHtmlRequestHandled();
            });
        } else {
            // метод вызывается в параллельном потоке, поэтому устанавливаем текст в основном
            runOnUiThread(() -> {
                mEditTextHtml.setText(htmlText);
                mEditTextHtml.requestFocus();
//            setProgressVisibility(false);
            });
        }
    }

    // endregion LoadPage

    // region LoadLinks

    /**
     * Открытие ссылки в тексте.
     * @param url
     */
    @Override
    public boolean onLinkLoad(String url) {
        String baseUrl = mEditor.getWebView().getBaseUrl();
        viewModel.onLinkLoad(url, baseUrl);
        return true;
    }

    @Override
    public void onYoutubeLinkLoad(String videoId) {
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + videoId));
        startActivity(webIntent);
    }

    private void openWebLink(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception ex) {
            LogManager.log(this, ex, Toast.LENGTH_LONG);
        }
    }

    // endregion LoadLinks

    // region ColorPicker

    @Override
    public void onPickColor(int curColor) {
        int defColor = curColor;
        if (defColor == 0) {
            // если не передали цвет, то достаем последний из сохраненных
            int[] savedColors = SettingsManager.getPickedColors(this);
            defColor = (savedColors != null && savedColors.length > 0)
                    ? savedColors[savedColors.length - 1] : Color.BLACK;
        }
        ColorPickerDialog.newBuilder()
                .setColor(defColor)
                .show(this);
    }

    @Override
    public void onColorSelected(int dialogId, int color) {
        SettingsManager.addPickedColor(this, color, WysiwygEditor.MAX_SAVED_COLORS);
        mEditor.setPickedColor(color);
    }

    @Override
    public int[] getSavedColors() {
        return SettingsManager.getPickedColors(this);
    }

    @Override
    public void removeSavedColor(int index, int color) {
        SettingsManager.removePickedColor(this, color);
    }

    @Override
    public void onDialogDismissed(int dialogId) {
    }

    // endregion ColorPicker

    // region Image

    @Override
    public void startPicker() {
        ImgPicker.startPicker(this);
    }

    @Override
    public void startCamera() {
        // проверка разрешения
        if (!PermissionManager.checkCameraPermission(this, Constants.REQUEST_CODE_PERMISSION_CAMERA)) {
            return;
        }
        // не удалось сохранять сделанную фотографию сразу в каталог записи
        // (возникает ошибка на Android 9)
//        Intent intent = ImgPicker.createCamera(DataManager.getStoragePathBase(), mRecord.getDirName())
        Intent intent = ImagePicker.cameraOnly()
                .getIntent(this);
        startActivityForResult(intent, Constants.REQUEST_CODE_CAMERA);
    }

    public void saveImage(Uri uri, boolean deleteSrcFile) {
        viewModel.saveImage(uri, deleteSrcFile);
    }

    /**
     * Загрузка содержимого Web-страницы по ссылке.
     * @param url
     * @return
     */
    @Override
    public void downloadWebPageContent(String url, boolean isTextOnly) {
        viewModel.downloadWebPageContent(url, isTextOnly);
    }

    /**
     * Загрузка изображение по ссылке.
     * @param url
     * @return
     */
    @Override
    public void downloadImage(String url) {
        viewModel.downloadImage(url);
    }

    // endregion Image

    // region Attach

    public void attachFile(Uri uri, boolean deleteSrcFile) {
        viewModel.attachFile(uri, deleteSrcFile);
    }

    private void onFileAttached(TetroidFile res) {
        AskDialogs.showYesNoDialog(this, new Dialogs.IApplyCancelResult() {
            @Override
            public void onApply() {
                viewModel.openRecordAttaches();
            }
            @Override
            public void onCancel() {
            }
        }, R.string.ask_open_attaches);
    }

    public void downloadAndAttachFile(Uri uri) {
        viewModel.downloadAndAttachFile(uri);
    }

    //endregion Attach

    //region Mode

//    private void switchMode(int newMode, boolean isNeedSave) {
//        this.mModeToSwitch = -1;
//        int oldMode = mCurMode;
//        // сохраняем
////        onSaveRecord();
//        boolean runBeforeSaving = false;
//        if (isNeedSave && SettingsManager.isRecordAutoSave(this) && !mRecord.isTemp()) {
//            // автоматически сохраняем текст записи, если:
//            //  * есть изменения
//            //  * не находимся в режиме HTML (сначала нужно перейти в режим EDIT (WebView), а уже потом можно сохранять)
//            //  * запись не временная
//            if (mEditor.isEdited() && mCurMode != MODE_HTML) {
//                runBeforeSaving = saveRecord(null);
//            }
//        }
//        // если асинхронно запущена предобработка сохранения, то выходим
//        if (runBeforeSaving) {
//            this.mModeToSwitch = newMode;
//            return;
//        }
//        // перезагружаем html-текст записи в webView, если был режим редактирования HTML
//        if (oldMode == MODE_HTML) {
//            loadRecordText(mRecord, true);
//        }
//        // переключаем элементы интерфейса, только если не редактировали только что html,
//        // т.к. тогда вызов switchViews() должен произойти уже после перезагрузки страницы
//        // на событии onPageLoaded())
////        if (oldMode != MODE_HTML) {
//        else {
//            switchViews(newMode);
//        }
//        this.mCurMode = newMode;
//
//        updateOptionsMenu();
//    }

    /**
     *
     * @param newMode
     */
    private void switchViews(int newMode) {
        switch (newMode) {
            case Constants.MODE_VIEW : {
                mEditor.setVisibility(View.VISIBLE);
                mEditor.setToolBarVisibility(false);
                mScrollViewHtml.setVisibility(View.GONE);
                setRecordFieldsVisibility(true);
                mEditor.setEditMode(false);
                mEditor.setScrollButtonsVisibility(true);
                setSubtitle(getString(R.string.subtitle_record_view));
                ViewUtils.hideKeyboard(this, mEditor.getWebView());
            } break;
            case Constants.MODE_EDIT : {
                mEditor.setVisibility(View.VISIBLE);
                // загружаем Javascript (если нужно)
//                if (!mEditor.getWebView().isEditorJSLoaded()) {
//                    setProgressVisibility(true);
//                }
                mEditor.getWebView().loadEditorJSScript(false);

                mEditor.setToolBarVisibility(true);
                mScrollViewHtml.setVisibility(View.GONE);
                setRecordFieldsVisibility(false);
                mEditor.setEditMode(true);
                mEditor.setScrollButtonsVisibility(false);
                setSubtitle(getString(R.string.subtitle_record_edit));
                mEditor.getWebView().focusEditor();
                ViewUtils.showKeyboard(this, mEditor.getWebView(), false);
            } break;
            case Constants.MODE_HTML : {
                mEditor.setVisibility(View.GONE);
                if (mEditor.getWebView().isHtmlRequestMade()) {
                    String htmlText = mEditor.getWebView().getEditableHtml();
                    mEditTextHtml.setText(htmlText);
                } else {
                    setProgressVisibility(true);
                    // загружаем Javascript (если нужно), и затем делаем запрос на html-текст
                    mEditor.getWebView().loadEditorJSScript(true);

                }
//                mEditor.getWebView().makeEditableHtmlRequest();
                mScrollViewHtml.setVisibility(View.VISIBLE);
                setRecordFieldsVisibility(false);
                setSubtitle(getString(R.string.subtitle_record_html));
                ViewUtils.showKeyboard(this, mEditor.getWebView(), false);
            } break;
        }
    }

    // endregion Image

    // region SaveRecord

//    /**
//     * Сохранение изменений при скрытии или выходе из активности.
//     * @param showAskDialog
//     * @param obj если null - закрываем активность, иначе - выполняем действия с объектом
//     * @return false - можно продолжать действие (н-р, закрывать активность), true - начатое
//     * действие нужно прервать, чтобы дождаться результата из диалога
//     */
//    private boolean onSaveRecord(boolean showAskDialog, ResultObj obj) {
//        if (mEditor.isEdited()) {
//            if (SettingsManager.isRecordAutoSave(this) && !mRecord.isTemp()) {
//                // сохраняем без запроса
//                return saveRecord(obj);
//            } else if (showAskDialog) {
//                // спрашиваем о сохранении, если нужно
//                RecordDialogs.saveRecord(RecordActivity.this, new Dialogs.IApplyCancelResult() {
//                    @Override
//                    public void onApply() {
//                        saveRecord(obj);
//                    }
//                    @Override
//                    public void onCancel() {
//                        onAfterSaving(obj);
//                    }
//                });
//                return true;
//            }
//        }
//        return false;
//    }

    private void askForSaving(ResultObj obj) {
        RecordDialogs.saveRecord(RecordActivity.this, new Dialogs.IApplyCancelResult() {
            @Override
            public void onApply() {
                viewModel.saveRecord(obj);
            }
            @Override
            public void onCancel() {
                viewModel.onAfterSaving(obj);
            }
        });
    }

    /**
     * Предобработка html-текста перед сохранением.
     * Выполнение кода продолжиться в функции onReceiveEditableHtml().
     */
    private void onBeforeSavingAsync() {
        mEditor.beforeSaveAsync(true);
    }

    // endregion SaveRecord

    // region Storage

    @Override
    public void afterStorageLoaded(boolean res) {
        viewModel.afterStorageLoaded();
    }

    // endregion Storage

    // region OptionsRecord

    /**
     * Отправка записи.
     */
    public void shareRecord() {
        String text = (viewModel.isHtmlMode())
                ? mEditTextHtml.getText().toString()
                : Utils.fromHtml(mEditor.getWebView().getEditableHtml()).toString();
        viewModel.shareRecord(text);
    }

    /**
     * Удаление записи.
     */
    public void deleteRecord() {
        RecordDialogs.deleteRecord(this, viewModel.getRecordName(), () -> {
            viewModel.deleteRecord();
        });
    }

    // endregion OptionsRecord

    // region Search

    /**
     * Поиск по тексту записи.
     * @param query
     */
    private void searchInRecordText(String query) {
        TetroidSuggestionProvider.saveRecentQuery(this, query);
        if (viewModel.isHtmlMode()) {
            int matches = mEditTextHtml.getSearcher().findAll(query);
            if (matches >= 0) {
                onFindTextResult(matches);
            }
        } else {
            mEditor.searchText(query);
        }
        // сбрасываем счетчик совпадений от прежнего поиска
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mFindListener.reset();
        }
    }

    private void findPrev() {
        if (viewModel.isHtmlMode()) {
            mEditTextHtml.getSearcher().prevMatch();
        } else {
            mEditor.prevMatch();
        }
    }

    private void findNext() {
        if (viewModel.isHtmlMode()) {
            mEditTextHtml.getSearcher().nextMatch();
        } else {
            mEditor.nextMatch();
        }
    }

    private void stopSearch() {
        if (viewModel.isHtmlMode()) {
            mEditTextHtml.getSearcher().stopSearch();
        } else {
            mEditor.stopSearch();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    class TextFindListener implements WebView.FindListener {
        boolean isStartSearch = true;

        @Override
        public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
            if (isDoneCounting && isStartSearch) {
                this.isStartSearch = false;
                onFindTextResult(numberOfMatches);
            }
        }

        void reset() {
            this.isStartSearch = true;
        }
    }

    public void onFindTextResult(int matches) {
        if (matches > 0) {
            LogManager.log(this, String.format(getString(R.string.log_n_matches_found), matches),
                    ILogger.Types.INFO, false, Toast.LENGTH_LONG);
        } else {
            LogManager.log(this, getString(R.string.log_matches_not_found),
                    ILogger.Types.INFO, false, Toast.LENGTH_LONG);
        }
    }

    // endregion Search


//    /**
//     * Действия перед закрытием активности, если свойства записи были изменены.
//     * @param startMainActivity
//     * @return false - можно продолжать действие (н-р, закрывать активность), true - начатое
//     *         действие нужно прервать, чтобы дождаться результата из диалога
//     */
//    private boolean onRecordFieldsIsEdited(boolean startMainActivity) {
//        if (mIsFieldsEdited) {
////                ActivityCompat.getReferrer()
//            if (getCallingActivity() != null) {
//                // закрываем активность, возвращая результат:
//                // указываем родительской активности, что нужно обновить список записей
//                Intent intent = new Intent();
//                intent.putExtra(Constants.EXTRA_IS_FIELDS_EDITED, true);
////            intent.setAction(ACTION_ADD_RECORD);
//                setResult(RESULT_OK, intent);
//            } else if (startMainActivity) {
//                // запускаем главную активность, помещая результат
//                Bundle bundle = new Bundle();
////                bundle.putString(EXTRA_OBJECT_ID, mRecord.getId());
//                if (mRecord.getNode() != null) {
//                    bundle.putInt(Constants.EXTRA_RESULT_CODE, Constants.RESULT_OPEN_NODE);
//                    bundle.putString(Constants.EXTRA_OBJECT_ID, mRecord.getNode().getId());
//                    ViewUtils.startActivity(this, MainActivity.class, bundle, Constants.ACTION_RECORD, 0, null);
//                    finish();
//                } else {
//                    Message.show(this, getString(R.string.log_record_node_is_empty), Toast.LENGTH_LONG);
//                }
//                return true;
//            } else {
//                finish();
//                return true;
//            }
//        }
//        return false;
//    }

    private boolean isFromAnotherActivity() {
//        ActivityCompat.getReferrer()
        return getCallingActivity() != null;
    }

    /**
     * Сохранение записи при любом скрытии активности.
     */
    @Override
    public void onPause() {
        viewModel.onPause();
        super.onPause();
    }

    /**
     * Обработка возвращаемого результата других активностей.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_CODE_SETTINGS_ACTIVITY) {
            if (data != null) {
                if (data.getBooleanExtra(Constants.EXTRA_IS_REINIT_STORAGE, false)) {
                    // хранилище изменено
//                    if (StorageManager.isLoaded()) {
                    if (viewModel.isLoaded()) {
                        // спрашиваем о перезагрузке хранилище, только если оно уже загружено
                        boolean isCreate = data.getBooleanExtra(Constants.EXTRA_IS_CREATE_STORAGE, false);
                        AskDialogs.showReloadStorageDialog(this, isCreate, true, () -> {
                            // перезагружаем хранилище в главной активности, если изменили путь,
                            finishWithResult(Constants.RESULT_REINIT_STORAGE, data.getExtras());
                        });
                    }
                } else if (data.getBooleanExtra(Constants.EXTRA_IS_PASS_CHANGED, false)) {
                    // пароль изменен
                    finishWithResult(Constants.RESULT_PASS_CHANGED, data.getExtras());
                }
            }
            // не гасим экран, если установили опцию
            App.checkKeepScreenOn(this);
        } else if (requestCode == Constants.REQUEST_CODE_CAMERA && resultCode == RESULT_OK) {
            viewModel.saveSelectedImages(data, true);
        } else if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            viewModel.saveSelectedImages(data, false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        checkReceivedIntent(intent);
        super.onNewIntent(intent);
    }

    /**
     * Проверка входящего Intent.
     */
    private void checkReceivedIntent(final Intent intent) {
        String action;
        if (intent == null || (action = intent.getAction()) == null) {
            return;
        }
        if (action.equals(FileObserverService.ACTION_OBSERVER_EVENT_COME)) {
            // обработка внешнего изменения дерева записей
//            mOutsideChangingHandler.run(true);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // обработка результата голосового поиска
            String query = intent.getStringExtra(SearchManager.QUERY);
            mSearchView.setQuery(query, true);
        }
    }

    // region OptionsMenu

    /**
     * Обработчик создания системного меню.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!super.onBeforeCreateOptionsMenu(menu))
            return true;
        getMenuInflater().inflate(R.menu.record, menu);
        this.optionsMenu = menu;

        initSearchView(menu);

        return super.onAfterCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!isOnCreateProcessed())
            return true;
        // режимы
        int mode = viewModel.getMCurMode();
        activateMenuItem(menu.findItem(R.id.action_record_view), mode == Constants.MODE_EDIT);
        activateMenuItem(menu.findItem(R.id.action_record_edit), mode == Constants.MODE_VIEW
            || mode == Constants.MODE_HTML);
        activateMenuItem(menu.findItem(R.id.action_record_html), mode == Constants.MODE_VIEW
                || mode == Constants.MODE_EDIT);
        activateMenuItem(menu.findItem(R.id.action_record_save), mode == Constants.MODE_EDIT);

//        boolean isLoadedFavoritesOnly = App.IsLoadedFavoritesOnly;
        boolean isLoadedFavoritesOnly = viewModel.isLoadedFavoritesOnly();
//        boolean isTemp = (mRecord == null || mRecord.isTemp());
        boolean isTemp = viewModel.isRecordTemprorary();
        activateMenuItem(menu.findItem(R.id.action_record_edit_fields), !isLoadedFavoritesOnly, !isTemp);
        activateMenuItem(menu.findItem(R.id.action_record_node), !isLoadedFavoritesOnly, !isTemp);
        activateMenuItem(menu.findItem(R.id.action_delete), !isLoadedFavoritesOnly, !isTemp);
        activateMenuItem(menu.findItem(R.id.action_attached_files), true, !isTemp);
        activateMenuItem(menu.findItem(R.id.action_cur_record_folder), true, !isTemp);
        activateMenuItem(menu.findItem(R.id.action_info), true, !isTemp);
        return super.onPrepareOptionsMenu(menu);
    }

    private void activateMenuItem(MenuItem menuItem, boolean isVisible, boolean isEnabled) {
        menuItem.setVisible(isVisible);
        menuItem.setEnabled(isEnabled);
    }

    private void activateMenuItem(MenuItem menuItem, boolean isVisible) {
        menuItem.setVisible(isVisible);
    }

    /**
     * Инициализация элементов для поиска.
     * @param menu
     */
    public void initSearchView(Menu menu) {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        this.mSearchView = (SearchView) menu.findItem(R.id.action_search_text).getActionView();
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setIconifiedByDefault(true);
        // добавлять кнопки справа в выпадающем списке предложений (suggestions), чтобы вставить выбранное
        // предложение в строку запроса для дальнейшего уточнения (изменения), а не для поиска по нему
        mSearchView.setQueryRefinementEnabled(true);
        new SearchViewXListener(mSearchView) {
            @Override
            public void onSearchClick() { }
            @Override
            public void onQuerySubmit(String query) {
                searchInRecordText(query);
                setFindButtonsVisibility(true);
            }
            @Override
            public void onQueryChange(String query) {
            }
            @Override
            public void onSuggestionSelectOrClick(String query) {
                mSearchView.setQuery(query, true);
            }
            @Override
            public void onClose() {
                setFindButtonsVisibility(false);
                stopSearch();
            }
        };
    }

    /**
     * Обработчик выбора пунктов системного меню.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_record_view:
                viewModel.switchMode(Constants.MODE_VIEW);
                return true;
            case R.id.action_record_edit:
                viewModel.switchMode(Constants.MODE_EDIT);
                return true;
            case R.id.action_record_html:
                viewModel.switchMode(Constants.MODE_HTML);
                return true;
            case R.id.action_record_save:
                viewModel.saveRecord(null);
                return true;
            case R.id.action_record_edit_fields:
                editFields(null);
                return true;
            case R.id.action_record_node:
                viewModel.showRecordNode();
                return true;
            case R.id.action_attached_files:
                viewModel.openRecordAttaches();
                return true;
            case R.id.action_cur_record_folder:
                viewModel.openRecordFolder();
                return true;
            case R.id.action_share:
                shareRecord();
                return true;
            case R.id.action_delete:
                deleteRecord();
                return true;
            case R.id.action_info:
                RecordDialogs.createRecordInfoDialog(this, viewModel.getMRecord());
                return true;
            case R.id.action_fullscreen:
                toggleFullscreen(false);
                return true;
            case R.id.action_settings:
                showActivityForResult(SettingsActivity.class, Constants.REQUEST_CODE_SETTINGS_ACTIVITY);
                return true;
            case R.id.action_storage_info:
                ViewUtils.startActivity(this, InfoActivity.class, null);
                return true;
            case android.R.id.home:
                return viewModel.onHomePressed();
        }
        return super.onOptionsItemSelected(item);
    }

    // endregion OptionsMenu

    // region RecordFields

    /**
     * Редактирование свойств записи.
     */
    private void editFields(ResultObj obj) {
        RecordDialogs.createRecordFieldsDialog(this, viewModel.getMRecord(), true, null,
                (name, tags, author, url, node, isFavor) -> {
                    viewModel.editFields(obj, name, tags, author, url, node, isFavor);
                });
    }

    /**
     * Скрытие/раскрытие панели со свойствами записи.
     */
    private void toggleRecordFieldsVisibility() {
        /*mFieldsExpanderLayout.toggle();

        if (mFieldsExpanderLayout.isExpanded()) {
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_up_white);
        } else {
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_down_white);
        }*/
        expandRecordFields(!mFieldsExpanderLayout.isExpanded());
        updateScrollButtonLocation();
    }

    private void expandRecordFields(boolean expand) {
        if (expand) {
            mFieldsExpanderLayout.expand();
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_up_white);
        } else {
            mFieldsExpanderLayout.collapse();
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_down_white);
        }
        updateScrollButtonLocation();
    }

    /**
     * Отображение или скрытие панели свойств в зависимости от настроек.
     */
    private void expandFieldsIfNeed() {
        expandRecordFields(viewModel.isNeedExpandFields());
    }

    /**
     * Управление видимостью панели со свойствами записи.
     * Полностью, вместе с кнопкой управления видимостью панели.
     * @param isVisible
     */
    private void setRecordFieldsVisibility(boolean isVisible) {
        if (isVisible) {
            mFieldsExpanderLayout.setVisibility(View.VISIBLE);
            mButtonToggleFields.show();
        } else {
            mFieldsExpanderLayout.setVisibility(View.GONE);
            mButtonToggleFields.hide();
        }
    }

    // endregion RecordFields


    @Override
    public int toggleFullscreen(boolean fromDoubleTap) {
        int res = super.toggleFullscreen(fromDoubleTap);
        if (res >= 0) {
            boolean isFullscreen = (res == 1);
            if (isFullscreen) {
                mButtonFullscreen.show();
            } else {
                mButtonFullscreen.hide();
            }
            if (viewModel.isViewMode()) {
                setRecordFieldsVisibility(!isFullscreen);
            }
        }
        return res;
    }

    /**
     * Обновление расположения кнопок скроллинга записи вниз/вверх.
     */
    private void updateScrollButtonLocation() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mButtonScrollTop.getLayoutParams();
//        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) scrollTopButton.getLayoutParams();
//        float density = getResources().getDisplayMetrics().density;
//        int fabMargin = (int) (getResources().getDimension(R.dimen.fab_small_margin) / density);

        if (mFieldsExpanderLayout.isExpanded()) {
//            params.topMargin = fabMargin;
            params.addRule(RelativeLayout.ALIGN_TOP, R.id.html_editor);
//            params.topMargin = fabMargin;
        } else {
//            int aboveButtonHeight = (int) (mButtonToggleFields.getMeasuredHeight() / density);
//            params.topMargin = fabMargin + aboveButtonHeight + fabMargin;
//            params.rightMargin = fabMargin; // для совпадения с mButtonToggleFields
            params.addRule(RelativeLayout.BELOW, R.id.button_toggle_fields);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.removeRule(RelativeLayout.ALIGN_TOP);
            }
//            params.topMargin = 0;
        }
        mButtonScrollTop.setLayoutParams(params);
    }

    /**
     * Установка видимости и позиционирования Fab кнопок при включении/выключении режима поиска.
     * @param vis
     */
    public void setFindButtonsVisibility(boolean vis) {
        ViewUtils.setFabVisibility(mButtonFindNext, vis);
        ViewUtils.setFabVisibility(mButtonFindPrev, vis);
        setRecordFieldsVisibility(!vis && viewModel.getMCurMode() != Constants.MODE_HTML);
        // установим позиционирование кнопки FindNext от правого края
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mButtonFindNext.getLayoutParams();
        if (vis) {
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.removeRule(RelativeLayout.RIGHT_OF);
            }
        } else {
            params.addRule(RelativeLayout.RIGHT_OF, R.id.button_toggle_fields);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            }
        }
        mButtonFindNext.setLayoutParams(params);
        // установим позиционирование кнопки ScrollToTop от кнопки FindNext
        if (vis) {
            params = (RelativeLayout.LayoutParams) mButtonScrollTop.getLayoutParams();
            params.addRule(RelativeLayout.BELOW, R.id.button_find_next);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.removeRule(RelativeLayout.ALIGN_TOP);
            }
            mButtonScrollTop.setLayoutParams(params);
        } else {
            updateScrollButtonLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean permGranted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        switch (requestCode) {
            case Constants.REQUEST_CODE_PERMISSION_CAMERA: {
                if (permGranted) {
                    LogManager.log(this, R.string.log_camera_perm_granted, ILogger.Types.INFO);
                    startCamera();
                } else {
                    LogManager.log(this, R.string.log_missing_camera_perm, ILogger.Types.WARNING, Toast.LENGTH_SHORT);
                }
            }
            break;
        }
    }

    public void showActivityForResult(Class<?> cls, int requestCode) {
        Intent intent = new Intent(this, cls);
        startActivityForResult(intent, requestCode);
    }

    /**
     * Формирование результата активности и ее закрытие.
     * При необходимости запуск главной активности, если текущая была запущена самостоятельно.
     * @param resCode
     * @param bundle
     */
    private void finishWithResult(int resCode, Bundle bundle) {
        if (getCallingActivity() != null) {
            if (bundle != null) {
                bundle.putInt(Constants.EXTRA_RESULT_CODE, resCode);
                Intent intent = new Intent(Constants.ACTION_RECORD);
                intent.putExtras(bundle);
                setResult(resCode, intent);
            } else {
                setResult(resCode);
            }
        } else {
            if (bundle == null) {
                bundle = new Bundle();
            }
            bundle.putInt(Constants.EXTRA_RESULT_CODE, resCode);
            ViewUtils.startActivity(this, MainActivity.class, bundle, Constants.ACTION_RECORD, 0, 0);
        }
        finish();
    }

    /**
     * Обработчик нажатия кнопки Назад.
     */
    @Override
    public void onBackPressed() {
        if (!onBeforeBackPressed()) {
            return;
        }
        // выполняем родительский метод только если не был запущен асинхронный код
        if (viewModel.isCanBack(isFromAnotherActivity())) {
            super.onBackPressed();
        }
    }

    /**
     * Обработчик события закрытия активности.
     */
    @Override
    protected void onStop() {
        // отключаем блокировку выключения экрана
        ViewUtils.setKeepScreenOn(this, false);
        super.onStop();
    }

    public void setProgressVisibility(boolean vis) {
        mHtmlProgressBar.setVisibility(ViewUtils.toVisibility(vis));
    }

    public TetroidEditor getEditor() {
        return mEditor;
    }

    /**
     * Обработчк изменения текста в окне HTML-кода записи.
     */
    private class HtmlEditTextEditTextListener implements TetroidEditText.ITetroidEditTextListener {

        @Override
        public void onAfterTextInited() {
            if (viewModel.isHtmlMode()) {
                setProgressVisibility(false);
            }
        }
        @Override
        public void onAfterTextChanged() {
            if (viewModel.isHtmlMode()) {
                mEditor.setIsEdited();
            }
        }
    }
}
