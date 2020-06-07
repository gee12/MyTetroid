package com.gee12.mytetroid.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.Spanned;
import android.text.TextUtils;

import androidx.core.text.HtmlCompat;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Utils {

    /**
     * Проверка рравенства двух строк.
     * @param one
     * @param two
     * @param checkCase
     * @return
     */
    public static boolean isEquals(String one, String two, boolean checkCase) {
        return (one != null && (checkCase && one.equals(two) || !checkCase && one.equalsIgnoreCase(two)));
    }

    /***
     * Преобразование строки в дату.
     * @param dateString
     * @return
     */
    public static Date toDate(String dateString, String pattern) {
        if (TextUtils.isEmpty(dateString)) {
            return null;
        }
        Date convertedDate = null;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
            convertedDate = dateFormat.parse(dateString);
        } catch (ParseException e) {
//            e.printStackTrace();
        }
        return convertedDate;
    }

    /**
     * Преобразование строки в дату.
     * @param date
     * @param pattern
     * @return
     */
    public static String dateToString(Date date, String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(date);
    }

    /**
     *
     * @param format
     * @return
     */
    public static boolean checkDateFormatString(String format) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
            dateFormat.format(new Date());
        } catch (Exception e) {
//            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Преобразование html-кода в текст.
     * @param htmlText
     * @return
     */
    public static Spanned fromHtml(String htmlText) {
//        if (Build.VERSION.SDK_INT >= 24) {
//            return Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY);
//        } else {
//            return Html.fromHtml(htmlText);
//        }
        return HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    public static String getStringFormat(String format, Object... args) {
        return String.format(Locale.getDefault(), format, args);
    }

    public static String getStringFormat(Context context, int format, Object... args) {
        return String.format(Locale.getDefault(), context.getString(format), args);
    }

    /**
     * Преобразование текста в MD5.
     * @param data Массив байт данных (можно signed, не имеет значение)
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static byte[] toMD5(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(data);
    }

    /**
     *
     * @param b
     * @return
     */
    public static int toUnsigned(byte b) {
        return 0x000000FF & b;
    }

    /**
     *
     * @param ba
     * @return
     */
    public static int[] toUnsigned(byte[] ba) {
        if (ba == null)
            return null;
        int[] res = new int[ba.length];
        for (int i = 0; i < ba.length; i ++) {
            res[i] = 0x000000FF & ba[i];
        }
        return res;
    }

    /**
     *
     * @param ba
     * @return
     */
    public static List<Integer> toUnsignedList(byte[] ba) {
        if (ba == null)
            return null;
        List<Integer> res = new ArrayList<>(ba.length);
        for (int i = 0; i < ba.length; i ++) {
            res.add(i, 0x000000FF & ba[i]);
        }
        return res;
    }

    /**
     *
     * @param i
     * @return
     */
    public static long toUnsignedInt(long i) {
        return 0x00000000FFFFFFFFL & i;
    }

    /**
     *
     * @param ia
     * @return
     */
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

    /**
     *
     * @param context
     * @return
     */
    public static String getVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            LogManager.log(e);
        }
        return null;
    }

    /**
     * Преобразование количество байт в удобочитаемый формат.
     * @param context
     * @param size Количество байт
     * @return
     */
    public static String fileSizeToString(Context context, long size) {
        if (size >= 1073741824) {
            return (size / 1073741824) + context.getString(R.string.g_bytes);
        } else if (size >= 1048576) {
            return (size / 1048576) + context.getString(R.string.m_bytes);
        } else if (size >= 1024) {
            return (size / 1024) + context.getString(R.string.k_bytes);
        } else {
            return size + context.getString(R.string.bytes);
        }
    }

    /**
     * Запись текста в буфер обмена.
     * @param context
     * @param label
     * @param text
     */
    public static void writeToClipboard(Context context, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }

    /**
     *
     * @param length
     * @return
     */
    public static byte[] createRandomBytes(int length) {
        byte[] res = new byte[length];
        Random rand = new Random();
        for (int i = 0; i < length; i++){
            res[i] = (byte) Math.abs(rand.nextInt() % 0xFF);
        }
        return res;
    }

    /**
     *
     * @param list
     * @param pos
     * @param isUp
     * @return
     */
    public static boolean swapListItems(List list, int pos, boolean isUp) {
        if (list == null)
            return false;
        if (isUp) {
            if (pos > 0) {
                Collections.swap(list, pos-1, pos);
                return true;
            }
        } else {
            if (pos < list.size() - 1) {
                Collections.swap(list, pos, pos+1);
                return true;
            }
        }
        return false;
    }
}
