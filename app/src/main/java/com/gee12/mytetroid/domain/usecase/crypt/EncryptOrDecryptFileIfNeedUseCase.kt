package com.gee12.mytetroid.domain.usecase.crypt

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.openInputStream
import com.anggrayudi.storage.file.openOutputStream
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
                logger.logOperStart(LogObj.FILE, LogOper.DECRYPT)
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
        val srcFilePath = FilePath.FileFull(srcFile.getAbsolutePath(context))
        val destFile = params.destFile
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
                getFailure(params, srcFilePath).toLeft()
            }
        } catch (ex: Exception) {
            getFailure(params, srcFilePath,ex).toLeft()
        }
    }

    private fun getFailure(
        params: Params,
        srcFilePath: FilePath,
        ex: Exception? = null
    ): Failure {
        return if (params.isEncrypt) {
            Failure.Encrypt.File(srcFilePath, ex)
        } else {
            Failure.Decrypt.File(srcFilePath, ex)
        }
    }

}