package com.gee12.mytetroid.domain.usecase

import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.model.obj.TetroidObject
import com.gee12.mytetroid.model.enums.TetroidObjectType

/**
 * Получение объекта хранилища по ссылке.
 */
class ParseObjectFromUrlUseCase : UseCase<TetroidObject?, ParseObjectFromUrlUseCase.Params>() {

    data class Params(
        val url: String,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidObject?> {
        var url = params.url
        val tetroidObject = if (url.startsWith(Constants.MYTETRA_LINK_PREFIX)) {
            url = url.replace("${Constants.MYTETRA_LINK_PREFIX}//", "")
            TetroidObjectType.values().firstNotNullOfOrNull {
                url.parseObjectByPrefix(it)
            }
        } else {
            null
        }
        return Either.Right(tetroidObject)
    }

    private fun String.parseObjectByPrefix(objectType: TetroidObjectType): TetroidObject? {
        val prefix = objectType.getPrefix()
        return if (!prefix.isNullOrEmpty() && startsWith(prefix)) {
            val id = replace("${prefix}/", "").trimEnd('/')
            TetroidObject(
                id = id,
                type = objectType,
                sourceName = "",
            )
        } else {
            null
        }
    }

}