package com.gee12.mytetroid.views;

import android.graphics.Color;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.widget.EditText;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextViewSearcher {

    private EditText mTextView;
    private ScrollView mScrollView;
    private int mScrollHeight;
    private Editable mEditable;
    private BackgroundColorSpan mSpan = new BackgroundColorSpan(Color.YELLOW);
    private String mQuery;
    private List<Integer> mMatches;
    private int mCurIndex;

    public TextViewSearcher(EditText tv, ScrollView sv) {
        this.mTextView = tv;
        this.mEditable = mTextView.getEditableText();
        this.mScrollView = sv;
        this.mScrollHeight = sv.getHeight();
        this.mMatches = new ArrayList<>();
    }

    public void findAll(String query) {
        this.mQuery = query;
        // Reset the index and clear highlighting
        if (query.length() == 0) {
            mEditable.removeSpan(mSpan);
        }
        // Use regex search and spannable for highlighting
        Pattern pattern = Pattern.compile(query,
                Pattern.CASE_INSENSITIVE | Pattern.LITERAL | Pattern.UNICODE_CASE);
        String text = mTextView.getText().toString();
        mMatches.clear();
        Matcher matcher = pattern.matcher(text);
        this.mCurIndex = -1;
        int index = 0;
        while (matcher.find()) {
            index = matcher.start();
            mMatches.add(index);
        }
    }

    public void nextMatch() {
        if (mCurIndex < 0)
            return;
        if (mCurIndex == mMatches.size() - 1) {
            mCurIndex = 0;
        } else {
            mCurIndex++;
        }
        showMatch(mCurIndex);
    }

    public void prevMatch() {
        if (mCurIndex < 0)
            return;
        if (mCurIndex == 0) {
            mCurIndex = mMatches.size() - 1;
        } else {
            mCurIndex--;
        }
        showMatch(mCurIndex);
    }

    private void showMatch(int index) {
        int line = mTextView.getLayout().getLineForOffset(index);
        int pos = mTextView.getLayout().getLineBaseline(line);
        // Scroll to it
        mScrollView.scrollTo(0, pos - mScrollHeight / 2);
        // Highlight it
        mEditable.setSpan(mSpan, index, index + mQuery.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public void stopSearch() {
        mEditable.removeSpan(mSpan);
    }

}
