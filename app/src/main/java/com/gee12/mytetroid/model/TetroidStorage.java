package com.gee12.mytetroid.model;

import java.util.Date;

public class TetroidStorage {

    private String mName;
    private String mPath;
    private Date mCreated;
    private Date mEdited;

    public TetroidStorage(String path) {
        Date curDate = new Date();
        init(path, getFolderNameFromPath(path), curDate, curDate);
    }

    public TetroidStorage(String name, String path, Date created, Date edited) {
        init(name, path, created, edited);
    }

    private void init(String name, String path, Date created, Date edited) {
        this.mName = name;
        this.mPath = path;
        this.mCreated = created;
        this.mEdited = edited;
    }

    public static String getFolderNameFromPath(String path) {
        return "";
    }
}
