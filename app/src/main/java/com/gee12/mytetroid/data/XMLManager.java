package com.gee12.mytetroid.data;

import android.text.TextUtils;
import android.util.Xml;

import com.gee12.mytetroid.AppDebug;
import com.gee12.mytetroid.crypt.CryptManager;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidObject;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;
import com.gee12.mytetroid.model.Version;
import com.gee12.mytetroid.utils.Utils;

import org.jsoup.internal.StringUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

/**
 *
 */
public abstract class XMLManager implements INodeIconLoader, ITagsParseHandler {

    /**
     *
     */
    private static final String ns = null;

    protected Version formatVersion;
    protected boolean isExistCryptedNodes;  // а вообще можно читать из crypt_mode=1

    protected IDecryptHandler decryptCallback;

    /**
     *
     */
    protected List<TetroidNode> rootNodesList;

    /**
     *
     */
    protected TreeMap<String, TetroidTag> tagsMap;

    /**
     * Статистические данные.
     */
    protected int nodesCount;
    protected int cryptedNodesCount;
    protected int recordsCount;
    protected int cryptedRecordsCount;
    protected int filesCount;
    protected int tagsCount;
    protected int uniqueTagsCount;
    protected int authorsCount;
    protected int iconsCount;
    protected int maxSubnodesCount;
    protected int maxDepthLevel;

    /**
     * Чтение хранилища из xml-файла.
     * @param in
     * @return Иерархический список веток с записями и документами
     * @throws XmlPullParserException
     * @throws IOException
     */
    public boolean parse(InputStream in, IDecryptHandler decryptHandler) throws XmlPullParserException, IOException {
        this.decryptCallback = decryptHandler;
        this.rootNodesList = new ArrayList<>();
        this.tagsMap = new TreeMap<>(new TetroidTag.TagsComparator());
        this.nodesCount = 0;
        this.cryptedNodesCount = 0;
        this.recordsCount = 0;
        this.cryptedRecordsCount = 0;
        this.filesCount = 0;
        this.tagsCount = 0;
        this.uniqueTagsCount = 0;
        this.authorsCount = 0;
        this.iconsCount = 0;
        this.maxSubnodesCount = 0;
        this.maxDepthLevel = 0;
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

    private boolean readRoot(XmlPullParser parser) throws XmlPullParserException, IOException {
        boolean res = false;
        parser.require(XmlPullParser.START_TAG, ns, "root");
        while (parser.next() != XmlPullParser.END_TAG) {
//        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("format")) {
                this.formatVersion = readFormatVersion(parser);
            } else if (tagName.equals("content")) {
                res = readContent(parser);
            }
        }
        return res;
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
        // принудительно вызываем nextTag(), чтобы найти закрытие тега "/>"
        parser.nextTag();
        parser.require(XmlPullParser.END_TAG, ns, "format");
        return new Version(version, subversion);
    }

    private boolean readContent(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<TetroidNode> nodes = new ArrayList();
        parser.require(XmlPullParser.START_TAG, ns, "content");
        TetroidNode rootNode = new TetroidNode("", "", 0);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("node")) {
                TetroidNode node = readNode(parser, 0, rootNode);
                if (node != null) {
                    nodes.add(node);
                }
                //
                if (AppDebug.isRecordsLoadedEnough(recordsCount)) {
                    break;
                }
            } else {
                skip(parser);
            }
        }
        rootNode.setSubNodes(nodes);
        this.rootNodesList = nodes;
        return true;
    }

    private TetroidNode readNode(XmlPullParser parser, int depthLevel, TetroidNode parentNode) throws XmlPullParserException, IOException {
        boolean crypt = false;
        String id = null;
        String name = null;
        String iconPath = null; // например: "/Gnome/color_gnome_2_computer.svg"
        parser.require(XmlPullParser.START_TAG, ns, "node");
        String tagName = parser.getName();
        if (tagName.equals("node")) {
            crypt = ("1".equals(parser.getAttributeValue(ns, "crypt")));
            //
            if (crypt && !AppDebug.isLoadCryptedRecords()) {
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() == XmlPullParser.START_TAG) {
                        skip(parser);
                    }
                }
                return null;
            }
            // наличие зашифрованных веток
            if (crypt && !isExistCryptedNodes)
                isExistCryptedNodes = true;
            id = parser.getAttributeValue(ns, "id");
            name = parser.getAttributeValue(ns, "name");
            iconPath = parser.getAttributeValue(ns, "icon");
        }
        TetroidNode node = new TetroidNode(crypt, id, name, iconPath, depthLevel);
        node.setParentNode(parentNode);

        List<TetroidNode> subNodes = new ArrayList<>();
        List<TetroidRecord> records = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            tagName = parser.getName();
            if (tagName.equals("recordtable")) {
                // записи
                records = readRecords(parser, node);
                //
                if (AppDebug.isRecordsLoadedEnough(recordsCount)) {
                    break;
                }
            } else if (tagName.equals("node")) {
                // вложенная ветка
                subNodes.add(readNode(parser, depthLevel+1, node));
            } else {
                skip(parser);
            }
        }
        node.setSubNodes(subNodes);
        node.setRecords(records);

        // расшифровка
        if (crypt && decryptCallback != null) {
            decryptCallback.decryptNode(node);
        }
        //else if (!crypt) {
        if (node.isNonCryptedOrDecrypted()) {
            // парсим метки, если запись не зашифрована
            parseNodeTags(node);
        }

        // загрузка иконки из файла (после расшифровки имени иконки)
        loadIcon(node);

        this.nodesCount++;
        if (crypt)
            this.cryptedNodesCount++;
        if (subNodes.size() > maxSubnodesCount)
            this.maxSubnodesCount = subNodes.size();
        if (depthLevel > maxDepthLevel)
            this.maxDepthLevel = depthLevel;
        if (!TextUtils.isEmpty(iconPath)) {
            this.iconsCount++;
        }

        return node;
    }

    private List<TetroidRecord> readRecords(XmlPullParser parser, TetroidNode node) throws XmlPullParserException, IOException {
        List<TetroidRecord> records = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, ns, "recordtable");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("record")) {
                TetroidRecord record = readRecord(parser, node);
                if (record != null) {
                    records.add(record);
                }
                //
                if (AppDebug.isRecordsLoadedEnough(recordsCount)) {
                    return records;
                }
            } else {
                skip(parser);
            }
        }
        return records;
    }

    /**
     * Разбираем метки у незашифрованных записей ветки.
     * @param node
     */
    protected void parseNodeTags(TetroidNode node) {
        for (TetroidRecord record : node.getRecords()) {
            parseRecordTags(record, record.getTagsString());
        }
    }

    /**
     * Разбираем строку с метками записи и добавляем метки в запись и в коллекцию.
     * @param record
     * @param tagsString Строка с метками (не зашифрована).
     *                   Передается отдельно, т.к. поле в записи может быть зашифровано.
     */
    public abstract void parseRecordTags(TetroidRecord record, String tagsString);

    /**
     * Удаление меток записи из списка.
     * @param record
     */
    public abstract void deleteRecordTags(TetroidRecord record);

    /**
     *
     * @param parser
     * @param node
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    private TetroidRecord readRecord(XmlPullParser parser, TetroidNode node) throws XmlPullParserException, IOException {
        boolean crypt = false;
        String id = null;
        String name = null;
        String tags = null;
        String author = null;
        String url = null;
        Date created = null;
        String dirName = null;
        String fileName = null;
        parser.require(XmlPullParser.START_TAG, ns, "record");
        String tagName = parser.getName();
        if (tagName.equals("record")) {
            crypt = ("1".equals(parser.getAttributeValue(ns, "crypt")));
            //
            if (crypt && !AppDebug.isLoadCryptedRecords()) {
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() == XmlPullParser.START_TAG) {
                        skip(parser);
                    }
                }
                parser.require(XmlPullParser.END_TAG, ns, "record");
                return null;
            }
            id = parser.getAttributeValue(ns, "id");
            name = parser.getAttributeValue(ns, "name");
            tags = parser.getAttributeValue(ns, "tags");
            author = parser.getAttributeValue(ns, "author");
            url = parser.getAttributeValue(ns, "url");
            // строка вида "yyyyMMddHHmmss" (например, "20180901211132")
            created = Utils.toDate(parser.getAttributeValue(ns, "ctime"), "yyyyMMddHHmmss");
            dirName = parser.getAttributeValue(ns, "dir");
            fileName = parser.getAttributeValue(ns, "file");
        }
        TetroidRecord record = new TetroidRecord(crypt, id, name, tags, author, url, created, dirName, fileName, node);

        if (!StringUtil.isBlank(author))
            this.authorsCount++;
//        parser.nextTag();
//        parser.require(XmlPullParser.END_TAG, ns, "record");
        // файлы
        List<TetroidFile> files = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            tagName = parser.getName();
            if (tagName.equals("files")) {
                files = readFiles(parser, record);
            } else {
                skip(parser);
            }
        }
        if (files != null) {
            record.setFiles(files);
        }
        parser.require(XmlPullParser.END_TAG, ns, "record");

        if (crypt)
            this.cryptedRecordsCount++;
        this.recordsCount++;

        return record;
    }

    /**
     *
     * @param parser
     * @param record
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    private List<TetroidFile> readFiles(XmlPullParser parser, TetroidRecord record) throws XmlPullParserException, IOException {
        List<TetroidFile> files = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, ns, "files");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("file")) {
                files.add(readFile(parser, record));
            } else {
                skip(parser);
            }
        }
        return files;
    }

    /**
     *
     * @param parser
     * @param record
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    private TetroidFile readFile(XmlPullParser parser, TetroidRecord record) throws XmlPullParserException, IOException {
        boolean crypt = false;
        String id = null;
        String fileName = null;
        String type = null;
        parser.require(XmlPullParser.START_TAG, ns, "file");
        String tagName = parser.getName();
        if (tagName.equals("file")) {
            id = parser.getAttributeValue(ns, "id");
            fileName = parser.getAttributeValue(ns, "fileName");
            type = parser.getAttributeValue(ns, "type");
            crypt = ("1".equals(parser.getAttributeValue(ns, "crypt")));
        }
        // принудительно вызываем nextTag(), чтобы найти закрытие тега "/>"
        parser.nextTag();
        parser.require(XmlPullParser.END_TAG, ns, "file");

        this.filesCount++;

        return new TetroidFile(crypt, id, fileName, type, record);
    }

    /**
     * Запись структуры хранилища в xml-файл.
     * @param fos
     * @return
     */
    public boolean save(FileOutputStream fos) throws IOException {
        XmlSerializer serializer = Xml.newSerializer();
        try {
            serializer.setOutput(fos, "UTF-8");
            // header
            serializer.startDocument("UTF-8", null);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            // попытка изменить установить размер отступа
//            serializer.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-indentation", " ");
//            serializer.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-line-separator", "\n");
            // исправлем отступ на следующую строку
            serializer.flush();
            fos.write("\n".getBytes());
            serializer.docdecl(" mytetradoc");
            // root
            serializer.startTag(ns, "root");
            // format
            serializer.startTag(ns, "format");
            addAttribute(serializer, "version", String.valueOf(formatVersion.getMajor()));
            addAttribute(serializer, "subversion", String.valueOf(formatVersion.getMinor()));
            serializer.endTag(ns, "format");
            // content
            serializer.startTag(ns, "content");
            saveNodes(serializer, rootNodesList);
            serializer.endTag(ns, "content");

            serializer.endTag(ns, "root");
            serializer.endDocument();
            serializer.flush();
            return true;
        } finally {
            fos.close();
        }
    }

    /**
     * Сохранение структуры подветок ветки.
     * @param serializer
     * @param nodes
     * @throws IOException
     */
    protected void saveNodes(XmlSerializer serializer, List<TetroidNode> nodes) throws IOException {

        for (TetroidNode node : nodes) {
            serializer.startTag(ns, "node");

            boolean crypted = node.isCrypted();
            addAttribute(serializer, "crypt", (crypted) ? "1" : "");
            if (!TextUtils.isEmpty(node.getIconName()))
                addCryptAttribute(serializer, node, "icon", node.getIconName());
            addAttribute(serializer, "id", node.getId());
            addCryptAttribute(serializer, node, "name", node.getName());

            if (node.getRecordsCount() > 0) {
                saveRecords(serializer, node.getRecords());
            }
            if (node.getSubNodesCount() > 0) {
                saveNodes(serializer, node.getSubNodes());
            }
            serializer.endTag(ns, "node");
        }
    }

    /**
     * Сохранение структуры записей ветки.
     * @param serializer
     * @param records
     * @throws IOException
     */
    protected void saveRecords(XmlSerializer serializer, List<TetroidRecord> records) throws IOException {

        serializer.startTag(ns, "recordtable");
        for (TetroidRecord record : records) {
            serializer.startTag(ns, "record");

            boolean crypted = record.isCrypted();
            addAttribute(serializer, "id", record.getId());
            addCryptAttribute(serializer, record, "name", record.getName());
            addCryptAttribute(serializer, record, "author", record.getAuthor());
            addCryptAttribute(serializer, record, "url", record.getUrl());
            addCryptAttribute(serializer, record, "tags", record.getTagsString());
            addAttribute(serializer, "ctime", record.getCreatedString("yyyyMMddHHmmss"));
            addAttribute(serializer, "dir", record.getDirName());
            addAttribute(serializer, "file", record.getFileName());
            if (crypted)
                addAttribute(serializer, "crypt", "1");

            if (record.getAttachedFilesCount() > 0) {
                saveFiles(serializer, record.getAttachedFiles());
            }
            serializer.endTag(ns, "record");
        }
        serializer.endTag(ns, "recordtable");
    }

    /**
     * Сохранение структуры прикрепленных файлов записи.
     * @param serializer
     * @param files
     * @throws IOException
     */
    protected void saveFiles(XmlSerializer serializer, List<TetroidFile> files) throws IOException {

        serializer.startTag(ns, "files");
        for (TetroidFile file : files) {
            serializer.startTag(ns, "file");

            boolean crypted = file.isCrypted();
            addAttribute(serializer, "id", file.getId());
            addCryptAttribute(serializer, file, "fileName", file.getName());
            addAttribute(serializer, "type", file.getFileType());
            if (crypted)
                addAttribute(serializer, "crypt", "1");

            serializer.endTag(ns, "file");
        }
        serializer.endTag(ns, "files");
    }

    private void addAttribute(XmlSerializer serializer, String name, String value) throws IOException {
        if (value == null)
            value = "";
        serializer.attribute(ns, name, value);
    }

    private void addCryptAttribute(XmlSerializer serializer, TetroidObject obj, String name, String value) throws IOException {
        addAttribute(serializer, name, cryptValue(obj.isCrypted() && obj.isDecrypted(), value));
    }

    private String cryptValue(boolean needEncrypt, String value) {
        return (needEncrypt) ? CryptManager.encryptTextBase64(value) : value;
    }

    /**
     * This is how it works:
     * - It throws an exception if the current event isn't a START_TAG.
     * - It consumes the START_TAG, and all events up to and including the matching END_TAG.
     * - To make sure that it stops at the correct END_TAG and not at the first tag it encounters
     * after the original START_TAG, it keeps track of the nesting depth.
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

    public Version getFormatVersion() {
        return formatVersion;
    }

    public int getNodesCount() {
        return nodesCount;
    }

    public int getCryptedNodesCount() {
        return cryptedNodesCount;
    }

    public int getRecordsCount() {
        return recordsCount;
    }

    public int getCryptedRecordsCount() {
        return cryptedRecordsCount;
    }

    public int getFilesCount() {
        return filesCount;
    }

    public int getTagsCount() {
        return tagsCount;
    }

    public int getUniqueTagsCount() {
        return uniqueTagsCount;
    }

    public int getAuthorsCount() {
        return authorsCount;
    }

    public int getIconsCount() {
        return iconsCount;
    }

    public int getMaxSubnodesCount() {
        return maxSubnodesCount;
    }

    public int getMaxDepthLevel() {
        return maxDepthLevel;
    }
}
