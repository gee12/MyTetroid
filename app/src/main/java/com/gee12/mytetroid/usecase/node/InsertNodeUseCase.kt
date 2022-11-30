package com.gee12.mytetroid.usecase.node

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.interactors.DataInteractor
import com.gee12.mytetroid.interactors.EncryptionInteractor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidTag
import com.gee12.mytetroid.usecase.LoadNodeIconUseCase
import com.gee12.mytetroid.usecase.record.CloneRecordToNodeUseCase
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import java.util.ArrayList
import java.util.HashMap

/**
 * Вставка ветки в указанную родительскую ветку.
 * @param isCut Если true, то запись была вырезана. Иначе - скопирована
 */
class InsertNodeUseCase(
    private val logger: ITetroidLogger,
    private val dataInteractor: DataInteractor,
    private val loadNodeIconUseCase: LoadNodeIconUseCase,
    private val cryptInteractor: EncryptionInteractor,
    private val saveStorageUseCase: SaveStorageUseCase,
    private val cloneRecordToNodeUseCase: CloneRecordToNodeUseCase,
) : UseCase<TetroidNode, InsertNodeUseCase.Params>() {

    data class Params(
        val srcNode: TetroidNode,
        val parentNode: TetroidNode,
        val isCut: Boolean,
        val tagsMap: HashMap<String, TetroidTag>,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidNode> {
        val srcNode = params.srcNode
        val parentNode = params.parentNode

        logger.logOperStart(LogObj.NODE, LogOper.INSERT, srcNode)

        return insertNodeRecursively(
            params = params,
            breakOnFSErrors = false
        ).flatMap { newNode ->
            // перезаписываем структуру хранилища в файл
            saveStorageUseCase.run().foldResult(
                onLeft = {
                    logger.logOperCancel(LogObj.NODE, LogOper.INSERT)
                    // удаляем запись из дерева
                    parentNode.subNodes.remove(newNode)
                    it.toLeft()
                },
                onRight = { newNode.toRight() }
            )
        }

    }

    private suspend fun insertNodeRecursively(
        params: Params,
        breakOnFSErrors: Boolean
    ): Either<Failure, TetroidNode> {
        val srcNode = params.srcNode
        val parentNode = params.parentNode
        val isCut = params.isCut

        // генерируем уникальный идентификатор, если ветка копируется
        val id = if (isCut) srcNode.id else dataInteractor.createUniqueId()
        val name = srcNode.name
        val iconName = srcNode.iconName

        // создаем копию ветки
        val crypted = parentNode.isCrypted
        val node = TetroidNode(
            crypted,
            id,
            cryptInteractor.encryptField(crypted, name),
            cryptInteractor.encryptField(crypted, iconName),
            parentNode.level + 1
        )
        node.parentNode = parentNode
        node.records = ArrayList()
        node.subNodes = ArrayList()
        if (crypted) {
            node.setDecryptedName(name)
            node.setDecryptedIconName(iconName)
            node.setIsDecrypted(true)
        }
        // загружаем такую же иконку
        loadNodeIconUseCase.execute(
            LoadNodeIconUseCase.Params(node)
        ).onFailure {
            logger.logFailure(it, show = false)
        }

        // добавляем записи
        if (srcNode.recordsCount > 0) {
            for (srcRecord in srcNode.records) {
                cloneRecordToNode(
                    srcRecord = srcRecord,
                    node = node,
                    isCut = isCut,
                    breakOnFSErrors = breakOnFSErrors,
                    tagsMap = params.tagsMap
                )
                    .onFailure {
                        if (breakOnFSErrors) {
                            return it.toLeft()
                    }
                }
            }
        }
        // добавляем подветки
        for (srcSubNode in srcNode.subNodes) {
            insertNodeRecursively(params, breakOnFSErrors)
                .onFailure {
                    return it.toLeft()
                }
        }

        // добавляем новую ветку в иерархию уже ПОСЛЕ копирования дерева существующих веток,
        //  чтобы не получилась вечная рекурсия в случае, если родительскую ветку копируем в дочернюю
        parentNode.addSubNode(node)

        return node.toRight()
    }

    private suspend fun cloneRecordToNode(
        srcRecord: TetroidRecord,
        node : TetroidNode,
        isCut : Boolean,
        breakOnFSErrors: Boolean,
        tagsMap: HashMap<String, TetroidTag>,
    ) : Either<Failure, None> {
        return cloneRecordToNodeUseCase.run(
            CloneRecordToNodeUseCase.Params(
                srcRecord = srcRecord,
                node = node,
                isCutted = isCut,
                breakOnFSErrors = breakOnFSErrors,
                tagsMap = tagsMap,
            )
        )
    }

}