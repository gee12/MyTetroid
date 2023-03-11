package com.gee12.mytetroid.domain.provider

import android.content.Context
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.makeFolderPath
import com.gee12.mytetroid.model.FilePath

interface IAppPathProvider {
    fun getPathToCacheFolder(): FilePath
    fun getPathToTrashFolder(): String
    fun getPathToLogsFolder(): String
}

class AppPathProvider(
    private val context: Context,
) : IAppPathProvider {

    override fun getPathToCacheFolder(): FilePath {
        return FilePath.FolderFull(getAppExternalCacheFolder())
    }

    override fun getPathToTrashFolder(): String {
        return makeFolderPath(getAppExternalFilesFolder(), Constants.TRASH_DIR_NAME)
    }

    override fun getPathToLogsFolder(): String {
        return makeFolderPath(getAppExternalFilesFolder(), Constants.LOG_DIR_NAME)
    }

    // каталог корзины в приватной области памяти приложения
    // /Android/data/com.gee12.mytetroid/files/trash
    // разрешения на чтение/запись не требуются
    private fun getAppExternalFilesFolder(): String {
        return context.getExternalFilesDir(null)?.absolutePath.orEmpty()
    }

    // каталог кэш-файлов в приватной области памяти приложения
    // /Android/data/com.gee12.mytetroid/cache
    // разрешения на чтение/запись не требуются
    private fun getAppExternalCacheFolder(): String {
        return context.externalCacheDir?.absolutePath.orEmpty()
    }

}