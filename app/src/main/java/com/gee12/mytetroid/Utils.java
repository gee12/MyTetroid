package com.gee12.mytetroid;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Utils {

    public static Drawable loadSVGFromFile(String fullFileName) throws FileNotFoundException {
        File file = new File(fullFileName);
        FileInputStream fileInputStream = new FileInputStream(file);
        SVG svg = SVGParser.getSVGFromInputStream(fileInputStream);
        return svg.createPictureDrawable();
    }

    public static Spanned fromHtml(String htmlText) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(htmlText);
        }
    }
}
