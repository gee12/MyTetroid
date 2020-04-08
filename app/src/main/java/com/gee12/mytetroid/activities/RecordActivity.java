package com.gee12.mytetroid.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidObject;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.AskDialogs;
import com.gee12.mytetroid.views.ImgPicker;
import com.gee12.mytetroid.views.RecordAskDialogs;
import com.gee12.mytetroid.views.TetroidEditor;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lumyjuwon.richwysiwygeditor.RichEditor.EditableWebView;
import com.lumyjuwon.richwysiwygeditor.WysiwygUtils.IImagePicker;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
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
    public static final String EXTRA_IS_RELOAD_STORAGE = "EXTRA_IS_RELOAD_STORAGE";
    public static final String EXTRA_IS_FIELDS_EDITED = "EXTRA_IS_FIELDS_EDITED";

    public static final int MODE_VIEW = 1;
    public static final int MODE_EDIT = 2;
    public static final int MODE_HTML = 3;
    public static final int RESULT_REINIT_STORAGE = 1;
    public static final int RESULT_OPEN_RECORD = 2;
    public static final int RESULT_OPEN_NODE = 3;
    public static final int RESULT_SHOW_TAG = 4;

//    private RelativeLayout mFieldsLayout;
    private ExpandableLayout mFieldsExpanderLayout;
    private FloatingActionButton mFabFieldsToggle;
    private WebView mWebViewTags;
    private ScrollView mScrollViewHtml;
    private EditText mEditTextHtml;
    private TextView mTvAuthor;
    private TextView mTvUrl;
    private TextView mTvDate;
    private ImageButton mButtonFieldsEdit;
    private TetroidEditor mEditor;
    private MenuItem mMenuItemView;
    private MenuItem mMenuItemEdit;
//    private MenuItem mMenuItemSave;
    private MenuItem mMenuItemHtml;
//    private MenuItem miCurNode;
//    private MenuItem miAttachedFiles;
//    private MenuItem miCurRecordFolder;
    private TetroidRecord mRecord;
    private int mCurMode;
    private boolean mIsFirstLoad = true;
    private boolean mIsFieldsEdited;


    public RecordActivity() {
        super(R.layout.activity_record);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String recordId = getIntent().getStringExtra(EXTRA_OBJECT_ID);
        if (recordId == null) {
            LogManager.addLog(getString(R.string.log_not_transferred_record_id), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            return;
        }
        // получаем запись
        this.mRecord = DataManager.getRecord(recordId);
        if (mRecord == null) {
            LogManager.addLog(getString(R.string.log_not_found_record) + recordId, LogManager.Types.ERROR, Toast.LENGTH_LONG);
            finish();
            return;
        } else {
            setTitle(mRecord.getName());
//            setSubtitle(getResources().getStringArray(R.array.view_type_titles)[1]);
        }

//        this.gestureDetector = new GestureDetectorCompat(this, new ActivityDoubleTapListener(this));

        this.mEditor = findViewById(R.id.web_view_record_text);
//        mEditor.setImagesFolder(DataManager.getPathToRecordFolder(mRecord));
        mEditor.setImgPickerCallback(this);
        mEditor.setToolBarVisibility(false);
//        mEditor.setOnTouchListener(this);
        mEditor.getWebView().setOnTouchListener(this);
        mEditor.setOnPageLoadListener(this);
        EditableWebView webView = mEditor.getWebView();
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setOnUrlLoadListener(this);
        webView.setOnHtmlReceiveListener(this);
        webView.setYoutubeLoadLinkListener(this);

//        this.mFieldsLayout = findViewById(R.id.layout_record_fields);
        this.mWebViewTags = findViewById(R.id.web_view_record_tags);
        mWebViewTags.setBackgroundColor(Color.TRANSPARENT);
        mWebViewTags.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                onTagUrlLoad(url);
                return true;
            }
        });
        this.mTvAuthor = findViewById(R.id.text_view_record_author);
        this.mTvUrl = findViewById(R.id.text_view_record_url);
        this.mTvDate = findViewById(R.id.text_view_record_date);
        this.mFieldsExpanderLayout = findViewById(R.id.layout_fields_expander);
//        ToggleButton tbRecordFieldsExpander = findViewById(R.id.toggle_button_expander);
//        tbRecordFieldsExpander.setOnCheckedChangeListener((buttonView, isChecked) -> mFieldsExpanderLayout.toggle());
        this.mFabFieldsToggle = findViewById(R.id.button_toggle_fields);
        mFabFieldsToggle.setOnClickListener(v -> toggleRecordFieldsVisibility());
        setRecordFieldsVisibility(false);

        this.mButtonFieldsEdit = findViewById(R.id.button_edit_fields);
        mButtonFieldsEdit.setOnClickListener(v -> editFields());

        this.mScrollViewHtml = findViewById(R.id.scroll_html);
        this.mEditTextHtml = findViewById(R.id.edit_text_html);
//        mEditTextHtml.setOnTouchListener(this); // работает криво
        mEditTextHtml.addTextChangedListener(mHtmlWatcher);

        // не гасим экран, если установлена опция
        setKeepScreenOn(SettingsManager.isKeepScreenOn());
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
        LogManager.addLog(getString(R.string.log_record_loading) + record.getId(), LogManager.Types.INFO);
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
        String tagsHtml = TetroidTag.createTagsHtmlString(record);
        if (tagsHtml != null) {
            // указываем charset в mimeType для кириллицы
            mWebViewTags.loadDataWithBaseURL(null, tagsHtml, "text/html", "UTF-8", null);
            id = R.id.web_view_record_tags;
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
        mTvAuthor.setLayoutParams(params2);
        // поля
        mTvAuthor.setText(record.getAuthor());
        mTvUrl.setText(record.getUrl());
        if (record.getCreated() != null)
            mTvDate.setText(record.getCreatedString(getString(R.string.full_date_format_string)));
    }

    /**
     * Загрузка html-кода записи в WebView.
     * @param record
     */
    private void loadRecordText(TetroidRecord record, boolean fromHtmlEditor) {
        // 1) если только что вернулись из редактора html-кода (fromHtmlEditor)
        // и не используется авто-сохранение изменений, то загружаем html-код из редактора
        // 2) если нет, то загружаем html-код из файла записи
        String textHtml = (fromHtmlEditor && !SettingsManager.isRecordAutoSave())
                ? mEditTextHtml.getText().toString()
                : DataManager.getRecordHtmlTextDecrypted(record);
//        mEditor.getWebView().clearAndFocusEditor();
        mEditor.getWebView().loadDataWithBaseURL(DataManager.getRecordDirUri(record),
                textHtml, "text/html", "UTF-8", null);
    }

    /**
     * Событие запуска загрузки страницы.
     */
    @Override
    public void onPageStartLoading() {

    }

    /**
     * Событие окончания загрузки страницы.
     */
    @Override
    public void onPageLoaded() {
        if (mIsFirstLoad) {
            // переключаем views и делаем другие обработки
            int defMode = (mRecord.isNew() || SettingsManager.isRecordEditMode()) ? MODE_EDIT : MODE_VIEW;

            // сбрасываем флаг, т.к. им уже воспользовались
            mRecord.setIsNew(false);

            switchMode(defMode);

            this.mIsFirstLoad = false;
        } else {
            // переключаем только views
            switchViews(mCurMode);
        }
    }

    /**
     * Открытие ссылки в тексте.
     * @param url
     */
    @Override
    public boolean onLinkLoad(String url) {
        // раскодируем url, т.к. он может содержать кириллицу
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
                    TetroidRecord record = DataManager.getRecord(obj.getId());
                    if (record != null) {
                        openAnotherRecord(record, true);
                    } else {
                        LogManager.addLog(getString(R.string.log_not_found_record) + obj.getId(), LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    }
                    break;
                }
                case FoundType.TYPE_NODE:
                    TetroidNode node = DataManager.getNode(obj.getId());
                    if (node != null) {
                        openAnotherNode(node, true);
                    } else {
                        LogManager.addLog(getString(R.string.log_not_found_node_id) + obj.getId(), LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    }
                    break;
                case FoundType.TYPE_TAG:
                    String tag = obj.getId();
                    if (!TextUtils.isEmpty(tag)) {
                        openTag(tag, true);
                    } else {
                        LogManager.addLog(getString(R.string.tag_name_is_empty), LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    }
                    break;
                case FoundType.TYPE_AUTHOR:
                case FoundType.TYPE_FILE:
                    break;
                default:
                    LogManager.addLog(getString(R.string.log_link_to_obj_parsing_error), LogManager.Types.WARNING, Toast.LENGTH_LONG);
            }
        } else {
            // обрабатываем внешнюю ссылку
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            } catch (Exception ex) {
                LogManager.addLog(ex, Toast.LENGTH_LONG);
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
            LogManager.addLog(getString(R.string.url_decode_error) + url, ex);
            return;
        }
        if (decodedUrl.startsWith(TetroidTag.LINKS_PREFIX)) {
            // избавляемся от приставки "tag:"
            String tagName = decodedUrl.substring(TetroidTag.LINKS_PREFIX.length());
            openTag(tagName, true);
        } else {
            LogManager.addLog(getString(R.string.log_wrong_tag_link_format), LogManager.Types.WARNING, Toast.LENGTH_LONG);
        }
    }

    /**
     * Открытие другой записи по внутренней ссылке.
     * @param record
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
     * Открытие записей метки в главной активности.
     * @param tagName
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

    /**
     * TODO:
     */
    private void showCurNode() {

    }

    /**
     * TODO:
     */
    private void showRecordFiles() {

    }

    private void toggleRecordFieldsVisibility() {
        mFieldsExpanderLayout.toggle();
        if (mFieldsExpanderLayout.isExpanded()) {
            mFabFieldsToggle.setImageResource(R.drawable.ic_arrow_drop_up);
        } else {
            mFabFieldsToggle.setImageResource(R.drawable.ic_arrow_drop_down);
        }
    }

    private void setRecordFieldsVisibility(boolean isVisible) {
//        mFieldsExpanderLayout.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
        if (isVisible) {
            mFieldsExpanderLayout.setVisibility(View.VISIBLE);
            mFabFieldsToggle.show();
        } else {
            mFieldsExpanderLayout.setVisibility(View.GONE);
            mFabFieldsToggle.hide();
        }
    }

    private void saveRecord() {
        LogManager.addLog(getString(R.string.log_before_record_save) + mRecord.getId(), LogManager.Types.INFO);
        String htmlText = (mCurMode == MODE_HTML)
                ? mEditor.getDocumentHtml(mEditTextHtml.getText().toString()) : mEditor.getDocumentHtml();
        if (DataManager.saveRecordHtmlText(mRecord, htmlText)) {
            LogManager.addLog(getString(R.string.log_record_saved), LogManager.Types.INFO, Toast.LENGTH_SHORT);
            // сбрасываем пометку изменения записи
            mEditor.setIsEdited(false);
        } else {
            LogManager.addLog(getString(R.string.log_record_save_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    @Override
    public void startPicker() {
        ImgPicker.startPicker(this);
    }

    @Override
    public void startCamera() {
        Intent intent = ImgPicker.createCamera(DataManager.getStoragePathBase(), mRecord.getDirName())
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
            LogManager.addLog(getString(R.string.log_selected_files_is_missing), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            return;
        }
        int errorCount = 0;
        List<String> savedImages = new ArrayList<>();
        for (Image image : images) {
            String savedImage = DataManager.saveImage(mRecord, image.getPath(), isCamera);
            if (savedImage != null) {
                savedImages.add(savedImage);
            } else {
                errorCount++;
            }
        }
        if (errorCount > 0) {
            LogManager.addLog(getString(R.string.log_failed_to_save_images), LogManager.Types.WARNING, Toast.LENGTH_LONG);
        }
        if (!savedImages.isEmpty()) {
            mEditor.onSelectImages(savedImages);
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
        LogManager.addLog(String.format(getString(R.string.search_text_by_query), record.getName(), query));
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
//                mMenuItemSave.setVisible(false);
                mEditor.setEditMode(false);
                setSubtitle(getString(R.string.record_subtitle_view));
                ViewUtils.hideKeyboard(this, mEditor.getWebView());
            } break;
            case MODE_EDIT : {
                mEditor.setVisibility(View.VISIBLE);
                mEditor.setToolBarVisibility(true);
                mScrollViewHtml.setVisibility(View.GONE);
                setRecordFieldsVisibility(false);
                mMenuItemView.setVisible(true);
                mMenuItemEdit.setVisible(false);
                mMenuItemHtml.setVisible(true);
//                mMenuItemSave.setVisible(true);
                mEditor.setEditMode(true);
                setSubtitle(getString(R.string.record_subtitle_edit));
                mEditor.getWebView().focusEditor();
//                ViewUtils.showKeyboard(this, mEditor.getWebView());
            } break;
            case MODE_HTML : {
                mEditor.setVisibility(View.GONE);
                String htmlText = mEditor.getWebView().getEditableHtml();
                mEditTextHtml.setText(htmlText);
//                mEditor.getWebView().makeEditableHtmlRequest();
                mScrollViewHtml.setVisibility(View.VISIBLE);
                mEditTextHtml.requestFocus();
                setRecordFieldsVisibility(false);
                mMenuItemView.setVisible(false);
                mMenuItemEdit.setVisible(true);
                mMenuItemHtml.setVisible(false);
//                mMenuItemSave.setVisible(true);
                setSubtitle(getString(R.string.record_subtitle_html));
            } break;
        }
    }

    /**
     * Перезагрузка хранилища в главной активности.
     */
    private void reinitStorage() {
//        Bundle bundle = new Bundle();
//        bundle.putInt(EXTRA_ACTION_ID, ACTION_REINIT_STORAGE);
//        ViewUtils.startActivity(this, MainActivity.class, bundle, ACTION_REINIT_STORAGE, Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        setResult(0, ViewUtils.createIntent(this, MainActivity.class, bundle, ACTION_REINIT_STORAGE));
        finishWithResult(RESULT_REINIT_STORAGE, null);
    }

    private void setKeepScreenOn(boolean keepScreenOn) {
        if (keepScreenOn)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean toggleFullscreen() {
        boolean isFullscreen = super.toggleFullscreen();
        if (mCurMode == MODE_VIEW) {
//            setRecordFieldsVisibility(!isFullscreen);
        }
        return isFullscreen;
    }

    public void openRecordFolder() {
        DataManager.openRecordFolder(this, mRecord);
    }

    @Override
    public void onReceiveEditableHtml(String htmlText) {
        mEditTextHtml.setText(htmlText);
    }

    private void editFields() {
        RecordAskDialogs.createRecordFieldsDialog(this, mRecord, (name, tags, author, url) -> {
            if (DataManager.editRecordFields(mRecord, name, tags, author, url)) {
                this.mIsFieldsEdited = true;
                setTitle(name);
                loadFields(mRecord);
            } else {
                LogManager.addLog(getString(R.string.log_record_edit_fields_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            }
        });
    }

    /**
     * Формирование результата активности и ее закрытие.
     * @param bundle
     */
    private void finishWithResult(int resCode, Bundle bundle) {
        Intent intent = new Intent();
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        setResult(resCode, intent);
        finish();
    }

    /**
     * Формирование результата активности в виде указания для родительской активности
     * обновить список записей и меток.
     */
    private void setResultFieldsEdited() {
        if (mIsFieldsEdited) {
            if (mIsFieldsEdited) {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_IS_FIELDS_EDITED, true);
                setResult(RESULT_OK, intent);
            }
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
            // перезагружаем хранилище, если изменили путь
            if (SettingsManager.isAskReloadStorage) {
                SettingsManager.isAskReloadStorage = false;
                AskDialogs.showReloadStorageDialog(this, () -> reinitStorage());
            }
            // не гасим экран, если установили опцию
            setKeepScreenOn(SettingsManager.isKeepScreenOn());
        } else if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK) {
            saveSelectedImages(data, true);
        } else if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            saveSelectedImages(data, false);
        }
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
//        this.mMenuItemSave = menu.findItem(R.id.action_record_save);
        this.mMenuItemHtml = menu.findItem(R.id.action_record_html);
        onMenuLoaded();
        return true;
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
            case R.id.action_cur_node:
                showCurNode();
                return true;
            case R.id.action_attached_files:
                showRecordFiles();
                return true;
            case R.id.action_cur_record_folder:
                openRecordFolder();
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

    public void showActivityForResult(Class<?> cls, int requestCode) {
        Intent intent = new Intent(this, cls);
        startActivityForResult(intent, requestCode);
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

    private TextWatcher mHtmlWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mCurMode == MODE_HTML) {
                mEditor.setIsEdited();
            }
        }
    };
}
