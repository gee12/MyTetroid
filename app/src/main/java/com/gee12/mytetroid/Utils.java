package com.gee12.mytetroid;

import android.graphics.drawable.Drawable;
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
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        return s == null || s.isEmpty();
    }

    /**
     * Преобразование в MD5
     * @param data Массив байт данных (можно signed, не имеет значение)
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static byte[] toMD5(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(data);
    }

    public static int toUnsigned(byte b) {
        return 0x000000FF & (int)b;
    }

    public static int[] toUnsigned(byte[] ba) {
        if (ba == null)
            return null;
        int[] res = new int[ba.length];
        for (int i = 0; i < ba.length; i ++) {
            res[i] = toUnsigned(ba[i]);
        }
        return res;
    }

    public static long toUnsignedInt(long i) {
        return 0x00000000FFFFFFFFL & i;
    }

    public static byte[] toBytes(int[] ia) {
        if (ia == null)
            return null;
        byte[] res = new byte[ia.length];
        for (int i = 0; i < ia.length; i ++) {
            res[i] = (byte)(ia[i]);
        }
        return res;
    }

    public static String readTextFile(URI fileUrl) {
        StringBuilder sb = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(fileUrl)));
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            return null;
        }
        return sb.toString();
    }

    public static byte[] readFile(URI fileUrl) {
        File file = new File(fileUrl);
        byte[] data = new byte[(int) file.length()];
        DataInputStream dis;
        try {
            dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(data);
            dis.close();
        } catch (IOException e) {
            return null;
        }
        return data;
    }

    public static String getExtWithComma(String fileFullName) {
        return fileFullName.substring(fileFullName.lastIndexOf("."));
    }
}
