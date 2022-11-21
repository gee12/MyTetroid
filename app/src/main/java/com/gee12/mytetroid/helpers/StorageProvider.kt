package com.gee12.mytetroid.helpers

import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidFavorite
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.model.TetroidTag
import java.util.HashMap

interface IStorageProvider {

    val storage: TetroidStorage?
    val databaseConfig: DatabaseConfig
    val favorites: MutableList<TetroidFavorite>

    fun init(storageDataProcessor: IStorageDataProcessor)
    fun setStorage(storage: TetroidStorage)
    fun resetStorage()
    fun isLoaded(): Boolean
    fun isLoadedFavoritesOnly(): Boolean
    fun isExistCryptedNodes(): Boolean
    fun setIsExistCryptedNodes(value: Boolean)
    fun getRootNodes(): List<TetroidNode>
    fun getTagsMap(): HashMap<String, TetroidTag>
    fun getRootNode(): TetroidNode
}

class StorageProvider(
    private val logger: ITetroidLogger,
) : IStorageProvider {

    private lateinit var storageDataProcessor: IStorageDataProcessor

    override var storage: TetroidStorage? = null
        private set

    override val databaseConfig = DatabaseConfig(logger)

    override val favorites: MutableList<TetroidFavorite> = mutableListOf()

    override fun init(storageDataProcessor: IStorageDataProcessor) {
        this.storageDataProcessor = storageDataProcessor
    }

    override fun setStorage(storage: TetroidStorage) {
        this.storage = storage
    }

    override fun resetStorage() {
        this.storage = null
        // TODO
        // dataProcessor.reset()
        // crypter.reset()
    }

    override fun isLoaded(): Boolean {
        return /*(storage?.isLoaded ?: false)
                &&*/ storageDataProcessor.isLoaded()
    }

    override fun isLoadedFavoritesOnly(): Boolean {
        return /*(storage?.isLoadFavoritesOnly ?: false)
                &&*/ storageDataProcessor.isLoadFavoritesOnlyMode()
    }

    override fun isExistCryptedNodes(): Boolean {
        return storageDataProcessor.isExistCryptedNodes
    }

    override fun setIsExistCryptedNodes(value: Boolean) {
        storageDataProcessor.isExistCryptedNodes = value
    }

    override fun getRootNodes(): List<TetroidNode> {
        return storageDataProcessor.getRootNodes()
    }

    override fun getTagsMap(): HashMap<String, TetroidTag> {
        return storageDataProcessor.getTagsMap()
    }

    override fun getRootNode(): TetroidNode {
        return storageDataProcessor.getRootNode()
    }

}