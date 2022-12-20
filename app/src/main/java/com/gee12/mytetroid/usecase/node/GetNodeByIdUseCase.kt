package com.gee12.mytetroid.usecase.node

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.providers.IStorageProvider

class GetNodeByIdUseCase(
    private val storageProvider: IStorageProvider,
) : UseCase<TetroidNode, GetNodeByIdUseCase.Params>() {

    data class Params(
        val nodeId: String,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidNode> {
        val nodeId = params.nodeId

        return getNodeInHierarchy(
            nodes = storageProvider.getRootNodes(),
            nodeId = nodeId,
        )?.toRight()
            ?: Failure.Node.NotFound(nodeId).toLeft()
    }

    private fun getNodeInHierarchy(nodes: List<TetroidNode>, nodeId: String): TetroidNode? {
        for (node in nodes) {
            if (nodeId == node.id) {
                return node
            } else if (node.isExpandable) {
                val found = getNodeInHierarchy(node.subNodes, nodeId)
                if (found != null) {
                    return found
                }
            }
        }
        return null
    }

}