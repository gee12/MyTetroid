package com.gee12.mytetroid.domain.usecase.storage

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.domain.manager.FavoritesManager
import com.gee12.mytetroid.model.TetroidStorage

/**
 * Создание файлов хранилища, если оно новое.
 */
class InitStorageUseCase(
    private val favoritesManager: FavoritesManager,
) : UseCase<UseCase.None, InitStorageUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
        val databaseConfig: DatabaseConfig,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val storage = params.storage

        return setPathToDatabaseIniConfig(params)
            .flatMap {
                loadConfig(params)
            }.onFailure {
                storage.isInited = false
            }.flatMap {
                storage.isInited = true

                favoritesManager.initIfNeed()

                None.toRight()
            }
    }

    private fun loadConfig(params: Params): Either<Failure.Storage, None> {
        val storage = params.storage
        val databaseConfig = params.databaseConfig

        return try {
            // загружаем database.ini
            if (params.databaseConfig.load()) {
                None.toRight()
            } else {
                Failure.Storage.DatabaseConfig.Load(
                    pathToFile = databaseConfig.getFileName().orEmpty()
                ).toLeft()
            }
        } catch (ex: Exception) {
            Failure.Storage.Init(storagePath = storage.path, ex).toLeft()
        }
    }

    private fun setPathToDatabaseIniConfig(params: Params): Either<Failure, None> {
        val databaseIniFileName = makePath(params.storage.path, Constants.DATABASE_INI_FILE_NAME)
        params.databaseConfig.setFileName(databaseIniFileName)
        return None.toRight()
    }


}