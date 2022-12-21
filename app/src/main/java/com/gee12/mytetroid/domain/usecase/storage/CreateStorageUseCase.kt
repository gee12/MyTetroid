package com.gee12.mytetroid.domain.usecase.storage

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.isDirEmpty
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.FavoritesManager
import com.gee12.mytetroid.domain.usecase.node.CreateNodeUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage
import java.io.File

/**
 * Создание файлов хранилища, если оно новое.
 */
class CreateStorageUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val storageDataProcessor: IStorageDataProcessor,
    private val favoritesManager: FavoritesManager,
    private val createNodeUseCase: CreateNodeUseCase,
) : UseCase<UseCase.None, CreateStorageUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
        val databaseConfig: DatabaseConfig,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val storage = params.storage

        logger.logDebug(resourcesProvider.getString(R.string.log_start_storage_creating_mask, storage.path))

        return setPathToDatabaseIniConfig(params)
            .flatMap { createStorageFiles(params) }
            .onFailure {
                storage.isInited = false
            }.flatMap {
                storage.isInited = true
                storage.isLoaded = true
                storage.isNew = false

                // обнуляем список избранных записей для нового хранилища
                favoritesManager.reset()

                None.toRight()
            }
    }

    private fun setPathToDatabaseIniConfig(params: Params): Either<Failure, None> {
        val databaseIniFileName = makePath(params.storage.path, Constants.DATABASE_INI_FILE_NAME)
        params.databaseConfig.setFileName(databaseIniFileName)
        return None.toRight()
    }

    private suspend fun createStorageFiles(params: Params): Either<Failure, None> {
        val storagePath = params.storage.path
        val databaseConfig = params.databaseConfig

        try {
            val storageDir = File(storagePath)
            if (storageDir.exists()) {
                // проверяем, пуст ли каталог
                if (!storageDir.isDirEmpty()) {
                    return Failure.Storage.Create.FolderNotEmpty(pathToFolder = storagePath).toLeft()
                }
            } else {
                return Failure.Storage.Create.FolderIsMissing(pathToFolder = storagePath).toLeft()
            }

            // сохраняем новый database.ini
            if (!databaseConfig.saveDefault()) {
                return Failure.Storage.DatabaseConfig.Save(
                    pathToFile = databaseConfig.getFileName().orEmpty()
                ).toLeft()
            }

            // создаем каталог base
            val baseDir = File(storagePath, Constants.BASE_DIR_NAME)
            if (!baseDir.mkdir()) {
                return Failure.Storage.Create.BaseFolder(pathToFolder = baseDir.path).toLeft()
            }

        } catch (ex: Exception) {
            Failure.Storage.Create.FilesError(pathToFolder = storagePath, ex).toLeft()
        }
        // добавляем корневую ветку
        storageDataProcessor.init()

        return createNodeUseCase.run(
            CreateNodeUseCase.Params(
                name = resourcesProvider.getString(R.string.title_first_node),
                parentNode = storageDataProcessor.getRootNode()
            )
        ).map { None }
    }

}