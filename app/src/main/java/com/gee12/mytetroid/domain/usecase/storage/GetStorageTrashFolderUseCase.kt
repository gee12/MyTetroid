package com.gee12.mytetroid.domain.usecase.storage

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.DocumentFileType
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidStorage

/**
 * Получение каталога корзины хранилище с созданием, если нужно.
 */
class GetStorageTrashFolderUseCase(
    private val context: Context,
    private val appPathProvider: IAppPathProvider,
) : UseCase<DocumentFile, GetStorageTrashFolderUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
        val createIfNotExist: Boolean = true,
    )

    override suspend fun run(params: Params): Either<Failure, DocumentFile> {
        val storage = params.storage
        val trashFolderPath = appPathProvider.getPathToTrashFolder()
        val storageTrashFolderPath = FilePath.Folder(trashFolderPath, storage.id.toString())

        var storageTrashFolder = DocumentFileCompat.fromFullPath(
            context = context,
            fullPath = storageTrashFolderPath.fullPath,
            documentType = DocumentFileType.FOLDER,
            requiresWriteAccess = true,
        )

        if (storageTrashFolder == null || !storageTrashFolder.exists()) {
            if (params.createIfNotExist) {
                try {
                    storageTrashFolder = DocumentFileCompat.mkdirs(
                        context = context,
                        fullPath = storageTrashFolderPath.fullPath,
                        requiresWriteAccess = true,
                    ) ?: return Failure.Folder.Create(storageTrashFolderPath).toLeft()
                } catch (ex: Exception) {
                    return Failure.Folder.Create(storageTrashFolderPath, ex).toLeft()
                }
            } else {
                return Failure.Folder.NotExist(storageTrashFolderPath).toLeft()
            }
        }

        return storageTrashFolder.toRight()
    }

}