package com.gee12.mytetroid.usecase.record

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.interactors.DataInteractor
import com.gee12.mytetroid.interactors.EncryptionInteractor
import com.gee12.mytetroid.interactors.FavoritesInteractor
import com.gee12.mytetroid.interactors.RecordsInteractor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidTag
import com.gee12.mytetroid.usecase.attach.CloneAttachesToRecordUseCase
import com.gee12.mytetroid.usecase.tag.ParseRecordTagsUseCase
import java.io.File
import java.io.IOException
import java.util.HashMap

/**
 * Перемещение или копирование записи в ветку.
 */
class CloneRecordToNodeUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val recordsInteractor: RecordsInteractor,
    private val favoritesInteractor: FavoritesInteractor,
    private val dataInteractor: DataInteractor,
    private val recordPathHelper: IRecordPathHelper,
    private val storagePathHelper: IStoragePathHelper,
    private val cryptInteractor: EncryptionInteractor,
    private val cloneAttachesToRecordUseCase: CloneAttachesToRecordUseCase,
    private val parseRecordTagsUseCase: ParseRecordTagsUseCase,
) : UseCase<UseCase.None, CloneRecordToNodeUseCase.Params>() {

    data class Params(
        val srcRecord: TetroidRecord,
        val node: TetroidNode,
        val isCutted: Boolean,
        val breakOnFSErrors: Boolean,
        val tagsMap: HashMap<String, TetroidTag>,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val srcRecord = params.srcRecord
        val node = params.node
        val isCutted = params.isCutted
        val breakOnFSErrors = params.breakOnFSErrors

        logger.logOperStart(LogObj.RECORD, LogOper.INSERT, srcRecord)

        // генерируем уникальные идентификаторы, если запись копируется
        val id = if (isCutted) srcRecord.id else dataInteractor.createUniqueId()
        val dirName = if (isCutted) srcRecord.dirName else dataInteractor.createUniqueId()
        val name = srcRecord.name
        val tagsString = srcRecord.tagsString
        val author = srcRecord.author
        val url = srcRecord.url

        // создаем копию записи
        val crypted = node.isCrypted
        val record = TetroidRecord(
            crypted, id,
            encryptField(crypted, name),
            encryptField(crypted, tagsString),
            encryptField(crypted, author),
            encryptField(crypted, url),
            srcRecord.created, dirName, srcRecord.fileName, node
        )
        if (crypted) {
            record.setDecryptedValues(name, tagsString, author, url)
            record.setIsDecrypted(true)
        }
        if (isCutted) {
            record.setIsFavorite(srcRecord.isFavorite)
        }
        // добавляем прикрепленные файлы в запись
        cloneAttachesToRecord(srcRecord, record, isCutted)
            .onFailure {
                return it.toLeft()
            }
        record.setIsNew(false)
        // добавляем запись в ветку (и соответственно, в дерево)
        node.addRecord(record)
        // добавляем в избранное обратно
        if (isCutted && record.isFavorite) {
            favoritesInteractor.add(record)
        }
        // добавляем метки в запись и в коллекцию меток
        parseRecordTags(record, tagsString, params.tagsMap)

        // TODO: создать Failure под конкретные ошибки вместо Int
        val result = if (breakOnFSErrors) {
            Failure.Record.CloneRecordToNode.toLeft()
        } else
        {
            None.toRight()
        }
        // проверяем существование каталога записи
        val srcDirPath = if (isCutted) {
            recordPathHelper.getPathToRecordFolderInTrash(srcRecord)
        } else {
            recordPathHelper.getPathToRecordFolder(srcRecord)
        }
        val dirRes = recordsInteractor.checkRecordFolder(srcDirPath, false)
        val srcDir = if (dirRes > 0) {
            File(srcDirPath)
        } else {
            return result
        }
        val destDirPath = recordPathHelper.getPathToRecordFolder(record)
        val destDir = File(destDirPath)
        if (isCutted) {
            // вырезаем уникальную приставку в имени каталога
            val dirNameInBase = srcRecord.dirName.substring(DataInteractor.PREFIX_DATE_TIME_FORMAT.length + 1)
            // перемещаем каталог записи
            val res = moveRecordFolder(
                record = record,
                srcPath = srcDirPath,
                destPath = storagePathHelper.getPathToStorageBaseFolder(),
                newDirName = dirNameInBase
            )
            if (res < 0) {
                return result
            }
        } else {
            // копируем каталог записи
            try {
                val res = FileUtils.copyDirRecursive(srcDir, destDir)
                if (res) {
                    logger.logDebug(resourcesProvider.getString(R.string.log_copy_record_dir_mask, destDirPath))
                    // переименовываем прикрепленные файлы
                    recordsInteractor.renameRecordAttaches(srcRecord, record)
                } else {
                    logger.logError(resourcesProvider.getString(R.string.log_error_copy_record_dir_mask, srcDirPath, destDirPath))
                    return result
                }
            } catch (ex: IOException) {
                logger.logError(resourcesProvider.getString(R.string.log_error_copy_record_dir_mask, srcDirPath, destDirPath), ex)
                return result
            }
        }

        // зашифровываем или расшифровываем файл записи
//        File recordFile = new File(getPathToFileInRecordFolder(record, record.getFileName()));
//        if (!cryptOrDecryptFile(recordFile, srcRecord.isCrypted(), crypted) && breakOnFSErrors) {
        return if (!cryptInteractor.cryptRecordFiles(record, srcRecord.isCrypted, crypted) && breakOnFSErrors) {
            result
        } else {
            None.toRight()
        }
    }

    private fun encryptField(isCrypted: Boolean, field: String?): String? {
        return cryptInteractor.encryptField(isCrypted, field)
    }

    private suspend fun cloneAttachesToRecord(
        srcRecord: TetroidRecord,
        destRecord: TetroidRecord,
        isCutted: Boolean
    ) : Either<Failure, None> {
        return cloneAttachesToRecordUseCase.run(
            CloneAttachesToRecordUseCase.Params(
                srcRecord = srcRecord,
                destRecord = destRecord,
                isCutted = isCutted,
            )
        )
    }

    private suspend fun parseRecordTags(
        record: TetroidRecord,
        tagsString: String,
        tagsMap: HashMap<String, TetroidTag>
    ) {
        parseRecordTagsUseCase.run(
            ParseRecordTagsUseCase.Params(
                record = record,
                tagsString = tagsString,
                tagsMap = tagsMap,
            )
        ).onFailure {
            logger.logFailure(it, show = false)
        }
    }

    private suspend fun moveRecordFolder(
        record: TetroidRecord,
        srcPath: String,
        destPath: String,
        newDirName: String?
    ): Int {
        val res = dataInteractor.moveFile(srcPath, destPath, newDirName)
        if (res > 0 && newDirName != null) {
            // обновляем имя каталога для дальнейшей вставки
            record.dirName = newDirName
        }
        return res
    }

}