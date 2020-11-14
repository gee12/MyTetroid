package com.gee12.mytetroid.data;

import android.content.Context;
import android.text.TextUtils;
import android.util.Xml;

import com.gee12.mytetroid.AppDebug;
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
public abstract class TetroidXml implements INodeIconLoader, ITagsParser {

    /**
     *
     */
    private static final String ns = null;

    /**
     * Формат даты создания записи.
     */
    public static final String DATE_TIME_FORMAT = "yyyyMMddHHmmss";

    /**
     * Разделитель меток - запятая или запятая с пробелами.
     */
    protected static final String TAGS_SEPAR = "\\s*,\\s*";

    public static final Version DEF_VERSION = new Version(1, 2);

    public static final TetroidNode ROOT_NODE = new TetroidNode("", "<root>", -1);

    protected Version mFormatVersion;
    protected boolean mIsExistCryptedNodes;  // а вообще можно читать из crypt_mode=1

    protected boolean mIsNeedDecrypt;

    /**
     * Загружено ли хранилище.
     */
    protected boolean mIsStorageLoaded;

    /**
     * Режим, когда загружаются только избранные записи.
     */
    protected boolean mIsFavoritesMode;

    /**
     * Список корневых веток.
     */
    protected List<TetroidNode> mRootNodesList;

    /**
     * Список меток.
     */
    protected TreeMap<String, TetroidTag> mTagsMap;

    /**
     * Статистические данные.
     */
    protected int mNodesCount;
    protected int mCryptedNodesCount;
    protected int mRecordsCount;
    protected int mCryptedRecordsCount;
    protected int mFilesCount;
    protected int mTagsCount;
    protected int mUniqueTagsCount;
    protected int mAuthorsCount;
    protected int mIconsCount;
    protected int mMaxSubnodesCount;
    protected int mMaxDepthLevel;

    /**
     * Обработчик события о необходимости расшифровки ветки (без дочерних объектов)
     * сразу после загрузки ветки из xml.
     * @param node
     * @return
     */
    protected abstract boolean decryptNode(Context context, TetroidNode node);

    /**
     * Обработчик события о необходимости расшифровки записи (вместе с прикрепленными файлами)
     * сразу после загрузки записи из xml.
     * @param record
     * @return
     */
    protected abstract boolean decryptRecord(Context context, TetroidRecord record);

    /**
     * Проверка является ли запись избранной.
     * @param id
     * @return
     */
    protected abstract boolean isRecordFavorite(String id);

    /**
     * Добавление записи в избранное.
     * @param record
     * @return
     */
    protected abstract void addRecordFavorite(TetroidRecord record);

    /**
     * Первоначальная инициализация переменных.
     */
    public void init() {
//        this.mFavoritesRecords = new ArrayList<>();
        this.mRootNodesList = new ArrayList<>();
        ROOT_NODE.setSubNodes(mRootNodesList);
        this.mTagsMap = new TreeMap<>(new TetroidTag.TagsComparator());
        this.mFormatVersion = DEF_VERSION;
        this.mIsStorageLoaded = false;
        // счетчики
        this.mNodesCount = 0;
        this.mCryptedNodesCount = 0;
        this.mRecordsCount = 0;
        this.mCryptedRecordsCount = 0;
        this.mFilesCount = 0;
        this.mTagsCount = 0;
        this.mUniqueTagsCount = 0;
        this.mAuthorsCount = 0;
        this.mIconsCount = 0;
        this.mMaxSubnodesCount = 0;
        this.mMaxDepthLevel = 0;
    }

    /**
     * Чтение хранилища из xml-файла.
     * @param in
     * @return Иерархический список веток с записями и документами
     * @throws XmlPullParserException
     * @throws IOException
     */
    public boolean parse(Context context, InputStream in, boolean isNeedDecrypt, boolean favoriteMode)
            throws XmlPullParserException, IOException {
        this.mIsNeedDecrypt = isNeedDecrypt;
        this.mIsFavoritesMode = favoriteMode;
        init();
        boolean res = false;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            res = readRoot(context, parser);
        } finally {
            in.close();
        }
        this.mIsStorageLoaded = res;
        return res;
    }

    /**
     *
     * @param parser
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    private boolean readRoot(Context context, XmlPullParser parser) throws XmlPullParserException, IOException {
        boolean res = false;
        parser.require(XmlPullParser.START_TAG, ns, "root");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("format")) {
                this.mFormatVersion = readFormatVersion(parser);
            } else if (tagName.equals("content")) {
                res = readContent(context, parser);
            }
        }
        return res;
    }

    /**
     *
     * @param parser
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
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

    /**
     *
     * @param parser
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    private boolean readContent(Context context, XmlPullParser parser) throws XmlPullParserException, IOException {
        List<TetroidNode> nodes = (!mIsFavoritesMode) ? new ArrayList<>() : null;
        parser.require(XmlPullParser.START_TAG, ns, "content");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("node")) {
                TetroidNode node = readNode(context, parser, 0, ROOT_NODE);
                if (node != null && !mIsFavoritesMode) {
                    nodes.add(node);
                }
                //
                if (AppDebug.isRecordsLoadedEnough(mRecordsCount)) {
                    break;
                }
            } else {
                skip(parser);
            }
        }
        ROOT_NODE.setSubNodes(nodes);
        this.mRootNodesList = nodes;
        return true;
    }

    /**
     *
     * @param parser
     * @param depthLevel
     * @param parentNode
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    private TetroidNode readNode(Context context, XmlPullParser parser, int depthLevel, TetroidNode parentNode)
            throws XmlPullParserException, IOException {
        boolean crypt = false;
        String id = null;
        String name = null;
        String iconName = null; // например: "/Gnome/color_gnome_2_computer.svg"
        TetroidNode node = null;

        parser.require(XmlPullParser.START_TAG, ns, "node");
        String tagName = parser.getName();

        if (!mIsFavoritesMode) {
            if (tagName.equals("node")) {
                crypt = ("1".equals(parser.getAttributeValue(ns, "crypt")));
                // пропуск зашифрованных веток (для отладки)
                /*if (crypt && !AppDebug.isLoadCryptedRecords()) {
                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.getEventType() == XmlPullParser.START_TAG) {
                            skip(parser);
                        }
                    }
                    return null;
                }*/
                // наличие зашифрованных веток
                if (crypt && !mIsExistCryptedNodes) {
                    this.mIsExistCryptedNodes = true;
                }
                id = parser.getAttributeValue(ns, "id");
                name = parser.getAttributeValue(ns, "name");
                iconName = parser.getAttributeValue(ns, "icon");
            }
            node = new TetroidNode(crypt, id, name, iconName, depthLevel);
            node.setParentNode(parentNode);
        }

        List<TetroidNode> subNodes = (!mIsFavoritesMode) ? new ArrayList<>() : null;
        List<TetroidRecord> records = (!mIsFavoritesMode) ? new ArrayList<>() : null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            tagName = parser.getName();
            if (tagName.equals("recordtable")) {
                // записи
                records = readRecords(context, parser, node);
                //
                if (AppDebug.isRecordsLoadedEnough(mRecordsCount)) {
                    break;
                }
            } else if (tagName.equals("node")) {
                // вложенная ветка
                TetroidNode subNode = readNode(context, parser, depthLevel + 1, node);
                if (!mIsFavoritesMode) {
                    subNodes.add(subNode);
                }
            } else {
                skip(parser);
            }
        }

        if (!mIsFavoritesMode) {
            node.setSubNodes(subNodes);
            node.setRecords(records);

            // расшифровка
            if (crypt && mIsNeedDecrypt) {
                decryptNode(context, node);
            }
            /*//else if (!crypt) {
            if (node.isNonCryptedOrDecrypted()) {
                // парсим метки, если запись не зашифрована
                parseNodeTags(node);
            }*/

            // загрузка иконки из файла (после расшифровки имени иконки)
            loadIcon(context, node);

            this.mNodesCount++;
            if (crypt)
                this.mCryptedNodesCount++;
            if (subNodes.size() > mMaxSubnodesCount)
                this.mMaxSubnodesCount = subNodes.size();
            if (depthLevel > mMaxDepthLevel)
                this.mMaxDepthLevel = depthLevel;
            if (!TextUtils.isEmpty(iconName)) {
                this.mIconsCount++;
            }
        }

        return node;
    }

    /**
     *
     * @param parser
     * @param node
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    private List<TetroidRecord> readRecords(Context context, XmlPullParser parser, TetroidNode node) throws XmlPullParserException, IOException {
        List<TetroidRecord> records = (!mIsFavoritesMode) ? new ArrayList<>() : null;

        parser.require(XmlPullParser.START_TAG, ns, "recordtable");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("record")) {
                TetroidRecord record = readRecord(context, parser, node);
                if (record != null && !mIsFavoritesMode) {
                    records.add(record);
                }
                //
                if (AppDebug.isRecordsLoadedEnough(mRecordsCount)) {
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
/*    protected void parseNodeTags(TetroidNode node) {
        for (TetroidRecord record : node.getRecords()) {
            parseRecordTags(record, record.getTagsString());
        }
    }*/

    /**
     * Разбираем строку с метками записи и добавляем метки в запись и в дерево.
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
    private TetroidRecord readRecord(Context context, XmlPullParser parser, TetroidNode node) throws XmlPullParserException, IOException {
        boolean crypt = false;
        String id = null;
        String name = null;
        String tags = null;
        String author = null;
        String url = null;
        Date created = null;
        String dirName = null;
        String fileName = null;
        boolean isFavorite = false;

        parser.require(XmlPullParser.START_TAG, ns, "record");
        String tagName = parser.getName();
        if (tagName.equals("record")) {
            crypt = ("1".equals(parser.getAttributeValue(ns, "crypt")));
            //
            /*if (crypt && !AppDebug.isLoadCryptedRecords()) {
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() == XmlPullParser.START_TAG) {
                        skip(parser);
                    }
                }
                parser.require(XmlPullParser.END_TAG, ns, "record");
                return null;
            }*/
            id = parser.getAttributeValue(ns, "id");

            // проверяем id на избранность
            isFavorite = isRecordFavorite(id);
            if (mIsFavoritesMode && !isFavorite) {
                // выходим, т.к. загружаем только избранные записи
                skip(parser); // пропускаем <files>, если есть
                parser.require(XmlPullParser.END_TAG, ns, "record");
                return null;
            }

            name = parser.getAttributeValue(ns, "name");
            tags = parser.getAttributeValue(ns, "tags");
            author = parser.getAttributeValue(ns, "author");
            url = parser.getAttributeValue(ns, "url");
            // строка формата "yyyyMMddHHmmss" (например, "20180901211132")
            created = Utils.toDate(parser.getAttributeValue(ns, "ctime"), DATE_TIME_FORMAT);
            dirName = parser.getAttributeValue(ns, "dir");
            fileName = parser.getAttributeValue(ns, "file");
        }
        TetroidRecord record = new TetroidRecord(crypt, id, name, tags, author, url, created, dirName, fileName, node);

        if (!StringUtil.isBlank(author))
            this.mAuthorsCount++;
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
            record.setAttachedFiles(files);
        }
//        record.setIsFavorite(isFavorite);
        if (isFavorite) {
            // добавляем избранную запись
//            mFavoritesRecords.add(record);
            addRecordFavorite(record);
        }

        // расшифровка
        if (crypt && mIsNeedDecrypt) {
            decryptRecord(context, record);
        }
        if (record.isNonCryptedOrDecrypted()) {
            // парсим метки, если запись не зашифрована
            parseRecordTags(record, record.getTagsString());
        }

        parser.require(XmlPullParser.END_TAG, ns, "record");

        if (!mIsFavoritesMode) {
            this.mRecordsCount++;
            if (crypt)
                this.mCryptedRecordsCount++;
        }

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

        this.mFilesCount++;

        return new TetroidFile(crypt, id, fileName, type, record);
    }

    /**
     * Запись структуры хранилища в xml-файл.
     * @param fos
     * @return
     */
    public boolean save(FileOutputStream fos) throws IOException {
        if (mRootNodesList == null)
            return false;

        XmlSerializer serializer = Xml.newSerializer();
        try {
            serializer.setOutput(fos, "UTF-8");
            // header:
            // <?xml version='1.0' encoding='UTF-8' ?>
            // FIXME: Но записывается с одинарными кавычками, а нужно с двойными вот так:
            //  <?xml version="1.0" encoding="UTF-8"?>
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
            addAttribute(serializer, "version", String.valueOf(mFormatVersion.getMajor()));
            addAttribute(serializer, "subversion", String.valueOf(mFormatVersion.getMinor()));
            serializer.endTag(ns, "format");
            // content
            serializer.startTag(ns, "content");
            saveNodes(serializer, mRootNodesList);
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
//            if (!TextUtils.isEmpty(node.getIconName())) {
                addCryptAttribute(serializer, node, "icon", node.getIconName(), node.getIconName(true));
//            }
            addAttribute(serializer, "id", node.getId());
            addCryptAttribute(serializer, node, "name", node.getName(), node.getName(true));

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
            addCryptAttribute(serializer, record, "name", record.getName(), record.getName(true));
            addCryptAttribute(serializer, record, "author", record.getAuthor(), record.getAuthor(true));
            addCryptAttribute(serializer, record, "url", record.getUrl(), record.getUrl(true));
            addCryptAttribute(serializer, record, "tags", record.getTagsString(), record.getTagsString(true));
            addAttribute(serializer, "ctime", record.getCreatedString("yyyyMMddHHmmss"));
            addAttribute(serializer, "dir", record.getDirName());
            addAttribute(serializer, "file", record.getFileName());
            if (crypted) {
                addAttribute(serializer, "crypt", "1");
            }
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
            addCryptAttribute(serializer, file, "fileName", file.getName(), file.getName(true));
            addAttribute(serializer, "type", file.getFileType());
            if (crypted) {
                addAttribute(serializer, "crypt", "1");
            }
            serializer.endTag(ns, "file");
        }
        serializer.endTag(ns, "files");
    }

    private void addAttribute(XmlSerializer serializer, String name, String value) throws IOException {
        if (value == null) {
            value = "";
        }
        serializer.attribute(ns, name, value);
    }

//    private void addCryptAttribute(XmlSerializer serializer, TetroidObject obj, String name, String value) throws IOException {
//        addAttribute(serializer, name, cryptValue(obj.isCrypted() && obj.isDecrypted(), value));
//    }

//    private String cryptValue(boolean needEncrypt, String value) {
//        return (needEncrypt) ? encryptField(value) : value;
//    }

    private void addCryptAttribute(XmlSerializer serializer, TetroidObject obj, String name, String value, String cryptedValue)
            throws IOException {
        addAttribute(serializer, name, (obj.isCrypted()) ? cryptedValue : value);
    }

    /**
     * Пересчет статистических счетчиков хранилища.
     */
    public void calcCounters() {
        if (mRootNodesList == null)
            return;

        this.mNodesCount = 0;
        this.mCryptedNodesCount = 0;
        this.mRecordsCount = 0;
        this.mCryptedRecordsCount = 0;
        this.mFilesCount = 0;
        this.mTagsCount = 0;
        this.mUniqueTagsCount = 0;
        this.mAuthorsCount = 0;
        this.mIconsCount = 0;
        this.mMaxSubnodesCount = 0;
        this.mMaxDepthLevel = 0;

        for (TetroidNode node : mRootNodesList) {
            calcCounters(node);
        }
        mUniqueTagsCount = mTagsMap.keySet().size();
    }

    /**
     * Пересчет статистических счетчиков ветки.
     */
    private void calcCounters(TetroidNode node) {
        if (node == null)
            return;
        mNodesCount++;
        if (node.isCrypted())
            mCryptedNodesCount++;
        if (!StringUtil.isBlank(node.getIconName()))
            mIconsCount++;
        if (node.getLevel() > mMaxDepthLevel)
            mMaxDepthLevel = node.getLevel();

        if (node.getRecordsCount() > 0) {
            for (TetroidRecord record : node.getRecords()) {
                mRecordsCount++;
                if (node.isCrypted())
                    mCryptedRecordsCount++;
                if (!StringUtil.isBlank(record.getAuthor()))
                    mAuthorsCount++;
                if (!StringUtil.isBlank(record.getTagsString())) {
                    mTagsCount += record.getTagsString().split(TAGS_SEPAR).length;
                }
                if (record.getAttachedFilesCount() > 0)
                    mFilesCount += record.getAttachedFilesCount();
            }
        }
        int subNodesCount = node.getSubNodesCount();
        if (subNodesCount > 0) {
            if (subNodesCount > mMaxSubnodesCount)
                mMaxSubnodesCount = subNodesCount;

            for (TetroidNode subNode : node.getSubNodes()) {
                calcCounters(subNode);
            }
        }
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
        return mFormatVersion;
    }

    public int getNodesCount() {
        return mNodesCount;
    }

    public int getCryptedNodesCount() {
        return mCryptedNodesCount;
    }

    public int getRecordsCount() {
        return mRecordsCount;
    }

    public int getCryptedRecordsCount() {
        return mCryptedRecordsCount;
    }

    public int getFilesCount() {
        return mFilesCount;
    }

    public int getTagsCount() {
        return mTagsCount;
    }

    public int getUniqueTagsCount() {
        return mUniqueTagsCount;
    }

    public int getAuthorsCount() {
        return mAuthorsCount;
    }

    public int getIconsCount() {
        return mIconsCount;
    }

    public int getMaxSubnodesCount() {
        return mMaxSubnodesCount;
    }

    public int getMaxDepthLevel() {
        return mMaxDepthLevel;
    }
}