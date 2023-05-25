package com.gee12.mytetroid.domain.usecase.record

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.openInputStream
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.orFalse
import com.gee12.mytetroid.common.extensions.readBytes
import com.gee12.mytetroid.common.extensions.readText
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.domain.provider.IRecordPathProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FilePath
import java.io.InputStream

/**
 * Получение содержимого записи в виде "сырого" html.
 */
class GetRecordHtmlTextUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val storageProvider: IStorageProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val cryptManager: IStorageCryptManager,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,
) : UseCase<String, GetRecordHtmlTextUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val recordFolder: DocumentFile?,
        val showMessage: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, String> {
        val storage = storageProvider.storage
        val record = params.record
        val showMessage = params.showMessage

        logger.logDebug(resourcesProvider.getString(R.string.start_record_file_reading_mask, record.id))

        val recordFolder = params.recordFolder ?: getRecordFolderUseCase.run(
            GetRecordFolderUseCase.Params(
                record = record,
                createIfNeed = true,
                inTrash = record.isTemp,
                showMessage = showMessage,
            )
        ).foldResult(
            onLeft = {
                return it.toLeft()
            },
            onRight = { it }
        )
        val filePath = FilePath.File(recordFolder.getAbsolutePath(context), record.fileName)

        val file = recordFolder.child(
            context = context,
            path = filePath.fileName,
            requiresWriteAccess = !storage?.isReadOnly.orFalse()
        ) ?: return Failure.File.Get(filePath).toLeft()

        // проверка существования файла записи
        if (!file.exists()) {
            return Failure.File.NotExist(filePath).toLeft()
        }

        return file.openInputStream(context)?.use { inputStream ->
            if (record.isCrypted) {
                if (record.isDecrypted) {
                    readAndDecryptRecordFile(inputStream, filePath)
                } else {
                    Failure.Record.Read.NotDecrypted().toLeft()
                }
            } else {
                readRecordFile(inputStream, filePath)
            }
        } ?: return Failure.File.Read(filePath).toLeft()
    }

    private fun readRecordFile(
        inputStream: InputStream,
        filePath: FilePath,
    ): Either<Failure, String> {
        return try {
            inputStream.readText().toRight()
        } catch (ex: Exception) {
            Failure.File.Read(filePath, ex).toLeft()
        }
    }

    private fun readAndDecryptRecordFile(
        inputStream: InputStream,
        filePath: FilePath,
    ): Either<Failure, String> {

        val bytes = try {
            inputStream.readBytes()
        } catch (ex: Exception) {
            return Failure.File.Read(filePath, ex).toLeft()
        }
        if (bytes.isEmpty()) {
            // файл пуст
            return "".toRight()
        }
        // расшифровываем содержимое файла
        logger.logDebug(resourcesProvider.getString(R.string.log_start_record_text_decrypting))
        return cryptManager.decryptText(bytes)?.toRight()
            ?: Failure.Decrypt.File(filePath).toLeft()
    }

}