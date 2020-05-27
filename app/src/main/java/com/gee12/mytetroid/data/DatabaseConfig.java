package com.gee12.mytetroid.data;

public class DatabaseConfig extends INIConfig {

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

    public static final String INI_SECTION_GENERAL = "General";
    public static final String INI_CRYPT_CHECK_SALT = "crypt_check_salt";
    public static final String INI_CRYPT_CHECK_HASH = "crypt_check_hash";
    public static final String INI_MIDDLE_HASH_CHECK_DATA = "middle_hash_check_data";
    public static final String INI_CRYPT_MODE = "crypt_mode";

    public DatabaseConfig(String fileName) {
        super(fileName);
        // отключаем экранирование символов, т.к. сохраняем значения в кавычках
        config.getConfig().setEscape(false);
        // убираем пробелы между ключем и значением
        config.getConfig().setStrictOperator(true);
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
        return getValueFromGeneral(INI_CRYPT_MODE).equals("1");
    }

    /**
     *
     * @param passHash
     * @param salt
     * @return
     */
    public boolean savePass(String passHash, String salt, boolean cryptMode) {
        setValueToGeneralWithQuotes(INI_CRYPT_CHECK_HASH, passHash);
        setValueToGeneralWithQuotes(INI_CRYPT_CHECK_SALT, salt);
        setValueToGeneral(INI_CRYPT_MODE, (cryptMode) ? "1" : "");
        return save();
    }

    public boolean saveCheckData(String checkData) {
        setValueToGeneralWithQuotes(INI_MIDDLE_HASH_CHECK_DATA, checkData);
        return save();
    }

    /**
     * Получение значения по ключу.
     * @param key
     * @return
     */
    public String getValueFromGeneral(String key) throws EmptyFieldException {
        String res = get(INI_SECTION_GENERAL, key);
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
        return getValueFromGeneral(key).replaceAll("^\"|\"$", "");
    }

    public void setValueToGeneral(String key, String value) {
        set(INI_SECTION_GENERAL, key, (value != null) ? value : "");
    }

    public void setValueToGeneralWithQuotes(String key, String value) {
        setValueToGeneral(key, (value != null) ? "\"" + value + "\"" : "");
    }
}
