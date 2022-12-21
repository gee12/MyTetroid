package com.gee12.mytetroid.domain.usecase.node

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.IStorageCrypter
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase

/**
 * Изменение свойств ветки.
 */
class EditNodeFieldsUseCase(
    private val logger: ITetroidLogger,
    private val storageCrypter: IStorageCrypter,
    private val saveStorageUseCase: SaveStorageUseCase,
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
        val oldName = node.getName(true)
        // обновляем поля
        val isCrypted = node.isCrypted
        node.name = encryptFieldIfNeed(name, isCrypted)
        if (isCrypted) {
            node.setDecryptedName(name)
        }
        // перезаписываем структуру хранилища в файл
        return saveStorageUseCase.run()
            .onFailure {
                logger.logOperCancel(LogObj.NODE_FIELDS, LogOper.CHANGE)
                // возвращаем изменения
                node.name = oldName
                if (isCrypted) {
                    node.setDecryptedName(storageCrypter.decryptTextBase64(oldName))
                }
            }
    }

    private fun encryptFieldIfNeed(fieldValue: String, isEncrypt: Boolean): String? {
        return if (isEncrypt) storageCrypter.encryptTextBase64(fieldValue) else fieldValue
    }

}