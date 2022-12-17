package com.gee12.mytetroid.usecase.record

import android.net.Uri
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.data.crypt.IStorageCrypter
import com.gee12.mytetroid.providers.IRecordPathProvider
import com.gee12.mytetroid.interactors.FavoritesManager
import com.gee12.mytetroid.providers.IDataNameProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.usecase.tag.ParseRecordTagsUseCase
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Создание записи (пустую без текста):
 * 1) создание объекта в памяти
 * 2) создание каталога
 * 3) добавление в структуру mytetra.xml
 */
class CreateRecordUseCase(
    private val logger: ITetroidLogger,
    private val dataNameProvider: IDataNameProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val crypter: IStorageCrypter,
    private val favoritesManager: FavoritesManager,
    private val checkRecordFolderUseCase: CheckRecordFolderUseCase,
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
        val crypted = node.isCrypted
        val record = TetroidRecord(
            crypted, id,
            crypter.encryptTextBase64(name),
            crypter.encryptTextBase64(tagsString),
            crypter.encryptTextBase64(author),
            crypter.encryptTextBase64(url),
            Date(), folderName, TetroidRecord.DEF_FILE_NAME, node
        )
        if (crypted) {
            record.setDecryptedValues(name, tagsString, author, url)
            record.setIsDecrypted(true)
        }
        record.setIsFavorite(isFavor)
        record.setIsNew(true)
        // создаем каталог записи
        val folderPath = recordPathProvider.getPathToRecordFolder(record)
        checkRecordFolderUseCase.run(
            CheckRecordFolderUseCase.Params(
                folderPath = folderPath,
                isCreate = true
            )
        ).onFailure {
            return it.toLeft()
        }
        val folder = File(folderPath)
        // создаем файл записи (пустой)
        val filePath = makePath(folderPath, record.fileName)
        val fileUri = try {
            Uri.parse(filePath)
        } catch (ex: Exception) {
            return Failure.File.CreateUriPath(path = filePath).toLeft()
        }
        val file = File(fileUri.path!!)
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            file.createNewFile()
        } catch (ex: IOException) {
            return Failure.File.Create(filePath = file.path, ex).toLeft()
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
                folder.delete()
            }
    }

}