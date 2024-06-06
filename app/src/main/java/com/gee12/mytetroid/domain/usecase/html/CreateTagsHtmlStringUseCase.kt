package com.gee12.mytetroid.domain.usecase.html

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.model.obj.TetroidRecord
import com.gee12.mytetroid.model.enums.TetroidObjectType

/**
 * Формирование списка меток в виде html-кода.
 */
class CreateTagsHtmlStringUseCase : UseCase<String, CreateTagsHtmlStringUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
    )

    override suspend fun run(params: Params): Either<Failure, String> {
        val record = params.record

        return record.tags.joinToString(separator = ", ") { tag ->
            val tagPrefix = TetroidObjectType.TAG.getPrefix()
            val tagName = tag.name
            "<a href=\"$tagPrefix:$tagName\">$tagName</a>"
        }.toRight()
    }

}