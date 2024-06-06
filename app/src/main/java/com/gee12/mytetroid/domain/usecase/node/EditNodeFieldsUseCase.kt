package com.gee12.mytetroid.domain.usecase.node

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.obj.TetroidNode
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageTreeUseCase

/**
 * Изменение свойств ветки.
 */
class EditNodeFieldsUseCase(
    private val logger: ITetroidLogger,
    private val cryptManager: IStorageCryptManager,
    private val saveStorageTreeUseCase: SaveStorageTreeUseCase,
) : UseCase<UseCase.None, EditNodeFieldsUseCase.Params>() {

    data class Params(
        val node: TetroidNode,
        val name: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val node = params.node
        val name = params.name

        if (name.isEmpty()) {
            return Failure.Node.NameIsEmpty.toLeft()
        }
        logger.logOperStart(LogObj.NODE_FIELDS, LogOper.CHANGE, node)
        val oldName = node.sourceName
        // обновляем поля
        val isEncrypted = node.isEncrypted
        node.sourceName = encryptFieldIfNeed(name, isEncrypted) ?: name
        if (isEncrypted) {
            node.decryptedName = name
        }
        // перезаписываем структуру хранилища в файл
        return saveStorageTreeUseCase.run()
            .onFailure {
                logger.logOperCancel(LogObj.NODE_FIELDS, LogOper.CHANGE)
                // возвращаем изменения
                node.sourceName = oldName
                if (isEncrypted) {
                    node.decryptedName = cryptManager.decryptTextBase64(oldName)
                }
            }
    }

    private fun encryptFieldIfNeed(value: String?, isEncrypt: Boolean): String? {
        return if (isEncrypt) value?.let { cryptManager.encryptTextBase64(value) } else value
    }

}