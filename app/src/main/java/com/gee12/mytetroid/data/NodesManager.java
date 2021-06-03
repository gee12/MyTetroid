package com.gee12.mytetroid.data;

import android.content.Context;
import android.text.TextUtils;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.logs.TetroidLog;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class NodesManager extends DataManager {

    /**
     * Создание ветки.
     * @param name
     * @param parentNode
     * @return
     */
    public static TetroidNode createNode(Context context, String name, TetroidNode parentNode) {
        if (TextUtils.isEmpty(name)) {
            LogManager.emptyParams(context, "DataManager.createNode()");
            return null;
        }
        TetroidLog.logOperStart(context, TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE);

        // генерируем уникальные идентификаторы
        String id = createUniqueId();

        boolean crypted = (parentNode != null && parentNode.isCrypted());
        int level = (parentNode != null) ? parentNode.getLevel() + 1 : 0;
        TetroidNode node = new TetroidNode(crypted, id,
                Instance.encryptField(crypted, name),
                null, level);
        node.setParentNode(parentNode);
        node.setRecords(new ArrayList<>());
        node.setSubNodes(new ArrayList<>());
        if (crypted) {
            node.setDecryptedName(name);
            node.setIsDecrypted(true);
        }
        // добавляем запись в родительскую ветку (и соответственно, в дерево), если она задана
        List<TetroidNode> list = (parentNode != null) ? parentNode.getSubNodes() : Instance.getRootNodes();
        list.add(node);
        // перезаписываем структуру хранилища в файл
        if (!Instance.saveStorage(context)) {
            TetroidLog.logOperCancel(context, TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE);
            // удаляем запись из дерева
            list.remove(node);
            return null;
        }
        return node;
    }

    /**
     * Изменение свойств ветки.
     * @param node
     * @param name
     * @return
     */
    public static boolean editNodeFields(Context context, TetroidNode node, String name) {
        if (node == null || TextUtils.isEmpty(name)) {
            LogManager.emptyParams(context, "DataManager.editNodeFields()");
            return false;
        }
        TetroidLog.logOperStart(context, TetroidLog.Objs.NODE_FIELDS, TetroidLog.Opers.CHANGE, node);

        String oldName = node.getName(true);
        // обновляем поля
        boolean crypted = node.isCrypted();
        node.setName(Instance.encryptField(crypted, name));
        if (crypted) {
            node.setDecryptedName(name);
        }
        // перезаписываем структуру хранилища в файл
        if (!Instance.saveStorage(context)) {
            TetroidLog.logOperCancel(context, TetroidLog.Objs.NODE_FIELDS, TetroidLog.Opers.CHANGE);
            // возвращаем изменения
            node.setName(oldName);
            if (crypted) {
                node.setDecryptedName(Instance.decryptField(crypted, oldName));
            }
            return false;
        }
        return true;
    }

    /**
     * Установка иконки ветки.
     * @param node
     * @param iconFileName
     * @return
     */
    public static boolean setNodeIcon(Context context, TetroidNode node, String iconFileName) {
        return setNodeIcon(context, node, iconFileName, false);
    }

    /**
     * Сброс иконки ветки.
     * @param node
     * @return
     */
    public static boolean dropNodeIcon(Context context, TetroidNode node) {
        return setNodeIcon(context, node, null, true);
    }

    /**
     * Установка иконки ветки.
     * @param node
     * @param iconFileName
     * @return
     */
    public static boolean setNodeIcon(Context context, TetroidNode node, String iconFileName, boolean isDrop) {
        if (node == null || TextUtils.isEmpty(iconFileName) && !isDrop) {
            LogManager.emptyParams(context, "DataManager.setNodeIcon()");
            return false;
        }
        TetroidLog.logOperStart(context, TetroidLog.Objs.NODE_FIELDS, TetroidLog.Opers.CHANGE, node);

        String oldIconName = node.getIconName(true);
        // обновляем поля
        boolean crypted = node.isCrypted();
        node.setIconName(Instance.encryptField(crypted, iconFileName));
        if (crypted) {
            node.setDecryptedIconName(iconFileName);
        }
        // перезаписываем структуру хранилища в файл
        if (Instance.saveStorage(context)) {
            node.loadIcon(context, IconsManager.getIconsFolderPath());
        } else {
            TetroidLog.logOperCancel(context, TetroidLog.Objs.NODE_FIELDS, TetroidLog.Opers.CHANGE);
            // возвращаем изменения
            node.setIconName(oldIconName);
            if (crypted) {
                node.setDecryptedIconName(Instance.decryptField(crypted, oldIconName));
            }
            return false;
        }
        return true;
    }

    /**
     * Удаление ветки.
     * @param node
     * @return
     */
    public static boolean deleteNode(Context context, TetroidNode node) {
        return deleteNode(context, node, SettingsManager.getTrashPath(context), false);
    }

    /**
     * Вырезание ветки из родительской ветки (добавление в "буфер обмена" и удаление).
     * @param node
     * @return
     */
    public static boolean cutNode(Context context, TetroidNode node) {
        return deleteNode(context, node, SettingsManager.getTrashPath(context), true);
    }

    /**
     * Проверка есть ли у ветки нерасшифрованные подветки.
     * @param node
     * @return
     */
    public static boolean hasNonDecryptedNodes(TetroidNode node) {
        if (node == null)
            return false;
        if (!node.isNonCryptedOrDecrypted())
            return true;
        if (node.getSubNodesCount() > 0) {
            for (TetroidNode subnode : node.getSubNodes()) {
                if (hasNonDecryptedNodes(subnode))
                    return true;
            }
        }
        return false;
    }

    /**
     * Вставка ветки в указанную ветку.
     * @param srcNode
     * @param destParentNode
     * @param isCutted Если true, то запись была вырезана. Иначе - скопирована
     * @return
     */
    public static boolean insertNode(Context context, TetroidNode srcNode, TetroidNode destParentNode, boolean isCutted) {
        if (srcNode == null || destParentNode == null) {
            LogManager.emptyParams(context, "DataManager.insertNode()");
            return false;
        }
        TetroidLog.logOperStart(context, TetroidLog.Objs.NODE, TetroidLog.Opers.INSERT, srcNode);

        TetroidNode newNode = insertNodeRecursively(context, srcNode, destParentNode, isCutted, false);

        // перезаписываем структуру хранилища в файл
        if (!Instance.saveStorage(context)) {
            TetroidLog.logOperCancel(context, TetroidLog.Objs.NODE, TetroidLog.Opers.INSERT);
            // удаляем запись из дерева
            destParentNode.getSubNodes().remove(newNode);
            return false;
        }

        return true;
    }

    /**
     * Вставка ветки в указанную ветку.
     * @param srcNode
     */
    public static TetroidNode insertNodeRecursively(Context context, TetroidNode srcNode, TetroidNode destParentNode,
                                                    boolean isCutted, boolean breakOnFSErrors) {
        if (srcNode == null || destParentNode == null)
            return null;

        // генерируем уникальный идентификатор, если ветка копируется
        String id = (isCutted) ? srcNode.getId() : createUniqueId();
        String name = srcNode.getName();
        String iconName = srcNode.getIconName();

        // создаем копию ветки
        boolean crypted = destParentNode.isCrypted();
        TetroidNode node = new TetroidNode(crypted, id,
                Instance.encryptField(crypted, name),
                Instance.encryptField(crypted, iconName),
                destParentNode.getLevel() + 1);
        node.setParentNode(destParentNode);
        node.setRecords(new ArrayList<>());
        node.setSubNodes(new ArrayList<>());
        if (crypted) {
            node.setDecryptedName(name);
            node.setDecryptedIconName(iconName);
            node.setIsDecrypted(true);
        }
        // загружаем такую же иконку
        Instance.mXml.loadIcon(context, node);
        destParentNode.addSubNode(node);

        // добавляем записи
        if (srcNode.getRecordsCount() > 0) {
            for (TetroidRecord srcRecord : srcNode.getRecords()) {
                if (RecordsManager.cloneRecordToNode(context, srcRecord, node, isCutted, breakOnFSErrors) == null && breakOnFSErrors) {
                    return null;
                }
            }
        }
        // добавляем подветки
        for (TetroidNode srcSubNode : srcNode.getSubNodes()) {
            // если srcNode=destParentNode (копируем ветку как подветку той же ветки),
            // то не нужно копировать самого себя рекурсивно еще раз (бесконечно)
            if (srcSubNode == node)
                continue;
            if (insertNodeRecursively(context, srcSubNode, node, isCutted, breakOnFSErrors) == null && breakOnFSErrors) {
                return null;
            }
        }
        return node;
    }

    /**
     * Удаление/вырезание ветки из родительской ветки.
     * @param node
     * @return
     */
    private static boolean deleteNode(Context context, TetroidNode node, String movePath, boolean isCutting) {
        if (node == null) {
            LogManager.emptyParams(context, "DataManager.deleteNode()");
            return false;
        }
        TetroidLog.logOperStart(context, TetroidLog.Objs.NODE, (isCutting) ? TetroidLog.Opers.CUT : TetroidLog.Opers.DELETE, node);

        // удаляем ветку из дерева
        List<TetroidNode> parentNodes = (node.getParentNode() != null) ? node.getParentNode().getSubNodes() : Instance.getRootNodes();
        if (!parentNodes.remove(node)) {
            LogManager.log(context, context.getString(R.string.log_not_found_node_id) + node.getId(), ILogger.Types.ERROR);
            return false;
        }

        // перезаписываем структуру хранилища в файл
        if (Instance.saveStorage(context)) {
            // удаление всех объектов ветки рекурсивно
            deleteNodeRecursively(context, node, movePath, false);
        } else {
            TetroidLog.logOperCancel(context, TetroidLog.Objs.NODE, (isCutting) ? TetroidLog.Opers.CUT : TetroidLog.Opers.DELETE);
            return false;
        }
        return true;
    }

    /**
     * Удаление объектов ветки.
     * @param node
     */
    private static boolean deleteNodeRecursively(Context context, TetroidNode node, String movePath, boolean breakOnFSErrors) {
        if (node == null)
            return false;
        int recordsCount = node.getRecordsCount();
        if (recordsCount > 0) {
            for (TetroidRecord record : node.getRecords()) {
                // удаляем из избранного
                if (record.isFavorite()) {
                    FavoritesManager.remove(context, record, false);
                }
                Instance.deleteRecordTags(record);
                // проверяем существование каталога
                String dirPath = RecordsManager.getPathToRecordFolderInBase(record);
                if (RecordsManager.checkRecordFolder(context, dirPath, false) <= 0) {
                    if (breakOnFSErrors) {
                        return false;
                    } else {
                        continue;
                    }
                }
                // перемещаем каталог
                if (RecordsManager.moveOrDeleteRecordFolder(context, record, dirPath, movePath) <= 0 && breakOnFSErrors) {
                    return false;
                }
            }
        }
        for (TetroidNode subNode : node.getSubNodes()) {
            if (!deleteNodeRecursively(context, subNode, movePath, breakOnFSErrors) && breakOnFSErrors) {
                return false;
            }
        }
        return true;
    }

    /**
     * Удаление ветки из дерева.
     * (устарело, т.к. сейчас используется поле parentNode)
     * @param nodes
     * @param nodeToDelete
     * @return
     */
    public static boolean deleteNodeInHierarchy(List<TetroidNode> nodes, TetroidNode nodeToDelete) {
        if (nodeToDelete == null)
            return false;
        for (TetroidNode node : nodes) {
            if (nodeToDelete.equals(node)) {
                return nodes.remove(nodeToDelete);
            } else if (node.isExpandable()) {
                if (deleteNodeInHierarchy(node.getSubNodes(), nodeToDelete))
                    return true;
            }
        }
        return false;
    }

    /**
     * Проверка существования шифрованных веток в хранилище.
     * @return
     */
    public static boolean isExistCryptedNodes(boolean recheck) {
        boolean res = Instance.mXml.mIsExistCryptedNodes;
        if (recheck) {
            res = Instance.mXml.mIsExistCryptedNodes = isExistCryptedNodes(Instance.mXml.mRootNodesList);
        }
        return res;
    }

    public static boolean isExistCryptedNodes(List<TetroidNode> nodes) {
        for (TetroidNode node : nodes) {
            if (node.isCrypted())
                return true;
            if (node.getSubNodesCount() > 0) {
                if (isExistCryptedNodes(node.getSubNodes()))
                    return true;
            }
        }
        return false;
    }

    /**
     * Получение иерархии веток. В корне стека - исходная ветка, на верхушке - ее самый дальний предок.
     * @param node
     * @return
     */
    public static Stack<TetroidNode> createNodesHierarchy(TetroidNode node) {
        if (node == null)
            return null;
        Stack<TetroidNode> hierarchy = new Stack<>();
        createNodesHierarchy(hierarchy, node);
        return hierarchy;
    }

    private static void createNodesHierarchy(Stack<TetroidNode> hierarchy, TetroidNode node) {
        hierarchy.push(node);
        if (node.getLevel() > 0) {
            createNodesHierarchy(hierarchy, node.getParentNode());
        }
    }

    public static TetroidNode getNode(String id) {
        return getNodeInHierarchy(Instance.mXml.mRootNodesList, id);
    }

    /**
     * Рекурсивный поиск ветки в списке веток.
     * @param nodes
     * @param id
     * @return
     */
    public static TetroidNode getNodeInHierarchy(List<TetroidNode> nodes, String id) {
        if (nodes == null || id == null)
            return null;
        for (TetroidNode node : nodes) {
            if (id.equals(node.getId()))
                return node;
            else if (node.isExpandable()) {
                TetroidNode found = getNodeInHierarchy(node.getSubNodes(), id);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /**
     * Рекурсивный подсчет дочерних веток и записей в ветке.
     * @param node
     * @return
     */
    public static int[] getNodesRecordsCount(TetroidNode node) {
        if (node == null)
            return null;
        int[] res = new int[2];
        int subNodesCount = node.getSubNodesCount();
        res[0] = subNodesCount;
        res[1] = node.getRecordsCount();
        if (subNodesCount > 0) {
            for (TetroidNode subNode : node.getSubNodes()) {
                int[] subRes = getNodesRecordsCount(subNode);
                if (subRes != null) {
                    res[0] += subRes[0];
                    res[1] += subRes[1];
                }
            }
        }
        return res;
    }

    public static TetroidNode getQuicklyNode() {
        return (Instance != null) ? Instance.mQuicklyNode : null;
    }

    public static void setQuicklyNode(TetroidNode node) {
        if (Instance != null) {
            Instance.mQuicklyNode = node;
        }
    }

    /**
     * Актуализация ветки для быстрой вставки в дереве.
     */
    public static void updateQuicklyNode(Context context) {
        String nodeId = SettingsManager.getQuicklyNodeId(context);
        if (nodeId != null && Instance != null && Instance.mXml.mIsStorageLoaded
                && !Instance.mXml.mIsFavoritesMode) {
            TetroidNode node = getNode(nodeId);
            // обновление значений или обнуление (если не найдено)
            SettingsManager.setQuicklyNode(context, node);
            Instance.mQuicklyNode = node;
        }
    }

    /**
     * Получение первой незашифрованной ветки.
     * @return
     */
    public static TetroidNode getDefaultNode() {
        if (Instance != null && !Instance.getRootNodes().isEmpty()) {
            for (TetroidNode node : Instance.getRootNodes()) {
                if (node.isNonCryptedOrDecrypted()) {
                    return node;
                }
            }
        }
        return null;
    }
}
