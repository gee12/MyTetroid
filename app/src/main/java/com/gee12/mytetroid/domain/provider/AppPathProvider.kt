package com.gee12.mytetroid.domain.provider

import android.content.Context
import android.os.Environment
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.makeFolderPath
import com.gee12.mytetroid.model.FilePath

interface IAppPathProvider {
    fun getPathToExternalFilesFolder(): FilePath
    fun getPathToCacheFolder(): FilePath
    fun getPathToTrashFolder(): FilePath.Folder
    fun getPathToLogsFolder(): FilePath.Folder
    fun getPathToDownloadsFolder(): String
}

class AppPathProvider(
    private val context: Context,
) : IAppPathProvider {

    // каталог в приватной области памяти приложения
    // /Android/data/com.gee12.mytetroid/files
    // разрешения на чтение/запись не требуются
    override fun getPathToExternalFilesFolder(): FilePath {
        return FilePath.FolderFull(context.getExternalFilesDir(null)?.absolutePath.orEmpty())
    }

    override fun getPathToCacheFolder(): FilePath {
        return FilePath.FolderFull(getAppExternalCacheFolder())
    }

    override fun getPathToTrashFolder(): FilePath.Folder {
        return FilePath.Folder(getPathToExternalFilesFolder().fullPath, Constants.TRASH_DIR_NAME)
    }

    override fun getPathToLogsFolder(): FilePath.Folder {
        return FilePath.Folder(getPathToExternalFilesFolder().fullPath, Constants.LOG_DIR_NAME)
    }

    override fun getPathToDownloadsFolder(): String {
        // т.к. на API >= 30 автоматически каталог /Downloads/mytetroid создать нельзя,
        // то нет смысла его использовать
        //return makeFolderPath(getDownloadFolderPath(), Constants.DOWNLOADS_DIR_NAME)
        return getDownloadFolderPath()
    }

    // каталог кэш-файлов в приватной области памяти приложения
    // /Android/data/com.gee12.mytetroid/cache
    // разрешения на чтение/запись не требуются
    private fun getAppExternalCacheFolder(): String {
        return context.externalCacheDir?.absolutePath.orEmpty()
    }

    private fun getDownloadFolderPath(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.path.orEmpty()
    }

}