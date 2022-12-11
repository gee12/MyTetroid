package com.gee12.mytetroid.usecase.attach

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.extensions.getStringFromTo
import com.gee12.mytetroid.common.extensions.getStringTo
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.helpers.IRecordPathProvider
import com.gee12.mytetroid.helpers.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidRecord
import java.io.File

/**
 * Переименование скопированных прикрепленных файлов в каталоге записи.
 */
class RenameRecordAttachesUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val recordPathProvider: IRecordPathProvider,
) : UseCase<UseCase.None, RenameRecordAttachesUseCase.Params>() {

    data class Params(
        val srcRecord: TetroidRecord,
        val destRecord: TetroidRecord,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val srcRecord = params.srcRecord
        val destRecord = params.destRecord

        for (i in 0 until srcRecord.attachedFilesCount) {
            val srcAttach = srcRecord.attachedFiles[i]
            val destAttach = destRecord.attachedFiles[i]
            val srcFileDisplayName = srcAttach.name
            val ext = FileUtils.getExtensionWithComma(srcFileDisplayName)
            val srcFileIdName = srcAttach.id + ext
            val destFileIdName = destAttach.id + ext
            // переименовываем
            val destPath = recordPathProvider.getPathToRecordFolder(destRecord)
            val srcFile = File(destPath, srcFileIdName)
            val destFile = File(destPath, destFileIdName)
            if (srcFile.renameTo(destFile)) {
                val to = resourcesProvider.getStringTo(destFile.absolutePath)
                logger.logOperRes(LogObj.FILE, LogOper.RENAME, to, false)
            } else {
                val fromTo = resourcesProvider.getStringFromTo(srcFile.absolutePath, destFile.name)
                logger.logOperError(LogObj.FILE, LogOper.RENAME, fromTo, false, false)
            }
        }

        return None.toRight()
    }

}