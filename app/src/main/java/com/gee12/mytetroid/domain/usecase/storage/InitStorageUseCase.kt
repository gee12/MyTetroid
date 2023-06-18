package com.gee12.mytetroid.domain.usecase.storage

import android.content.Context
import android.net.Uri
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.openInputStream
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.parseUri
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.domain.manager.FavoritesManager
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidStorage

/**
 * Создание файлов хранилища, если оно новое.
 */
class InitStorageUseCase(
    private val context: Context,
    private val favoritesManager: FavoritesManager,
) : UseCase<UseCase.None, InitStorageUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
        val databaseConfig: DatabaseConfig,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val storage = params.storage

        return loadIniConfig(params)
            .onFailure {
                storage.isInited = false
            }.flatMap {
                storage.isInited = true

                favoritesManager.initIfNeed()

                None.toRight()
            }
    }

    private fun loadIniConfig(params: Params): Either<Failure, None> {
        val storage = params.storage
        val databaseConfig = params.databaseConfig
        val storageFolderUri = storage.uri.parseUri()
        val storageFolderPath = FilePath.FolderFull(storageFolderUri.path.orEmpty())
        val iniFilePath = FilePath.File(storageFolderPath.fullPath, Constants.DATABASE_INI_FILE_NAME)

        return try {
            val storageFolder = DocumentFileCompat.fromUri(context, storageFolderUri)
                ?: return Failure.Folder.Get(storageFolderPath).toLeft()

            val iniDocumentFile = storageFolder.child(
                context = context,
                path = iniFilePath.fileName,
                requiresWriteAccess = false,
            ) ?: return Failure.File.Get(iniFilePath).toLeft()

            // загружаем database.ini
            iniDocumentFile.openInputStream(context)?.use { stream ->
                databaseConfig.load(stream)
            } ?: Failure.File.Read(iniFilePath).toLeft()

            None.toRight()
        } catch (ex: Exception) {
            Failure.Storage.Init(storageFolderPath, ex).toLeft()
        }
    }

}