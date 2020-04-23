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

    public static boolean decryptAll(List<TetroidNode> nodes, boolean isDecryptSubNodes, INodeIconLoader iconLoader) {
        return decryptNodes(nodes, isDecryptSubNodes, iconLoader);
    }

    public static boolean decryptNodes(List<TetroidNode> nodes, boolean isDecryptSubNodes, INodeIconLoader iconLoader) {
        boolean res = true;
        for (TetroidNode node : nodes) {
            if (node.isCrypted())
                res = res & decryptNode(node, isDecryptSubNodes, iconLoader);
        }
        return res;
    }

    public static boolean decryptNode(TetroidNode node, boolean isDecryptSubNodes, INodeIconLoader iconLoader) {
        if (node == null)
            return false;
        boolean res;
        // расшифровываем поля
        res = decryptNodeFields(node);
        // загружаем иконку
        if (iconLoader != null) {
            iconLoader.loadIcon(node);
        }
        // расшифровывать список записей сразу или при выделении ?
        // пока сразу
        if (node.getRecordsCount() > 0) {
            res = res & decryptRecordsFields(node.getRecords());
        }
        // расшифровываем подветки
        if (isDecryptSubNodes && node.getSubNodesCount() > 0) {
            res = res & decryptNodes(node.getSubNodes(), isDecryptSubNodes, iconLoader);
        }
        return res;
    }

    /**
     * Расшифровка ветки
     * @param node
     * @return
     */
    public static boolean decryptNodeFields(TetroidNode node) {
        boolean res;
        // name
        String temp = CryptManager.decryptBase64(mCryptKey, node.getName());
        res = (temp != null);
        if (res) {
//            node.setName(temp);
            node.setDecryptedName(temp);
        }
        // icon
        temp = CryptManager.decryptBase64(mCryptKey, node.getIconName());
        res = res & (temp != null);
        if (temp != null) {
//            node.setIconName(temp);
            node.setDecryptedIconName(temp);
        }
        // decryption result
        node.setDecrypted(res);
        return res;
    }

    /**
     * Расшифровка списка записей
     * @param records
     * @return
     */
    public static boolean decryptRecordsFields(List<TetroidRecord> records) {
        boolean res = true;
        for (TetroidRecord record : records) {
            if (record.isCrypted()) {
                res = res & decryptRecordFields(record);
                if (record.getAttachedFilesCount() > 0)
                    for (TetroidFile file : record.getAttachedFiles()) {
                        res = res & decryptAttach(file);
                    }
            }
        }
        return res;
    }

    /**
     * Расшифровка записи
     * Расшифровать сразу и записи, или при выделении ветки?
     * @param record
     * @return
     */
    public static boolean decryptRecordFields(TetroidRecord record) {
        boolean res;
        String temp = CryptManager.decryptBase64(mCryptKey, record.getName());
        res = (temp != null);
        if (res) {
//            record.setName(temp);
            record.setDecryptedName(temp);
        }
        temp = CryptManager.decryptBase64(mCryptKey, record.getTagsString());
        res = res & (temp != null);
        if (temp != null) {
//            record.setTagsString(temp);
            record.setDecryptedTagsString(temp);
            tagsParser.parseRecordTags(record, temp);
        }
        temp = CryptManager.decryptBase64(mCryptKey, record.getAuthor());
        res = res & (temp != null);
        if (temp != null) {
//            record.setAuthor(temp);
            record.setDecryptedAuthor(temp);
        }
        temp = CryptManager.decryptBase64(mCryptKey, record.getUrl());
        res = res & (temp != null);
        if (temp != null) {
//            record.setUrl(temp);
            record.setDecryptedUrl(temp);
        }
        record.setDecrypted(res);
        return res;
    }

    /**
     * Расшифровка прикрепленного файла.
     * @param file
     * @return
     */
    public static boolean decryptAttach(TetroidFile file) {
        String temp = CryptManager.decryptBase64(mCryptKey, file.getName());
        boolean res = (temp != null);
        if (res) {
//            file.setName(temp);
            file.setDecryptedName(temp);
        }
        file.setDecrypted(res);
        return res;
    }

}
