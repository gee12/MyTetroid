package com.gee12.mytetroid.domain.usecase.node

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.domain.provider.IDataNameProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase

/**
 * Создание ветки.
 */
class CreateNodeUseCase(
    private val logger: ITetroidLogger,
    private val dataNameProvider: IDataNameProvider,
    private val storageProvider: IStorageProvider,
    private val cryptManager: IStorageCryptManager,
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
            return Failure.Node.NameIsEmpty.toLeft()
        }
        logger.logOperStart(LogObj.NODE, LogOper.CREATE)

        // генерируем уникальные идентификаторы
        val id: String = dataNameProvider.createUniqueId()
        val isEncrypted = (parentNode != null && parentNode.isCrypted)
        val level = if (parentNode != null) parentNode.level + 1 else 0
        val node = TetroidNode(
            isEncrypted,
            id,
            encryptFieldIfNeed(name, isEncrypted),
            null,
            level,
        )
        node.parentNode = parentNode
        node.records = ArrayList()
        node.subNodes = ArrayList()
        if (isEncrypted) {
            node.setDecryptedName(name)
            node.setIsDecrypted(true)
        }

        // добавляем запись в родительскую ветку (и соответственно, в дерево), если она задана
        val nodesList = (if (parentNode != null) {
            parentNode.subNodes
        } else {
            storageProvider.getRootNodes()
        }) as MutableList<TetroidNode>
        nodesList.add(node)

        // перезаписываем структуру хранилища в файл
        return saveStorageUseCase.run()
            .foldResult(
                onLeft = { failure ->
                    logger.logOperCancel(LogObj.NODE, LogOper.CREATE)
                    // удаляем запись из дерева
                    nodesList.remove(node)
                    failure.toLeft()
                },
                onRight = {
                    node.toRight()
                }
            )
    }

    private fun encryptFieldIfNeed(fieldValue: String, isEncrypt: Boolean): String? {
        return if (isEncrypt) cryptManager.encryptTextBase64(fieldValue) else fieldValue
    }

}