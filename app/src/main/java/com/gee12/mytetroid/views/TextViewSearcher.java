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

    class SpanIndex {
        int index;
        BackgroundColorSpan span;

        public SpanIndex(int index, BackgroundColorSpan span) {
            this.index = index;
            this.span = span;
        }
    }

    private EditText mTextView;
    private ScrollView mScrollView;
    private int mScrollHeight;
    private Editable mEditable;
    private BackgroundColorSpan mMatchSpan = new BackgroundColorSpan(Color.YELLOW);
    private BackgroundColorSpan mCurMatchSpan = new BackgroundColorSpan(Color.rgb(255, 165, 0));
    private String mQuery;
    private List<SpanIndex> mMatches;
    private int mCurIndex;

    public TextViewSearcher(EditText tv, ScrollView sv) {
        this.mTextView = tv;
        this.mScrollView = sv;
        this.mMatches = new ArrayList<>();
    }

    public void findAll(String query) {
        this.mQuery = query;
        this.mEditable = mTextView.getEditableText();
        this.mScrollHeight = mScrollView.getHeight();
        // Reset the index and clear highlighting
//        if (query.length() == 0) {
//            mEditable.removeSpan(mMatchSpan);
//        }
        // Use regex search and spannable for highlighting
        Pattern pattern = Pattern.compile(query,
                Pattern.CASE_INSENSITIVE | Pattern.LITERAL | Pattern.UNICODE_CASE);
        String text = mTextView.getText().toString();
        mMatches.clear();
        Matcher matcher = pattern.matcher(text);
        this.mCurIndex = -1;
        boolean isFirst = true;
        while (matcher.find()) {
            // TODO: использовать один и тот же mMatchSpan или создавать новый 
//            BackgroundColorSpan span = (isFirst) ? mCurMatchSpan : new BackgroundColorSpan(Color.YELLOW);
            BackgroundColorSpan span = (isFirst) ? mCurMatchSpan : mMatchSpan;
            SpanIndex spanIndex = new SpanIndex(matcher.start(), span);
            mMatches.add(spanIndex);
            showMatch(spanIndex);
        }
//        if (!mMatches.isEmpty()) {
//            this.mCurIndex = 0;
//            showMatch(mCurIndex);
//        }
    }

    public void nextMatch() {
        if (mCurIndex < 0)
            return;
        if (mCurIndex == mMatches.size() - 1) {
            mCurIndex = 0;
        } else {
            mCurIndex++;
        }
        // TODO: заменить старый span на новый
        // ...
        showMatch(mCurIndex);
    }

    public void prevMatch() {
        if (mCurIndex < 0)
            return;
        int oldIndex = mCurIndex;
        if (mCurIndex == 0) {
            mCurIndex = mMatches.size() - 1;
        } else {
            mCurIndex--;
        }

        // TODO: заменить старый span на новый
        SpanIndex oldSpanIndex = mMatches.get(oldIndex);
//        mEditable.removeSpan(oldSpanIndex.span);
        oldSpanIndex.span = mMatchSpan;

        showMatch(mCurIndex);
    }

    private void showMatch(int index) {
        SpanIndex spanIndex = mMatches.get(index);
        showMatch(spanIndex);
    }

    private void showMatch(SpanIndex spanIndex) {
        showMatch(spanIndex.index, spanIndex.span);
    }

    private void showMatch(int startIndex, BackgroundColorSpan span) {
        int line = mTextView.getLayout().getLineForOffset(startIndex);
        int pos = mTextView.getLayout().getLineBaseline(line);
        // Scroll to it
        mScrollView.scrollTo(0, pos - mScrollHeight / 2);
        // Highlight it
        mEditable.setSpan(span, startIndex, startIndex + mQuery.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public void stopSearch() {
        // TODO: удалить все span или достаточно только mMatchSpan
        for (SpanIndex spanIndex : mMatches) {
            mEditable.removeSpan(spanIndex);
        }
    }

}
