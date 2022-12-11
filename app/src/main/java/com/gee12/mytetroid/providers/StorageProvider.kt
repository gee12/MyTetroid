package com.gee12.mytetroid.helpers

import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.data.crypt.IStorageCrypter
import com.gee12.mytetroid.data.crypt.StorageCrypter
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidFavorite
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.model.TetroidTag
import com.gee12.mytetroid.usecase.crypt.CryptRecordFilesUseCase
import com.gee12.mytetroid.usecase.tag.ParseRecordTagsUseCase
import java.util.HashMap

interface IStorageProvider {

    val storage: TetroidStorage?
    val databaseConfig: DatabaseConfig
    val dataProcessor: IStorageDataProcessor
    val favorites: MutableList<TetroidFavorite>
//    val pathProvider: IStoragePathProvider
//    val recordPathProvider: IRecordPathProvider
    val crypter: IStorageCrypter
//    val sensitiveDataProvider: ISensitiveDataProvider

//    fun init(/*storageDataProcessor: IStorageDataProcessor*/)
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
    override val crypter: IStorageCrypter,
//    private val cryptRecordFilesUseCase: CryptRecordFilesUseCase,
//    private val parseRecordTagsUseCase: ParseRecordTagsUseCase,
    override val dataProcessor: IStorageDataProcessor,
) : IStorageProvider {

    override var storage: TetroidStorage? = null
        private set
    override val databaseConfig = DatabaseConfig(logger)
//    override lateinit var dataProcessor: IStorageDataProcessor
//        private set
    override val favorites: MutableList<TetroidFavorite> = mutableListOf()
//    override lateinit var pathProvider: IStoragePathProvider
//    override lateinit var recordPathProvider: IRecordPathProvider
//    override lateinit var crypter: IStorageCrypter
//    override lateinit var sensitiveDataProvider: ISensitiveDataProvider


//    override fun init(/*storageDataProcessor: IStorageDataProcessor*/) {
//        this.dataProcessor = storageDataProcessor
//        this.pathProvider = StoragePathProvider(
//
//        )
//        this.sensitiveDataProvider = SensitiveDataProvider()
//        this.crypter = StorageCrypter(
//            logger = logger,
//            crypter = _crypter,
//            cryptRecordFilesUseCase = cryptRecordFilesUseCase,
//            parseRecordTagsUseCase = parseRecordTagsUseCase,
//        )
//    }

    override fun setStorage(storage: TetroidStorage) {
        this.storage = storage
    }

    override fun resetStorage() {
        storage = null
        databaseConfig.setFileName(null)
        favorites.clear()
        // TODO
//         dataProcessor.reset()
//         crypter.reset()
    }

    override fun isLoaded(): Boolean {
        return /*(storage?.isLoaded ?: false)
                &&*/ dataProcessor.isLoaded()
    }

    override fun isLoadedFavoritesOnly(): Boolean {
        return /*(storage?.isLoadFavoritesOnly ?: false)
                &&*/ dataProcessor.isLoadFavoritesOnlyMode()
    }

    override fun isExistCryptedNodes(): Boolean {
        return dataProcessor.isExistCryptedNodes
    }

    override fun setIsExistCryptedNodes(value: Boolean) {
        dataProcessor.isExistCryptedNodes = value
    }

    override fun getRootNodes(): List<TetroidNode> {
        return dataProcessor.getRootNodes()
    }

    override fun getTagsMap(): HashMap<String, TetroidTag> {
        return dataProcessor.getTagsMap()
    }

    override fun getRootNode(): TetroidNode {
        return dataProcessor.getRootNode()
    }

}