package com.gee12.mytetroid.domain.usecase.file

import android.content.Context
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FilePath
import java.util.*

class GetFileModifiedDateInStorageUseCase(
    private val context: Context,
    private val storageProvider: IStorageProvider,
) : UseCase<Date, GetFileModifiedDateInStorageUseCase.Params>() {

    data class Params(
        val fileRelativePath: String,
    )

    override suspend fun run(params: Params): Either<Failure, Date> {
        val fileRelativePath = params.fileRelativePath
        val storageFolder = storageProvider.rootFolder
        val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
        val filePath = FilePath.File(storageFolderPath, fileRelativePath)

        return try {
            val file = storageFolder?.child(
                context = context,
                path = fileRelativePath,
                requiresWriteAccess = false,
            ) ?: return Failure.File.Get(filePath).toLeft()

            if (!file.exists()) {
                return Failure.File.NotExist(filePath).toLeft()
            }
            Date(file.lastModified()).toRight()
        } catch (ex: SecurityException) {
            Failure.File.AccessDenied(filePath, ex).toLeft()
        } catch (ex: Exception) {
            Failure.File.GetFileSize(filePath, ex).toLeft()
        }
    }

}