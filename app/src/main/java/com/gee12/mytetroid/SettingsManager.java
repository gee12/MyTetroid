package com.gee12.mytetroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingsManager {
    public static final String APP_PREFERENCES = "MyTetroidSettings";

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
        String def = null;
        if(settings.contains(context.getString(R.string.pref_key_storage_path))) {
            return settings.getString(context.getString(R.string.pref_key_storage_path), def);
        }
        return def;
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
        boolean def = true;
        if(settings.contains(context.getString(R.string.pref_key_is_load_last_storage_path))) {
            return settings.getBoolean(context.getString(R.string.pref_key_is_load_last_storage_path), def);
        }
        return def;
    }

//    public static void setIsLoadLastStoragePath(boolean value) {
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putBoolean(context.getString(R.string.pref_key_is_load_last_storage_path), value);
//        editor.apply();
//    }

    /**
     * Сохранять хэш пароля локально?
     * По-умолчанию - да
     * @return
     */
    public static boolean isSavePasswordHashLocal() {
        boolean def = true;
        if(settings.contains(context.getString(R.string.pref_key_is_save_pass_hash_local))) {
            return settings.getBoolean(context.getString(R.string.pref_key_is_save_pass_hash_local), def);
        }
        return def;
    }

    /**
     * Хэш пароля
     * @return
     */
    public static String getPassHash() {
        String def = null;
        if(settings.contains(context.getString(R.string.pref_key_pass_hash))) {
            return settings.getString(context.getString(R.string.pref_key_pass_hash), def);
        }
        return def;
    }

    public static void setPassHash(String pass) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(context.getString(R.string.pref_key_pass_hash), pass);
        editor.apply();
    }

    /**
     * Когда спрашивать пароль?
     * По-умолчанию - при выборе зашифрованной ветки
     * @return
     */
    public static String getWhenAskPass() {
        String def = context.getString(R.string.pref_when_ask_password_on_select);
        if(settings.contains(context.getString(R.string.pref_key_when_ask_password))) {
            return settings.getString(context.getString(R.string.pref_key_when_ask_password), def);
        }
        return def;
    }

    /**
     * Выделять записи в списке, у которых есть прикрепленные файлы?
     * По-умолчанию - да
     * @return
     */
    public static boolean isHighlightRecordWithAttach() {
        boolean def = true;
        if(settings.contains(context.getString(R.string.pref_key_is_highlight_attach))) {
            return settings.getBoolean(context.getString(R.string.pref_key_is_highlight_attach), def);
        }
        return def;
    }

    /**
     * Выделять записи в списке, у которых есть прикрепленные файлы?
     * По-умолчанию - да
     * @return
     */
    public static int highlightAttachColor() {
        int def = R.color.colorHighlight;
        if(settings.contains(context.getString(R.string.pref_key_highlight_attach_color))) {
            return settings.getInt(context.getString(R.string.pref_key_highlight_attach_color), def);
        }
        return def;
    }

    /**
     * Устанавливать текущей выбранную при предыдущем запуске ветку
     * По-умолчанию - да
     * @return
     */
    public static boolean isKeepSelectedNode() {
        boolean def = true;
        if(settings.contains(context.getString(R.string.pref_key_is_keep_selected_node))) {
            return settings.getBoolean(context.getString(R.string.pref_key_is_keep_selected_node), def);
        }
        return def;
    }

    /**
     * Id выбранной при предыдущем запуске ветки
     * @return
     */
    public static String getSelectedNodeId() {
        String def = null;
        if(settings.contains(context.getString(R.string.pref_key_selected_node_id))) {
            return settings.getString(context.getString(R.string.pref_key_selected_node_id), def);
        }
        return def;
    }

    public static SharedPreferences getSettings() {
        return settings;
    }
}
