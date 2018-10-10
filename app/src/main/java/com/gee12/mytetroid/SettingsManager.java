package com.gee12.mytetroid;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    public static final String APP_PREFERENCES = "MyTetroidSettings";
    public static final String APP_PREFERENCES_STORAGE_PATH = "StoragePath";
    public static final String APP_PREFERENCES_EXIST_STORAGE_QUESTION = "ExistStorageQuestion";
    public static final String APP_PREFERENCES_ASK_PASSWORD_ON_START = "AskPasswordOnStart";
    public static final String APP_PREFERENCES_HIGHLIGHT_ATTACHED = "HighlightAttached";
    public static final String APP_PREFERENCES_KEEP_SELECTED_NODE = "KeepSelectedNode";

    private static SharedPreferences settings;

    public static void init(Context context) {
        settings = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    /**
     * Путь к хранилищу
     * @return
     */
    public static String getStoragePath() {
        if(settings.contains(APP_PREFERENCES_STORAGE_PATH)) {
            return settings.getString(APP_PREFERENCES_STORAGE_PATH, null);
        }
        return null;
    }

    public static void setStoragePath(String value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(APP_PREFERENCES_STORAGE_PATH, value);
        editor.apply();
    }

    /**
     * Задавать вопрос о загрузке хранилища по сохраненному пути при старте?
     * По-умолчанию - да
     * @return
     */
    public static boolean isShowExistStorageQuestion() {
        if(settings.contains(APP_PREFERENCES_EXIST_STORAGE_QUESTION)) {
            return settings.getBoolean(APP_PREFERENCES_EXIST_STORAGE_QUESTION, true);
        }
        return true;
    }

    public static void setIsShowExistStorageQuestion(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(APP_PREFERENCES_EXIST_STORAGE_QUESTION, value);
        editor.apply();
    }

    /**
     * Запрашивать пароль от зашифрованных веток при старте?
     * (если нет, то только при выборе зашифрованной ветки)
     * По-умолчанию - нет
     * @return
     */
    public static boolean isAskPasswordOnStart() {
        if(settings.contains(APP_PREFERENCES_ASK_PASSWORD_ON_START)) {
            return settings.getBoolean(APP_PREFERENCES_ASK_PASSWORD_ON_START, false);
        }
        return false;
    }

    public static void setIsAskPasswordOnStart(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(APP_PREFERENCES_ASK_PASSWORD_ON_START, value);
        editor.apply();
    }

    /**
     * Выделять записи в списке, у которых есть прикрепленные файлы?
     * По-умолчанию - да
     * @return
     */
    public static boolean isHighlightAttached() {
        if(settings.contains(APP_PREFERENCES_HIGHLIGHT_ATTACHED)) {
            return settings.getBoolean(APP_PREFERENCES_HIGHLIGHT_ATTACHED, true);
        }
        return true;
    }

    public static void setIsHighlightAttached(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(APP_PREFERENCES_HIGHLIGHT_ATTACHED, value);
        editor.apply();
    }

    /**
     * Устанавливать текущей выбранную при предыдущем запуске ветку
     * По-умолчанию - да
     * @return
     */
    public static boolean isKeepSelectedNode() {
        if(settings.contains(APP_PREFERENCES_KEEP_SELECTED_NODE)) {
            return settings.getBoolean(APP_PREFERENCES_KEEP_SELECTED_NODE, true);
        }
        return true;
    }

    public static void setIsKeepSelectedNode(boolean value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(APP_PREFERENCES_KEEP_SELECTED_NODE, value);
        editor.apply();
    }

}
