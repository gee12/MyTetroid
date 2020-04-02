package com.gee12.mytetroid.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TetroidRecord extends TetroidObject {

    public static final String PREFIX = "note";

    public static int FIELD_ID = 1;
    public static int FIELD_DIR_NAME = 2;
    public static final String DEF_FILE_NAME = "text.html";

    private TetroidNode node;
    private String tagsString;
    private String author;
    private String url;
    private Date created;
    protected boolean isNew;

    private String dirName;
    private String fileName;
    List<TetroidFile> files;
    List<TetroidTag> tags;

    public TetroidRecord(boolean isCrypted, String id, String name, String tagsString, String author, String url,
                         Date created, String dirName, String fileName, TetroidNode node) {
        super(FoundType.TYPE_RECORD, isCrypted, id, name);
        this.tagsString = tagsString;
        this.author = author;
        this.url = url;
        this.created = created;
        this.dirName = dirName;
        this.fileName = fileName;
        this.files = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.node = node;
        this.isNew = false;
    }

//    public TetroidRecord(boolean isCrypted, String id, Date created, String dirName, String fileName, TetroidNode node) {
//        super(FoundType.TYPE_RECORD, id, name, isCrypted);
//        this.isCrypted = isCrypted;
//        this.id = id;
//        this.created = created;
//        this.dirName = dirName;
//        this.fileName = fileName;
//        this.node = node;
//        this.files = new ArrayList<>();
//        this.tags = new ArrayList<>();
//    }

    public TetroidNode getNode() {
        return node;
    }

    public String getTagsString() {
        return tagsString;
    }

    public String getAuthor() {
        return author;
    }

    public String getUrl() {
        return url;
    }

    public Date getCreated() {
        return created;
    }

    public String getCreatedString(String format) {
        return (created != null) ? new SimpleDateFormat(format).format(created) : "";
//        return dateFormat.format(created);
    }

    public String getDirName() {
        return dirName;
    }

    public String getFileName() {
        return fileName;
    }

    public List<TetroidFile> getAttachedFiles() {
        return files;
    }

    public int getAttachedFilesCount() {
        return files.size();
    }

    public List<TetroidTag> getTags() {
        return tags;
    }

    /**
     * Получение признака, что запись не зашифрована.
     * @return True, если не зашифровано, или уже расшифровано.
     */
    public boolean isNonCryptedOrDecrypted() {
        return (!isCrypted || isDecrypted);
    }

    public void setTagsString(String tagsString) {
        this.tagsString = tagsString;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setDirName(String dirName) {
        this.dirName = dirName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setNode(TetroidNode node) {
        this.node = node;
    }

    public void addTag(TetroidTag tag) {
        tags.add(tag);
    }

    public void setAttachedFiles(List<TetroidFile> files) {
        this.files = files;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
