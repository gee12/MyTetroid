package com.gee12.mytetroid;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для работы с историей открытия объектов приложения.
 */
public class HistoryManager {

    private static List<Object> chain = new ArrayList<>();
    private static int curIndex = -1;

    public static boolean hasPrev() {
        return (curIndex >= 1);
    }

    public static boolean hasNext() {
        return (curIndex < getLastIndex());
    }

    public static Object toPrev() {
        if (hasPrev()) {
            curIndex--;
            return chain.get(curIndex);
        }
        return null;
    }

    public static Object toNext() {
        if (hasNext()) {
            curIndex++;
            return chain.get(curIndex);
        }
        return null;
    }

    public static void addMember(Object obj) {
        // удаляем звенья, вместо которых нужно записать новую историю
        // (когда курсор не в конце цепи)
        if (curIndex < getLastIndex()) {
            for (int i = getLastIndex(); i > curIndex; i--) {
                chain.remove(i);
            }
        }
        chain.add(obj);
        curIndex++;
    }

    public static int getLastIndex() {
        return chain.size()-1;
    }

}
