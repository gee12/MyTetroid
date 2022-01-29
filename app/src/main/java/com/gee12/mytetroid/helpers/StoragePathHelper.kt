package com.gee12.mytetroid.helpers

import android.content.Context
import android.net.Uri
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.utils.FileUtils

interface IStoragePathHelper {
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
    fun checkStorageFilesExistingError(context: Context): String?
}

class StoragePathHelper(
    private val storageProvider: IStorageProvider
) : IStoragePathHelper {

    companion object {
        const val FILE_URI_PREFIX = "file://"
    }

    override fun getStoragePath() = storageProvider.getStorageOrNull()?.path.orEmpty()

    override fun getPathToTrash() = storageProvider.getStorageOrNull()?.trashPath.orEmpty()

    override fun getPathToMyTetraXml(): String {
        return "${getStoragePath()}${Constants.SEPAR}${Constants.MYTETRA_XML_FILE_NAME}"
    }

    override fun getPathToStorageBaseFolder(): String {
        return "${getStoragePath()}${Constants.SEPAR}${Constants.BASE_DIR_NAME}"
    }

    override fun getUriToStorageBaseFolder(): Uri {
        return Uri.parse("${FILE_URI_PREFIX}${getPathToStorageBaseFolder()}")
    }

    override fun getPathToDatabaseIniConfig(): String {
        return "${getStoragePath()}${Constants.SEPAR}${Constants.DATABASE_INI_FILE_NAME}"
    }

    override fun getPathToIcons(): String {
        return "${getStoragePath()}${Constants.SEPAR}${Constants.ICONS_DIR_NAME}"
    }

    override fun getPathToFileInIconsFolder(fileName: String): String {
        return "${getPathToIcons()}${Constants.SEPAR}$fileName"
    }

    override fun getPathToStorageTrashFolder(): String {
        return getPathToTrash()
    }

    override fun getUriToStorageTrashFolder(): Uri {
        return Uri.parse("${FILE_URI_PREFIX}${getPathToTrash()}")
    }

    override fun checkStorageFilesExistingError(context: Context): String? {
        return when {
            !FileUtils.isFileExist(getStoragePath()) -> {
                context.getString(R.string.error_storage_folder_is_not_exists)
            }
            FileUtils.isDirEmpty(getStoragePath()) -> {
                context.getString(R.string.error_storage_folder_is_empty)
            }
            else -> {
                val errorList = mutableListOf<String>()
                if (!FileUtils.isFileExist(getPathToStorageBaseFolder())) {
                    errorList.add(context.getString(R.string.folder_name_mask, Constants.BASE_DIR_NAME))
                }
                if (!FileUtils.isFileExist(getPathToDatabaseIniConfig())) {
                    errorList.add(context.getString(R.string.file_name_mask, Constants.DATABASE_INI_FILE_NAME))
                }
                if (!FileUtils.isFileExist(getPathToMyTetraXml())) {
                    errorList.add(context.getString(R.string.file_name_mask, Constants.MYTETRA_XML_FILE_NAME))
                }
                when (errorList.size) {
                    1 -> {
                        context.getString(R.string.title_is_not_exist_mask, errorList.first())
                    }
                    2,3 -> {
                        context.getString(R.string.title_is_not_exist_plural_mask, errorList.joinToString())
                    }
                    else -> null
                }
            }
        }
    }

}