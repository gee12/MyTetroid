package com.gee12.mytetroid.helpers;

import androidx.annotation.NonNull;

public class SortHelper {

    public static final String SORT_BY_NAME = "name";
    public static final String SORT_BY_COUNT = "count";
    public static final String SORT_ASC = " ASC";
    public static final String SORT_DESC = " DESC";

    private String mField;
    private boolean mIsAscent;

    public SortHelper() {
    }

    public SortHelper(String value) {
        if (value == null)
            return;
        mField = (value.startsWith(SORT_BY_COUNT)) ? SORT_BY_COUNT : SORT_BY_NAME;
        String dir = value.substring(mField.length());
        mIsAscent = (dir == null || !dir.startsWith(SORT_DESC));
    }

    public static String byNameAsc() {
        return SORT_BY_NAME + SORT_ASC;
    }

    public static String byNameDesc() {
        return SORT_BY_NAME + SORT_DESC;
    }

    public static String byCountAsc() {
        return SORT_BY_COUNT + SORT_ASC;
    }

    public static String byCountDesc() {
        return SORT_BY_COUNT + SORT_DESC;
    }

    public boolean isByName() {
        return (SORT_BY_NAME.equals(mField));
    }

    public boolean isAscent() {
        return mIsAscent;
    }

    @NonNull
    @Override
    public String toString() {
        return mField + ((mIsAscent) ? SORT_ASC : SORT_DESC);
    }
}
