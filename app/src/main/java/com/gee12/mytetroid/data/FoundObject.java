package com.gee12.mytetroid.data;

public class FoundObject {

    public static final int TYPE_RECORD_NAME = 0;
    public static final int TYPE_RECORD_TEXT = 1;
    public static final int TYPE_AUTHOR = 2;
    public static final int TYPE_URL = 3;
    public static final int TYPE_FILE = 4;
    public static final int TYPE_TAG = 5;
    public static final int TYPE_NODE = 6;

    private String matchText;
    private Object foundObj;
    private int objType;

    public FoundObject(String matchText, Object foundObj, int objType) {
        this.matchText = matchText;
        this.foundObj = foundObj;
        this.objType = objType;
    }


}
