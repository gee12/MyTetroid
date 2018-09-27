package com.gee12.mytetroid.data;

import android.text.Spanned;

import com.gee12.mytetroid.Utils;

import java.util.Calendar;
import java.util.Date;

public class TetroidRecord {
    private int id;
    private TetroidNode node;
    private String name;
    private String author;
    private String url;
//    private String htmlContent;
    private Spanned content;    // потом не хранить и читать из файла
    private Date created;

    private String dirName;
    private String fileName;

    public TetroidRecord(int id,String name) {
        this.id = id;
        this.name = name;
//        this.htmlContent = htmlContent;
        this.content = Utils.fromHtml(name);
        this.created = Calendar.getInstance().getTime();
    }

    public TetroidRecord(int id,String name, String htmlContent) {
        this.id = id;
        this.name = name;
//        this.htmlContent = htmlContent;
        this.content = Utils.fromHtml(htmlContent);
        this.created = Calendar.getInstance().getTime();
    }

    public TetroidRecord(int id, TetroidNode node, String name, String author, String url, String htmlContent, Date created, String dirName, String fileName) {
        this.id = id;
        this.node = node;
        this.name = name;
        this.author = author;
        this.url = url;
//        this.htmlContent = htmlContent;
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

//    public String getHtmlContent() {
//        return htmlContent;
//    }

    public Spanned getContent() {
        return content;
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
