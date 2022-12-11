package com.gee12.mytetroid.usecase.node

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.model.TetroidNode

class GetNodeByIdUseCase(

) : UseCase<TetroidNode, GetNodeByIdUseCase.Params>() {

    data class Params(
        val nodeId: String,
        val rootNodes: List<TetroidNode>,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidNode> {
        return getNodeInHierarchy(
            nodes = params.rootNodes,
            nodeId = params.nodeId,
        )
    }

    private fun getNodeInHierarchy(nodes: List<TetroidNode>, nodeId: String): Either<Failure, TetroidNode> {
        for (node in nodes) {
            if (nodeId == node.id) {
                return node.toRight()
            } else if (node.isExpandable) {
                val found = getNodeInHierarchy(node.subNodes, nodeId)
                if (found != null) {
                    return found
                }
            }
        }
        return Failure.Node.NotFound(nodeId).toLeft()
    }

}