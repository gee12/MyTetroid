package com.gee12.mytetroid.crypt;

import com.gee12.mytetroid.data.INodeIconLoader;
import com.gee12.mytetroid.data.ITagsParseHandler;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;

import java.util.List;

public class CryptManager extends Crypter {

    private static ITagsParseHandler tagsParser;


    public static void initFromPass(String pass, ITagsParseHandler tagsParser) {
        int[] key = passToKey(pass);
        // записываем в память
        setCryptKey(key);
        CryptManager.tagsParser = tagsParser;
    }

    public static void initFromMiddleHash(String passHash, ITagsParseHandler tagsParser) {
        int[] key = middlePassHashToKey(passHash);
        // записываем в память
        setCryptKey(key);
        CryptManager.tagsParser = tagsParser;
    }

    /**
     * Расшифровка веток (временная).
     * @param nodes
     * @param isDecryptSubNodes
     * @param iconLoader
     * @return
     */
    public static boolean decryptAll(List<TetroidNode> nodes, boolean isDecryptSubNodes, INodeIconLoader iconLoader) {
        return decryptNodes(nodes, isDecryptSubNodes, iconLoader, false);
    }

    /**
     * Расшифровка веток.
     * @param nodes
     * @param isDecryptSubNodes
     * @param iconLoader
     * @param nocrypt Если true - объект расшифровывается навсегда, false - временная расшифровка.
     * @return
     */
    public static boolean decryptNodes(List<TetroidNode> nodes, boolean isDecryptSubNodes, INodeIconLoader iconLoader,
                                       boolean nocrypt) {
        boolean res = true;
        for (TetroidNode node : nodes) {
            if (node.isCrypted()) {
                res = res & decryptNode(node, isDecryptSubNodes, iconLoader, nocrypt);
            }
        }
        return res;
    }

    /**
     * Расшифровка ветки.
     * @param node
     * @param isDecryptSubNodes
     * @param iconLoader
     * @param nocrypt Если true - объект расшифровывается навсегда, false - временная расшифровка.
     * @return
     */
    public static boolean decryptNode(TetroidNode node, boolean isDecryptSubNodes, INodeIconLoader iconLoader,
                                      boolean nocrypt) {
        if (node == null)
            return false;
        boolean res;
        // расшифровываем поля
        res = decryptNodeFields(node, nocrypt);
        // загружаем иконку
        if (iconLoader != null) {
            iconLoader.loadIcon(node);
        }
        // TODO: расшифровывать список записей сразу или при выделении ?
        //  (пока сразу)
        if (node.getRecordsCount() > 0) {
            res = res & decryptRecordsFields(node.getRecords(), nocrypt);
        }
        // расшифровываем подветки
        if (isDecryptSubNodes && node.getSubNodesCount() > 0) {
            res = res & decryptNodes(node.getSubNodes(), isDecryptSubNodes, iconLoader, nocrypt);
        }
        return res;
    }

    /**
     * Расшифровка полей ветки.
     * @param node
     * @param nocrypt Если true - объект расшифровывается навсегда, false - временная расшифровка.
     * @return
     */
    public static boolean decryptNodeFields(TetroidNode node, boolean nocrypt) {
        boolean res;
        // name
        String temp = CryptManager.decryptBase64(mCryptKey, node.getName());
        res = (temp != null);
        if (res) {
            if (nocrypt)
                node.setName(temp);
            else
                node.setDecryptedName(temp);
        }
        // icon
        temp = CryptManager.decryptBase64(mCryptKey, node.getIconName());
        res = res & (temp != null);
        if (temp != null) {
            if (nocrypt)
                node.setIconName(temp);
            else
                node.setDecryptedIconName(temp);
        }
        // decryption result
        if (nocrypt)
            node.setIsCrypted(!res);
        else
            node.setDecrypted(res);
        return res;
    }

    /**
     * Расшифровка полей списка записей.
     * @param records
     * @param nocrypt Если true - объект расшифровывается навсегда, false - временная расшифровка.
     * @return
     */
    public static boolean decryptRecordsFields(List<TetroidRecord> records, boolean nocrypt) {
        boolean res = true;
        for (TetroidRecord record : records) {
            if (record.isCrypted()) {
                res = res & decryptRecordFields(record, nocrypt);
                if (record.getAttachedFilesCount() > 0)
                    for (TetroidFile file : record.getAttachedFiles()) {
                        res = res & decryptAttach(file, nocrypt);
                    }
            }
        }
        return res;
    }

    /**
     * Расшифровка полей записи.
     * @param record
     * @param nocrypt Если true - объект расшифровывается навсегда, false - временная расшифровка.
     * @return
     */
    public static boolean decryptRecordFields(TetroidRecord record, boolean nocrypt) {
        boolean res;
        String temp = CryptManager.decryptBase64(mCryptKey, record.getName());
        res = (temp != null);
        if (res) {
            if (nocrypt)
                record.setName(temp);
            else
                record.setDecryptedName(temp);
        }
        temp = CryptManager.decryptBase64(mCryptKey, record.getTagsString());
        res = res & (temp != null);
        if (temp != null) {
            if (nocrypt)
                record.setTagsString(temp);
            else
                record.setDecryptedTagsString(temp);
            tagsParser.parseRecordTags(record, temp);
        }
        temp = CryptManager.decryptBase64(mCryptKey, record.getAuthor());
        res = res & (temp != null);
        if (temp != null) {
            if (nocrypt)
                record.setAuthor(temp);
            else
                record.setDecryptedAuthor(temp);
        }
        temp = CryptManager.decryptBase64(mCryptKey, record.getUrl());
        res = res & (temp != null);
        if (temp != null) {
            if (nocrypt)
                record.setUrl(temp);
            else
                record.setDecryptedUrl(temp);
        }
        if (nocrypt)
            record.setIsCrypted(!res);
        else
            record.setDecrypted(res);
        return res;
    }

    /**
     * Расшифровка полей прикрепленного файла.
     * @param file
     * @param nocrypt Если true - объект расшифровывается навсегда, false - временная расшифровка.
     * @return
     */
    public static boolean decryptAttach(TetroidFile file, boolean nocrypt) {
        String temp = CryptManager.decryptBase64(mCryptKey, file.getName());
        boolean res = (temp != null);
        if (res) {
            if (nocrypt)
                file.setName(temp);
            else
                file.setDecryptedName(temp);
        }
        if (nocrypt)
            file.setIsCrypted(!res);
        else
            file.setDecrypted(res);
        return res;
    }

}
