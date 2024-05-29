package com.gee12.mytetroid.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryDbEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,

    @ColumnInfo(name = "storageId")
    var storageId: Int,

    @ColumnInfo(name = "object_id")
    var objectId: String,

    @ColumnInfo(name = "type_id")
    var typeId: Int,

    @ColumnInfo(name = "name")
    var name: String,
) : BaseDbEntity()