package com.gee12.mytetroid.interactors

import android.text.TextUtils
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.onFailure
import com.gee12.mytetroid.helpers.IRecordPathHelper
import com.gee12.mytetroid.helpers.IResourcesProvider
import com.gee12.mytetroid.helpers.IStoragePathHelper
import com.gee12.mytetroid.helpers.IStorageProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.usecase.LoadNodeIconUseCase
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.usecase.tag.DeleteRecordTagsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Создается для конкретного хранилища.
 */
class NodesInteractor(
    private val logger: ITetroidLogger,
    private val resourcesProvider: IResourcesProvider,
    private val cryptInteractor: EncryptionInteractor,
    private val dataInteractor: DataInteractor,
    private val recordsInteractor: RecordsInteractor,
    private val favoritesInteractor: FavoritesInteractor,
    private val storagePathHelper: IStoragePathHelper,
    private val recordPathHelper: IRecordPathHelper,
    private val storageProvider: IStorageProvider,
    private val deleteRecordTagsUseCase: DeleteRecordTagsUseCase,
    private val loadNodeIconUseCase: LoadNodeIconUseCase,
    private val saveStorageUseCase: SaveStorageUseCase,
) {

    /**
     * Изменение свойств ветки.
     * @param node
     * @param name
     * @return
     */
    suspend fun editNodeFields(node: TetroidNode, name: String): Boolean {
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
        if (!saveStorage()) {
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
    suspend fun setNodeIcon(node: TetroidNode, iconFileName: String): Boolean {
        return setNodeIcon(node, iconFileName, false)
    }

    /**
     * Сброс иконки ветки.
     * @param node
     * @return
     */
    suspend fun dropNodeIcon(node: TetroidNode): Boolean {
        return setNodeIcon(node, null, true)
    }

    /**
     * Установка иконки ветки.
     * @param node
     * @param iconFileName
     * @return
     */
    suspend fun setNodeIcon(node: TetroidNode?, iconFileName: String?, isDrop: Boolean): Boolean {
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
        if (saveStorage()) {
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
    private fun loadNodeIcon(node: TetroidNode) {
        loadNodeIconUseCase.execute(
            LoadNodeIconUseCase.Params(node)
        ).onFailure {
            logger.logFailure(it, show = false)
        }
    }

    /**
     * Удаление ветки.
     * @param node
     * @return
     */
    suspend fun deleteNode(node: TetroidNode): Boolean {
        return deleteNode(node, storagePathHelper.getPathToTrash(), false)
    }

    /**
     * Вырезание ветки из родительской ветки (добавление в "буфер обмена" и удаление).
     * @param node
     * @return
     */
    suspend fun cutNode(node: TetroidNode): Boolean {
        return deleteNode(node, storagePathHelper.getPathToTrash(), true)
    }

    /**
     * Удаление/вырезание ветки из родительской ветки.
     * @param node
     * @return
     */
    private suspend fun deleteNode(node: TetroidNode, movePath: String, isCutting: Boolean): Boolean {
        logger.logOperStart(LogObj.NODE, if (isCutting) LogOper.CUT else LogOper.DELETE, node)

        // удаляем ветку из дерева
        val parentNodes = (if (node.parentNode != null) node.parentNode.subNodes else storageProvider.getRootNodes()) as MutableList<TetroidNode>
        if (!parentNodes.remove(node)) {
            logger.logError(resourcesProvider.getString(R.string.log_not_found_node_id) + node.id)
            return false
        }

        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
            // удаление всех объектов ветки рекурсивно
            deleteNodeRecursively(node, movePath, false)
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
                deleteRecordTagsUseCase.run(
                    DeleteRecordTagsUseCase.Params(record)
                ).onFailure {
                    logger.logFailure(it, show = false)
                }
                // проверяем существование каталога
                val dirPath = recordPathHelper.getPathToRecordFolder(record)
                if (recordsInteractor.checkRecordFolder(dirPath, false) <= 0) {
                    return if (breakOnFSErrors) {
                        false
                    } else {
                        continue
                    }
                }
                // перемещаем каталог
                if (recordsInteractor.moveOrDeleteRecordFolder(record, dirPath, movePath) <= 0 && breakOnFSErrors) {
                    return false
                }
            }
        }
        for (subNode in node.subNodes) {
            if (!deleteNodeRecursively(subNode, movePath, breakOnFSErrors) && breakOnFSErrors) {
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
        return getNodeInHierarchy(storageProvider.getRootNodes(), id)
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
        var res: Boolean = storageProvider.isExistCryptedNodes()
        if (recheck) {
            storageProvider.setIsExistCryptedNodes(isExistCryptedNodes(storageProvider.getRootNodes()))
            res = storageProvider.isExistCryptedNodes()
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

    private suspend fun saveStorage(): Boolean {
        return withContext(Dispatchers.IO) {
            saveStorageUseCase.run()
        }.foldResult(
            onLeft = {
                logger.logFailure(it)
                false
            },
            onRight = { true }
        )
    }

}