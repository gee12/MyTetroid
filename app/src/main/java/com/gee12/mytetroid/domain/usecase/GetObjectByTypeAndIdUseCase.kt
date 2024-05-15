package com.gee12.mytetroid.domain.usecase

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.domain.usecase.node.GetNodeByIdUseCase
import com.gee12.mytetroid.domain.usecase.record.GetRecordByIdUseCase
import com.gee12.mytetroid.model.TetroidObject
import com.gee12.mytetroid.model.enums.TetroidObjectType

class GetObjectByTypeAndIdUseCase(
    private val getRecordByIdUseCase: GetRecordByIdUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) : UseCase<TetroidObject?, GetObjectByTypeAndIdUseCase.Params>() {

    data class Params(
        val objectId: String,
        val objectTypeId: Int,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidObject?> {
        val objectTypeId = params.objectTypeId
        val objectId = params.objectId

        val type = TetroidObjectType.getById(objectTypeId)
            ?.takeIf { it != TetroidObjectType.NONE }

        val tetroidObject = type?.let {
                when (type) {
                    TetroidObjectType.RECORD -> {
                        getRecordByIdUseCase.run(
                            GetRecordByIdUseCase.Params(recordId = objectId)
                        ).foldResult(
                            onLeft = { null },
                            onRight = { it }
                        )
                    }
                    TetroidObjectType.NODE -> {
                        getNodeByIdUseCase.run(
                            GetNodeByIdUseCase.Params(nodeId = objectId)
                        ).foldResult(
                            onLeft = { null },
                            onRight = { it }
                        )
                    }
                    //TetroidObjectType.TAG -> TODO ?
                    //TetroidObjectType.ATTACH -> TODO ?
                    else -> {
                        TetroidObject(type.id, objectId)
                    }
                }
            }
        return Either.Right(tetroidObject)
    }

}