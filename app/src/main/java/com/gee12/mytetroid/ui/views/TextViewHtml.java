package com.gee12.mytetroid.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Html;
import android.text.method.LinkMovementMethod;
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
            // isHtml
            if (a.getBoolean(R.styleable.TextViewHtml_isHtml, false)) {
                String text = a.getString(R.styleable.TextViewHtml_android_text);
                if (text != null) {
                    setText(Html.fromHtml(text));
                }
            }
            // withLinks
            if (a.getBoolean(R.styleable.TextViewHtml_withLinks, false)) {
                setMovementMethod(LinkMovementMethod.getInstance());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            a.recycle();
        }
    }
}
