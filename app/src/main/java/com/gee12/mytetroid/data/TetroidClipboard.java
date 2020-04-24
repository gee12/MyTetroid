package com.gee12.mytetroid.data;

import com.gee12.mytetroid.model.TetroidObject;

public class TetroidClipboard {

    static TetroidClipboard mInstance;

    private TetroidObject mObj;
    private boolean mIsCutted;

    public TetroidClipboard() {
        this.mObj = null;
    }

    public static void copy(TetroidObject obj) {
        getInstance().push(obj, false);
    }

    public static void cut(TetroidObject obj) {
        getInstance().push(obj, true);
    }

    private void push(TetroidObject obj, boolean isCut) {
        this.mObj = obj;
        this.mIsCutted = isCut;
    }

    public static TetroidClipboard get() {
        return getInstance();
    }

    public TetroidObject getObject() {
        return mObj;
    }

    public boolean isCutted() {
        return mIsCutted;
    }

    private static TetroidClipboard getInstance() {
        return (mInstance != null) ? mInstance : new TetroidClipboard();
    }
}
