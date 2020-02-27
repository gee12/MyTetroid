package com.gee12.mytetroid.utils;

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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
        if (fileUri == null)
            return null;
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
     * Чтение файла.
     * @param fileUri
     * @return
     * @throws IOException
     */
    public static byte[] readFile(Uri fileUri) throws IOException {
        if (fileUri == null)
            return null;
        File file = new File(fileUri.getPath());
        byte[] data = new byte[(int) file.length()];
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        dis.readFully(data);
        dis.close();
        return data;
    }

    /**
     * Запись файла.
     * @param fileUri
     * @param text
     * @return
     * @throws IOException
     */
    public static boolean writeFile(Uri fileUri, String text) throws IOException {
        if (fileUri == null || text == null)
            return false;
        return writeFile(fileUri, text.getBytes());
    }

    /**
     * Запись файла.
     * @param fileUri
     * @param bytes
     * @return
     * @throws IOException
     */
    public static boolean writeFile(Uri fileUri, byte[] bytes) throws IOException {
        if (fileUri == null || bytes == null)
            return false;
        File file = new File(fileUri.getPath());
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
        dos.write(bytes);
        dos.flush();
        dos.close();
        return true;
    }

    /**
     * Получение расширения файла с точкой.
     * @param fileFullName
     * @return Расширение файла с точкой, или пустую строку, если расширение отсутствует.
     */
    public static String getExtWithComma(String fileFullName) {
        int extIndex = fileFullName.lastIndexOf(".");
        return (extIndex > -1) ? fileFullName.substring(extIndex) : "";
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
     * FIXME: Еще нужно проверять Environment.getExternalStorageState():
     * Environment.MEDIA_MOUNTED или Environment.MEDIA_MOUNTED_READ_ONLY
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
