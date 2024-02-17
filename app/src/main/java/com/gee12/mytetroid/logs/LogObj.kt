package com.gee12.mytetroid.logs

import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.enums.Tense


enum class LogObj(private val tensesResArray: Int? = null) {
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
    NEW_PASS,
    SCRIPT(R.array.obj_script);

    fun getString(tense: Tense, resourcesProvider: IResourcesProvider): String {
        return tensesResArray?.takeIf { tense.id in 0..2 }?.let {
            resourcesProvider.getStringArray(tensesResArray)[tense.id]
        }.orEmpty()
    }

}