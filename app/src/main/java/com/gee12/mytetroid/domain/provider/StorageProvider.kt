package com.gee12.mytetroid.domain.provider

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.DocumentFileType
import com.anggrayudi.storage.file.child
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.*
import java.util.HashMap

interface IStorageProvider {

    val storage: TetroidStorage?
    val rootFolder: DocumentFile?
    val baseFolder: DocumentFile?
    val trashFolder: DocumentFile?
    val databaseConfig: DatabaseConfig
    val dataProcessor: IStorageDataProcessor
    val favorites: MutableList<TetroidFavorite>

    fun init(storageDataProcessor: IStorageDataProcessor)
    fun setStorage(storage: TetroidStorage)
    fun setRootFolder(root: DocumentFile)
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
    private val context: Context,
    private val logger: ITetroidLogger,
    private val appPathProvider: IAppPathProvider,
) : IStorageProvider {

    override var storage: TetroidStorage? = null
        private set
    override var rootFolder: DocumentFile? = null
        private set
    override var baseFolder: DocumentFile? = null
    override var trashFolder: DocumentFile? = null
    override val databaseConfig = DatabaseConfig(logger)
    override lateinit var dataProcessor: IStorageDataProcessor
        private set
    override val favorites: MutableList<TetroidFavorite> = mutableListOf()


    // TODO: по хорошему, вызывать только из InitStorageUseCase
    override fun init(storageDataProcessor: IStorageDataProcessor) {
        this.dataProcessor = storageDataProcessor
    }

    override fun setStorage(storage: TetroidStorage) {
        this.storage = storage
    }

    override fun setRootFolder(root: DocumentFile) {
        this.rootFolder = root
        this.baseFolder = getBaseFolderDirectly()
        this.trashFolder = getTrashFolderDirectly()
    }

    override fun resetStorage() {
        storage = null
        rootFolder = null
        baseFolder = null
        trashFolder = null
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

    private fun getBaseFolderDirectly(): DocumentFile? {
        return rootFolder?.child(
            context = context,
            path = Constants.BASE_DIR_NAME,
            requiresWriteAccess = true,
        )
    }

    private fun getPathToTrashFolder(): FilePath {
        val trashFolderPath = appPathProvider.getPathToTrashFolder()
        return FilePath.Folder(trashFolderPath, getStorageId().toString())
    }

    private fun getTrashFolderDirectly(requiresWriteAccess: Boolean = true): DocumentFile? {
        return DocumentFileCompat.fromFullPath(
            context = context,
            fullPath = getPathToTrashFolder().fullPath,
            documentType = DocumentFileType.FOLDER,
            requiresWriteAccess = requiresWriteAccess,
        )
    }

    private fun getStorageId(): Int {
        return storage?.id ?: 0
    }

}