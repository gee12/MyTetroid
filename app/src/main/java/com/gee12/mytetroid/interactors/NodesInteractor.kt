package com.gee12.mytetroid.interactors

import android.content.Context
import android.text.TextUtils
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.data.xml.IStorageLoadHelper
import com.gee12.mytetroid.data.xml.TetroidXml
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.utils.FileUtils
import java.lang.Exception
import java.util.*

/**
 * Создается для конкретного хранилища.
 */
class NodesInteractor(
    private val logger: ITetroidLogger,
    private val storageInteractor: StorageInteractor,
    private val cryptInteractor: EncryptionInteractor,
    private val dataInteractor: DataInteractor,
    private val recordsInteractor: RecordsInteractor,
    private val favoritesInteractor: FavoritesInteractor,
    private val storageHelper: IStorageLoadHelper,
    private val xmlLoader: TetroidXml
) {

    /**
     * Создание ветки.
     * @param name
     * @param parentNode
     * @return
     */
    suspend fun createNode(context: Context, name: String, parentNode: TetroidNode?): TetroidNode? {
        if (TextUtils.isEmpty(name)) {
            logger.logEmptyParams("DataManager.createNode()")
            return null
        }
        logger.logOperStart(LogObj.NODE, LogOper.CREATE)

        // генерируем уникальные идентификаторы
        val id: String = dataInteractor.createUniqueId()
        val crypted = (parentNode != null && parentNode.isCrypted)
        val level = if (parentNode != null) parentNode.level + 1 else 0
        val node = TetroidNode(
            crypted, id,
            cryptInteractor.encryptField(crypted, name),
            null, level
        )
        node.parentNode = parentNode
        node.records = ArrayList()
        node.subNodes = ArrayList()
        if (crypted) {
            node.setDecryptedName(name)
            node.setIsDecrypted(true)
        }
        // добавляем запись в родительскую ветку (и соответственно, в дерево), если она задана
        val list = (if (parentNode != null) parentNode.subNodes else storageInteractor.getRootNodes()) as MutableList<TetroidNode>
        list.add(node)
        // перезаписываем структуру хранилища в файл
        if (!storageInteractor.saveStorage(context)) {
            logger.logOperCancel(LogObj.NODE, LogOper.CREATE)
            // удаляем запись из дерева
            list.remove(node)
            return null
        }
        return node
    }

    /**
     * Изменение свойств ветки.
     * @param node
     * @param name
     * @return
     */
    suspend fun editNodeFields(context: Context, node: TetroidNode, name: String): Boolean {
        if (TextUtils.isEmpty(name)) {
            logger.logEmptyParams("DataManager.editNodeFields()")
            return false
        }
        logger.logOperStart(LogObj.NODE_FIELDS, LogOper.CHANGE, node)
        val oldName = node.getName(true)
        // обновляем поля
        val crypted = node.isCrypted
        node.name = cryptInteractor.encryptField(crypted, name)
        if (crypted) {
            node.setDecryptedName(name)
        }
        // перезаписываем структуру хранилища в файл
        if (!storageInteractor.saveStorage(context)) {
            logger.logOperCancel(LogObj.NODE_FIELDS, LogOper.CHANGE)
            // возвращаем изменения
            node.name = oldName
            if (crypted) {
                node.setDecryptedName(cryptInteractor.decryptField(crypted, oldName))
            }
            return false
        }
        return true
    }

    /**
     * Установка иконки ветки.
     * @param node
     * @param iconFileName
     * @return
     */
    suspend fun setNodeIcon(context: Context, node: TetroidNode, iconFileName: String): Boolean {
        return setNodeIcon(context, node, iconFileName, false)
    }

    /**
     * Сброс иконки ветки.
     * @param node
     * @return
     */
    suspend fun dropNodeIcon(context: Context, node: TetroidNode): Boolean {
        return setNodeIcon(context, node, null, true)
    }

    /**
     * Установка иконки ветки.
     * @param node
     * @param iconFileName
     * @return
     */
    suspend fun setNodeIcon(context: Context, node: TetroidNode?, iconFileName: String?, isDrop: Boolean): Boolean {
        if (node == null || TextUtils.isEmpty(iconFileName) && !isDrop) {
            logger.logEmptyParams("DataManager.setNodeIcon()")
            return false
        }
        logger.logOperStart(LogObj.NODE_FIELDS, LogOper.CHANGE, node)
        val oldIconName = node.getIconName(true)
        // обновляем поля
        val crypted = node.isCrypted
        node.iconName = cryptInteractor.encryptField(crypted, iconFileName)
        if (crypted) {
            node.setDecryptedIconName(iconFileName)
        }
        // перезаписываем структуру хранилища в файл
        if (storageInteractor.saveStorage(context)) {
            loadNodeIcon(node)
        } else {
            logger.logOperCancel(LogObj.NODE_FIELDS, LogOper.CHANGE)
            // возвращаем изменения
            node.iconName = oldIconName
            if (crypted) {
                node.setDecryptedIconName(cryptInteractor.decryptField(crypted, oldIconName))
            }
            return false
        }
        return true
    }

    /**
     * Загрузка иконки ветки.
     */
    fun loadNodeIcon(node: TetroidNode) {
        if (TextUtils.isEmpty(node.iconName)) {
            node.icon = null
            return
        } else {
            try {
                node.icon = FileUtils.loadSVGFromFile(storageInteractor.getPathToFileInIconsFolder(node.iconName))
            } catch (ex: Exception) {
                logger.logError(ex)
            }
        }
    }

    /**
     * Вставка ветки в указанную ветку.
     * @param srcNode
     * @param destParentNode
     * @param isCutted Если true, то запись была вырезана. Иначе - скопирована
     * @return
     */
    suspend fun insertNode(
        context: Context,
        srcNode: TetroidNode,
        destParentNode: TetroidNode,
        isCutted: Boolean
    ): Boolean {
        logger.logOperStart(LogObj.NODE, LogOper.INSERT, srcNode)
        val newNode = insertNodeRecursively(context,
            srcNode = srcNode,
            destParentNode = destParentNode,
            isCutted = isCutted,
            breakOnFSErrors = false
        )

        // перезаписываем структуру хранилища в файл
        if (!storageInteractor.saveStorage(context)) {
            logger.logOperCancel(LogObj.NODE, LogOper.INSERT)
            // удаляем запись из дерева
            destParentNode.subNodes.remove(newNode)
            return false
        }
        return true
    }

    /**
     * Вставка ветки в указанную ветку.
     * @param srcNode
     */
    suspend fun insertNodeRecursively(
        context: Context,
        srcNode: TetroidNode,
        destParentNode: TetroidNode,
        isCutted: Boolean,
        breakOnFSErrors: Boolean
    ): TetroidNode? {
        // генерируем уникальный идентификатор, если ветка копируется
        val id = if (isCutted) srcNode.id else dataInteractor.createUniqueId()
        val name = srcNode.name
        val iconName = srcNode.iconName

        // создаем копию ветки
        val crypted = destParentNode.isCrypted
        val node = TetroidNode(
            crypted,
            id,
            cryptInteractor.encryptField(crypted, name),
            cryptInteractor.encryptField(crypted, iconName),
            destParentNode.level + 1
        )
        node.parentNode = destParentNode
        node.records = ArrayList()
        node.subNodes = ArrayList()
        if (crypted) {
            node.setDecryptedName(name)
            node.setDecryptedIconName(iconName)
            node.setIsDecrypted(true)
        }
        // загружаем такую же иконку
        storageHelper.loadIcon(context, node)

        // добавляем записи
        if (srcNode.recordsCount > 0) {
            for (srcRecord in srcNode.records) {
                if (recordsInteractor.cloneRecordToNode(context, srcRecord, node, isCutted, breakOnFSErrors) == null && breakOnFSErrors) {
                    return null
                }
            }
        }
        // добавляем подветки
        for (srcSubNode in srcNode.subNodes) {
            if (insertNodeRecursively(
                    context,
                    srcSubNode,
                    node,
                    isCutted,
                    breakOnFSErrors
                ) == null && breakOnFSErrors) {
                return null
            }
        }

        // добавляем новую ветку в иерархию уже ПОСЛЕ копирования дерева существующих веток,
        //  чтобы не получилась вечная рекурсия в случае, если родительскую ветку копируем в дочернюю
        destParentNode.addSubNode(node)

        return node
    }

    /**
     * Удаление ветки.
     * @param node
     * @return
     */
    suspend fun deleteNode(context: Context, node: TetroidNode): Boolean {
        return deleteNode(context, node, CommonSettings.getTrashPathDef(context), false)
    }

    /**
     * Вырезание ветки из родительской ветки (добавление в "буфер обмена" и удаление).
     * @param node
     * @return
     */
    suspend fun cutNode(context: Context, node: TetroidNode): Boolean {
        return deleteNode(context, node, CommonSettings.getTrashPathDef(context), true)
    }

    /**
     * Удаление/вырезание ветки из родительской ветки.
     * @param node
     * @return
     */
    private suspend fun deleteNode(context: Context, node: TetroidNode, movePath: String, isCutting: Boolean): Boolean {
        logger.logOperStart(LogObj.NODE, if (isCutting) LogOper.CUT else LogOper.DELETE, node)

        // удаляем ветку из дерева
        val parentNodes = (if (node.parentNode != null) node.parentNode.subNodes else storageInteractor.getRootNodes()) as MutableList<TetroidNode>
        if (!parentNodes.remove(node)) {
            logger.logError(context.getString(R.string.log_not_found_node_id) + node.id)
            return false
        }

        // перезаписываем структуру хранилища в файл
        if (storageInteractor.saveStorage(context)) {
            // удаление всех объектов ветки рекурсивно
            deleteNodeRecursively(context, node, movePath, false)
        } else {
            logger.logOperCancel(LogObj.NODE, if (isCutting) LogOper.CUT else LogOper.DELETE)
            return false
        }
        return true
    }

    /**
     * Удаление объектов ветки.
     * @param node
     */
    private suspend fun deleteNodeRecursively(
        context: Context,
        node: TetroidNode,
        movePath: String,
        breakOnFSErrors: Boolean
    ): Boolean {
        val recordsCount = node.recordsCount
        if (recordsCount > 0) {
            for (record in node.records) {
                // удаляем из избранного
                if (record.isFavorite) {
                    favoritesInteractor.remove(record, false)
                }
                storageHelper.deleteRecordTags(record)
                // проверяем существование каталога
                val dirPath = recordsInteractor.getPathToRecordFolder(record)
                if (recordsInteractor.checkRecordFolder(context, dirPath, false) <= 0) {
                    return if (breakOnFSErrors) {
                        false
                    } else {
                        continue
                    }
                }
                // перемещаем каталог
                if (recordsInteractor.moveOrDeleteRecordFolder(context, record, dirPath, movePath) <= 0 && breakOnFSErrors) {
                    return false
                }
            }
        }
        for (subNode in node.subNodes) {
            if (!deleteNodeRecursively(context, subNode, movePath, breakOnFSErrors) && breakOnFSErrors) {
                return false
            }
        }
        return true
    }

    /**
     * Получение иерархии веток. В корне стека - исходная ветка, на верхушке - ее самый дальний предок.
     * @param node
     * @return
     */
    fun createNodesHierarchy(node: TetroidNode?): Stack<TetroidNode>? {
        if (node == null) return null
        val hierarchy = Stack<TetroidNode>()
        createNodesHierarchy(hierarchy, node)
        return hierarchy
    }

    private fun createNodesHierarchy(hierarchy: Stack<TetroidNode>, node: TetroidNode) {
        hierarchy.push(node)
        if (node.level > 0) {
            createNodesHierarchy(hierarchy, node.parentNode)
        }
    }

    fun getNode(id: String): TetroidNode? {
        return getNodeInHierarchy(xmlLoader.mRootNodesList, id)
    }

    /**
     * Рекурсивный подсчет дочерних веток и записей в ветке.
     * @param node
     * @return
     */
    fun getNodesRecordsCount(node: TetroidNode): IntArray {
        val res = IntArray(2)
        val subNodesCount = node.subNodesCount
        res[0] = subNodesCount
        res[1] = node.recordsCount
        if (subNodesCount > 0) {
            for (subNode in node.subNodes) {
                val subRes = getNodesRecordsCount(subNode)
                res[0] += subRes[0]
                res[1] += subRes[1]
            }
        }
        return res
    }

    /**
     * Рекурсивный поиск ветки в списке веток.
     * @param nodes
     * @param id
     * @return
     */
    fun getNodeInHierarchy(nodes: List<TetroidNode>, id: String): TetroidNode? {
        for (node in nodes) {
            if (id == node.id) {
                return node
            } else if (node.isExpandable) {
                val found = getNodeInHierarchy(node.subNodes, id)
                if (found != null) {
                    return found
                }
            }
        }
        return null
    }

    /**
     * Проверка есть ли у ветки нерасшифрованные подветки.
     * @param node
     * @return
     */
    fun hasNonDecryptedNodes(node: TetroidNode): Boolean {
        if (!node.isNonCryptedOrDecrypted) return true
        if (node.subNodesCount > 0) {
            for (subnode in node.subNodes) {
                if (hasNonDecryptedNodes(subnode)) return true
            }
        }
        return false
    }

    /**
     * Проверка существования шифрованных веток в хранилище.
     * @return
     */
    fun isExistCryptedNodes(recheck: Boolean): Boolean {
        var res: Boolean = xmlLoader.mIsExistCryptedNodes
        if (recheck) {
            xmlLoader.mIsExistCryptedNodes = isExistCryptedNodes(xmlLoader.mRootNodesList)
            res = xmlLoader.mIsExistCryptedNodes
        }
        return res
    }

    fun isExistCryptedNodes(nodes: List<TetroidNode>): Boolean {
        for (node in nodes) {
            if (node.isCrypted) return true
            if (node.subNodesCount > 0) {
                if (isExistCryptedNodes(node.subNodes)) return true
            }
        }
        return false
    }

    /**
     * Проверка содержится ли ветка node в ветке nodeAsParent.
     * @param node
     * @param nodeAsParent
     * @return
     */
    fun isNodeInNode(node: TetroidNode?, nodeAsParent: TetroidNode?): Boolean {
        if (node == null || nodeAsParent == null) return false
        return if (node.parentNode != null) {
            if (node.parentNode == nodeAsParent) true else isNodeInNode(node.parentNode, nodeAsParent)
        } else false
    }

}