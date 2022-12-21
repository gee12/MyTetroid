package com.gee12.mytetroid.domain.usecase.record

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.toFile
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.domain.IStorageCrypter
import com.gee12.mytetroid.domain.FavoritesManager
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
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import java.io.File
import java.io.IOException

/**
 * Вставка записи в указанную ветку.
 * @param isCutted Если true, то запись была вырезана. Иначе - скопирована
 * @param withoutDir Не пытаться восстановить каталог записи
 */
class InsertRecordUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val storagePathProvider: IStoragePathProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val dataNameProvider: IDataNameProvider,
    private val storageCrypter: IStorageCrypter,
    private val favoritesManager: FavoritesManager,
    private val checkRecordFolderUseCase: CheckRecordFolderUseCase,
    private val cloneAttachesToRecordUseCase: CloneAttachesToRecordUseCase,
    private val renameRecordAttachesUseCase: RenameRecordAttachesUseCase,
    private val moveFileUseCase: MoveFileUseCase,
    private val parseRecordTagsUseCase: ParseRecordTagsUseCase,
    private val cryptRecordFilesUseCase: CryptRecordFilesUseCase,
    private val saveStorageUseCase: SaveStorageUseCase,
) : UseCase<UseCase.None, InsertRecordUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val node: TetroidNode,
        val isCutting: Boolean,
        val withoutDir: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val srcRecord = params.record
        val node = params.node
        val isCutting = params.isCutting
        val withoutDir = params.withoutDir

        logger.logOperStart(LogObj.RECORD, LogOper.INSERT, srcRecord)
        var srcFolderPath = ""
        var srcFolder: File? = null
        // проверяем существование каталога записи
        if (!withoutDir) {
            srcFolderPath = if (isCutting) {
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
            srcFolder = srcFolderPath.toFile()
        }

        // генерируем уникальные идентификаторы, если запись копируется
        val id = if (isCutting) srcRecord.id else dataNameProvider.createUniqueId()
        val folderName = if (isCutting) srcRecord.dirName else dataNameProvider.createUniqueId()
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
            folderName,
            srcRecord.fileName,
            node,
        )
        if (isCrypted) {
            record.setDecryptedValues(name, tagsString, author, url)
            record.setIsDecrypted(true)
        }
        // прикрепленные файлы
        cloneAttachesToRecordUseCase.run(
            CloneAttachesToRecordUseCase.Params(
                srcRecord = srcRecord,
                destRecord = record,
                isCutted = isCutting
            )
        ).onFailure {
            return it.toLeft()
        }
        record.setIsNew(false)
        val destFolderPath = recordPathProvider.getPathToRecordFolder(record)
        val destFolder = File(destFolderPath)
        if (!withoutDir) {
            if (isCutting) {
                // вырезаем уникальную приставку в имени каталога
                val dirNameInBase = srcRecord.dirName.substring(DataNameProvider.PREFIX_DATE_TIME_FORMAT.length + 1)
                // перемещаем каталог записи
                moveFileUseCase.run(
                    MoveFileUseCase.Params(
                        srcFullFileName = srcFolderPath,
                        destPath = storagePathProvider.getPathToStorageBaseFolder(),
                        newFileName = dirNameInBase
                    )
                ).map {
                    // обновляем имя каталога для дальнейшей вставки
                    record.dirName = dirNameInBase
                }.onFailure {
                    return it.toLeft()
                }
            } else {
                // копируем каталог записи
                try {
                    val copyDirResult = FileUtils.copyDirRecursive(srcFolder, destFolder)
                    if (copyDirResult) {
                        logger.logDebug(resourcesProvider.getString(R.string.log_copy_record_dir_mask, destFolderPath))
                        // переименовываем прикрепленные файлы
                        renameRecordAttachesUseCase.run(
                            RenameRecordAttachesUseCase.Params(
                                srcRecord = srcRecord,
                                destRecord = record,
                            )
                        )
                    } else {
//                        logger.log(resourcesProvider.getString(R.string.log_error_copy_record_dir_mask, srcFolderPath, destFolderPath))
                        return Failure.Folder.Copy(path = srcFolderPath, newPath = destFolderPath).toLeft()
                    }
                } catch (ex: IOException) {
//                    logger.logError(resourcesProvider.getString(R.string.log_error_copy_record_dir_mask, srcFolderPath, destFolderPath), ex)
                    return Failure.Folder.Copy(path = srcFolderPath, newPath = destFolderPath).toLeft()
                }
            }
        }

        // добавляем запись в ветку (и соответственно, в дерево)
        node.addRecord(record)
        // перезаписываем структуру хранилища в файл
        return saveStorageUseCase.run()
            .flatMap {
                // добавляем в избранное обратно
                if (isCutting && srcRecord.isFavorite) {
                    favoritesManager.add(record)
                }
                // добавляем метки в запись и в коллекцию меток
                parseRecordTagsUseCase.run(
                    ParseRecordTagsUseCase.Params(
                        record = record,
                        tagsString = tagsString,
                    )
                ).flatMap {
                    if (!withoutDir) {
                        // зашифровываем или расшифровываем файл записи и прикрепленные файлы
                        cryptRecordFilesUseCase.run(
                            CryptRecordFilesUseCase.Params(
                                record = record,
                                isCrypted = srcRecord.isCrypted,
                                isEncrypt = isCrypted,
                            )
                        )
                    }
                    None.toRight()
                }
            }.onFailure {
                logger.logOperCancel(LogObj.RECORD, LogOper.INSERT)
                // удаляем запись из ветки
                node.records.remove(record)
                if (!withoutDir) {
                    if (isCutting) {
                        // перемещаем каталог записи обратно в корзину
                        moveFileUseCase.run(
                            MoveFileUseCase.Params(
                                srcFullFileName = destFolderPath,
                                destPath = storagePathProvider.getPathToStorageTrashFolder(),
                                newFileName = srcRecord.dirName
                            )
                        )
                    } else {
                        // удаляем только что скопированный каталог записи
                        if (FileUtils.deleteRecursive(destFolder)) {
                            logger.logOperRes(LogObj.RECORD_DIR, LogOper.DELETE)
                        } else {
//                            logger.logOperError(LogObj.RECORD_DIR, LogOper.DELETE,": $destFolderPath", false, false)
                            return Failure.Folder.Delete(path = destFolder.path).toLeft()
                        }
                    }
                }
                return it.toLeft()
            }
    }

    private fun encryptFieldIfNeed(fieldValue: String, isEncrypt: Boolean): String? {
        return if (isEncrypt) storageCrypter.encryptTextBase64(fieldValue) else fieldValue
    }

}