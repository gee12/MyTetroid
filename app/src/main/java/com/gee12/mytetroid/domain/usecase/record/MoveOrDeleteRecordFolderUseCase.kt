package com.gee12.mytetroid.domain.usecase.record

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.deleteRecursively
import com.anggrayudi.storage.file.getAbsolutePath
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.usecase.file.MoveFileOrFolderUseCase
import com.gee12.mytetroid.domain.provider.IDataNameProvider
import com.gee12.mytetroid.domain.provider.IStoragePathProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidRecord

class MoveOrDeleteRecordFolderUseCase(
    private val context: Context,
    private val logger: ITetroidLogger,
    private val storageProvider: IStorageProvider,
    private val storagePathProvider: IStoragePathProvider,
    private val moveFileUseCase: MoveFileOrFolderUseCase,
    private val dataNameProvider: IDataNameProvider,
) : UseCase<UseCase.None, MoveOrDeleteRecordFolderUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val recordFolder: DocumentFile,
        val isMoveToTrash: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        return if (!params.isMoveToTrash) {
            // удаляем каталог записи
            deleteRecordFolder(params)
        } else {
            moveRecordFolderToTrash(params)
        }
    }

    private fun deleteRecordFolder(params: Params): Either<Failure.Folder.Delete, None> {
        val recordFolder = params.recordFolder
        val recordFolderPath = FilePath.FolderFull(recordFolder.getAbsolutePath(context))

        return if (recordFolder.deleteRecursively(context, childrenOnly = false)) {
            logger.logOperRes(LogObj.RECORD_DIR, LogOper.DELETE)
            None.toRight()
        } else {
            logger.logOperError(LogObj.RECORD_DIR, LogOper.DELETE, ": $recordFolderPath", false, show = false)
            Failure.Folder.Delete(recordFolderPath).toLeft()
        }
    }

    // перемещаем каталог записи в корзину
    // с добавлением префикса в виде текущей даты и времени
    private suspend fun moveRecordFolderToTrash(params: Params): Either<Failure, None> {
        val recordFolder = params.recordFolder
        val folderNameInTrash = "${dataNameProvider.createDateTimePrefix()}_${params.record.dirName}"
        val trashFolderPath = storagePathProvider.getPathToStorageTrashFolder()

        val trashFolder = storageProvider.trashFolder
            ?: return Failure.Folder.Get(trashFolderPath).toLeft()

        return moveFileUseCase.run(
            MoveFileOrFolderUseCase.Params(
                srcFileOrFolder = recordFolder,
                destFolder = trashFolder,
                newName = folderNameInTrash,
            )
        ).map {
            // обновляем имя каталога для дальнейшей вставки
            params.record.dirName = folderNameInTrash
            None
        }
    }

}