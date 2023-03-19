package com.gee12.mytetroid.domain.usecase.crypt

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.domain.provider.ISensitiveDataProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.logs.ITetroidLogger

class CheckPasswordOrPinAndAskUseCase(
    private val logger: ITetroidLogger,
    private val cryptManager: IStorageCryptManager,
    private val storageProvider: IStorageProvider,
    private val sensitiveDataProvider: ISensitiveDataProvider,
) : UseCase<CheckPasswordOrPinAndAskUseCase.Result, CheckPasswordOrPinAndAskUseCase.Params>() {

    data class Params(
        val isStorageEncrypted: Boolean,
    )

    sealed class Result {
        object PasswordNotSet : Result()
        object AskPassword : Result()
        data class AskPin(
            val specialFlag: Boolean,
        ) : Result()
        data class AskForEmptyPassCheckingField(
            val fieldName: String,
            val passHash: String,
        ) : Result()
    }

    private val databaseConfig: DatabaseConfig
        get() = storageProvider.databaseConfig

    override suspend fun run(params: Params): Either<Failure, Result> {
        val storage = storageProvider.storage
        val isStorageEncrypted = params.isStorageEncrypted
        val isSaveMiddlePassLocal = storage?.isSavePassLocal ?: false

        var middlePassHash = sensitiveDataProvider.getMiddlePasswordHashOrNull()
        return when {
            !databaseConfig.isCryptMode -> {
                Result.PasswordNotSet.toRight()
            }
            middlePassHash != null -> {
                // хэш пароля сохранен в оперативной памяти (вводили до этого и проверяли)
                cryptManager.setKeyFromMiddleHash(middlePassHash)
                // запрос ПИН-кода
                Result.AskPin(
                    specialFlag = true,
                ).toRight()
            }
            isSaveMiddlePassLocal && storage?.middlePassHash != null -> {
                middlePassHash = storage.middlePassHash!!
                // хэш пароля сохранен локально, проверяем
                try {
                    if (checkMiddlePassHash(middlePassHash)) {
                        // сохраненный хеш пароля подошел, устанавливаем его
                        sensitiveDataProvider.saveMiddlePasswordHash(middlePassHash)
                        cryptManager.setKeyFromMiddleHash(middlePassHash)
                        // запрос ПИН-кода
                        Result.AskPin(
                            specialFlag = true,
                        ).toRight()
                    } else {
                        logger.log(R.string.log_wrong_saved_pass, true)
                        // спрашиваем пароль
                        Result.AskPassword.toRight()
                    }
                } catch (ex: DatabaseConfig.EmptyFieldException) {
                    // если поля в INI-файле для проверки пустые
                    logger.logError(ex)
                    if (isStorageEncrypted) {
                        // спрашиваем "continue anyway?"
                        Result.AskForEmptyPassCheckingField(
                            fieldName = ex.fieldName,
                            passHash = middlePassHash,
                        ).toRight()
                    } else {
                        // если нет зашифрованных веток, но пароль сохранен
                        cryptManager.setKeyFromMiddleHash(middlePassHash)
                        Result.AskPin(
                            specialFlag = true,
                        ).toRight()
                    }
                }
            }
            else -> {
                // спрашиваем или задаем пароль
                Result.AskPassword.toRight()
            }
        }
    }

    /**
     * Проверка сохраненного хэша пароля с помощью сохраненных зашифрованных данных.
     * @throws DatabaseConfig.EmptyFieldException
     */
    @Throws(DatabaseConfig.EmptyFieldException::class)
    fun checkMiddlePassHash(passHash: String?): Boolean {
        val checkData = databaseConfig.middleHashCheckData
        return cryptManager.checkMiddlePassHash(passHash, checkData)
    }

}