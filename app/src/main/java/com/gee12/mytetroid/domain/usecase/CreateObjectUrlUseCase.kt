package com.gee12.mytetroid.domain.usecase

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.model.obj.TetroidObject

/**
 * Получение объекта хранилища по ссылке.
 */
class CreateObjectUrlUseCase : UseCase<String, CreateObjectUrlUseCase.Params>() {

    data class Params(
        val obj: TetroidObject,
    )

    override suspend fun run(params: Params): Either<Failure, String> {
        return params.obj.let { obj ->
            val myTetraPrefix = Constants.MYTETRA_LINK_PREFIX
            val prefix = obj.type.getPrefix()
            val id = obj.id
            "$myTetraPrefix//${prefix}/$id"
        }.toRight()
    }
}