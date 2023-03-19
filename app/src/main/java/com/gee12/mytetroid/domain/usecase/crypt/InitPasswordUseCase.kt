package com.gee12.mytetroid.domain.usecase.crypt

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.ISensitiveDataProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.TetroidStorage

/**
 * Сохранение пароля в настройках и его установка для шифрования.
 */
class InitPasswordUseCase(
    private val storageProvider: IStorageProvider,
    private val cryptManager: IStorageCryptManager,
    private val sensitiveDataProvider: ISensitiveDataProvider,
    private val saveMiddlePasswordHashUseCase: SaveMiddlePasswordHashUseCase,
) : UseCase<UseCase.None, InitPasswordUseCase.Params>() {

    data class Params(
        val password: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val storage = storageProvider.storage
            ?: return Failure.Storage.StorageNotInited.toLeft()

        return initPassword(
            password = params.password,
            storage = storage,
        )
    }

    private suspend fun initPassword(password: String, storage: TetroidStorage): Either<Failure, None> {

        val passHash = cryptManager.passToHash(password)
        if (storage.isSavePassLocal) {
            // записываем проверочную строку
            saveMiddlePasswordHashUseCase.run(
                SaveMiddlePasswordHashUseCase.Params(passHash)
            ).onFailure {
                return it.toLeft()
            }
        } else {
            // сохраняем хэш пароля в оперативную память, на время "сеанса" работы приложения
            sensitiveDataProvider.saveMiddlePasswordHash(passHash)
        }
        cryptManager.setKeyFromMiddleHash(passHash)

        return None.toRight()
    }

}