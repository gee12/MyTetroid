package com.gee12.mytetroid.domain.usecase.node.icon

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.obj.TetroidNode
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageTreeUseCase

class SetNodeIconUseCase(
    private val logger: ITetroidLogger,
    private val cryptManager: IStorageCryptManager,
    private val loadNodeIconUseCase: LoadNodeIconUseCase,
    private val saveStorageTreeUseCase: SaveStorageTreeUseCase,
) : UseCase<UseCase.None, SetNodeIconUseCase.Params>() {

    data class Params(
        val node: TetroidNode,
        val iconFileName: String?,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val node = params.node
        val iconFileName = params.iconFileName

        logger.logOperStart(LogObj.NODE_FIELDS, LogOper.CHANGE, node)
        val oldIconName = node.sourceIconName
        // обновляем поля
        val isEncrypted = node.isEncrypted
        node.sourceIconName = if (isEncrypted && iconFileName != null) {
            cryptManager.encryptTextBase64(iconFileName)
        } else {
            iconFileName
        }
        if (isEncrypted) {
            node.decryptedIconName = iconFileName
        }
        // перезаписываем структуру хранилища в файл
        return saveStorageTreeUseCase.run()
            .flatMap {
                loadNodeIconUseCase.run(
                    LoadNodeIconUseCase.Params(node)
                )
            }.onFailure {
                logger.logOperCancel(LogObj.NODE_FIELDS, LogOper.CHANGE)
                // возвращаем изменения
                node.sourceIconName = oldIconName
                if (isEncrypted) {
                    node.decryptedIconName = oldIconName?.let { cryptManager.decryptTextBase64(it) }
                }
            }
    }

}