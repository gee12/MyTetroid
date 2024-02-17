package com.gee12.mytetroid.database.map.scriptToObject

import com.gee12.mytetroid.database.entity.ScriptToObjectDbEntity
import com.gee12.mytetroid.model.TetroidScriptToObject
import com.gee12.mytetroid.model.enums.TetroidObjectType


fun ScriptToObjectDbEntity.toEntity(objectName: String? = null): TetroidScriptToObject {
    val dbEntity = this
    return TetroidScriptToObject(
        id = dbEntity.id,
        scriptId = dbEntity.scriptId,
        objectType = dbEntity.objectTypeId?.let { TetroidObjectType.getById(it) },
        objectName = objectName,
        objectId = dbEntity.objectId,
    )
}

fun TetroidScriptToObject.toDbEntity(): ScriptToObjectDbEntity {
    val entity = this
    return ScriptToObjectDbEntity(
        id = entity.id,
        scriptId = entity.scriptId,
        objectTypeId = entity.objectType?.id,
        objectId = entity.objectId,
    )
}
