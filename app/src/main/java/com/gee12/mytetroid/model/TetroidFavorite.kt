package com.gee12.mytetroid.model

import com.gee12.mytetroid.database.entity.FavoriteEntity

class TetroidFavorite(
    storageId: Int,
    objectId: String,
    order: Int = 0
) : FavoriteEntity(storageId, objectId, 0, order) {
    var obj: TetroidObject? = null

    constructor(storageId: Int, record: TetroidRecord)
            : this(storageId = storageId, objectId = record.id) {
        this.obj = record
    }
}