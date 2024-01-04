package com.gee12.mytetroid.domain.usecase.network

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.usecase.html.HtmlElementToTextUseCase
import org.jsoup.Jsoup
import java.lang.Exception

/**
 * Загрузка содержимого веб-страницы по URL.
 */
class DownloadWebPageContentUseCase(
    private val htmlElementToTextUseCase: HtmlElementToTextUseCase,
) : UseCase<String, DownloadWebPageContentUseCase.Params>() {

    data class Params(
        val url: String,
        val isTextOnly: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, String> {
        return try {
            val connection = Jsoup.connect(params.url)
            val doc = connection.get()
            val body = doc.body()
            if (body != null) {
                if (params.isTextOnly) {
                    htmlElementToTextUseCase.run(
                        HtmlElementToTextUseCase.Params(
                            element = body,
                        )
                    )
                } else {
                    body.html().toRight()
                }
            } else {
                "".toRight()
            }
        } catch (ex: Exception) {
            Failure.Network.DownloadWebPageError(ex).toLeft()
        }
    }

}