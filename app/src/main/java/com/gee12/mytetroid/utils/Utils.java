package com.gee12.mytetroid.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.Spanned;
import android.text.TextUtils;

import androidx.core.text.HtmlCompat;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.logs.LogManager;

import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
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
     * Удаление элемента из массива строк по индексу.
     * @param array
     * @param index
     */
    public static String[] removeArrayItem(String[] array, int index) {
        if (array == null || index < 0 || index + 1 >= array.length)
            return array;
        return Arrays.copyOfRange(array, index + 1, array.length);
    }

    public static int[] splitToInts(String s, String separ) {
        if (s == null)
            return null;
        String[] parts = s.split(separ);
        int[] res = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                res[i] = Integer.parseInt(parts[i]);
            } catch(Exception ex) {}
        }
        return res;
    }

    public static String concatToString(int[] arr, String separ) {
        if (arr == null)
            return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) {
                sb.append(separ);
            }
        }
        return sb.toString();
//        return String.join(separ, parts);
//        return StringUtils.join(ArrayUtils.toObject(arr), " - ");
//        return Arrays.stream(arr).mapToObj(String::valueOf)
//                .collect(Collectors.joining(" - "));
    }

    public static int[] addElem(int[] arr, int value) {
        if (arr == null)
            return null;
        arr  = Arrays.copyOf(arr, arr.length + 1);
        arr[arr.length - 1] = value;
        return arr;
//        ArrayUtils.add(arr, value);
    }

    /**
     * Добавление элемента в конец массива.
     * @param arr Исходный массив
     * @param value Значение нового элемента
     * @param maxLength Максимальное количество элементов нового массива
     * @return Копия исходного массива arr с добавленным элементом в конце
     */
    public static int[] addElem(int[] arr, int value, int maxLength, boolean addNotUnique) {
        if (maxLength <= 0)
            return null;
        int[] res;
        if (arr == null) {
            res = new int[1];
            res[0] = value;
            return res;
        } else if (!addNotUnique) {
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == value) {
                    res = new int[arr.length];
                    System.arraycopy(arr, 0, res, 0, arr.length);
                    return res;
                }
            }
        }
        if (arr.length >= maxLength) {
            res = new int[arr.length];
            // смещение элементов влево
            System.arraycopy(arr, 1, res, 0, maxLength - 1);
            // установка нового цвета в конец
            res[maxLength - 1] = value;
        } else {
            res = new int[arr.length + 1];
            System.arraycopy(arr, 0, res, 0, arr.length);
            res[arr.length] = value;
        }
        return res;
    }

    public static int[] removeElem(int[] arr, int value) {
        if (arr == null)
            return null;
        ArrayList<Integer> res = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != value) {
                res.add(arr[i]);
            }
        }
//        return res.stream().mapToInt(i -> i).toArray();
//        return ArrayUtils.toPrimitive(res.toArray(new Integer[0]));
        return convertToInts(res);
    }

    public static int[] convertToInts(List<Integer> integers)
    {
        int[] ret = new int[integers.size()];
        Iterator<Integer> iterator = integers.iterator();
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = iterator.next().intValue();
        }
        return ret;
    }

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
            LogManager.log(context, e);
        }
        return null;
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
    public static boolean swapListItems(List list, int pos, boolean isUp, boolean through)
            throws IndexOutOfBoundsException {
        if (list == null)
            return false;
        if (isUp) {
            if (pos > 0 || through && pos == 0) {
                int newPos = (through && pos == 0) ? list.size() - 1 : pos - 1;
                Collections.swap(list, newPos, pos);
                return true;
            }
        } else {
            if (pos < list.size() - 1 || through && pos == list.size() - 1) {
                int newPos = (through && pos == list.size() - 1) ? 0 : pos + 1;
                Collections.swap(list, pos, newPos);
                return true;
            }
        }
        return false;
    }
}
