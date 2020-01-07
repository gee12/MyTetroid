package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
//        ViewUtils.disableCopyAndPaste(etHtml);
//        etHtml.requestFocus();

        etHtml.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
//                if (!hasFocus) {
//                    etHtml.clearFocus();
//                } else {
//                    etHtml.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            etHtml.requestFocus();
//                            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//                            imm.showSoftInput(etHtml, InputMethodManager.SHOW_IMPLICIT);
//                        }
//                    });
//                }
                mainView.setKeyboard(hasFocus);
            }
        });
//        getDialog().getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    public void setFocus(EditText view) {
        view.requestFocus();
        InputMethodManager mgr = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
//        view.setSelection(view.getText().length());
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
