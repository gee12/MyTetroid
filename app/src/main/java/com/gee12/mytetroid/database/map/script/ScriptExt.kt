package com.gee12.mytetroid.database.map.script

import com.gee12.mytetroid.common.extensions.orZero
import com.gee12.mytetroid.database.entity.ScriptDbEntity
import com.gee12.mytetroid.model.TetroidScript
import com.gee12.mytetroid.model.TetroidScriptToObject


fun ScriptDbEntity.toEntity(
    objects: List<TetroidScriptToObject>? = null,
    isActive: Boolean = false,
): TetroidScript {
    val dbEntity = this
    return TetroidScript(
        storageId = dbEntity.storageId,
        id = dbEntity.id,
        fileName = dbEntity.fileName,
        description = dbEntity.description,
        order = dbEntity.order,
        createdDate = dbEntity.createdDate,
        editedDate = dbEntity.editedDate,
        objects = objects,
        isActive = isActive,
    )
}

fun TetroidScript.toDbEntity(): ScriptDbEntity {
    val entity = this
    return ScriptDbEntity(
        storageId = entity.storageId,
        id = entity.id.orZero(),
        fileName = entity.fileName,
        description = entity.description,
        order = entity.order,
    ).apply {
        createdDate = entity.createdDate
        editedDate = entity.editedDate
    }
}
