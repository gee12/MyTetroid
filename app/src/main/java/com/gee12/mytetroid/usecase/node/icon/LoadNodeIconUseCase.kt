package com.gee12.mytetroid.usecase.node.icon

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.helpers.IStoragePathProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidNode
import java.lang.Exception

class LoadNodeIconUseCase(
    private val logger: ITetroidLogger,
    private val storagePathProvider: IStoragePathProvider,
) : UseCase<UseCase.None, LoadNodeIconUseCase.Params>() {

    data class Params(
        val node: TetroidNode,
    )

    suspend fun run(node: TetroidNode): Either<Failure, None> {
        return run(Params(node))
    }

    override suspend fun run(params: Params): Either<Failure, None> {
        val node = params.node

        if (params.node.isNonCryptedOrDecrypted) {
            node.icon = if (node.iconName.isNullOrEmpty()) {
                null
            } else {
                try {
                    FileUtils.loadSVGFromFile(storagePathProvider.getPathToFileInIconsFolder(node.iconName))
                } catch (ex: Exception) {
                    logger.logError(ex, show = false)
                    null
                }
            }
        }
        return None.toRight()
    }
}