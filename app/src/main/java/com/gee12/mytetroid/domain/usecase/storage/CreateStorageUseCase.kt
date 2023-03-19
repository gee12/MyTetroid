package com.gee12.mytetroid.domain.usecase.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.manager.FavoritesManager
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.domain.usecase.node.CreateNodeUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidStorage

/**
 * Создание файлов хранилища, если оно новое.
 */
class CreateStorageUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val storageProvider: IStorageProvider,
    private val storageDataProcessor: IStorageDataProcessor,
    private val favoritesManager: FavoritesManager,
    private val createNodeUseCase: CreateNodeUseCase,
    private val getStorageTrashFolderUseCase: GetStorageTrashFolderUseCase,
) : UseCase<UseCase.None, CreateStorageUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
        val databaseConfig: DatabaseConfig,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val storage = params.storage

        return createStorageFiles(params)
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

    private suspend fun createStorageFiles(params: Params): Either<Failure, None> {
        val storage = params.storage
        val databaseConfig = params.databaseConfig
        val storageFolderUri = Uri.parse(storage.uri)
        var storageFolderPath = FilePath.FolderFull(storageFolderUri.path.orEmpty())

        val storageFolder: DocumentFile?
        try {
            storageFolder = DocumentFileCompat.fromUri(context, storageFolderUri)

            if (storageFolder != null && storageFolder.exists()) {
                storageFolderPath = FilePath.FolderFull(storageFolder.getAbsolutePath(context))

                // проверяем, пуст ли каталог
                if (!storageFolder.isEmpty(context)) {
                    return Failure.Storage.Create.FolderNotEmpty(storageFolderPath).toLeft()
                }
            } else {
                return Failure.Storage.Create.FolderIsMissing(storageFolderPath).toLeft()
            }

            logger.logDebug(resourcesProvider.getString(R.string.log_start_storage_creating_mask, storageFolderPath.fullPath))

            // сохраняем новый database.ini
            val iniFilePath = FilePath.File(storageFolderPath.fullPath, Constants.DATABASE_INI_FILE_NAME)

            val iniFile = storageFolder.makeFile(
                context = context,
                name = iniFilePath.fileName,
                mimeType = MimeType.TEXT,
                mode = CreateMode.REPLACE,
            ) ?: return Failure.File.Get(iniFilePath).toLeft()

            databaseConfig.setDefault()

            iniFile.openOutputStream(context, append = false)?.use {
                databaseConfig.save(it)
            }

            // создаем каталог base
            val baseFolderPath = FilePath.Folder(storageFolderPath.fullPath, Constants.BASE_DIR_NAME)

            storageFolder.makeFolder(
                context = context,
                name = baseFolderPath.folderName,
                mode = CreateMode.CREATE_NEW
            ) ?: return Failure.Folder.Create(baseFolderPath).toLeft()

            // создаем каталог корзины
            // игнорируем, если не смогли создать
            getStorageTrashFolderUseCase.run(
                GetStorageTrashFolderUseCase.Params(
                    storage = storage,
                    createIfNotExist = true,
                )
            )

        } catch (ex: Exception) {
            return Failure.Storage.Create.FilesError(storageFolderPath, ex).toLeft()
        }
        // добавляем корневую ветку
        storageDataProcessor.init()

        // инициализируем провайдер
        storageProvider.init(storageDataProcessor)
        storageProvider.setStorage(storage)
        storageProvider.setRootFolder(storageFolder)

        return createNodeUseCase.run(
            CreateNodeUseCase.Params(
                name = resourcesProvider.getString(R.string.title_first_node),
                parentNode = storageDataProcessor.getRootNode()
            )
        ).map { None }
    }

}