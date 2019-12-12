package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.TetroidRecord;
import com.gee12.mytetroid.views.TetroidWebView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class ViewerFragment extends RecordFragment implements CompoundButton.OnCheckedChangeListener {

    private RelativeLayout recordFieldsLayout;
    private ExpandableLayout expRecordFieldsLayout;
    private ToggleButton tbRecordFieldsExpander;
    //    private TextView tvRecordTags;
    private WebView wvRecordTags;
    private TextView tvRecordAuthor;
    private TextView tvRecordUrl;
    private TextView tvRecordDate;
    private TetroidWebView recordWebView;

    public ViewerFragment(Context context) {
        super(context);

        View view = this;

        // текст записи
        this.recordWebView = view.findViewById(R.id.web_view_record_content);
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


        FloatingActionButton fab = view.findViewById(R.id.button_edit_record);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_editor, container, false);
//        setMainView(getArguments());
//
//        return view;
//    }

    /**
     * Отображение записи
     * @param record Запись
     */
    public void showRecord(final TetroidRecord record) {
        if (record == null)
            return;
//        this.curRecord = record;
        this.record = record;

        LogManager.addLog("Чтение записи: id=" + record.getId());
        String text = DataManager.getRecordHtmlTextDecrypted(record);
        if (text == null) {
            LogManager.addLog("Ошибка чтения записи", Toast.LENGTH_LONG);
            return;
        }
        // поля
//                tvRecordTags.setText(record.getTagsString());
        String tagsString = record.getTagsLinksString();
        int id = R.id.label_record_tags;
        if (tagsString != null) {
            // указываем charset в mimeType для кириллицы
            wvRecordTags.loadData(tagsString, "text/html; charset=UTF-8", null);
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
        recordWebView.loadDataWithBaseURL(DataManager.getRecordDirUri(record),
                text, "text/html", "UTF-8", null);
//            recordWebView.loadUrl(recordContentUrl);
        recordWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                showView(VIEW_RECORD_TEXT);
            }

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
            showRecord(record);
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

    public void setRecordFieldsVisibility(boolean isVisible) {
        recordFieldsLayout.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        expRecordFieldsLayout.toggle();
    }

//    @Override
//    public String getTitle() {
//        return null;
//    }
}
