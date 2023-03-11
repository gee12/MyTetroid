package com.gee12.mytetroid.data.xml

import android.util.Xml
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.onFailure
import kotlin.Throws
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.manager.FavoritesManager
import com.gee12.mytetroid.domain.usecase.node.icon.LoadNodeIconUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import org.jdom2.output.XMLOutputter
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.domain.provider.IStorageInfoProvider
import org.jdom2.DocType
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.output.Format
import org.jdom2.output.LineSeparator
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import java.lang.Exception
import java.util.*

interface IStorageDataProcessor : IStorageInfoProvider {
    var isExistCryptedNodes: Boolean

    fun init()
    @Throws(XmlPullParserException::class, IOException::class)
    suspend fun parse(
        fis: InputStream,
        isNeedDecrypt: Boolean,
        isLoadFavoritesOnly: Boolean
    ): Boolean
    @Throws(Exception::class)
    suspend fun save(fos: OutputStream): Boolean
    fun isLoaded(): Boolean
    fun isLoadFavoritesOnlyMode(): Boolean
    fun getRootNodes(): List<TetroidNode>
    fun getTagsMap(): HashMap<String, TetroidTag>
    fun getRootNode(): TetroidNode
}

/**
 * Класс для загрузки и сохранения структуры хранилища в файле mytetra.xml.
 */
open class StorageDataXmlProcessor(
    private val logger: ITetroidLogger,
    private val cryptManager: IStorageCryptManager,
    private val favoritesManager: FavoritesManager,
    private val parseRecordTagsUseCase: ParseRecordTagsUseCase,
    private val loadNodeIconUseCase: LoadNodeIconUseCase,
) : IStorageDataProcessor {

    companion object {
        /**
         * Версия формата структуры хранилища.
         */
        val DEF_VERSION = Version(1, 2)

    }

    private val ns: String? = null

    override var isExistCryptedNodes = false // а вообще можно читать из crypt_mode=1

    var isNeedDecrypt = false

    override var formatVersion: Version? = null
    override var nodesCount: Int = 0
    override var cryptedNodesCount: Int = 0
    override var recordsCount: Int = 0
    override var cryptedRecordsCount: Int = 0
    override var filesCount: Int = 0
    override var tagsCount: Int = 0
    override var uniqueTagsCount: Int = 0
    override var authorsCount: Int = 0
    override var maxSubnodesCount: Int = 0
    override var maxDepthLevel: Int = 0

    private val rootNode = TetroidNode("", "<root>", -1)

    /**
     * Загружено ли хранилище.
     */
    private var isLoaded = false
    override fun isLoaded() = isLoaded

    /**
     * Режим, когда загружаются только избранные записи.
     */
    private var isLoadFavoritesOnly = false
    override fun isLoadFavoritesOnlyMode() = isLoadFavoritesOnly

    /**
     * Список корневых веток.
     */
    private var rootNodes: MutableList<TetroidNode> = ArrayList()
    override fun getRootNodes() = rootNodes

    /**
     * Корневая ветка. Используется для добавления временных записей, которые
     * в mytetra.xml не записываются.
     */
    override fun getRootNode() = rootNode

    /**
     * Список меток.
     */
    private var tagsMap: HashMap<String, TetroidTag> = HashMap()
    override fun getTagsMap() = tagsMap

    /**
     * Первоначальная инициализация переменных.
     */
    override fun init() {
        rootNodes.clear()
        tagsMap.clear()
        formatVersion = DEF_VERSION
        isLoaded = false
        // счетчики
        nodesCount = 0
        cryptedNodesCount = 0
        recordsCount = 0
        cryptedRecordsCount = 0
        filesCount = 0
        tagsCount = 0
        uniqueTagsCount = 0
        authorsCount = 0
        maxSubnodesCount = 0
        maxDepthLevel = 0
        rootNode.subNodes = rootNodes
    }

    /**
     * Чтение хранилища из xml-файла.
     * @param fis
     * @return Иерархический список веток с записями и документами
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    override suspend fun parse(
        fis: InputStream,
        isNeedDecrypt: Boolean,
        isLoadFavoritesOnly: Boolean
    ): Boolean {
        this.isNeedDecrypt = isNeedDecrypt
        this.isLoadFavoritesOnly = isLoadFavoritesOnly

        init()

        return fis.use {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(fis, null)
                nextTag()
            }
            readRoot(parser).also {
                isLoaded = it
            }
        }
    }

    /**
     * Чтение корневого элемента.
     * @param parser
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private suspend fun readRoot(parser: XmlPullParser): Boolean {
        var res = false

        parser.require(XmlPullParser.START_TAG, ns, "root")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val tagName = parser.name
            if (tagName == "format") {
                formatVersion = readFormatVersion(parser)
            } else if (tagName == "content") {
                res = readContent(parser)
            }
        }
        return res
    }

    /**
     * Чтение версии формата.
     * @param parser
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFormatVersion(parser: XmlPullParser): Version {
        var version = 0
        var subversion = 0

        parser.require(XmlPullParser.START_TAG, ns, "format")
        val tagName = parser.name
        if (tagName == "format") {
            version = parser.getAttributeValue(ns, "version").toInt()
            subversion = parser.getAttributeValue(ns, "subversion").toInt()
        }
        // принудительно вызываем nextTag(), чтобы найти закрытие тега "/>"
        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, ns, "format")
        return Version(version, subversion)
    }

    /**
     * Чтение корневой ветки и далее вглубь дерева.
     * @param parser
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private suspend fun readContent(parser: XmlPullParser): Boolean {
        val nodes: MutableList<TetroidNode>? = if (!isLoadFavoritesOnly) ArrayList() else null

        parser.require(XmlPullParser.START_TAG, ns, "content")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val tagName = parser.name
            if (tagName == "node") {
                val node = readNode(parser, 0, rootNode)
                if (node != null && !isLoadFavoritesOnly) {
                    nodes?.add(node)
                }
                /*if (AppDebug.isRecordsLoadedEnough(mRecordsCount)) {
                    break
                }*/
            } else {
                skip(parser)
            }
        }
        rootNode.subNodes = nodes
        rootNodes = nodes ?: ArrayList()
        return true
    }

    /**
     * Рекурсивное чтение веток.
     * @param parser
     * @param depthLevel
     * @param parentNode
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private suspend fun readNode(parser: XmlPullParser, depthLevel: Int, parentNode: TetroidNode?): TetroidNode? {
        var crypt = false
        var id: String? = null
        var name: String? = null
        var iconName: String? = null // например: "/Gnome/color_gnome_2_computer.svg"
        var node: TetroidNode? = null

        parser.require(XmlPullParser.START_TAG, ns, "node")
        var tagName = parser.name
        if (!isLoadFavoritesOnly) {
            if (tagName == "node") {
                crypt = "1" == parser.getAttributeValue(ns, "crypt")
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
                if (crypt && !isExistCryptedNodes) {
                    isExistCryptedNodes = true
                }
                id = parser.getAttributeValue(ns, "id")
                name = parser.getAttributeValue(ns, "name")
                iconName = parser.getAttributeValue(ns, "icon")
            }
            node = TetroidNode(crypt, id, name, iconName, depthLevel)
            node.parentNode = parentNode
        }

        val subNodes: MutableList<TetroidNode>? = if (!isLoadFavoritesOnly) ArrayList() else null
        var records: List<TetroidRecord>? = if (!isLoadFavoritesOnly) ArrayList() else null
        loop@ while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            tagName = parser.name
            when (tagName) {
                "recordtable" -> {
                    // записи
                    records = readRecords(parser, node)

                    /*if (AppDebug.isRecordsLoadedEnough(recordsCount)) {
                        break@loop
                    }*/
                }
                "node" -> {
                    // вложенная ветка
                    val subNode = readNode(parser, depthLevel + 1, node)
                    if (subNode != null && !isLoadFavoritesOnly) {
                        subNodes?.add(subNode)
                    }
                }
                else -> {
                    skip(parser)
                }
            }
        }
        if (node != null && !isLoadFavoritesOnly) {
            node.subNodes = subNodes
            node.records = records

            // расшифровка
            if (crypt && isNeedDecrypt) {
                decryptNode(node)
            }

            // загрузка иконки из файла (после расшифровки имени иконки)
            loadNodeIcon(node)
            nodesCount++
            if (crypt) cryptedNodesCount++
            if ((subNodes?.size ?: 0) > maxSubnodesCount) maxSubnodesCount = subNodes?.size ?: 0
            if (depthLevel > maxDepthLevel) maxDepthLevel = depthLevel
        }
        return node
    }

    /**
     *
     * @param parser
     * @param node
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private suspend fun readRecords(parser: XmlPullParser, node: TetroidNode?): List<TetroidRecord>? {
        val records: MutableList<TetroidRecord>? = if (!isLoadFavoritesOnly) ArrayList() else null

        parser.require(XmlPullParser.START_TAG, ns, "recordtable")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val tagName = parser.name
            if (tagName == "record") {
                val record = readRecord(parser, node)
                if (record != null && !isLoadFavoritesOnly) {
                    records!!.add(record)
                }

                /*if (AppDebug.isRecordsLoadedEnough(recordsCount)) {
                    return records
                }*/
            } else {
                skip(parser)
            }
        }
        return records
    }

    /**
     * Чтение записи.
     * @param parser
     * @param node
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private suspend fun readRecord(parser: XmlPullParser, node: TetroidNode?): TetroidRecord? {
        var crypt = false
        var id: String? = null
        var name: String? = null
        var tags: String? = null
        var author: String? = null
        var url: String? = null
        var created: Date? = null
        var dirName: String? = null
        var fileName: String? = null
        var isFavorite = false

        parser.require(XmlPullParser.START_TAG, ns, "record")
        var tagName = parser.name
        if (tagName == "record") {
            crypt = "1" == parser.getAttributeValue(ns, "crypt")
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
            id = parser.getAttributeValue(ns, "id")

            // проверяем id на избранность
            isFavorite = favoritesManager.isFavorite(id)
            if (isLoadFavoritesOnly && !isFavorite) {
                // выходим, т.к. загружаем только избранные записи
                skip(parser) // пропускаем <files>, если есть
                parser.require(XmlPullParser.END_TAG, ns, "record")
                return null
            }
            name = parser.getAttributeValue(ns, "name")
            tags = parser.getAttributeValue(ns, "tags")
            author = parser.getAttributeValue(ns, "author")
            url = parser.getAttributeValue(ns, "url")
            // строка формата "yyyyMMddHHmmss" (например, "20180901211132")
            created = Utils.toDate(parser.getAttributeValue(ns, "ctime"), Constants.DATE_TIME_FORMAT)
            dirName = parser.getAttributeValue(ns, "dir")
            fileName = parser.getAttributeValue(ns, "file")
        }
        val record = TetroidRecord(crypt, id, name, tags, author, url, created, dirName, fileName, node)
        if (!author.isNullOrBlank()) authorsCount++

        // файлы
        var files: List<TetroidFile> = ArrayList()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            tagName = parser.name
            if (tagName == "files") {
                files = readFiles(parser, record)
            } else {
                skip(parser)
            }
        }

        record.attachedFiles = files
        if (isFavorite) {
            // добавляем избранную запись
            favoritesManager.setObject(record)
        }

        // расшифровка
        if (crypt && isNeedDecrypt) {
            decryptRecord(record)
        }
        if (record.isNonCryptedOrDecrypted) {
            // парсим метки, если запись не зашифрована
            parseRecordTags(record)
        }
        parser.require(XmlPullParser.END_TAG, ns, "record")
        if (!isLoadFavoritesOnly) {
            recordsCount++
            if (crypt) cryptedRecordsCount++
        }
        return record
    }

    /**
     * Чтение прикрепленных файлов.
     * @param parser
     * @param record
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFiles(parser: XmlPullParser, record: TetroidRecord): List<TetroidFile> {
        val files: MutableList<TetroidFile> = ArrayList()

        parser.require(XmlPullParser.START_TAG, ns, "files")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val tagName = parser.name
            if (tagName == "file") {
                files.add(readFile(parser, record))
            } else {
                skip(parser)
            }
        }
        return files
    }

    /**
     * Чтение прикрепленного файла.
     * @param parser
     * @param record
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFile(parser: XmlPullParser, record: TetroidRecord): TetroidFile {
        var crypt = false
        var id: String? = null
        var fileName: String? = null
        var type: String? = null

        parser.require(XmlPullParser.START_TAG, ns, "file")
        val tagName = parser.name
        if (tagName == "file") {
            id = parser.getAttributeValue(ns, "id")
            fileName = parser.getAttributeValue(ns, "fileName")
            type = parser.getAttributeValue(ns, "type")
            crypt = "1" == parser.getAttributeValue(ns, "crypt")
        }
        // принудительно вызываем nextTag(), чтобы найти закрытие тега "/>"
        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, ns, "file")
        filesCount++
        return TetroidFile(crypt, id, fileName, type, record)
    }

    /**
     * Запись структуры хранилища в xml-файл.
     * @param fos
     * @return
     */
    @Throws(Exception::class)
    override suspend fun save(fos: OutputStream): Boolean {
        return fos.use {
            // параметры XML
            val format = Format.getPrettyFormat()
            format.encoding = "UTF-8"
            format.indent = " "
            format.setLineSeparator(LineSeparator.UNIX)
            val xmlOutput = XMLOutputter(format, TetroidXMLProcessor())
            val doc = Document()
            doc.docType = DocType("mytetradoc")

            // root
            val rootElem = Element("root")
            doc.rootElement = rootElem

            // format
            val formatElem = Element("format")
            formatElem.setAttribute("version", formatVersion!!.major.toString())
            formatElem.setAttribute("subversion", formatVersion!!.minor.toString())
            rootElem.addContent(formatElem)

            // content
            val contentElem = Element("content")
            saveNodes(contentElem, rootNodes)
            rootElem.addContent(contentElem)
            xmlOutput.output(doc, fos)
            true
        }
    }

    /**
     * Сохранение структуры подветок ветки.
     * @param parentElem
     * @param nodes
     * @throws IOException
     */
    @Throws(Exception::class)
    private fun saveNodes(parentElem: Element, nodes: List<TetroidNode>) {
        for (node in nodes) {
            val nodeElem = Element("node")
            val crypted = node.isCrypted
            addAttribute(nodeElem, "crypt", if (crypted) "1" else "")
            addCryptAttribute(nodeElem, node, "icon", node.iconName.orEmpty(), node.getIconName(true).orEmpty())
            addAttribute(nodeElem, "id", node.id)
            addCryptAttribute(nodeElem, node, "name", node.name, node.getName(true))
            if (node.recordsCount > 0) {
                saveRecords(nodeElem, node.records)
            }
            if (node.subNodesCount > 0) {
                saveNodes(nodeElem, node.subNodes)
            }
            parentElem.addContent(nodeElem)
        }
    }

    /**
     * Сохранение структуры записей ветки.
     * @param parentElem
     * @param records
     * @throws IOException
     */
    @Throws(Exception::class)
    protected fun saveRecords(parentElem: Element, records: List<TetroidRecord>) {
        val recordsElem = Element("recordtable")
        for (record in records) {
            val recordElem = Element("record")
            val crypted = record.isCrypted
            addAttribute(recordElem, "id", record.id)
            addCryptAttribute(recordElem, record, "name", record.name, record.getName(true))
            addCryptAttribute(recordElem, record, "author", record.author, record.getAuthor(true))
            addCryptAttribute(recordElem, record, "url", record.url, record.getUrl(true))
            addCryptAttribute(recordElem, record, "tags", record.tagsString, record.getTagsString(true))
            addAttribute(recordElem, "ctime", record.getCreatedString("yyyyMMddHHmmss"))
            addAttribute(recordElem, "dir", record.dirName)
            addAttribute(recordElem, "file", record.fileName)
            if (crypted) {
                addAttribute(recordElem, "crypt", "1")
            }
            if (record.attachedFilesCount > 0) {
                saveFiles(recordElem, record.attachedFiles)
            }
            recordsElem.addContent(recordElem)
        }
        parentElem.addContent(recordsElem)
    }

    /**
     * Сохранение структуры прикрепленных файлов записи.
     * @param parentElem
     * @param files
     * @throws IOException
     */
    @Throws(Exception::class)
    protected fun saveFiles(parentElem: Element, files: List<TetroidFile>) {
        val filesElem = Element("files")
        for (file in files) {
            val fileElem = Element("file")
            val crypted = file.isCrypted
            addAttribute(fileElem, "id", file.id)
            addCryptAttribute(fileElem, file, "fileName", file.name, file.getName(true))
            addAttribute(fileElem, "type", file.fileType)
            if (crypted) {
                addAttribute(fileElem, "crypt", "1")
            }
            filesElem.addContent(fileElem)
        }
        parentElem.addContent(filesElem)
    }

    @Throws(Exception::class)
    private fun addAttribute(elem: Element, name: String, value: String?) {
        elem.setAttribute(name, value.orEmpty())
    }

    @Throws(Exception::class)
    private fun addCryptAttribute(elem: Element, obj: TetroidObject, name: String, value: String?, cryptedValue: String?) {
        addAttribute(elem, name, if (obj.isCrypted) cryptedValue else value)
    }

    /**
     * Пересчет статистических счетчиков хранилища.
     */
    override fun calcCounters() {
        nodesCount = 0
        cryptedNodesCount = 0
        recordsCount = 0
        cryptedRecordsCount = 0
        filesCount = 0
        tagsCount = 0
        uniqueTagsCount = 0
        authorsCount = 0
        maxSubnodesCount = 0
        maxDepthLevel = 0
        for (node in rootNodes) {
            calcCounters(node)
        }
        uniqueTagsCount = tagsMap.keys.size
    }

    /**
     * Пересчет статистических счетчиков ветки.
     */
    private fun calcCounters(node: TetroidNode?) {
        if (node == null) return
        nodesCount++
        if (node.isCrypted) cryptedNodesCount++
        if (node.level > maxDepthLevel) maxDepthLevel = node.level
        if (node.recordsCount > 0) {
            for (record in node.records) {
                recordsCount++
                if (node.isCrypted) cryptedRecordsCount++
                if (!record.author.isNullOrBlank()) authorsCount++
                if (!record.tagsString.isNullOrBlank()) {
                    tagsCount += record.tagsString.split(Constants.TAGS_SEPARATOR_MASK.toRegex()).toTypedArray().size
                }
                if (record.attachedFilesCount > 0) filesCount += record.attachedFilesCount
            }
        }
        val subNodesCount = node.subNodesCount
        if (subNodesCount > 0) {
            if (subNodesCount > maxSubnodesCount) maxSubnodesCount = subNodesCount
            for (subNode in node.subNodes) {
                calcCounters(subNode)
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
    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        check(parser.eventType == XmlPullParser.START_TAG)
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }


    private suspend fun decryptNode(node: TetroidNode): Boolean {
        return cryptManager.decryptNode(
            node = node,
            isDecryptSubNodes = false,
            isDecryptRecords = false,
            loadIconCallback = {
                loadNodeIcon(node)
            },
            isDropCrypt = false,
            isDecryptFiles = false,
        )
    }

    private suspend fun loadNodeIcon(node: TetroidNode) {
        loadNodeIconUseCase.run(
            LoadNodeIconUseCase.Params(node)
        ).onFailure {
            logger.logFailure(it, show = false)
        }
    }

    private suspend fun decryptRecord(record: TetroidRecord): Boolean {
        return cryptManager.decryptRecordAndFiles(
            record = record,
            dropCrypt = false,
            decryptFiles = false
        )
    }

    private suspend fun parseRecordTags(record: TetroidRecord) {
        parseRecordTagsUseCase.run(
            ParseRecordTagsUseCase.Params(
                record = record,
                tagsString = record.tagsString,
//                tagsMap = tagsMap,
            )
        ).onFailure {
            logger.logFailure(it, show = false)
        }
    }

}