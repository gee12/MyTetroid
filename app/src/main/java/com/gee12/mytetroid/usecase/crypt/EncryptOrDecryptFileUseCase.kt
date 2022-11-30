package com.gee12.mytetroid.usecase.crypt

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.data.crypt.Crypter
import java.io.File

/**
 * Зашифровка или расшифровка файла при необходимости.
 */
class EncryptOrDecryptFileUseCase(
    private val crypter: Crypter,
) : UseCase<EncryptOrDecryptFileUseCase.Result, EncryptOrDecryptFileUseCase.Params>() {

    data class Params(
        val file: File,
        val isCrypted: Boolean,
        val isEncrypt: Boolean,
    )

    sealed class Result {
        object None : Result()
        object Success : Result()
    }

    override suspend fun run(params: Params): Either<Failure, Result> {
        val file = params.file
        val isCrypted = params.isCrypted
        val isEncrypt = params.isEncrypt

        return if (isCrypted && !isEncrypt) {
            // расшифровуем файл записи
            encryptDecryptFile(file, isEncrypt = false)
        } else if (!isCrypted && isEncrypt) {
            // зашифровуем файл записи
            encryptDecryptFile(file, isEncrypt = true)
        } else {
            Result.None.toRight()
        }
    }

    private fun encryptDecryptFile(file: File, isEncrypt: Boolean): Either<Failure, Result> {
        @Suppress("BlockingMethodInNonBlockingContext")
        return try {
            if (crypter.encryptDecryptFile(file, file, isEncrypt)) {
                Result.Success.toRight()
            } else {
                handleFailure(isEncrypt, file)
            }
        } catch (ex: Exception) {
            handleFailure(isEncrypt, file, ex)
        }
    }

    private fun handleFailure(
        isEncrypt: Boolean,
        file: File,
        ex: Exception? = null
    ): Either.Left<Failure> {
        return if (isEncrypt) {
            Failure.Encrypt.File(fileName = file.path, ex).toLeft()
        } else {
            Failure.Decrypt.File(fileName = file.path, ex).toLeft()
        }
    }

}