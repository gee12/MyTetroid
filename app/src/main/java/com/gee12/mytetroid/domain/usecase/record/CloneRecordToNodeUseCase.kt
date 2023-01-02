package com.gee12.mytetroid.domain.usecase.record

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.manager.FavoritesManager
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.domain.usecase.attach.CloneAttachesToRecordUseCase
import com.gee12.mytetroid.domain.usecase.attach.RenameRecordAttachesUseCase
import com.gee12.mytetroid.domain.usecase.crypt.CryptRecordFilesUseCase
import com.gee12.mytetroid.domain.usecase.file.MoveFileUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import java.io.File
import java.io.IOException

/**
 * Перемещение или копирование записи в ветку.
 */
class CloneRecordToNodeUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val favoritesManager: FavoritesManager,
    private val dataNameProvider: IDataNameProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val storagePathProvider: IStoragePathProvider,
    private val cryptManager: IStorageCryptManager,
    private val checkRecordFolderUseCase: CheckRecordFolderUseCase,
    private val cloneAttachesToRecordUseCase: CloneAttachesToRecordUseCase,
    private val renameRecordAttachesUseCase: RenameRecordAttachesUseCase,
    private val parseRecordTagsUseCase: ParseRecordTagsUseCase,
    private val cryptRecordFilesUseCase: CryptRecordFilesUseCase,
    private val moveFileUseCase: MoveFileUseCase,
) : UseCase<UseCase.None, CloneRecordToNodeUseCase.Params>() {

    data class Params(
        val srcRecord: TetroidRecord,
        val node: TetroidNode,
        val isCutted: Boolean,
        val breakOnFSErrors: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val srcRecord = params.srcRecord
        val node = params.node
        val isCutted = params.isCutted
        val breakOnFSErrors = params.breakOnFSErrors

        logger.logOperStart(LogObj.RECORD, LogOper.INSERT, srcRecord)

        // генерируем уникальные идентификаторы, если запись копируется
        val id = if (isCutted) srcRecord.id else dataNameProvider.createUniqueId()
        val dirName = if (isCutted) srcRecord.dirName else dataNameProvider.createUniqueId()
        val name = srcRecord.name
        val tagsString = srcRecord.tagsString
        val author = srcRecord.author
        val url = srcRecord.url

        // создаем копию записи
        val isCrypted = node.isCrypted
        val record = TetroidRecord(
            isCrypted,
            id,
            encryptFieldIfNeed(name, isCrypted),
            encryptFieldIfNeed(tagsString, isCrypted),
            encryptFieldIfNeed(author, isCrypted),
            encryptFieldIfNeed(url, isCrypted),
            srcRecord.created,
            dirName,
            srcRecord.fileName,
            node,
        )
        if (isCrypted) {
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
            favoritesManager.add(record)
        }
        // добавляем метки в запись и в коллекцию меток
        parseRecordTags(
            record = record,
            tagsString = tagsString,
//            params.tagsMap
        )

        // TODO: создать Failure под конкретные ошибки вместо Int
        val result = if (breakOnFSErrors) {
            Failure.Record.CloneRecordToNode.toLeft()
        } else {
            None.toRight()
        }
        // проверяем существование каталога записи
        val srcFolderPath = if (isCutted) {
            recordPathProvider.getPathToRecordFolderInTrash(srcRecord)
        } else {
            recordPathProvider.getPathToRecordFolder(srcRecord)
        }
        checkRecordFolderUseCase.run(
            CheckRecordFolderUseCase.Params(
                folderPath = srcFolderPath,
                isCreate = false,
            )
        ).onFailure {
            return it.toLeft()
        }
        val srcFolder = File(srcFolderPath)
        val destFolderPath = recordPathProvider.getPathToRecordFolder(record)
        val destFolder = File(destFolderPath)
        if (isCutted) {
            // вырезаем уникальную приставку в имени каталога
            val dirNameInBase = srcRecord.dirName.substring(DataNameProvider.PREFIX_DATE_TIME_FORMAT.length + 1)
            // перемещаем каталог записи
            moveRecordFolder(
                record = record,
                srcPath = srcFolderPath,
                destPath = storagePathProvider.getPathToStorageBaseFolder(),
                newDirName = dirNameInBase
            ).onFailure {
                return it.toLeft()
            }
        } else {
            // копируем каталог записи
            try {
                val res = FileUtils.copyDirRecursive(srcFolder, destFolder)
                if (res) {
                    logger.logDebug(resourcesProvider.getString(R.string.log_copy_record_dir_mask, destFolderPath))
                    // переименовываем прикрепленные файлы
                    renameRecordAttachesUseCase.run(
                        RenameRecordAttachesUseCase.Params(
                            srcRecord = srcRecord,
                            destRecord = record,
                        )
                    )
                } else {
//                    logger.logError(resourcesProvider.getString(R.string.log_error_copy_record_dir_mask, srcFolderPath, destFolderPath))
                    return Failure.Folder.Copy(path = srcFolderPath, newPath = destFolderPath).toLeft()
                }
            } catch (ex: IOException) {
//                logger.logError(resourcesProvider.getString(R.string.log_error_copy_record_dir_mask, srcFolderPath, destFolderPath), ex)
                return Failure.Folder.Copy(path = srcFolderPath, newPath = destFolderPath).toLeft()
            }
        }

        // зашифровываем или расшифровываем файл записи
//        File recordFile = new File(getPathToFileInRecordFolder(record, record.getFileName()));
//        if (!cryptOrDecryptFile(recordFile, srcRecord.isCrypted(), crypted) && breakOnFSErrors) {
        return cryptRecordFilesUseCase.run(
            CryptRecordFilesUseCase.Params(
                record = record,
                isCrypted = srcRecord.isCrypted,
                isEncrypt = isCrypted
            )
        ).flatMap {
            None.toRight()
        }.onFailure {
            return result
        }
    }

    private fun encryptFieldIfNeed(fieldValue: String, isEncrypt: Boolean): String? {
        return if (isEncrypt) cryptManager.encryptTextBase64(fieldValue) else fieldValue
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
    ) {
        parseRecordTagsUseCase.run(
            ParseRecordTagsUseCase.Params(
                record = record,
                tagsString = tagsString,
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
    ): Either<Failure, None> {
        return moveFileUseCase.run(
            MoveFileUseCase.Params(
                srcFullFileName = srcPath,
                destPath = destPath,
                newFileName = newDirName,
            )
        ).flatMap {
            if (newDirName != null) {
                // обновляем имя каталога для дальнейшей вставки
                record.dirName = newDirName
            }
            None.toRight()
        }
    }

}