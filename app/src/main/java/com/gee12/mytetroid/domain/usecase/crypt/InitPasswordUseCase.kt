package com.gee12.mytetroid.domain.usecase.crypt

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.domain.provider.ISensitiveDataProvider
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.domain.repo.StoragesRepo

/**
 * Сохранение пароля в настройках и его установка для шифрования.
 */
class InitPasswordUseCase(
    private val storagesRepo: StoragesRepo,
    private val cryptManager: IStorageCryptManager,
    private val sensitiveDataProvider: ISensitiveDataProvider,
) : UseCase<UseCase.None, InitPasswordUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
        val databaseConfig: DatabaseConfig,
        val password: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        return initPassword(params)
            .flatMap { updateStorage(params.storage) }
            .map { None }
    }

    private fun initPassword(params: Params): Either<Failure, None> {
        val storage = params.storage
        val password = params.password
        val databaseConfig = params.databaseConfig

        val passHash = cryptManager.passToHash(password)
        if (storage.isSavePassLocal) {
            // сохраняем хэш пароля
            storage.middlePassHash = passHash
            // записываем проверочную строку
            saveMiddlePassCheckData(passHash, databaseConfig)
        } else {
            // сохраняем хэш пароля в оперативную память, на вермя "сеанса" работы приложения
            sensitiveDataProvider.saveMiddlePassHash(passHash)
        }
//        cryptInteractor.initCryptPass(passHash, isMiddleHash = true)
        cryptManager.setKeyFromMiddleHash(passHash)

        return None.toRight()
    }

    /**
     * Сохранение проверочной строки промежуточного хэша пароля в database.ini.
     */
    private fun saveMiddlePassCheckData(
        passHash: String,
        databaseConfig: DatabaseConfig,
    ): Boolean {
        val checkData = cryptManager.createMiddlePassHashCheckData(passHash)
        return databaseConfig.saveCheckData(checkData)
    }

    // сохраняем хэш пароля в бд (если установлена соответствующая опция)
    private suspend fun updateStorage(storage: TetroidStorage): Either<Failure, Boolean> {
        return ifEitherOrTrue(!storagesRepo.updateStorage(storage)) {
            Failure.Storage.Save.UpdateInBase.toLeft()
        }
    }

}