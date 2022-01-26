package com.gee12.mytetroid.model;

import com.gee12.mytetroid.common.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TetroidRecord extends TetroidObject {

    public static final String PREFIX = "note";

    public static int FIELD_ID = 1;
    public static int FIELD_DIR_NAME = 2;
    public static final String DEF_FILE_NAME = "text.html";

    private TetroidNode mNode;
    private String mTagsString;
    private String mAuthor;
    private String mUrl;
    private Date mCreated;
    protected boolean mIsNew;
    protected boolean mIsFavorite;
    protected boolean mIsTemp;

    private String mDirName;
    private String mFileName;
    private List<TetroidFile> mFiles;
    private List<TetroidTag> mTags;

    private String mDecryptedTagsString;
    private String mDecryptedAuthor;
    private String mDecryptedUrl;

    public TetroidRecord(boolean isCrypted, String id, String name, String tagsString, String author, String url,
                         Date created, String dirName, String fileName, TetroidNode node) {
        super(FoundType.TYPE_RECORD, isCrypted, id, name);
        init(tagsString, author, url, created, dirName, fileName, node);
    }

    public TetroidRecord(TetroidRecord record) {
        super(FoundType.TYPE_RECORD, record.isCrypted, record.getId(), record.getName());
        init(record.getTagsString(), record.getAuthor(false), record.getUrl(), record.getCreated(),
                record.getDirName(), record.getFileName(), record.getNode());
    }

    private void init(String tagsString, String author, String url, Date created, String dirName, String fileName, TetroidNode node) {
        this.mTagsString = tagsString;
        this.mAuthor = author;
        this.mUrl = url;
        this.mCreated = created;
        this.mDirName = dirName;
        this.mFileName = fileName;
        this.mFiles = new ArrayList<>();
        this.mTags = new ArrayList<>();
        this.mNode = node;
        this.mIsNew = false;
        this.mIsFavorite = false;
        this.mIsTemp = false;
    }

    public void setDecryptedValues(String name, String tagsString, String author, String url) {
        this.decryptedName = name;
        this.mDecryptedTagsString = tagsString;
        this.mDecryptedAuthor = author;
        this.mDecryptedUrl = url;
    }

    public TetroidNode getNode() {
        return mNode;
    }

    public String getTagsString() {
        return (isCrypted && isDecrypted) ? mDecryptedTagsString : mTagsString;
    }

    public String getTagsString(boolean cryptedValue) {
        return (cryptedValue) ? mTagsString : mDecryptedTagsString;
    }

    public String getAuthor() {
        return (isCrypted && isDecrypted) ? mDecryptedAuthor : mAuthor;
    }

    public String getAuthor(boolean cryptedValue) {
        return (cryptedValue) ? mAuthor : mDecryptedAuthor;
    }

    public String getUrl() {
        return (isCrypted && isDecrypted) ? mDecryptedUrl : mUrl;
    }

    public String getUrl(boolean cryptedValue) {
        return (cryptedValue) ? mUrl : mDecryptedUrl;
    }

    public Date getCreated() {
        return mCreated;
    }

    public String getCreatedString(String format) {
        return (mCreated != null) ? Utils.dateToString(mCreated, format) : "";
    }

    public String getDirName() {
        return mDirName;
    }

    public String getFileName() {
        return mFileName;
    }

    public List<TetroidFile> getAttachedFiles() {
        return mFiles;
    }

    public int getAttachedFilesCount() {
        return mFiles.size();
    }

    public List<TetroidTag> getTags() {
        return mTags;
    }

    /**
     * Сформировать заново строку со метками записи.
     */
    public void updateTagsString() {
        if (mTags == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (TetroidTag tag : mTags) {
            if (!isFirst) {
                sb.append(", ");
            }
            sb.append(tag.getName());
            isFirst = false;
        }
        this.mTagsString = sb.toString();
    }

    /**
     * Получение признака, что запись не зашифрована.
     * @return True, если не зашифровано, или уже расшифровано.
     */
    public boolean isNonCryptedOrDecrypted() {
        return (!isCrypted || isDecrypted);
    }

    public void setTagsString(String tagsString) {
        this.mTagsString = tagsString;
    }

    public void setDecryptedTagsString(String value) {
        this.mDecryptedTagsString = value;
    }

    public void setAuthor(String author) {
        this.mAuthor = author;
    }

    public void setDecryptedAuthor(String value) {
        this.mDecryptedAuthor = value;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    public void setDecryptedUrl(String value) {
        this.mDecryptedUrl = value;
    }

    public void setCreated(Date created) {
        this.mCreated = created;
    }

    public void setDirName(String dirName) {
        this.mDirName = dirName;
    }

    public void setFileName(String mFileName) {
        this.mFileName = mFileName;
    }

    public void setNode(TetroidNode node) {
        this.mNode = node;
    }

    public void addTag(TetroidTag tag) {
        mTags.add(tag);
    }

    public void setAttachedFiles(List<TetroidFile> files) {
        this.mFiles = files;
    }

    public boolean isNew() {
        return mIsNew;
    }

    public void setIsNew(boolean isNew) {
        this.mIsNew = isNew;
    }

    public void setIsFavorite(boolean isFavorite) {
        this.mIsFavorite = isFavorite;
    }

    public boolean isFavorite() {
        return mIsFavorite;
    }

    public void setIsTemp(boolean isTemp) {
        this.mIsTemp = isTemp;
    }

    public boolean isTemp() {
        return mIsTemp;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

}
