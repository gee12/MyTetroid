package com.gee12.mytetroid.data.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.BuildConfig;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.RecordFieldsSelector;
import com.gee12.mytetroid.StringList;
import com.gee12.mytetroid.common.Constants;
import com.gee12.mytetroid.interactors.CommonSettingsInteractor;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommonSettings {

    public static final String PREFS_NAME = "_preferences";
    public static final int DEF_PIN_CODE_LENGTH = 5;
    public static final boolean DEF_SEARCH_IN_RECORD_TEXT = true;
    public static final boolean DEF_SEARCH_IN_RECORDS_NAMES = true;
    public static final boolean DEF_SEARCH_IN_AUTHOR = true;
    public static final boolean DEF_SEARCH_IN_URL = true;
    public static final boolean DEF_SEARCH_IN_TAGS = true;
    public static final boolean DEF_SEARCH_IN_NODES = true;
    public static final boolean DEF_SEARCH_IN_FILES = true;
    public static final boolean DEF_SEARCH_IN_IDS = true;
    public static final boolean DEF_SEARCH_SPLIT_TO_WORDS = false;
    public static final boolean DEF_SEARCH_IN_WHOLE_WORDS = false;
    public static final boolean DEF_SEARCH_IN_CUR_NODE = false;

    private static SharedPreferences settings;
    private static boolean isCopiedFromFree;

    //region Загрузка настроек

    /**
     * Инициализация настроек.
     */
    public static void init(Context context, CommonSettingsInteractor interactor) {
        CommonSettings.settings = getPrefs(context);
        PreferenceManager.setDefaultValues(context, R.xml.prefs, false);

        // стартовые значения, которые нельзя установить в xml
        if (getTrashPathDef(context) == null) {
            setTrashPathDef(context, interactor.getDefaultTrashPath(context));
        }
        if (getLogPath(context) == null) {
            setLogPath(context, interactor.getDefaultLogPath(context));
        }
        if (App.INSTANCE.isFreeVersion()) {
            // принудительно отключаем
            setIsLoadFavoritesOnly(context, false);
        }

        // удаление неактуальной опции из версии 4.1
        if (isContains(context, R.string.pref_key_is_show_tags_in_records)) {
            boolean isShow = isShowTagsInRecordsList(context);
            Set<String> valuesSet = getRecordFieldsInList(context);
            String value = context.getString(R.string.title_tags);
            // включение или отключение значения списка выбора
            setRecordFieldsInList(context, setItemInStringSet(context, valuesSet, isShow, value));
            // удаляем значение старой опции
            removePref(context, R.string.pref_key_is_show_tags_in_records);
        }

        App.IsHighlightAttach = isHighlightRecordWithAttach(context);
        App.IsHighlightCryptedNodes = isHighlightEncryptedNodes(context);
        App.HighlightAttachColor = getHighlightColor(context);
        App.DateFormatString = getDateFormatString(context);
        App.RecordFieldsInList = new RecordFieldsSelector(context, getRecordFieldsInList(context));
    }

    /**
     *
     * @param context
     * @return
     */
    private static SharedPreferences getPrefs(Context context) {
//        SettingsManager.settings = PreferenceManager.getDefaultSharedPreferences(context);
        // SecurityException: MODE_WORLD_READABLE no longer supported
        int mode = (BuildConfig.VERSION_CODE < 24) ? Context.MODE_WORLD_READABLE : Context.MODE_PRIVATE;
        String defAppId = BuildConfig.DEF_APPLICATION_ID;
        //if (BuildConfig.DEBUG) defAppId += ".debug";
        SharedPreferences prefs;
        if (App.INSTANCE.isFullVersion()) {
            prefs = getPrefs(context, Context.MODE_PRIVATE);

            if (prefs != null && prefs.getAll().size() == 0) {
                // настроек нет, версия pro запущена в первый раз
                try {
                    Context freeContext = context.createPackageContext(defAppId, Context.CONTEXT_IGNORE_SECURITY);
                    SharedPreferences freePrefs = freeContext.getSharedPreferences(
                            defAppId + PREFS_NAME, mode);
                    if (freePrefs.getAll().size() > 0) {
                        // сохраняем все настройки из free в pro
                        copyPrefs(freePrefs, prefs);
                        isCopiedFromFree = true;
                    }
                } catch (Exception ex) {
                    return prefs;
                }
            }
            return prefs;
        } else {
            // открываем доступ к чтению настроек для версии Pro
            prefs = getPrefs(context, mode);
        }
        return prefs;
    }

    private static SharedPreferences getPrefs(Context context, int mode) {
        SharedPreferences prefs;
        try {
            prefs = context.getSharedPreferences(BuildConfig.APPLICATION_ID + PREFS_NAME, mode);
        } catch (Exception ex) {
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
        }
        return prefs;
    }
    /**
     * Копирование настроек.
     * @param srcPrefs
     * @param destPrefs
     */
    private static void copyPrefs(SharedPreferences srcPrefs, SharedPreferences destPrefs) {
        Map<String,?> srcMap = srcPrefs.getAll();
        SharedPreferences.Editor destEditor = destPrefs.edit();

        for (Map.Entry<String,?> entry : srcMap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean)
                destEditor.putBoolean(entry.getKey(), (Boolean) value);
            else if (value instanceof String)
                destEditor.putString(entry.getKey(), (String) value);
            else if (value instanceof Integer)
                destEditor.putInt(entry.getKey(), (Integer) value);
            else if (value instanceof Float)
                destEditor.putFloat(entry.getKey(), (Float) value);
            else if (value instanceof Long)
                destEditor.putLong(entry.getKey(), (Long) value);
            else if (value instanceof Set)
                destEditor.putStringSet(entry.getKey(), Set.class.cast(value));
        }
        destEditor.apply();
    }

    public static boolean isCopiedFromFree() {
        return isCopiedFromFree;
    }

    //endregion Загрузка настроек

    //region Миграция

    /**
     * Текущая версия настроек.
     * Значение по-умолчанию = VERSION_FIRST_MIGRATION, т.к. это первая версия, с которой нужно выполнять миграцию настроек.
     */
    public static int getSettingsVersion(Context context) {
        return getInt(context, R.string.pref_key_settings_version, 0);
    }

    public static void setSettingsCurrentVersion(Context context) {
        setInt(context, R.string.pref_key_settings_version, Constants.SETTINGS_VERSION_CURRENT);
    }

    //endregion Миграция

    //region Хранилище

    public static int getLastStorageId(Context context) {
        return getInt(context, R.string.pref_key_last_storage_id, 0);
    }

    public static void setLastStorageId(Context context, int value) {
        setInt(context, R.string.pref_key_last_storage_id, value);
    }

    /**
     * Путь к хранилищу (устаревшее).
     * Используется для совместимости.
     * @return
     */
    @Deprecated
    public static String getStoragePath(Context context) {
        return getString(context, R.string.pref_key_storage_path, null);
    }

    /**
     * Загружать хранилище, используемое при прошлом запуске.
     * По-умолчанию - да.
     */
    @Deprecated
    public static boolean isLoadLastStoragePath(Context context) {
        return getBoolean(context, R.string.pref_key_is_load_last_storage_path, true);
    }

    /**
     * Путь к каталогу корзины.
     * Используется по-умолчанию у хранилищ.
     */
    public static String getTrashPathDef(Context context) {
        return getString(context, R.string.pref_key_temp_path, null);
    }

    public static void setTrashPathDef(Context context, String value) {
        setString(context, R.string.pref_key_temp_path, value);
    }

    /**
     * Очищать корзину перед выходом ?
     * Используется по-умолчанию у хранилищ.
     */
    public static boolean isClearTrashBeforeExitDef(Context context) {
        return getBoolean(context, R.string.pref_key_is_clear_trash_before_exit, false);
    }

    /**
     * Спрашивать перед очисткой корзины ?
     * Используется по-умолчанию у хранилищ.
     */
    public static boolean isAskBeforeClearTrashBeforeExitDef(Context context) {
        return getBoolean(context, R.string.pref_key_is_clear_trash_before_exit, false);
    }

    /**
     * Ветка для быстрой вставки записей. Используется при создании записи:
     *  1) из внешнего Intent
     *  2) из виджета
     *  3) из Избранного
     * Используется по-умолчанию у хранилищ.
     */
    @Deprecated
    public static String getQuicklyNodeId(Context context) {
        return getString(context, R.string.pref_key_quickly_node_id, null);
    }
    @Deprecated
    public static String getQuicklyNodeName(Context context) {
        return getString(context, R.string.pref_key_quickly_node_name, null);
    }

    @Deprecated
    public static void setQuicklyNode(Context context, TetroidNode node) {
        setString(context, R.string.pref_key_quickly_node_id, (node != null) ? node.getId() : null);
        setString(context, R.string.pref_key_quickly_node_name, (node != null) ? node.getName() : null);
    }

    /**
     * Загружать при старте только избранные записи ?
     * По-умолчанию - нет.
     * Используется по-умолчанию у хранилищ.
     */
    public static boolean isLoadFavoritesOnlyDef(Context context) {
        return getBoolean(context, R.string.pref_key_is_load_favorites, false);
    }

    public static void setIsLoadFavoritesOnly(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_is_load_favorites, value);
    }

    /**
     * Получить список Id избранных записей.
     */
    @Deprecated
    public static StringList getFavorites(Context context) {
//        String value = getString(R.string.pref_key_favorites, "");
//        return (!StringUtil.isBlank(value)) ? value.split(";") : new String[0];
        StringList res = new StringList();
        Set<String> set = getStringSet(context, R.string.pref_key_favorites, null);
        if (set != null && set.size() > 0) {
            res.addAll(set);
            // пересохраняем с новым ключем
            setFavorites(context, res);
            // очищаем за ненадобностью
            setStringSet(context, R.string.pref_key_favorites, null);
        } else {
            String json = getString(context, R.string.pref_key_favorites_json, null);
            if (json != null) {
                res.fromJSONString(json);
            }
        }
        return res;
    }

    @Deprecated
    public static void setFavorites(Context context, StringList ids) {
//        setStringSet(new HashSet<>(Arrays.asList(ids)));
//        String value = TextUtils.join(";", ids);
//        setStringSet(context, R.string.pref_key_favorites, new HashSet<>(ids));
        setString(context, R.string.pref_key_favorites_json, ids.toString());
    }

    /**
     * Устанавливать текущей выбранную при предыдущем запуске ветку ?
     * По-умолчанию - да.
     * Используется по-умолчанию у хранилищ.
     */
    public static boolean isKeepLastNodeDef(Context context) {
        return getBoolean(context, R.string.pref_key_is_keep_selected_node, true);
    }

    /**
     * Устаревшее, используется для совместимости с версиями < 5.0.
     * Id ветки, выбранной последний раз.
     */
    @Deprecated
    public static String getLastNodeId(Context context) {
        return getString(context, R.string.pref_key_selected_node_id, null);
    }

    //endregion Хранилище

    //region Шифрование

    /**
     * Сохранять хэш пароля локально ?
     * По-умолчанию - да.
     * Используется по-умолчанию у хранилищ.
     */
    public static boolean isSaveMiddlePassHashLocalDef(Context context) {
        return getBoolean(context, R.string.pref_key_is_save_pass_hash_local, true);
    }

    public static void setIsSaveMiddlePassHashLocal(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_is_save_pass_hash_local, value);
    }

    /**
     * Устаревшее, оставлено для миграции с версии < 5.0.
     * Хэш пароля.
     */
    @Deprecated
    public static String getMiddlePassHash(Context context) {
        return getString(context, R.string.pref_key_pass_hash, null);
    }

    /**
     * Запрашивать ли ПИН-код ?
     */
    public static boolean isRequestPINCode(Context context) {
        return getBoolean(context, R.string.pref_key_request_pin_code, false);
    }

    public static void setIsRequestPINCode(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_request_pin_code, value);
    }

    /**
     * Длина ПИН-кода.
     */
    public static int getPinCodeLength(Context context) {
        return getInt(context, R.string.pref_key_pin_code_length, DEF_PIN_CODE_LENGTH);
    }

    public static void setPINCodeLength(Context context, int value) {
        setInt(context, R.string.pref_key_pin_code_length, value);
    }

    /**
     * Хэш ПИН-кода.
     */
    public static String getPINCodeHash(Context context) {
        return getString(context, R.string.pref_key_pin_code_hash, null);
    }

    public static void setPINCodeHash(Context context, String code) {
        setString(context, R.string.pref_key_pin_code_hash, code);
    }

    /**
     * Когда спрашивать пароль ?
     * По-умолчанию - при выборе зашифрованной ветки.
     */
    public static String getWhenAskPass(Context context) {
        return getString(context, R.string.pref_key_when_ask_password, context.getString(R.string.pref_when_ask_password_on_select));
    }

    public static boolean isAskPassOnStart(Context context) {
        String value = getWhenAskPass(context);
        return value.equals(context.getString(R.string.pref_when_ask_password_on_start));
    }

    /**
     * Расшифровывать прикрепленные файлы во временный каталог при предпросмотре?
     * По-умолчанию - нет.
     * Используется по-умолчанию у хранилищ.
     */
    public static boolean isDecryptFilesInTempDef(Context context) {
        return getBoolean(context, R.string.pref_key_is_decrypt_in_temp, false);
    }

    //endregion Шифрование

    //region Синхронизация

    /**
     * Использовать синхронизацию хранилища.
     * По-умолчанию - нет.
     * Используется по-умолчанию у хранилищ.
     */
    public static boolean isSyncStorageDef(Context context) {
        return getBoolean(context, R.string.pref_key_is_sync_storage, false);
    }

    public static void setIsSyncStorage(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_is_sync_storage, value);
    }

    /**
     * Имя приложения для синхронизации.
     * Используется по-умолчанию у хранилищ.
     */
    public static String getSyncAppNameDef(Context context) {
        return getString(context, R.string.pref_key_app_for_sync, context.getString(R.string.title_app_mgit));
    }

    /**
     * Команда/скрипт синхронизации для стороннего приложения.
     * Например: "git pull".
     * Используется по-умолчанию у хранилищ.
     */
    public static String getSyncCommandDef(Context context) {
        return getString(context, R.string.pref_key_sync_command, "");
    }

    /**
     * Запускать синхронизацию хранилища перед его загрузкой.
     * По-умолчанию - да.
     * Используется по-умолчанию у хранилищ.
     */
    public static boolean isSyncBeforeInitDef(Context context) {
        return getBoolean(context, R.string.pref_key_is_sync_before_init, true);
    }

    /**
     * Запускать синхронизацию хранилища перед выходом из приложения.
     * По-умолчанию - да.
     * Используется по-умолчанию у хранилищ.
     */
    public static boolean isSyncBeforeExitDef(Context context) {
        return getBoolean(context, R.string.pref_key_is_sync_before_exit, true);
    }

    /**
     * Выводить подтверждение запуска синхронизации перед загрузкой хранилища.
     * По-умолчанию - да.
     * Используется по-умолчанию у хранилищ.
     */
    public static boolean isAskBeforeSyncOnInitDef(Context context) {
        return getBoolean(context, R.string.pref_key_is_ask_before_sync, true);
    }

    /**
     * Выводить подтверждение запуска синхронизации при выходе из приложения.
     * По-умолчанию - нет.
     * Используется по-умолчанию у хранилищ.
     */
    public static boolean isAskBeforeSyncOnExitDef(Context context) {
        return getBoolean(context, R.string.pref_key_is_ask_before_exit_sync, false);
    }

    /**
     * Устаревшее, уже не используется.
     * Не запоминать используемое приложения для синхронизации в последний раз.
     * По-умолчанию - нет.
     */
    @Deprecated
    public static boolean isNotRememberSyncApp(Context context) {
//        return getBoolean(context, R.string.pref_key_is_not_remember_sync_app, false);
        return false;
    }

    /**
     * Отслеживать изменения структуры хранилища внешними программами.
     * Используется по-умолчанию у хранилищ.
     */
    public static boolean isCheckOutsideChangingDef(Context context) {
        return getBoolean(context, R.string.pref_key_check_outside_changing, true);
    }

    //endregion Синхронизация

    //region Редактирование

    /**
     * Открывать записи сразу в режиме редактирования ?
     * По-умолчанию - нет.
     */
    public static boolean isRecordEditMode(Context context) {
        return getBoolean(context, R.string.pref_key_is_record_edit_mode, false);
    }

    /**
     * Сохранять изменения записи автоматически ?
     * По-умолчанию - нет.
     */
    public static boolean isRecordAutoSave(Context context) {
        return getBoolean(context, R.string.pref_key_is_record_auto_save, false);
    }

    /**
     * Исправлять в html-тексте записи абзацы со стилем "-qt-paragraph-type:empty;" для MyTetra ?
     * По-умолчанию - да.
     */
    public static boolean isFixEmptyParagraphs(Context context) {
        return getBoolean(context, R.string.pref_key_fix_empty_paragraphs, true);
    }

    //endregion Редактирование

    //region Отображение

    /**
     * Сохранять ли экран активным при просмотре записи ?
     * По-умолчанию - нет.
     */
    public static boolean isKeepScreenOn(Context context) {
        return getBoolean(context, R.string.pref_key_is_keep_screen_on, false);
    }

    /**
     * Отображать ли панель свойств записи при открытии ?
     * По-умолчанию - не отображать.
     */
    public static String getShowRecordFields(Context context) {
        return getString(context, R.string.pref_key_show_record_fields,
                context.getString(R.string.pref_show_record_fields_no));
    }

    /**
     * Выделять записи в списке, у которых есть прикрепленные файлы ?
     * По-умолчанию - нет.
     */
    public static boolean isHighlightRecordWithAttach(Context context) {
        return getBoolean(context, R.string.pref_key_is_highlight_attach, false);
    }

    /**
     * Выделять зашифрованные ветки в списке ?
     * По-умолчанию - нет.
     */
    public static boolean isHighlightEncryptedNodes(Context context) {
        return getBoolean(context, R.string.pref_key_is_highlight_crypted_nodes, false);
    }

    /**
     * Цвет подсветки.
     * По-умолчанию - светло зеленый.
     */
    public static int getHighlightColor(Context context) {
        return getInt(context, R.string.pref_key_highlight_attach_color, R.color.colorHighlight);
    }

    public static int[] getPickedColors(Context context) {
        String value = getString(context, R.string.pref_key_picked_colors, null);
        return (value != null) ? Utils.splitToInts(value, ";") : null;
    }

    public static void setPickedColors(Context context, int[] value) {
        String s = Utils.concatToString(value, ";");
        setString(context, R.string.pref_key_picked_colors, s);
    }

    public static void addPickedColor(Context context, int color, int maxColors) {
        int[] savedColors = CommonSettings.getPickedColors(context);
        int[] res = Utils.addElem(savedColors, color, maxColors, false);
        CommonSettings.setPickedColors(context, res);
    }

    public static void removePickedColor(Context context, int color) {
        int[] savedColors = CommonSettings.getPickedColors(context);
        int[] res = Utils.removeElem(savedColors, color);
        CommonSettings.setPickedColors(context, res);
    }

    /**
     * Формат даты создания записи.
     */
    public static String getDateFormatString(Context context) {
        return getString(context, R.string.pref_key_date_format_string,
                context.getString(R.string.def_date_format_string));
    }

    /**
     * Устарело, оставлено для совместимости с версиями <= 4.1
     * Отображать ли метки в списке записей ?
     */
    @Deprecated
    public static boolean isShowTagsInRecordsList(Context context) {
        return getBoolean(context, R.string.pref_key_is_show_tags_in_records, false);
    }

    /**
     * Какие свойства отображать в списке записей ?
     * По-умолчанию: метки и дата создания.
     */
    public static Set<String> getRecordFieldsInList(Context context) {
        HashSet<String> defValues = new HashSet<>(
                Arrays.asList(context.getResources().getStringArray(R.array.record_fields_in_list_def_entries)));
        return getStringSet(context, R.string.pref_key_record_fields_in_list, defValues);
    }

    public static void setRecordFieldsInList(Context context, Set<String> values) {
        setStringSet(context, R.string.pref_key_record_fields_in_list, values);
    }

    /**
     * Параметры сортировки списка меток (поле, направление).
     */
    public static void setTagsSortMode(Context context, String mode) {
        setString(context, R.string.pref_key_tags_sort_mode, mode);
    }

    public static String getTagsSortMode(Context context, String def) {
        return getString(context, R.string.pref_key_tags_sort_mode, def);
    }

    //endregion Отображение

    //region Управление

    /**
     * Включать/отключать полноэкранный режим при двойном тапе ?
     * По-умолчанию - да.
     */
    public static boolean isDoubleTapFullscreen(Context context) {
        return getBoolean(context, R.string.pref_key_double_tap_fullscreen, true);
    }

    /**
     * Разворот пустых веток вместо отображения их записей.
     */
    public static boolean isExpandEmptyNode(Context context) {
        return getBoolean(context, R.string.pref_key_is_expand_empty_nodes, false);
    }

    /**
     * Отображать ли список веток вместо выхода из приложения ?
     * По-умолчанию - нет.
     */
    public static boolean isShowNodesInsteadExit(Context context) {
        return getBoolean(context, R.string.pref_key_show_nodes_instead_exit, true);
    }

    /**
     * Подтверждать выход из приложения ?
     * По-умолчанию - да.
     */
    public static boolean isConfirmAppExit(Context context) {
        return getBoolean(context, R.string.pref_key_is_confirm_app_exit, true);
    }

    //endregion Управление

    //region Остальное

    /**
     * Писать логи в файл ?
     * По-умолчанию - нет.
     */
    public static boolean isWriteLogToFile(Context context) {
        return getBoolean(context, R.string.pref_key_is_write_log, false);
    }

    /**
     * Путь к каталогу с лог-файлом.
     */
    public static String getLogPath(Context context) {
        return getString(context, R.string.pref_key_log_path, null);
    }

    public static void setLogPath(Context context, String value) {
       setString(context, R.string.pref_key_log_path, value);
    }

    /**
     * Путь к каталогу, выбранному в последний раз.
     */
    public static String getLastChoosedFolderPath(Context context) {
        return getString(context, R.string.pref_key_last_folder, null);
    }

    public static void setLastChoosedFolder(Context context, String path) {
        setString(context, R.string.pref_key_last_folder, path);
    }

    //endregion Остальное

    //region Глобальный поиск

    /**
     * Поисковой запрос.
     */
    public static String getSearchQuery(Context context) {
        return getString(context, R.string.pref_key_search_query, null);
    }

    public static void setSearchQuery(Context context, String value) {
        setString(context, R.string.pref_key_search_query, value);
    }

    /**
     * Поиск по содержимому записей.
     */
    public static boolean isSearchInText(Context context) {
        return getBoolean(context, R.string.pref_key_search_text, DEF_SEARCH_IN_RECORD_TEXT);
    }

    public static void setSearchInText(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_search_text, value);
    }

    /**
     * Поиск по именам записей.
     */
    public static boolean isSearchInRecordsNames(Context context) {
        return getBoolean(context, R.string.pref_key_search_records_names, DEF_SEARCH_IN_RECORDS_NAMES);
    }

    public static void setSearchInRecordsNames(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_search_records_names, value);
    }

    /**
     * Поиск по авторам записей.
     */
    public static boolean isSearchInAuthor(Context context) {
        return getBoolean(context, R.string.pref_key_search_author, DEF_SEARCH_IN_AUTHOR);
    }

    public static void setSearchInAuthor(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_search_author, value);
    }

    /**
     * Поиск по url записей.
     */
    public static boolean isSearchInUrl(Context context) {
        return getBoolean(context, R.string.pref_key_search_url, DEF_SEARCH_IN_URL);
    }

    public static void setSearchInUrl(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_search_url, value);
    }

    /**
     * Поиск по меткам.
     */
    public static boolean isSearchInTags(Context context) {
        return getBoolean(context, R.string.pref_key_search_tags, DEF_SEARCH_IN_TAGS);
    }

    public static void setSearchInTags(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_search_tags, value);
    }

    /**
     * Поиск по веткам.
     */
    public static boolean isSearchInNodes(Context context) {
        return getBoolean(context, R.string.pref_key_search_nodes, DEF_SEARCH_IN_NODES);
    }

    public static void setSearchInNodes(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_search_nodes, value);
    }

    /**
     * Поиск по прикрепленным файлам.
     */
    public static boolean isSearchInFiles(Context context) {
        return getBoolean(context, R.string.pref_key_search_files, DEF_SEARCH_IN_FILES);
    }

    public static void setSearchInFiles(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_search_files, value);
    }

    /**
     * Поиск по Id.
     */
    public static boolean isSearchInIds(Context context) {
        return getBoolean(context, R.string.pref_key_search_ids, DEF_SEARCH_IN_IDS);
    }

    public static void setSearchInIds(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_search_ids, value);
    }

    /**
     * Поиск по каждому из слов в запросе ?
     */
    public static boolean isSearchSplitToWords(Context context) {
        return getBoolean(context, R.string.pref_key_search_split_to_words, DEF_SEARCH_SPLIT_TO_WORDS);
    }

    public static void setSearchSplitToWords(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_search_split_to_words, value);
    }

    /**
     * Поиск по совпадению только целых слов ?
     */
    public static boolean isSearchInWholeWords(Context context) {
        return getBoolean(context, R.string.pref_key_search_in_whole_words, DEF_SEARCH_IN_WHOLE_WORDS);
    }

    public static void setSearchInWholeWords(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_search_in_whole_words, value);
    }

    /**
     * Устарело, используется для совместимости.
     * Поиск только в текущей ветке ?
     */
    @Deprecated
    public static boolean isSearchInCurNode(Context context) {
        return getBoolean(context, R.string.pref_key_search_in_cur_node, DEF_SEARCH_IN_CUR_NODE);
    }

    @Deprecated
    public static void setSearchInCurNode(Context context, boolean value) {
        setBoolean(context, R.string.pref_key_search_in_cur_node, value);
    }

    /**
     * Варианты поиска: по всей базе, в текущей ветке или в указанной ветке.
     */
    public static int getSearchInNodeMode(Context context) {
        return getInt(context, R.string.pref_key_search_in_node_mode, isSearchInCurNode(context) ? 1 : 0);
    }

    public static void setSearchInNodeMode(Context context, int mode) {
        setInt(context, R.string.pref_key_search_in_node_mode, mode);
    }

    /**
     * Id указанной ветки для поиска.
     */
    public static String getSearchNodeId(Context context) {
        return getString(context, R.string.pref_key_search_node_id, null);
    }

    public static void setSearchNodeId(Context context, String nodeId) {
        setString(context, R.string.pref_key_search_node_id, nodeId);
    }

    /**
     * Очистка параметров глобального поиска.
     */
    public static void clearSearchOptions(Context context) {
        setSearchQuery(context, null);
        setSearchInText(context, DEF_SEARCH_IN_RECORD_TEXT);
        setSearchInRecordsNames(context, DEF_SEARCH_IN_RECORDS_NAMES);
        setSearchInAuthor(context, DEF_SEARCH_IN_AUTHOR);
        setSearchInUrl(context, DEF_SEARCH_IN_URL);
        setSearchInTags(context, DEF_SEARCH_IN_TAGS);
        setSearchInNodes(context, DEF_SEARCH_IN_NODES);
        setSearchInFiles(context, DEF_SEARCH_IN_FILES);
        setSearchSplitToWords(context, DEF_SEARCH_SPLIT_TO_WORDS);
        setSearchInWholeWords(context, DEF_SEARCH_IN_WHOLE_WORDS);
        setSearchInCurNode(context, DEF_SEARCH_IN_CUR_NODE);
    }

    //endregion Глобальный поиск

    //region Вспомогательные функции

    /**
     * Установить boolean опцию.
     * @param prefKeyStringRes
     * @param value
     */
    private static void setBoolean(Context context, int prefKeyStringRes, boolean value) {
        if (settings == null)
            return;
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(context.getString(prefKeyStringRes), value);
        editor.apply();
    }

    /**
     * Установить int опцию.
     * @param prefKeyStringRes
     * @param value
     */
    private static void setInt(Context context, int prefKeyStringRes, int value) {
        if (settings == null)
            return;
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(context.getString(prefKeyStringRes), value);
        editor.apply();
    }

    /**
     * Установить String опцию.
     * @param prefKeyStringRes
     * @param value
     */
    private static void setString(Context context, int prefKeyStringRes, String value) {
        if (settings == null)
            return;
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(context.getString(prefKeyStringRes), value);
        editor.apply();
    }

    /**
     * Установить коллекцию значений String в опцию.
     * @param prefKeyStringRes
     * @param set
     */
    private static void setStringSet(Context context, int prefKeyStringRes, Set<String> set) {
        if (settings == null)
            return;
        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(context.getString(prefKeyStringRes), set);
        editor.apply();
    }

    /**
     * Получить boolean опцию.
     * @param prefKeyStringRes
     * @param defValue
     * @return
     */
    private static boolean getBoolean(Context context, int prefKeyStringRes, final boolean defValue) {
        if (settings == null)
            return defValue;
        if (settings.contains(context.getString(prefKeyStringRes))) {
            return settings.getBoolean(context.getString(prefKeyStringRes), defValue);
        }
        return defValue;
    }

    /**
     * Получить int опцию.
     * @param prefKeyStringRes
     * @param defValue
     * @return
     */
    private static int getInt(Context context, int prefKeyStringRes, final int defValue) {
        if (settings == null)
            return defValue;
        if (settings.contains(context.getString(prefKeyStringRes))) {
            return settings.getInt(context.getString(prefKeyStringRes), defValue);
        }
        return defValue;
    }

    /**
     * Получить String опцию.
     * @param prefKeyStringRes
     * @param defValue
     * @return
     */
    private static String getString(Context context, int prefKeyStringRes, final String defValue) {
        if (settings == null)
            return defValue;
        if (settings.contains(context.getString(prefKeyStringRes))) {
            return settings.getString(context.getString(prefKeyStringRes), defValue);
        }
        return defValue;
    }

    /**
     * Получить коллекцию значений String из опции.
     * @param prefKeyStringRes
     * @param defValues
     * @return
     */
    private static Set<String> getStringSet(Context context, int prefKeyStringRes, Set<String> defValues) {
        if (settings == null)
            return null;
        if (settings.contains(context.getString(prefKeyStringRes))) {
            return settings.getStringSet(context.getString(prefKeyStringRes), defValues);
        }
        return defValues;
    }

    /**
     * Удаление опции из настроек.
     * @param context
     * @param prefKeyStringRes
     */
    private static void removePref(Context context, int prefKeyStringRes) {
        if (settings == null)
            return;
        if (settings.contains(context.getString(prefKeyStringRes))) {
            SharedPreferences.Editor editor = settings.edit();
            editor.remove(context.getString(prefKeyStringRes));
            editor.apply();
        }
    }

    /**
     * Установка/отключение элемента списка выбора.
     * @param context
     * @param set
     * @param isSet
     * @param value
     */
    public static Set<String> setItemInStringSet(Context context, Set<String> set, boolean isSet, String value) {
        List<String> valuesList = new ArrayList<>(set);
        if (isSet) {
            if (!valuesList.contains(value)) {
                valuesList.add(value);
            }
        } else {
            valuesList.remove(value);
        }
        return new HashSet<>(valuesList);
    }

    /**
     * Проверка существования значения опции в настройках.
     * @param context
     * @param prefKeyStringRes
     * @return
     */
    public static boolean isContains(Context context, int prefKeyStringRes) {
        if (settings == null)
            return false;
        return settings.contains(context.getString(prefKeyStringRes));
    }

    /**
     * Получение настроек.
     * @return
     */
    public static SharedPreferences getSettings(Context context) {
        if (settings == null) {
            settings = getPrefs(context);
        }
        return settings;
    }

    //endregion Вспомогательные функции

}
