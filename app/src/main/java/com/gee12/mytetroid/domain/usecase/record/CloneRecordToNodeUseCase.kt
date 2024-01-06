package com.gee12.mytetroid.domain.usecase.record

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.FavoritesManager
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.IDataNameProvider
import com.gee12.mytetroid.domain.usecase.attach.CloneAttachesToRecordUseCase
import com.gee12.mytetroid.domain.usecase.crypt.CryptRecordFilesIfNeedUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord

/**
 * Перемещение или копирование записи в ветку.
 */
class CloneRecordToNodeUseCase(
    private val logger: ITetroidLogger,
    private val favoritesManager: FavoritesManager,
    private val dataNameProvider: IDataNameProvider,
    private val cryptManager: IStorageCryptManager,
    private val cloneAttachesToRecordUseCase: CloneAttachesToRecordUseCase,
    private val parseRecordTagsUseCase: ParseRecordTagsUseCase,
    private val cryptRecordFilesIfNeedUseCase: CryptRecordFilesIfNeedUseCase,
    private val moveOrCopyRecordFolderUseCase: MoveOrCopyRecordFolderUseCase,
) : UseCase<UseCase.None, CloneRecordToNodeUseCase.Params>() {

    data class Params(
        val srcRecord: TetroidRecord,
        val node: TetroidNode,
        val isCutting: Boolean,
        val breakOnFSErrors: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val srcRecord = params.srcRecord
        val node = params.node
        val isCutting = params.isCutting
        val breakOnFSErrors = params.breakOnFSErrors

        logger.logOperStart(LogObj.RECORD, LogOper.INSERT, srcRecord)

        // генерируем уникальные идентификаторы, если запись копируется
        val id = if (isCutting) srcRecord.id else dataNameProvider.createUniqueId()
        val dirName = if (isCutting) srcRecord.dirName else dataNameProvider.createUniqueId()
        val name = srcRecord.name
        val tagsString = srcRecord.tagsString.orEmpty()
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
            dirName,
            srcRecord.fileName,
            node,
        )
        if (isEncrypted) {
            destRecord.setDecryptedValues(name, tagsString, author, url)
            destRecord.setIsDecrypted(true)
        }
        if (isCutting) {
            destRecord.setIsFavorite(srcRecord.isFavorite)
        }
        // добавляем прикрепленные файлы в запись
        cloneAttachesToRecord(srcRecord, destRecord, isCutting)
            .onFailure {
                return it.toLeft()
            }
        destRecord.setIsNew(false)
        // добавляем запись в ветку (и соответственно, в дерево)
        node.addRecord(destRecord)
        // добавляем в избранное обратно
        if (isCutting && destRecord.isFavorite) {
            favoritesManager.add(destRecord)
        }
        // добавляем метки в запись и в коллекцию меток
        parseRecordTags(
            record = destRecord,
            tagsString = tagsString,
        )

        return moveOrCopyRecordFolderUseCase.run(
            MoveOrCopyRecordFolderUseCase.Params(
                srcRecord = srcRecord,
                destRecord = destRecord,
                isCutting = isCutting,
            )
        ).flatMap {
            // зашифровываем или расшифровываем файл записи
            cryptRecordFilesIfNeedUseCase.run(
                CryptRecordFilesIfNeedUseCase.Params(
                    record = destRecord,
                    isEncrypted = srcRecord.isCrypted,
                    isEncrypt = isEncrypted
                )
            )
        }.flatMap {
            None.toRight()
        }.onFailure {
            return ifEitherOrNoneSuspend (breakOnFSErrors) {
                Failure.Record.CloneRecordToNode.toLeft()
            }
        }
    }

    private fun encryptFieldIfNeed(fieldValue: String, isEncrypt: Boolean): String? {
        return if (isEncrypt) cryptManager.encryptTextBase64(fieldValue) else fieldValue
    }

    private suspend fun cloneAttachesToRecord(
        srcRecord: TetroidRecord,
        destRecord: TetroidRecord,
        isCutting: Boolean
    ) : Either<Failure, None> {
        return cloneAttachesToRecordUseCase.run(
            CloneAttachesToRecordUseCase.Params(
                srcRecord = srcRecord,
                destRecord = destRecord,
                isCutting = isCutting,
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

}