package com.gee12.mytetroid.domain.provider

import com.gee12.mytetroid.database.entity.SyncProfileEntity
import com.gee12.mytetroid.model.TetroidStorage

interface IStorageSettingsProvider {
    fun isDecryptAttachesToTempFolder(): Boolean
    fun getSyncProfile(): SyncProfileEntity?
    fun isSyncEnabled(): Boolean
    fun getSyncAppName(): String
    fun getSyncCommand(): String
    fun isLoadFavoritesOnly(): Boolean
    fun isKeepLastNode(): Boolean
    fun getLastNodeId(): String?
    fun isSaveMiddlePassLocal(): Boolean
    fun getMiddlePassHash(): String?
    fun isCheckOutsideChanging(): Boolean
}

class StorageSettingsProvider(
    private val storageProvider: IStorageProvider,
) : IStorageSettingsProvider {

    private val storage: TetroidStorage?
        get() = storageProvider.storage

    override fun isDecryptAttachesToTempFolder(): Boolean = storage?.isDecyptToTemp ?: false

    override fun getSyncProfile() = storage?.syncProfile

    override fun isSyncEnabled() = storage?.syncProfile?.isEnabled ?: false

    override fun getSyncAppName() = storage?.syncProfile?.appName.orEmpty()

    override fun getSyncCommand() = storage?.syncProfile?.command.orEmpty()

    override fun isLoadFavoritesOnly() = /*storageProvider.isLoadedFavoritesOnly()*/ storage?.isLoadFavoritesOnly ?: false

    override fun isKeepLastNode() = storage?.isKeepLastNode ?: false

    override fun getLastNodeId() = storage?.lastNodeId

    override fun isSaveMiddlePassLocal() = storage?.isSavePassLocal ?: false

    override fun getMiddlePassHash() = storage?.middlePassHash

    override fun isCheckOutsideChanging() = storage?.syncProfile?.isCheckOutsideChanging ?: false

}