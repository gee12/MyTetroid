package com.gee12.mytetroid.domain.usecase.file

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.callback.FileCallback
import com.anggrayudi.storage.callback.FolderCallback
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.moveFileTo
import com.anggrayudi.storage.file.moveFolderTo
import com.anggrayudi.storage.media.FileDescription
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getStringFromTo
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.FilePath
import kotlinx.coroutines.coroutineScope

class MoveFileOrFolderUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
) : UseCase<UseCase.None, MoveFileOrFolderUseCase.Params>() {

    data class Params(
        val srcFileOrFolder: DocumentFile,
        val destFolder: DocumentFile,
        val newName: String?,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val srcFile = params.srcFileOrFolder
        val destFolder = params.destFolder
        val newFileName = params.newName

        // перемещаем файл или каталог
        return if (srcFile.isDirectory) {
            moveFolder(srcFile = srcFile, destFolder = destFolder, newFileName = newFileName)
        } else if (srcFile.isFile) {
            moveFile(srcFile, destFolder, newFileName)
        } else {
            None.toRight()
        }
    }

    private suspend fun moveFile(
        srcFile: DocumentFile,
        destFolder: DocumentFile,
        newFileName: String?,
    ): Either<Failure, None> {
        val srcFilePath = FilePath.FileFull(srcFile.getAbsolutePath(context))
        val destFolderPath = FilePath.Folder(destFolder.getAbsolutePath(context), newFileName.orEmpty())
        var isMoved = false

        coroutineScope {
            srcFile.moveFileTo(
                context = context,
                targetFolder = destFolder,
                fileDescription = newFileName?.let { FileDescription(name = it) },
                callback = object : FileCallback(uiScope = this) {
                    override fun onCompleted(result: Any) {
                        isMoved = true
                    }
                    override fun onFailed(errorCode: ErrorCode) {
                        isMoved = false
                    }
                }
            )
        }

        return if (isMoved) {
            val to = resourcesProvider.getString(R.string.log_to_mask, destFolderPath.fullPath)
            logger.logOperRes(LogObj.FILE, LogOper.MOVE, to, show = false)
            None.toRight()
        } else {
            val fromTo = resourcesProvider.getStringFromTo(srcFilePath.fullPath, destFolderPath.fullPath)
            logger.logOperError(LogObj.FILE, LogOper.MOVE, fromTo, false, show = false)
            Failure.File.Move(from = srcFilePath, to = destFolderPath).toLeft()
        }
    }

    private suspend fun moveFolder(
        srcFile: DocumentFile,
        destFolder: DocumentFile,
        newFileName: String?,
    ): Either<Failure, None> {
        val srcFilePath = FilePath.FileFull(srcFile.getAbsolutePath(context))
        val destFolderPath = FilePath.Folder(destFolder.getAbsolutePath(context), newFileName.orEmpty())
        var isMoved = false

        coroutineScope {
            srcFile.moveFolderTo(
                context = context,
                targetParentFolder = destFolder,
                skipEmptyFiles = false,
                newFolderNameInTargetPath = newFileName,
                callback = object : FolderCallback(uiScope = this) {
                    override fun onCompleted(result: Result) {
                        isMoved = result.success
                    }
                    override fun onFailed(errorCode: ErrorCode) {
                        isMoved = false
                    }
                }
            )
        }

        return if (isMoved) {
            val to = resourcesProvider.getString(R.string.log_to_mask, destFolderPath.fullPath)
            logger.logOperRes(LogObj.FOLDER, LogOper.MOVE, to, show = false)
            None.toRight()
        } else {
            val fromTo = resourcesProvider.getStringFromTo(srcFilePath.fullPath, destFolderPath.fullPath)
            logger.logOperError(LogObj.FOLDER, LogOper.MOVE, fromTo, false, show = false)
            Failure.Folder.Move(from = srcFilePath, to = destFolderPath).toLeft()
        }
    }

}