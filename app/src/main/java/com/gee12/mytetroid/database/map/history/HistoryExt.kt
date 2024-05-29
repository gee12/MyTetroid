package com.gee12.mytetroid.database.map.history

import com.gee12.mytetroid.common.extensions.orZero
import com.gee12.mytetroid.database.entity.HistoryDbEntity
import com.gee12.mytetroid.model.HistoryEntity
import com.gee12.mytetroid.model.TetroidObject
import com.gee12.mytetroid.model.enums.TetroidObjectType

fun HistoryDbEntity.toEntity(): HistoryEntity {
    val dbEntity = this
    val type = TetroidObjectType.getById(dbEntity.typeId) ?: TetroidObjectType.NONE

    return HistoryEntity(
        id = dbEntity.id,
        storageId = dbEntity.storageId,
        obj = TetroidObject(
            type.id,
            false,
            dbEntity.objectId,
            dbEntity.name,
        ),
        type = type,
        createdDate = dbEntity.createdDate,
    )
}

fun HistoryEntity.toDbEntity(): HistoryDbEntity {
    val entity = this
    return HistoryDbEntity(
        id = entity.id.orZero(),
        storageId = entity.storageId,
        objectId = obj.id,
        typeId = type.id,
        name = obj.name,
    ).apply {
        createdDate = entity.createdDate
    }
}
