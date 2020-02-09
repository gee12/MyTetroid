package com.gee12.mytetroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import androidx.annotation.ColorInt;

import com.gee12.mytetroid.utils.FileUtils;

public class SettingsManager {

    private static SharedPreferences settings;
    private static Context context;

    public static boolean isAskReloadStorage;
    public static boolean IsHighlightAttachCache;
    @ColorInt
    public static int HighlightAttachColorCache;
    public static String DateFormatStringCache;
    public static boolean IsFullScreen;

    public static void init(Context ctx) {
        SettingsManager.context = ctx;
        SettingsManager.settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        PreferenceManager.setDefaultValues(ctx, R.xml.prefs, false);
        // стартовые значения, которые нельзя установить в xml
//        if (getStoragePath() == null) {
//            setStoragePath(Utils.getExtPublicDocumentsDir());
//        }
        if (getTempPath() == null) {
            setTempPath(FileUtils.getAppExtFilesDir(context));
        }
        if (getLogPath() == null) {
            setLogPath(FileUtils.getAppExtFilesDir(context));
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
        return getString(R.string.pref_key_storage_path, null);
    }

    public static void setStoragePath(String value) {
        String oldPath = getStoragePath();
        if (TextUtils.isEmpty(oldPath) || !value.equals(oldPath)) {
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
        return getBoolean(R.string.pref_key_is_load_last_storage_path, true);
    }

//    public static void setIsLoadLastStoragePath(boolean value) {
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putBoolean(context.getString(R.string.pref_key_is_load_last_storage_path), value);
//        editor.apply();
//    }

    /**
     * Использовать синхронизацию хранилища.
     * По-умолчанию - нет
     * @return
     */
    public static boolean isSyncStorage() {
        return getBoolean(R.string.pref_key_is_sync_storage, false);
    }

    /**
     * Команда синхронизации для стороннего приложения.
     * Например: "git pull".
     * @return
     */
    public static String getSyncCommand() {
        return getString(R.string.pref_key_sync_command, null);
    }

    /**
     * Запускать синхронизацию хранилища перед его загрузкой.
     * По-умолчанию - да
     * @return
     */
    public static boolean isSyncBeforeInit() {
        return getBoolean(R.string.pref_key_is_sync_before_init, true);
    }

    /**
     * Выводить подтверждение запуска синхронизации.
     * По-умолчанию - да
     * @return
     */
    public static boolean isAskBeforeSync() {
        return getBoolean(R.string.pref_key_is_ask_before_sync, true);
    }

    /**
     * Не запоминать используемое приложения для синхронизации в последний раз.
     * По-умолчанию - нет
     * @return
     */
    public static boolean isNotRememberSyncApp() {
        return getBoolean(R.string.pref_key_is_not_remember_sync_app, false);
    }

    /**
     * Сохранять хэш пароля локально?
     * По-умолчанию - да
     * @return
     */
    public static boolean isSaveMiddlePassHashLocal() {
        return getBoolean(R.string.pref_key_is_save_pass_hash_local, true);
    }

    /**
     * Хэш пароля
     * @return
     */
    public static String getMiddlePassHash() {
        return getString(R.string.pref_key_pass_hash, null);
    }

    public static void setMiddlePassHash(String pass) {
        setString(R.string.pref_key_pass_hash, pass);
    }

    /**
     * Когда спрашивать пароль?
     * По-умолчанию - при выборе зашифрованной ветки
     * @return
     */
    public static String getWhenAskPass() {
        return getString(R.string.pref_key_when_ask_password,
                context.getString(R.string.pref_when_ask_password_on_select));
    }

    /**
     * Расшифровывать прикрепленные файлы во временный каталог при предпросмотре?
     * По-умолчанию - нет
     * @return
     */
    public static boolean isDecryptFilesInTemp() {
        return getBoolean(R.string.pref_key_is_decrypt_in_temp, false);
    }

    /**
     * Путь к временному каталогу
     * @return
     */
    public static String getTempPath() {
        return getString(R.string.pref_key_temp_path, null);
    }

    public static void setTempPath(String value) {
        setString(R.string.pref_key_temp_path, value);
    }

    /**
     * Выделять записи в списке, у которых есть прикрепленные файлы?
     * По-умолчанию - да
     * @return
     */
    public static boolean isHighlightRecordWithAttach() {
        return getBoolean(R.string.pref_key_is_highlight_attach, true);
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
     * По-умолчанию - нет
     * @return
     */
    public static boolean isKeepScreenOn() {
        return getBoolean(R.string.pref_key_is_keep_screen_on, false);
    }

    /**
     * Устанавливать текущей выбранную при предыдущем запуске ветку
     * По-умолчанию - да
     * @return
     */
    public static boolean isKeepSelectedNode() {
        return getBoolean(R.string.pref_key_is_keep_selected_node, true);
    }

    /**
     * Id выбранной при предыдущем запуске ветки
     * @return
     */
    public static String getSelectedNodeId() {
        return getString(R.string.pref_key_selected_node_id, null);
    }

    /**
     * Формат даты создания записи
     * @return
     */
    public static String getDateFormatString() {
        return getString(R.string.pref_key_date_format_string,
                context.getString(R.string.def_date_format_string));
    }

    /**
     * Открывать записи сразу в режиме редактирования?
     * По-умолчанию - нет
     * @return
     */
    public static boolean isRecordEditMode() {
        return getBoolean(R.string.pref_key_is_record_edit_mode, false);
    }

    /**
     * Сохранять изменения записи автоматически?
     * По-умолчанию - нет
     * @return
     */
    public static boolean isRecordAutoSave() {
        return getBoolean(R.string.pref_key_is_record_auto_save, true);
    }

    /**
     * Писать логи в файл
     * По-умолчанию - нет
     * @return
     */
    public static boolean isWriteLog() {
        return getBoolean(R.string.pref_key_is_write_log, false);
    }

    /**
     * Путь к каталогу с лог-файлом
     * @return
     */
    public static String getLogPath() {
        return getString(R.string.pref_key_log_path, null);
    }

    public static void setLogPath(String value) {
       setString(R.string.pref_key_log_path, value);
    }

    /**
     * Писать логи в файл
     * По-умолчанию - нет
     * @return
     */
    public static boolean isConfirmAppExit() {
        return getBoolean(R.string.pref_key_is_confirm_app_exit, false);
    }

    /**
     * Поисковой запрос
     * @return
     */
    public static String getSearchQuery() {
        return getString(R.string.pref_key_search_query, null);
    }

    public static void setSearchQuery(String value) {
        setString(R.string.pref_key_search_query, value);
    }

    /**
     * Поиск по содержимому записей
     * @return
     */
    public static boolean isSearchInText() {
        return getBoolean(R.string.pref_key_search_text, true);
    }

    public static void setSearchInText(boolean value) {
        setBoolean(R.string.pref_key_search_text, value);
    }

    /**
     * Поиск по именам записей
     * @return
     */
    public static boolean isSearchInRecordsNames() {
        return getBoolean(R.string.pref_key_search_records_names, true);
    }

    public static void setSearchInRecordsNames(boolean value) {
        setBoolean(R.string.pref_key_search_records_names, value);
    }

    /**
     * Поиск по авторам записей
     * @return
     */
    public static boolean isSearchInAuthor() {
        return getBoolean(R.string.pref_key_search_author, true);
    }

    public static void setSearchInAuthor(boolean value) {
        setBoolean(R.string.pref_key_search_author, value);
    }

    /**
     * Поиск по url записей
     * @return
     */
    public static boolean isSearchInUrl() {
        return getBoolean(R.string.pref_key_search_url, true);
    }

    public static void setSearchInUrl(boolean value) {
        setBoolean(R.string.pref_key_search_url, value);
    }

    /**
     * Поиск по меткам
     * @return
     */
    public static boolean isSearchInTags() {
        return getBoolean(R.string.pref_key_search_tags, true);
    }

    public static void setSearchInTags(boolean value) {
        setBoolean(R.string.pref_key_search_tags, value);
    }

    /**
     * Поиск по веткам
     * @return
     */
    public static boolean isSearchInNodes() {
        return getBoolean(R.string.pref_key_search_nodes, true);
    }

    public static void setSearchInNodes(boolean value) {
        setBoolean(R.string.pref_key_search_nodes, value);
    }

    /**
     * Поиск по прикрепленным файлам
     * @return
     */
    public static boolean isSearchInFiles() {
        return getBoolean(R.string.pref_key_search_files, true);
    }

    public static void setSearchInFiles(boolean value) {
        setBoolean(R.string.pref_key_search_files, value);
    }

    /**
     * Поиск по каждому из слов в запросе?
     * @return
     */
    public static boolean isSearchSplitToWords() {
        return getBoolean(R.string.pref_key_search_split_to_words, true);
    }

    public static void setSearchSplitToWords(boolean value) {
        setBoolean(R.string.pref_key_search_split_to_words, value);
    }

    /**
     * Поиск по совпадению только целых слов?
     * @return
     */
    public static boolean isSearchInWholeWords() {
        return getBoolean(R.string.pref_key_search_in_whole_words, true);
    }

    public static void setSearchInWholeWords(boolean value) {
        setBoolean(R.string.pref_key_search_in_whole_words, value);
    }

    /**
     * Поиск только в текущей ветке?
     * @return
     */
    public static boolean isSearchInCurNode() {
        return getBoolean(R.string.pref_key_search_in_cur_node, true);
    }

    public static void setSearchInCurNode(boolean value) {
        setBoolean(R.string.pref_key_search_in_cur_node, value);
    }

    private static void setBoolean(int prefKeyStringRes, boolean value) {
        if (settings == null)
            return;
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(prefKeyStringRes), value);
        editor.apply();
    }

    private static void setString(int prefKeyStringRes, String value) {
        if (settings == null)
            return;
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(context.getString(prefKeyStringRes), value);
        editor.apply();
    }

    /**
     * Получить boolean опцию.
     * @param prefKeyStringRes
     * @param defValue
     * @return
     */
    private static boolean getBoolean(int prefKeyStringRes, final boolean defValue) {
        if (settings == null)
            return defValue;
        if(settings.contains(context.getString(prefKeyStringRes))) {
            return settings.getBoolean(context.getString(prefKeyStringRes), defValue);
        }
        return defValue;
    }

    /**
     * Получить String опцию.
     * @param prefKeyStringRes
     * @param defValue
     * @return
     */
    private static String getString(int prefKeyStringRes, final String defValue) {
        if (settings == null)
            return defValue;
        if(settings.contains(context.getString(prefKeyStringRes))) {
            return settings.getString(context.getString(prefKeyStringRes), defValue);
        }
        return defValue;
    }
    public static SharedPreferences getSettings() {
        return settings;
    }
}
