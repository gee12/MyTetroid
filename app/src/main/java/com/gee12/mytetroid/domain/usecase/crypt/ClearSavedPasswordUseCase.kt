package com.gee12.mytetroid.domain.usecase.crypt

import android.content.Context
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.openOutputStream
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FilePath

/**
 * Сброс сохраненного хэша пароля и его проверочных данных.
 */
class ClearSavedPasswordUseCase(
    private val context: Context,
    private val storageProvider: IStorageProvider,
) : UseCase<UseCase.None, ClearSavedPasswordUseCase.Params>() {

    object Params

    override suspend fun run(params: Params): Either<Failure, None> {
        val storage = storageProvider.storage
        val databaseConfig = storageProvider.databaseConfig

        storage?.middlePassHash = null

        return try {
            val storageFolder = storageProvider.rootFolder
            val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
            val filePath = FilePath.File(storageFolderPath, Constants.DATABASE_INI_FILE_NAME)
            val configFile = storageFolder?.child(
                context = context,
                path = filePath.fileName,
                requiresWriteAccess = true,
            ) ?: return Failure.File.Get(filePath).toLeft()

            configFile.openOutputStream(context, append = false)?.use {
                // очистка сохраненного проверочнго хэша пароля
                databaseConfig.savePassword(
                    outputStream = it,
                    passHash = null,
                    salt = null,
                    cryptMode = false,
                )
                // очистка сохраненной проверочной строки промежуточного хэша пароля
                databaseConfig.saveCheckData(
                    outputStream = it,
                    checkData = null,
                )

            } ?: return Failure.File.Write(filePath).toLeft()

            None.toRight()
        } catch (ex: Exception) {
            Failure.Storage.Save.PasswordHash(ex).toLeft()
        }
    }

}