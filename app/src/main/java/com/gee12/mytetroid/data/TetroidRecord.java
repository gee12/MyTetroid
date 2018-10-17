package com.gee12.mytetroid.data;

import android.text.Spanned;

import com.gee12.mytetroid.Utils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TetroidRecord {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private String id;
    private TetroidNode node;
    private String name;
    private String author;
    private String url;
//    private String htmlContent;
//    private Spanned content;    // потом не хранить и читать из файла
    private Date created;
    private boolean isCrypted;

    private String dirName;
    private String fileName;
    List<TetroidFile> files = new ArrayList<>();

    boolean isDecrypted;

    public TetroidRecord(String id, String name) {
        this.id = id;
        this.name = name;
//        this.htmlContent = htmlContent;
//        this.content = Utils.fromHtml(name);
        this.created = Calendar.getInstance().getTime();
    }

    public TetroidRecord(String id,String name, String htmlContent) {
        this.id = id;
        this.name = name;
//        this.htmlContent = htmlContent;
//        this.content = Utils.fromHtml(htmlContent);
        this.created = Calendar.getInstance().getTime();
    }

    public TetroidRecord(boolean crypt, String id, String name, String author, String url,
                         Date created, String dirName, String fileName, List<TetroidFile> files) {
        this.isCrypted = crypt;
        this.id = id;
        this.name = name;
        this.author = author;
        this.url = url;
        this.created = created;
        this.dirName = dirName;
        this.fileName = fileName;
        this.files = files;
    }

    public String getId() {
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

    public String getRecordTextUrl(String storagePath, String tempPath) {
        String path = (isCrypted && isDecrypted)
                ? tempPath+dirName+"/"+fileName
                : storagePath+"/base/"+dirName+"/"+fileName;
//        File file = new File(storagePath+"/base/"+dirName+"/"+fileName);
//        return "file:///" + file.getAbsolutePath();
        return "file:///" + path;
    }

    public Date getCreated() {
        return created;
    }

    public String getCreatedString() {
//        return SimpleDateFormat.getDateTimeInstance().format(created);
        return simpleDateFormat.format(created);
    }

    public String getDirName() {
        return dirName;
    }

    public String getFileName() {
        return fileName;
    }

    public List<TetroidFile> getAttachedFiles() {
        return files;
    }

    public int getAttachedFilesCount() {
        return files.size();
    }

    public boolean isCrypted() {
        return isCrypted;
    }

    public boolean isDecrypted() {
        return (!isCrypted || isDecrypted);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setDirName(String dirName) {
        this.dirName = dirName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setDecrypted(boolean decrypted) {
        isDecrypted = decrypted;
    }

    public void setNode(TetroidNode node) {
        this.node = node;
    }

}
