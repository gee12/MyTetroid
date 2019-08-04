package com.gee12.mytetroid.data;

public class TetroidFile implements ITetroidObject {
    private String id;
    private String name;
    private String fileType;

    public TetroidFile(String id, String name, String fileType) {
        this.id = id;
        this.name = name;
        this.fileType = fileType;
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
}
