package com.gee12.mytetroid.domain.usecase.attach

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.domain.IStorageCrypter
import com.gee12.mytetroid.domain.provider.IRecordPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.domain.provider.IStorageSettingsProvider
import java.io.File
import java.io.IOException
import kotlin.Exception

/**
 * Получение прикрепленного файла с предварительной расшифровкой (если необходимо).
 */
class GetFileFromAttachUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val storageCrypter: IStorageCrypter,
    private val recordPathProvider: IRecordPathProvider,
    private val storageSettingsProvider: IStorageSettingsProvider,
) : UseCase<File, GetFileFromAttachUseCase.Params>() {

    data class Params(
        val attach: TetroidFile
    )

    override suspend fun run(params: Params): Either<Failure, File> {
        val file = params.attach

        logger.logDebug(resourcesProvider.getString(R.string.log_start_attach_file_opening) + file.id)
        val record = file.record
        val fileDisplayName = file.name
        val ext = FileUtils.getExtensionWithComma(fileDisplayName)
        val fileIdName = file.id + ext
        val fullFileName: String = recordPathProvider.getPathToFileInRecordFolder(record, fileIdName)
        var srcFile = File(fullFileName)
//        srcFile = try {
//            File(fullFileName)
//        } catch (ex: Exception) {
//            logger.logError(resourcesProvider.getString(R.string.log_error_file_open) + fullFileName, true)
//            logger.logError(ex, false)
//            return false
//        }
        //
        logger.log(resourcesProvider.getString(R.string.log_open_file) + fullFileName, false)
        if (!srcFile.exists()) {
//            logger.logError(resourcesProvider.getString(R.string.log_file_is_absent) + fullFileName, true)
            return Failure.File.NotExist(path = srcFile.path).toLeft()
        }
        // если запись зашифрована
        if (record.isCrypted && storageSettingsProvider.isDecryptAttachesToTempFolder()) {
            // создаем временный файл
//                File tempFile = createTempCacheFile(context, fileIdName);
//                File tempFile = new File(String.format("%s%s/_%s", getStoragePathBase(), record.getDirName(), fileIdName));
//                File tempFile = createTempExtStorageFile(context, fileIdName);
            val tempFolderPath = recordPathProvider.getPathToRecordFolderInTrash(record)
            val tempFolder = File(tempFolderPath)
            if (!tempFolder.exists() && !tempFolder.mkdirs()) {
                logger.logError(resourcesProvider.getString(R.string.log_could_not_create_temp_dir) + tempFolderPath, true)
            }
            val tempFile = File(tempFolder, fileIdName)

            // расшифровываем во временный файл
            val existOrCreated = if (!tempFile.exists()) {
                try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    tempFile.createNewFile()
                } catch (ex: Exception) {
                    return Failure.File.Create(filePath = tempFile.path, ex).toLeft()
                }
            } else {
                true
            }

            try {
                if (existOrCreated && storageCrypter.encryptDecryptFile(
                        srcFile = srcFile,
                        destFile = tempFile,
                        encrypt = false
                    )
                ) {
                    srcFile = tempFile
                } else {
//                    logger.logError(resourcesProvider.getString(R.string.log_could_not_decrypt_file) + fullFileName, true)
                    return Failure.Decrypt.File(fileName = srcFile.path).toLeft()
                }
            } catch (ex: IOException) {
//                logger.logError(resourcesProvider.getString(R.string.log_file_decryption_error) + ex.message, true)
                return Failure.Decrypt.File(fileName = srcFile.path).toLeft()
            }
        }

        return srcFile.toRight()
    }

}