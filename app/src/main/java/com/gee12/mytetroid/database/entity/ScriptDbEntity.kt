package com.gee12.mytetroid.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class ScriptDbEntity(
    @ColumnInfo(name = "storageId")
    var storageId: Int,

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "fileName")
    var fileName: String,

    @ColumnInfo(name = "description")
    var description: String?,

    @ColumnInfo(name = "orderNum")
    var order: Int,
) : BaseDbEntity()