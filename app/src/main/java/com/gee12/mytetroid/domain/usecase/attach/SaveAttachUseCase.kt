package com.gee12.mytetroid.domain.usecase.attach

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getIdString
import com.gee12.mytetroid.common.extensions.getStringTo
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.IRecordPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidFile
import java.io.File
import java.io.IOException

/**
 * Сохранение прикрепленного файла по указанному пути.
 */
class SaveAttachUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val recordPathProvider: IRecordPathProvider,
    private val cryptManager: IStorageCryptManager,
) : UseCase<UseCase.None, SaveAttachUseCase.Params>() {

    data class Params(
        val attach: TetroidFile,
        val destPath: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val attach = params.attach
        val destPath = params.destPath

        val mes = attach.getIdString(resourcesProvider) + resourcesProvider.getStringTo(destPath)
        logger.logOperStart(LogObj.FILE, LogOper.SAVE, ": $mes")

        // проверка исходного файла
        val fileIdName = attach.idName
        val recordPath: String = recordPathProvider.getPathToRecordFolder(attach.record)
        val srcFile = File(recordPath, fileIdName)
        if (!srcFile.exists()) {
//                logger.logError(resourcesProvider.getString(R.string.log_file_is_absent) + fileIdName)
            return Failure.File.NotExist(path = srcFile.path).toLeft()
        }
        // копирование файла в указанный каталог, расшифровуя при необходимости
        val destFile = File(destPath, attach.name)
//        val fromTo: String = resourcesProvider.getStringFromTo(srcFile.absolutePath, destFile.absolutePath)
        if (attach.isCrypted) {
            logger.logOperStart(LogObj.FILE, LogOper.DECRYPT)
            try {
                if (!cryptManager.encryptDecryptFile(srcFile, destFile, false)) {
//                        logger.logOperError(LogObj.FILE, LogOper.DECRYPT, fromTo, false, false)
                    return Failure.Decrypt.File(fileName = srcFile.path).toLeft()
                }
            } catch (ex: IOException) {
                return Failure.Decrypt.File(fileName = srcFile.path).toLeft()
            }
        } else {
            logger.logOperStart(LogObj.FILE, LogOper.COPY)
            try {
                if (!FileUtils.copyFile(srcFile, destFile)) {
//                    logger.logOperError(LogObj.FILE, LogOper.COPY, fromTo, false, false)
                    return Failure.File.Copy(filePath = srcFile.path, newPath = destFile.path).toLeft()
                }
            } catch (ex: IOException) {
                return Failure.File.Copy(filePath = srcFile.path, newPath = destFile.path).toLeft()
            }
        }
        return None.toRight()
    }

}