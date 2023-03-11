package com.gee12.mytetroid.domain.usecase.file

import android.content.Context
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.getFormattedSize
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
            file.getFormattedSize(context).toRight()
        } catch (ex: SecurityException) {
            Failure.File.AccessDenied(folderPath, ex).toLeft()
        } catch (ex: Exception) {
            Failure.File.GetFileSize(folderPath, ex).toLeft()
        }
    }

}