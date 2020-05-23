package com.gee12.mytetroid.crypt;

import com.gee12.mytetroid.data.INodeIconLoader;
import com.gee12.mytetroid.data.IRecordFileCrypter;
import com.gee12.mytetroid.data.ITagsParser;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;

import org.jsoup.internal.StringUtil;

import java.util.List;

public class CryptManager extends Crypter {

    private static ITagsParser tagsParser;
    private static IRecordFileCrypter recordFileCrypter;


    public static void initFromPass(String pass, ITagsParser tagsParser, IRecordFileCrypter recordFileCrypter) {
        int[] key = passToKey(pass);
        // записываем в память
        setCryptKey(key);
        init(key, tagsParser, recordFileCrypter);
    }

    public static void initFromMiddleHash(String passHash, ITagsParser tagsParser, IRecordFileCrypter recordFileCrypter) {
        int[] key = middlePassHashToKey(passHash);
        init(key, tagsParser, recordFileCrypter);
    }

    public static void init(int[] key, ITagsParser tagsParser, IRecordFileCrypter recordFileCrypter) {
        // записываем в память
        setCryptKey(key);
        CryptManager.tagsParser = tagsParser;
        CryptManager.recordFileCrypter = recordFileCrypter;
    }

    /**
     * Зашифровка веток.
     * @param nodes
     * @param isReencrypt Если true, то повторное шифрование зашифрованного объекта
     *      *                    (должно быть расшифрованно перед этим)
     * @return
     */
    public static boolean encryptNodes(List<TetroidNode> nodes, boolean isReencrypt) {
        boolean res = true;
        for (TetroidNode node : nodes) {
            if (!isReencrypt && !node.isCrypted() || isReencrypt && node.isCrypted() && node.isDecrypted()) {
                res = res & encryptNode(node, isReencrypt);
            }
        }
        return res;
    }

    /**
     * Зашифровка ветки.
     * @param node
     * @param isReencrypt
     * @return
     */
    public static boolean encryptNode(TetroidNode node, boolean isReencrypt) {
        if (node == null)
            return false;
        boolean res;
        // засшифровываем поля
        res = encryptNodeFields(node, isReencrypt);
        if (node.getRecordsCount() > 0) {
            res = res & encryptRecordsAndFiles(node.getRecords(), isReencrypt);
        }
        // расшифровываем подветки
        if (node.getSubNodesCount() > 0) {
            res = res & encryptNodes(node.getSubNodes(), isReencrypt);
        }
        return res;
    }

    /**
     * Зашифровка полей ветки.
     * @param node
     * @param isReencrypt
     * @return
     */
    public static boolean encryptNodeFields(TetroidNode node, boolean isReencrypt) {
        boolean res;
        // name
        String temp = CryptManager.encryptTextBase64(node.getName());
        res = (temp != null);
        if (res) {
            if (!isReencrypt && !node.isCrypted()) {
                node.setDecryptedName(node.getName());
            }
            node.setName(temp);
        }
        // icon
        String iconName = node.getIconName();
        if (!StringUtil.isBlank(iconName)) {
            temp = CryptManager.encryptTextBase64(iconName);
            res = res & (temp != null);
            if (temp != null) {
                if (!isReencrypt && !node.isCrypted()) {
                    node.setDecryptedIconName(iconName);
                }
                node.setIconName(temp);
            }
        }
        // encryption result
        if (!isReencrypt && !node.isCrypted()) {
            node.setIsCrypted(res);
            node.setDecrypted(res);
        }
        return res;
    }

    /**
     * Зашифровка полей списка записей и полей их прикрепленных прифайлов.
     * @param records
     * @param isReencrypt
     * @return
     */
    public static boolean encryptRecordsAndFiles(List<TetroidRecord> records, boolean isReencrypt) {
        boolean res = true;
        for (TetroidRecord record : records) {
            // зашифровываем файл записи
            if (recordFileCrypter != null) {
                recordFileCrypter.cryptRecordFile(record, true);
            }
            res = res & encryptRecordFields(record, isReencrypt);
            if (record.getAttachedFilesCount() > 0)
                for (TetroidFile file : record.getAttachedFiles()) {
                    res = res & encryptAttach(file, isReencrypt);
                }
        }
        return res;
    }

    /**
     * Зашифровка полей записи.
     * @param record
     * @param isReencrypt
     * @return
     */
    public static boolean encryptRecordFields(TetroidRecord record, boolean isReencrypt) {
        boolean res;
        String temp = CryptManager.encryptTextBase64(record.getName());
        res = (temp != null);
        if (res) {
            if (!isReencrypt && !record.isCrypted()) {
                record.setDecryptedName(record.getName());
            }
            record.setName(temp);
        }
        String tagsString = record.getTagsString();
        if (!StringUtil.isBlank(tagsString)) {
            temp = CryptManager.encryptTextBase64(tagsString);
            res = res & (temp != null);
            if (temp != null) {
                if (!isReencrypt && !record.isCrypted()) {
                    record.setDecryptedTagsString(tagsString);
                }
                record.setTagsString(temp);
            }
        }
        String author = record.getAuthor();
        if (!StringUtil.isBlank(author)) {
            temp = CryptManager.encryptTextBase64(author);
            res = res & (temp != null);
            if (temp != null) {
                if (!isReencrypt && !record.isCrypted()) {
                    record.setDecryptedAuthor(author);
                }
                record.setAuthor(temp);
            }
        }
        String url = record.getUrl();
        if (!StringUtil.isBlank(url)) {
            temp = CryptManager.encryptTextBase64(url);
            res = res & (temp != null);
            if (temp != null) {
                if (!isReencrypt && !record.isCrypted()) {
                    record.setDecryptedUrl(url);
                }
                record.setUrl(temp);
            }
        }
        if (!isReencrypt && !record.isCrypted()) {
            record.setIsCrypted(res);
            record.setDecrypted(res);
        }
        return res;
    }

    /**
     * Зашифровка полей прикрепленного файла.
     * @param file
     * @param isReencrypt
     * @return
     */
    public static boolean encryptAttach(TetroidFile file, boolean isReencrypt) {
        String temp = CryptManager.encryptTextBase64(file.getName());
        boolean res = (temp != null);
        if (res) {
            if (!isReencrypt && !file.isCrypted()) {
                file.setDecryptedName(file.getName());
            }
            file.setName(temp);
        }
        if (!isReencrypt && !file.isCrypted()) {
            file.setIsCrypted(res);
            file.setDecrypted(res);
        }
        return res;
    }

    /**
     * Расшифровка веток.
     * @param nodes
     * @param isDecryptSubNodes
     * @param iconLoader
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    public static boolean decryptNodes(List<TetroidNode> nodes, boolean isDecryptSubNodes, INodeIconLoader iconLoader,
                                       boolean dropCrypt) {
        boolean res = true;
        for (TetroidNode node : nodes) {
            res = res & decryptNode(node, isDecryptSubNodes, iconLoader, dropCrypt);
        }
        return res;
    }

    /**
     * Расшифровка ветки.
     * @param node
     * @param isDecryptSubNodes
     * @param iconLoader
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    public static boolean decryptNode(TetroidNode node, boolean isDecryptSubNodes, INodeIconLoader iconLoader,
                                      boolean dropCrypt) {
        if (node == null)
            return false;
        boolean res = true;
        if (node.isCrypted() && (!node.isDecrypted() || dropCrypt)) {
            // расшифровываем поля
            res = decryptNodeFields(node, dropCrypt);
            // загружаем иконку
            if (iconLoader != null) {
                iconLoader.loadIcon(node);
            }
            // TODO: расшифровывать список записей сразу или при выделении ?
            //  (пока сразу)
            if (node.getRecordsCount() > 0) {
                res = res & decryptRecordsAndFiles(node.getRecords(), dropCrypt);
            }
        }
        // расшифровываем подветки
        if (isDecryptSubNodes && node.getSubNodesCount() > 0) {
            res = res & decryptNodes(node.getSubNodes(), true, iconLoader, dropCrypt);
        }
        return res;
    }

    /**
     * Расшифровка полей ветки.
     * @param node
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    public static boolean decryptNodeFields(TetroidNode node, boolean dropCrypt) {
        boolean res;
        // name
        String temp = CryptManager.decryptBase64(node.getName(true));
        res = (temp != null);
        if (res) {
            if (dropCrypt) {
                node.setName(temp);
                node.setDecryptedName(null);
            } else
                node.setDecryptedName(temp);
        }
        // icon
        temp = CryptManager.decryptBase64(node.getIconName(true));
        res = res & (temp != null);
        if (temp != null) {
            if (dropCrypt) {
                node.setIconName(temp);
                node.setDecryptedIconName(null);
            } else
                node.setDecryptedIconName(temp);
        }
        // decryption result
        if (dropCrypt) {
            node.setIsCrypted(!res);
            node.setDecrypted(!res);
        } else
            node.setDecrypted(res);
        return res;
    }

    /**
     * Расшифровка полей списка записей и полей их прикрепленных прифайлов.
     * @param records
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    public static boolean decryptRecordsAndFiles(List<TetroidRecord> records, boolean dropCrypt) {
        boolean res = true;
        for (TetroidRecord record : records) {
//            if (record.isCrypted()) {
                // расшифровываем файл записи
                if (dropCrypt && recordFileCrypter != null) {
                    recordFileCrypter.cryptRecordFile(record, false);
                }
                res = res & decryptRecordFields(record, dropCrypt);
                if (record.getAttachedFilesCount() > 0)
                    for (TetroidFile file : record.getAttachedFiles()) {
                        res = res & decryptAttach(file, dropCrypt);
                    }
//            }
        }
        return res;
    }

    /**
     * Расшифровка полей записи.
     * @param record
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    public static boolean decryptRecordFields(TetroidRecord record, boolean dropCrypt) {
        boolean res;
        String temp = CryptManager.decryptBase64(record.getName(true));
        res = (temp != null);
        if (res) {
            if (dropCrypt) {
                record.setName(temp);
                record.setDecryptedName(null);
            } else
                record.setDecryptedName(temp);
        }
        temp = CryptManager.decryptBase64(record.getTagsString(true));
        res = res & (temp != null);
        if (temp != null) {
            if (dropCrypt) {
                record.setTagsString(temp);
                record.setDecryptedTagsString(null);
            } else
                record.setDecryptedTagsString(temp);
            if (tagsParser != null) {
                tagsParser.parseRecordTags(record, temp);
            }
        }
        temp = CryptManager.decryptBase64(record.getAuthor(true));
        res = res & (temp != null);
        if (temp != null) {
            if (dropCrypt) {
                record.setAuthor(temp);
                record.setDecryptedAuthor(null);
            } else
                record.setDecryptedAuthor(temp);
        }
        temp = CryptManager.decryptBase64(record.getUrl(true));
        res = res & (temp != null);
        if (temp != null) {
            if (dropCrypt) {
                record.setUrl(temp);
                record.setDecryptedUrl(null);
            } else
                record.setDecryptedUrl(temp);
        }
        if (dropCrypt) {
            record.setIsCrypted(!res);
            record.setDecrypted(!res);
        } else
            record.setDecrypted(res);
        return res;
    }

    /**
     * Расшифровка полей прикрепленного файла.
     * @param file
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    public static boolean decryptAttach(TetroidFile file, boolean dropCrypt) {
        String temp = CryptManager.decryptBase64(file.getName(true));
        boolean res = (temp != null);
        if (res) {
            if (dropCrypt) {
                file.setName(temp);
                file.setDecryptedName(null);
            } else
                file.setDecryptedName(temp);
        }
        if (dropCrypt) {
            file.setIsCrypted(!res);
            file.setDecrypted(!res);
        } else
            file.setDecrypted(res);
        return res;
    }

}
