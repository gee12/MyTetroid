package com.gee12.mytetroid.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.DoubleTapListener;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.AskDialogs;
import com.lumyjuwon.richwysiwygeditor.RichEditor.EditableWebView;
import com.lumyjuwon.richwysiwygeditor.WysiwygEditor;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class RecordActivity extends AppCompatActivity implements View.OnTouchListener,
        EditableWebView.IUrlLoadListener, EditableWebView.IPageLoadListener, WysiwygEditor.IEditorListener {

    public static final int REQUEST_CODE_SETTINGS_ACTIVITY = 1;
    public static final String EXTRA_RECORD_ID = "EXTRA_RECORD_ID";
    public static final String EXTRA_TAG_NAME = "EXTRA_TAG_NAME";
    public static final String EXTRA_IS_RELOAD_STORAGE = "EXTRA_IS_RELOAD_STORAGE";

//    public static final int RECORD_VIEW_NONE = -1;
    public static final int MODE_VIEW = 1;
    public static final int MODE_EDIT = 2;
    public static final int MODE_HTML = 3;

    protected GestureDetectorCompat gestureDetector;
    private RelativeLayout recordFieldsLayout;
    private ExpandableLayout expRecordFieldsLayout;
    private WebView wvRecordTags;
    private EditText etHtml;
    private TextView tvRecordAuthor;
    private TextView tvRecordUrl;
    private TextView tvRecordDate;
    //    private TetroidWebView editor;
    private WysiwygEditor editor;
    private MenuItem miRecordView;
    private MenuItem miRecordEdit;
    private MenuItem miRecordSave;
    private MenuItem miRecordHtml;
//    private MenuItem miCurNode;
//    private MenuItem miAttachedFiles;
//    private MenuItem miCurRecordFolder;
//    private ViewFlipper vfRecord;
//    private FloatingActionButton fabRecordViewLeft;
//    private FloatingActionButton fabRecordViewRight;
//    private int curRecordViewId;
    private TetroidRecord record;
    private int curMode;
    private int lastMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
            toolbar.setSubtitle(getResources().getStringArray(R.array.view_type_titles)[1]);
        }

        this.gestureDetector = new GestureDetectorCompat(this, new DoubleTapListener(this));

        this.editor = findViewById(R.id.web_view_record_text);
        editor.setOnTouchListener(this);
        editor.getWebView().getSettings().setBuiltInZoomControls(true);
        editor.getWebView().getSettings().setDisplayZoomControls(false);

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
        String textHtml = DataManager.getRecordHtmlTextDecrypted(record);

//        editor.getWebView().clearAndFocusEditor();
//        editor.getWebView().setHtml(textHtml);
        editor.loadDataWithBaseURL(DataManager.getRecordDirUri(record), textHtml);

        EditableWebView webView = editor.getWebView();
//        editor.getWebView().getSettings().setAllowFileAccessFromFileURLs(true);
//        webView.getSettings().setDomStorageEnabled(true);

//        webView.addJavascriptInterface(new IJavascriptHandler(), "test");

//        webView.setOnInitialLoadListener(new EditableWebView.IPageLoadListener() {
//            @Override
//            public void onPageLoad(boolean isReady) {
//                if (Build.VERSION.SDK_INT >= 19) {
////                    webView.evaluateJavascript(js2, s -> {
////                    });
//                    webView.evaluateJavascript(js, s -> {
//                    });
//                } else {
////                    webView.loadUrl(js2);
//                    webView.loadUrl(js);
//                }
//            }
//        });
        webView.setOnUrlLoadListener(this);

//        workWithJavascript();
    }

//    void workWithJavascript() {
//        EditableWebView webView = editor.getWebView();
//
//        webView.addJavascriptInterface(new IJavascriptHandler(), "test");
//
//        WebViewClient BrowserHandler = new WebViewClient() {
//            @Override
//            public void onPageFinished(WebView view, String url) {
//                view.loadUrl("javascript:window.test.onPageLoad(document.body.innerHTML);void(0);");
//            }
//        };
//    }


    @Override
    public void onPageLoad(boolean isReady) {
        // режим по-умолчанию
        int mode = (SettingsManager.isRecordEditMode()) ? MODE_EDIT : MODE_VIEW;
//        int mode = MODE_EDIT;
        switchMode(mode);
    }
    /**
     * Открытие ссылки в тексте.
     * @param url
     */
    @Override
    public boolean onUrlLoad(String url) {
        if (url.startsWith("mytetra")) {
            // обрабатываем внутреннюю ссылку
            String id = url.substring(url.lastIndexOf('/')+1);
            TetroidRecord record = DataManager.getRecord(id);

            // !!
            // вот тут пока неясно что делать потом с командой Back, например.
//            mainView.openRecord(recordExt);
            openRecord(this, record);
            // return super.shouldOverrideUrlLoading(view, request);
        } else {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            } catch (Exception ex) {
                LogManager.addLog(ex);
            }
        }
        return true;
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
        openTag(RecordActivity.this, tagName);
    }

    public static void openRecord(Context context, TetroidRecord record) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_RECORD_ID, record.getId());
        ViewUtils.startActivity(context, RecordActivity.class, bundle);
    }


    public static void openTag(Context context, String tagName) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_TAG_NAME, tagName);
        ViewUtils.startActivity(context, MainActivity.class, bundle);
    }

    public void setRecordFieldsVisibility(boolean isVisible) {
        recordFieldsLayout.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
    }

    private void onSaveRecord() {
        if (curMode == MODE_EDIT || curMode == MODE_HTML) {
            saveRecord();
        }
    }

    private void saveRecord() {
//        if (BuildConfig.DEBUG)
//            return;
        String htmlText = editor.getWebView().getDocumentHtml();
        DataManager.saveRecordHtmlText(record, htmlText);
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
     * @param mode
     */
    private void switchMode(int mode) {
        onSaveRecord();
        this.lastMode = curMode;
        this.curMode = mode;
        switch (mode) {
            case MODE_VIEW : {
                editor.setVisibility(View.VISIBLE);
                editor.setToolBarVisibility(false);
                etHtml.setVisibility(View.GONE);
                setRecordFieldsVisibility(true);
                miRecordView.setVisible(false);
                miRecordEdit.setVisible(true);
                miRecordHtml.setVisible(true);
                miRecordSave.setVisible(false);
                editor.setEditMode(false);
            } break;
            case MODE_EDIT : {
                editor.setVisibility(View.VISIBLE);
                editor.setToolBarVisibility(true);
                etHtml.setVisibility(View.GONE);
                setRecordFieldsVisibility(false);
                miRecordView.setVisible(true);
                miRecordEdit.setVisible(false);
                miRecordHtml.setVisible(true);
                miRecordSave.setVisible(true);
                editor.setEditMode(true);
            } break;
            case MODE_HTML : {
                editor.setVisibility(View.GONE);
                String htmlText = editor.getWebView().getEditableHtml();
                etHtml.setText(htmlText);
//                editor.getWebView().makeHtmlRequest();
                etHtml.setVisibility(View.VISIBLE);
                setRecordFieldsVisibility(false);
                miRecordView.setVisible(false);
                miRecordEdit.setVisible(true);
                miRecordHtml.setVisible(false);
                miRecordSave.setVisible(true);
            } break;
        }
    }

    private void reinitStorage() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(EXTRA_IS_RELOAD_STORAGE, true);
        ViewUtils.startActivity(this, MainActivity.class, bundle);
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

    @Override
    public void onGetHtml(String html) {
        etHtml.setText(html);
    }

    /**
     * Сохранение записи при любом скрытии активности.
     */
    @Override
    public void onPause() {
        onSaveRecord();
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

                return true;
            case R.id.action_attached_files:

                return true;
            case R.id.action_cur_record_folder:

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
        if (lastMode > 0) {
            switchMode(lastMode);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Переопределяем обработчик нажатия на экране
     * для обработки перехода в полноэкранный режим.
     * @param v
     * @param event
     * @return
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return false;
    }
}
