package com.gee12.mytetroid.domain.usecase.record

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.provider.IRecordPathProvider
import com.gee12.mytetroid.domain.provider.IStoragePathProvider
import com.gee12.mytetroid.domain.provider.DataNameProvider
import com.gee12.mytetroid.domain.manager.FavoritesManager
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.domain.usecase.crypt.CryptRecordFilesUseCase
import com.gee12.mytetroid.domain.usecase.file.MoveFileUseCase
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.domain.usecase.tag.DeleteRecordTagsUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase

/**
 * Изменение свойств записи или сохранение временной записи.
 */
class EditRecordFieldsUseCase(
    private val logger: ITetroidLogger,
    private val buildInfoProvider: BuildInfoProvider,
    private val storagePathProvider: IStoragePathProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val favoritesManager: FavoritesManager,
    private val cryptManager: IStorageCryptManager,
    private val moveFileUseCase: MoveFileUseCase,
    private val deleteRecordTagsUseCase: DeleteRecordTagsUseCase,
    private val parseRecordTagsUseCase: ParseRecordTagsUseCase,
    private val cryptRecordFilesUseCase: CryptRecordFilesUseCase,
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
        val isTemp = record.isTemp
        if (isTemp) {
            logger.logOperStart(LogObj.TEMP_RECORD, LogOper.SAVE, record)
        } else {
            logger.logOperStart(LogObj.RECORD_FIELDS, LogOper.CHANGE, record)
        }
        val oldIsCrypted = record.isCrypted
        val oldName = record.getName(true)
        val oldAuthor = record.getAuthor(true)
        val oldTagsString = record.getTagsString(true)
        val oldUrl = record.getUrl(true)
        val oldNode = record.node
        val oldDirName = record.dirName
        val oldIsFavor = record.isFavorite
        // обновляем поля
        val isCrypted = node.isCrypted
        record.name = encryptFieldIfNeed(name, isCrypted)
        record.tagsString = encryptFieldIfNeed(tagsString, isCrypted)
        record.author = encryptFieldIfNeed(author, isCrypted)
        record.url = encryptFieldIfNeed(url, isCrypted)
        record.setIsCrypted(isCrypted)
        if (isCrypted) {
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
        if (isTemp) {
            // вырезаем уникальную приставку в имени каталога
            val dirNameInBase = oldDirName.substring(DataNameProvider.PREFIX_DATE_TIME_FORMAT.length + 1)
            // перемещаем каталог записи из корзины
            moveFileUseCase.run(
                MoveFileUseCase.Params(
                    srcFullFileName = recordPathProvider.getPathToRecordFolderInTrash(record),
                    destPath = storagePathProvider.getPathToStorageBaseFolder(),
                    newFileName = dirNameInBase,
                )
            ).map {
                // обновляем имя каталога для дальнейшей вставки
                record.dirName = dirNameInBase
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
                    cryptRecordFilesUseCase.run(
                        CryptRecordFilesUseCase.Params(
                            record = record,
                            isCrypted = oldIsCrypted,
                            isEncrypt = isCrypted,
                        )
                    ).flatMap {
                        None.toRight()
                    }
                }.onFailure {
                    if (isTemp) {
                        logger.logOperCancel(LogObj.TEMP_RECORD, LogOper.SAVE)
                    } else {
                        logger.logOperCancel(LogObj.RECORD_FIELDS, LogOper.CHANGE)
                    }
                    // возвращаем изменения
                    record.name = oldName
                    record.tagsString = oldTagsString
                    record.author = oldAuthor
                    record.url = oldUrl
                    if (isCrypted) {
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
                    if (isTemp) {
                        record.setIsTemp(true)
                        moveFileUseCase.run(
                            MoveFileUseCase.Params(
                                srcFullFileName = recordPathProvider.getPathToRecordFolder(record),
                                destPath = storagePathProvider.getPathToStorageTrashFolder(),
                                newFileName = oldDirName,
                            )
                        ).map {
                            // обновляем имя каталога для дальнейшей вставки
                            record.dirName = oldDirName
                        }
                    }
                }
            }
    }

    private fun encryptFieldIfNeed(fieldValue: String, isEncrypt: Boolean): String? {
        return if (isEncrypt) cryptManager.encryptTextBase64(fieldValue) else fieldValue
    }

}