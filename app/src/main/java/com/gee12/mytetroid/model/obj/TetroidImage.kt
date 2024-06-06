package com.gee12.mytetroid.model.obj

import com.gee12.mytetroid.model.enums.TetroidObjectType

// TODO: нужно ли наследоваться от TetroidObject ?
class TetroidImage(
    nameId: String,
    val record: TetroidRecord,
    var width: Int = 0,
    var height: Int = 0,
) : TetroidObject(
    id = nameId,
    type = TetroidObjectType.IMAGE,
    sourceName = nameId,
    isEncrypted = record.isEncrypted,
)