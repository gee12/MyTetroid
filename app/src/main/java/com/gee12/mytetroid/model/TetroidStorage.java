package com.gee12.mytetroid.model;

import com.gee12.mytetroid.data.TetroidXml;
import com.gee12.mytetroid.data.crypt.TetroidCrypter;
import com.gee12.mytetroid.data.ini.DatabaseConfig;

import java.util.Date;
import java.util.List;

public class TetroidStorage {

    // хранилище
    private boolean mIsMain;
    private int mOrder;
    private String mName;
    private String mPath;
//    private boolean mIsUseTrash;
    private String mTrashPath;
    private Date mCreatedDate;
    private Date mEditedDate;
    private boolean mIsReadOnly;
    private String mQuickNodeId;
    private boolean mIsLoadFavoritesOnly;
    private boolean mIsKeepLastNode;
    private String mLastNodeId;

    // шифрование
    private boolean mIsSavePassHash;
    private String mMiddlePassHash;
    private boolean mIsRequestPIN;
    private boolean mPINLength;
    private String mPINHash;
    private int mWhenAskPass;

    // синхронизация
    // ...
    private int mIsCheckOutsideChanging;

    // редактирование
    private int mIsRecordEditMode;
    private int mIsRecordAutoSave;
    private int mIsFixEmptyParagraphs;

    // отображение
    private int mIsKeepScreenOn;
    private int mIsDoubleTapFullscreen;
    private int mIsShowRecordFields;
    private int mIsHighlightRecordWithAttach;
    private int mIsHighlightEncryptedNodes;
    private int mHighlightColor;
    private String mDateFormatString;
    private List<Integer> mRecordFieldsInList;

    private int mIsWriteLogToFile;
    private String mLogPath;
    //
    /**
     * Ветка для быстрой вставки.
     */
    protected TetroidNode mQuicklyNode;

    /**
     *
     */
    protected TetroidCrypter mCrypter;

    /**
     *
     */
    protected DatabaseConfig mDatabaseConfig;

    protected TetroidXml mXml;

    // временное
    boolean isInited;
    boolean isLoaded;
    boolean isDecrypted;
    boolean isCrypted;

    public TetroidStorage(String path) {
        Date curDate = new Date();
        init(path, getFolderNameFromPath(path), curDate, curDate);
    }

    public TetroidStorage(String name, String path, Date created, Date edited) {
        init(name, path, created, edited);
    }

    private void init(String name, String path, Date created, Date edited) {
        this.mName = name;
        this.mPath = path;
        this.mCreatedDate = created;
        this.mEditedDate = edited;
    }

    public static String getFolderNameFromPath(String path) {
        return "";
    }
}
