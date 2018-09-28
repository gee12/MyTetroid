package com.gee12.mytetroid.data;

import android.net.Uri;
import android.util.Xml;

import com.gee12.mytetroid.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Пока не реализовано:
 * - зашифрованные ветки
 * - чтение файлов
 */
public class XMLManager {

    private static final String ns = null;

    private Version formatVersion;

    /**
     *
     * @param in Файл mytetra.xml
     * @return Иерархический список веток с записями и документами
     * @throws XmlPullParserException
     * @throws IOException
     */
    public List<TetroidNode> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readRoot(parser);
        } finally {
            in.close();
        }
    }

    private List<TetroidNode> readRoot(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<TetroidNode> nodes = new ArrayList();
        parser.require(XmlPullParser.START_TAG, ns, "root");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("format")) {
                formatVersion = readFormatVersion(parser);
            } else if (tagName.equals("content")) {
                nodes = readContent(parser);
            }
        }
        return nodes;
    }

    private Version readFormatVersion(XmlPullParser parser) throws XmlPullParserException, IOException {
        int version = 0;
        int subversion = 0;
        parser.require(XmlPullParser.START_TAG, ns, "format");
        String tagName = parser.getName();
        if (tagName.equals("format")) {
            version = Integer.parseInt(parser.getAttributeValue(ns, "version"));
            subversion = Integer.parseInt(parser.getAttributeValue(ns, "subversion"));
        }
        parser.require(XmlPullParser.END_TAG, ns, "format");
        return new Version(version, subversion);
    }

    private List<TetroidNode> readContent(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<TetroidNode> nodes = new ArrayList();
        parser.require(XmlPullParser.START_TAG, ns, "content");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("node")) {
                nodes.add(readNode(parser));
            } else {
                skip(parser);
            }
        }
        return nodes;
    }

    private TetroidNode readNode(XmlPullParser parser) throws XmlPullParserException, IOException {
        boolean crypt = false;
        String id = null;
        String name = null;
        String icon = null; // например: "/Gnome/color_gnome_2_computer.svg"
        parser.require(XmlPullParser.START_TAG, ns, "node");
        String tagName = parser.getName();
        if (tagName.equals("node")) {
            crypt = ("1".equals(parser.getAttributeValue(ns, "crypt")));
            id = parser.getAttributeValue(ns, "id");
            name = parser.getAttributeValue(ns, "name");
            icon = parser.getAttributeValue(ns, "icon");
        }
        List<TetroidNode> subNodes = new ArrayList<>();
        List<TetroidRecord> records = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            tagName = parser.getName();
            if (tagName.equals("recordtable")) {
                // записи
                records = readRecords(parser);
            } else if (tagName.equals("node")) {
                // вложенная ветка
                subNodes.add(readNode(parser));
            } else {
                skip(parser);
            }
        }
        return new TetroidNode(id, name, icon, crypt, subNodes, records);
    }

    private List<TetroidRecord> readRecords(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<TetroidRecord> records = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, ns, "recordtable");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            tagName = parser.getName();
            if (tagName.equals("record")) {
                records.add(readRecord(parser));
            } else {
                skip(parser);
            }
        }
        return records;
    }

    private TetroidRecord readRecord(XmlPullParser parser) throws XmlPullParserException, IOException {
        String id = null;
        String name = null;
        String author = null;
        String url = null;
        Date created = null;
        String dirName = null;
        String fileName = null;
        parser.require(XmlPullParser.START_TAG, ns, "record");
        String tagName = parser.getName();
        if (tagName.equals("record")) {
            id = parser.getAttributeValue(ns, "id");
            name = parser.getAttributeValue(ns, "name");
            author = parser.getAttributeValue(ns, "author");
            url = parser.getAttributeValue(ns, "url");
            // строка вида "yyyyMMddHHmmss" (например, "20180901211132")
            created = Utils.toDate(parser.getAttributeValue(ns, "created"), "yyyyMMddHHmmss");
            dirName = parser.getAttributeValue(ns, "dirName");
            fileName = parser.getAttributeValue(ns, "fileName");
        }
        // файлы
//        List<TetroidRecord> records = new ArrayList<>();
//        while (parser.next() != XmlPullParser.END_TAG) {
//            if (parser.getEventType() != XmlPullParser.START_TAG) {
//                continue;
//            }
//            tagName = parser.getName();
//            if (tagName.equals("recordtable")) {
//                records = readRecords(parser);
//            } else if (tagName.equals("node")) {
//                subNodes.add(readNode(parser));
//            } else {
//                skip(parser);
//            }
//        }
        return new TetroidRecord(id, name, author, url, created, dirName, fileName);
    }

    /**
     * This is how it works:
     * - It throws an exception if the current event isn't a START_TAG.
     * - It consumes the START_TAG, and all events up to and including the matching END_TAG.
     * - To make sure that it stops at the correct END_TAG and not at the first tag it encounters after the original START_TAG, it keeps track of the nesting depth.
     * @param parser
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
