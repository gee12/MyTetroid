package com.gee12.mytetroid;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    public static final String APP_PREFERENCES = "MyTetroidSettings";
    public static final String APP_PREFERENCES_STORAGE_PATH = "StoragePath";

    private static SharedPreferences settings;

    public static void init(Context context) {
        settings = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    public static String getStoragePath() {
        if(settings.contains(APP_PREFERENCES_STORAGE_PATH)) {
            return settings.getString(APP_PREFERENCES_STORAGE_PATH, null);
        }
        return null;
    }

    public static void setStoragePath(String path) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(APP_PREFERENCES_STORAGE_PATH, path);
        editor.apply();
    }
}
