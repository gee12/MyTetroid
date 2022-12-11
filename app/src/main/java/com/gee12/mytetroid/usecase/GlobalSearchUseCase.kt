package com.gee12.mytetroid.usecase

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.data.crypt.IStorageCrypter
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.usecase.node.GetNodeByIdUseCase
import com.gee12.mytetroid.usecase.record.GetRecordParsedTextUseCase
import java.util.regex.Pattern

class GlobalSearchUseCase(
    private val logger: ITetroidLogger,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getRecordParsedTextUseCase: GetRecordParsedTextUseCase,
) : UseCase<GlobalSearchUseCase.Result, GlobalSearchUseCase.Params>() {

    // TODO: вернуть зависимости в конструктор
    data class Params(
        val profile: SearchProfile,
        val rootNodes: List<TetroidNode>,
        val storageCrypter: IStorageCrypter,
        val getPathToRecordFolderCallback: (TetroidRecord) -> String,
    )

    data class Result(
        val foundObjects: Map<ITetroidObject, FoundType>,
        var isExistCryptedNodes: Boolean,
    )

    private val QUERY_SEPAR = " "

    private var isExistCryptedNodes = false

    override suspend fun run(params: Params): Either<Failure, Result> {
        val profile = params.profile

        profile.node = profile.nodeId?.let { nodeId ->
            getNodeById(nodeId, params)
        }

        return buildMap {
            if (profile.isSplitToWords) {
                for (word in profile.query.split(QUERY_SEPAR).toTypedArray()) {
                    putAll(
                        globalSearch(
                            params = params,
                            foundObjects = this,
                            node = profile.node,
                            query = word
                        )
                    )
                }
            } else {
                putAll(
                    globalSearch(
                        params = params,
                        foundObjects = this,
                        node = profile.node,
                        query = profile.query
                    )
                )
            }
        }.let {
            Result(
                foundObjects = it,
                isExistCryptedNodes = isExistCryptedNodes,
            )
        }.toRight()
    }

    private suspend fun getNodeById(nodeId: String, params: Params): TetroidNode? {
        return getNodeByIdUseCase.run(
            GetNodeByIdUseCase.Params(
                nodeId = nodeId,
                rootNodes = params.rootNodes,
            )
        ).foldResult(
            onLeft = { null },
            onRight = { it }
        )
    }

    private suspend fun globalSearch(
        params: Params,
        foundObjects: MutableMap<ITetroidObject, FoundType>,
        node: TetroidNode?,
        query: String
    ): Map<ITetroidObject, FoundType> {
        val srcNodes: List<TetroidNode>
        val profile = params.profile

        if (profile.isSearchInNode) {
            if (node != null) {
                srcNodes = ArrayList()
                srcNodes.add(node)
            } else {
                return foundObjects
            }
        } else {
            srcNodes = params.rootNodes
        }
        val regex = buildRegex(query, profile.isOnlyWholeWords)
        // поиск по веткам, записям, реквизитам записей, файлам
//        boolean inRecords = inRecordsNames || inText || inAuthor || inUrl || inFiles || inIds
//                // 2 - если при поиске по меткам добавляем в результат сами записи, а не метки
//                || inTags;
        val inRecords = profile.isInRecords()
        if (profile.inNodes || inRecords) {
            globalSearchInNodes(params, foundObjects, srcNodes, regex, inRecords)
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
    private suspend fun globalSearchInNodes(
        params: Params,
        foundObjects: MutableMap<ITetroidObject, FoundType>,
        nodes: List<TetroidNode>,
        regex: Regex,
        inRecords: Boolean
    ) {
        val profile = params.profile

        for (node in nodes) {
            if (!node.isNonCryptedOrDecrypted) {
                isExistCryptedNodes = true
                continue
            }
            // поиск по именам веток
            if (profile.inNodes && node.name.matches(regex)) {
                addFoundObject(foundObjects, node, FoundType.TYPE_NODE)
            }
            // поиск по id веток
            if (profile.inIds && node.id.matches(regex)) {
                addFoundObject(foundObjects, node, FoundType.TYPE_NODE_ID)
            }
            if (inRecords && node.recordsCount > 0) {
                globalSearchInRecords(params, foundObjects, node.records, regex)
            }
            if (node.subNodesCount > 0) {
                globalSearchInNodes(params, foundObjects, node.subNodes, regex, inRecords)
            }
        }
    }

    /**
     * Поиск по названиям записей.
     * @param srcRecords
     * @param regex
     * @return
     */
    private suspend fun globalSearchInRecords(
        params: Params,
        foundObjects: MutableMap<ITetroidObject, FoundType>,
        srcRecords: List<TetroidRecord>,
        regex: Regex
    ) {
        val profile = params.profile

        for (record in srcRecords) {
            // поиск по именам записей
            if (profile.inRecordsNames && record.name.matches(regex)) {
                addFoundObject(foundObjects, record, FoundType.TYPE_RECORD)
            }
            // поиск по авторам
            if (profile.inAuthor && record.author.matches(regex)) {
                addFoundObject(foundObjects, record, FoundType.TYPE_AUTHOR)
            }
            // поиск по ссылкам
            if (profile.inUrl && record.author.matches(regex)) {
                addFoundObject(foundObjects, record, FoundType.TYPE_URL)
            }
            // поиск по файлам записи
            if (profile.inFiles && record.attachedFilesCount > 0) {
                globalSearchInFiles(foundObjects, record.attachedFiles, regex)
            }
            // поиск по id записей
            if (profile.inIds && record.id.matches(regex)) {
                addFoundObject(foundObjects, record, FoundType.TYPE_RECORD_ID)
            }
            // поиск по тексту записи (читаем текст html файла)
            if (profile.inText) {
                val text = getRecordParsedTextUseCase.run(
                    GetRecordParsedTextUseCase.Params(
                        record = record,
                        pathToRecordFolder = params.getPathToRecordFolderCallback(record),
                        crypter = params.storageCrypter,
                        showMessage = false,
                    )
                ).foldResult(
                    onLeft = {
                        logger.logFailure(it)
                        null
                    },
                    onRight = { it }
                )
                if (text != null && text.matches(regex)) {
                    addFoundObject(foundObjects, record, FoundType.TYPE_RECORD_TEXT)
                }
            }
            // поиск по меткам (только если указана ветка для поиска)
            // 2 - комментируем isSearchInNode, если используем тип (2)
            if (profile.inTags /*&& isSearchInNode*/) {
                // 1 - добавляем саму метку в результат
//                globalSearchInTags(record.getTags(), query, isOnlyWholeWords);
                // 2 - добавляем запись, содержащую метку
                if (record.tagsString.matches(regex)) {
                    addFoundObject(foundObjects, record, FoundType.TYPE_TAG)
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
    private fun globalSearchInFiles(foundObjects: MutableMap<ITetroidObject, FoundType>, files: List<TetroidFile>, regex: Regex) {
        for (file in files) {
            // поиск по именам файлов
            if (file.name.matches(regex)) {
                addFoundObject(foundObjects, file, FoundType.TYPE_FILE)
            }
            // поиск по id файлов
            if (file.id.matches(regex)) {
                addFoundObject(foundObjects, file, FoundType.TYPE_FILE_ID)
            }
        }
    }

    /**
     * Глобальный поиск по меткам.
     * Не используется в связи с переходом на 2й тип поиска по меткам.
     * @param tags
     * @param regex
     */
    private fun globalSearchInTags(foundObjects: MutableMap<ITetroidObject, FoundType>, tags: List<TetroidTag>, regex: Regex) {
        for (tagEntry in tags) {
            if (tagEntry.name.matches(regex)) {
                // 1 - добавляем саму метку в результат
//                addFoundObject(tagEntry, FoundType.TYPE_TAG);
                // 2 - добавляем записи метки в результат
                for (record in tagEntry.records) {
                    addFoundObject(foundObjects, record, FoundType.TYPE_TAG)
                }
            }
        }
    }

    /**
     * Добавление типа найденного объекта (в зависимости от того, где он найден).
     * @param obj
     * @param foundType
     */
    private fun addFoundObject(foundObjects: MutableMap<ITetroidObject, FoundType>, obj: ITetroidObject, foundType: Int) {
        if (foundObjects.containsKey(obj)) {
            foundObjects[obj]?.addValue(foundType)
        } else {
            foundObjects[obj] = FoundType(foundType)
        }
    }

    /**
     * Поиск по названиям веток (рекурсивно с подветками).
     * Пропускает зашифрованные ветки.
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