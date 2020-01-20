package com.gee12.mytetroid.model;

import com.gee12.mytetroid.data.ITetroidObject;

public class TetroidFile implements ITetroidObject {
    private String id;
    private String name;
    private String fileType;
    private TetroidRecord record;

    public TetroidFile(String id, String name, String fileType, TetroidRecord record) {
        this.id = id;
        this.name = name;
        this.fileType = fileType;
        this.record = record;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getType() {
        return FoundType.TYPE_FILE;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getFileType() {
        return fileType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TetroidRecord getRecord() {
        return record;
    }
}
