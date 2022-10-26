package com.gee12.mytetroid.usecase.storage

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.toLeft
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.interactors.FavoritesInteractor
import com.gee12.mytetroid.interactors.StorageInteractor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage

/**
 * Непосредственная инициализация хранилища, с созданием файлов, если оно новое.
 * Загрузка параметров из файла database.ini и инициализация переменных.
 */
class InitOrCreateStorageUseCase(
    private val logger: ITetroidLogger,
    private val resourcesProvider: IResourcesProvider,
    private val storageProvider: IStorageProvider,
    private val storageInteractor: StorageInteractor,
    private val favoritesInteractor: FavoritesInteractor,
) : UseCase<InitOrCreateStorageUseCase.Result, InitOrCreateStorageUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
        val databaseConfig: DatabaseConfig,
    )

    sealed class Result {
        object InitStorage : Result()
        object CreateStorage : Result()
    }

    override suspend fun run(params: Params): Either<Failure, Result> {
        val storage = params.storage
        val databaseConfig = params.databaseConfig

        //storage.isLoaded = false
        val databaseConfigFileName = storageInteractor.getPathToDatabaseIniConfig()
        databaseConfig.setFileName(databaseConfigFileName)

        return try {
            if (storage.isNew) {
                logger.logDebug(resourcesProvider.getString(R.string.log_start_storage_creating_mask, storage.path))
                val res = storageInteractor.createStorage(storage)
                storage.isInited = res
                if (res) {
                    storage.isNew = false
                    storage.isLoaded = true
                    logger.log(R.string.log_storage_created, true)
                    // обнуляем список избранных записей для нового хранилища
                    favoritesInteractor.reset()
                    Result.CreateStorage.toRight()
                } else {
                    Failure.Storage.Create(
                        storageName = storage.name
                    ).toLeft()
                }
            } else {
                // загружаем database.ini
                val res = databaseConfig.load()
                storage.isInited = res
                if (res) {
                    // FIXME: изменить ?
                    // получаем id избранных записей из настроек
                    if (storage.id != storageProvider.storage?.id) {
                        favoritesInteractor.init()
                    }
                    Result.InitStorage.toRight()
                } else {
                    Failure.Storage.DatabaseConfig.Load(
                        storagePath = databaseConfigFileName
                    ).toLeft()
                }
            }
        } catch (ex: Exception) {
            if (storage.isNew) {
                Failure.Storage.Create(storageName = storage.name, ex).toLeft()
            } else {
                Failure.Storage.Init(storagePath = storage.path, ex).toLeft()
            }
        }
    }

}