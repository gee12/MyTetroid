package com.gee12.mytetroid.model.permission

enum class PermissionRequestCode(val code: Int) {
    OPEN_STORAGE_FOLDER(101),
    CHANGE_STORAGE_FOLDER(102),
    SELECT_FOLDER_FOR_NEW_STORAGE(103),
    CREATE_STORAGE_FILES(104),
    OPEN_RECORD_FILE(105),
    OPEN_ATTACH_FILE(106),
    OPEN_CAMERA(107),
    RECORD_AUDIO(108),
    TERMUX(109),
    EXPORT_PDF(110),
    PICK_ATTACH_FILE(111),
    PICK_FOLDER_FOR_ATTACH_FILE(112);

    fun toPermission(): TetroidPermission? {
        return when (this) {
            OPEN_STORAGE_FOLDER,
            CHANGE_STORAGE_FOLDER,
            SELECT_FOLDER_FOR_NEW_STORAGE,
            CREATE_STORAGE_FILES,
            OPEN_RECORD_FILE,
            OPEN_ATTACH_FILE,
            EXPORT_PDF,
            PICK_ATTACH_FILE,
            PICK_FOLDER_FOR_ATTACH_FILE -> null // для данных разрешений permission известен сразу
            OPEN_CAMERA -> TetroidPermission.Camera
            RECORD_AUDIO -> TetroidPermission.RecordAudio
            TERMUX -> TetroidPermission.Termux
        }
    }

    companion object {

        fun fromCode(code: Int) : PermissionRequestCode? {
            return values().firstOrNull { it.code == code }
        }

    }
}