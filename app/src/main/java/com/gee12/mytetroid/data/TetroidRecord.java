package com.gee12.mytetroid.data;

import java.util.Calendar;
import java.util.Date;

public class TetroidRecord {
    private int id;
    private TetroidNode node;
    private String name;
    private String author;
    private String url;
    private String text;
    private Date created;

    private String dirName;
    private String fileName;

    public TetroidRecord(int id,String name) {
        this.id = id;
        this.name = name;
        this.text = name;
        this.created = Calendar.getInstance().getTime();
    }

    public TetroidRecord(int id, TetroidNode node, String name, String author, String url, String text, Date created, String dirName, String fileName) {
        this.id = id;
        this.node = node;
        this.name = name;
        this.author = author;
        this.url = url;
        this.text = text;
        this.created = created;
        this.dirName = dirName;
        this.fileName = fileName;
    }

    public int getId() {
        return id;
    }

    public TetroidNode getNode() {
        return node;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getUrl() {
        return url;
    }

    public String getText() {
        return text;
    }

    public Date getCreated() {
        return created;
    }

    public String getDirName() {
        return dirName;
    }

    public String getFileName() {
        return fileName;
    }

    public int getAttachedFilesCount() {
        return 0;
    }

    public void setNode(TetroidNode node) {
        this.node = node;
    }
}
