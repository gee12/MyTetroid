package com.gee12.mytetroid.model;

import com.gee12.mytetroid.data.TetroidXml;
import com.gee12.mytetroid.data.crypt.TetroidCrypter;
import com.gee12.mytetroid.data.ini.DatabaseConfig;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Date;
import java.util.List;

public class TetroidStorage {

    // хранилище
    private int mId;
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

    public TetroidStorage(int id, String name, String path, boolean isReadOnly) {
        Date curDate = new Date();
        init(id, name, path, curDate, curDate);
        this.mIsReadOnly = isReadOnly;
    }

    public TetroidStorage(String path) {
        Date curDate = new Date();
        init(0, getFolderNameFromPath(path), path, curDate, curDate);
    }

    public TetroidStorage(String name, String path, Date created, Date edited) {
        init(0, name, path, created, edited);
    }

    private void init(int id, String name, String path, Date created, Date edited) {
        this.mId = id;
        this.mName = name;
        this.mPath = path;
        this.mCreatedDate = created;
        this.mEditedDate = edited;
    }

    public static String getFolderNameFromPath(String path) {
        return new File(path).getName();
    }

    public int getId() {
        return mId;
    }

    public boolean isMain() {
        return mIsMain;
    }

    public int getOrder() {
        return mOrder;
    }

    public String getName() {
        return mName;
    }

    public String getPath() {
        return mPath;
    }

    public String getTrashPath() {
        return mTrashPath;
    }

    public Date getCreatedDate() {
        return mCreatedDate;
    }

    public Date getEditedDate() {
        return mEditedDate;
    }

    public boolean isReadOnly() {
        return mIsReadOnly;
    }

    public String getQuickNodeId() {
        return mQuickNodeId;
    }

    public boolean isIsLoadFavoritesOnly() {
        return mIsLoadFavoritesOnly;
    }

    public boolean isKeepLastNode() {
        return mIsKeepLastNode;
    }

    public String getLastNodeId() {
        return mLastNodeId;
    }

    public boolean isSavePassHash() {
        return mIsSavePassHash;
    }

    public String getMiddlePassHash() {
        return mMiddlePassHash;
    }

    public boolean isRequestPIN() {
        return mIsRequestPIN;
    }

    public boolean isPINLength() {
        return mPINLength;
    }

    public String getPINHash() {
        return mPINHash;
    }

    public int getWhenAskPass() {
        return mWhenAskPass;
    }

    public int isCheckOutsideChanging() {
        return mIsCheckOutsideChanging;
    }

    public int isRecordEditMode() {
        return mIsRecordEditMode;
    }

    public int isRecordAutoSave() {
        return mIsRecordAutoSave;
    }

    public int isFixEmptyParagraphs() {
        return mIsFixEmptyParagraphs;
    }

    public int isKeepScreenOn() {
        return mIsKeepScreenOn;
    }

    public int isDoubleTapFullscreen() {
        return mIsDoubleTapFullscreen;
    }

    public int isShowRecordFields() {
        return mIsShowRecordFields;
    }

    public int isHighlightRecordWithAttach() {
        return mIsHighlightRecordWithAttach;
    }

    public int isHighlightEncryptedNodes() {
        return mIsHighlightEncryptedNodes;
    }

    public int getHighlightColor() {
        return mHighlightColor;
    }

    public String getDateFormatString() {
        return mDateFormatString;
    }

    public List<Integer> getRecordFieldsInList() {
        return mRecordFieldsInList;
    }

    public int isWriteLogToFile() {
        return mIsWriteLogToFile;
    }

    public String getLogPath() {
        return mLogPath;
    }

    public TetroidNode getQuicklyNode() {
        return mQuicklyNode;
    }

    public TetroidCrypter getCrypter() {
        return mCrypter;
    }

    public DatabaseConfig getDatabaseConfig() {
        return mDatabaseConfig;
    }

    public TetroidXml getXml() {
        return mXml;
    }

    public boolean isInited() {
        return isInited;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public boolean isDecrypted() {
        return isDecrypted;
    }

    public boolean isCrypted() {
        return isCrypted;
    }
}
