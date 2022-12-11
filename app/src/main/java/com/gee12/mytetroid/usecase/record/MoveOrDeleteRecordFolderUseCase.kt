package com.gee12.mytetroid.usecase.record

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.providers.IDataNameProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.usecase.file.MoveFileUseCase
import java.io.File

class MoveOrDeleteRecordFolderUseCase(
    private val logger: ITetroidLogger,
    private val moveFileUseCase: MoveFileUseCase,
    private val dataNameProvider: IDataNameProvider,
) : UseCase<UseCase.None, MoveOrDeleteRecordFolderUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val folderPath: String,
        val movePath: String?,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val record = params.record
        val folderPath = params.folderPath
        val movePath = params.movePath

        return if (movePath == null) {
            // удаляем каталог записи
            val dir = File(folderPath)
            if (FileUtils.deleteRecursive(dir)) {
                logger.logOperRes(LogObj.RECORD_DIR, LogOper.DELETE)
                None.toRight()
            } else {
                logger.logOperError(LogObj.RECORD_DIR, LogOper.DELETE,": $folderPath", false, false)
                Failure.Folder.Delete(path = folderPath).toLeft()
            }
        } else {
            moveRecordFolder(
                record = record,
                srcPath = folderPath,
                destPath = movePath,
            )
        }
    }

    // перемещаем каталог записи в корзину
    // с добавлением префикса в виде текущей даты и времени
    private suspend fun moveRecordFolder(
        record: TetroidRecord,
        srcPath: String,
        destPath: String,
    ): Either<Failure, None> {
        val newDirName = dataNameProvider.createDateTimePrefix()

        return moveFileUseCase.run(
            MoveFileUseCase.Params(
                srcFullFileName = srcPath,
                destPath = destPath,
                newFileName = newDirName
            )
        ).map {
            // обновляем имя каталога для дальнейшей вставки
            record.dirName = newDirName
            None
        }
    }

}