package com.gee12.mytetroid.common

import java.io.File

object Constants {

    val SEPAR = File.separator

    const val ACTION_MAIN_ACTIVITY = "ACTION_MAIN_ACTIVITY"
    const val ACTION_RECORD = "ACTION_RECORD"
    const val ACTION_ADD_RECORD = "ACTION_ADD_RECORD"

    const val REQUEST_CODE_SETTINGS_ACTIVITY = 101
    const val REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY = 102
    const val REQUEST_CODE_RECORD_ACTIVITY = 103
    const val REQUEST_CODE_SEARCH_ACTIVITY = 104
    const val REQUEST_CODE_STORAGES_ACTIVITY = 105
    const val REQUEST_CODE_FILE_PICKER = 106
    const val REQUEST_CODE_FOLDER_PICKER = 107
    const val REQUEST_CODE_NODE_ICON = 108
    const val REQUEST_CODE_CAMERA = 109
    const val REQUEST_CODE_OPEN_STORAGE_PATH = 110
    const val REQUEST_CODE_CREATE_STORAGE_PATH = 111
    const val REQUEST_CODE_OPEN_TEMP_PATH = 112
    const val REQUEST_CODE_OPEN_LOG_PATH = 113

    const val REQUEST_CODE_OPEN_STORAGE = 111
    const val REQUEST_CODE_CREATE_STORAGE = 112
    const val REQUEST_CODE_SYNC_STORAGE = 113

    const val REQUEST_CODE_PERMISSION_WRITE_STORAGE = 101
    const val REQUEST_CODE_PERMISSION_WRITE_TEMP = 102
    const val REQUEST_CODE_PERMISSION_CAMERA = 103
    const val REQUEST_CODE_PERMISSION_TERMUX = 104

    const val PAGE_MAIN = 0
    const val PAGE_FOUND = 1

    const val MAIN_VIEW_GLOBAL_FOUND = -1
    const val MAIN_VIEW_NONE = 0
    const val MAIN_VIEW_NODE_RECORDS = 1
    const val MAIN_VIEW_RECORD_FILES = 2
    const val MAIN_VIEW_TAG_RECORDS = 3
    const val MAIN_VIEW_FAVORITES = 4

    const val EXTRA_OBJECT_ID = "EXTRA_OBJECT_ID"
    const val EXTRA_CUR_NODE_ID = "EXTRA_CUR_NODE_ID"
    const val EXTRA_NODE_ID = "EXTRA_NODE_ID"
    const val EXTRA_STORAGE_ID = "EXTRA_STORAGE_ID"

    const val EXTRA_QUERY = "EXTRA_QUERY"
    const val EXTRA_SHOW_STORAGE_INFO = "EXTRA_SHOW_STORAGE_INFO"

    const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
    const val EXTRA_TAG_NAME = "EXTRA_TAG_NAME"
    const val EXTRA_IS_FIELDS_EDITED = "EXTRA_IS_FIELDS_EDITED"
    const val EXTRA_IMAGES_URI = "EXTRA_IMAGES_URI"
    const val EXTRA_ATTACHED_FILES = "EXTRA_ATTACHED_FILES"

    const val EXTRA_NODE_ICON_PATH = "EXTRA_NODE_ICON_PATH"
    const val EXTRA_IS_DROP = "EXTRA_IS_DROP"

    const val EXTRA_SCAN_MANAGER = "EXTRA_SCAN_MANAGER"

    const val EXTRA_IS_REINIT_STORAGE = "EXTRA_IS_REINIT_STORAGE"
    const val EXTRA_IS_CREATE_STORAGE = "EXTRA_IS_CREATE_STORAGE"
    const val EXTRA_IS_LOAD_STORAGE = "EXTRA_IS_LOAD_STORAGE"
    const val EXTRA_IS_LOAD_ALL_NODES = "EXTRA_IS_LOAD_ALL_NODES"
    const val EXTRA_IS_PASS_CHANGED = "EXTRA_IS_PASS_CHANGED"


    enum class TetroidView {
        Main,
        Settings
    }

    enum class ViewEvents {
        // activity
        InitGUI,
        UpdateToolbar,
        HandleReceivedIntent,

        // pages
        OpenPage,
        MainPageCreated,
        ShowMainView,
        ClearMainView,
        CloseFoundView,

        // long-term tasks
        TaskStarted,
        TaskFinished,
        ShowProgress,
        ShowProgressText,
    }

    enum class StorageEvents {
        PermissionCheck,
        PermissionChecked,
        AskBeforeSyncOnInit,
        AskAfterSyncOnInit,
        AskBeforeSyncOnExit,
        Init,
        FilesCreated,
        Loaded,
        Decrypted,

        Added,
        Edited,
        Selected,
        InitFailed,
        EmptyPassCheck,
        AskPassword,
        AskPinCode,

        ChangedOutside
    }

    enum class ObjectEvents {
        ShowMoreInLogs,

        // nodes
        ShowNode,
        NodeRenamed,
        AskForDeleteNode,
        NodeCutted,
        NodeDeleted,
        AskForClearStoragePass,
        UpdateNodes,

        // records
        OpenRecord,
        ShowRecords,
        RecordsFiltered,
        RecordDeleted,
        RecordCutted,
        UpdateRecords,

        // tags
        UpdateTags,

        // attaches
        CheckPermissionAndOpenAttach,
        ShowAttaches,
        AttachesFiltered,
        UpdateAttaches,
        AttachDeleted,

        // favorites
        ShowFavorites,
        UpdateFavorites,

        // global search
        GlobalSearchStart,
        GlobalResearch,
        GlobalSearchFinished,

        // storage tree observer
        StartFileObserver,
        StopFileObserver,

        // file system
        AskForOperationWithoutDir,
        AskForOperationWithoutFile,
        OpenFilePicker,
        OpenFolderPicker
    }
}