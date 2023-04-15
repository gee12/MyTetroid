package com.gee12.mytetroid.domain.usecase.storage

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.model.TetroidStorage

/**
 * Непосредственная инициализация хранилища, с созданием файлов, если оно новое.
 * Загрузка параметров из файла database.ini и инициализация переменных.
 */
class InitOrCreateStorageUseCase(
    private val createStorageUseCase: CreateStorageUseCase,
    private val initStorageUseCase: InitStorageUseCase,
) : UseCase<InitOrCreateStorageUseCase.Result, InitOrCreateStorageUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
        val databaseConfig: DatabaseConfig,
    )

    sealed class Result {
        object Inited : Result()
        object Created : Result()
    }

    override suspend fun run(params: Params): Either<Failure, Result> {
        val storage = params.storage
        val databaseConfig = params.databaseConfig

        //storage.isLoaded = false
        return if (storage.isNew) {
            createStorageUseCase.run(
                CreateStorageUseCase.Params(
                    storage = storage,
                    databaseConfig = databaseConfig
                )
            ).map { Result.Created }
        } else {
            initStorageUseCase.run(
                InitStorageUseCase.Params(
                    storage = storage,
                    databaseConfig = databaseConfig
                )
            ).map { Result.Inited}
        }
    }

}