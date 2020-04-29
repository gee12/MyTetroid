package com.gee12.mytetroid.data;

import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidObject;
import com.gee12.mytetroid.model.TetroidRecord;

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

    public static void clear() {
        if (mInstance == null)
            return;
        mInstance.push(null, false);
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

    /**
     *
     * @param type
     * @return
     */
    public static boolean checkType(int type) {
        if (mInstance == null || mInstance.mObj == null)
            return false;
        return (type == FoundType.TYPE_RECORD && mInstance.mObj instanceof TetroidRecord
            || type == FoundType.TYPE_NODE && mInstance.mObj instanceof TetroidNode
                || type == FoundType.TYPE_FILE && mInstance.mObj instanceof TetroidFile);
    }

    private static TetroidClipboard getInstance() {
        if (mInstance == null) {
            mInstance = new TetroidClipboard();
        }
        return mInstance;
    }
}
