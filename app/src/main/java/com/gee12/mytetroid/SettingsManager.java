package com.gee12.mytetroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingsManager {
    public static final String APP_PREFERENCES = "MyTetroidSettings";
//    public static final String APP_PREFERENCES_STORAGE_PATH = "StoragePath";
//    public static final String APP_PREFERENCES_EXIST_STORAGE_QUESTION = "ExistStorageQuestion";
//    public static final String APP_PREFERENCES_ASK_PASSWORD_ON_START = "AskPasswordOnStart";
//    public static final String APP_PREFERENCES_HIGHLIGHT_ATTACHED = "HighlightAttached";
//    public static final String APP_PREFERENCES_KEEP_SELECTED_NODE = "KeepSelectedNode";

    private static SharedPreferences settings;
    private static Context context;

    public static void init(Context ctx) {
        context = ctx;
        settings = ctx.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        PreferenceManager.setDefaultValues(ctx, R.xml.prefs, false);
    }

    /**
     * Путь к хранилищу
     * @return
     */
    public static String getStoragePath() {
        if(settings.contains(context.getString(R.string.pref_key_storage_path))) {
            return settings.getString(context.getString(R.string.pref_key_storage_path), null);
        }
        return null;
    }

    public static void setStoragePath(String value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(context.getString(R.string.pref_key_storage_path), value);
        editor.apply();
    }

    /**
     * Загружать хранилище, используемое при прошлом запуске
     * По-умолчанию - да
     * @return
     */
    public static boolean isLoadLastStoragePath() {
        if(settings.contains(context.getString(R.string.pref_key_is_load_last_storage_path))) {
            return settings.getBoolean(context.getString(R.string.pref_key_is_load_last_storage_path), true);
        }
        return true;
    }

    public static void setIsLoadLastStoragePath(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(R.string.pref_key_is_load_last_storage_path), value);
        editor.apply();
    }

    /**
     * Сохранять хэш пароля локально?
     * По-умолчанию - да
     * @return
     */
    public static boolean isSavePasswordHashLocal() {
        if(settings.contains(context.getString(R.string.pref_key_is_save_pass_hash_local))) {
            return settings.getBoolean(context.getString(R.string.pref_key_is_save_pass_hash_local), true);
        }
        return false;
    }

    public static void setIsSavePasswordHashLocal(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(R.string.pref_key_is_save_pass_hash_local), value);
        editor.apply();
    }

    /**
     * Выделять записи в списке, у которых есть прикрепленные файлы?
     * По-умолчанию - да
     * @return
     */
//    public static boolean isHighlightAttached() {
//        if(settings.contains(APP_PREFERENCES_HIGHLIGHT_ATTACHED)) {
//            return settings.getBoolean(APP_PREFERENCES_HIGHLIGHT_ATTACHED, true);
//        }
//        return true;
//    }
//
//    public static void setIsHighlightAttached(boolean value) {
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putBoolean(APP_PREFERENCES_HIGHLIGHT_ATTACHED, value);
//        editor.apply();
//    }

    /**
     * Устанавливать текущей выбранную при предыдущем запуске ветку
     * По-умолчанию - да
     * @return
     */
    public static boolean isKeepSelectedNode() {
        if(settings.contains(context.getString(R.string.pref_key_is_select_last_node))) {
            return settings.getBoolean(context.getString(R.string.pref_key_is_select_last_node), true);
        }
        return true;
    }

    public static void setIsKeepSelectedNode(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(R.string.pref_key_is_select_last_node), value);
        editor.apply();
    }

}
