package com.gee12.mytetroid.model;

public class TetroidFile extends TetroidObject {

    public static final String PREFIX = "file";
    public static final String DEF_FILE_TYPE = "file";

    private String fileType;
    private TetroidRecord record;

    public TetroidFile(boolean isCrypted, String id, String name, String fileType, TetroidRecord record) {
        super(FoundType.TYPE_FILE, isCrypted, id, name);
        this.fileType = fileType;
        this.record = record;
    }

    public String getFileType() {
        return fileType;
    }

    public TetroidRecord getRecord() {
        return record;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
