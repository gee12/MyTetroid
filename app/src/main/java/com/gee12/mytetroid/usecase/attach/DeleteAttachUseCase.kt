package com.gee12.mytetroid.usecase.attach

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.helpers.IRecordPathProvider
import com.gee12.mytetroid.helpers.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.usecase.record.CheckRecordFolderUseCase
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import java.io.File

/**
 * Удаление прикрепленного файла.
 * @param withoutFile не пытаться удалить сам файл на диске
 */
class DeleteAttachUseCase(
//    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val recordPathProvider: IRecordPathProvider,
    private val checkRecordFolderUseCase: CheckRecordFolderUseCase,
    private val saveStorageUseCase: SaveStorageUseCase,
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
        // TODO: уйдет когда объекты будут на Kotlin
//        if (record == null) {
//            logger.logError(resourcesProvider.getString(R.string.log_file_record_is_null))
//            return 0
//        }
        val folderPath: String
        var destFilePath: String? = null
        var destFile: File? = null
        if (!withoutFile) {
            // проверяем существование каталога записи
            folderPath = recordPathProvider.getPathToRecordFolder(record)
            checkRecordFolderUseCase.run(
                CheckRecordFolderUseCase.Params(
                    folderPath = folderPath,
                    isCreate = false,
                )
            ).onFailure {
                return it.toLeft()
            }
            // проверяем существование самого файла
            val ext = FileUtils.getExtensionWithComma(attach.name)
            val fileIdName = attach.id + ext
            destFilePath = makePath(folderPath, fileIdName)
            destFile = File(destFilePath)
            if (!destFile.exists()) {
//                logger.logError(resourcesProvider.getString(R.string.error_file_is_missing_mask) + destFilePath)
                return Failure.File.NotExist(path = destFile.path).toLeft()
            }
        }

        // удаляем файл из списка файлов записи (и соответственно, из дерева)
        val recordAttaches = record.attachedFiles
        // TODO: уйдет когда объекты будут на Kotlin
//        if (recordAttaches != null) {
            if (!recordAttaches.remove(attach)) {
//                logger.logError(resourcesProvider.getString(R.string.error_attach_not_found_in_record))
                return Failure.Attach.NotFoundInRecord(attachId = attach.id).toLeft()
            }
//        } else {
//            logger.logError(resourcesProvider.getString(R.string.log_record_not_have_attached_files))
//            return 0
//        }

        // перезаписываем структуру хранилища в файл
        return saveStorageUseCase.run()
            .flatMap {
                // удаляем сам файл
                if (!withoutFile && destFile != null && destFilePath != null) {
                    if (!FileUtils.deleteRecursive(destFile)) {
                        return Failure.File.Delete(filePath = destFile.path).toLeft()
                    }
                }
                None.toRight()
            }.onFailure {
                logger.logOperCancel(LogObj.FILE, LogOper.DELETE)
            }
    }

}