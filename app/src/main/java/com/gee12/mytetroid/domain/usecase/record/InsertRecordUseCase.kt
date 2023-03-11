package com.gee12.mytetroid.domain.usecase.record

import android.content.Context
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.FavoritesManager
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.domain.usecase.attach.CloneAttachesToRecordUseCase
import com.gee12.mytetroid.domain.usecase.crypt.CryptRecordFilesIfNeedUseCase
import com.gee12.mytetroid.domain.usecase.file.MoveFileOrFolderUseCase
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord

/**
 * Вставка записи в указанную ветку.
 * @param isCutting Если true, то запись была вырезана. Иначе - скопирована
 * @param withoutDir Не пытаться восстановить каталог записи
 */
class InsertRecordUseCase(
    private val context: Context,
    private val logger: ITetroidLogger,
    private val storageProvider: IStorageProvider,
    private val storagePathProvider: IStoragePathProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val dataNameProvider: IDataNameProvider,
    private val cryptManager: IStorageCryptManager,
    private val favoritesManager: FavoritesManager,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,
    private val cloneAttachesToRecordUseCase: CloneAttachesToRecordUseCase,
    private val moveOrCopyRecordFolderUseCase: MoveOrCopyRecordFolderUseCase,
    private val moveFileOrFolderUseCase: MoveFileOrFolderUseCase,
    private val parseRecordTagsUseCase: ParseRecordTagsUseCase,
    private val cryptRecordFilesIfNeedUseCase: CryptRecordFilesIfNeedUseCase,
    private val saveStorageUseCase: SaveStorageUseCase,
) : UseCase<TetroidRecord, InsertRecordUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val node: TetroidNode,
        val isCutting: Boolean,
        val withoutDir: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidRecord> {
        val srcRecord = params.record
        val node = params.node
        val isCutting = params.isCutting
        val withoutDir = params.withoutDir

        logger.logOperStart(LogObj.RECORD, LogOper.INSERT, srcRecord)

        // генерируем уникальные идентификаторы, если запись копируется
        val id = if (isCutting) srcRecord.id else dataNameProvider.createUniqueId()
        val folderName = if (isCutting) srcRecord.dirName else dataNameProvider.createUniqueId()
        val name = srcRecord.name
        val tagsString = srcRecord.tagsString
        val author = srcRecord.author
        val url = srcRecord.url

        // создаем копию записи
        val isEncrypted = node.isCrypted
        val destRecord = TetroidRecord(
            isEncrypted,
            id,
            encryptFieldIfNeed(name, isEncrypted),
            encryptFieldIfNeed(tagsString, isEncrypted),
            encryptFieldIfNeed(author, isEncrypted),
            encryptFieldIfNeed(url, isEncrypted),
            srcRecord.created,
            folderName,
            srcRecord.fileName,
            node,
        )
        if (isEncrypted) {
            destRecord.setDecryptedValues(name, tagsString, author, url)
            destRecord.setIsDecrypted(true)
        }
        // прикрепленные файлы
        cloneAttachesToRecordUseCase.run(
            CloneAttachesToRecordUseCase.Params(
                srcRecord = srcRecord,
                destRecord = destRecord,
                isCutting = isCutting
            )
        ).onFailure {
            return it.toLeft()
        }
        destRecord.setIsNew(false)

        if (!withoutDir) {
            moveOrCopyRecordFolderUseCase.run(
                MoveOrCopyRecordFolderUseCase.Params(
                    srcRecord = srcRecord,
                    destRecord = destRecord,
                    isCutting = isCutting,
                )
            ).onFailure {
                return it.toLeft()
            }
        }

        // добавляем запись в ветку (и соответственно, в дерево)
        node.addRecord(destRecord)
        // перезаписываем структуру хранилища в файл
        return saveStorageUseCase.run()
            .flatMap {
                // добавляем в избранное обратно
                if (isCutting && srcRecord.isFavorite) {
                    favoritesManager.add(destRecord)
                }
                // добавляем метки в запись и в коллекцию меток
                parseRecordTagsUseCase.run(
                    ParseRecordTagsUseCase.Params(
                        record = destRecord,
                        tagsString = tagsString,
                    )
                ).flatMap {
                    if (!withoutDir) {
                        // зашифровываем или расшифровываем файл записи и прикрепленные файлы
                        cryptRecordFilesIfNeedUseCase.run(
                            CryptRecordFilesIfNeedUseCase.Params(
                                record = destRecord,
                                isEncrypted = srcRecord.isCrypted,
                                isEncrypt = isEncrypted,
                            )
                        )
                    }
                    destRecord.toRight()
                }
            }.onFailure {
                logger.logOperCancel(LogObj.RECORD, LogOper.INSERT)
                // удаляем запись из ветки
                node.records.remove(destRecord)

                if (!withoutDir) {
                    moveFolderBack(params, destRecord)
                        .onFailure {
                            return it.toLeft()
                        }
                }
                return it.toLeft()
            }
    }

    private fun encryptFieldIfNeed(fieldValue: String, isEncrypt: Boolean): String? {
        return if (isEncrypt) cryptManager.encryptTextBase64(fieldValue) else fieldValue
    }

    private suspend fun moveFolderBack(params: Params, destRecord: TetroidRecord): Either<Failure, None> {
        val srcRecord = params.record

        if (params.isCutting) {
            val trashFolderPath = storagePathProvider.getPathToStorageTrashFolder()

            val trashFolder = storageProvider.trashFolder
                ?: return Failure.Folder.Get(trashFolderPath).toLeft()

            getRecordFolderUseCase.run(
                GetRecordFolderUseCase.Params(
                    record = srcRecord,
                    createIfNeed = false,
                    inTrash = true,
                )
            ).flatMap { srcFolder ->
                // перемещаем каталог записи обратно в корзину
                moveFileOrFolderUseCase.run(
                    MoveFileOrFolderUseCase.Params(
                        srcFileOrFolder = srcFolder,
                        destFolder = trashFolder,
                        newName = srcRecord.dirName
                    )
                )
            }

        } else {
            val storageFolder = storageProvider.rootFolder
            val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
            val destFolderRelativePath = recordPathProvider.getRelativePathToRecordFolder(destRecord)
            val destFolderPath = FilePath.Folder(storageFolderPath, destFolderRelativePath)

            val destFolder = storageFolder?.child(
                context = context,
                path = destFolderRelativePath,
                requiresWriteAccess = true,
            ) ?: return Failure.Folder.Get(destFolderPath).toLeft()

            // удаляем только что скопированный каталог записи
            if (destFolder.deleteRecursively(context, childrenOnly = false)) {
                logger.logOperRes(LogObj.RECORD_DIR, LogOper.DELETE)
            } else {
                return Failure.Folder.Delete(destFolderPath).toLeft()
            }
        }
        return None.toRight()
    }

}