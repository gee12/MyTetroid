package com.gee12.mytetroid.domain.usecase.crypt

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.ISensitiveDataProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.ui.storage.StorageParams

class CheckPasswordOrPinAndDecryptUseCase(
    private val logger: ITetroidLogger,
    private val sensitiveDataProvider: ISensitiveDataProvider,
    private val settingsManager: CommonSettingsManager,
    private val cryptManager: IStorageCryptManager,
    private val storageProvider: IStorageProvider,
) : UseCase<CheckPasswordOrPinAndDecryptUseCase.Result, CheckPasswordOrPinAndDecryptUseCase.Params>() {

    data class Params(
        val params: StorageParams,
        val isAlreadyTryDecrypt: Boolean,
    )

    sealed class Result {
        object PasswordNotSet : Result()
        object None : Result()
        object AskPassword : Result()
        object AskPin : Result()
        data class LoadWithoutDecrypt(
            val params: StorageParams,
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
        val isSaveMiddlePassLocal = storage?.isSavePassLocal ?: false
        val isNodeOpening = params.params.isNodeOpening

        var middlePassHash = sensitiveDataProvider.getMiddlePasswordHashOrNull()
        return when {
            !databaseConfig.isCryptMode -> {
                Result.PasswordNotSet.toRight()
            }
            middlePassHash != null -> {
                // хэш пароля уже установлен (вводили до этого и проверяли)
                cryptManager.setKeyFromMiddleHash(middlePassHash)
                // спрашиваем ПИН-код
                Result.AskPin.toRight()
            }
            isSaveMiddlePassLocal && storage?.middlePassHash != null -> {
                middlePassHash = storage.middlePassHash!!
                // хэш пароля сохранен локально, проверяем
                params.params.passHash = middlePassHash

                try {
                    if (checkMiddlePassHash(middlePassHash)) {
                        // сохраненный хеш пароля подошел, устанавливаем его
                        sensitiveDataProvider.saveMiddlePasswordHash(middlePassHash)
                        cryptManager.setKeyFromMiddleHash(middlePassHash)
                        // спрашиваем ПИН-код
                        Result.AskPin.toRight()
                    } else if (isNodeOpening) {
                        // если сохраненный хэш пароля не подошел, и это открытие зашифрованной ветки,
                        //  то сразу спрашиваем пароль
                        Result.AskPassword.toRight()
                    } else {
                        // в остальных случаях, когда сохраненный хэш пароля не подошел,
                        //  загружаем хранилище без расшифровки
                        logger.log(R.string.log_wrong_saved_pass, show = true)
                        if (!params.isAlreadyTryDecrypt) {
                            params.params.isDecrypt = false

                            Result.LoadWithoutDecrypt(
                                params = params.params,
                            ).toRight()
                        } else {
                            Result.None.toRight()
                        }
                    }
                } catch (ex: DatabaseConfig.EmptyFieldException) {
                    // если поля в INI-файле для проверки пустые
                    logger.logError(ex, show = true)
                    // спрашиваем "continue anyway?"
                    params.params.fieldName = ex.fieldName

                    Result.AskForEmptyPassCheckingField(
                        fieldName = ex.fieldName,
                        passHash = middlePassHash,
                    ).toRight()
                }
            }
            settingsManager.isAskPassOnStart() || isNodeOpening -> {
                // если пароль не установлен и не сохранен локально, то спрашиваем его, если:
                //  * нужно расшифровывать хранилище сразу на старте
                //  * функция вызвана во время открытия зашифрованной ветки
                //  * ??? если мы не вызвали загрузку всех веток
                Result.AskPassword.toRight()
            }
            else -> {
                // если пароль не установлен и не сохранен локально, и его не нужно спрашивать, то
                //  просто загружаем хранилище без расшифровки
                params.params.isDecrypt = false

                Result.LoadWithoutDecrypt(
                    params = params.params,
                ).toRight()
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