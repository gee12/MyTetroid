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

    fun run(node: TetroidNode): Either<Failure, Result> {
        return getNodesAndRecordsCount(Params(node))
    }

    override suspend fun run(params: Params): Either<Failure, Result> {
        return getNodesAndRecordsCount(params)
    }

    private fun getNodesAndRecordsCount(params: Params): Either<Failure, Result> {
        val node = params.node
        val subNodesCount = node.subNodesCount

        var nodesCount = subNodesCount
        var recordsCount = node.recordsCount

        if (subNodesCount > 0) {
            for (subNode in node.subNodes) {
                getNodesAndRecordsCount(params)
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