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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.TetroidRecord;
import com.lumyjuwon.richwysiwygeditor.RichWysiwyg;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class RecordActivity extends AppCompatActivity implements View.OnTouchListener {
    public static final String EXTRA_RECORD_ID = "EXTRA_RECORD_ID";
    public static final String EXTRA_TAG_NAME = "EXTRA_TAG_NAME";

//    public static final int RECORD_VIEW_NONE = -1;
//    public static final int RECORD_VIEW_VIEWER = 0;
//    public static final int RECORD_VIEW_EDITOR = 1;
//    public static final int RECORD_VIEW_HTML = 2;

    protected GestureDetectorCompat gestureDetector;
    private RelativeLayout recordFieldsLayout;
    private ExpandableLayout expRecordFieldsLayout;
    private ToggleButton tbRecordFieldsExpander;
    //    private TextView tvRecordTags;
    private WebView wvRecordTags;
    private TextView tvRecordAuthor;
    private TextView tvRecordUrl;
    private TextView tvRecordDate;
    //    private TetroidWebView recordWebView;
    private RichWysiwyg recordWebView;
    private MenuItem miRecordEdit;
    private MenuItem miRecordSave;
    private MenuItem miRecordHtml;
//    private ViewFlipper vfRecord;
//    private FloatingActionButton fabRecordViewLeft;
//    private FloatingActionButton fabRecordViewRight;
    private int curRecordViewId;
    private TetroidRecord record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);
        if (recordId == null) {

            return;
        }
        this.record = DataManager.getRecord(recordId);
        if (record == null) {

            return;
        }
        openRecord(record);
//        TetroidRecordExt ext = readRecord(this, record);

        // текст записи
        this.recordWebView = findViewById(R.id.web_view_record_text);
        // обработка нажатия на тексте записи
        recordWebView.setOnTouchListener(this);
        recordWebView.getEditor().getSettings().setBuiltInZoomControls(true);
        recordWebView.getEditor().getSettings().setDisplayZoomControls(false);
        this.recordFieldsLayout = findViewById(R.id.layout_record_fields);
//        this.tvRecordTags =  view.findViewById(R.id.text_view_record_tags);
        this.wvRecordTags = findViewById(R.id.web_view_record_tags);
        wvRecordTags.setBackgroundColor(Color.TRANSPARENT);
        wvRecordTags.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                String decodedUrl;
                // декодируем url
                try {
                    decodedUrl = URLDecoder.decode(url, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                    LogManager.addLog("Ошибка декодирования url: " + url, ex);
                    return true;
                }
                // избавляемся от приставки "tag:"
                String tagName = decodedUrl.substring(TetroidRecord.TAG_LINKS_PREF.length());
                openTag(RecordActivity.this, tagName);
                return true;
            }
        });
        this.tvRecordAuthor =  findViewById(R.id.text_view_record_author);
        this.tvRecordUrl =  findViewById(R.id.text_view_record_url);
        this.tvRecordDate =  findViewById(R.id.text_view_record_date);
        this.expRecordFieldsLayout =  findViewById(R.id.layout_expander);
        this.tbRecordFieldsExpander =  findViewById(R.id.toggle_button_expander);
        tbRecordFieldsExpander.setOnCheckedChangeListener(this);


//        this.recordViewer = view.findViewById(R.id.record_viewer_view);
//        recordViewer.setGestureDetector(gestureDetector);
//        this.recordEditor = view.findViewById(R.id.record_editor_view);
//        recordEditor.setGestureDetector(gestureDetector);

//        this.vfRecord = view.findViewById(R.id.view_flipper_record);
//        this.fabRecordViewLeft = view.findViewById(R.id.button_record_left);
//        fabRecordViewLeft.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                switchRecordView(view);
//            }
//        });
////        fabRecordViewEdit.setAlpha(0.5f);
//        this.fabRecordViewRight = view.findViewById(R.id.button_record_right);
//        fabRecordViewRight.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                switchRecordView(view);
//            }
//        });

    }

//    private void initRecordViews() {
//        for(int i = 0; i < vfRecord.getChildCount(); i++) {
//            RecordView view = (RecordView)vfRecord.getChildAt(i);
//            if (view != null) {
//                view.init(mainView, gestureDetector);
//            } else {
//                LogManager.addLog(getString(R.string.not_found_child_in_viewflipper), LogManager.Types.ERROR, Toast.LENGTH_LONG);
//            }
//        }
//    }

//    public void openRecord(final TetroidRecord record) {
//        this.recordExt = readRecord(getContext(), record);
//        if (recordExt == null) {
//            return;
//        }
//        openRecord();
//        setFullscreen(mainView.isFullscreen());
//    }


    /**
     * Отображение записи
     */
    public void openRecord(TetroidRecord record) {
        int id = R.id.label_record_tags;
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

        tvRecordAuthor.setText(record.getAuthor());
        tvRecordUrl.setText(record.getUrl());
        if (record.getCreated() != null)
            tvRecordDate.setText(record.getCreatedString(getString(R.string.full_date_format_string)));

        // текст
        String textHtml = DataManager.getRecordHtmlTextDecrypted(record);

        recordWebView.getEditor().clearAndFocusEditor();
        recordWebView.getEditor().loadDataWithBaseURL(DataManager.getRecordDirUri(record),
                textHtml, "text/html", "UTF-8", null);
//        recordWebView.getEditor().setHtml(textHtml);
        //            recordWebView.loadUrl(recordContentUrl);
        recordWebView.getEditor().setWebViewClient(new WebViewClient() {

            //            @Override
            //            public void onPageFinished(WebView view, String url) {
            //                showView(MAIN_VIEW_RECORD_TEXT);
            //            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                onUrlClick(url);
                return true;
            }
        });

    }

    private void onUrlClick(String url) {
        if (url.startsWith("mytetra")) {
            // обрабатываем внутреннюю ссылку
            String id = url.substring(url.lastIndexOf('/')+1);
            TetroidRecord record = DataManager.getRecord(id);

            // !!
            // вот тут пока неясно что делать потом с командой Back, например.
//            mainView.openRecord(recordExt);
            openAnotherRecord(this, record);
            // return super.shouldOverrideUrlLoading(view, request);
        } else {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            } catch (Exception ex) {
                LogManager.addLog(ex);
            }
        }
    }

    public static void openAnotherRecord(Context context, TetroidRecord record) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_RECORD_ID, record.getId());
        startActivity(context, RecordActivity.class, bundle);
    }


    public static void openTag(Context context, String tagName) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_TAG_NAME, tagName);
        startActivity(context, MainActivity.class, bundle);
    }

    public static void startActivity(Context context, Class<?> cls, Bundle bundle) {
        Intent intent = new Intent(context, cls);
        context.startActivity(intent, bundle);
    }

    public void setRecordFieldsVisibility(boolean isVisible) {
        recordFieldsLayout.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
    }


//    /**
//     * Чтение записи.
//     * @param record Запись
//     */
//    public static TetroidRecordExt readRecord(Context context, final TetroidRecord record) {
//        if (record == null)
//            return null;
//        TetroidRecordExt recordExt = new TetroidRecordExt(record);
//
//        LogManager.addLog(context.getString(R.string.record_file_reading) + record.getId());
//        String text = DataManager.getRecordHtmlTextDecrypted(record);
//        if (text == null) {
//            LogManager.addLog(context.getString(R.string.error_record_reading), Toast.LENGTH_LONG);
//            return null;
//        }
//        recordExt.setTextHtml(text);
//        recordExt.setTagsHtml(TetroidRecord.createTagsLinksString(record));
//        return recordExt;
//    }



//    private void switchRecordView(View view) {
//        int recordViewId;
//        if (view == fabRecordViewLeft) {
//            recordViewId = (curRecordViewId == RECORD_VIEW_EDITOR)
//                    ? RECORD_VIEW_VIEWER : RECORD_VIEW_EDITOR;
//        } else {
//            recordViewId = (curRecordViewId == RECORD_VIEW_EDITOR)
//                    ? RECORD_VIEW_HTML : RECORD_VIEW_EDITOR;
//        }
//        // сохраняем внесенные изменения в текст
//        saveCurRecord();
//        // переключаем
//        showCurRecord(recordViewId);
//    }

//    private void showRecordView(int recordViewId) {
//    }

//    private void updateRecordView(int recordViewId) {
//        if (recordViewId == RECORD_VIEW_VIEWER) {
////            ViewUtils.hideKeyboard(getContext(), getView());
//        }
//        updateFab(recordViewId);
//
//        miRecordSave.setVisible(recordViewId != RECORD_VIEW_VIEWER);
//        miRecordHtml.setVisible(recordViewId == RECORD_VIEW_EDITOR);
//    }

//    private void updateFab(int recordViewId) {
//        // bottom margin
//        final int defMargin = 12;
//        int bottomMargin = (recordViewId == RECORD_VIEW_EDITOR) ? 56 : defMargin;
//        if (recordViewId == RECORD_VIEW_VIEWER) {
//            fabRecordViewLeft.hide();
//        } else {
//            fabRecordViewLeft.show();
//            RelativeLayout.LayoutParams leftParams = (RelativeLayout.LayoutParams) fabRecordViewLeft.getLayoutParams();
//            leftParams.setMargins(defMargin, defMargin, defMargin, bottomMargin);
//            fabRecordViewLeft.setImageResource((recordViewId == RECORD_VIEW_EDITOR)
//                    ? android.R.drawable.ic_menu_view : android.R.drawable.ic_menu_edit);
//            fabRecordViewLeft.setBackgroundTintList(ColorStateList.valueOf(
//                    getResources().getColor((recordViewId == RECORD_VIEW_EDITOR)
//                            ? R.color.colorYellow_50 : R.color.colorGreen_50)));
//        }
//        if (recordViewId == RECORD_VIEW_HTML) {
//            fabRecordViewRight.hide();
//        } else {
//            fabRecordViewRight.show();
//            RelativeLayout.LayoutParams rightParams = (RelativeLayout.LayoutParams) fabRecordViewRight.getLayoutParams();
//            rightParams.setMargins(defMargin, defMargin, defMargin, bottomMargin);
//
//            fabRecordViewRight.setImageResource((recordViewId == RECORD_VIEW_EDITOR)
//                    ? R.drawable.ic_code_24dp : android.R.drawable.ic_menu_edit);
//            fabRecordViewRight.setBackgroundTintList(ColorStateList.valueOf(
//                    getResources().getColor((recordViewId == RECORD_VIEW_EDITOR)
//                            ? R.color.colorBlue_50 : R.color.colorGreen_50)));
//        }
////        fabRecordViewEdit.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), colorId)));
//    }

//    private void saveCurRecord() {
//        if (curRecordViewId == RECORD_VIEW_EDITOR || curRecordViewId == RECORD_VIEW_HTML) {
//            RecordView view = getCurRecordView();
//            if (view != null) {
//                saveRecord();
//            }
//        }
//    }

    private void saveRecord() {
        String htmlText = recordWebView.getEditor().getHtml();
        DataManager.saveRecordHtmlText(record, htmlText);
    }

//    public RecordView getCurRecordView() {
//        int count = vfRecord.getChildCount();
//        return (curRecordViewId >= 0 && count > 0 && curRecordViewId < count)
//                ? (RecordView)vfRecord.getChildAt(curRecordViewId)
//                : null;
//    }

//    private int getDefaultRecordViewId() {
//        return (SettingsManager.isRecordEditMode()) ? RECORD_VIEW_EDITOR : RECORD_VIEW_VIEWER;
//    }

    /**
     * Обработчик создания системного меню.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.record, menu);
        this.miRecordEdit = menu.findItem(R.id.action_record_edit);
        this.miRecordSave = menu.findItem(R.id.action_record_save);
        this.miRecordHtml = menu.findItem(R.id.action_record_html);

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
            case R.id.action_record_edit:
                return true;
            case R.id.action_record_save:
                return true;
            case R.id.action_record_html:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     *
     * @param detector
     */
    public void setGestureDetector(GestureDetectorCompat detector) {
        this.gestureDetector = detector;
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
        if (gestureDetector != null)
            gestureDetector.onTouchEvent(event);
        return false;
    }
}
