package com.gee12.mytetroid.domain.usecase.node.icon

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.domain.usecase.image.LoadDrawableFromFileUseCase
import com.gee12.mytetroid.model.TetroidNode

class LoadNodeIconUseCase(
    private val loadDrawableFromFileUseCase: LoadDrawableFromFileUseCase,
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
            node.icon = if (!node.iconName.isNullOrEmpty()) {
                loadDrawableFromFileUseCase.run(
                    LoadDrawableFromFileUseCase.Params(
                        relativeIconPath = node.iconName
                    )
                ).foldResult(
                    onLeft = { null },
                    onRight = { it }
                )
            } else {
                null
            }
        }
        return None.toRight()
    }
}