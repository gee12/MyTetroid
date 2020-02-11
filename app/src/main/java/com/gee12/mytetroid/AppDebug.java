package com.gee12.mytetroid;

public class AppDebug {

    public static final int DEBUG_RECORDS_NUM = 0;
    public static final boolean DEBUG_IS_LOAD_CRYPTED_RECORDS = true;

    public static boolean isRecordsLoadedEnough(int recordsNum) {
        return (BuildConfig.DEBUG && DEBUG_RECORDS_NUM > 0 && recordsNum >= DEBUG_RECORDS_NUM);
    }

    public static boolean isLoadCryptedRecords() {
        return (BuildConfig.DEBUG && DEBUG_IS_LOAD_CRYPTED_RECORDS);
    }

}
