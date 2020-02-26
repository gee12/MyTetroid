package com.gee12.mytetroid.model;

public class TetroidObject implements ITetroidObject {

    public static final String MYTETRA_PREFIX = "mytetra:";
    public static final String MYTETRA_RECORD_PREFIX = MYTETRA_PREFIX + "//note/";
    public static final String MYTETRA_NODE_PREFIX = MYTETRA_PREFIX + "//branch/";
    public static final String MYTETRA_TAG_PREFIX = MYTETRA_PREFIX + "//tag/";
    public static final String MYTETRA_FILE_PREFIX = MYTETRA_PREFIX + "//file/";

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
     *
     * @param url
     * @return
     */
    public static TetroidObject parseUrl(String url) {
        if (url.startsWith(MYTETRA_PREFIX)) {
            int type = FoundType.TYPE_NONE;
            String id = "";
            // ссылка на запись типа "mytetra://<type>/<id>"
            if (url.startsWith(MYTETRA_RECORD_PREFIX)) {
                id = parseId(url, MYTETRA_RECORD_PREFIX);
                type = FoundType.TYPE_RECORD;
            } else if (url.startsWith(MYTETRA_NODE_PREFIX)) {
                id = parseId(url, MYTETRA_NODE_PREFIX);
                type = FoundType.TYPE_NODE;
            } else if (url.startsWith(MYTETRA_TAG_PREFIX)) {
                id = parseId(url, MYTETRA_TAG_PREFIX);
                type = FoundType.TYPE_TAG;
            } else if (url.startsWith(MYTETRA_FILE_PREFIX)) {
                id = parseId(url, MYTETRA_FILE_PREFIX);
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
//        String id = url.substring(url.lastIndexOf('/')+1);
        String id = url.replace(prefix, "");

        // TODO: Обрезать лишние символы вконце ? (н-р, символы '/' и т.д.)

        return id;
    }

}
