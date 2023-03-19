package com.gee12.mytetroid.domain.usecase.crypt

import android.content.Context
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.openOutputStream
import com.anggrayudi.storage.file.recreateFile
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.domain.repo.StoragesRepo
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidStorage

/**
 * Сброс сохраненного хэша пароля из database.ini и бд.
 */
class ClearSavedPasswordHashUseCase(
    private val context: Context,
    private val storageProvider: IStorageProvider,
    private val storagesRepo: StoragesRepo,
) : UseCase<UseCase.None, ClearSavedPasswordHashUseCase.Params>() {

    data class Params(
        val isDropSavePasswordLocal: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val storage = storageProvider.storage
            ?: return Failure.Storage.StorageNotInited.toLeft()

        return clearInDatabaseConfig().flatMap {
            storage.middlePassHash = null

            if (params.isDropSavePasswordLocal) {
                storage.isSavePassLocal = false
            }

            updateStorageInDb(storage)
        }
    }

    private fun clearInDatabaseConfig(): Either<Failure, None> {
        val databaseConfig = storageProvider.databaseConfig

        return try {
            val storageFolder = storageProvider.rootFolder
            val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
            val filePath = FilePath.File(storageFolderPath, Constants.DATABASE_INI_FILE_NAME)
            val configFile = storageFolder?.child(
                context = context,
                path = filePath.fileName,
                requiresWriteAccess = true,
            ) ?: return Failure.File.Get(filePath).toLeft()

            // очистка данных для проверки сохраненного промежуточного хэша пароля
            databaseConfig.setMiddleHashCheckData(
                checkData = null,
            )

            // баг при перезаписи database.ini с помощью OutputStream
            configFile.recreateFile(context)

            configFile.openOutputStream(context, append = false)?.use {
                databaseConfig.save(it)
            } ?: return Failure.File.Write(filePath).toLeft()

            None.toRight()
        } catch (ex: Exception) {
            Failure.Storage.Save.PasswordHash(ex).toLeft()
        }
    }

    private suspend fun updateStorageInDb(storage: TetroidStorage): Either<Failure, None> {
        return ifEitherOrNone(!storagesRepo.updateStorage(storage)) {
            Failure.Storage.Save.UpdateInBase.toLeft()
        }
    }

}