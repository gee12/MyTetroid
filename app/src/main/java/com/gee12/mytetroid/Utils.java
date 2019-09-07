package com.gee12.mytetroid;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
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
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
        if (isNullOrEmpty(dateString)) {
            return null;
        }
        Date convertedDate = null;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
            convertedDate = dateFormat.parse(dateString);
        } catch (ParseException e) {
//            e.printStackTrace();
        }
        return convertedDate;
    }

    public static boolean checkDateFormatString(String format) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            dateFormat.format(new Date());
        } catch (Exception e) {
//            e.printStackTrace();
            return false;
        }
        return true;
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
        return 0x000000FF & b;
    }

    public static int[] toUnsigned(byte[] ba) {
        if (ba == null)
            return null;
        int[] res = new int[ba.length];
        for (int i = 0; i < ba.length; i ++) {
            res[i] = 0x000000FF & ba[i];
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

//    public static byte[] toBytes(List<Integer> ia) {
//        if (ia == null)
//            return null;
//        byte[] res = new byte[ia.size()];
//        for (int i = 0; i < ia.size(); i ++) {
//            res[i] = ia.get(i).byteValue();
//        }
//        return res;
//    }

    public static String readTextFile(URI fileUrl) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(new File(fileUrl)));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }
        br.close();
        return sb.toString();
    }

    public static byte[] readFile(URI fileUrl) throws IOException {
        File file = new File(fileUrl);
        byte[] data = new byte[(int) file.length()];
        DataInputStream dis;
        dis = new DataInputStream(new FileInputStream(file));
        dis.readFully(data);
        dis.close();
        return data;
    }

    public static String getExtWithComma(String fileFullName) {
        return fileFullName.substring(fileFullName.lastIndexOf("."));
    }

    public static String getAppExtFilesDir(Context context) {
        File file = context.getExternalFilesDir(null);
        return (file != null) ? file.getAbsolutePath() : null;
    }

    /**
     * Получение внешнего каталога по-умоланию.
     *
     * !!!
     * Еще нужно проверять getExternalStorageState().
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

    public static String getVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            LogManager.addLog(e);
        }
        return null;
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

}
