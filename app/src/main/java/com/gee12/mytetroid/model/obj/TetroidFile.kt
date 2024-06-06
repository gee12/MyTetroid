package com.gee12.mytetroid.model.obj

import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.model.enums.TetroidObjectType

class TetroidFile(
    id: String,
    name: String,
    isEncrypted: Boolean = false,
    var fileType: String? = DEF_FILE_TYPE,
    var record: TetroidRecord,
) : TetroidObject(
    id = id,
    type = TetroidObjectType.ATTACH,
    sourceName = name,
    isEncrypted = isEncrypted,
) {

    companion object {
        const val DEF_FILE_TYPE = "file"
    }

    val idName: String
        get() {
            val ext = FileUtils.getExtensionWithComma(name)
            return id + ext
        }
}
