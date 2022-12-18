package com.gee12.mytetroid.usecase.crypt

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.data.crypt.IStorageCrypter
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.usecase.node.icon.LoadNodeIconUseCase

/**
 * Расшифровка хранилища (временная).
 */
class DecryptStorageUseCase(
    private val logger: ITetroidLogger,
    private val storageCrypter: IStorageCrypter,
    private val storageDataProcessor: IStorageDataProcessor,
    private val loadNodeIconUseCase: LoadNodeIconUseCase,
) : UseCase<Boolean, DecryptStorageUseCase.Params>() {

    data class Params(
        val decryptFiles: Boolean
    )

    override suspend fun run(params: Params): Either<Failure, Boolean> {
        return storageCrypter.decryptNodes(
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