package com.gee12.mytetroid.domain.usecase.attach

import android.content.Context
import android.net.Uri
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getExtensionWithoutComma
import com.gee12.mytetroid.common.extensions.withExtension
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.IRecordPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.domain.provider.IStorageSettingsProvider
import com.gee12.mytetroid.domain.usecase.record.GetRecordFolderUseCase
import com.gee12.mytetroid.model.FilePath
import java.io.IOException

/**
 * Получение прикрепленного файла с предварительной расшифровкой (если необходимо).
 */
class PrepareAttachForOpenUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val cryptManager: IStorageCryptManager,
    private val storageProvider: IStorageProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val storageSettingsProvider: IStorageSettingsProvider,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,
) : UseCase<Uri, PrepareAttachForOpenUseCase.Params>() {

    data class Params(
        val attach: TetroidFile
    )

    override suspend fun run(params: Params): Either<Failure, Uri> {
        val attach = params.attach
        val storageFolder = storageProvider.rootFolder
        val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()

        logger.logDebug(resourcesProvider.getString(R.string.log_start_attach_file_opening) + attach.id)
        val record = attach.record
        val fileDisplayName = attach.name
        val ext = fileDisplayName.getExtensionWithoutComma()
        val fileIdName = attach.id.withExtension(ext)

        val attachFileRelativePath = recordPathProvider.getRelativePathToRecordAttach(attach)
        val attachFilePath = FilePath.File(storageFolderPath, attachFileRelativePath)

        val attachFile = storageFolder?.child(
            context = context,
            path = attachFileRelativePath,
            requiresWriteAccess = false,
        ) ?: return Failure.File.Get(attachFilePath).toLeft()

        logger.log(resourcesProvider.getString(R.string.log_open_attach_file_mask, fileDisplayName), show = false)
        if (!attachFile.exists()) {
            return Failure.File.NotExist(attachFilePath).toLeft()
        }
        // если запись зашифрована

        // TODO: use CopyFileWithCryptUseCase

        return if (record.isCrypted && storageSettingsProvider.isDecryptAttachesToTempFolder()) {
            // создаем временный файл
            val tempFolder = getRecordFolderUseCase.run(
                GetRecordFolderUseCase.Params(
                    record = record,
                    createIfNeed = true,
                    inTrash = true,
                )
            ).foldResult(
                onLeft = {
                    return it.toLeft()
                },
                onRight = { it }
            )
            val tempFolderPath = tempFolder.getAbsolutePath(context)
            val tempFilePath = FilePath.File(tempFolderPath, fileIdName)

            // расшифровываем во временный файл
            val tempFile = try {
                tempFolder.makeFile(
                    context = context,
                    name = fileIdName,
                    mimeType = MimeType.TEXT,
                    mode = CreateMode.REPLACE,
                ) ?: return Failure.File.Create(tempFilePath).toLeft()
            } catch (ex: Exception) {
                return Failure.File.Create(tempFilePath, ex).toLeft()
            }

            try {
                val isDecrypted = attachFile.openInputStream(context)?.use { inputStream ->
                    tempFile.openOutputStream(context, append = false)?.use { outputStream ->
                        cryptManager.encryptOrDecryptFile(
                            srcFileStream = inputStream,
                            destFileStream = outputStream,
                            encrypt = false
                        )
                    } ?: return Failure.File.Write(tempFilePath).toLeft()
                } ?: return Failure.File.Read(attachFilePath).toLeft()

                if (isDecrypted) {
                    tempFile.uri.toRight()
                } else {
                    return Failure.Decrypt.File(attachFilePath).toLeft()
                }
            } catch (ex: IOException) {
                return Failure.Decrypt.File(attachFilePath).toLeft()
            }
        } else {
            attachFile.uri.toRight()
        }
    }

}