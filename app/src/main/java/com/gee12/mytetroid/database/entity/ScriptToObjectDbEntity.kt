package com.gee12.mytetroid.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scriptsToObjects")
open class ScriptToObjectDbEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,

    @ColumnInfo(name = "scriptId")
    var scriptId: Int,

    @ColumnInfo(name = "objectTypeId")
    var objectTypeId: Int?,

    @ColumnInfo(name = "objectId")
    var objectId: String?,
) : BaseDbEntity()