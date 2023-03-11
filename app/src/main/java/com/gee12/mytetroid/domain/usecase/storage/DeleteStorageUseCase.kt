package com.gee12.mytetroid.domain.usecase.storage

import android.content.Context
import android.net.Uri
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.DocumentFileType
import com.anggrayudi.storage.file.deleteRecursively
import com.anggrayudi.storage.file.getAbsolutePath
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.repo.StoragesRepo
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidStorage

class DeleteStorageUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val appPathProvider: IAppPathProvider,
    private val storagesRepo: StoragesRepo,
) : UseCase<UseCase.None, DeleteStorageUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
        val withFiles: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val storage = params.storage

        return deleteFromDb(storage).flatMap {
            deleteTemporaryFolder(storage)
        }.flatMap {
            deleteStorageFolder(storage)
        }
    }

    private suspend fun deleteFromDb(storage: TetroidStorage): Either<Failure, None> {
        return if (storagesRepo.deleteStorage(storage)) {
            None.toRight()
        } else {
            Failure.Storage.Delete.FromDb.toLeft()
        }
    }

    private fun deleteTemporaryFolder(storage: TetroidStorage): Either<Failure, None> {
        val trashFolderPath = appPathProvider.getPathToTrashFolder()
        val storageTrashFolderPath = FilePath.Folder(trashFolderPath, storage.id.toString())

        val storageTemporaryFolder = DocumentFileCompat.fromFullPath(
            context = context,
            fullPath = storageTrashFolderPath.fullPath,
            documentType = DocumentFileType.FOLDER,
            requiresWriteAccess = true,
        ) ?: return None.toRight()

        return if (storageTemporaryFolder.deleteRecursively(context, childrenOnly = false)) {
            None.toRight()
        } else {
            Failure.Folder.Delete(storageTrashFolderPath).toLeft()
        }
    }

    private fun deleteStorageFolder(storage: TetroidStorage): Either<Failure, None> {
        val storageFolderUri = Uri.parse(storage.uri)
        var storageFolderPath = FilePath.FolderFull(storageFolderUri.path.orEmpty())

        return try {
            val storageFolder = DocumentFileCompat.fromUri(context, storageFolderUri)

            if (storageFolder == null || !storageFolder.exists()) {
                return None.toRight()
            }

            storageFolderPath = FilePath.FolderFull(storageFolder.getAbsolutePath(context))

            logger.logDebug(resourcesProvider.getString(R.string.log_start_storage_deleting_mask, storageFolderPath.fullPath))

            if (storageFolder.deleteRecursively(context, childrenOnly = false)) {
                None.toRight()
            } else {
                Failure.Folder.Delete(storageFolderPath).toLeft()
            }
        } catch (ex: Exception) {
            Failure.Folder.Delete(storageFolderPath, ex).toLeft()
        }
    }

}