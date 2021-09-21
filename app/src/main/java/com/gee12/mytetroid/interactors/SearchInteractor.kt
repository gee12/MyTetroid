package com.gee12.mytetroid.interactors

import android.content.Context
import com.gee12.mytetroid.model.SearchProfile
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.ITetroidObject
import com.gee12.mytetroid.model.FoundType
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidTag
import java.util.ArrayList
import java.util.HashMap
import java.util.regex.Pattern

/**
 * Поиск объектов хранилища.
 * Название такое, а не SearchManager, из-за существования одноименного класса в пакете android.app.
 */
class SearchInteractor(
    private val profile: SearchProfile,
    private val storageInteractor: StorageInteractor,
    private val nodesInteractor: NodesInteractor,
    private val recordsInteractor: RecordsInteractor
) {

    /**
     * Словарь найденных объектов.
     */
    lateinit var foundObjects: HashMap<ITetroidObject, FoundType>

    /**
     * Найдены зашифрованные ветки.
     */
    var isExistCryptedNodes = false
        private set

    /**
     * Глобальный поиск.
     * @param context
     * @return
     */
    suspend fun globalSearch(context: Context): HashMap<ITetroidObject, FoundType> {
        foundObjects = HashMap()

        profile.node = profile.nodeId?.let { nodesInteractor.getNode(it) }
        if (profile.isSplitToWords) {
            for (word in profile.query.split(QUERY_SEPAR).toTypedArray()) {
                foundObjects.putAll(globalSearch(context, profile.node, word)!!)
            }
        } else {
            foundObjects.putAll(globalSearch(context, profile.node, profile.query)!!)
        }
        return foundObjects
    }

    /**
     * Глобальный поиск
     * @param node
     * @param query
     * @return
     */
    private suspend fun globalSearch(context: Context, node: TetroidNode?, query: String): HashMap<ITetroidObject, FoundType>? {
        val srcNodes: List<TetroidNode>
        if (profile.isSearchInNode) {
            if (node != null) {
                srcNodes = ArrayList()
                srcNodes.add(node)
            } else {
                return foundObjects
            }
        } else {
            srcNodes = storageInteractor.getRootNodes()
        }
        val regex = buildRegex(query, profile.isOnlyWholeWords)
        // поиск по веткам, записям, реквизитам записей, файлам
//        boolean inRecords = inRecordsNames || inText || inAuthor || inUrl || inFiles || inIds
//                // 2 - если при поиске по меткам добавляем в результат сами записи, а не метки
//                || inTags;
        val inRecords = profile.isInRecords()
        if (profile.inNodes || inRecords) {
            globalSearchInNodes(context, srcNodes, regex, inRecords)
        }
        // поиск по всем меткам в базе, если не указана ветка для поиска
        // 2 - если используем тип (2), то комментируем
//        if (inTags && !isSearchInNode) {
//            globalSearchInTags(DataManager.getTags(), regex, isOnlyWholeWords);
//        }
        return foundObjects
    }

    /**
     * Глобальный поиск по названиям веток.
     * Пропускает зашифрованные ветки.
     * @param nodes
     * @param regex
     * @param inRecords
     */
    private suspend fun globalSearchInNodes(context: Context, nodes: List<TetroidNode>, regex: Regex, inRecords: Boolean) {
        for (node in nodes) {
            if (!node.isNonCryptedOrDecrypted) {
                isExistCryptedNodes = true
                continue
            }
            // поиск по именам веток
            if (profile.inNodes && node.name.matches(regex)) {
                addFoundObject(node, FoundType.TYPE_NODE)
            }
            // поиск по id веток
            if (profile.inIds && node.id.matches(regex)) {
                addFoundObject(node, FoundType.TYPE_NODE_ID)
            }
            if (inRecords && node.recordsCount > 0) {
                globalSearchInRecords(context, node.records, regex)
            }
            if (node.subNodesCount > 0) {
                globalSearchInNodes(context, node.subNodes, regex, inRecords)
            }
        }
    }

    /**
     * Поиск по названиям записей.
     * @param srcRecords
     * @param regex
     * @return
     */
    private suspend fun globalSearchInRecords(context: Context, srcRecords: List<TetroidRecord>, regex: Regex) {
        for (record in srcRecords) {
            // поиск по именам записей
            if (profile.inRecordsNames && record.name.matches(regex)) {
                addFoundObject(record, FoundType.TYPE_RECORD)
            }
            // поиск по авторам
            if (profile.inAuthor && record.author.matches(regex)) {
                addFoundObject(record, FoundType.TYPE_AUTHOR)
            }
            // поиск по ссылкам
            if (profile.inUrl && record.author.matches(regex)) {
                addFoundObject(record, FoundType.TYPE_URL)
            }
            // поиск по файлам записи
            if (profile.inFiles && record.attachedFilesCount > 0) {
                globalSearchInFiles(record.attachedFiles, regex)
            }
            // поиск по id записей
            if (profile.inIds && record.id.matches(regex)) {
                addFoundObject(record, FoundType.TYPE_RECORD_ID)
            }
            // поиск по тексту записи (читаем текст html файла)
            if (profile.inText) {
                val text = recordsInteractor.getRecordTextDecrypted(context, record)
                if (text != null && text.matches(regex)) {
                    addFoundObject(record, FoundType.TYPE_RECORD_TEXT)
                }
            }
            // поиск по меткам (только если указана ветка для поиска)
            // 2 - комментируем isSearchInNode, если используем тип (2)
            if (profile.inTags /*&& isSearchInNode*/) {
                // 1 - добавляем саму метку в результат
//                globalSearchInTags(record.getTags(), query, isOnlyWholeWords);
                // 2 - добавляем запись, содержащую метку
                if (record.tagsString.matches(regex)) {
                    addFoundObject(record, FoundType.TYPE_TAG)
                }
            }
        }
    }

    /**
     * Глобальный поиск по файлам записи.
     * @param files
     * @param regex
     * @return
     */
    private suspend fun globalSearchInFiles(files: List<TetroidFile>, regex: Regex) {
        for (file in files) {
            // поиск по именам файлов
            if (file.name.matches(regex)) {
                addFoundObject(file, FoundType.TYPE_FILE)
            }
            // поиск по id файлов
            if (file.id.matches(regex)) {
                addFoundObject(file, FoundType.TYPE_FILE_ID)
            }
        }
    }

    /**
     * Глобальный поиск по меткам.
     * Не используется в связи с переходом на 2й тип поиска по меткам.
     * @param tags
     * @param regex
     */
    suspend fun globalSearchInTags(tags: List<TetroidTag>, regex: Regex) {
        for (tagEntry in tags) {
            if (tagEntry.name.matches(regex)) {
                // 1 - добавляем саму метку в результат
//                addFoundObject(tagEntry, FoundType.TYPE_TAG);
                // 2 - добавляем записи метки в результат
                for (record in tagEntry.records) {
                    addFoundObject(record, FoundType.TYPE_TAG)
                }
            }
        }
    }

    /**
     * Добавление типа найденного объекта (в зависимости от того, где он найден).
     * @param obj
     * @param foundType
     */
    private fun addFoundObject(obj: ITetroidObject, foundType: Int) {
        if (foundObjects!!.containsKey(obj)) {
            foundObjects!![obj]!!.addValue(foundType)
        } else {
            foundObjects!![obj] = FoundType(foundType)
        }
    }

    companion object {
        const val QUERY_SEPAR = " "

        /**
         * Поиск по названиям веток (рекурсивно с подветками).
         * Пропускает зашифрованные ветки.
         * @param nodes
         * @param query
         * @return
         */
        suspend fun searchInNodesNames(nodes: List<TetroidNode>, query: String): List<TetroidNode> {
            val regex = buildRegex(query)
            return searchInNodesNamesRecursively(nodes, regex)
        }

        private suspend fun searchInNodesNamesRecursively(nodes: List<TetroidNode>, regex: Regex): List<TetroidNode> {
            val res: MutableList<TetroidNode> = ArrayList()
            for (node in nodes) {
                if (!node.isNonCryptedOrDecrypted) continue
                if (node.name.matches(regex)) {
                    res.add(node)
                    continue
                }
                if (node.subNodesCount > 0) {
                    res.addAll(searchInNodesNamesRecursively(node.subNodes, regex))
                }
            }
            return res
        }

        /**
         * Поиск по названиям записей.
         * @param srcRecords
         * @param query
         * @return
         */
        suspend fun searchInRecordsNames(srcRecords: List<TetroidRecord>, query: String): List<TetroidRecord> {
            val found: MutableList<TetroidRecord> = ArrayList()
            val regex = buildRegex(query)
            for (record in srcRecords) {
                if (record.name.matches(regex)) {
                    found.add(record)
                }
            }
            return found
        }

        /**
         * Поиск по файлам записи.
         * @param files
         * @param query
         * @return
         */
        suspend fun searchInFiles(files: List<TetroidFile>, query: String): List<TetroidFile> {
            val found: MutableList<TetroidFile> = ArrayList()
            val regex = buildRegex(query)
            for (file in files) {
                if (file.name.matches(regex)) {
                    found.add(file)
                }
            }
            return found
        }

        /**
         * Поиск по меткам.
         * @param tags
         * @param query
         * @return
         */
        suspend fun searchInTags(tags: List<TetroidTag>, query: String): List<TetroidTag> {
            val found: MutableList<TetroidTag> = ArrayList()
            val regex = buildRegex(query)
            for (tag in tags) {
                if (tag.name.matches(regex)) {
                    found.add(tag)
                }
            }
            return found
        }

        suspend fun searchInTags(tags: Map<String, TetroidTag>, query: String): Map<String, TetroidTag> {
            val found: MutableMap<String, TetroidTag> = HashMap()
            val regex = buildRegex(query)
            for ((key, value) in tags) {
//            if (tag.getValue().getName().matches(regex)) {
                if (key.matches(regex)) {
                    found[key] = value
                }
            }
            return found
        }

        /**
         * Формирование регулярного выражения для поиска.
         * i - CASE_INSENSITIVE - регистронезависимый режим
         * s - DOTALL - режим одной строки (\n и \r не играют роли)
         * @param query
         * @return
         */
        private fun buildRegex(query: String): Regex {
            return Regex("(?is)" + ".*" + Pattern.quote(query) + ".*")
        }

        private fun buildRegex(query: String, isOnlyWholeWords: Boolean): Regex {
            val boundary = if (isOnlyWholeWords) "\\b" else ""
            // Pattern.quote - помещаем запрос между \Q и \E, чтобы он интерпритировался "как есть"
            return Regex("(?is).*%s%s%s.*".format(boundary, Pattern.quote(query), boundary))
        }
    }
}