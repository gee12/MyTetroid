package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.gee12.mytetroid.R;

public class RecordHtmlView extends RecordView {

    private EditText etHtml;

    public RecordHtmlView(Context context) {
        super(context);
        initView();
    }

    public RecordHtmlView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public RecordHtmlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RecordHtmlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView();
    }

    private void initView() {
        this.etHtml = findViewById(R.id.edit_text_html);
    }

    @Override
    protected int getViewId() {
        return R.layout.layout_record_html;
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View rootView = inflater.inflate(R.layout.layout_record_html, container, false);
//        setMainView(getArguments());
//
//
//        return rootView;
//    }


    /**
     * Отображение записи
     */
    @Override
    public void openRecord() {//final TetroidRecordExt record) {
        etHtml.setText(recordExt.getTextHtml());
    }

    @Override
    public String getRecordHtml() {
        return etHtml.getText().toString();
    }

    @Override
    public void setFullscreen(boolean isFullscreen) {

    }

//    @Override
//    public String getTitle() {
//        return null;
//    }
}
