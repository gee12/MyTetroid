package com.gee12.mytetroid.ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.ScrollView;

import androidx.annotation.RequiresApi;

import com.gee12.mytetroid.ui.TextViewSearcher;

@SuppressLint("AppCompatCustomView")
public class TetroidEditText extends EditText implements TextWatcher {

    public interface ITetroidEditTextListener {
        void onAfterTextInited();
        void onAfterTextChanged();
    }

    private ITetroidEditTextListener mListener;
    private TextViewSearcher mTextViewSearcher;
    private int lastTextLength = 0;

    public TetroidEditText(Context context) {
        super(context);
        init();
    }

    public TetroidEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TetroidEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TetroidEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        addTextChangedListener(this);

    }

    public void initSearcher(ScrollView scrollView) {
        this.mTextViewSearcher = new TextViewSearcher(this, scrollView);

    }

    public void setText(String text) {
        super.setText(text);

    }

    public void reset() {
        super.setText(null);

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        this.lastTextLength = getText().length();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mListener != null) {
            if (lastTextLength > 0) {
                mListener.onAfterTextChanged();
            } else {
                mListener.onAfterTextInited();
            }
        }
    }

    public TextViewSearcher getSearcher() {
        return mTextViewSearcher;
    }

    public void setTetroidListener(ITetroidEditTextListener listener) {
        this.mListener = listener;
    }
}
