package com.gee12.mytetroid.domain.usecase.html

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidTag

/**
 * Формирование списка меток в виде html-кода.
 */
class CreateTagsHtmlStringUseCase : UseCase<String, CreateTagsHtmlStringUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
    )

    override suspend fun run(params: Params): Either<Failure, String> {
        val record = params.record

        var size = record.tags.size
        return if (size > 0) {
            buildString {
                for (tag in record.tags) {
                    append("<a href=\"")
                    append(TetroidTag.LINKS_PREFIX)
                    append(tag.name)
                    append("\">")
                    append(tag.name)
                    append("</a>")
                    if (--size > 0) {
                        append(", ")
                    }
                }
            }
        } else {
            ""
        }.toRight()
    }

}