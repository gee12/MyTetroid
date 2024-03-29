package com.gee12.mytetroid.common

object Constants {

    // TODO: перенести в DataNameProvider
    /**
     * Формат даты создания записи.
     */
    const val DATE_TIME_FORMAT = "yyyyMMddHHmmss"

    /**
     * Разделитель меток - запятая или запятая с пробелами.
     */
    const val TAGS_SEPARATOR = ", "
    const val TAGS_SEPARATOR_MASK = "\\s*,\\s*"

    const val BASE_DIR_NAME = "base"
    const val TRASH_DIR_NAME = "trash"
    const val LOG_DIR_NAME = "log"
    const val ICONS_DIR_NAME = "icons"
    const val DOWNLOADS_DIR_NAME = "mytetroid"
    const val MYTETRA_XML_FILE_NAME = "mytetra.xml"
    const val DATABASE_INI_FILE_NAME = "database.ini"

    const val LOG_TAG = "MyTetroid"
    /**
     * Версии настроек в SharedPreferences:
     *  1. Начальные, до версии приложения 50
     *  2. Многобазовость, начиная с версии приложения 50; выполняется миграция на настройки в бд
     */
    const val SETTINGS_VERSION_1_START = 1
    const val SETTINGS_VERSION_2_MULTIBASE = 2
    const val SETTINGS_VERSION_CURRENT = SETTINGS_VERSION_2_MULTIBASE

    const val MIN_PINCODE_LENGTH = 4
    const val MAX_PINCODE_LENGTH = 8

    const val ACTION_MAIN_ACTIVITY = "ACTION_MAIN_ACTIVITY"
    const val ACTION_RECORD = "ACTION_RECORD"
    const val ACTION_ADD_RECORD = "ACTION_ADD_RECORD"
    const val ACTION_STORAGE_SETTINGS = "ACTION_STORAGE_SETTINGS"

    const val REQUEST_CODE_COMMON_SETTINGS_ACTIVITY = 101   // общие настройки
    const val REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY = 102  // настройки хранилища
    const val REQUEST_CODE_RECORD_ACTIVITY = 103            // текст записи
    const val REQUEST_CODE_SEARCH_ACTIVITY = 104            // глобальный поиск
    const val REQUEST_CODE_STORAGES_ACTIVITY = 105          // список хранилищ
    const val REQUEST_CODE_NODE_ICON = 106                  // иконка ветки
    const val REQUEST_CODE_SYNC_STORAGE = 107               // синхронизация хранилища

    const val RESULT_REINIT_STORAGE = 1
    const val RESULT_PASS_CHANGED = 2
    const val RESULT_OPEN_RECORD = 3
    const val RESULT_OPEN_NODE = 4
    const val RESULT_SHOW_ATTACHES = 5
    const val RESULT_SHOW_TAG = 6
    const val RESULT_DELETE_RECORD = 7

    const val EXTRA_START_RECORD_ACTIVITY = "START_RECORD_ACTIVITY"

    const val EXTRA_RECORD_ID = "RECORD_ID"
    const val EXTRA_CUR_NODE_ID = "CUR_NODE_ID"
    const val EXTRA_NODE_ID = "NODE_ID"
    const val EXTRA_STORAGE_ID = "STORAGE_ID"

    const val EXTRA_QUERY = "QUERY"
    const val EXTRA_SHOW_STORAGE_INFO = "SHOW_STORAGE_INFO"

    const val EXTRA_RESULT_ACTION_TYPE = "RESULT_ACTION_TYPE"
    const val EXTRA_TAG_NAME = "TAG_NAME"
    const val EXTRA_IS_SAVED_IN_TREE = "IS_SAVED_IN_TREE"
    const val EXTRA_IS_FIELDS_EDITED = "IS_FIELDS_EDITED"
    const val EXTRA_IS_TEXT_EDITED = "IS_TEXT_EDITED"
    const val EXTRA_IMAGES_URI = "IMAGES_URI"
    const val EXTRA_ATTACHED_FILES = "ATTACHED_FILES"

    const val EXTRA_NODE_ICON_PATH = "NODE_ICON_PATH"
    const val EXTRA_IS_DROP = "IS_DROP"

    const val EXTRA_SEARCH_PROFILE = "SEARCH_PROFILE"

    const val EXTRA_IS_REINIT_STORAGE = "IS_REINIT_STORAGE"
    const val EXTRA_IS_CREATE_STORAGE = "IS_CREATE_STORAGE"
    const val EXTRA_IS_RELOAD_STORAGE_ENTITY = "IS_RELOAD_STORAGE_ENTITY"
    const val EXTRA_IS_LOAD_STORAGE = "IS_LOAD_STORAGE"
    const val EXTRA_IS_LOAD_ALL_NODES = "IS_LOAD_ALL_NODES"
    const val EXTRA_IS_PASS_CHANGED = "IS_PASS_CHANGED"
    const val EXTRA_IS_CLOSE_STORAGE = "IS_CLOSE_STORAGE"

    enum class TetroidView {
        Main,
        Settings
    }

}