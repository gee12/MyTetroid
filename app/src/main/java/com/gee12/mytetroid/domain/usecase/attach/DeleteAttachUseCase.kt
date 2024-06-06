package com.gee12.mytetroid.domain.usecase.attach

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getExtensionWithoutComma
import com.gee12.mytetroid.common.extensions.withExtension
import com.gee12.mytetroid.domain.usecase.record.GetRecordFolderUseCase
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageTreeUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.obj.TetroidFile

/**
 * Удаление прикрепленного файла.
 * @param withoutFile не пытаться удалить сам файл на диске
 */
class DeleteAttachUseCase(
    private val context: Context,
    private val logger: ITetroidLogger,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,
    private val saveStorageTreeUseCase: SaveStorageTreeUseCase,
) : UseCase<UseCase.None, DeleteAttachUseCase.Params>() {

    data class Params(
        val attach: TetroidFile,
        val withoutFile: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val attach = params.attach
        val withoutFile = params.withoutFile

        logger.logOperStart(LogObj.FILE, LogOper.DELETE, attach)
        val record = attach.record

        var destFilePath: FilePath? = null
        var destFile: DocumentFile? = null
        if (!withoutFile) {
            val recordFolder = getRecordFolderUseCase.run(
                GetRecordFolderUseCase.Params(
                    record = record,
                    createIfNeed = false,
                    inTrash = false,
                )
            ).foldResult(
                onLeft = {
                    return it.toLeft()
                },
                onRight = { it }
            )
            val folderPath = recordFolder.getAbsolutePath(context)

            // проверяем существование самого файла
            val ext = attach.name.getExtensionWithoutComma()
            val fileIdName = attach.id.withExtension(ext)
            destFilePath = FilePath.File(folderPath, fileIdName)

            destFile = recordFolder.child(
                context = context,
                path = fileIdName,
                requiresWriteAccess = true,
            )
            if (destFile == null || !destFile.exists()) {
                return Failure.File.NotExist(destFilePath).toLeft()
            }
        }

        // удаляем файл из списка файлов записи (и соответственно, из дерева)
        val recordAttaches = record.attachedFiles
        if (!recordAttaches.remove(attach)) {
            return Failure.Attach.NotFoundInRecord(attachId = attach.id).toLeft()
        }

        // перезаписываем структуру хранилища в файл
        return saveStorageTreeUseCase.run()
            .flatMap {
                // удаляем сам файл
                if (!withoutFile && destFile != null && destFilePath != null) {
                    if (!destFile.delete()) {
                        return Failure.File.Delete(destFilePath).toLeft()
                    }
                }
                None.toRight()
            }.onFailure {
                logger.logOperCancel(LogObj.FILE, LogOper.DELETE)
            }
    }

}