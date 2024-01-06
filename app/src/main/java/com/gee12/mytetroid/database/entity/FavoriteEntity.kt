package com.gee12.mytetroid.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
open class FavoriteEntity(
    @ColumnInfo(name = "storageId")
    var storageId: Int,

    @ColumnInfo(name = "objectId")
    var objectId: String,

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,

    @ColumnInfo(name = "orderNum")
    var order: Int = 0
) : BaseEntity()