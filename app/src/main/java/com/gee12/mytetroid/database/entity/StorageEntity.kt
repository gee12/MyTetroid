package com.gee12.mytetroid.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storages")
open class StorageEntity(
    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "path")
    var path: String
) : BaseEntity() {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

    @ColumnInfo(name = "isDefault")
    var isDefault: Boolean = false

    @ColumnInfo(name = "isReadOnly")
    var isReadOnly: Boolean = false

    @ColumnInfo(name = "orderNum")
    var order: Int = 0

    @ColumnInfo(name = "trashPath")
    var trashPath: String? = null

    @ColumnInfo(name = "isClearTrashBeforeExit")
    var isClearTrashBeforeExit: Boolean = false

    @ColumnInfo(name = "isAskBeforeClearTrashBeforeExit")
    var isAskBeforeClearTrashBeforeExit: Boolean = false

    @ColumnInfo(name = "quickNodeId")
    var quickNodeId: String? = null

    @ColumnInfo(name = "isLoadFavoritesOnly")
    var isLoadFavoritesOnly: Boolean = false

    @ColumnInfo(name = "isKeepLastNode")
    var isKeepLastNode: Boolean = false

    @ColumnInfo(name = "lastNodeId")
    var lastNodeId: String? = null

    @ColumnInfo(name = "isSavePassLocal")
    var isSavePassLocal: Boolean = false

    @ColumnInfo(name = "middlePassHash")
    var middlePassHash: String? = null

    @ColumnInfo(name = "isDecryptToTemp")
    var isDecyptToTemp: Boolean = false

    @Embedded(prefix="syncProfile")
    var syncProfile = SyncProfileEntity(false)
}
