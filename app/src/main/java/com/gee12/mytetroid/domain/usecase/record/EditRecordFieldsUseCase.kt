package com.gee12.mytetroid.domain.usecase.record

import android.content.Context
import com.anggrayudi.storage.file.child
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.manager.FavoritesManager
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.domain.usecase.crypt.CryptRecordFilesIfNeedUseCase
import com.gee12.mytetroid.domain.usecase.file.MoveFileOrFolderUseCase
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.domain.usecase.tag.DeleteRecordTagsUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase

/**
 * Изменение свойств записи или сохранение временной записи.
 */
class EditRecordFieldsUseCase(
    private val context: Context,
    private val logger: ITetroidLogger,
    private val buildInfoProvider: BuildInfoProvider,
    private val storageProvider: IStorageProvider,
    private val storagePathProvider: IStoragePathProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val favoritesManager: FavoritesManager,
    private val cryptManager: IStorageCryptManager,
    private val moveFileUseCase: MoveFileOrFolderUseCase,
    private val deleteRecordTagsUseCase: DeleteRecordTagsUseCase,
    private val parseRecordTagsUseCase: ParseRecordTagsUseCase,
    private val cryptRecordFilesIfNeedUseCase: CryptRecordFilesIfNeedUseCase,
    private val saveStorageUseCase: SaveStorageUseCase,
) : UseCase<UseCase.None, EditRecordFieldsUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val name: String,
        val tagsString: String,
        val author: String,
        val url: String,
        val node: TetroidNode,
        val isFavor: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val record = params.record
        val name = params.name
        val tagsString = params.tagsString
        val author = params.author
        val url = params.url
        val node = params.node
        val isFavor = params.isFavor

        if (name.isEmpty()) {
            return Failure.Record.NameIsEmpty.toLeft()
        }
        val isTemporary = record.isTemp
        if (isTemporary) {
            logger.logOperStart(LogObj.TEMP_RECORD, LogOper.SAVE, record)
        } else {
            logger.logOperStart(LogObj.RECORD_FIELDS, LogOper.CHANGE, record)
        }
        val oldIsEncrypted = record.isCrypted
        val oldName = record.getName(true)
        val oldAuthor = record.getAuthor(true)
        val oldTagsString = record.getTagsString(true)
        val oldUrl = record.getUrl(true)
        val oldNode = record.node
        val oldFolderName = record.dirName
        val oldIsFavor = record.isFavorite
        // обновляем поля
        val isEncrypted = node.isCrypted
        record.name = encryptFieldIfNeed(name, isEncrypted)
        record.tagsString = encryptFieldIfNeed(tagsString, isEncrypted)
        record.author = encryptFieldIfNeed(author, isEncrypted)
        record.url = encryptFieldIfNeed(url, isEncrypted)
        record.setIsCrypted(isEncrypted)
        if (isEncrypted) {
            record.setDecryptedValues(name, tagsString, author, url)
            record.setIsDecrypted(true)
        }
        record.setIsFavorite(isFavor)
        // обновляем ветку
        if (oldNode !== node) {
            oldNode?.deleteRecord(record)
            node.addRecord(record)
        }
        // удаляем пометку временной записи
        if (isTemporary) {
            val trashFolderPath = storagePathProvider.getPathToStorageTrashFolder()
            val baseFolderPath = storagePathProvider.getPathToBaseFolder()

            // вырезаем уникальную приставку в имени каталога
            val folderNameWithoutPrefix = oldFolderName.substring(DataNameProvider.PREFIX_DATE_TIME_FORMAT.length + 1)

            val trashFolder = storageProvider.trashFolder
                ?: return Failure.Folder.Get(trashFolderPath).toLeft()

            val recordFolderInTrashPath = recordPathProvider.getPathToRecordFolderInTrash(record)
            val recordFolderInTrash = trashFolder.child(
                context = context,
                path = oldFolderName,
                requiresWriteAccess = true,
            ) ?: return Failure.Folder.Get(recordFolderInTrashPath).toLeft()

            val baseFolder = storageProvider.baseFolder
                ?: return Failure.Folder.Get(baseFolderPath).toLeft()

            // перемещаем каталог записи из корзины
            moveFileUseCase.run(
                MoveFileOrFolderUseCase.Params(
                    srcFileOrFolder = recordFolderInTrash,
                    destFolder = baseFolder,
                    newName = folderNameWithoutPrefix,
                )
            ).map {
                // обновляем имя каталога для дальнейшей вставки
                record.dirName = folderNameWithoutPrefix
            }.onFailure {
                return it.toLeft()
            }
            record.setIsTemp(false)
        }

        // перезаписываем структуру хранилища в файл
        return saveStorageUseCase.run()
            .flatMap {
                ifEitherOrNoneSuspend(
                    oldTagsString == null && tagsString.isNotEmpty()
                            || oldTagsString != null && oldTagsString != tagsString
                ) {
                    // удаляем старые метки
                    deleteRecordTagsUseCase.run(
                        DeleteRecordTagsUseCase.Params(record)
                    ).flatMap {
                        // добавляем новые метки
                        parseRecordTagsUseCase.run(
                            ParseRecordTagsUseCase.Params(
                                record = record,
                                tagsString = tagsString,
                            )
                        )
                    }
                }.flatMap {
                    ifEitherOrNoneSuspend(buildInfoProvider.isFullVersion()) {
                        // добавляем/удаляем из избранного
                        favoritesManager.addOrRemoveIfNeed(record, isFavor)
                        None.toRight()
                    }
                }.flatMap {
                    // зашифровываем или расшифровываем файл записи и прикрепленные файлы
                    cryptRecordFilesIfNeedUseCase.run(
                        CryptRecordFilesIfNeedUseCase.Params(
                            record = record,
                            isEncrypted = oldIsEncrypted,
                            isEncrypt = isEncrypted,
                        )
                    ).flatMap {
                        None.toRight()
                    }
                }.onFailure {
                    if (isTemporary) {
                        logger.logOperCancel(LogObj.TEMP_RECORD, LogOper.SAVE)
                    } else {
                        logger.logOperCancel(LogObj.RECORD_FIELDS, LogOper.CHANGE)
                    }
                    // возвращаем изменения
                    record.name = oldName
                    record.tagsString = oldTagsString
                    record.author = oldAuthor
                    record.url = oldUrl
                    if (isEncrypted) {
                        record.setDecryptedValues(
                            cryptManager.decryptTextBase64(oldName),
                            cryptManager.decryptTextBase64(oldTagsString),
                            cryptManager.decryptTextBase64(oldAuthor),
                            cryptManager.decryptTextBase64(url)
                        )
                    }
                    node.deleteRecord(record)
                    oldNode?.addRecord(record)
                    if (buildInfoProvider.isFullVersion()) {
                        favoritesManager.addOrRemoveIfNeed(record, oldIsFavor)
                    }
                    if (isTemporary) {
                        record.setIsTemp(true)

                        moveFolderBackToTrash(params, oldFolderName)
                            .map {
                                // обновляем имя каталога для дальнейшей вставки
                                record.dirName = oldFolderName
                            }
                    }
                }
            }
    }

    private fun encryptFieldIfNeed(fieldValue: String, isEncrypt: Boolean): String? {
        return if (isEncrypt) cryptManager.encryptTextBase64(fieldValue) else fieldValue
    }

    private suspend fun moveFolderBackToTrash(
        params: Params,
        oldFolderName: String
    ): Either<Failure, None> {
        val record = params.record
        val storageFolder = storageProvider.rootFolder
        val recordFolderPath = recordPathProvider.getPathToRecordFolder(record)
        val trashFolderPath = storagePathProvider.getPathToStorageTrashFolder()

        val srcFolder = storageFolder?.child(
            context = context,
            path = recordFolderPath.folderName,
            requiresWriteAccess = false,
        ) ?: return Failure.Folder.Get(recordFolderPath).toLeft()

        val trashFolder = storageProvider.trashFolder
            ?: return Failure.Folder.Get(trashFolderPath).toLeft()

        return moveFileUseCase.run(
            MoveFileOrFolderUseCase.Params(
                srcFileOrFolder = srcFolder,
                destFolder = trashFolder,
                newName = oldFolderName,
            )
        )
    }

}