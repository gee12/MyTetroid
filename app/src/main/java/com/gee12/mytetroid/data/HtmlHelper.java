package com.gee12.mytetroid.data;

import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeFilter;
import org.jsoup.select.NodeTraversor;

public class HtmlHelper {

    public static final String HTML_START_WITH =
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0//EN\" \"http://www.w3.org/TR/REC-html40/strict.dtd\">\n" +
                    "<html><head>" +
                    "<meta name=\"qrichtext\" content=\"1\" />" +
                    "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />" +
                    "<style type=\"text/css\">\n" +
                    "p, li { white-space: pre-wrap; }\n" +
                    "</style></head>" +
                    "<body style=\" font-family:'DejaVu Sans'; font-size:11pt; font-weight:400; font-style:normal;\">";
    public static final String HTML_END_WITH = "</body></html>";


    /**
     * Формирование списка меток в виде html-кода.
     * @return
     */
    public static String createTagsHtmlStringFull(TetroidRecord record) {
        if (record == null)
            return null;
        StringBuilder sb = new StringBuilder();
        int size = record.getTags().size();
        if (size == 0) {
            return null;
        }
        // #a4a4e4 - это colorLightLabelText
        sb.append("<body style=\"font-family:'DejaVu Sans';color:#a4a4e4;margin:0px;\">");
        for (TetroidTag tag : record.getTags()) {
            // #303F9F - это colorBlueDark
            sb.append("<a href=\"").append(TetroidTag.LINKS_PREFIX).append(tag.getName()).append("\" style=\"color:#303F9F;\">").
                    append(tag.getName()).append("</a>");
            if (--size > 0)
                sb.append(", ");
        }
        sb.append("</body>");
        return sb.toString();
    }

    /**
     * Формирование списка меток в виде html-кода.
     * @return
     */
    public static String createTagsHtmlString(TetroidRecord record) {
        if (record == null)
            return null;
        StringBuilder sb = new StringBuilder();
        int size = record.getTags().size();
        if (size == 0) {
            return null;
        }
        for (TetroidTag tag : record.getTags()) {
            sb.append("<a href=\"").append(TetroidTag.LINKS_PREFIX).append(tag.getName()).append("\">").
                    append(tag.getName()).append("</a>");
            if (--size > 0)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * Получение содержимого элемента html в виде текста.
     * @param element
     * @return
     */
    public static String elementToText(Element element) {
        final StringBuilder buffer = new StringBuilder();

        NodeTraversor.filter(new NodeFilter() {
            boolean isNewline = true;

            @Override
            public FilterResult head(Node node, int depth) {
                if (node instanceof TextNode) {
                    TextNode textNode = (TextNode) node;
                    String text = textNode.text().replace('\u00A0', ' ').trim();
                    if (!text.isEmpty()) {
                        buffer.append(text);
                        isNewline = false;
                    }
                } else if (node instanceof Element) {
                    Element element = (Element) node;
                    if (!isNewline) {
                        if ((element.isBlock() || element.tagName().equals("br"))) {
                            buffer.append("\n");
                            isNewline = true;
                        }
                    }
                }
                return FilterResult.CONTINUE;
            }

            @Override
            public FilterResult tail(Node node, int depth) {
                return null;
            }
        }, element);

        return buffer.toString();
    }
}
