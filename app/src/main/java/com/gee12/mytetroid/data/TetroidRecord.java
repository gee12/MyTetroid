package com.gee12.mytetroid.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TetroidRecord extends FoundObject {

    private String id;
    private TetroidNode node;
    private String name;
    private String tagsString;
    private String author;
    private String url;
    private Date created;
    private boolean isCrypted;

    private String dirName;
    private String fileName;
    List<TetroidFile> files = new ArrayList<>();
    List<TetroidTag> tags = new ArrayList<>();

    private boolean isDecrypted;

    public TetroidRecord(String id, String name) {
        this.id = id;
        this.name = name;
//        this.htmlContent = htmlContent;
//        this.content = Utils.fromHtml(name);
        this.created = Calendar.getInstance().getTime();
        this.tags = new ArrayList<>();
    }

    public TetroidRecord(String id,String name, String htmlContent) {
        this.id = id;
        this.name = name;
//        this.htmlContent = htmlContent;
//        this.content = Utils.fromHtml(htmlContent);
        this.created = Calendar.getInstance().getTime();
        this.tags = new ArrayList<>();
    }

    public TetroidRecord(boolean crypt, String id, String name, String tagsString, String author, String url,
                         Date created, String dirName, String fileName, List<TetroidFile> files) {
        this.isCrypted = crypt;
        this.id = id;
        this.name = name;
        this.tagsString = tagsString;
        this.author = author;
        this.url = url;
        this.created = created;
        this.dirName = dirName;
        this.fileName = fileName;
        this.files = files;
        this.tags = new ArrayList<>();
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

    public String getTagsString() {
        return tagsString;
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

    /**
     * Получение пути к файлу с содержимым записи.
     * Если расшифрован, то в tempPath. Если не был зашифрован, то в storagePath.
     * @param storagePath
     * @param tempPath
     * @return
     */
    /*public String getRecordTextUri(String storagePath, String tempPath) {
        String path = (isCrypted && isNonCryptedOrDecrypted)    // логическая ошибка в условии
                ? tempPath+dirName+"/"+fileName
                : storagePath+"/base/"+dirName+"/"+fileName;
//        File file = new File(storagePath+"/base/"+dirName+"/"+fileName);
//        return "file:///" + file.getAbsolutePath();
        return "file:///" + path;
    }*/

    public Date getCreated() {
        return created;
    }

    public String getCreatedString(String format) {
        return (created != null) ? new SimpleDateFormat(format).format(created) : "";
//        return dateFormat.format(created);
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

    /**
     * Получение признака, что запись не зашифрована.
     * @return True, если не зашифровано, или уже расшифровано.
     */
    public boolean isNonCryptedOrDecrypted() {
        return (!isCrypted || isDecrypted);
    }

    public boolean isDecrypted() {
        return isDecrypted;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTagsString(String tagsString) {
        this.tagsString = tagsString;
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

    public void addTag(TetroidTag tag) {
        tags.add(tag);
    }

    public List<TetroidTag> getTags() {
        return tags;
    }

    @Override
    public String getFoundName() {
        return name;
    }

//    public void setDateFormat(String format) {
//        this.dateFormat = new SimpleDateFormat(format);
//    }
}
