package com.gee12.mytetroid;

public class AppDebug {

    public static final int RECORDS_MAX_NUM = 0;
    public static final boolean IS_LOAD_CRYPTED_RECORDS = true;

    public static boolean isRecordsLoadedEnough(int recordsNum) {
        return (BuildConfig.DEBUG && RECORDS_MAX_NUM > 0 && recordsNum >= RECORDS_MAX_NUM);
    }

    public static boolean isLoadCryptedRecords() {
        return (!BuildConfig.DEBUG || IS_LOAD_CRYPTED_RECORDS);
    }

}
