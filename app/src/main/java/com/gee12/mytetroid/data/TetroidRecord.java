package com.gee12.mytetroid.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TetroidRecord implements ITetroidObject {

    public static final String TAG_LINKS_PREF = "tag:";

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
    List<TetroidFile> files;
    List<TetroidTag> tags;

    private boolean isDecrypted;

    public TetroidRecord(boolean crypt, String id, String name, String tagsString, String author, String url,
                         Date created, String dirName, String fileName, TetroidNode node) {
        this.isCrypted = crypt;
        this.id = id;
        this.name = name;
        this.tagsString = tagsString;
        this.author = author;
        this.url = url;
        this.created = created;
        this.dirName = dirName;
        this.fileName = fileName;
        this.files = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.node = node;
    }

    @Override
    public String getId() {
        return id;
    }

    public TetroidNode getNode() {
        return node;
    }

    @Override
    public int getType() {
        return FoundType.TYPE_RECORD;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getTagsString() {
        return tagsString;
    }

    /**
     * Формирование списка меток в виде html-кода.
     * @return
     */
    public static String createTagsLinksString(TetroidRecord record) {
        StringBuilder sb = new StringBuilder();
        int size = record.getTags().size();
        if (size == 0) {
            return null;
        }
        // #a4a4e4 - это colorLightLabelText
        sb.append("<body style=\"font-family:'DejaVu Sans';color:#a4a4e4;\">");
        for (TetroidTag tag : record.getTags()) {
            // #303F9F - это colorBlueDark
            sb.append("<a href=\"").append(TAG_LINKS_PREF).append(tag.getName()).append("\" style=\"color:#303F9F;\">").
                    append(tag.getName()).append("</a>");
            if (--size > 0)
                sb.append(", ");
        }
        sb.append("</body>");
        return sb.toString();
    }

    public String getAuthor() {
        return author;
    }

    public String getUrl() {
        return url;
    }

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

    public List<TetroidTag> getTags() {
        return tags;
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

    public void setFiles(List<TetroidFile> files) {
        this.files = files;
    }
}
