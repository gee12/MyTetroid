package com.gee12.mytetroid.logs

import com.gee12.mytetroid.R


enum class LogObj {
    NONE,
    STORAGE(R.array.obj_storage),
    NODE(R.array.obj_node),
    NODE_FIELDS(R.array.obj_node_fields),
    RECORD(R.array.obj_record),
    TEMP_RECORD(R.array.obj_temp_record),
    RECORD_FIELDS(R.array.obj_record_fields),
    RECORD_DIR(R.array.obj_record_dir),
    TAG(R.array.obj_tag),
    FILE(R.array.obj_file),
    FOLDER(R.array.obj_folder),
    IMAGE(R.array.obj_image),
    FILE_FIELDS(R.array.obj_file_fields),
    CUR_PASS,
    NEW_PASS;

    var maRes: Int

    constructor() {
        maRes = 0
    }

    constructor(arrayRes: Int) {
        maRes = arrayRes
    }

    fun getString(tense: Int, getStringArrayCallback: (arrayResId: Int) -> Array<String>): String {
        return if (maRes > 0 && tense >= 0 && tense < 3) getStringArrayCallback(maRes)[tense] else ""
    }

}