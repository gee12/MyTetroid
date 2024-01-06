package com.gee12.mytetroid.domain.usecase.html

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.toRight
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeFilter
import org.jsoup.select.NodeTraversor

/**
 * Получение содержимого элемента html в виде текста.
 */
class HtmlElementToTextUseCase : UseCase<String, HtmlElementToTextUseCase.Params>() {

    data class Params(
        val element: Element,
    )

    override suspend fun run(params: Params): Either<Failure, String> {
        val buffer = StringBuilder()

        NodeTraversor.filter(object : NodeFilter {
            var isNewline = true

            override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
                if (node is TextNode) {
                    val text = node.text()
                        .replace('\u00A0', ' ')
                        .trim { it <= ' ' }
                    if (text.isNotEmpty()) {
                        buffer.append(text)
                        isNewline = false
                    }
                } else if (node is Element) {
                    if (!isNewline) {
                        if (node.isBlock || node.tagName() == "br") {
                            buffer.append("\n")
                            isNewline = true
                        }
                    }
                }
                return NodeFilter.FilterResult.CONTINUE
            }

            override fun tail(node: Node, depth: Int): NodeFilter.FilterResult? {
                return null
            }
        }, params.element)

        return buffer.toString().toRight()
    }

}