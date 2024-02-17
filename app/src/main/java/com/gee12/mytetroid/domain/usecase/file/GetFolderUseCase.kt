package com.gee12.mytetroid.domain.usecase.file

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.DocumentFileType
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.model.FilePath

/**
 * Получение каталога с созданием, если нужно.
 */
class GetFolderUseCase(
    private val context: Context,
) : UseCase<DocumentFile, GetFolderUseCase.Params>() {

    data class Params(
        val path: String,
        val isCreateIfNotExist: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, DocumentFile> {
        val storageTrashFolderPath = FilePath.FolderFull(params.path)

        val folder = DocumentFileCompat.fromFullPath(
            context = context,
            fullPath = storageTrashFolderPath.fullPath,
            documentType = DocumentFileType.FOLDER,
            requiresWriteAccess = true,
        )

        return if (folder == null || !folder.exists()) {
            if (params.isCreateIfNotExist) {
                try {
                    DocumentFileCompat.mkdirs(
                        context = context,
                        fullPath = storageTrashFolderPath.fullPath,
                        requiresWriteAccess = true,
                    )?.toRight()
                        ?: Failure.Folder.Create(storageTrashFolderPath).toLeft()
                } catch (ex: Exception) {
                    Failure.Folder.Create(storageTrashFolderPath, ex).toLeft()
                }
            } else {
                Failure.Folder.NotExist(storageTrashFolderPath).toLeft()
            }
        } else {
            folder.toRight()
        }
    }

}