package com.gee12.mytetroid.domain.usecase.crypt

import android.content.Context
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.openOutputStream
import com.anggrayudi.storage.file.recreateFile
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.domain.repo.StoragesRepo
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidStorage
import java.lang.Exception

/**
 * Сохранение проверочной строки промежуточного хэша пароля в database.ini.
 */
class SaveMiddlePasswordHashUseCase(
    private val context: Context,
    private val storageProvider: IStorageProvider,
    private val storagesRepo: StoragesRepo,
    private val cryptManager: IStorageCryptManager,
) : UseCase<UseCase.None, SaveMiddlePasswordHashUseCase.Params>() {

    data class Params(
        val middlePasswordHash: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val middlePasswordHash = params.middlePasswordHash
        val storage = storageProvider.storage
            ?: return Failure.Storage.StorageNotInited.toLeft()

        return createCheckData(middlePasswordHash).flatMap { data ->
            saveInDatabaseConfig(checkData = data).flatMap {
                storage.isSavePassLocal = true
                storage.middlePassHash = middlePasswordHash

                updateStorageInDb(storage)
            }
        }
    }

    private fun createCheckData(middlePasswordHash: String): Either<Failure, String> {
        return try {
            cryptManager.createMiddlePassHashCheckData(
                passHash = middlePasswordHash,
            )?.toRight()
                ?: Failure.Storage.Save.PasswordCheckData().toLeft()
        } catch (ex: Exception) {
            Failure.Storage.Save.PasswordCheckData(ex).toLeft()
        }
    }

    private fun saveInDatabaseConfig(checkData: String?): Either<Failure, None> {
        val databaseConfig = storageProvider.databaseConfig
        val storageFolder = storageProvider.rootFolder

        return try {
            val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
            val filePath = FilePath.File(storageFolderPath, Constants.DATABASE_INI_FILE_NAME)
            val configFile = storageFolder?.child(
                context = context,
                path = filePath.fileName,
                requiresWriteAccess = true,
            ) ?: return Failure.File.Get(filePath).toLeft()

            // сохранение данных для проверки сохраненного промежуточного хэша пароля
            databaseConfig.setMiddleHashCheckData(
                checkData = checkData,
            )

            // баг при перезаписи database.ini с помощью OutputStream
            configFile.recreateFile(context)

            configFile.openOutputStream(context, append = false)?.use {
                databaseConfig.save(it)
            } ?: return Failure.File.Write(filePath).toLeft()

            None.toRight()
        } catch (ex: Exception) {
            Failure.Storage.Save.PasswordCheckData(ex).toLeft()
        }
    }

    private suspend fun updateStorageInDb(storage: TetroidStorage): Either<Failure, None> {
        return ifEitherOrNone(!storagesRepo.updateStorage(storage)) {
            Failure.Storage.Save.UpdateInBase.toLeft()
        }
    }

}