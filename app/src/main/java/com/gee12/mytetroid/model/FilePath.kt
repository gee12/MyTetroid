package com.gee12.mytetroid.model

import com.gee12.mytetroid.common.extensions.makeFolderPath
import com.gee12.mytetroid.common.extensions.makePath


sealed class FilePath {

    abstract val fullPath: String

    data class File(
        val path: String,
        val fileName: String,
    ) : FilePath() {
        override val fullPath = makePath(path, fileName)
    }

    data class FileFull(
        override val fullPath: String,
    ) : FilePath()

    data class Folder(
        val path: String,
        val folderName: String,
    ) : FilePath() {
        override val fullPath = makeFolderPath(path, folderName)
    }

    data class FolderFull(
        override val fullPath: String,
    ) : FilePath()

}
