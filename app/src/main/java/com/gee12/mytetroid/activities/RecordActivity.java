package com.gee12.mytetroid.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.ActivityDoubleTapListener;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.TetroidObject;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.AskDialogs;
import com.gee12.mytetroid.views.TetroidEditor;
import com.lumyjuwon.richwysiwygeditor.RichEditor.EditableWebView;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class RecordActivity extends TetroidActivity implements
        EditableWebView.IPageLoadListener,
        EditableWebView.ILinkLoadListener,
        EditableWebView.IHtmlReceiveListener,
        EditableWebView.IYoutubeLinkLoadListener {

    public static final int REQUEST_CODE_SETTINGS_ACTIVITY = 1;
    public static final String EXTRA_RECORD_ID = "EXTRA_RECORD_ID";
    public static final String EXTRA_TAG_NAME = "EXTRA_TAG_NAME";
    public static final String EXTRA_IS_RELOAD_STORAGE = "EXTRA_IS_RELOAD_STORAGE";
    public static final boolean IS_EDIT_CRYPTED_RECORDS = false;

    public static final int MODE_VIEW = 1;
    public static final int MODE_EDIT = 2;
    public static final int MODE_HTML = 3;
    public static final int RESULT_REINIT_STORAGE = 1;
    public static final int RESULT_OPEN_RECORD = 2;
    public static final int RESULT_SHOW_TAG = 3;

    private RelativeLayout mRecordFieldsLayout;
    private ExpandableLayout mExpRecordFieldsLayout;
    private WebView mWebViewTags;
    private ScrollView mScrollViewHtml;
    private EditText mEditTextHtml;
    private TextView mTvRecordAuthor;
    private TextView mTvRecordUrl;
    private TextView mTvRecordDate;
    private TetroidEditor mEditor;
    private MenuItem mMenuItemView;
    private MenuItem mMenuItemEdit;
    private MenuItem mMenuItemSave;
    private MenuItem mMenuItemHtml;
//    private MenuItem miCurNode;
//    private MenuItem miAttachedFiles;
//    private MenuItem miCurRecordFolder;
    private TetroidRecord mRecord;
    private int mCurMode;
    private boolean mIsFirstLoad = true;


    public RecordActivity() {
        super(R.layout.activity_record);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);
        if (recordId == null) {
            LogManager.addLog(getString(R.string.not_transferred_record_id), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            return;
        }
        // получаем запись
        this.mRecord = DataManager.getRecord(recordId);
        if (mRecord == null) {
            LogManager.addLog(getString(R.string.not_found_record) + recordId, LogManager.Types.ERROR, Toast.LENGTH_LONG);
            finish();
            return;
        } else {
            setTitle(mRecord.getName());
//            setSubtitle(getResources().getStringArray(R.array.view_type_titles)[1]);
        }

        this.gestureDetector = new GestureDetectorCompat(this, new ActivityDoubleTapListener(this));

        this.mEditor = findViewById(R.id.web_view_record_text);
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

        this.mRecordFieldsLayout = findViewById(R.id.layout_record_fields);
        this.mWebViewTags = findViewById(R.id.web_view_record_tags);
        mWebViewTags.setBackgroundColor(Color.TRANSPARENT);
        mWebViewTags.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                onTagUrlLoad(url);
                return true;
            }
        });
        this.mTvRecordAuthor = findViewById(R.id.text_view_record_author);
        this.mTvRecordUrl = findViewById(R.id.text_view_record_url);
        this.mTvRecordDate = findViewById(R.id.text_view_record_date);
        this.mExpRecordFieldsLayout = findViewById(R.id.layout_expander);
        ToggleButton tbRecordFieldsExpander = findViewById(R.id.toggle_button_expander);
        tbRecordFieldsExpander.setOnCheckedChangeListener((buttonView, isChecked) -> mExpRecordFieldsLayout.toggle());
        setRecordFieldsVisibility(false);

        this.mScrollViewHtml = findViewById(R.id.scroll_html);
        this.mEditTextHtml = findViewById(R.id.edit_text_html);
        mEditTextHtml.addTextChangedListener(mHtmlWatcher);
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
        int id = R.id.label_record_tags;
        // метки
        String tagsHtml = TetroidTag.createTagsLinksString(record);
        if (tagsHtml != null) {
            // указываем charset в mimeType для кириллицы
            mWebViewTags.loadData(tagsHtml, "text/html; charset=UTF-8", null);
            id = R.id.web_view_record_tags;
        }
        // указываем относительно чего теперь выравнивать следующую панель
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, id);
        mExpRecordFieldsLayout.setLayoutParams(params);
        // поля
        mTvRecordAuthor.setText(record.getAuthor());
        mTvRecordUrl.setText(record.getUrl());
        if (record.getCreated() != null)
            mTvRecordDate.setText(record.getCreatedString(getString(R.string.full_date_format_string)));
        // текст
        loadRecordText(record, false);
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

//            if (mRecord.isCrypted() && defMode == MODE_EDIT && !IS_EDIT_CRYPTED_RECORDS) {
//                Message.show(this, getString(R.string.editing_crypted_records), Toast.LENGTH_LONG);
//                defMode = MODE_VIEW;
//            }

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
        // удаляем BaseUrl из строки адреса
        String baseUrl = mEditor.getWebView().getUrl();
        if (url.startsWith(baseUrl)) {
            url = url.replace(baseUrl, "");
        }

        TetroidObject obj;
        if ((obj = TetroidObject.parseUrl(url)) != null) {
            // обрабатываем внутреннюю ссылку
            switch (obj.getType()) {
                case FoundType.TYPE_RECORD: {
                    // ссылка на запись
                    TetroidRecord record = DataManager.getRecord(obj.getId());
                    if (record != null) {
                        openAnotherRecord(record);
                    } else {
                        LogManager.addLog(getString(R.string.not_found_record) + obj.getId(), LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    }
                    break;

                }
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
     * @param url
     */
    private void onTagUrlLoad(String url) {
        String decodedUrl;
        // декодируем url
        try {
            decodedUrl = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            LogManager.addLog(getString(R.string.url_decode_error) + url, ex);
            return;
        }
        if (decodedUrl.startsWith(TetroidTag.TAG_LINKS_PREF)) {
            // избавляемся от приставки "tag:"
            String tagName = decodedUrl.substring(TetroidTag.TAG_LINKS_PREF.length());
            openTag(tagName);
        } else {
            LogManager.addLog(getString(R.string.wrong_tag_link_format), LogManager.Types.WARNING, Toast.LENGTH_LONG);
        }
    }

    /**
     * Открытие другой записи по внутренней ссылке.
     * @param record
     */
    public void openAnotherRecord(TetroidRecord record) {
        if (record == null)
            return;
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_RECORD_ID, record.getId());
        finishWithResult(RESULT_OPEN_RECORD, bundle);
    }

    /**
     * Открытие записей метки в главной активности.
     * @param tagName
     */
    public void openTag(String tagName) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_TAG_NAME, tagName);
        finishWithResult(RESULT_SHOW_TAG, bundle);
    }

    /**
     * TODO:
     */
    public void showCurNode() {

    }

    /**
     * TODO:
     */
    public void showRecordFiles() {

    }

    public void setRecordFieldsVisibility(boolean isVisible) {
        mRecordFieldsLayout.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
    }

    private void saveRecord() {
//        if (BuildConfig.DEBUG)
//            return;
        LogManager.addLog(getString(R.string.before_record_save) + mRecord.getId(), LogManager.Types.INFO);
        String htmlText = (mCurMode == MODE_HTML)
                ? mEditor.getDocumentHtml(mEditTextHtml.getText().toString()) : mEditor.getDocumentHtml();
        if (DataManager.saveRecordHtmlText(mRecord, htmlText)) {
            LogManager.addLog(getString(R.string.record_saved), LogManager.Types.INFO, Toast.LENGTH_SHORT);
            // сбрасываем пометку изменения записи
            mEditor.setIsEdited(false);
        } else {
            LogManager.addLog(getString(R.string.record_save_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
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

//        if (mRecord.isCrypted() && newMode != MODE_VIEW && !IS_EDIT_CRYPTED_RECORDS) {
//            Message.show(this, getString(R.string.editing_crypted_records), Toast.LENGTH_LONG);
//            return;
//        }

        int oldMode = mCurMode;
        // сохраняем
        onSaveRecord(oldMode, newMode);
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
     * @param oldMode
     * @param newMode
     */
    private void onSaveRecord(int oldMode, int newMode) {
        if (SettingsManager.isRecordAutoSave()) {

            // заменил проверку режима на mEditor.isEdited()
//            if (oldMode == MODE_EDIT || oldMode == MODE_HTML) {
            if (mEditor.isEdited()) {
                saveRecord();
            }
        } /*else {
            if (newMode == MODE_VIEW && oldMode != 0) {
                AskDialogs.showSaveDialog(RecordActivity.this, new AskDialogs.IApplyCancelResult() {
                    @Override
                    public void onApply() {
                        saveRecord();
                    }

                    @Override
                    public void onCancel() {
                    }
                });
            }
        }*/
    }

    /**
     * Сохранение изменений при скрытии или выходе из активности.
     * @param curMode
     * @param isAskAndExit
     * @return false - можно продолжать действие (н-р, закрывать активность), true - начатое
     * действие нужно прервать, чтобы дождаться результата из диалога
     */
    private boolean onSaveRecord(int curMode, boolean isAskAndExit) {

        // заменил проверку режима на mEditor.isEdited()
        /*curMode == MODE_EDIT || curMode == MODE_HTML
                        || curMode == MODE_VIEW &&*/

        if (mEditor.isEdited()) {
            if (SettingsManager.isRecordAutoSave()) {
                saveRecord();
            } else if (isAskAndExit) {
                AskDialogs.showSaveDialog(RecordActivity.this, new AskDialogs.IApplyCancelResult() {
                    @Override
                    public void onApply() {
                        saveRecord();
                        finish();
                    }

                    @Override
                    public void onCancel() {
                        finish();
                    }
                });
                return true;
            }
        }
        return false;
    }

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
                mMenuItemSave.setVisible(true);
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
                mMenuItemSave.setVisible(true);
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

    private void setKeepScreenOn(boolean keepScreenOn) {
        if (keepScreenOn)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void setFullscreen(boolean isFullscreen) {
        ViewUtils.setFullscreen(this, isFullscreen);
        setRecordFieldsVisibility(!isFullscreen);
    }

    public void openRecordFolder() {
        DataManager.openFolder(this, DataManager.getRecordDirUri(mRecord));
    }

    @Override
    public void onReceiveEditableHtml(String htmlText) {
        mEditTextHtml.setText(htmlText);
    }

    /**
     * Сохранение записи при любом скрытии активности.
     */
    @Override
    public void onPause() {
        onSaveRecord(mCurMode, false);
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
        this.mMenuItemSave = menu.findItem(R.id.action_record_save);
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
                setFullscreen(!SettingsManager.IsFullScreen);
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
                return onSaveRecord(mCurMode, true);
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
        if (!onSaveRecord(mCurMode, true)) {
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
