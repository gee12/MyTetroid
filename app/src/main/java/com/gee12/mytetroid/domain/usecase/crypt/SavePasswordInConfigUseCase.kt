package com.gee12.mytetroid.domain.usecase.crypt

import android.content.Context
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.openOutputStream
import com.anggrayudi.storage.file.recreateFile
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.data.crypt.Base64
import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidStorage
import java.lang.Exception

/**
 * Сохранение проверочного хэша пароля и сопутствующих данных в database.ini.
 */
class SavePasswordInConfigUseCase(
    private val context: Context,
    private val storageProvider: IStorageProvider,
    private val cryptManager: IStorageCryptManager,
) : UseCase<UseCase.None, SavePasswordInConfigUseCase.Params>() {

    data class Params(
        val password: String,
    )

    data class PasswordData(
        val passwordHash: String,
        val salt: String,
        val middleHashCheckData: String?,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val storage = storageProvider.storage
            ?: return Failure.Storage.StorageNotInited.toLeft()

        return createPasswordHash(password = params.password).flatMap { data ->
            saveInDatabaseConfig(
                storage = storage,
                data = data,
            )
        }
    }

    private fun createPasswordHash(password: String): Either<Failure, PasswordData> {
        return try {
            val salt = Utils.createRandomBytes(32)
            val hash = Crypter.calculatePBKDF2Hash(password, salt)
            val hashString = Base64.encodeToString(hash, false)
            val checkDataString = cryptManager.createMiddlePassHashCheckData(
                passHash = hashString,
            )
            PasswordData(
                salt = Base64.encodeToString(salt, false),
                passwordHash = hashString,
                middleHashCheckData = checkDataString,
            ).toRight()
        } catch (ex: Exception) {
            Failure.Storage.Save.PasswordHash(ex).toLeft()
        }
    }

    private fun saveInDatabaseConfig(
        storage: TetroidStorage,
        data: PasswordData,
    ): Either<Failure, None> {
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

            // сохранение хэша пароля и соли для проверки введенного пароля
            databaseConfig.setPasswordHashAndSalt(
                passHash = data.passwordHash,
                salt = data.salt,
                cryptMode = true,
            )

            if (storage.isSavePassLocal) {
                // сохранение данных для проверки сохраненного промежуточного хэша пароля
                databaseConfig.setMiddleHashCheckData(
                    checkData = data.middleHashCheckData,
                )
            }

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

}