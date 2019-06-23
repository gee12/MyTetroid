package com.gee12.mytetroid.data;

import android.content.Context;

import com.gee12.mytetroid.R;

public abstract class FoundObject {

    public static final int TYPES_COUNT = 7;
    public static final int TYPE_RECORD_NAME = 0;
    public static final int TYPE_RECORD_TEXT = 1;
    public static final int TYPE_AUTHOR = 2;
    public static final int TYPE_URL = 3;
    public static final int TYPE_FILE = 4;
    public static final int TYPE_TAG = 5;
    public static final int TYPE_NODE = 6;

//    private String matchText;
//    private T foundObj;
    private int foundType;

    /*public FoundObject(String matchText, T foundObj, int foundType) {
        this.matchText = matchText;
//        this.foundObj = foundObj;
        this.foundType = foundType;
    }*/

    public void addFoundType(int foundType) {
        this.foundType |= (1 << foundType);
    }

//    public String getMatchText() {
//        return matchText;
//    }

//    public T getFoundObj() {
//        return foundObj;
//    }

    public int getFoundType() {
        return foundType;
    }

    public boolean checkFoundType(int type) {
        return ((foundType & (1 << type)) > 0);
    }

    public String  getFoundTypeString(Context context) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (int i = 0; i < TYPES_COUNT; i++)
            if ((foundType & (1 << i)) > 0) {
                if (!isFirst)
                    sb.append(", ");
                sb.append(context.getResources().getStringArray(R.array.found_types)[i]);
                isFirst = false;
            }
        return sb.toString();
    }

    public abstract String getFoundName();

}
