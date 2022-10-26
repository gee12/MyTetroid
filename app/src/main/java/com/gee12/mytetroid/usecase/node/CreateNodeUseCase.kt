package com.gee12.mytetroid.usecase.node

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.helpers.IStorageProvider
import com.gee12.mytetroid.interactors.DataInteractor
import com.gee12.mytetroid.interactors.EncryptionInteractor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import java.util.ArrayList

/**
 * Создание ветки.
 */
class CreateNodeUseCase(
    private val logger: ITetroidLogger,
    private val dataInteractor: DataInteractor,
    private val cryptInteractor: EncryptionInteractor,
    private val storageProvider: IStorageProvider,
    private val saveStorageUseCase: SaveStorageUseCase,
) : UseCase<TetroidNode, CreateNodeUseCase.Params>() {

    data class Params(
        val name: String,
        val parentNode: TetroidNode?,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidNode> {
        val name = params.name
        val parentNode = params.parentNode

        if (name.isEmpty()) {
            return Failure.Node.Create.NameIsEmpty.toLeft()
        }
        logger.logOperStart(LogObj.NODE, LogOper.CREATE)

        // генерируем уникальные идентификаторы
        val id: String = dataInteractor.createUniqueId()
        val crypted = (parentNode != null && parentNode.isCrypted)
        val level = if (parentNode != null) parentNode.level + 1 else 0
        val node = TetroidNode(
            crypted, id,
            cryptInteractor.encryptField(crypted, name),
            null, level
        )
        node.parentNode = parentNode
        node.records = ArrayList()
        node.subNodes = ArrayList()
        if (crypted) {
            node.setDecryptedName(name)
            node.setIsDecrypted(true)
        }

        // добавляем запись в родительскую ветку (и соответственно, в дерево), если она задана
        val list = (if (parentNode != null) parentNode.subNodes else storageProvider.getRootNodes()) as MutableList<TetroidNode>
        list.add(node)

        // перезаписываем структуру хранилища в файл
        return saveStorageUseCase.run()
            .foldResult(
                onLeft = { failure ->
                    logger.logOperCancel(LogObj.NODE, LogOper.CREATE)
                    // удаляем запись из дерева
                    list.remove(node)
                    failure.toLeft()
                },
                onRight = {
                    node.toRight()
                }
            )
    }

}