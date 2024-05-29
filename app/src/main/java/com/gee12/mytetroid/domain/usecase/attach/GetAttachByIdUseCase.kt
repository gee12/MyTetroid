package com.gee12.mytetroid.domain.usecase.attach

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.TetroidFile

class GetAttachByIdUseCase(
    private val storageProvider: IStorageProvider,
) : UseCase<TetroidFile, GetAttachByIdUseCase.Params>() {

    data class Params(
        val attachId: String,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidFile> {
        val attachId = params.attachId

        return getNodeInHierarchy(
            nodes = storageProvider.getRootNodes(),
            attachId = attachId,
        )?.toRight()
            ?: Failure.Attach.NotFound(attachId).toLeft()
    }

    private fun getNodeInHierarchy(nodes: List<TetroidNode>, attachId: String): TetroidFile? {
        for (node in nodes) {
            for (record in node.records) {
                for (attach in record.attachedFiles) {
                    if (attach.id == attachId) {
                        return attach
                    }
                }
            }
            if (node.isExpandable) {
                val found = getNodeInHierarchy(node.subNodes, attachId)
                if (found != null) {
                    return found
                }
            }
        }
        return null
    }

}