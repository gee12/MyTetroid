package com.gee12.mytetroid.data;

import android.text.TextUtils;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.TetroidLog;
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
    public static TetroidNode createNode(String name, TetroidNode parentNode) {
        if (TextUtils.isEmpty(name)) {
            LogManager.emptyParams("DataManager.createNode()");
            return null;
        }
        TetroidLog.addOperStartLog(TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE);

        // генерируем уникальные идентификаторы
        String id = createUniqueId();

        boolean crypted = (parentNode != null && parentNode.isCrypted());
        int level = (parentNode != null) ? parentNode.getLevel() + 1 : 0;
        TetroidNode node = new TetroidNode(crypted, id,
                encryptField(crypted, name),
                null, level);
        node.setParentNode(parentNode);
        node.setRecords(new ArrayList<>());
        node.setSubNodes(new ArrayList<>());
        if (crypted) {
            node.setDecryptedName(name);
            node.setDecrypted(true);
        }
        // добавляем запись в родительскую ветку (и соответственно, в коллекцию), если она задана
        List<TetroidNode> list = (parentNode != null) ? parentNode.getSubNodes() : getRootNodes();
        list.add(node);
        // перезаписываем структуру хранилища в файл
        if (!saveStorage()) {
            TetroidLog.addOperCancelLog(TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE);
            // удаляем запись из коллекции
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
    public static boolean editNodeFields(TetroidNode node, String name) {
        if (node == null || TextUtils.isEmpty(name)) {
            LogManager.emptyParams("DataManager.editNodeFields()");
            return false;
        }
//        LogManager.log(context.getString(R.string.log_start_node_fields_editing), LogManager.Types.DEBUG);
        TetroidLog.addOperStartLog(TetroidLog.Objs.NODE_FIELDS, TetroidLog.Opers.CHANGE);

        String oldName = node.getName(true);
        // обновляем поля
        boolean crypted = node.isCrypted();
        node.setName(encryptField(crypted, name));
        if (crypted) {
            node.setDecryptedName(name);
        }
        // перезаписываем структуру хранилища в файл
        if (!saveStorage()) {
//            LogManager.log(context.getString(R.string.log_cancel_node_changing), LogManager.Types.ERROR);
            TetroidLog.addOperCancelLog(TetroidLog.Objs.NODE_FIELDS, TetroidLog.Opers.CHANGE);
            // возвращаем изменения
            node.setName(oldName);
//            node.setDecryptedName(decryptField(node, oldName));
            if (crypted) {
                node.setDecryptedName(decryptField(crypted, oldName));
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
    public static boolean deleteNode(TetroidNode node) {
        return deleteNode(node, SettingsManager.getTrashPath(), false);
    }

    /**
     * Вырезание ветки из родительской ветки (добавление в "буфер обмена" и удаление).
     * @param node
     * @return
     */
    public static boolean cutNode(TetroidNode node) {
        return deleteNode(node, SettingsManager.getTrashPath(), true);
    }

    /**
     * Вставка ветки в указанную ветку.
     * @param srcNode
     * @param destParentNode
     * @param isCutted Если true, то запись была вырезана. Иначе - скопирована
     * @return
     */
    public static boolean insertNode(TetroidNode srcNode, TetroidNode destParentNode, boolean isCutted) {
        if (srcNode == null || destParentNode == null) {
            LogManager.emptyParams("DataManager.insertNode()");
            return false;
        }
//        TetroidLog.addOperStartLog(TetroidLog.Objs.NODE, TetroidLog.Opers.INSERT);

        TetroidNode newNode = insertNodeRecursively(srcNode, destParentNode, isCutted, false);

        // перезаписываем структуру хранилища в файл
        if (!saveStorage()) {
            TetroidLog.addOperCancelLog(TetroidLog.Objs.NODE, TetroidLog.Opers.INSERT);
            // удаляем запись из коллекции
            destParentNode.getSubNodes().remove(newNode);
            return false;
        }

        return true;
    }

    /**
     * Вставка ветки в указанную ветку.
     * @param srcNode
     */
    public static TetroidNode insertNodeRecursively(TetroidNode srcNode, TetroidNode destParentNode, boolean isCutted, boolean breakOnFSErrors) {
        if (srcNode == null || destParentNode == null)
            return null;
        TetroidLog.addOperStartLog(TetroidLog.Objs.NODE, TetroidLog.Opers.INSERT);

        // генерируем уникальный идентификатор, если ветка копируется
        String id = (isCutted) ? srcNode.getId() : createUniqueId();
        String name = srcNode.getName();
        String iconName = srcNode.getIconName();

        // создаем копию ветки
        boolean crypted = destParentNode.isCrypted();
        TetroidNode node = new TetroidNode(crypted, id,
                encryptField(crypted, name),
                encryptField(crypted, iconName),
                destParentNode.getLevel() + 1);
        node.setParentNode(destParentNode);
        node.setRecords(new ArrayList<>());
        node.setSubNodes(new ArrayList<>());
        if (crypted) {
            node.setDecryptedName(name);
            node.setDecryptedIconName(iconName);
            node.setDecrypted(true);
        }
        // загружаем такую же иконку
        instance.loadIcon(node);
        destParentNode.addSubNode(node);

        // добавляем записи
        if (srcNode.getRecordsCount() > 0) {
            for (TetroidRecord srcRecord : srcNode.getRecords()) {
                if (RecordsManager.cloneRecordToNode(srcRecord, node, isCutted, breakOnFSErrors) == null && breakOnFSErrors) {
                    return null;
                }
            }
        }
        // добавляем подветки
        for (TetroidNode srcSubNode : srcNode.getSubNodes()) {
            if (insertNodeRecursively(srcSubNode, node, isCutted, breakOnFSErrors) == null && breakOnFSErrors) {
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
    private static boolean deleteNode(TetroidNode node, String movePath, boolean isCutting) {
        if (node == null) {
            LogManager.emptyParams("DataManager.deleteNode()");
            return false;
        }
//        TetroidLog.addOperStartLog(TetroidLog.Objs.NODE, TetroidLog.Opers.DELETE);
        TetroidLog.addOperStartLog(TetroidLog.Objs.NODE, (isCutting) ? TetroidLog.Opers.CUT : TetroidLog.Opers.DELETE);

        // удаляем ветку из коллекции
        List<TetroidNode> parentNodes = (node.getParentNode() != null) ? node.getParentNode().getSubNodes() : getRootNodes();
        if (!parentNodes.remove(node)) {
            LogManager.log(context.getString(R.string.log_not_found_node_id) + node.getId(), LogManager.Types.ERROR);
            return false;
        }

        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
            /*// TODO: необходим обход всего дерева веток для пересчета следующих счетчиков:
            instance.mMaxSubnodesCount = -1;
            instance.mMaxDepthLevel = -1;
            instance.mUniqueTagsCount = -1;*/
            // удаление всех объектов ветки рекурсивно
            deleteNodeRecursively(node, movePath, false);
        } else {
//            TetroidLog.addOperCancelLog(TetroidLog.Objs.NODE, TetroidLog.Opers.DELETE);
            TetroidLog.addOperCancelLog(TetroidLog.Objs.NODE, (isCutting) ? TetroidLog.Opers.CUT : TetroidLog.Opers.DELETE);
            return false;
        }
        return true;
    }

    /**
     * Удаление объектов ветки.
     * @param node
     */
    private static boolean deleteNodeRecursively(TetroidNode node, String movePath, boolean breakOnFSErrors) {
        if (node == null)
            return false;
       /* mNodesCount--;
        if (node.isCrypted()) {
            mCryptedNodesCount--;
        }
        if (!TextUtils.isEmpty(node.getIconName())) {
            mIconsCount--;
        }*/
        int recordsCount = node.getRecordsCount();
        if (recordsCount > 0) {
           /* instance.mRecordsCount -= recordsCount;
            if (node.isCrypted()) {
                instance.mCryptedRecordsCount -= recordsCount;
            }*/
            for (TetroidRecord record : node.getRecords()) {
                /*if (!StringUtil.isBlank(record.getAuthor())) {
                    mAuthorsCount--;
                }
                if (record.getAttachedFilesCount() > 0) {
                    mFilesCount -= record.getAttachedFilesCount();
                }*/
                instance.deleteRecordTags(record);
//                deleteRecordFolder(record);
                // проверяем существование каталога
                String dirPath = RecordsManager.getPathToRecordFolder(record);
                if (RecordsManager.checkRecordFolder(dirPath, false) <= 0) {
                    if (breakOnFSErrors) {
                        return false;
                    } else {
                        continue;
                    }
                }
                // перемещаем каталог
                if (RecordsManager.moveOrDeleteRecordFolder(record, dirPath, movePath) <= 0 && breakOnFSErrors) {
                    return false;
                }
            }
        }
        for (TetroidNode subNode : node.getSubNodes()) {
            if (!deleteNodeRecursively(subNode, movePath, breakOnFSErrors) && breakOnFSErrors) {
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
        boolean res = instance.mIsExistCryptedNodes;
        if (recheck) {
            res = instance.mIsExistCryptedNodes = isExistCryptedNodes(instance.mRootNodesList);
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
        createNodesHierarhy(hierarchy, node);
        return hierarchy;
    }

    private static void createNodesHierarhy(Stack<TetroidNode> hierarchy, TetroidNode node) {
        hierarchy.push(node);
        if (node.getLevel() > 0) {
            createNodesHierarhy(hierarchy, node.getParentNode());
        }
    }

    public static TetroidNode getNode(String id) {
        return getNodeInHierarchy(instance.mRootNodesList, id);
    }

    public static TetroidNode getNodeInHierarchy(List<TetroidNode> nodes, String id) {
        if (id == null)
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
}
