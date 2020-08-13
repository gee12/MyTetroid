package com.gee12.mytetroid.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.gee12.htmlwysiwygeditor.IImagePicker;
import com.gee12.mytetroid.App;
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
import com.gee12.mytetroid.dialogs.AskDialogs;
import com.gee12.mytetroid.dialogs.RecordAskDialogs;
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
import com.gee12.mytetroid.views.SearchViewXListener;
import com.gee12.mytetroid.views.TetroidEditText;
import com.gee12.mytetroid.views.TetroidEditor;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lumyjuwon.richwysiwygeditor.RichEditor.EditableWebView;

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
        IImagePicker {

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

    public static final int REQUEST_CODE_PERMISSION_CAMERA = 1;

    private ExpandableLayout mFieldsExpanderLayout;
    private FloatingActionButton mButtonToggleFields;
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

    private TetroidRecord mRecord;
    private int mCurMode;
    private boolean mIsFirstLoad = true;
    private boolean mIsFieldsEdited;
    private TextFindListener mFindListener;
    private SearchView mSearchView;
    private boolean mIsReceivedImages;


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

        Intent intent = getIntent();
        String action;
        if (intent == null || (action = intent.getAction()) == null) {
            finish();
            return;
        }
        if (action.equals(Intent.ACTION_MAIN)) {
            // получаем переданную запись
            String recordId = intent.getStringExtra(EXTRA_OBJECT_ID);
            if (recordId != null) {
                // получаем запись
                this.mRecord = RecordsManager.getRecord(recordId);
                if (mRecord == null) {
                    LogManager.log(getString(R.string.log_not_found_record) + recordId, LogManager.Types.ERROR, Toast.LENGTH_LONG);
                    finish();
                    return;
                } else {
                    setTitle(mRecord.getName());
                }
            } else {
                LogManager.log(getString(R.string.log_not_transferred_record_id), LogManager.Types.ERROR, Toast.LENGTH_LONG);
                finish();
                return;
            }
        } else {
            finish();
            return;
        }

        // проверяем передавались ли изображения
        this.mIsReceivedImages = intent.hasExtra(EXTRA_IMAGES_URI);

        this.mEditor = findViewById(R.id.html_editor);
        mEditor.setImgPickerCallback(this);
        mEditor.setToolBarVisibility(false);
//        mEditor.setOnTouchListener(this);
        mEditor.setOnPageLoadListener(this);
        EditableWebView webView = mEditor.getWebView();
//        webView.setOnTouchListener(this);
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

    private void onMenuLoaded() {
        openRecord(mRecord);
    }

    /**
     * Отображение записи
     */
    public void openRecord(TetroidRecord record) {
        if (record == null)
            return;
        LogManager.log(getString(R.string.log_record_loading) + record.getId(), LogManager.Types.INFO);
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

        TextView tvAuthor = findViewById(R.id.text_view_record_author);
        TextView tvUrl = findViewById(R.id.text_view_record_url);
        TextView tvCreated = findViewById(R.id.text_view_record_created);
        TextView tvEdited = findViewById(R.id.text_view_record_edited);
        int id = R.id.label_record_tags;
        // метки
        String tagsHtml = HtmlHelper.createTagsHtmlString(record);
        if (tagsHtml != null) {
            // указываем charset в mimeType для кириллицы
//            mWebViewTags.loadDataWithBaseURL(null, tagsHtml, "text/html", "UTF-8", null);

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

//            id = R.id.web_view_record_tags;
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
        tvAuthor.setLayoutParams(params2);
        // поля
        tvAuthor.setText(record.getAuthor());
        tvUrl.setText(record.getUrl());
//        if (record.getCreated() != null) {
//            tvCreated.setText(record.getCreatedString(getString(R.string.full_date_format_string)));
//        }
        String dateFormat = getString(R.string.full_date_format_string);
        Date created = record.getCreated();
        tvCreated.setText((created != null) ? Utils.dateToString(created, dateFormat) : "");
        Date edited = RecordsManager.getEditedDate(this, record);
        tvEdited.setText((edited != null) ? Utils.dateToString(edited, dateFormat) : "");
    }

    /**
     * Загрузка html-кода записи в WebView.
     * @param record
     */
    private void loadRecordText(TetroidRecord record, boolean fromHtmlEditor) {
        // 1) если только что вернулись из редактора html-кода (fromHtmlEditor)
        // и не используется авто-сохранение изменений, то загружаем html-код из редактора
        // 2) если нет, то загружаем html-код из файла записи
        String textHtml = null;
        if (fromHtmlEditor && !SettingsManager.isRecordAutoSave()) {
            textHtml = mEditTextHtml.getText().toString();
        } else {
            if (!record.isNew()) {
                textHtml = RecordsManager.getRecordHtmlTextDecrypted(record, Toast.LENGTH_LONG);
                if (textHtml == null) {
                    if (record.isCrypted() && CryptManager.getErrorCode() > 0) {
                        LogManager.log(R.string.log_error_record_file_decrypting, LogManager.Types.ERROR, Toast.LENGTH_LONG);
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
            int defMode = (mRecord.isNew() || mIsReceivedImages || SettingsManager.isRecordEditMode())
                    ? MODE_EDIT : MODE_VIEW;

            // сбрасываем флаг, т.к. уже воспользовались
            mRecord.setIsNew(false);

            switchMode(defMode);
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

    @Override
    public void onReceiveEditableHtml(String htmlText) {
        // метод вызывается в параллельном потоке, поэтому устанавливаем текст в основном
        runOnUiThread(() -> {
            mEditTextHtml.setText(htmlText);
            mEditTextHtml.requestFocus();
//            setProgressVisibility(false);
        });
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
                        LogManager.log(getString(R.string.log_not_found_record) + obj.getId(), LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    }
                    break;
                }
                case FoundType.TYPE_NODE:
                    TetroidNode node = NodesManager.getNode(obj.getId());
                    if (node != null) {
                        openAnotherNode(node, true);
                    } else {
                        LogManager.log(getString(R.string.log_not_found_node_id) + obj.getId(), LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    }
                    break;
                case FoundType.TYPE_TAG:
                    String tag = obj.getId();
                    if (!TextUtils.isEmpty(tag)) {
                        openTag(tag, true);
                    } else {
                        LogManager.log(getString(R.string.title_tag_name_is_empty), LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    }
                    break;
                case FoundType.TYPE_AUTHOR:
                case FoundType.TYPE_FILE:
                    break;
                default:
                    LogManager.log(getString(R.string.log_link_to_obj_parsing_error), LogManager.Types.WARNING, Toast.LENGTH_LONG);
            }
        } else {
            // обрабатываем внешнюю ссылку
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            } catch (Exception ex) {
                LogManager.log(ex, Toast.LENGTH_LONG);
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
            LogManager.log(getString(R.string.log_url_decode_error) + url, ex);
            return;
        }
        if (decodedUrl.startsWith(TetroidTag.LINKS_PREFIX)) {
            // избавляемся от приставки "tag:"
            String tagName = decodedUrl.substring(TetroidTag.LINKS_PREFIX.length());
            openTag(tagName, true);
        } else {
            LogManager.log(getString(R.string.log_wrong_tag_link_format), LogManager.Types.WARNING, Toast.LENGTH_LONG);
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
        if (onSaveRecord(isAskForSave, record))
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
        if (onSaveRecord(isAskForSave, node))
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
        if (onSaveRecord(isAskForSave, new TetroidFile()))
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
        if (onSaveRecord(isAskForSave, tagName))
            return;
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_TAG_NAME, tagName);
        if (mIsFieldsEdited) {
            bundle.putBoolean(EXTRA_IS_FIELDS_EDITED, true);
        }
        finishWithResult(RESULT_SHOW_TAG, bundle);
    }

    private void toggleRecordFieldsVisibility() {
        mFieldsExpanderLayout.toggle();

        if (mFieldsExpanderLayout.isExpanded()) {
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_up_white);
        } else {
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_down_white);
        }
        updateScrollButtonLocation();
    }

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

    private void saveRecord() {
        LogManager.log(getString(R.string.log_before_record_save) + mRecord.getId(), LogManager.Types.INFO);
        String htmlText = (mCurMode == MODE_HTML)
                ? TetroidEditor.getDocumentHtml(mEditTextHtml.getText().toString()) : mEditor.getDocumentHtml();
        if (RecordsManager.saveRecordHtmlText(mRecord, htmlText)) {
//            LogManager.log(getString(R.string.log_record_saved), LogManager.Types.INFO, Toast.LENGTH_SHORT);
            TetroidLog.logOperRes(TetroidLog.Objs.RECORD, TetroidLog.Opers.SAVE);
            // сбрасываем пометку изменения записи
            mEditor.setIsEdited(false);
        } else {
//            LogManager.log(getString(R.string.log_record_save_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            TetroidLog.logOperErrorMore(TetroidLog.Objs.RECORD, TetroidLog.Opers.SAVE);
        }
    }

    @Override
    public void startPicker() {
        ImgPicker.startPicker(this);
    }

    @Override
    public void startCamera() {
        // проверка разрешения
        if (!PermissionManager.checkCameraPermission(this, REQUEST_CODE_PERMISSION_CAMERA)) {
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
            LogManager.log(getString(R.string.log_selected_files_is_missing), LogManager.Types.ERROR, Toast.LENGTH_LONG);
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
            LogManager.log(String.format(getString(R.string.log_failed_to_save_images_mask), errorCount),
                    LogManager.Types.WARNING, Toast.LENGTH_LONG);
        }
        if (!savedImages.isEmpty()) {
            mEditor.insertImages(savedImages);
        }
    }

    /**
     * Поиск по тексту записи.
     * @param query
     * @param record
     */
    private void searchInText(String query, TetroidRecord record) {
        //
        // TODO: реализовать поиск по тексту записи
        //
        LogManager.log(String.format(getString(R.string.search_text_by_query), record.getName(), query));
    }

    /**
     * Переключение режима отображения содержимого записи.
     * @param newMode
     */
    private void switchMode(int newMode) {
        int oldMode = mCurMode;
        // сохраняем
        onSaveRecord();
        // перезагружаем текст записи в webView, если меняли вручную html
        if (oldMode == MODE_HTML) {
            loadRecordText(mRecord, true);
        }
        // переключаем элементы интерфейса, только если не редактировали только что html,
        // т.к. тогда вызов switchViews() должен произойти уже после перезагрузки страницы
        // на событии onPageLoaded())
        if (oldMode != MODE_HTML) {
            switchViews(newMode);
        }
        this.mCurMode = newMode;
    }

    /**
     * Сохранение изменений при смене режима.
     */
    private void onSaveRecord() {
        if (SettingsManager.isRecordAutoSave()) {
            if (mEditor.isEdited()) {
                saveRecord();
            }
        }
    }

    /**
     * Сохранение изменений при скрытии или выходе из активности.
     * @param showAskDialog
     * @param obj если null - закрываем активность, иначе - выполняем действия с объектом
     * @return false - можно продолжать действие (н-р, закрывать активность), true - начатое
     * действие нужно прервать, чтобы дождаться результата из диалога
     */
    private boolean onSaveRecord(boolean showAskDialog, Object obj) {
        if (mEditor.isEdited()) {
            if (SettingsManager.isRecordAutoSave()) {
                // сохраняем без запроса
                saveRecord();
            } else if (showAskDialog) {
                // спрашиваем о сохранении, если нужно
                RecordAskDialogs.saveRecord(RecordActivity.this, new AskDialogs.IApplyCancelResult() {
                    @Override
                    public void onApply() {
                        saveRecord();
                        onAfterSaving(obj);
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
     *
     * @param obj
     */
    public void onAfterSaving(Object obj) {
        if (obj == null) {
            setResultFieldsEdited();
            finish();
        } else if (obj instanceof TetroidRecord) {
            openAnotherRecord((TetroidRecord) obj, false);
        } else if (obj instanceof TetroidNode) {
            openAnotherNode((TetroidNode) obj, false);
        } else if (obj instanceof TetroidFile) {
            openRecordFiles(mRecord, false);
        } else if (obj instanceof String) {
            openTag((String) obj, false);
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
                setSubtitle(getString(R.string.subtitle_record_html));
            } break;
        }
    }

    @Override
    public boolean toggleFullscreen() {
        boolean isFullscreen = super.toggleFullscreen();
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
            LogManager.log(R.string.log_missing_file_manager, Toast.LENGTH_LONG);
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
        RecordAskDialogs.deleteRecord(this, () -> {
            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_OBJECT_ID, mRecord.getId());
            finishWithResult(RESULT_DELETE_RECORD, bundle);
        });
    }

    private void editFields() {
        RecordAskDialogs.createRecordFieldsDialog(this, mRecord, (name, tags, author, url) -> {
            if (RecordsManager.editRecordFields(mRecord, name, tags, author, url)) {
                this.mIsFieldsEdited = true;
                setTitle(name);
                loadFields(mRecord);
                TetroidLog.logOperRes(TetroidLog.Objs.RECORD_FIELDS, TetroidLog.Opers.CHANGE);
            } else {
                TetroidLog.logOperErrorMore(TetroidLog.Objs.RECORD_FIELDS, TetroidLog.Opers.CHANGE);
            }
        });
    }

    /**
     * Поиск по тексту записи..
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
            LogManager.log(String.format(getString(R.string.log_n_matches_found), matches),
                    LogManager.Types.INFO, false, Toast.LENGTH_LONG);
        } else {
            LogManager.log(getString(R.string.log_matches_not_found),
                    LogManager.Types.INFO, false, Toast.LENGTH_LONG);
        }
    }

    /**
     * Формирование результата активности в виде указания для родительской активности
     * обновить список записей и меток.
     */
    private void setResultFieldsEdited() {
        if (mIsFieldsEdited) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_IS_FIELDS_EDITED, true);
            setResult(RESULT_OK, intent);
        }
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
                if (data.getBooleanExtra(SettingsActivity.EXTRA_IS_REINIT_STORAGE, false)) {
                    boolean isCreate = data.getBooleanExtra(SettingsActivity.EXTRA_IS_CREATE_STORAGE, false);
                    AskDialogs.showReloadStorageDialog(this, isCreate, true, () -> {
                        // перезагружаем хранилища в главной активности, если изменили путь
                        finishWithResult(RESULT_REINIT_STORAGE, data.getExtras());
                    });
                } else if (data.getBooleanExtra(SettingsActivity.EXTRA_IS_PASS_CHANGED, false)) {
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isLoadedFavoritesOnly = App.IsLoadedFavoritesOnly;
        activateMenuItem(menu.findItem(R.id.action_record_edit_fields), !isLoadedFavoritesOnly);
        activateMenuItem(menu.findItem(R.id.action_record_node), !isLoadedFavoritesOnly);
        activateMenuItem(menu.findItem(R.id.action_delete), !isLoadedFavoritesOnly);
        return super.onPrepareOptionsMenu(menu);
    }

    private void activateMenuItem(MenuItem menuItem, boolean isActivate) {
        menuItem.setVisible(isActivate);
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
                saveRecord();
                return true;
            case R.id.action_record_edit_fields:
                editFields();
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
                RecordAskDialogs.createRecordInfoDialog(this, mRecord);
                return true;
            case R.id.action_fullscreen:
                toggleFullscreen();
                return true;
            case R.id.action_settings:
                showActivityForResult(SettingsActivity.class, REQUEST_CODE_SETTINGS_ACTIVITY);
                return true;
            case R.id.action_storage_info:
                ViewUtils.startActivity(this, InfoActivity.class, null);
                return true;
            case R.id.action_about_app:
                ViewUtils.startActivity(this, AboutActivity.class, null);
                return true;
            case android.R.id.home:
                boolean res = onSaveRecord(true, null);
                if (!res) {
                    // указываем родительской активности, что нужно обновить список записей
                    setResultFieldsEdited();
                }
                return res;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        boolean permGranted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_CAMERA: {
                if (permGranted) {
                    LogManager.log(R.string.log_camera_perm_granted, LogManager.Types.INFO);
                    startCamera();
                } else {
                    LogManager.log(R.string.log_missing_camera_perm, LogManager.Types.WARNING, Toast.LENGTH_SHORT);
                }
            } break;
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

    @Override
    protected void onNewIntent(Intent intent) {
        // обработка результата голосового поиска
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            mSearchView.setQuery(query, true);
        }
        super.onNewIntent(intent);
    }

    /**
     * Обработчик нажатия кнопки Назад.
     */
    @Override
    public void onBackPressed() {
        if (!onSaveRecord(true, null)) {
            // указываем родительской активности, что нужно обновить список записей
            setResultFieldsEdited();
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
