package com.gee12.mytetroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.ColorInt;

public class SettingsManager {

    private static SharedPreferences settings;
    private static Context context;

    public static boolean isAskReloadStorage;
    public static boolean IsHighlightAttachCache;
    @ColorInt
    public static int HighlightAttachColorCache;
    public static String DateFormatStringCache;

    public static void init(Context ctx) {
        SettingsManager.context = ctx;
        SettingsManager.settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        PreferenceManager.setDefaultValues(ctx, R.xml.prefs, false);
        // стартовые значения, которые нельзя установить в xml
//        if (getStoragePath() == null) {
//            setStoragePath(Utils.getExtPublicDocumentsDir());
//        }
        if (getTempPath() == null) {
            setTempPath(Utils.getAppExtFilesDir(context));
        }
        if (getLogPath() == null) {
            setLogPath(Utils.getAppExtFilesDir(context));
        }
//        LastStoragePath = getStoragePath();
        SettingsManager.HighlightAttachColorCache = getHighlightAttachColor();
        SettingsManager.IsHighlightAttachCache = isHighlightRecordWithAttach();
        SettingsManager.DateFormatStringCache = getDateFormatString();
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
        String oldPath = getStoragePath();
        if (Utils.isNullOrEmpty(oldPath) || !value.equals(oldPath)) {
//            isAskReloadStorage = true;
            SettingsManager.setMiddlePassHash(null);
        }
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
    public static boolean isSaveMiddlePassHashLocal() {
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
    public static String getMiddlePassHash() {
        String def = null;
        if(settings.contains(context.getString(R.string.pref_key_pass_hash))) {
            return settings.getString(context.getString(R.string.pref_key_pass_hash), def);
        }
        return def;
    }

    public static void setMiddlePassHash(String pass) {
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
     * Расшифровывать прикрепленные файлы во временный каталог при предпросмотре?
     * По-умолчанию - нет
     * @return
     */
    public static boolean isDecryptFilesInTemp() {
        boolean def = false;
        if(settings.contains(context.getString(R.string.pref_key_is_decrypt_in_temp))) {
            return settings.getBoolean(context.getString(R.string.pref_key_is_decrypt_in_temp), def);
        }
        return def;
    }

    /**
     * Путь к временному каталогу
     * @return
     */
    public static String getTempPath() {
        String def = null;
        if(settings.contains(context.getString(R.string.pref_key_temp_path))) {
            return settings.getString(context.getString(R.string.pref_key_temp_path), def);
        }
        return def;
    }

    public static void setTempPath(String value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(context.getString(R.string.pref_key_temp_path), value);
        editor.apply();
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
     * Цвет записей в списке с прикрепленными файлами
     * По-умолчанию - светло зеленый
     * @return
     */
    public static int getHighlightAttachColor() {
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

    /**
     * Формат даты создания записи
     * @return
     */
    public static String getDateFormatString() {
        String def = context.getString(R.string.def_date_format_string);
        if(settings.contains(context.getString(R.string.pref_key_date_format_string))) {
            return settings.getString(context.getString(R.string.pref_key_date_format_string), def);
        }
        return def;
    }

    /**
     * Писать логи в файл
     * По-умолчанию - нет
     * @return
     */
    public static boolean isWriteLog() {
        boolean def = false;
        if(settings.contains(context.getString(R.string.pref_key_is_write_log))) {
            return settings.getBoolean(context.getString(R.string.pref_key_is_write_log), def);
        }
        return def;
    }

    /**
     * Путь к каталогу с лог-файлом
     * @return
     */
    public static String getLogPath() {
        String def = null;
        if(settings.contains(context.getString(R.string.pref_key_log_path))) {
            return settings.getString(context.getString(R.string.pref_key_log_path), def);
        }
        return def;
    }

    public static void setLogPath(String value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(context.getString(R.string.pref_key_log_path), value);
        editor.apply();
    }

    /**
     * Писать логи в файл
     * По-умолчанию - нет
     * @return
     */
    public static boolean isConfirmAppExit() {
        boolean def = true;
        if(settings.contains(context.getString(R.string.pref_key_is_confirm_app_exit))) {
            return settings.getBoolean(context.getString(R.string.pref_key_is_confirm_app_exit), def);
        }
        return def;
    }

    public static SharedPreferences getSettings() {
        return settings;
    }
}