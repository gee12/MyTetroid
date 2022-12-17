package com.gee12.mytetroid.providers

import android.net.Uri
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.model.TetroidStorage

interface IStoragePathProvider {
    fun getStoragePath(): String
    fun getPathToTrash(): String
    fun getPathToMyTetraXml(): String
    fun getPathToStorageBaseFolder(): String
    fun getUriToStorageBaseFolder(): Uri
    fun getPathToDatabaseIniConfig(): String
    fun getPathToIcons(): String
    fun getPathToFileInIconsFolder(fileName: String): String
    fun getPathToStorageTrashFolder(): String
    fun getUriToStorageTrashFolder(): Uri
}

class StoragePathProvider(
    // TODO: ?
    private val storageProvider: IStorageProvider?,
    private val storage: TetroidStorage? = null,
//    private val storage: TetroidStorage,
) : IStoragePathProvider {

    companion object {
        const val FILE_URI_PREFIX = "file://"
    }

    override fun getStoragePath() = storage?.path ?: storageProvider?.storage?.path.orEmpty()

    override fun getPathToTrash() = storage?.trashPath ?: storageProvider?.storage?.trashPath.orEmpty()

//    override fun getStoragePath() = storage.path

//    override fun getPathToTrash() = storage.trashPath.orEmpty()

    override fun getPathToMyTetraXml(): String {
        return makePath(getStoragePath(), Constants.MYTETRA_XML_FILE_NAME)
    }

    override fun getPathToStorageBaseFolder(): String {
        return makePath(getStoragePath(), Constants.BASE_DIR_NAME)
    }

    override fun getUriToStorageBaseFolder(): Uri {
        return Uri.parse("$FILE_URI_PREFIX${getPathToStorageBaseFolder()}")
    }

    override fun getPathToDatabaseIniConfig(): String {
        return makePath(getStoragePath(), Constants.DATABASE_INI_FILE_NAME)
    }

    override fun getPathToIcons(): String {
        return makePath(getStoragePath(), Constants.ICONS_DIR_NAME)
    }

    override fun getPathToFileInIconsFolder(fileName: String): String {
        return makePath(getPathToIcons(), fileName)
    }

    override fun getPathToStorageTrashFolder(): String {
        return getPathToTrash()
    }

    override fun getUriToStorageTrashFolder(): Uri {
        return Uri.parse("$FILE_URI_PREFIX${getPathToTrash()}")
    }

}