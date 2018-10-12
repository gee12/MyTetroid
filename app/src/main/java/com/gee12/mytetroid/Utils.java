package com.gee12.mytetroid;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParseException;
import com.larvalabs.svgandroid.SVGParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utils {

    public static Drawable loadSVGFromFile(String fullFileName) throws FileNotFoundException, SVGParseException, NullPointerException {
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

    /***
     * Преобразование строки в дату
     * @param dateString
     * @return
     */
    public static Date toDate(String dateString, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        Date convertedDate = null;
        try {
            convertedDate = dateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return convertedDate;
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.equals("");
    }

    /**
     * Преобразование в MD5
     * @param data
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static byte[] getMD5(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(data);
    }
}
