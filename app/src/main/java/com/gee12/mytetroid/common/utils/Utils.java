package com.gee12.mytetroid.common.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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

}
