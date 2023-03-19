package com.gee12.mytetroid.domain.usecase.crypt

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.logs.ITetroidLogger

/**
 * Проверка установленного пароля.
 */
class CheckPasswordUseCase(
    private val logger: ITetroidLogger,
    private val storageProvider: IStorageProvider,
    private val cryptManager: IStorageCryptManager,
) : UseCase<CheckPasswordUseCase.Result, CheckPasswordUseCase.Params>() {

    data class Params(
        val password: String,
    )

    private val databaseConfig: DatabaseConfig
        get() = storageProvider.databaseConfig

    sealed class Result {
        object PasswordNotSet : Result()
        object Success : Result()
        object NotMatched : Result()
        data class AskForEmptyPassCheckingField(val fieldName: String) : Result()
    }

    override suspend fun run(params: Params): Either<Failure, Result> {
        val password = params.password

        return try {
            when {
                !databaseConfig.isCryptMode -> {
                    Result.PasswordNotSet.toRight()
                }
                checkPass(password) -> {
                    Result.Success.toRight()
                }
                else -> {
                    Result.NotMatched.toRight()
                }
            }
        } catch (ex: DatabaseConfig.EmptyFieldException) {
            // если поля в INI-файле для проверки пустые
            logger.logError(ex)
            // спрашиваем "continue anyway?"
            Result.AskForEmptyPassCheckingField(
                fieldName = ex.fieldName,
            ).toRight()
        }
    }

    /**
     * Проверка введенного пароля с сохраненным проверочным хэшем.
     * @throws DatabaseConfig.EmptyFieldException
     */
    @Throws(DatabaseConfig.EmptyFieldException::class)
    fun checkPass(pass: String?): Boolean {
        val salt = databaseConfig.cryptCheckSalt
        val checkHash = databaseConfig.cryptCheckHash
        return cryptManager.checkPass(pass, salt, checkHash)
    }

}