package com.gee12.mytetroid.model;

import com.gee12.mytetroid.utils.FileUtils;

public class TetroidFile extends TetroidObject {

    public static final String PREFIX = "file";
    public static final String DEF_FILE_TYPE = "file";

    private String mFileType;
    private TetroidRecord mRecord;

    public TetroidFile() {
        super(FoundType.TYPE_FILE, "");
    }

    public TetroidFile(boolean isCrypted, String id, String name, String fileType, TetroidRecord record) {
        super(FoundType.TYPE_FILE, isCrypted, id, name);
        this.mFileType = fileType;
        this.mRecord = record;
    }

    public String getFileType() {
        return mFileType;
    }

    public TetroidRecord getRecord() {
        return mRecord;
    }

    public String getIdName() {
        String fileDisplayName = getName();
        String ext = FileUtils.getExtensionWithComma(fileDisplayName);
        return id + ext;
    }

    public void setRecord(TetroidRecord record) {
        this.mRecord = record;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
