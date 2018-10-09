package com.gee12.mytetroid.data;

public class TetroidFile {
    private String id;
    private String fileName;
    private String type;

    public TetroidFile(String id, String fileName, String type) {
        this.id = id;
        this.fileName = fileName;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getType() {
        return type;
    }

    public String getFileSize() {
        return "100Кб";
    }
}
