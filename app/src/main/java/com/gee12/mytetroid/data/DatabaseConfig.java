package com.gee12.mytetroid.data;

public class DatabaseConfig extends INIProperties {

    public class EmptyFieldException extends Exception {

        private String fieldName;

        public EmptyFieldException(String fieldName) {
            super();
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    public static final String INI_CRYPT_CHECK_SALT = "crypt_check_salt";
    public static final String INI_CRYPT_CHECK_HASH = "crypt_check_hash";
    public static final String INI_MIDDLE_HASH_CHECK_DATA = "middle_hash_check_data";
    public static final String INI_CRYPT_MODE = "crypt_mode";

    /*private String cryptCheckHash;
    private String cryptCheckSalt;
    private String middleHashCheckData;
    private boolean cryptMode;*/

    public DatabaseConfig(String fileName) {
        super(fileName);
    }

    public String getCryptCheckHash() throws EmptyFieldException {
        return getWithoutQuotes(INI_CRYPT_CHECK_HASH);
    }

    public String getCryptCheckSalt() throws EmptyFieldException {
        return getWithoutQuotes(INI_CRYPT_CHECK_SALT);
    }

    public String getMiddleHashCheckData() throws EmptyFieldException {
        return getWithoutQuotes(INI_MIDDLE_HASH_CHECK_DATA);
    }

    public boolean isCryptMode() throws EmptyFieldException {
        return getValue(INI_CRYPT_MODE).equals("1");
    }

    /**
     *
     * @param passHash
     * @param salt
     * @return
     */
    public boolean savePass(String passHash, String salt, boolean cryptMode) {
        set(INI_CRYPT_CHECK_HASH, (passHash != null) ? passHash : "");
        set(INI_CRYPT_CHECK_SALT, (salt != null) ? salt : "");
        set(INI_CRYPT_MODE, (cryptMode) ? "1" : "");
        return save();
    }

    /**
     * Получение значения по ключу.
     * @param key
     * @return
     */
    public String getValue(String key) throws EmptyFieldException {
        String res = get(key);
        if (res == null || res.isEmpty()) {
            throw new EmptyFieldException(key);
        }
        return res;
    }
    /**
     * Получение значения по ключу без двойных кавычек вначале и вконце.
     * @param key
     * @return
     */
    public String getWithoutQuotes(String key) throws EmptyFieldException {
        return getValue(key).replaceAll("^\"|\"$", "");
    }
}
