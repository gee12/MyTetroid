package com.gee12.mytetroid.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
     * Преобразование в MD5
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
    public static List<Integer> toUnsigned2(byte[] ba) {
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
            LogManager.addLog(e);
        }
        return null;
    }

    /**
     * Преобразование количество байт в удобочитаемый формат.
     * @param context
     * @param size Количество байт
     * @return
     */
    public static String sizeToString(Context context, long size) {
        if (size == 0) {
            return null;
        } else if (size >= 1073741824) {
            return (size / 1073741824) + context.getString(R.string.g_bytes);
        } else if (size >= 1048576) {
            return (size / 1048576) + context.getString(R.string.m_bytes);
        } else if (size >= 1024) {
            return (size / 1024) + context.getString(R.string.k_bytes);
        } else {
            return String.valueOf(size);
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
}
