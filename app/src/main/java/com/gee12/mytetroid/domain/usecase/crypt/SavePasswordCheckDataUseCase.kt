package com.gee12.mytetroid.domain.usecase.crypt

import android.content.Context
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.openOutputStream
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.data.crypt.Base64
import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FilePath
import java.lang.Exception

/**
 * Сохранение проверочного хэша пароля и сопутствующих данных в database.ini.
 */
class SavePasswordCheckDataUseCase(
    private val context: Context,
    private val storageProvider: IStorageProvider,
) : UseCase<Boolean, SavePasswordCheckDataUseCase.Params>() {

    data class Params(
        val databaseConfig: DatabaseConfig,
        val password: String,
    )

    override suspend fun run(params: Params): Either<Failure, Boolean> {
        val databaseConfig = params.databaseConfig

        return try {
            val salt = Utils.createRandomBytes(32)
            val passHash = Crypter.calculatePBKDF2Hash(params.password, salt)

            val storageFolder = storageProvider.rootFolder
            val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
            val filePath = FilePath.File(storageFolderPath, Constants.DATABASE_INI_FILE_NAME)
            val configFile = storageFolder?.child(
                context = context,
                path = filePath.fileName,
                requiresWriteAccess = true,
            ) ?: return Failure.File.Get(filePath).toLeft()

            val isSaved = configFile.openOutputStream(context, append = false)?.use {
                databaseConfig.savePass(
                    outputStream = it,
                    passHash = Base64.encodeToString(passHash, false),
                    salt = Base64.encodeToString(salt, false),
                    cryptMode = true,
                )
            } ?: return Failure.File.Write(filePath).toLeft()

            isSaved.toRight()
        } catch (ex: Exception) {
            Failure.Storage.Save.PasswordHash(ex).toLeft()
        }
    }

}