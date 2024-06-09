package com.gee12.mytetroid.data.xml

import android.util.Xml
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.format
import com.gee12.mytetroid.common.extensions.orZero
import com.gee12.mytetroid.common.extensions.toDate
import com.gee12.mytetroid.common.onFailure
import kotlin.Throws
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.manager.FavoritesManager
import com.gee12.mytetroid.domain.usecase.node.icon.LoadNodeIconUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import org.jdom2.output.XMLOutputter
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.domain.provider.IStorageInfoProvider
import com.gee12.mytetroid.model.obj.*
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
    fun reset()
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

    private var isNeedDecrypt = false

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

    private val rootNode = TetroidNode(
        id = "",
        sourceName = "<root>",
        level = -1,
    )

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
        isLoadFavoritesOnly = false
        isExistCryptedNodes = false
        isNeedDecrypt = false
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

    override fun reset() {
        init()
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
        init()

        this.isNeedDecrypt = isNeedDecrypt
        this.isLoadFavoritesOnly = isLoadFavoritesOnly

        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(fis, null)
            nextTag()
        }
        return readRoot(parser).also {
            isLoaded = it
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
        val nodes: MutableList<TetroidNode>? = if (!isLoadFavoritesOnly) mutableListOf() else null

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
        nodes?.also {
            rootNode.subNodes = nodes
        }
        rootNodes = nodes ?: mutableListOf()
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

        parser.require(XmlPullParser.START_TAG, ns, "node")
        var tagName = parser.name
        val node = if (isLoadFavoritesOnly) {
            FavoritesManager.FAVORITES_NODE
        } else if (tagName == "node") {
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

            if (id == null || name == null) {
                skip(parser)
                parser.require(XmlPullParser.END_TAG, ns, "node")
                return null
            }
            TetroidNode(
                id = id,
                sourceName = name,
                isEncrypted = crypt,
                sourceIconName = iconName,
                level = depthLevel,
                parentNode = parentNode,
            )
        } else  {
            return null
        }

        val subNodes: MutableList<TetroidNode>? = if (!isLoadFavoritesOnly) mutableListOf() else null
        var records: List<TetroidRecord>? = if (!isLoadFavoritesOnly) mutableListOf() else null
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
        if (!isLoadFavoritesOnly) {
            subNodes?.also {
                node.subNodes = subNodes
            }
            records?.also {
                node.records = records.toMutableList()
            }

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
    private suspend fun readRecords(parser: XmlPullParser, node: TetroidNode): List<TetroidRecord>? {
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
                    records?.add(record)
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
    private suspend fun readRecord(parser: XmlPullParser, node: TetroidNode): TetroidRecord? {
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
            created = parser.getAttributeValue(ns, "ctime")?.toDate(Constants.DATE_TIME_FORMAT)
            dirName = parser.getAttributeValue(ns, "dir")
            fileName = parser.getAttributeValue(ns, "file")
        }
        if (id == null || name == null || dirName == null || fileName == null) {
            skip(parser)
            parser.require(XmlPullParser.END_TAG, ns, "record")
            return null
        }
        val record = TetroidRecord(
            isEncrypted = crypt,
            id = id,
            sourceName = name,
            sourceTagsString = tags,
            sourceAuthor = author,
            sourceUrl = url,
            created = created,
            folderName = dirName,
            fileName = fileName,
            node = node,
        )
        if (!author.isNullOrBlank()) authorsCount++

        // файлы
        var files: List<TetroidFile> = emptyList()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            tagName = parser.name
            if (tagName == "files") {
                files = readAttachedFiles(parser, record)
            } else {
                skip(parser)
            }
        }

        record.attachedFiles = files.toMutableList()
        if (isFavorite) {
            // добавляем избранную запись
            favoritesManager.setObject(record)
        }

        // расшифровка
        if (crypt && isNeedDecrypt) {
            decryptRecord(record)
        }
        if (record.isNonEncryptedOrDecrypted && !record.tagsString.isNullOrBlank()) {
            // парсим метки, если поле не пусто и запись не зашифрована
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
    private fun readAttachedFiles(parser: XmlPullParser, record: TetroidRecord): List<TetroidFile> {
        val files: MutableList<TetroidFile> = ArrayList()

        parser.require(XmlPullParser.START_TAG, ns, "files")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val tagName = parser.name
            if (tagName == "file") {
                val attachedFile = readAttachedFile(parser, record)
                if (attachedFile != null) {
                    files.add(attachedFile)
                }
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
    private fun readAttachedFile(parser: XmlPullParser, record: TetroidRecord): TetroidFile? {
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
        if (id == null || fileName == null) {
            skip(parser)
            parser.require(XmlPullParser.END_TAG, ns, "file")
            return null
        }
        val attachedFile = TetroidFile(
            id = id,
            name = fileName,
            isEncrypted = crypt,
            fileType = type,
            record = record,
        )
        // принудительно вызываем nextTag(), чтобы найти закрытие тега "/>"
        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, ns, "file")
        filesCount++
        return attachedFile
    }

    /**
     * Запись структуры хранилища в xml-файл.
     * @param fos
     * @return
     */
    @Throws(Exception::class)
    override suspend fun save(fos: OutputStream): Boolean {
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
        return true
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
            val isEncrypted = node.isEncrypted
            addAttribute(nodeElem, "crypt", if (isEncrypted) "1" else "")
            addCryptAttribute(nodeElem, node, "icon", node.iconName, node.sourceIconName)
            addAttribute(nodeElem, "id", node.id)
            addCryptAttribute(nodeElem, node, "name", node.name, node.sourceName)
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
            val isEncrypted = record.isEncrypted
            addAttribute(recordElem, "id", record.id)
            addCryptAttribute(recordElem, record, "name", record.name, record.sourceName)
            addCryptAttribute(recordElem, record, "author", record.author, record.sourceAuthor)
            addCryptAttribute(recordElem, record, "url", record.url, record.sourceUrl)
            addCryptAttribute(recordElem, record, "tags", record.tagsString, record.sourceTagsString)
            addAttribute(recordElem, "ctime", record.created?.format("yyyyMMddHHmmss"))
            addAttribute(recordElem, "dir", record.folderName)
            addAttribute(recordElem, "file", record.fileName)
            if (isEncrypted) {
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
            val isEncrypted = file.isEncrypted
            addAttribute(fileElem, "id", file.id)
            addCryptAttribute(fileElem, file, "fileName", file.name, file.sourceName)
            addAttribute(fileElem, "type", file.fileType)
            if (isEncrypted) {
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
        addAttribute(elem, name, if (obj.isEncrypted) cryptedValue else value)
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
        if (node.isEncrypted) cryptedNodesCount++
        if (node.level > maxDepthLevel) maxDepthLevel = node.level
        if (node.recordsCount > 0) {
            for (record in node.records) {
                recordsCount++
                if (node.isEncrypted) cryptedRecordsCount++
                if (!record.author.isNullOrBlank()) authorsCount++
                if (!record.tagsString.isNullOrBlank()) {
                    tagsCount += record.tagsString?.split(Constants.TAGS_SEPARATOR_MASK.toRegex())?.size.orZero()
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
                tagsString = record.tagsString.orEmpty(),
            )
        ).onFailure {
            logger.logFailure(it, show = false)
        }
    }

}