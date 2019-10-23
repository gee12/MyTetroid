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
        final String def = null;
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
        final boolean def = true;
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
        final boolean def = true;
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
        final String def = null;
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
        final String def = context.getString(R.string.pref_when_ask_password_on_select);
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
        final boolean def = false;
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
        final String def = null;
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
        final boolean def = true;
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
        final int def = R.color.colorHighlight;
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
        final boolean def = true;
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
        final String def = null;
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
        final String def = context.getString(R.string.def_date_format_string);
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
        final boolean def = false;
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
        final String def = null;
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
        final boolean def = true;
        if(settings.contains(context.getString(R.string.pref_key_is_confirm_app_exit))) {
            return settings.getBoolean(context.getString(R.string.pref_key_is_confirm_app_exit), def);
        }
        return def;
    }

    /**
     * Поисковой запрос
     * @return
     */
    public static String getSearchQuery() {
        final String def = null;
        if(settings.contains(context.getString(R.string.pref_key_search_query))) {
            return settings.getString(context.getString(R.string.pref_key_search_query), def);
        }
        return def;
    }

    public static void setSearchQuery(String value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(context.getString(R.string.pref_key_search_query), value);
        editor.apply();
    }

    /**
     * Поиск по содержимому записей
     * @return
     */
    public static boolean getSearchInText() {
        final boolean def = true;
        if(settings.contains(context.getString(R.string.pref_key_search_text))) {
            return settings.getBoolean(context.getString(R.string.pref_key_search_text), def);
        }
        return def;
    }

    public static void setSearchInText(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(R.string.pref_key_search_text), value);
        editor.apply();
    }

    /**
     * Поиск по именам записей
     * @return
     */
    public static boolean getSearchInRecordsNames() {
        final boolean def = true;
        if(settings.contains(context.getString(R.string.pref_key_search_records_names))) {
            return settings.getBoolean(context.getString(R.string.pref_key_search_records_names), def);
        }
        return def;
    }

    public static void setSearchInRecordsNames(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(R.string.pref_key_search_records_names), value);
        editor.apply();
    }

    /**
     * Поиск по авторам записей
     * @return
     */
    public static boolean getSearchInAuthor() {
        final boolean def = true;
        if(settings.contains(context.getString(R.string.pref_key_search_author))) {
            return settings.getBoolean(context.getString(R.string.pref_key_search_author), def);
        }
        return def;
    }

    public static void setSearchInAuthor(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(R.string.pref_key_search_author), value);
        editor.apply();
    }

    /**
     * Поиск по url записей
     * @return
     */
    public static boolean getSearchInUrl() {
        final boolean def = true;
        if(settings.contains(context.getString(R.string.pref_key_search_url))) {
            return settings.getBoolean(context.getString(R.string.pref_key_search_url), def);
        }
        return def;
    }

    public static void setSearchInUrl(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(R.string.pref_key_search_url), value);
        editor.apply();
    }

    /**
     * Поиск по меткам
     * @return
     */
    public static boolean getSearchInTags() {
        final boolean def = true;
        if(settings.contains(context.getString(R.string.pref_key_search_tags))) {
            return settings.getBoolean(context.getString(R.string.pref_key_search_tags), def);
        }
        return def;
    }

    public static void setSearchInTags(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(R.string.pref_key_search_tags), value);
        editor.apply();
    }

    /**
     * Поиск по веткам
     * @return
     */
    public static boolean getSearchInNodes() {
        final boolean def = true;
        if(settings.contains(context.getString(R.string.pref_key_search_nodes))) {
            return settings.getBoolean(context.getString(R.string.pref_key_search_nodes), def);
        }
        return def;
    }

    public static void setSearchInNodes(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(R.string.pref_key_search_nodes), value);
        editor.apply();
    }

    /**
     * Поиск по прикрепленным файлам
     * @return
     */
    public static boolean getSearchInFiles() {
        final boolean def = true;
        if(settings.contains(context.getString(R.string.pref_key_search_files))) {
            return settings.getBoolean(context.getString(R.string.pref_key_search_files), def);
        }
        return def;
    }

    public static void setSearchInFiles(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(R.string.pref_key_search_files), value);
        editor.apply();
    }

    /**
     * Поиск по каждому из слов в запросе?
     * @return
     */
    public static boolean getSearchSplitToWords() {
        final boolean def = false;
        if(settings.contains(context.getString(R.string.pref_key_search_split_to_words))) {
            return settings.getBoolean(context.getString(R.string.pref_key_search_split_to_words), def);
        }
        return def;
    }

    public static void setSearchSplitToWords(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(R.string.pref_key_search_split_to_words), value);
        editor.apply();
    }

    /**
     * Поиск по совпадению только целых слов?
     * @return
     */
    public static boolean getSearchInWholeWords() {
        final boolean def = false;
        if(settings.contains(context.getString(R.string.pref_key_search_in_whole_words))) {
            return settings.getBoolean(context.getString(R.string.pref_key_search_in_whole_words), def);
        }
        return def;
    }

    public static void setSearchInWholeWords(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(R.string.pref_key_search_in_whole_words), value);
        editor.apply();
    }

    /**
     * Поиск только в текущей ветке?
     * @return
     */
    public static boolean getSearchInCurNode() {
        final boolean def = false;
        if(settings.contains(context.getString(R.string.pref_key_search_in_cur_node))) {
            return settings.getBoolean(context.getString(R.string.pref_key_search_in_cur_node), def);
        }
        return def;
    }

    public static void setSearchInCurNode(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(R.string.pref_key_search_in_cur_node), value);
        editor.apply();
    }

    public static SharedPreferences getSettings() {
        return settings;
    }
}
