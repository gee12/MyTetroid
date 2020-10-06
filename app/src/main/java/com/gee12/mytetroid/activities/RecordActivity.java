package com.gee12.mytetroid.activities;

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
import android.text.TextUtils;
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
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.SearchView;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.htmlwysiwygeditor.IImagePicker;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.FileObserverService;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.PermissionManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TetroidLog;
import com.gee12.mytetroid.TetroidSuggestionProvider;
import com.gee12.mytetroid.crypt.CryptManager;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.HtmlHelper;
import com.gee12.mytetroid.data.NodesManager;
import com.gee12.mytetroid.data.RecordsManager;
import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.data.StorageManager;
import com.gee12.mytetroid.dialogs.AskDialogs;
import com.gee12.mytetroid.dialogs.RecordDialogs;
import com.gee12.mytetroid.fragments.SettingsFragment;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidImage;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidObject;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.ImgPicker;
import com.gee12.mytetroid.views.IntentDialog;
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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RecordActivity extends TetroidActivity implements
        EditableWebView.IPageLoadListener,
        EditableWebView.ILinkLoadListener,
        EditableWebView.IHtmlReceiveListener,
        EditableWebView.IYoutubeLinkLoadListener,
        IImagePicker,
        IColorPicker,
        ColorPickerDialogListener {

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
        Object obj;

        ResultObj(int type) {
            this.type = type;
            this.obj = null;
        }

        ResultObj(Object obj) {
            this.obj = obj;
            if (obj instanceof TetroidRecord) {
                this.type = OPEN_RECORD;
            } else if (obj instanceof TetroidNode) {
                this.type = OPEN_NODE;
            } else if (obj instanceof TetroidFile) {
                this.type = OPEN_FILE;
            } else if (obj instanceof String) {
                this.type = OPEN_TAG;
            } else {
                this.type = NONE;
            }
        }
    }

    public static final String ACTION_ADD_RECORD = "ACTION_ADD_RECORD";
    public static final int REQUEST_CODE_SETTINGS_ACTIVITY = 1;
    public static final int REQUEST_CODE_CAMERA = 2;
    public static final String EXTRA_OBJECT_ID = "EXTRA_OBJECT_ID";
    public static final String EXTRA_TAG_NAME = "EXTRA_TAG_NAME";
    public static final String EXTRA_IS_FIELDS_EDITED = "EXTRA_IS_FIELDS_EDITED";
    public static final String EXTRA_IMAGES_URI = "EXTRA_IMAGES_URI";

    public static final int MODE_VIEW = 1;
    public static final int MODE_EDIT = 2;
    public static final int MODE_HTML = 3;

    public static final int RESULT_REINIT_STORAGE = 1;
    public static final int RESULT_PASS_CHANGED = 2;
    public static final int RESULT_OPEN_RECORD = 3;
    public static final int RESULT_OPEN_NODE = 4;
    public static final int RESULT_SHOW_FILES = 5;
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
    private FloatingActionButton mButtonScrollBottom;
    private FloatingActionButton mButtonScrollTop;
    private FloatingActionButton mButtonFindNext;
    private FloatingActionButton mButtonFindPrev;
    private MenuItem mMenuItemView;
    private MenuItem mMenuItemEdit;
    private MenuItem mMenuItemHtml;
    private MenuItem mMenuItemSave;

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

        String action;
        if (mReceivedIntent == null || (action = mReceivedIntent.getAction()) == null) {
            finish();
            return;
        }
        if (action.equals(Intent.ACTION_MAIN)) {
            // открытие или создание записи из главной активности
            if (!initRecordFromMain(mReceivedIntent)) {
                finish();
                return;
            }
        } else if (action.equals(ACTION_ADD_RECORD)) {
            // создание записи из виджета
            // сначала инициализируем службы
            App.init(this);
            if (!initRecordFromWidget()) {
                finish();
                return;
            }
        } else if (action.equals(Intent.ACTION_SEND)) {
            // прием текста из другого приложения
            String type = mReceivedIntent.getType();
            if (type != null && type.startsWith("text/")) {
                String text = mReceivedIntent.getStringExtra(Intent.EXTRA_TEXT);
                if (text == null) {
                    LogManager.log(this, R.string.log_not_passed_text, LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    return;
                }
                LogManager.log(this, getString(R.string.log_receiving_intent_text), LogManager.Types.INFO);
                showIntentDialog(mReceivedIntent, text);
            } else {
                finish();
                return;
            }
        } else {
            finish();
            return;
        }

        // проверяем передавались ли изображения
        this.mIsReceivedImages = mReceivedIntent.hasExtra(EXTRA_IMAGES_URI);

        this.mEditor = findViewById(R.id.html_editor);
        mEditor.setColorPickerListener(this);
        mEditor.setImagePickerListener(this);
        mEditor.setToolBarVisibility(false);
//        mEditor.setOnTouchListener(this);
        mEditor.setOnPageLoadListener(this);
        EditableWebView webView = mEditor.getWebView();
        webView.setOnTouchListener(this);
        webView.setOnUrlLoadListener(this);
        webView.setOnHtmlReceiveListener(this);
        webView.setYoutubeLoadLinkListener(this);

//        this.mFieldsLayout = findViewById(R.id.layout_record_fields);
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

        this.mButtonScrollBottom = findViewById(R.id.button_scroll_bottom);
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
    }

    /**
     * Получение записи из хранилища, т.к. актиность была запущена из MainActivity.
     * @param intent
     * @return
     */
    private boolean initRecordFromMain(Intent intent) {
        // получаем переданную запись
        String recordId = intent.getStringExtra(EXTRA_OBJECT_ID);
        if (recordId != null) {
            // получаем запись
            this.mRecord = RecordsManager.getRecord(recordId);
            if (mRecord == null) {
                LogManager.log(this, getString(R.string.log_not_found_record) + recordId, LogManager.Types.ERROR, Toast.LENGTH_LONG);
                finish();
                return true;
            } else {
                setTitle(mRecord.getName());
            }
        } else {
            LogManager.log(this, getString(R.string.log_not_transferred_record_id), LogManager.Types.ERROR, Toast.LENGTH_LONG);
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
     * Запуск диалога с вариантами обработки переданного текста из другого приложения.
     * @param intent
     * @param text
     */
    private void showIntentDialog(Intent intent, String text) {
        setVisibilityActionHome(false);
        IntentDialog.createDialog(this, true, (receivedData) -> {
            if (receivedData.isCreate()) {
                initRecordFromSentText(intent, text);

            } else {
                // TODO: реализовать выбор имеющихся записей
            }
        });

    }

    /**
     * Создание временной записи, т.к. активность была запущена при передаче текста
     *  из другого приложения.
     * @param intent
     * @param text
     */
    private void initRecordFromSentText(Intent intent, String text) {
        // имя записи
        String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        String url = null;
        if (Build.VERSION.SDK_INT >= 17) {
            url = intent.getStringExtra(Intent.EXTRA_ORIGINATING_URI);
        }
        // создаем запись
//        TetroidRecord record = RecordsManager.createRecord(subject, url, text, node);
        this.mRecord = RecordsManager.createTempRecord(this, subject, url, text);
    }

    /**
     * Обработчик события загрузки пунктов меню.
     * Выполняется в последнюю очередь, после чего можно запускать отображение содержимого записи.
     */
    private void onMenuLoaded() {
        openRecord(mRecord);
    }

    /**
     * Отображение записи (свойств и текста).
     * @param record
     */
    public void openRecord(TetroidRecord record) {
        if (record == null)
            return;
        LogManager.log(this, getString(R.string.log_record_loading) + record.getId(), LogManager.Types.INFO);
        loadFields(record);
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
                        onTagUrlLoad(span.getURL());
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
     * Обновление поля последнего изменения записи.
     */
    private void updateEditedDate() {
        if (App.isFullVersion()) {
            String dateFormat = getString(R.string.full_date_format_string);
            Date edited = RecordsManager.getEditedDate(this, mRecord);
            ((TextView) findViewById(R.id.text_view_record_edited)).setText(
                    (edited != null) ? Utils.dateToString(edited, dateFormat) : "");
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
                    if (record.isCrypted() && CryptManager.getErrorCode() > 0) {
                        LogManager.log(this, R.string.log_error_record_file_decrypting, LogManager.Types.ERROR, Toast.LENGTH_LONG);
                    }
                }
            }
        }
        mEditTextHtml.reset();
        //mEditor.getWebView().clearAndFocusEditor();
        final String text = textHtml;
        mEditor.getWebView().loadDataWithBaseURL(RecordsManager.getUriToRecordFolder(record),
                text, "text/html", "UTF-8", null);
    }

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
        if (mIsFirstLoad) {
            this.mIsFirstLoad = false;
            // переключаем режим отображения
            int defMode = (mRecord.isNew() || mIsReceivedImages || SettingsManager.isRecordEditMode(this))
                    ? MODE_EDIT : MODE_VIEW;

            // сбрасываем флаг, т.к. уже воспользовались
            mRecord.setIsNew(false);

            switchMode(defMode);

            if (defMode == MODE_EDIT) {
                ViewUtils.showKeyboard(this, mEditor.getWebView(), false);
//                Keyboard.showKeyboard(mEditor);
            }

        } else {
            // переключаем только views
            switchViews(mCurMode);
        }
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

        // вставляем переданные изображения
        if (mIsReceivedImages) {
            List<Uri> uris = getIntent().getParcelableArrayListExtra(EXTRA_IMAGES_URI);
            saveImages(uris, false);
        }
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
            // теперь сохраняем текст заметки без вызова предварительных методов
            saveRecord(false, mResultObj);
            // переключаем режим, если асинхронное сохранение было вызвано в процессе переключения режима
            if (mModeToSwitch > 0) {
                runOnUiThread(() -> {
                    switchMode(mModeToSwitch, false);
                });
            }
        } else {
            // метод вызывается в параллельном потоке, поэтому устанавливаем текст в основном
            runOnUiThread(() -> {
                mEditTextHtml.setText(htmlText);
                mEditTextHtml.requestFocus();
//            setProgressVisibility(false);
            });
        }
    }

    /**
     * Открытие ссылки в тексте.
     * @param url
     */
    @Override
    public boolean onLinkLoad(String url) {
        // раскодируем url, т.к. он может содержать кириллицу и прочее
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        // удаляем baseUrl из строки адреса
        String baseUrl = mEditor.getWebView().getBaseUrl();
        if (baseUrl != null && url.startsWith(baseUrl)) {
            url = url.replace(baseUrl, "");
        }

        TetroidObject obj;
        if ((obj = TetroidObject.parseUrl(url)) != null) {
            // обрабатываем внутреннюю ссылку
            switch (obj.getType()) {
                case FoundType.TYPE_RECORD: {
                    TetroidRecord record = RecordsManager.getRecord(obj.getId());
                    if (record != null) {
                        openAnotherRecord(record, true);
                    } else {
                        LogManager.log(this, getString(R.string.log_not_found_record) + obj.getId(), LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    }
                    break;
                }
                case FoundType.TYPE_NODE:
                    TetroidNode node = NodesManager.getNode(obj.getId());
                    if (node != null) {
                        openAnotherNode(node, true);
                    } else {
                        LogManager.log(this, getString(R.string.log_not_found_node_id) + obj.getId(), LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    }
                    break;
                case FoundType.TYPE_TAG:
                    String tag = obj.getId();
                    if (!TextUtils.isEmpty(tag)) {
                        openTag(tag, true);
                    } else {
                        LogManager.log(this, getString(R.string.title_tag_name_is_empty), LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    }
                    break;
                case FoundType.TYPE_AUTHOR:
                case FoundType.TYPE_FILE:
                    break;
                default:
                    LogManager.log(this, getString(R.string.log_link_to_obj_parsing_error), LogManager.Types.WARNING, Toast.LENGTH_LONG);
            }
        } else {
            // обрабатываем внешнюю ссылку
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            } catch (Exception ex) {
                LogManager.log(this, ex, Toast.LENGTH_LONG);
            }
        }
        return true;
    }

    @Override
    public void onYoutubeLinkLoad(String videoId) {
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + videoId));
        startActivity(webIntent);
    }

    /**
     * Открытие метки по ссылке.
     *
     * TODO: Заменить на TetroidObject.parseUrl(url)
     *
     * @param url
     */
    private void onTagUrlLoad(String url) {
        String decodedUrl;
        // декодируем url
        try {
            decodedUrl = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LogManager.log(this, getString(R.string.log_url_decode_error) + url, ex);
            return;
        }
        if (decodedUrl.startsWith(TetroidTag.LINKS_PREFIX)) {
            // избавляемся от приставки "tag:"
            String tagName = decodedUrl.substring(TetroidTag.LINKS_PREFIX.length());
            openTag(tagName, true);
        } else {
            LogManager.log(this, getString(R.string.log_wrong_tag_link_format), LogManager.Types.WARNING, Toast.LENGTH_LONG);
        }
    }

    /**
     * Открытие другой записи по внутренней ссылке.
     * @param record
     * @param isAskForSave
     */
    private void openAnotherRecord(TetroidRecord record, boolean isAskForSave) {
        if (record == null)
            return;
        if (onSaveRecord(isAskForSave, new ResultObj(record)))
            return;
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_OBJECT_ID, record.getId());
        if (mIsFieldsEdited) {
            bundle.putBoolean(EXTRA_IS_FIELDS_EDITED, true);
        }
        finishWithResult(RESULT_OPEN_RECORD, bundle);
    }

    /**
     * Открытие другой ветки по внутренней ссылке.
     * @param node
     * @param isAskForSave
     */
    private void openAnotherNode(TetroidNode node, boolean isAskForSave) {
        if (node == null)
            return;
        if (onSaveRecord(isAskForSave, new ResultObj(node)))
            return;
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_OBJECT_ID, node.getId());
        finishWithResult(RESULT_OPEN_NODE, bundle);
    }

    /**
     * Открытие ветки записи.
     */
    private void showCurNode() {
        openAnotherNode(mRecord.getNode(), true);
    }

    /**
     * Открытие списка файлов записи.
     * @param isAskForSave
     */
    private void openRecordFiles(TetroidRecord record, boolean isAskForSave) {
        if (record == null)
            return;
        if (onSaveRecord(isAskForSave, new ResultObj(ResultObj.OPEN_FILE)))
            return;
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_OBJECT_ID, record.getId());
        finishWithResult(RESULT_SHOW_FILES, bundle);
    }

    /**
     *
     */
    private void openCurRecordFiles() {
        openRecordFiles(mRecord, true);
    }

    /**
     * Открытие записей метки в главной активности.
     * @param tagName
     * @param isAskForSave
     */
    private void openTag(String tagName, boolean isAskForSave) {
        if (onSaveRecord(isAskForSave, new ResultObj(tagName)))
            return;
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_TAG_NAME, tagName);
        if (mIsFieldsEdited) {
            bundle.putBoolean(EXTRA_IS_FIELDS_EDITED, true);
        }
        finishWithResult(RESULT_SHOW_TAG, bundle);
    }

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

    @Override
    public void startPicker() {
        ImgPicker.startPicker(this);
    }

    @Override
    public void startCamera() {
        // проверка разрешения
        if (!PermissionManager.checkCameraPermission(this, StorageManager.REQUEST_CODE_PERMISSION_CAMERA)) {
            return;
        }
        // не удалось сохранять сделанную фотографию сразу в каталог записи
        // (возникает ошибка на Android 9)
//        Intent intent = ImgPicker.createCamera(DataManager.getStoragePathBase(), mRecord.getDirName())
        Intent intent = ImagePicker.cameraOnly()
                .getIntent(this);
        startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }

    /**
     * Обработка выбранных изображений.
     * @param data
     */
    private void saveSelectedImages(Intent data, boolean isCamera) {
        List<Image> images = ImagePicker.getImages(data);
        if (images == null) {
            LogManager.log(this, getString(R.string.log_selected_files_is_missing), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            return;
        }
        List<Uri> uris = new ArrayList<>();
        for (Image image : images) {
            uris.add(Uri.fromFile(new File(image.getPath())));
        }
        saveImages(uris, isCamera);
    }

    private void saveImages(List<Uri> imageUris, boolean deleteSrcFile) {
        if (imageUris == null)
            return;
        int errorCount = 0;
        List<TetroidImage> savedImages = new ArrayList<>();
        for (Uri uri : imageUris) {
            TetroidImage savedImage = DataManager.saveImage(this, mRecord, uri, deleteSrcFile);
            if (savedImage != null) {
                savedImages.add(savedImage);
            } else {
                errorCount++;
            }
        }
        if (errorCount > 0) {
            LogManager.log(this, String.format(getString(R.string.log_failed_to_save_images_mask), errorCount),
                    LogManager.Types.WARNING, Toast.LENGTH_LONG);
        }
        if (!savedImages.isEmpty()) {
            mEditor.insertImages(savedImages);
        }
    }

    /**
     * Переключение режима отображения содержимого записи.
     * @param newMode
     */
    private void switchMode(int newMode) {
        switchMode(newMode, true);
    }

    private void switchMode(int newMode, boolean isNeedSave) {
        this.mModeToSwitch = -1;
        int oldMode = mCurMode;
        // сохраняем
//        onSaveRecord();
        boolean runBeforeSaving = false;
        if (isNeedSave && SettingsManager.isRecordAutoSave(this)) {
            // автоматически сохраняем текст записи, если:
            //  * есть изменения
            //  * не находимся в режиме HTML (сначала нужно перейти в режим EDIT (WebView), а уж потом можно сохранять)
            if (mEditor.isEdited() && mCurMode != MODE_HTML) {
                runBeforeSaving = saveRecord(null);
            }
        }
        // если асинхронно запущена предобработка сохранения, то выходим
        if (runBeforeSaving) {
            this.mModeToSwitch = newMode;
            return;
        }
        // перезагружаем html-текст записи в webView, если был режим редактирования HTML
        if (oldMode == MODE_HTML) {
            loadRecordText(mRecord, true);
        }
        // переключаем элементы интерфейса, только если не редактировали только что html,
        // т.к. тогда вызов switchViews() должен произойти уже после перезагрузки страницы
        // на событии onPageLoaded())
//        if (oldMode != MODE_HTML) {
        else {
            switchViews(newMode);
        }
        this.mCurMode = newMode;
    }

    /**
     * Сохранение изменений при скрытии или выходе из активности.
     * @param showAskDialog
     * @param obj если null - закрываем активность, иначе - выполняем действия с объектом
     * @return false - можно продолжать действие (н-р, закрывать активность), true - начатое
     * действие нужно прервать, чтобы дождаться результата из диалога
     */
    private boolean onSaveRecord(boolean showAskDialog, ResultObj obj) {
        if (mEditor.isEdited()) {
            if (SettingsManager.isRecordAutoSave(this)) {
                // сохраняем без запроса
                return saveRecord(obj);
            } else if (showAskDialog) {
                // спрашиваем о сохранении, если нужно
                RecordDialogs.saveRecord(RecordActivity.this, new Dialogs.IApplyCancelResult() {
                    @Override
                    public void onApply() {
                        saveRecord(obj);
                    }
                    @Override
                    public void onCancel() {
                        onAfterSaving(obj);
                    }
                });
                return true;
            }
        }
        return false;
    }

    /**
     * Сохранение html-текста записи в файл.
     * @return true - запущена ли перед сохранением предобработка в асинхронном режиме.
     */
    private boolean saveRecord(ResultObj obj) {
        boolean runBeforeSaving = SettingsManager.isFixEmptyParagraphs(this);
        if (runBeforeSaving) {
            this.mResultObj = obj;
        }
        return saveRecord(runBeforeSaving, obj) || runBeforeSaving;
    }

    /**
     * Сохранение html-текста записи в файл с предобработкой.
     * @param callBefore Нужно ли перед сохранением совершить какие-либы манипуляции с html ?
     * @return true - запущен ли код в асинхронном режиме.
     */
    private boolean saveRecord(boolean callBefore, ResultObj obj) {
        if (callBefore) {
            onBeforeSavingAsync();
            return true;
        } else {
            if (mRecord.isTemp()) {
                if (DataManager.isLoaded()) {
                    this.runOnUiThread(() -> editFields(obj));
                } else {
                    this.mIsSaveTempAfterStorageLoaded = true;
                    this.runOnUiThread(() -> loadStorage());
                }
                return true;
            } else {
                save();
                onAfterSaving(obj);
            }
        }
        return false;
    }

    private void loadStorage() {
        StorageManager.setStorageCallback(this);
        StorageManager.startInitStorage(this, true);
    }

    @Override
    public void afterStorageLoaded(boolean res) {
        if (mIsSaveTempAfterStorageLoaded) {
            this.mIsSaveTempAfterStorageLoaded = false;
            // сохраняем временную запись
            editFields(mResultObj);
//            this.runOnUiThread(() -> editFields());
        }
    }

    /**
     * Предобработка html-текста перед сохранением.
     * Выполнение кода продолжиться в функции onReceiveEditableHtml().
     */
    private void onBeforeSavingAsync() {
        mEditor.beforeSaveAsync(true);
    }

    /**
     * Получение актуального html-текста записи из WebView и непосредственное сохранение в файл.
     */
    private void save() {
        LogManager.log(this, getString(R.string.log_before_record_save) + mRecord.getId(), LogManager.Types.INFO);
        String htmlText = mEditor.getDocumentHtml();
//        String htmlText = (mCurMode == MODE_HTML)
//                ? TetroidEditor.getDocumentHtml(mEditTextHtml.getText().toString()) : mEditor.getDocumentHtml();
        if (RecordsManager.saveRecordHtmlText(this, mRecord, htmlText)) {
//            TetroidLog.logOperRes(TetroidLog.Objs.RECORD, TetroidLog.Opers.SAVE);
            LogManager.log(this, R.string.log_record_saved, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            // сбрасываем пометку изменения записи
            mEditor.setIsEdited(false);
            updateEditedDate();
        } else {
            TetroidLog.logOperErrorMore(this, TetroidLog.Objs.RECORD, TetroidLog.Opers.SAVE);
        }
    }

    /**
     * Обработчик события после сохранения записи, вызванное при ответе на запрос сохранения в диалоге.
     * @param resObj
     */
    public void onAfterSaving(ResultObj resObj) {
        if (resObj == null) {
            resObj = new ResultObj(null);
        }
        switch (resObj.type) {
            case ResultObj.EXIT:
            case ResultObj.START_MAIN_ACTIVITY:
                if (!onRecordFieldsIsEdited(resObj.type == ResultObj.START_MAIN_ACTIVITY)) {
                    finish();
                }
                break;
            case ResultObj.OPEN_RECORD:
                openAnotherRecord((TetroidRecord) resObj.obj, false);
                break;
            case ResultObj.OPEN_NODE:
                openAnotherNode((TetroidNode) resObj.obj, false);
                break;
            case ResultObj.OPEN_FILE:
                openRecordFiles(mRecord, false);
                break;
            case ResultObj.OPEN_TAG:
                openTag((String) resObj.obj, false);
                break;
            case ResultObj.NONE:
                break;
        }
    }

    /**
     *
     * @param newMode
     */
    private void switchViews(int newMode) {
        switch (newMode) {
            case MODE_VIEW : {
                mEditor.setVisibility(View.VISIBLE);
                mEditor.setToolBarVisibility(false);
                mScrollViewHtml.setVisibility(View.GONE);
                setRecordFieldsVisibility(true);
                mMenuItemView.setVisible(false);
                mMenuItemEdit.setVisible(true);
                mMenuItemHtml.setVisible(true);
                mMenuItemSave.setVisible(false);
                mEditor.setEditMode(false);
                mEditor.setScrollButtonsVisibility(true);
                setSubtitle(getString(R.string.subtitle_record_view));
                ViewUtils.hideKeyboard(this, mEditor.getWebView());
            } break;
            case MODE_EDIT : {
                mEditor.setVisibility(View.VISIBLE);
                // загружаем Javascript (если нужно)
//                if (!mEditor.getWebView().isEditorJSLoaded()) {
//                    setProgressVisibility(true);
//                }
                mEditor.getWebView().loadEditorJSScript(false);

                mEditor.setToolBarVisibility(true);
                mScrollViewHtml.setVisibility(View.GONE);
                setRecordFieldsVisibility(false);
                mMenuItemView.setVisible(true);
                mMenuItemEdit.setVisible(false);
                mMenuItemHtml.setVisible(true);
                mMenuItemSave.setVisible(true);
                mEditor.setEditMode(true);
                mEditor.setScrollButtonsVisibility(false);
                setSubtitle(getString(R.string.subtitle_record_edit));
                mEditor.getWebView().focusEditor();
            } break;
            case MODE_HTML : {
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
                mMenuItemView.setVisible(false);
                mMenuItemEdit.setVisible(true);
                mMenuItemHtml.setVisible(false);
                mMenuItemSave.setVisible(false);
                setSubtitle(getString(R.string.subtitle_record_html));
            } break;
        }
    }

    @Override
    public boolean toggleFullscreen(boolean fromDoubleTap) {
        boolean isFullscreen = super.toggleFullscreen(fromDoubleTap);
        if (isFullscreen)
            mButtonFullscreen.show();
        else
            mButtonFullscreen.hide();
        if (mCurMode == MODE_VIEW) {
            setRecordFieldsVisibility(!isFullscreen);
        }
        return isFullscreen;
    }

    /**
     * Открытие каталога записи.
     */
    public void openRecordFolder() {
        if (!RecordsManager.openRecordFolder(this, mRecord)) {
            LogManager.log(this, R.string.log_missing_file_manager, Toast.LENGTH_LONG);
        }
    }


    /**
     * Отправка записи.
     */
    public void shareRecord() {
        String text = (mCurMode == MODE_HTML)
                ? mEditTextHtml.getText().toString()
                : Utils.fromHtml(mEditor.getWebView().getEditableHtml()).toString();

        // FIXME: если получать текст из html-кода ВСЕЙ страницы,
        //  то Html.fromHtml() неверно обрабатывает код стиля в шапке:
        //  <header><style> p, li { white-space: pre-wrap; } </style></header>
        //  добавляя его в результат
        String except = "p, li { white-space: pre-wrap; } ";
        if (text.startsWith(except)) {
            text = text.substring(except.length());
        }

        DataManager.shareText(this, mRecord.getName(), text);
    }

    /**
     * Удаление записи.
     */
    public void deleteRecord() {
        RecordDialogs.deleteRecord(this, () -> {
            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_OBJECT_ID, mRecord.getId());
            finishWithResult(RESULT_DELETE_RECORD, bundle);
        });
    }

    /**
     * Редактирование свойств записи.
     */
    private void editFields(ResultObj obj) {
        boolean wasTemp = mRecord.isTemp();
        RecordDialogs.createRecordFieldsDialog(this, mRecord, true, null, (name, tags, author, url, node, isFavor) -> {
            if (RecordsManager.editRecordFields(this, mRecord, name, tags, author, url, node, isFavor)) {
                this.mIsFieldsEdited = true;
                setTitle(name);
                loadFields(mRecord);
                if (wasTemp) {
                    // сохраняем текст записи
//                    save();
                    saveRecord(obj);
//                    TetroidLog.logOperRes(TetroidLog.Objs.TEMP_RECORD, TetroidLog.Opers.SAVE);
                    // показываем кнопку Home для возврата в ветку записи
                    setVisibilityActionHome(true);
                } else {
//                    TetroidLog.logOperRes(TetroidLog.Objs.RECORD_FIELDS, TetroidLog.Opers.CHANGE);
                    LogManager.log(this, R.string.log_record_fields_changed, LogManager.Types.INFO, Toast.LENGTH_SHORT);
                }
            } else {
                if (wasTemp) {
                    // все равно сохраняем текст записи
//                    save();
                    saveRecord(obj);
                    TetroidLog.logOperErrorMore(this, TetroidLog.Objs.TEMP_RECORD, TetroidLog.Opers.SAVE);
                } else {
                    TetroidLog.logOperErrorMore(this, TetroidLog.Objs.RECORD_FIELDS, TetroidLog.Opers.CHANGE);
                }
            }
        });
    }

    /**
     * Поиск по тексту записи.
     * @param query
     */
    private void searchInRecordText(String query) {
        TetroidSuggestionProvider.saveRecentQuery(this, query);
        if (mCurMode == MODE_HTML) {
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
        if (mCurMode == MODE_HTML) {
            mEditTextHtml.getSearcher().prevMatch();
        } else {
            mEditor.prevMatch();
        }
    }

    private void findNext() {
        if (mCurMode == MODE_HTML) {
            mEditTextHtml.getSearcher().nextMatch();
        } else {
            mEditor.nextMatch();
        }
    }

    private void stopSearch() {
        if (mCurMode == MODE_HTML) {
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
            } else {

            }
        }

        void reset() {
            this.isStartSearch = true;
        }
    }

    public void onFindTextResult(int matches) {
        if (matches > 0) {
            LogManager.log(this, String.format(getString(R.string.log_n_matches_found), matches),
                    LogManager.Types.INFO, false, Toast.LENGTH_LONG);
        } else {
            LogManager.log(this, getString(R.string.log_matches_not_found),
                    LogManager.Types.INFO, false, Toast.LENGTH_LONG);
        }
    }

    /**
     * Действия перед закрытием активности, если свойства записи были изменены.
     * @param startMainActivity
     * @return false - можно продолжать действие (н-р, закрывать активность), true - начатое
     *         действие нужно прервать, чтобы дождаться результата из диалога
     */
    private boolean onRecordFieldsIsEdited(boolean startMainActivity) {
        if (mIsFieldsEdited) {
//                ActivityCompat.getReferrer()
            if (getCallingActivity() != null) {
                // закрываем активность, возвращая результат:
                // указываем родительской активности, что нужно обновить список записей
                Intent intent = new Intent();
                intent.putExtra(EXTRA_IS_FIELDS_EDITED, true);
//            intent.setAction(ACTION_ADD_RECORD);
                setResult(RESULT_OK, intent);
            } else if (startMainActivity) {
                // запускаем главную активность, помещая результат
                Bundle bundle = new Bundle();
                bundle.putString(EXTRA_OBJECT_ID, mRecord.getId());
                ViewUtils.startActivity(this, MainActivity.class, bundle, ACTION_ADD_RECORD, 0, null);
                finish();
                return true;
            } else {
                finish();
                return true;
            }
        }
        return false;
    }

    /**
     * Сохранение записи при любом скрытии активности.
     */
    @Override
    public void onPause() {
        onSaveRecord(false, null);
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

        if (requestCode == REQUEST_CODE_SETTINGS_ACTIVITY) {
            if (data != null) {
                if (data.getBooleanExtra(SettingsFragment.EXTRA_IS_REINIT_STORAGE, false)) {
                    boolean isCreate = data.getBooleanExtra(SettingsFragment.EXTRA_IS_CREATE_STORAGE, false);
                    AskDialogs.showReloadStorageDialog(this, isCreate, true, () -> {
                        // перезагружаем хранилища в главной активности, если изменили путь
                        finishWithResult(RESULT_REINIT_STORAGE, data.getExtras());
                    });
                } else if (data.getBooleanExtra(SettingsFragment.EXTRA_IS_PASS_CHANGED, false)) {
                    finishWithResult(RESULT_PASS_CHANGED, data.getExtras());
                }
            }
            // не гасим экран, если установили опцию
            App.checkKeepScreenOn(this);
        } else if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK) {
            saveSelectedImages(data, true);
        } else if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            saveSelectedImages(data, false);
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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isLoadedFavoritesOnly = App.IsLoadedFavoritesOnly;
        boolean isTemp = mRecord.isTemp();
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

    /**
     * Обработчик создания системного меню.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.record, menu);
        this.mMenuItemView = menu.findItem(R.id.action_record_view);
        this.mMenuItemEdit = menu.findItem(R.id.action_record_edit);
        this.mMenuItemHtml = menu.findItem(R.id.action_record_html);
        this.mMenuItemSave = menu.findItem(R.id.action_record_save);
        initSearchView(menu);

        // для отображения иконок
        if (menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }
        onMenuLoaded();
        return true;
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
     * Скрытие/раскрытие панели со свойствами записи.
     */
    private void toggleRecordFieldsVisibility() {
        mFieldsExpanderLayout.toggle();

        if (mFieldsExpanderLayout.isExpanded()) {
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_up_white);
        } else {
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_down_white);
        }
        updateScrollButtonLocation();
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
     * Управление видимостью панели со свойствами записи.
     * @param isVisible
     */
    private void setRecordFieldsVisibility(boolean isVisible) {
//        mFieldsExpanderLayout.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
        if (isVisible) {
            mFieldsExpanderLayout.setVisibility(View.VISIBLE);
            mButtonToggleFields.show();
        } else {
            mFieldsExpanderLayout.setVisibility(View.GONE);
            mButtonToggleFields.hide();
        }
    }

    /**
     * Установка видимости и позиционирования Fab кнопок при включении/выключении режима поиска.
     * @param vis
     */
    public void setFindButtonsVisibility(boolean vis) {
        ViewUtils.setFabVisibility(mButtonFindNext, vis);
        ViewUtils.setFabVisibility(mButtonFindPrev, vis);
        setRecordFieldsVisibility(!vis);
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
                switchMode(MODE_VIEW);
                return true;
            case R.id.action_record_edit:
                switchMode(MODE_EDIT);
                return true;
            case R.id.action_record_html:
                switchMode(MODE_HTML);
                return true;
            case R.id.action_record_save:
                saveRecord(null);
                return true;
            case R.id.action_record_edit_fields:
                editFields(null);
                return true;
            case R.id.action_record_node:
                showCurNode();
                return true;
            case R.id.action_attached_files:
                openCurRecordFiles();
                return true;
            case R.id.action_cur_record_folder:
                openRecordFolder();
                return true;
            case R.id.action_share:
                shareRecord();
                return true;
            case R.id.action_delete:
                deleteRecord();
                return true;
            case R.id.action_info:
                RecordDialogs.createRecordInfoDialog(this, mRecord);
                return true;
            case R.id.action_fullscreen:
                toggleFullscreen(false);
                return true;
            case R.id.action_settings:
                showActivityForResult(SettingsActivity.class, REQUEST_CODE_SETTINGS_ACTIVITY);
                return true;
            case R.id.action_storage_info:
                ViewUtils.startActivity(this, InfoActivity.class, null);
                return true;
            case android.R.id.home:
                if (!onSaveRecord(true, new ResultObj(ResultObj.START_MAIN_ACTIVITY))) {
                    // не был запущен асинхронный код при сохранении, поэтому
                    //  можем выполнить стандартный обработчик кнопки home (должен быть возврат false),
                    //  но после проверки изменения свойств (если изменены, то включится другой механизм
                    //  выхода из активности)
                    return onRecordFieldsIsEdited(true);
                }
                // был запущен асинхронный код при сохранении,
                //  поэтому не выполняем стандартный обработчик кнопки home
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean permGranted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        switch (requestCode) {
            case StorageManager.REQUEST_CODE_PERMISSION_WRITE_STORAGE: {
                if (permGranted) {
                    LogManager.log(this, R.string.log_write_ext_storage_perm_granted, LogManager.Types.INFO);
                    StorageManager.startInitStorage(this, false);
                } else {
                    LogManager.log(this, R.string.log_missing_read_ext_storage_permissions, LogManager.Types.WARNING, Toast.LENGTH_SHORT);
                }
            }
            break;
            case StorageManager.REQUEST_CODE_PERMISSION_CAMERA: {
                if (permGranted) {
                    LogManager.log(this, R.string.log_camera_perm_granted, LogManager.Types.INFO);
                    startCamera();
                } else {
                    LogManager.log(this, R.string.log_missing_camera_perm, LogManager.Types.WARNING, Toast.LENGTH_SHORT);
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
     * @param bundle
     */
    private void finishWithResult(int resCode, Bundle bundle) {
        if (bundle != null) {
            Intent intent = new Intent();
            intent.putExtras(bundle);
            setResult(resCode, intent);
        } else {
            setResult(resCode);
        }
        finish();
    }

    /**
     * Обработчик нажатия кнопки Назад.
     */
    @Override
    public void onBackPressed() {
        // выполняем родительский метод только если не был запущен асинхронный код
        if (!onSaveRecord(true, new ResultObj(ResultObj.EXIT))) {
            if (!onRecordFieldsIsEdited(false)) {
                super.onBackPressed();
            }
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

    /**
     *
     */
    private class HtmlEditTextEditTextListener implements TetroidEditText.ITetroidEditTextListener {

        @Override
        public void onAfterTextInited() {
            if (mCurMode == MODE_HTML) {
                setProgressVisibility(false);
            }
        }
        @Override
        public void onAfterTextChanged() {
            if (mCurMode == MODE_HTML) {
                mEditor.setIsEdited();
            }
        }
    }
}
