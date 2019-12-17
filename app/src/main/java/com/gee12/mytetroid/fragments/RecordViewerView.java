package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.TetroidRecord;
import com.gee12.mytetroid.views.TetroidWebView;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class RecordViewerView extends RecordView implements CompoundButton.OnCheckedChangeListener {

    private RelativeLayout recordFieldsLayout;
    private ExpandableLayout expRecordFieldsLayout;
    private ToggleButton tbRecordFieldsExpander;
    //    private TextView tvRecordTags;
    private WebView wvRecordTags;
    private TextView tvRecordAuthor;
    private TextView tvRecordUrl;
    private TextView tvRecordDate;
    private TetroidWebView recordWebView;

    public RecordViewerView(Context context) {
        super(context);
        initView();
    }

    public RecordViewerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public RecordViewerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RecordViewerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView();
    }

    private void initView() {
        View view = this;

        // текст записи
        this.recordWebView = view.findViewById(R.id.web_view_record_text);
        // обработка нажатия на тексте записи
        recordWebView.setOnTouchListener(this);
        recordWebView.getSettings().setBuiltInZoomControls(true);
        recordWebView.getSettings().setDisplayZoomControls(false);
        this.recordFieldsLayout = view.findViewById(R.id.layout_record_fields);
//        this.tvRecordTags =  view.findViewById(R.id.text_view_record_tags);
        this.wvRecordTags =  view.findViewById(R.id.web_view_record_tags);
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
                mainView.openTag(tagName);
                return true;
            }
        });
        this.tvRecordAuthor =  view.findViewById(R.id.text_view_record_author);
        this.tvRecordUrl =  view.findViewById(R.id.text_view_record_url);
        this.tvRecordDate =  view.findViewById(R.id.text_view_record_date);
        this.expRecordFieldsLayout =  view.findViewById(R.id.layout_expander);
        this.tbRecordFieldsExpander =  view.findViewById(R.id.toggle_button_expander);
        tbRecordFieldsExpander.setOnCheckedChangeListener(this);


//        FloatingActionButton fab = view.findViewById(R.id.button_edit_record);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//        });
    }

    @Override
    protected int getViewId() {
        return R.layout.layout_record_viewer;
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.layout_record_editor, container, false);
//        setMainView(getArguments());
//
//        return view;
//    }

    /**
     * Отображение записи
     */
    @Override
    public void openRecord() {
        int id = R.id.label_record_tags;
        if (recordExt.getTagsHtml() != null) {
            // указываем charset в mimeType для кириллицы
            wvRecordTags.loadData(recordExt.getTagsHtml(), "text/html; charset=UTF-8", null);
            id = R.id.web_view_record_tags;
        }
        // указываем относительно чего теперь выравнивать следующую панель
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, id);
        expRecordFieldsLayout.setLayoutParams(params);

        tvRecordAuthor.setText(recordExt.getRecord().getAuthor());
        tvRecordUrl.setText(recordExt.getRecord().getUrl());
        if (recordExt.getRecord().getCreated() != null)
            tvRecordDate.setText(recordExt.getRecord().getCreatedString(getContext().getString(R.string.full_date_format_string)));

        // текст
        recordWebView.loadDataWithBaseURL(DataManager.getRecordDirUri(recordExt.getRecord()),
                recordExt.getTextHtml(), "text/html", "UTF-8", null);
    //            recordWebView.loadUrl(recordContentUrl);
        recordWebView.setWebViewClient(new WebViewClient() {

    //            @Override
    //            public void onPageFinished(WebView view, String url) {
    //                showView(VIEW_RECORD_TEXT);
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
            TetroidRecord recordExt = DataManager.getRecord(id);

            // !!
            // вот тут пока неясно что делать потом с командой Back, например.
            mainView.openRecord(recordExt);
            // return super.shouldOverrideUrlLoading(view, request);
        } else {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                getContext().startActivity(browserIntent);
            } catch (Exception ex) {
                LogManager.addLog(ex);
            }
        }
    }

    public void setRecordFieldsVisibility(boolean isVisible) {
        recordFieldsLayout.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        expRecordFieldsLayout.toggle();
    }

    @Override
    public void setFullscreen(boolean isFullscreen) {
        setRecordFieldsVisibility(!isFullscreen);
    }

//    @Override
//    public String getTitle() {
//        return null;
//    }
}
