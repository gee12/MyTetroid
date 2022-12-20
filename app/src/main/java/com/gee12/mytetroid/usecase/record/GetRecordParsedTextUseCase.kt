package com.gee12.mytetroid.usecase.record

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.model.TetroidRecord
import org.jsoup.Jsoup

/**
 * Получение содержимого записи в виде текста.
 */
class GetRecordParsedTextUseCase(
    private val getRecordHtmlTextDecryptedUseCase: GetRecordHtmlTextUseCase,
) : UseCase<String, GetRecordParsedTextUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val showMessage: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, String> {

        return getRecordHtmlTextDecryptedUseCase.run(
            GetRecordHtmlTextUseCase.Params(
                record = params.record,
                showMessage = params.showMessage,
            )
        ).flatMap { html ->
            try {
                Jsoup.parse(html).text().toRight()
            } catch (ex: Exception) {
                Failure.Record.Read.ParseFromHtml(ex).toLeft()
            }
        }
    }

}