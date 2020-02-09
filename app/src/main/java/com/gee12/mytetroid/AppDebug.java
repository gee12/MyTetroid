package com.gee12.mytetroid;

public class AppDebug {

    public static final int DEBUG_RECORDS_NUM = 1000;
    public static final boolean DEBUG_IS_LOAD_CRYPTED_RECORDS = false;

    public static boolean isRecordsLoadedEnough(int recordsNum) {
        return (BuildConfig.DEBUG && recordsNum >= DEBUG_RECORDS_NUM);
    }

    public static boolean isLoadCryptedRecords() {
        return (BuildConfig.DEBUG && DEBUG_IS_LOAD_CRYPTED_RECORDS);
    }

}
