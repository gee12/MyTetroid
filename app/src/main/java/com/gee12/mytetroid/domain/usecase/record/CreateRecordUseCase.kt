package com.gee12.mytetroid.domain.usecase.record

import android.content.Context
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.manager.FavoritesManager
import com.gee12.mytetroid.domain.provider.IDataNameProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import com.gee12.mytetroid.model.FilePath
import java.io.IOException
import java.util.*

/**
 * Создание записи (пустую без текста):
 * 1) создание объекта в памяти
 * 2) создание каталога
 * 3) добавление в структуру mytetra.xml
 */
class CreateRecordUseCase(
    private val context: Context,
    private val logger: ITetroidLogger,
    private val dataNameProvider: IDataNameProvider,
    private val cryptManager: IStorageCryptManager,
    private val favoritesManager: FavoritesManager,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,
    private val parseRecordTagsUseCase: ParseRecordTagsUseCase,
    private val saveStorageUseCase: SaveStorageUseCase,
) : UseCase<TetroidRecord, CreateRecordUseCase.Params>() {

    data class Params(
        val name: String,
        val tagsString: String,
        val author: String,
        val url: String,
        val node: TetroidNode,
        val isFavor: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidRecord> {
        val name = params.name
        val tagsString = params.tagsString
        val author = params.author
        val url = params.url
        val node = params.node
        val isFavor = params.isFavor

        if (name.isEmpty()) {
            return Failure.Record.NameIsEmpty.toLeft()
        }
        logger.logOperStart(LogObj.RECORD, LogOper.CREATE)

        // генерируем уникальные идентификаторы
        val id = dataNameProvider.createUniqueId()
        val folderName = dataNameProvider.createUniqueId()
        val isEncrypted = node.isCrypted
        val record = TetroidRecord(
            isEncrypted,
            id,
            encryptFieldIfNeed(name, isEncrypted),
            encryptFieldIfNeed(tagsString, isEncrypted),
            encryptFieldIfNeed(author, isEncrypted),
            encryptFieldIfNeed(url, isEncrypted),
            Date(),
            folderName,
            TetroidRecord.DEF_FILE_NAME,
            node,
        )
        if (isEncrypted) {
            record.setDecryptedValues(name, tagsString, author, url)
            record.setIsDecrypted(true)
        }
        record.setIsFavorite(isFavor)
        record.setIsNew(true)

        // создаем каталог записи
        val recordFolder = getRecordFolderUseCase.run(
            GetRecordFolderUseCase.Params(
                record = record,
                createIfNeed = true,
                inTrash = false,
            )
        ).foldResult(
            onLeft = {
                return it.toLeft()
            },
            onRight = { it }
        )
        val recordFolderPath = recordFolder.getAbsolutePath(context)
        val filePath = FilePath.File(recordFolderPath, record.fileName)

        // создаем файл записи (пустой)
        val file = try {
            recordFolder.makeFile(
                context = context,
                name = record.fileName,
                mimeType = MimeType.TEXT,
                mode = CreateMode.REPLACE,
            ) ?: return Failure.File.Create(filePath).toLeft()
        } catch (ex: IOException) {
            return Failure.File.Create(filePath, ex).toLeft()
        }

        // добавляем запись в ветку (и соответственно, в дерево)
        node.addRecord(record)
        // перезаписываем структуру хранилища в файл
        return saveStorageUseCase.run()
            .flatMap {
                // добавляем метки в запись и в коллекцию меток
                parseRecordTagsUseCase.run(
                    ParseRecordTagsUseCase.Params(
                        record = record,
                        tagsString = tagsString
                    )
                ).flatMap {
                    // добавляем в избранное
                    if (isFavor) {
                        favoritesManager.add(record)
                    }
                    record.toRight()
                }
            }.onFailure {
                logger.logOperCancel(LogObj.RECORD, LogOper.CREATE)
                // удаляем запись из ветки
                node.deleteRecord(record)
                // удаляем файл записи
                file.delete()
                // удаляем каталог записи (пустой)
                recordFolder.delete()
            }
    }

    private fun encryptFieldIfNeed(fieldValue: String, isEncrypt: Boolean): String? {
        return if (isEncrypt) cryptManager.encryptTextBase64(fieldValue) else fieldValue
    }

}