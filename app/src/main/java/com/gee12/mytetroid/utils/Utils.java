package com.gee12.mytetroid.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {

    /**
     *
     * @param one
     * @param two
     * @param checkCase
     * @return
     */
    public static boolean isEquals(String one, String two, boolean checkCase) {
        return (one != null && (checkCase && one.equals(two) || !checkCase && one.equalsIgnoreCase(two)));
    }

    /***
     * Преобразование строки в дату
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
}
