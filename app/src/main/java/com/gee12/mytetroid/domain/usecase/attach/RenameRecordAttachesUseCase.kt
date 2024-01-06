package com.gee12.mytetroid.domain.usecase.attach

import android.content.Context
import com.anggrayudi.storage.file.changeName
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getExtensionWithoutComma
import com.gee12.mytetroid.common.extensions.getStringFromTo
import com.gee12.mytetroid.common.extensions.getStringTo
import com.gee12.mytetroid.common.extensions.withExtension
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.domain.provider.IRecordPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidRecord

/**
 * Переименование скопированных прикрепленных файлов в каталоге записи.
 */
class RenameRecordAttachesUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val storageProvider: IStorageProvider,
    private val recordPathProvider: IRecordPathProvider,
) : UseCase<UseCase.None, RenameRecordAttachesUseCase.Params>() {

    data class Params(
        val srcRecord: TetroidRecord,
        val destRecord: TetroidRecord,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val srcRecord = params.srcRecord
        val destRecord = params.destRecord
        val storageFolder = storageProvider.rootFolder
        val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
        val recordFolderRelativePath = recordPathProvider.getRelativePathToRecordFolder(destRecord)
        val recordFolderPath = FilePath.Folder(storageFolderPath, recordFolderRelativePath)

        val recordFolder = storageFolder?.child(
            context = context,
            path = recordFolderRelativePath
        ) ?: return Failure.Folder.Get(recordFolderPath).toLeft()

        for (i in 0 until srcRecord.attachedFilesCount) {
            val srcAttach = srcRecord.attachedFiles[i]
            val destAttach = destRecord.attachedFiles[i]
            val srcFileDisplayName = srcAttach.name
            val ext = srcFileDisplayName.getExtensionWithoutComma()
            val srcFileIdName = srcAttach.id.withExtension(ext)
            val destFileIdName = destAttach.id.withExtension(ext)
            // переименовываем
            val srcFilePath = FilePath.File(recordFolderPath.fullPath, srcFileIdName)
            val srcFile = recordFolder.child(
                context = context,
                path = srcFileIdName,
                requiresWriteAccess = true,
            )
            val destFile = srcFile?.changeName(context, newBaseName = destAttach.id, newExtension = ext)
            if (destFile != null) {
                val to = resourcesProvider.getStringTo(destFile.getAbsolutePath(context))
                logger.logOperRes(LogObj.FILE, LogOper.RENAME, to, show = false)
            } else {
                val fromTo = resourcesProvider.getStringFromTo(srcFilePath.fullPath, destFileIdName)
                logger.logOperError(LogObj.FILE, LogOper.RENAME, fromTo, false, show = false)
            }
        }

        return None.toRight()
    }

}