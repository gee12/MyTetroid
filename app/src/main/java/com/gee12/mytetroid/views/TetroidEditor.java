package com.gee12.mytetroid.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.lumyjuwon.richwysiwygeditor.WysiwygEditor;

public class TetroidEditor extends WysiwygEditor {

    public static final String HTML_START_WITH =
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0//EN\" \"http://www.w3.org/TR/REC-html40/strict.dtd\">\n" +
                    "<html><head><meta name=\"qrichtext\" content=\"1\" /><style type=\"text/css\">\n" +
                    "p, li { white-space: pre-wrap; }\n" +
                    "</style></head><body style=\" font-family:'DejaVu Sans'; font-size:11pt; font-weight:400; font-style:normal;\">";
    public static final String HTML_END_WITH = "</body></html>";

    public TetroidEditor(Context context) {
        super(context);
    }

    public TetroidEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TetroidEditor(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public String getDocumentHtml(String bodyHtml) {
        StringBuilder sb = new StringBuilder(3);
        sb.append(HTML_START_WITH);
        sb.append(bodyHtml);
        sb.append(HTML_END_WITH);
        return sb.toString();
    }

    public String getDocumentHtml() {
        return getDocumentHtml(webView.getEditableHtml());
    }
}
