package com.gee12.mytetroid.usecase.record

import android.net.Uri
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.helpers.IRecordPathProvider
import com.gee12.mytetroid.helpers.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.providers.IDataNameProvider
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Создание временной записи (без сохранения в дерево) при использовании виджета.
 */
class CreateTempRecordUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val dataNameProvider: IDataNameProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val checkRecordFolderUseCase: CheckRecordFolderUseCase,
    private val saveRecordHtmlTextUseCase: SaveRecordHtmlTextUseCase,
) : UseCase<TetroidRecord, CreateTempRecordUseCase.Params>() {

    data class Params(
        val srcName: String?,
        val url: String?,
        val text: String?,
        val node: TetroidNode,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidRecord> {
        val url = params.url
        val text = params.text
        val node = params.node

        logger.logOperStart(LogObj.TEMP_RECORD, LogOper.CREATE)
        // генерируем уникальный идентификатор
        val id = dataNameProvider.createUniqueId()
        // имя каталога с добавлением префикса в виде текущей даты и времени
        val folderName = dataNameProvider.createDateTimePrefix() + "_" + dataNameProvider.createUniqueId()
        val name = params.srcName?.ifEmpty {
//            name = String.format("%s - %s", resourcesProvider.getString(R.string.title_new_record),
//                    Utils.dateToString(new Date(), "yyyy.MM.dd HH:mm:ss"));
//            name = String.format(Locale.getDefault(), "%1$te %1$tb %1$tY %1$tR", new Date());
           "%1\$te %1\$tb %1\$tR".format(Locale.getDefault(), Date())
        }
        val record = TetroidRecord(
            false,
            id,
            name,
            null,
            null,
            url,
            Date(),
            folderName,
            TetroidRecord.DEF_FILE_NAME,
            node
        )
        record.setIsNew(true)
        record.setIsTemp(true)

        // создаем каталог записи в корзине
        val folderPath = recordPathProvider.getPathToRecordFolderInTrash(record)
        checkRecordFolderUseCase.run(
            CheckRecordFolderUseCase.Params(
                folderPath = folderPath,
                isCreate = true,
            )
        ).onFailure {
            return it.toLeft()
        }
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
        // текст записи
        if (!text.isNullOrEmpty()) {
            saveRecordHtmlTextUseCase.run(
                SaveRecordHtmlTextUseCase.Params(
                    record = record,
                    html = text,
                )
            ).map {
                record.setIsNew(false)
            }
        }
        // добавляем запись в дерево
        node.addRecord(record)
        return record.toRight()
    }

}