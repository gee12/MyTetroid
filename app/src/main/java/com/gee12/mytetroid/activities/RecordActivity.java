package com.gee12.mytetroid.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.AskDialogs;
import com.gee12.mytetroid.views.Message;
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
//    public static final String EXTRA_ACTION_ID = "EXTRA_ACTION_ID";
    public static final String EXTRA_RECORD_ID = "EXTRA_RECORD_ID";
    public static final String EXTRA_TAG_NAME = "EXTRA_TAG_NAME";
    public static final String EXTRA_IS_RELOAD_STORAGE = "EXTRA_IS_RELOAD_STORAGE";
    public static final boolean IS_EDIT_CRYPTED_RECORDS = false;

    public static final int MODE_VIEW = 1;
    public static final int MODE_EDIT = 2;
    public static final int MODE_HTML = 3;
//    public static final String ACTION_REINIT_STORAGE = "ACTION_REINIT_STORAGE";
//    public static final String ACTION_OPEN_RECORD = "ACTION_OPEN_RECORD";
//    public static final String ACTION_SHOW_TAG = "ACTION_SHOW_TAG";
//    public static final int ACTION_REINIT_STORAGE = 1;
//    public static final int ACTION_OPEN_RECORD = 2;
//    public static final int ACTION_SHOW_TAG = 3;
    public static final int RESULT_REINIT_STORAGE = 1;
    public static final int RESULT_OPEN_RECORD = 2;
    public static final int RESULT_SHOW_TAG = 3;

    private RelativeLayout recordFieldsLayout;
    private ExpandableLayout expRecordFieldsLayout;
    private WebView wvRecordTags;
    private ScrollView scrollViewHtml;
    private EditText etHtml;
    private TextView tvRecordAuthor;
    private TextView tvRecordUrl;
    private TextView tvRecordDate;
    private TetroidEditor editor;
    private MenuItem miRecordView;
    private MenuItem miRecordEdit;
    private MenuItem miRecordSave;
    private MenuItem miRecordHtml;
//    private MenuItem miCurNode;
//    private MenuItem miAttachedFiles;
//    private MenuItem miCurRecordFolder;
    private TetroidRecord record;
    private int curMode;
    private boolean isFirstLoad = true;


    public RecordActivity() {
        super(R.layout.activity_record);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_record);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);
        if (recordId == null) {
            LogManager.addLog(getString(R.string.not_transferred_record_id), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            return;
        }
        // получаем запись
        this.record = DataManager.getRecord(recordId);
        if (record == null) {
            LogManager.addLog(getString(R.string.not_found_record) + recordId, LogManager.Types.ERROR, Toast.LENGTH_LONG);
            return;
        } else {
            setTitle(record.getName());
//            setSubtitle(getResources().getStringArray(R.array.view_type_titles)[1]);
        }

        this.gestureDetector = new GestureDetectorCompat(this, new ActivityDoubleTapListener(this));

        this.editor = findViewById(R.id.web_view_record_text);
        editor.setToolBarVisibility(false);
//        editor.setOnTouchListener(this);
        editor.getWebView().setOnTouchListener(this);
        editor.setOnPageLoadListener(this);
        EditableWebView webView = editor.getWebView();
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
//        webView.setOnPageLoadListener(this);
        webView.setOnUrlLoadListener(this);
        webView.setOnHtmlReceiveListener(this);
        webView.setYoutubeLoadLinkListener(this);

        this.recordFieldsLayout = findViewById(R.id.layout_record_fields);
        this.wvRecordTags = findViewById(R.id.web_view_record_tags);
        wvRecordTags.setBackgroundColor(Color.TRANSPARENT);
        wvRecordTags.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                onTagUrlLoad(url);
                return true;
            }
        });
        this.tvRecordAuthor = findViewById(R.id.text_view_record_author);
        this.tvRecordUrl = findViewById(R.id.text_view_record_url);
        this.tvRecordDate = findViewById(R.id.text_view_record_date);
        this.expRecordFieldsLayout = findViewById(R.id.layout_expander);
        ToggleButton tbRecordFieldsExpander = findViewById(R.id.toggle_button_expander);
        tbRecordFieldsExpander.setOnCheckedChangeListener((buttonView, isChecked) -> expRecordFieldsLayout.toggle());
        setRecordFieldsVisibility(false);

        this.scrollViewHtml = findViewById(R.id.scroll_html);
        this.etHtml = findViewById(R.id.edit_text_html);
    }

    private void onMenuLoaded() {
        openRecord(record);
    }

    /**
     * Отображение записи
     */
    public void openRecord(TetroidRecord record) {
        int id = R.id.label_record_tags;
        // метки
        String tagsHtml = TetroidRecord.createTagsLinksString(record);
        if (tagsHtml != null) {
            // указываем charset в mimeType для кириллицы
            wvRecordTags.loadData(tagsHtml, "text/html; charset=UTF-8", null);
            id = R.id.web_view_record_tags;
        }
        // указываем относительно чего теперь выравнивать следующую панель
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, id);
        expRecordFieldsLayout.setLayoutParams(params);
        // поля
        tvRecordAuthor.setText(record.getAuthor());
        tvRecordUrl.setText(record.getUrl());
        if (record.getCreated() != null)
            tvRecordDate.setText(record.getCreatedString(getString(R.string.full_date_format_string)));
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
                ? etHtml.getText().toString()
                : DataManager.getRecordHtmlTextDecrypted(record);
//        editor.getWebView().clearAndFocusEditor();
        editor.getWebView().loadDataWithBaseURL(DataManager.getRecordDirUri(record),
                textHtml, "text/html", "UTF-8", null);
    }

    @Override
    public void onPageStartLoading() {

    }

    @Override
    public void onPageLoaded() {
        if (isFirstLoad) {
            // переключаем views и делаем другие обработки
            int defMode = (SettingsManager.isRecordEditMode()) ? MODE_EDIT : MODE_VIEW;

            // FIXME: реализовать сохранение зашифрованных записей
            if (record.isCrypted() && defMode == MODE_EDIT && !IS_EDIT_CRYPTED_RECORDS) {
                Message.show(this, getString(R.string.editing_crypted_records), Toast.LENGTH_LONG);
                defMode = MODE_VIEW;
            }

            switchMode(defMode);
        } else {
            // переключаем только views
            switchViews(curMode);
        }
        this.isFirstLoad = false;
    }

    /**
     * Открытие ссылки в тексте.
     * @param url
     */
    @Override
    public boolean onLinkLoad(String url) {
        // удаляем BaseUrl из строки адреса
        String baseUrl = editor.getWebView().getUrl();
        if (url.startsWith(baseUrl)) {
            url = url.replace(baseUrl, "");
        }

        /*if (url.startsWith(DataManager.MYTETRA_PREFIX)) {
            // обрабатываем внутреннюю ссылку
            if (url.startsWith(DataManager.MYTETRA_RECORD_PREFIX)) {
                // ссылка на запись типа "mytetra://note/1581762186rr4m8ttsgb"
//            String recordId = url.substring(url.lastIndexOf('/')+1);
                String recordId = url.replace(DataManager.MYTETRA_RECORD_PREFIX, "");
                TetroidRecord record = DataManager.getRecord(recordId);
                if (record != null) {
                    openAnotherRecord(record);
                } else {
                    LogManager.addLog(getString(R.string.not_found_record) + recordId, LogManager.Types.WARNING, Toast.LENGTH_LONG);
                }
            }
        }*/
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
            LogManager.addLog("Ошибка декодирования url: " + url, ex);
            return;
        }
        // избавляемся от приставки "tag:"
        String tagName = decodedUrl.substring(TetroidRecord.TAG_LINKS_PREF.length());
        openTag(tagName);
    }

    /**
     * FIXME:
     * @param record
     */
    public void openAnotherRecord(TetroidRecord record) {
        if (record == null)
            return;
        Bundle bundle = new Bundle();
//        bundle.putInt(EXTRA_ACTION_ID, ACTION_OPEN_RECORD);
        bundle.putString(EXTRA_RECORD_ID, record.getId());
//        ViewUtils.startActivity(activity, MainActivity.class, bundle, ACTION_OPEN_RECORD, Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        ViewUtils.startActivity(activity, RecordActivity.class, bundle);
        // если открываем запись из той же активности RecordActivity, то нужно ее закрыть
//        if (activity.getClass().getSimpleName().equals(RecordActivity.class.getSimpleName())) {
//            activity.finish();
//        }
//        Intent intent = new Intent();
//        intent.putExtras(bundle);
//        activity.startActivityForResult(intent, REQUEST_CODE_RECORD_ACTIVITY);
//        ViewUtils.startActivity(activity, RecordActivity.class, bundle, MainActivity.REQUEST_CODE_RECORD_ACTIVITY);
//        setResult(Activity.RESULT_OK, intent);
//        finish();
        finishWithResult(RESULT_OPEN_RECORD, bundle);
    }

    /**
     * Открытие записей метки в главной активности.
     * @param tagName
     */
    public void openTag(String tagName) {
        Bundle bundle = new Bundle();
//        bundle.putInt(EXTRA_ACTION_ID, ACTION_SHOW_TAG);
        bundle.putString(EXTRA_TAG_NAME, tagName);
        // Action = android.intent.action.MAIN
        // Categories[0] = android.intent.category.LAUNCHER
//        ViewUtils.startActivity(activity, MainActivity.class, bundle, ACTION_SHOW_TAG, Intent.FLAG_ACTIVITY_SINGLE_TOP, null);
//        ViewUtils.startActivity(context, MainActivity.class, bundle);
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

//    public static void openTag(Activity activity, String tagName) {
//        Bundle bundle = new Bundle();
//        bundle.putString(EXTRA_TAG_NAME, tagName);
////        ViewUtils.startActivity(context, MainActivity.class, bundle);
//        activity.startActivity(ViewUtils.createIntent(activity, MainActivity.class, bundle));
//    }

    public void setRecordFieldsVisibility(boolean isVisible) {
        recordFieldsLayout.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
    }

    private void saveRecord() {
//        if (BuildConfig.DEBUG)
//            return;
        LogManager.addLog("Сохранение текста записи id=" + record.getId(), LogManager.Types.INFO);
        String htmlText = (curMode == MODE_HTML)
                ? editor.getDocumentHtml(etHtml.getText().toString()) : editor.getDocumentHtml();
        if (!DataManager.saveRecordHtmlText(record, htmlText)) {
            LogManager.addLog("Не удается сохранить запись (подробнее в логах)", LogManager.Types.ERROR, Toast.LENGTH_LONG);
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

        // FIXME: реализовать сохранение зашифрованных записей
        if (record.isCrypted() && newMode != MODE_VIEW && !IS_EDIT_CRYPTED_RECORDS) {
            Message.show(this, getString(R.string.editing_crypted_records), Toast.LENGTH_LONG);
            return;
        }

        int oldMode = curMode;
        // сохраняем
        onSaveRecord(oldMode, newMode);
        // перезагружаем текст записи в webView, если меняли вручную html
        if (oldMode == MODE_HTML) {
            loadRecordText(record, true);
        }
        // переключаем элементы интерфейса, только если не редактировали только что html,
        // т.к. тогда вызов switchViews() должен произойти уже после перезагрузки страницы
        // на событии onPageLoaded())
        if (oldMode != MODE_HTML) {
            switchViews(newMode);
        }
        this.curMode = newMode;
    }

    /**
     * Сохранение изменений при смене режима.
     * @param oldMode
     * @param newMode
     */
    private void onSaveRecord(int oldMode, int newMode) {
        if (SettingsManager.isRecordAutoSave()) {
            if (oldMode == MODE_EDIT || oldMode == MODE_HTML) {
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
     */
    private boolean onSaveRecord(int curMode, boolean isAskAndExit) {
        if (curMode == MODE_EDIT || curMode == MODE_HTML) {
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
                editor.setVisibility(View.VISIBLE);
                editor.setToolBarVisibility(false);
                scrollViewHtml.setVisibility(View.GONE);
                setRecordFieldsVisibility(true);
                miRecordView.setVisible(false);
                miRecordEdit.setVisible(true);
                miRecordHtml.setVisible(true);
                miRecordSave.setVisible(false);
                editor.setEditMode(false);
                setSubtitle(getString(R.string.record_subtitle_view));
                ViewUtils.hideKeyboard(this, editor.getWebView());
            } break;
            case MODE_EDIT : {
                editor.setVisibility(View.VISIBLE);
                editor.setToolBarVisibility(true);
                scrollViewHtml.setVisibility(View.GONE);
                setRecordFieldsVisibility(false);
                miRecordView.setVisible(true);
                miRecordEdit.setVisible(false);
                miRecordHtml.setVisible(true);
                miRecordSave.setVisible(true);
                editor.setEditMode(true);
                setSubtitle(getString(R.string.record_subtitle_edit));
                editor.getWebView().focusEditor();
//                ViewUtils.showKeyboard(this, editor.getWebView());
            } break;
            case MODE_HTML : {
                editor.setVisibility(View.GONE);
                String htmlText = editor.getWebView().getEditableHtml();
                etHtml.setText(htmlText);
//                editor.getWebView().makeEditableHtmlRequest();
                scrollViewHtml.setVisibility(View.VISIBLE);
                etHtml.requestFocus();
                setRecordFieldsVisibility(false);
                miRecordView.setVisible(false);
                miRecordEdit.setVisible(true);
                miRecordHtml.setVisible(false);
                miRecordSave.setVisible(true);
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
        DataManager.openFolder(this, DataManager.getRecordDirUri(record));
    }

    @Override
    public void onReceiveEditableHtml(String htmlText) {
        etHtml.setText(htmlText);
    }

    /**
     * Сохранение записи при любом скрытии активности.
     */
    @Override
    public void onPause() {
        onSaveRecord(curMode, false);
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
        this.miRecordView = menu.findItem(R.id.action_record_view);
        this.miRecordEdit = menu.findItem(R.id.action_record_edit);
        this.miRecordSave = menu.findItem(R.id.action_record_save);
        this.miRecordHtml = menu.findItem(R.id.action_record_html);
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
                return onSaveRecord(curMode, true);
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
        if (!onSaveRecord(curMode, true)) {
            super.onBackPressed();
        }
    }

}
