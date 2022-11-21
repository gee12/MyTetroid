package com.gee12.mytetroid.usecase.crypt

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.helpers.ISensitiveDataProvider
import com.gee12.mytetroid.interactors.EncryptionInteractor
import com.gee12.mytetroid.interactors.PasswordInteractor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage

class CheckStoragePasswordAndAskUseCase(
    private val logger: ITetroidLogger,
    private val cryptInteractor: EncryptionInteractor,
    private val passInteractor: PasswordInteractor,
    private val sensitiveDataProvider: ISensitiveDataProvider,
) : UseCase<CheckStoragePasswordAndAskUseCase.Result, CheckStoragePasswordAndAskUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage?,
        val isStorageCrypted: Boolean,
    )

    sealed class Result {
        object AskPassword : Result()
        data class AskPin(
            val specialFlag: Boolean,
        ) : Result()
        data class AskForEmptyPassCheckingField(
            val fieldName: String,
            val passHash: String,
        ) : Result()
    }

    override suspend fun run(params: Params): Either<Failure, Result> {
        val storage = params.storage
        val isStorageCrypted = params.isStorageCrypted
        val isSaveMiddlePassLocal = storage?.isSavePassLocal ?: false

        var middlePassHash: String? = null
        return when {
            sensitiveDataProvider.getMiddlePassHashOrNull()?.also { middlePassHash = it } != null -> {
                // хэш пароля сохранен в оперативной памяти (вводили до этого и проверяли)
                cryptInteractor.initCryptPass(middlePassHash!!, true)
                // запрос ПИН-кода
                Result.AskPin(
                    specialFlag = true,
                ).toRight()
            }
            isSaveMiddlePassLocal && storage?.middlePassHash.also { middlePassHash = it } != null -> {
                // хэш пароля сохранен локально, проверяем
                try {
                    if (passInteractor.checkMiddlePassHash(middlePassHash)) {
                        cryptInteractor.initCryptPass(middlePassHash!!, true)
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
                    // if (DataManager.isExistsCryptedNodes()) {
                    if (isStorageCrypted) {
                        // спрашиваем "continue anyway?"
                        Result.AskForEmptyPassCheckingField(
                            fieldName = ex.fieldName,
                            passHash = middlePassHash!!,
                        ).toRight()
                    } else {
                        // если нет зашифрованных веток, но пароль сохранен
                        cryptInteractor.initCryptPass(middlePassHash!!, true)
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

}