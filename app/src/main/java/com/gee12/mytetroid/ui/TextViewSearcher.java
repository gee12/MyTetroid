package com.gee12.mytetroid.ui;

import android.graphics.Color;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.widget.EditText;
import android.widget.ScrollView;

import org.jsoup.internal.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Поисковик текста в EditText.
 * Основа взята отсюда: https://billthefarmer.github.io/blog/android-text-search/
 */
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
    private BackgroundColorSpan mCurMatchSpan = new BackgroundColorSpan(Color.rgb(255, 165, 0));
    private String mQuery;
    private List<SpanIndex> mMatches;
    private int mCurIndex;

    /**
     *
     * @param tv
     * @param sv
     */
    public TextViewSearcher(EditText tv, ScrollView sv) {
        this.mTextView = tv;
        this.mScrollView = sv;
        this.mMatches = new ArrayList<>();
    }

    /**
     *
     * @param query
     * @return
     */
    public int findAll(String query) {
        if (StringUtil.isBlank(query))
            return -1;
        this.mQuery = query;
        this.mEditable = mTextView.getEditableText();
        this.mScrollHeight = mScrollView.getHeight();
        // убираем выделение
        removeSpans();
        // создаем критерий поиска
        Pattern pattern = Pattern.compile(query,
                Pattern.CASE_INSENSITIVE | Pattern.LITERAL | Pattern.UNICODE_CASE);
        String text = mTextView.getText().toString();
        mMatches.clear();
        Matcher matcher = pattern.matcher(text);
        this.mCurIndex = -1;

        // выделяем первое совпадение и отображаем
        if (matcher.find()) {
            SpanIndex spanIndex = new SpanIndex(matcher.start(), mCurMatchSpan);
            mMatches.add(spanIndex);
            this.mCurIndex = 0;
            setSpan(spanIndex.index, spanIndex.span);
            showMatch(mCurIndex);

            // выделяем остальные совпадения
            while (matcher.find()) {
                spanIndex = new SpanIndex(matcher.start(), new BackgroundColorSpan(Color.YELLOW));
                mMatches.add(spanIndex);
                setSpan(spanIndex.index, spanIndex.span);
            }
        }
        return mMatches.size();
    }

    /**
     *
     */
    public void nextMatch() {
        if (mCurIndex < 0)
            return;
        int oldIndex = mCurIndex;
        if (mCurIndex == mMatches.size() - 1) {
            mCurIndex = 0;
        } else {
            mCurIndex++;
        }
        updateSpans(oldIndex, mCurIndex);
        showMatch(mCurIndex);
    }

    /**
     *
     */
    public void prevMatch() {
        if (mCurIndex < 0)
            return;
        int oldIndex = mCurIndex;
        if (mCurIndex == 0) {
            mCurIndex = mMatches.size() - 1;
        } else {
            mCurIndex--;
        }
        updateSpans(oldIndex, mCurIndex);
        showMatch(mCurIndex);
    }

    /**
     *
     * @param oldIndex
     * @param newIndex
     */
    private void updateSpans(int oldIndex, int newIndex) {
        SpanIndex oldSpanIndex = mMatches.get(oldIndex);
        mEditable.removeSpan(oldSpanIndex.span);
        oldSpanIndex.span = new BackgroundColorSpan(Color.YELLOW);
        setSpan(oldSpanIndex.index, oldSpanIndex.span);

        SpanIndex curSpanIndex = mMatches.get(newIndex);
        mEditable.removeSpan(curSpanIndex.span);
        curSpanIndex.span = mCurMatchSpan;
        setSpan(curSpanIndex.index, curSpanIndex.span);

    }

    /**
     *
     * @param index
     */
    private void showMatch(int index) {
        int startIndex = mMatches.get(index).index;
        int line = mTextView.getLayout().getLineForOffset(startIndex);
        int pos = mTextView.getLayout().getLineBaseline(line);
        mScrollView.scrollTo(0, pos - mScrollHeight / 2);
    }

    /**
     *
     * @param startIndex
     * @param span
     */
    private void setSpan(int startIndex, BackgroundColorSpan span) {
        mEditable.setSpan(span, startIndex, startIndex + mQuery.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     *
     */
    public void stopSearch() {
        removeSpans();
    }

    /**
     *
     */
    private void removeSpans() {
        for (SpanIndex spanIndex : mMatches) {
            mEditable.removeSpan(spanIndex.span);
        }
    }

}
