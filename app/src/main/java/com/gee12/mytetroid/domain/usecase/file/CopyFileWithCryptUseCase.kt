package com.gee12.mytetroid.domain.usecase.file

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.usecase.crypt.EncryptOrDecryptFileIfNeedUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.FilePath

/**
 * Сохранение файла по указанному пути с зашифровкой/расшифровкой при необходимости.
 */
class CopyFileWithCryptUseCase(
    private val context: Context,
    private val logger: ITetroidLogger,
    private val encryptOrDecryptFileIfNeedUseCase: EncryptOrDecryptFileIfNeedUseCase,
) : UseCase<UseCase.None, CopyFileWithCryptUseCase.Params>() {

    data class Params(
        val srcFile: DocumentFile,
        val destFile: DocumentFile,
        val isEncrypt: Boolean,
        val isDecrypt: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        return when {
            params.isEncrypt || params.isDecrypt -> {
                encryptOrDecryptFileIfNeedUseCase.run(
                    EncryptOrDecryptFileIfNeedUseCase.Params(
                        srcFile = params.srcFile,
                        destFile = params.destFile,
                        isEncrypt = params.isEncrypt,
                        isDecrypt = params.isDecrypt,
                    )
                )
            }
            else -> {
                logger.logOperStart(LogObj.FILE, LogOper.COPY)
                copyFile(params)
            }
        }
    }

    private fun copyFile(params: Params): Either<Failure, None> {
        val srcFile = params.srcFile
        val srcFilePath = FilePath.FileFull(srcFile.getAbsolutePath(context))
        val destFile = params.destFile
        val destFilePath = FilePath.FileFull(destFile.getAbsolutePath(context))

        return try {
            val copiedBytesCount = srcFile.openInputStream(context)?.use { inputStream ->
                destFile.openOutputStream(context, append = false)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                } ?: return Failure.File.Write(destFilePath).toLeft()
            } ?: return Failure.File.Read(srcFilePath).toLeft()
            if (copiedBytesCount > 0) {
                None.toRight()
            } else {
                Failure.File.Copy(from = srcFilePath, to = destFilePath).toLeft()
            }
        } catch (ex: Exception) {
            Failure.File.Copy(from = srcFilePath, to = destFilePath, ex).toLeft()
        }
    }

}