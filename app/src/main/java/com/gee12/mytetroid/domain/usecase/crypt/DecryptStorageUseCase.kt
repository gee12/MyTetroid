package com.gee12.mytetroid.domain.usecase.crypt

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.domain.usecase.node.icon.LoadNodeIconUseCase

/**
 * Расшифровка хранилища (временная).
 */
class DecryptStorageUseCase(
    private val logger: ITetroidLogger,
    private val cryptManager: IStorageCryptManager,
    private val storageDataProcessor: IStorageDataProcessor,
    private val loadNodeIconUseCase: LoadNodeIconUseCase,
) : UseCase<Boolean, DecryptStorageUseCase.Params>() {

    data class Params(
        val decryptFiles: Boolean
    )

    override suspend fun run(params: Params): Either<Failure, Boolean> {
        return cryptManager.decryptNodes(
            nodes = storageDataProcessor.getRootNodes(),
            isDecryptSubNodes = true,
            isDecryptRecords = true,
            loadIconCallback = { node ->
                loadNodeIconUseCase.execute(
                    LoadNodeIconUseCase.Params(node)
                ).onFailure {
                    logger.logFailure(it, show = false)
                }
            },
            isDropCrypt = false,
            isDecryptFiles = params.decryptFiles
        ).toRight()
    }

}