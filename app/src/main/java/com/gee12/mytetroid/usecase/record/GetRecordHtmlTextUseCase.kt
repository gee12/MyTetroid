package com.gee12.mytetroid.usecase.record

import android.net.Uri
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.data.crypt.IStorageCrypter
import com.gee12.mytetroid.helpers.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidRecord
import java.io.File

/**
 * Получение содержимого записи в виде "сырого" html.
 */
class GetRecordHtmlTextUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val checkRecordFolderUseCase: CheckRecordFolderUseCase,
) : UseCase<String, GetRecordHtmlTextUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val pathToRecordFolder: String,
        val showMessage: Boolean,
        val crypter: IStorageCrypter,
    )

    override suspend fun run(params: Params): Either<Failure, String> {
        val record = params.record
        val folderPath = params.pathToRecordFolder
        val showMessage = params.showMessage

        logger.logDebug(resourcesProvider.getString(R.string.start_record_file_reading_mask, record.id))
        // проверка существования каталога записи
        checkRecordFolderUseCase.run(
            CheckRecordFolderUseCase.Params(
                folderPath = folderPath,
                isCreate = true,
                showMessage = showMessage,
            )
        ).onFailure {
            return it.toLeft()
        }
        val filePath = makePath(folderPath, record.fileName)
        val uri = try {
            Uri.parse(filePath)
        } catch (ex: Exception) {
//            logger.logError(resourcesProvider.getString(R.string.error_generate_record_uri_path_mask) + filePath, ex)
            return Failure.File.CreateUriPath(path = filePath).toLeft()
        }
        // проверка существования файла записи
        val file = File(uri.path.orEmpty())
        if (!file.exists()) {
//            logger.logWarning(resourcesProvider.getString(R.string.log_record_file_is_missing), showMessage)
            return Failure.File.NotExist(filePath).toLeft()
        }
        return if (record.isCrypted) {
            if (record.isDecrypted) {
                readAndDecryptRecordFile(
                    uri = uri,
                    crypter = params.crypter
                )
            } else {
                Failure.Record.Read.NotDecrypted().toLeft()
            }
        } else {
            try {
                FileUtils.readTextFile(uri).toRight()
            } catch (ex: Exception) {
                Failure.File.Read(path = uri.toString(), ex).toLeft()
            }
        }
    }

    private fun readAndDecryptRecordFile(uri: Uri, crypter: IStorageCrypter): Either<Failure, String> {
        val bytes: ByteArray? = try {
            FileUtils.readFile(uri)
        } catch (ex: Exception) {
            return Failure.File.Read(path = uri.toString(), ex).toLeft()
        }
        if (bytes == null) {
//                    logger.logError(resourcesProvider.getString(R.string.log_error_decrypt_record_file) + filePath)
            return Failure.Decrypt.File(fileName = uri.toString()).toLeft()
        } else if (bytes.isEmpty()) {
            // файл пуст
            return "".toRight()
        }
        // расшифровываем содержимое файла
        logger.logDebug(resourcesProvider.getString(R.string.log_start_record_text_decrypting))
        val res = crypter.decryptText(bytes)
        if (res == null) {
//                    logger.logError(resourcesProvider.getString(R.string.log_error_decrypt_record_file) + filePath)
            return Failure.Decrypt.File(fileName = uri.toString()).toLeft()
        } else {
            return res.toRight()
        }
    }

}