package com.gee12.mytetroid.model;

import java.util.Locale;

/**
 * Объект хранилища.
 */
public class TetroidObject implements ITetroidObject {

    public static final String MYTETRA_LINK_PREFIX = "mytetra:";
//    public static final String MYTETRA_RECORD_PREFIX = MYTETRA_LINK_PREFIX + "//note/";
//    public static final String MYTETRA_NODE_PREFIX = MYTETRA_LINK_PREFIX + "//branch/";
//    public static final String MYTETRA_TAG_PREFIX = MYTETRA_LINK_PREFIX + "//tag/";
//    public static final String MYTETRA_FILE_PREFIX = MYTETRA_LINK_PREFIX + "//file/";

    protected int type = FoundType.TYPE_NONE;
    protected String id;
    protected String name;
    protected boolean isCrypted;
    protected boolean isDecrypted;


    public TetroidObject(int type, String id) {
        this.type = type;
        this.id = id;
    }

    public TetroidObject(int type, boolean isCrypted, String id, String name) {
        this.type = type;
        this.isCrypted = isCrypted;
        this.id = id;
        this.name = name;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isCrypted() {
        return isCrypted;
    }

    public boolean isDecrypted() {
        return isDecrypted;
    }

    public String getPrefix() {
        return "";
    }

    /**
     * Получение признака, что запись не зашифрована.
     * @return True, если не зашифровано (crypt=0), или уже расшифровано.
     */
    public boolean isNonCryptedOrDecrypted() {
        return (!isCrypted || isDecrypted);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDecrypted(boolean decrypted) {
        isDecrypted = decrypted;
    }

    /**
     * Формирование ссылки на объект хранилища.
     * @return
     */
    public String createUrl() {
        return createUrl(id);
    }

    public String createUrl(String uniqueField) {
        return String.format(Locale.getDefault(), "%s//%s/%s", MYTETRA_LINK_PREFIX, getPrefix(), uniqueField);
    }

    /**
     * Получение объекта хранилища по ссылке.
     * @param url
     * @return
     */
    public static TetroidObject parseUrl(String url) {
        if (url == null)
            return null;
        if (url.startsWith(MYTETRA_LINK_PREFIX)) {
            int type = FoundType.TYPE_NONE;
            String id = "";
            // ссылка на запись типа "mytetra://<type>/<id>"
            if ((id = parseId(url, TetroidRecord.PREFIX)) != null) {
                type = FoundType.TYPE_RECORD;
            } else if ((id = parseId(url, TetroidNode.PREFIX)) != null) {
                type = FoundType.TYPE_NODE;
            } else if ((id = parseId(url, TetroidTag.PREFIX)) != null) {
                type = FoundType.TYPE_TAG;
            } else if ((id = parseId(url, TetroidFile.PREFIX)) != null) {
                type = FoundType.TYPE_FILE;
            }
            return new TetroidObject(type, id);
        }
        return null;
    }

    /**
     * Получение id объекта хранилища из url, зная его префикс (тип объекта)
     * @param url
     * @param prefix
     * @return
     */
    public static String parseId(String url, String prefix) {
        String start = String.format(Locale.getDefault(), "%s//%s/", MYTETRA_LINK_PREFIX, prefix);
        if (url.startsWith(start)) {
//        String id = url.substring(url.lastIndexOf('/')+1);

            // TODO: Обрезать лишние символы вконце ? (н-р, символы '/' и т.д.)

            return url.replace(start, "");
        }
        return null;
    }

}
