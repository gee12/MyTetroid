package com.gee12.mytetroid.data;

public class DatabaseConfig {
    private String cryptCheckHash;
    private String cryptCheckSalt;
    private String middleHashCheckData;
    private boolean cryptMode;

    public String getCryptCheckHash() {
        return cryptCheckHash;
    }

    public String getCryptCheckSalt() {
        return cryptCheckSalt;
    }

    public String getMiddleHashCheckData() {
        return middleHashCheckData;
    }

    public boolean isCryptMode() {
        return cryptMode;
    }
}
