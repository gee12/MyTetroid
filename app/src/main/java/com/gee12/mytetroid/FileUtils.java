package com.gee12.mytetroid;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.Html;
import android.text.Spanned;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParseException;
import com.larvalabs.svgandroid.SVGParser;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class FileUtils {

    /**
     *
     * @param htmlText
     * @return
     */
    public static Spanned fromHtml(String htmlText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(htmlText);
        }
    }
    /**
     *
     * @param fullFileName
     * @return
     * @throws FileNotFoundException
     * @throws SVGParseException
     * @throws NullPointerException
     */
    public static Drawable loadSVGFromFile(String fullFileName) throws FileNotFoundException, SVGParseException, NullPointerException {
        File file = new File(fullFileName);
        FileInputStream fileInputStream = new FileInputStream(file);
        SVG svg = SVGParser.getSVGFromInputStream(fileInputStream);
        return svg.createPictureDrawable();
    }

    /**
     *
     * @param fileUri
     * @return
     * @throws IOException
     */
    public static String readTextFile(Uri fileUri) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(new File(fileUri.getPath())));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }
        br.close();
        return sb.toString();
    }

    /**
     *
     * @param fileUri
     * @return
     * @throws IOException
     */
    public static byte[] readFile(Uri fileUri) throws IOException {
        File file = new File(fileUri.getPath());
        byte[] data = new byte[(int) file.length()];
        DataInputStream dis;
        dis = new DataInputStream(new FileInputStream(file));
        dis.readFully(data);
        dis.close();
        return data;
    }

    /**
     *
     * @param fileFullName
     * @return
     */
    public static String getExtWithComma(String fileFullName) {
        return fileFullName.substring(fileFullName.lastIndexOf("."));
    }

    /**
     *
     * @param context
     * @return
     */
    public static String getAppExtFilesDir(Context context) {
        File file = context.getExternalFilesDir(null);
        return (file != null) ? file.getAbsolutePath() : null;
    }

    /**
     * Получение внешнего каталога по-умоланию.
     *
     * // FIXME: Еще нужно проверять getExternalStorageState().
     *
     * @return
     */
    public static String getExtPublicDocumentsDir() {
//        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        if (Build.VERSION.SDK_INT >= 19) {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        } else {
            return Environment.getExternalStorageDirectory() + "/Documents";
        }
    }
}
