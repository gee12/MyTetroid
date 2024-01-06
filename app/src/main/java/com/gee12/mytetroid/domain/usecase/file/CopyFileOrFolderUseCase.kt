package com.gee12.mytetroid.domain.usecase.file

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.callback.FileCallback
import com.anggrayudi.storage.callback.FolderCallback
import com.anggrayudi.storage.file.*
import com.anggrayudi.storage.media.FileDescription
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getStringFromTo
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.FilePath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope

class CopyFileOrFolderUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
) : UseCase<UseCase.None, CopyFileOrFolderUseCase.Params>() {

    data class Params(
        val srcFileOrFolder: DocumentFile,
        val destFolder: DocumentFile,
        val newName: String?,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val srcFile = params.srcFileOrFolder
        val destFolder = params.destFolder
        val newFileName = params.newName

        return if (srcFile.isDirectory) {
            copyFolder(srcFile, destFolder, newFileName)
        } else if (srcFile.isFile) {
            copyFile(srcFile, destFolder, newFileName)
        } else {
            None.toRight()
        }
    }

    private suspend fun copyFile(
        srcFile: DocumentFile,
        destFolder: DocumentFile,
        newFileName: String?,
    ): Either<Failure, None> {
        val srcFilePath = FilePath.FileFull(srcFile.getAbsolutePath(context))
        val destFolderPath = FilePath.Folder(destFolder.getAbsolutePath(context), newFileName.orEmpty())
        var isCopied = false

        coroutineScope {
            srcFile.copyFileTo(
                context = context,
                targetFolder = destFolder,
                fileDescription = newFileName?.let { FileDescription(name = it) },
                callback = object : FileCallback(uiScope = this) {
                    override fun onCompleted(result: Any) {
                        isCopied = true
                    }
                    override fun onFailed(errorCode: ErrorCode) {
                        isCopied = false
                    }
                }
            )
        }

        return if (isCopied) {
            val to = resourcesProvider.getString(R.string.log_to_mask, destFolderPath.fullPath)
            logger.logOperRes(LogObj.FILE, LogOper.COPY, to, show = false)
            None.toRight()
        } else {
            val fromTo = resourcesProvider.getStringFromTo(srcFilePath.fullPath, destFolderPath.fullPath)
            logger.logOperError(LogObj.FILE, LogOper.COPY, fromTo, false, show = false)
            Failure.File.Copy(from = srcFilePath, to = destFolderPath).toLeft()
        }
    }

    private suspend fun copyFolder(
        srcFile: DocumentFile,
        destFolder: DocumentFile,
        newFileName: String?,
    ): Either<Failure, None> {
        val srcFilePath = FilePath.FileFull(srcFile.getAbsolutePath(context))
        val destFolderPath = FilePath.Folder(destFolder.getAbsolutePath(context), newFileName.orEmpty())
        var isCopied = false

        coroutineScope {
            srcFile.copyFolderTo(
                context = context,
                targetParentFolder = destFolder,
                skipEmptyFiles = false,
                newFolderNameInTargetPath = newFileName,
                callback = object : FolderCallback(uiScope = this) {
                    override fun onCompleted(result: Result) {
                        isCopied = result.success
                    }
                    override fun onFailed(errorCode: ErrorCode) {
                        isCopied = false
                    }
                }
            )
        }

        return if (isCopied) {
            val to = resourcesProvider.getString(R.string.log_to_mask, destFolderPath.fullPath)
            logger.logOperRes(LogObj.FOLDER, LogOper.COPY, to, show = false)
            None.toRight()
        } else {
            val fromTo = resourcesProvider.getStringFromTo(srcFilePath.fullPath, destFolderPath.fullPath)
            logger.logOperError(LogObj.FOLDER, LogOper.COPY, fromTo, false, show = false)
            Failure.Folder.Copy(from = srcFilePath, to = destFolderPath).toLeft()
        }
    }

}