package com.gee12.mytetroid.domain.usecase.record

import android.content.Context
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.openOutputStream
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidRecord

/**
 * Сохранение содержимого записи в файл.
 */
class SaveRecordHtmlTextUseCase(
    private val context: Context,
    private val cryptManager: IStorageCryptManager,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,
) : UseCase<UseCase.None, SaveRecordHtmlTextUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val html: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val record = params.record
        val html = params.html

        val recordFolder = getRecordFolderUseCase.run(
            GetRecordFolderUseCase.Params(
                record = record,
                createIfNeed = true,
                inTrash = record.isTemp,
                showMessage = true,
            )
        ).foldResult(
            onLeft = {
                return it.toLeft()
            },
            onRight = { it }
        )
        val folderPath = recordFolder.getAbsolutePath(context)
        val filePath = FilePath.File(folderPath, record.fileName)

        // формирование пути к файлу записи
        val file = recordFolder.child(
            context = context,
            path = record.fileName,
            requiresWriteAccess = true,
        ) ?: return Failure.File.Get(filePath).toLeft()

        // запись файла с шифрованием при необходимости
        try {
            val bytes = if (record.isCrypted) {
                cryptManager.encryptTextBytes(html)
            } else {
                html.toByteArray()
            }
            file.openOutputStream(context, append = false)?.use { outputStream ->
                outputStream.write(bytes)
            }
        } catch (ex: Exception) {
            return Failure.File.Write(filePath, ex).toLeft()
        }
        return None.toRight()
    }

}