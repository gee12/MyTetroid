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
    var uri: String
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

    // TODO: сделать миграцию и убрать поле

    @Deprecated("Используется для совместимости")
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


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StorageEntity) return false

        if (name != other.name) return false
        if (uri != other.name) return false
        if (id != other.id) return false
        if (isDefault != other.isDefault) return false
        if (isReadOnly != other.isReadOnly) return false
        if (order != other.order) return false
        if (trashPath != other.trashPath) return false
        if (isClearTrashBeforeExit != other.isClearTrashBeforeExit) return false
        if (isAskBeforeClearTrashBeforeExit != other.isAskBeforeClearTrashBeforeExit) return false
        if (quickNodeId != other.quickNodeId) return false
        if (isLoadFavoritesOnly != other.isLoadFavoritesOnly) return false
        if (isKeepLastNode != other.isKeepLastNode) return false
        if (lastNodeId != other.lastNodeId) return false
        if (isSavePassLocal != other.isSavePassLocal) return false
        if (middlePassHash != other.middlePassHash) return false
        if (isDecyptToTemp != other.isDecyptToTemp) return false
        if (syncProfile != other.syncProfile) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + id
        result = 31 * result + isDefault.hashCode()
        result = 31 * result + isReadOnly.hashCode()
        result = 31 * result + order
        result = 31 * result + (trashPath?.hashCode() ?: 0)
        result = 31 * result + isClearTrashBeforeExit.hashCode()
        result = 31 * result + isAskBeforeClearTrashBeforeExit.hashCode()
        result = 31 * result + (quickNodeId?.hashCode() ?: 0)
        result = 31 * result + isLoadFavoritesOnly.hashCode()
        result = 31 * result + isKeepLastNode.hashCode()
        result = 31 * result + (lastNodeId?.hashCode() ?: 0)
        result = 31 * result + isSavePassLocal.hashCode()
        result = 31 * result + (middlePassHash?.hashCode() ?: 0)
        result = 31 * result + isDecyptToTemp.hashCode()
        result = 31 * result + syncProfile.hashCode()
        return result
    }

}
