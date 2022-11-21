package com.gee12.mytetroid.interactors

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.onFailure
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.helpers.IResourcesProvider
import com.gee12.mytetroid.helpers.IStoragePathHelper
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.usecase.node.CreateNodeUseCase
import java.io.File

/**
 * Создается для конкретного хранилища.
 */
class StorageInteractor(
    private val logger: ITetroidLogger,
    private val resourcesProvider: IResourcesProvider,
    private val storagePathHelper: IStoragePathHelper,
    private val storageDataProcessor: IStorageDataProcessor,
    private val createNodeUseCase: CreateNodeUseCase,
) {

    /**
     * Создание файлов хранилища, если оно новое.
     * @param storage
     */
    // TODO: CreateStorageUseCase
    suspend fun createStorage(storage: TetroidStorage): Boolean {
        try {
            if (storage.isNew) {
                if (createStorageFiles(storage.path)) {
                    storage.isNew = false
                    return true
                }
            }
        } catch (ex: Exception) {
            logger.logError(ex)
        }
        return false
    }

    /**
     * Создание файлов хранилища в указанном расположении.
     * @param storagePath
     */
    private suspend fun createStorageFiles(storagePath: String): Boolean {
        val storageDir = File(storagePath)
        if (storageDir.exists()) {
            // проверяем, пуст ли каталог
            if (!FileUtils.isDirEmpty(storageDir)) {
                logger.log(R.string.log_dir_not_empty)
                return false
            }
        } else {
            logger.log(R.string.log_dir_is_missing, LogType.ERROR)
            return false
        }

        // сохраняем новый database.ini
        val databaseConfig = DatabaseConfig(logger).apply {
            setFileName(makePath(storagePath, Constants.DATABASE_INI_FILE_NAME))
        }
        if (!databaseConfig.saveDefault()) {
            return false
        }

        // создаем каталог base
        if (!createBaseFolder(storagePath)) {
            return false
        }

        // добавляем корневую ветку
        storageDataProcessor.init()

        createNodeUseCase.run(
            CreateNodeUseCase.Params(
                name = resourcesProvider.getString(R.string.title_first_node),
                parentNode = storageDataProcessor.getRootNode()
            )
        ).onFailure { failure ->
            logger.logFailure(failure, show = false)
            return false
        }

        return true
    }

    private fun createBaseFolder(storagePath: String): Boolean {
        val baseDir = File(storagePath, Constants.BASE_DIR_NAME)
        return baseDir.mkdir()
    }

    fun getPathToDatabaseIniConfig() = storagePathHelper.getPathToDatabaseIniConfig()

}