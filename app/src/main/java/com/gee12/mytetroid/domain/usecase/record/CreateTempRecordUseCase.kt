package com.gee12.mytetroid.domain.usecase.record

import android.content.Context
import com.anggrayudi.storage.file.CreateMode
import com.anggrayudi.storage.file.MimeType
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.makeFile
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IDataNameProvider
import com.gee12.mytetroid.domain.provider.IRecordPathProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import java.io.IOException
import java.util.*

/**
 * Создание временной записи (без сохранения в дерево) при использовании виджета.
 */
class CreateTempRecordUseCase(
    private val context: Context,
    private val logger: ITetroidLogger,
    private val dataNameProvider: IDataNameProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,
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
        val folderNameInTrash = "${dataNameProvider.createDateTimePrefix()}_${dataNameProvider.createUniqueId()}"
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
            folderNameInTrash,
            TetroidRecord.DEF_FILE_NAME,
            node
        )
        record.setIsNew(true)
        record.setIsTemp(true)

        // создаем каталог записи в корзине
        val recordFolder = getRecordFolderUseCase.run(
            GetRecordFolderUseCase.Params(
                record = record,
                createIfNeed = true,
                inTrash = true,
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
        try {
            recordFolder.makeFile(
                context = context,
                name = record.fileName,
                mimeType = MimeType.TEXT,
                mode = CreateMode.REPLACE,
            ) ?: return Failure.File.Create(filePath).toLeft()
        } catch (ex: IOException) {
            return Failure.File.Create(filePath, ex).toLeft()
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