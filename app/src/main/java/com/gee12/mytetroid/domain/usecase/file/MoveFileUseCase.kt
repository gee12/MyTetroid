package com.gee12.mytetroid.domain.usecase.file

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getStringFromTo
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import java.io.File

class MoveFileUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
) : UseCase<UseCase.None, MoveFileUseCase.Params>() {

    data class Params(
        val srcFullFileName: String,
        val destPath: String,
        val newFileName: String?,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val srcFullFileName = params.srcFullFileName
        val destPath = params.destPath
        val newFileName = params.newFileName

        var srcFile = File(srcFullFileName)
        val srcFileName = srcFile.name
        val destDir = File(destPath)

        // перемещаем файл или каталог
        val moveToDirRecursive = FileUtils.moveToDirRecursive(srcFile, destDir)
        if (!moveToDirRecursive) {
            val fromTo = resourcesProvider.getStringFromTo(srcFullFileName, destPath)
//            logger.log(String.format(context.getString(R.string.log_error_move_file_mask),
//                    srcFullFileName, destPath), LogManager.Types.ERROR);
            logger.logOperError(LogObj.FILE, LogOper.REORDER, fromTo, false, false)
            return Failure.File.MoveToFolder(filePath = srcFullFileName, folderPath = destPath).toLeft()
        }
        if (newFileName == null) {
            val destDirPath = makePath(destDir.absolutePath, srcFileName)
            val to = resourcesProvider.getString(R.string.log_to_mask, destDirPath)
//            logger.log(String.format(context.getString(R.string.log_file_moved_mask),
//                    destDirPath), LogManager.Types.DEBUG);
            logger.logOperRes(LogObj.FILE, LogOper.REORDER, to, false)
        } else {
            // добавляем к имени каталога записи уникальную приставку
            srcFile = File(destPath, srcFileName)
            val destFile = File(destPath, newFileName)
            if (srcFile.renameTo(destFile)) {
                val to = resourcesProvider.getString(R.string.log_to_mask, destFile.absolutePath)
//                logger.log(String.format(context.getString(R.string.log_file_moved_mask),
//                        destFile.getAbsolutePath()), LogManager.Types.DEBUG);
                logger.logOperRes(LogObj.FILE, LogOper.REORDER, to, false)
            } else {
                val fromTo = resourcesProvider.getStringFromTo(srcFile.absolutePath, destFile.absolutePath)
//                logger.log(String.format(context.getString(R.string.log_error_move_file_mask),
//                        srcFile.getAbsolutePath(), destFile.getAbsolutePath()), LogManager.Types.ERROR);
                logger.logOperError(LogObj.FILE, LogOper.REORDER, fromTo, false, false)
                return Failure.File.RenameTo(filePath = srcFile.path, newName = destFile.path).toLeft()
            }
        }
        return None.toRight()
    }

}