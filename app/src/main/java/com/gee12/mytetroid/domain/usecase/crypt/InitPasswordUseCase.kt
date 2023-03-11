package com.gee12.mytetroid.domain.usecase.crypt

import android.content.Context
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.openOutputStream
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.domain.provider.ISensitiveDataProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.domain.repo.StoragesRepo
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.FilePath
import java.lang.Exception

/**
 * Сохранение пароля в настройках и его установка для шифрования.
 */
class InitPasswordUseCase(
    private val context: Context,
    private val logger: ITetroidLogger,
    private val storageProvider: IStorageProvider,
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
                .onFailure {
                    logger.logFailure(it, show = true)
                }
        } else {
            // сохраняем хэш пароля в оперативную память, на вермя "сеанса" работы приложения
            sensitiveDataProvider.saveMiddlePassHash(passHash)
        }
        cryptManager.setKeyFromMiddleHash(passHash)

        return None.toRight()
    }

    /**
     * Сохранение проверочной строки промежуточного хэша пароля в database.ini.
     */
    private fun saveMiddlePassCheckData(
        passHash: String,
        databaseConfig: DatabaseConfig,
    ): Either<Failure, Boolean> {
        val checkData = cryptManager.createMiddlePassHashCheckData(passHash)

        val storageFolder = storageProvider.rootFolder
        val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
        val filePath = FilePath.File(storageFolderPath, Constants.DATABASE_INI_FILE_NAME)

        return try {
            val configFile = storageFolder?.child(
                context = context,
                path = filePath.fileName,
                requiresWriteAccess = true,
            ) ?: return Failure.File.Get(filePath).toLeft()

            val isSaved = configFile.openOutputStream(context, append = false)?.use {
                databaseConfig.saveCheckData(it, checkData)
            } ?: return Failure.File.Write(filePath).toLeft()

            isSaved.toRight()
        } catch (ex: Exception) {
            Failure.Storage.Save.PasswordCheckData(ex).toLeft()
        }
    }

    // сохраняем хэш пароля в бд (если установлена соответствующая опция)
    private suspend fun updateStorage(storage: TetroidStorage): Either<Failure, Boolean> {
        return ifEitherOrTrue(!storagesRepo.updateStorage(storage)) {
            Failure.Storage.Save.UpdateInBase.toLeft()
        }
    }

}