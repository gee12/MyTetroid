package com.gee12.mytetroid.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Html;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gee12.mytetroid.R;

public class TextViewHtml extends androidx.appcompat.widget.AppCompatTextView {

    public TextViewHtml(@NonNull Context context) {
        super(context);
    }

    public TextViewHtml(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public TextViewHtml(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void init(Context context, @Nullable AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextViewHtml, 0, 0);
        try {
            boolean isHtml = a.getBoolean(R.styleable.TextViewHtml_isHtml, false);
            if (isHtml) {
                String text = a.getString(R.styleable.TextViewHtml_android_text);
                if (text != null) {
                    setText(Html.fromHtml(text));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            a.recycle();
        }
    }
}
