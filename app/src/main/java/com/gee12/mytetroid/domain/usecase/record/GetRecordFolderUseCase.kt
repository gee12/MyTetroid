package com.gee12.mytetroid.domain.usecase.record

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.orFalse
import com.gee12.mytetroid.domain.provider.IRecordPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidRecord

/**
 * Проверка существования каталога записи и его создание при необходимости.
 */
class GetRecordFolderUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val recordPathProvider: IRecordPathProvider,
    private val storageProvider: IStorageProvider,
) : UseCase<DocumentFile, GetRecordFolderUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val createIfNeed: Boolean,
        val inTrash: Boolean,
        val showMessage: Boolean = false,
    )

    override suspend fun run(params: Params): Either<Failure, DocumentFile> {
        return if (params.inTrash) {
            getInTrashFolder(params)
        } else {
            getInStorageFolder(params)
        }
    }

    private fun getInTrashFolder(params: Params): Either<Failure, DocumentFile> {
        val record = params.record
        val showMessage = params.showMessage
        // путь к каталогу записи в корзине хранилища
        val recordFolderPath = recordPathProvider.getPathToRecordFolderInTrash(record)
        val trashFolder = storageProvider.trashFolder

        return try {
            var recordFolderInTrash = trashFolder?.child(
                context = context,
                path = record.dirName,
                requiresWriteAccess = true,
            )

            if (recordFolderInTrash == null || !recordFolderInTrash.exists()) {
                if (params.createIfNeed) {
                    logger.logWarning( //
                        resourcesProvider.getString(R.string.log_create_record_folder_in_trash_mask, recordFolderPath.fullPath),
                        show = showMessage
                    )

                    recordFolderInTrash = trashFolder?.makeFolder(
                        context = context,
                        name = record.dirName,
                        mode = CreateMode.REUSE,
                    ) ?: return Failure.Folder.Create(recordFolderPath).toLeft()
                } else {
                    return Failure.Folder.NotExist(recordFolderPath).toLeft()
                }
            }
            recordFolderInTrash.toRight()
        } catch (ex: Exception) {
            Failure.Folder.Unknown(recordFolderPath, ex).toLeft()
        }
    }

    private fun getInStorageFolder(params: Params): Either<Failure, DocumentFile> {
        val storage = storageProvider.storage
        val record = params.record
        val baseFolder = storageProvider.baseFolder
        val baseFolderPath = baseFolder?.getAbsolutePath(context).orEmpty()
        val showMessage = params.showMessage
        // путь к каталогу записи в каталоге хранилища
        val recordFolderPath = FilePath.Folder(baseFolderPath, record.dirName)

        return try {
            var folder = baseFolder?.child(
                context = context,
                path = recordFolderPath.folderName,
                requiresWriteAccess = !storage?.isReadOnly.orFalse(),
            )

            if (folder == null || !folder.exists()) {
                if (params.createIfNeed) {
                    logger.logDebug(
                        resourcesProvider.getString(R.string.log_create_record_folder_mask, recordFolderPath.fullPath),
                        show = showMessage
                    )

                    folder = baseFolder?.makeFolder(
                        context = context,
                        name = recordFolderPath.folderName,
                        mode = CreateMode.REUSE,
                    ) ?: return Failure.Folder.Create(recordFolderPath).toLeft()
                } else {
                    return Failure.Folder.NotExist(recordFolderPath).toLeft()
                }
            }
            folder.toRight()
        } catch (ex: Exception) {
            Failure.Folder.Unknown(recordFolderPath, ex).toLeft()
        }
    }

}