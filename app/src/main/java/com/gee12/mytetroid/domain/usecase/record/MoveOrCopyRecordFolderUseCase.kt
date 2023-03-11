package com.gee12.mytetroid.domain.usecase.record

import androidx.documentfile.provider.DocumentFile
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.domain.usecase.attach.RenameRecordAttachesUseCase
import com.gee12.mytetroid.domain.usecase.file.CopyFileOrFolderUseCase
import com.gee12.mytetroid.domain.usecase.file.MoveFileOrFolderUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidRecord

/**
 * Перемещение или копирование файлов записи.
 */
class MoveOrCopyRecordFolderUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val storageProvider: IStorageProvider,
    private val storagePathProvider: IStoragePathProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,
    private val renameRecordAttachesUseCase: RenameRecordAttachesUseCase,
    private val moveFileOrFolderUseCase: MoveFileOrFolderUseCase,
    private val copyFileOrFolderUseCase: CopyFileOrFolderUseCase,
) : UseCase<UseCase.None, MoveOrCopyRecordFolderUseCase.Params>() {

    data class Params(
        val srcRecord: TetroidRecord,
        val destRecord: TetroidRecord,
        val isCutting: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val srcRecord = params.srcRecord
        val destRecord = params.destRecord
        val isCutting = params.isCutting

        return getRecordFolderUseCase.run(
            GetRecordFolderUseCase.Params(
                record = srcRecord,
                createIfNeed = false,
                inTrash = isCutting,
            )
        ).flatMap { srcFolder ->
            if (isCutting) {
                moveRecordFolder(
                    srcRecord = srcRecord,
                    destRecord = destRecord,
                    srcFolder = srcFolder,
                )
            } else {
                copyRecordFolder(
                    srcRecord = srcRecord,
                    destRecord = destRecord,
                    srcFolder = srcFolder,
                )
            }
        }
    }

    private suspend fun moveRecordFolder(
        srcRecord: TetroidRecord,
        destRecord: TetroidRecord,
        srcFolder: DocumentFile,
    ): Either<Failure, None> {
        val baseFolder = storageProvider.baseFolder
            ?: return Failure.Folder.Get(storagePathProvider.getPathToBaseFolder()).toLeft()

        // вырезаем уникальную приставку в имени каталога
        val folderNameWithoutPrefix = srcRecord.dirName.substring(DataNameProvider.PREFIX_DATE_TIME_FORMAT.length + 1)

        return moveFileOrFolderUseCase.run(
            MoveFileOrFolderUseCase.Params(
                srcFileOrFolder = srcFolder,
                destFolder = baseFolder,
                newName = folderNameWithoutPrefix,
            )
        ).flatMap {
            // обновляем имя каталога для дальнейшей вставки
            destRecord.dirName = folderNameWithoutPrefix
            None.toRight()
        }
    }

    private suspend fun copyRecordFolder(
        srcRecord: TetroidRecord,
        destRecord: TetroidRecord,
        srcFolder: DocumentFile,
    ): Either<Failure, None> {
        val baseFolder = storageProvider.baseFolder
            ?: return Failure.Folder.Get(storagePathProvider.getPathToBaseFolder()).toLeft()

        val destFolderPath = recordPathProvider.getPathToRecordFolder(destRecord)

        return copyFileOrFolderUseCase.run(
            CopyFileOrFolderUseCase.Params(
                srcFileOrFolder = srcFolder,
                destFolder = baseFolder,
                newName = destRecord.dirName,
            )
        ).flatMap {
            logger.logDebug(resourcesProvider.getString(R.string.log_copy_record_folder_mask, destFolderPath.fullPath))
            // переименовываем прикрепленные файлы
            renameRecordAttachesUseCase.run(
                RenameRecordAttachesUseCase.Params(
                    srcRecord = srcRecord,
                    destRecord = destRecord,
                )
            )
        }
    }

}