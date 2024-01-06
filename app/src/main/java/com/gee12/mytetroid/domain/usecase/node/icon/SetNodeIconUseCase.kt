package com.gee12.mytetroid.domain.usecase.node.icon

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase

class SetNodeIconUseCase(
    private val logger: ITetroidLogger,
    private val cryptManager: IStorageCryptManager,
    private val loadNodeIconUseCase: LoadNodeIconUseCase,
    private val saveStorageUseCase: SaveStorageUseCase,
) : UseCase<UseCase.None, SetNodeIconUseCase.Params>() {

    data class Params(
        val node: TetroidNode,
        val iconFileName: String?,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val node = params.node
        val iconFileName = params.iconFileName

        logger.logOperStart(LogObj.NODE_FIELDS, LogOper.CHANGE, node)
        val oldIconName = node.getIconName(true)
        // обновляем поля
        val isCrypted = node.isCrypted
        node.iconName = if (isCrypted && iconFileName != null) {
            cryptManager.encryptTextBase64(iconFileName)
        } else {
            iconFileName
        }
        if (isCrypted) {
            node.setDecryptedIconName(iconFileName)
        }
        // перезаписываем структуру хранилища в файл
        return saveStorageUseCase.run()
            .flatMap {
                loadNodeIconUseCase.run(
                    LoadNodeIconUseCase.Params(node)
                )
            }.onFailure {
                logger.logOperCancel(LogObj.NODE_FIELDS, LogOper.CHANGE)
                // возвращаем изменения
                node.iconName = oldIconName
                if (isCrypted) {
                    node.setDecryptedIconName(cryptManager.decryptTextBase64(oldIconName))
                }
            }
    }

}