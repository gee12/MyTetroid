package com.gee12.mytetroid.domain.provider

import android.content.Context
import android.net.Uri
import com.anggrayudi.storage.file.getAbsolutePath
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.extensions.uriToAbsolutePathIfPossible
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidStorage

interface IStoragePathProvider {
    fun getPathToRootFolder(): FilePath
    fun getPathToMyTetraXml(): String
    fun getPathToBaseFolder(): FilePath
    fun getRelativePathToBaseFolder(): String
    fun getUriToBaseFolder(): Uri
    fun getPathToDatabaseIniConfig(): String
    fun getPathToIconsFolder(): String
    fun getRelativePathToIconsFolder(): String
    fun getPathToFileInIconsFolder(fileName: String): String
    fun getPathToStorageTrashFolder(): FilePath
    fun getUriToStorageTrashFolder(): Uri
}

class StoragePathProvider(
    private val context: Context,
    // TODO: ?
    private val storageProvider: IStorageProvider?,
    private val storage: TetroidStorage? = null,
    private val appPathProvider: IAppPathProvider,
) : IStoragePathProvider {

    companion object {
        // TODO: убрать
        const val FILE_URI_PREFIX = "file://"
    }

    override fun getPathToRootFolder(): FilePath {
        val path = storage?.uri?.uriToAbsolutePathIfPossible(context)
            ?: storageProvider?.rootFolder?.getAbsolutePath(context).orEmpty()
        return FilePath.FolderFull(path)
    }

    override fun getPathToMyTetraXml(): String {
        return makePath(getPathToRootFolder().fullPath, Constants.MYTETRA_XML_FILE_NAME)
    }

    override fun getPathToBaseFolder(): FilePath {
        return FilePath.Folder(getPathToRootFolder().fullPath, Constants.BASE_DIR_NAME)
    }

    override fun getRelativePathToBaseFolder(): String {
        return Constants.BASE_DIR_NAME
    }

    override fun getUriToBaseFolder(): Uri {
        return Uri.parse("$FILE_URI_PREFIX${getPathToBaseFolder()}")
    }

    override fun getPathToDatabaseIniConfig(): String {
        return makePath(getPathToRootFolder().fullPath, Constants.DATABASE_INI_FILE_NAME)
    }

    override fun getPathToIconsFolder(): String {
        return makePath(getPathToRootFolder().fullPath, Constants.ICONS_DIR_NAME)
    }

    override fun getRelativePathToIconsFolder(): String {
        return Constants.ICONS_DIR_NAME
    }

    override fun getPathToFileInIconsFolder(fileName: String): String {
        return makePath(getPathToIconsFolder(), fileName)
    }

    override fun getPathToStorageTrashFolder(): FilePath {
        val trashFolderPath = appPathProvider.getPathToTrashFolder()
        return FilePath.Folder(trashFolderPath, getStorageId().toString())
    }

    override fun getUriToStorageTrashFolder(): Uri {
        return Uri.parse("$FILE_URI_PREFIX${getPathToStorageTrashFolder().fullPath}")
    }

    private fun getStorageId(): Int {
        return storage?.id ?: storageProvider?.storage?.id ?: 0
    }

}