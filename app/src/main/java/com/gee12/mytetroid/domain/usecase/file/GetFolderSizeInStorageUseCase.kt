package com.gee12.mytetroid.domain.usecase.file

import android.content.Context
import android.text.format.Formatter
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FilePath

class GetFolderSizeInStorageUseCase(
    private val context: Context,
    private val storageProvider: IStorageProvider,
) : UseCase<String, GetFolderSizeInStorageUseCase.Params>() {

    data class Params(
        val folderRelativePath: String,
    )

    override suspend fun run(params: Params): Either<Failure, String> {
        val folderRelativePath = params.folderRelativePath
        val storageFolder = storageProvider.rootFolder
        val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
        val folderPath = FilePath.Folder(storageFolderPath, folderRelativePath)

        return try {
            val file = storageFolder?.child(
                context = context,
                path = folderRelativePath,
                requiresWriteAccess = false,
            ) ?: return Failure.File.Get(folderPath).toLeft()

            if (!file.exists()) {
                return Failure.File.NotExist(folderPath).toLeft()
            }
            val size = getFileSize(file)
            Formatter.formatFileSize(context, size).toRight()
        } catch (ex: SecurityException) {
            Failure.File.AccessDenied(folderPath, ex).toLeft()
        } catch (ex: Exception) {
            Failure.File.GetFileSize(folderPath, ex).toLeft()
        }
    }

    private fun getFileSize(file: DocumentFile): Long {
        if (!file.exists()) {
            return 0
        }
        var size: Long = 0
        if (!file.isDirectory) {
            size = file.length()
        } else {
            val folders = mutableListOf<DocumentFile>()
            folders.add(file)
            while (folders.isNotEmpty()) {
                val dir = folders.removeAt(0)
                if (!dir.exists()) {
                    continue
                }
                val listFiles = dir.listFiles()
                if (listFiles.isEmpty()) {
                    continue
                }
                for (child in listFiles) {
                    size += child.length()
                    if (child.isDirectory) folders.add(child)
                }
            }
        }
        return size
    }

}