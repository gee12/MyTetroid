package com.gee12.mytetroid.domain.provider

import android.content.Context
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
    fun getPathToDatabaseIniConfig(): String
    fun getPathToIconsFolder(): String
    fun getPathToScriptsFolder(): String
    fun getRelativePathToScript(scriptFileName: String): String
    fun getRelativePathToIconsFolder(): String
    fun getPathToStorageTrashFolder(): FilePath
}

class StoragePathProvider(
    private val context: Context,
    // TODO: ?
    private val storageProvider: IStorageProvider?,
    private val storage: TetroidStorage? = null,
    private val appPathProvider: IAppPathProvider,
) : IStoragePathProvider {

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

    override fun getPathToDatabaseIniConfig(): String {
        return makePath(getPathToRootFolder().fullPath, Constants.DATABASE_INI_FILE_NAME)
    }

    override fun getPathToIconsFolder(): String {
        return makePath(getPathToRootFolder().fullPath, Constants.ICONS_DIR_NAME)
    }

    override fun getPathToScriptsFolder(): String {
        return makePath(getPathToRootFolder().fullPath, Constants.SCRIPTS_DIR_NAME)
    }

    override fun getRelativePathToScript(scriptFileName: String): String {
        return makePath(Constants.SCRIPTS_DIR_NAME, scriptFileName)
    }

    override fun getRelativePathToIconsFolder(): String {
        return Constants.ICONS_DIR_NAME
    }

    override fun getPathToStorageTrashFolder(): FilePath {
        val trashFolderPath = appPathProvider.getPathToTrashFolder().fullPath
        return FilePath.Folder(trashFolderPath, getStorageId().toString())
    }

    private fun getStorageId(): Int {
        return storage?.id ?: storageProvider?.storage?.id ?: 0
    }

}