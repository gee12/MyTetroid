package com.gee12.mytetroid.domain.usecase.crypt

import android.content.Context
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IRecordPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidRecord

/**
 * Зашифровка или расшифровка файла записи и прикрепленных файлов при необходимости.
 */
class CryptRecordFilesIfNeedUseCase(
    private val context: Context,
    private val logger: ITetroidLogger,
    private val resourcesProvider: IResourcesProvider,
    private val storageProvider: IStorageProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val encryptOrDecryptFileUseCase: EncryptOrDecryptFileIfNeedUseCase,
) : UseCase<Boolean, CryptRecordFilesIfNeedUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val isEncrypted: Boolean,
        val isEncrypt: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, Boolean> {
        val record = params.record
        val isEncrypted = params.isEncrypted
        val isEncrypt = params.isEncrypt

        // файл записи
        val storageFolder = storageProvider.rootFolder
        val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
        val htmlFileRelativePath = recordPathProvider.getRelativePathToRecordHtml(record)
        val htmlFilePath = FilePath.File(storageFolderPath, htmlFileRelativePath)

        val htmlFile = storageFolder?.child(
            context = context,
            path = htmlFileRelativePath,
            requiresWriteAccess = false,
        )

        if (htmlFile != null && htmlFile.exists()) {
            encryptOrDecryptFileUseCase.run(
                EncryptOrDecryptFileIfNeedUseCase.Params(
                    srcFile = htmlFile,
                    destFile = htmlFile,
                    isEncrypt = !isEncrypted && isEncrypt,
                    isDecrypt = isEncrypted && !isEncrypt,
                )
            ).onFailure {
                return it.toLeft()
            }
        } else {
            logger.logWarning(resourcesProvider.getString(R.string.error_file_is_missing_mask, htmlFilePath.fullPath), show = false)
        }

        // прикрепленные файлы
        if (record.attachedFilesCount > 0) {
            for (attach in record.attachedFiles) {
                val attachFileRelativePath = recordPathProvider.getRelativePathToRecordAttach(attach)
                val attachFilePath = FilePath.File(storageFolderPath, attachFileRelativePath)

                val attachFile = storageFolder?.child(
                    context = context,
                    path = attachFileRelativePath,
                    requiresWriteAccess = false,
                ) ?: return Failure.File.Get(attachFilePath).toLeft()

                if (!attachFile.exists()) {
                    logger.logWarning(resourcesProvider.getString(R.string.error_file_is_missing_mask, attachFilePath.fullPath), false)
                    continue
                }
                encryptOrDecryptFileUseCase.run(
                    EncryptOrDecryptFileIfNeedUseCase.Params(
                        srcFile = attachFile,
                        destFile = attachFile,
                        isEncrypt = !isEncrypted && isEncrypt,
                        isDecrypt = isEncrypted && !isEncrypt,
                    )
                ).onFailure {
                    return it.toLeft()
                }

            }
        }
        return true.toRight()
    }
}