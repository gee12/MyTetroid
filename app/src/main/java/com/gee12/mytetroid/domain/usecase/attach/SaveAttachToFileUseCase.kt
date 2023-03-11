package com.gee12.mytetroid.domain.usecase.attach

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getIdString
import com.gee12.mytetroid.common.extensions.getStringTo
import com.gee12.mytetroid.domain.provider.IRecordPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.domain.usecase.file.CopyFileWithCryptUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidFile

/**
 * Сохранение прикрепленного файла по указанному пути.
 */
class SaveAttachToFileUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val storageProvider: IStorageProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val copyFileWithCryptUseCase: CopyFileWithCryptUseCase,
) : UseCase<UseCase.None, SaveAttachToFileUseCase.Params>() {

    data class Params(
        val attach: TetroidFile,
        val destFolder: DocumentFile,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val attach = params.attach
        val record = attach.record
        val destFolder = params.destFolder
        val destFolderPath = destFolder.uri.path.orEmpty()
        val storageFolder = storageProvider.rootFolder
        val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
        val recordFolderRelativePath = recordPathProvider.getRelativePathToRecordFolder(record)
        val recordFolderPath = FilePath.Folder(storageFolderPath, recordFolderRelativePath)

        val recordFolder = storageFolder?.child(
            context = context,
            path = recordFolderRelativePath
        ) ?: return Failure.Folder.Get(recordFolderPath).toLeft()

        val mes = attach.getIdString(resourcesProvider) + resourcesProvider.getStringTo(destFolderPath)
        logger.logOperStart(LogObj.FILE, LogOper.SAVE, ": $mes")

        // проверка исходного файла
        val srcFilePath = FilePath.File(recordFolderPath.fullPath, attach.idName)

        val srcFile = recordFolder.child(
            context = context,
            path = srcFilePath.fileName,
            requiresWriteAccess = true,
        ) ?: return Failure.File.Get(srcFilePath).toLeft()

        if (!srcFile.exists()) {
            return Failure.File.NotExist(srcFilePath).toLeft()
        }

        // создание нового пустого файла
        val destFile = destFolder.makeFile(
            context = context,
            name = attach.name,
            mimeType = MimeType.UNKNOWN,
            mode = CreateMode.REPLACE,
        ) ?: return Failure.File.Create(srcFilePath).toLeft()
        if (!srcFile.exists()) {
            return Failure.File.NotExist(srcFilePath).toLeft()
        }

        // копирование файла в указанный каталог, расшифровуя при необходимости
        return copyFileWithCryptUseCase.run(
            CopyFileWithCryptUseCase.Params(
                srcFile = srcFile,
                destFile = destFile,
                isEncrypt = false,
                isDecrypt = attach.isCrypted,
            )
        )
    }

}