package com.gee12.mytetroid.domain.usecase.attach

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getExtensionWithoutComma
import com.gee12.mytetroid.common.extensions.withExtension
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.domain.usecase.crypt.EncryptOrDecryptFileIfNeedUseCase
import com.gee12.mytetroid.domain.usecase.record.GetRecordFolderUseCase
import com.gee12.mytetroid.model.FilePath

/**
 * Получение прикрепленного файла с предварительной расшифровкой (если необходимо).
 */
class PrepareAttachForOpenUseCase(
    private val context: Context,
    private val appBuildInfoProvider: BuildInfoProvider,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val storageProvider: IStorageProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val storageSettingsProvider: IStorageSettingsProvider,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,
    private val encryptOrDecryptFileIfNeedUseCase: EncryptOrDecryptFileIfNeedUseCase,
) : UseCase<Uri, PrepareAttachForOpenUseCase.Params>() {

    data class Params(
        val attach: TetroidFile
    )

    override suspend fun run(params: Params): Either<Failure, Uri> {
        return getFile(params).flatMap {  file ->
            getContentFileUri(file)
        }
    }

    private suspend fun getFile(params: Params): Either<Failure, DocumentFile> {
        val attach = params.attach
        val storageFolder = storageProvider.rootFolder
        val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()

        logger.logDebug(resourcesProvider.getString(R.string.log_start_attach_file_opening) + attach.id)
        val record = attach.record
        val fileDisplayName = attach.name

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
        return if (record.isCrypted && storageSettingsProvider.isDecryptAttachesToTempFolder()) {
            decryptAttachFile(attach, attachFile)
        } else {
            attachFile.toRight()
        }
    }

    private suspend fun decryptAttachFile(attach: TetroidFile, attachFile: DocumentFile): Either<Failure, DocumentFile> {
        val record = attach.record
        val fileDisplayName = attach.name
        val ext = fileDisplayName.getExtensionWithoutComma()
        val fileIdName = attach.id.withExtension(ext)

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

        return encryptOrDecryptFileIfNeedUseCase.run(
            EncryptOrDecryptFileIfNeedUseCase.Params(
                srcFile = attachFile,
                destFile = tempFile,
                isEncrypt = false,
                isDecrypt = true,
            )
        ).map {
            tempFile
        }
    }

    private fun getContentFileUri(file: DocumentFile): Either<Failure, Uri> {
        return if (file.isRawFile && appBuildInfoProvider.appVersionCode >= 24) {
            val filePath = FilePath.FileFull(file.getAbsolutePath(context))
            try {
                // Начиная с API 24, для предоставления доступа к файлам, который ассоциируется с приложением
                // (по сути, для открытия файла другими приложениями с помощью Intent),
                // нужно использовать механизм FileProvider.
                // Uri должен быть иметь scheme "content://"
                FileProvider.getUriForFile(
                    context,
                    "${appBuildInfoProvider.applicationId}.provider",
                    file.toRawFile(context)!!
                ).toRight()
            } catch (ex: Exception) {
                Failure.File.GetContentUri(filePath, ex).toLeft()
            }
        } else {
            file.uri.toRight()
        }
    }

}