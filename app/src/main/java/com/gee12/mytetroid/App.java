package com.gee12.mytetroid;

public class App {

    /**
     * Проверка - полная ли версия
     * @return
     */
    public static boolean isFullVersion() {
        return (BuildConfig.FLAVOR.equals("pro"));
    }
}
