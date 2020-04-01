package com.gee12.mytetroid.model;

public class TetroidFile extends TetroidObject {

    public static final String PREFIX = "file";
    public static final String DEF_FILE_TYPE = "file";

//    private String id;
//    private String name;
    private String fileType;
    private TetroidRecord record;

    public TetroidFile(boolean isCrypted, String id, String name, String fileType, TetroidRecord record) {
        super(FoundType.TYPE_FILE, isCrypted, id, name);
//        this.id = id;
//        this.name = name;
        this.fileType = fileType;
        this.record = record;
    }

//    @Override
//    public String getId() {
//        return id;
//    }
//
//    @Override
//    public int getType() {
//        return FoundType.TYPE_FILE;
//    }
//
//    @Override
//    public String getName() {
//        return name;
//    }

    public String getFileType() {
        return fileType;
    }

//    public void setName(String name) {
//        this.name = name;
//    }

    public TetroidRecord getRecord() {
        return record;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
