package com.gee12.mytetroid.domain.usecase.node

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.model.TetroidNode

/**
 * Рекурсивный подсчет дочерних веток и записей в ветке.
 */
class GetNodesAndRecordsCountUseCase(
) : UseCase<GetNodesAndRecordsCountUseCase.Result, GetNodesAndRecordsCountUseCase.Params>() {

    data class Params(
        val node: TetroidNode,
    )

    data class Result(
        val nodesCount: Int,
        val recordsCount: Int,
    )

    suspend fun run(node: TetroidNode): Either<Failure, Result> {
        return run(Params(node))
    }

    override suspend fun run(params: Params): Either<Failure, Result> {
        return getNodesAndRecordsCount(node = params.node)
    }

    private fun getNodesAndRecordsCount(node: TetroidNode): Either<Failure, Result> {
        val subNodesCount = node.subNodesCount

        var nodesCount = subNodesCount
        var recordsCount = node.recordsCount

        if (subNodesCount > 0) {
            for (subNode in node.subNodes) {
                getNodesAndRecordsCount(node = subNode)
                    .onSuccess {
                        nodesCount += it.nodesCount
                        recordsCount += it.recordsCount
                    }.onFailure {
                        return it.toLeft()
                    }
            }
        }
        return Result(
            nodesCount = nodesCount,
            recordsCount = recordsCount
        ).toRight()
    }

}