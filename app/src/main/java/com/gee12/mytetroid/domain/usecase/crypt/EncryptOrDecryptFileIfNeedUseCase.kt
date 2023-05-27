package com.gee12.mytetroid.domain.usecase.crypt

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.FilePath

/**
 * Зашифровка или расшифровка файла при необходимости.
 */
class EncryptOrDecryptFileIfNeedUseCase(
    private val context: Context,
    private val logger: ITetroidLogger,
    private val cryptManager: IStorageCryptManager,
) : UseCase<UseCase.None, EncryptOrDecryptFileIfNeedUseCase.Params>() {

    data class Params(
        val srcFile: DocumentFile,
        val destFile: DocumentFile,
        val isEncrypt: Boolean,
        val isDecrypt: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        return when {
            params.isEncrypt -> {
                logger.logOperStart(LogObj.FILE, LogOper.ENCRYPT)
                encryptOrDecryptFile(params, isEncrypt = true)
            }
            params.isDecrypt -> {
                logger.logOperStart(LogObj.FILE, LogOper.DECRYPT)
                encryptOrDecryptFile(params, isEncrypt = false)
            }
            else -> {
                None.toRight()
            }
        }
    }

    private fun encryptOrDecryptFile(params: Params, isEncrypt: Boolean): Either<Failure, None> {
        val srcFile = params.srcFile
        val destFile = params.destFile

        return if (srcFile.id == destFile.id) {
            encryptOrDecryptOneFile(srcFile, isEncrypt)
        } else {
            encryptOrDecryptDifferentFiles(srcFile, destFile, isEncrypt)
        }
    }

    private fun encryptOrDecryptDifferentFiles(
        srcFile: DocumentFile,
        destFile: DocumentFile,
        isEncrypt: Boolean,
    ): Either<Failure, None> {
        val srcFilePath = FilePath.FileFull(srcFile.getAbsolutePath(context))
        val destFilePath = FilePath.FileFull(destFile.getAbsolutePath(context))

        return try {
            val result = srcFile.openInputStream(context)?.use { inputStream ->
                destFile.openOutputStream(context, append = false)?.use { outputStream ->
                    cryptManager.encryptOrDecryptFile(
                        srcFileStream = inputStream,
                        destFileStream = outputStream,
                        encrypt = isEncrypt,
                    )
                } ?: return Failure.File.Write(destFilePath).toLeft()
            } ?: return Failure.File.Read(srcFilePath).toLeft()
            if (result) {
                None.toRight()
            } else {
                getFailure(isEncrypt, srcFilePath).toLeft()
            }
        } catch (ex: Exception) {
            getFailure(isEncrypt, srcFilePath, ex).toLeft()
        }
    }

    private fun encryptOrDecryptOneFile(srcFile: DocumentFile, isEncrypt: Boolean): Either<Failure, None> {
        val srcFilePath = FilePath.FileFull(srcFile.getAbsolutePath(context))

        return try {
            val srcFileFolder = srcFile.findParent(
                context = context,
                requiresWriteAccess = true,
            ) ?: return Failure.Folder.Get(path = srcFilePath).toLeft()
            val srcFileName = srcFile.name!!
            val tempFileName = "${srcFileName}_temp"
            val tempFilePath = FilePath.File(srcFileFolder.getAbsolutePath(context), tempFileName)

            // создаем временный файл, в который будем зашифровывать/расшифровывать данные исходного файла
            val tempFile = srcFileFolder.makeFile(
                context = context,
                name = tempFileName,
                mimeType = MimeType.UNKNOWN,
                mode = CreateMode.CREATE_NEW,
            ) ?: return Failure.File.Create(tempFilePath).toLeft()

            return encryptOrDecryptDifferentFiles(
                srcFile = srcFile,
                destFile = tempFile,
                isEncrypt = isEncrypt,
            ).map {
                // удаляем исходный файл
                srcFile.delete()
            }.flatMap {
                // переименовываем временный файл в исходный
                tempFile.renameTo(srcFileName)
                None.toRight()
            }
        } catch (ex: Exception) {
            getFailure(isEncrypt, srcFilePath, ex).toLeft()
        }
    }

    private fun getFailure(
        isEncrypt: Boolean,
        srcFilePath: FilePath,
        ex: Exception? = null
    ): Failure {
        return if (isEncrypt) {
            Failure.Encrypt.File(srcFilePath, ex)
        } else {
            Failure.Decrypt.File(srcFilePath, ex)
        }
    }

}