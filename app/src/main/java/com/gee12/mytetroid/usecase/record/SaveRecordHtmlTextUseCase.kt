package com.gee12.mytetroid.usecase.record

import android.net.Uri
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.data.crypt.IStorageCrypter
import com.gee12.mytetroid.helpers.IRecordPathProvider
import com.gee12.mytetroid.helpers.RecordPathProvider
import com.gee12.mytetroid.model.TetroidRecord
import java.io.IOException

/**
 * Сохранение содержимого записи в файл.
 */
class SaveRecordHtmlTextUseCase(
    private val recordPathProvider: IRecordPathProvider,
    private val crypter: IStorageCrypter,
    private val checkRecordFolderUseCase: CheckRecordFolderUseCase,
) : UseCase<UseCase.None, SaveRecordHtmlTextUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
//        val pathToRecordFolder: String,
        val html: String,
//        val storageCrypter: IStorageCrypter,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val record = params.record
        val html = params.html

        // проверка существования каталога записи
        val folderPath = recordPathProvider.getPathToRecordFolder(record)
        checkRecordFolderUseCase.run(
            CheckRecordFolderUseCase.Params(
                folderPath = folderPath,
                isCreate = true,
                showMessage = true,
            )
        ).onFailure {
            return it.toLeft()
        }
        // формирование пути к файлу записи
        val filePath = makePath(folderPath, record.fileName)
        val uri = try {
            Uri.parse(filePath)
        } catch (ex: Exception) {
//            logger.logError(resourcesProvider.getString(R.string.error_generate_record_uri_path_mask, filePath), ex)
            return Failure.File.CreateUriPath(path = filePath).toLeft()
        }
        // запись файла с шифрованием при необходимости
        try {
            if (record.isCrypted) {
                val res = crypter.encryptTextBytes(html)
                FileUtils.writeFile(uri, res)
            } else {
                FileUtils.writeFile(uri, html)
            }
        } catch (ex: IOException) {
//            logger.logError(resourcesProvider.getString(R.string.log_error_write_to_record_file) + filePath, ex)
            return Failure.File.Write(fileName = record.fileName, path = filePath, ex).toLeft()
        }
        return None.toRight()
    }

}