package com.gee12.mytetroid.usecase.crypt

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.helpers.ISensitiveDataProvider
import com.gee12.mytetroid.interactors.EncryptionInteractor
import com.gee12.mytetroid.interactors.PasswordInteractor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.viewmodels.EmptyPassCheckingFieldCallbackParams
import com.gee12.mytetroid.viewmodels.EventCallbackParams

class CheckStoragePasswordUseCase(
    private val logger: ITetroidLogger,
    private val cryptInteractor: EncryptionInteractor,
    private val passInteractor: PasswordInteractor,
    private val sensitiveDataProvider: ISensitiveDataProvider,
) : UseCase<CheckStoragePasswordUseCase.Result, CheckStoragePasswordUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage?,
        val isStorageCrypted: Boolean,
        val callback: EventCallbackParams,
    )

    sealed class Result {
        data class AskPassword(
            val callback: EventCallbackParams
        ) : Result()
        data class AskPin(
            val specialFlag: Boolean, val callback: EventCallbackParams
        ) : Result()
        data class AskForEmptyPassCheckingField(
            //val middlePassHash: String,
            //val callback: EventCallbackParams,
            val params: EmptyPassCheckingFieldCallbackParams,
        ) : Result()
    }

    override suspend fun run(params: Params): Either<Failure, Result> {
        val storage = params.storage
        val isStorageCrypted = params.isStorageCrypted
        val isSaveMiddlePassLocal = storage?.isSavePassLocal ?: false
        val callback = params.callback

        var middlePassHash: String? = null
        return when {
            sensitiveDataProvider.getMiddlePassHashOrNull()?.also { middlePassHash = it } != null -> {
                // хэш пароля сохранен в оперативной памяти (вводили до этого и проверяли)
                cryptInteractor.initCryptPass(middlePassHash!!, true)
                // запрос ПИН-кода
                Result.AskPin(
                    specialFlag = true,
                    callback = callback,
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
                            callback = callback,
                        ).toRight()
                    } else {
                        logger.log(R.string.log_wrong_saved_pass, true)
                        // спрашиваем пароль
                        Result.AskPassword(
                            callback = callback,
                        ).toRight()
                    }
                } catch (ex: DatabaseConfig.EmptyFieldException) {
                    // если поля в INI-файле для проверки пустые
                    logger.logError(ex)
                    // if (DataManager.isExistsCryptedNodes()) {
                    if (isStorageCrypted) {
                        // спрашиваем "continue anyway?"
                        Result.AskForEmptyPassCheckingField(
                            params = EmptyPassCheckingFieldCallbackParams(
                                fieldName = ex.fieldName,
                                passHash = middlePassHash!!,
                                callback = callback
                            )
                        ).toRight()
                    } else {
                        // если нет зашифрованных веток, но пароль сохранен
                        cryptInteractor.initCryptPass(middlePassHash!!, true)
                        Result.AskPin(
                            specialFlag = true,
                            callback = callback,
                        ).toRight()
                    }
                }
            }
            else -> {
                // спрашиваем или задаем пароль
                Result.AskPassword(
                    callback = callback,
                ).toRight()
            }
        }
    }

}