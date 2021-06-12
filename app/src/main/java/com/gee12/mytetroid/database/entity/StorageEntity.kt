package com.gee12.mytetroid.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storages")
data class StorageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "isReadOnly")
    val isReadOnly: Boolean
)
