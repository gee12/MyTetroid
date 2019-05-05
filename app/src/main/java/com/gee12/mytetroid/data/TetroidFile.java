package com.gee12.mytetroid.data;

public class TetroidFile {
    private String id;
    private String name;
    private String type;

    public TetroidFile(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }
}
