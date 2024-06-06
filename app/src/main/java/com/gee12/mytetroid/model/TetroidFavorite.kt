package com.gee12.mytetroid.model

import com.gee12.mytetroid.database.entity.FavoriteDbEntity
import com.gee12.mytetroid.model.obj.TetroidObject
import com.gee12.mytetroid.model.obj.TetroidRecord

class TetroidFavorite(
    storageId: Int,
    objectId: String,
    order: Int = 0
) : FavoriteDbEntity(
    storageId = storageId,
    objectId = objectId,
    id = 0,
    order = order,
) {
    var obj: TetroidObject? = null

    constructor(storageId: Int, record: TetroidRecord)
            : this(storageId = storageId, objectId = record.id) {
        this.obj = record
    }
}