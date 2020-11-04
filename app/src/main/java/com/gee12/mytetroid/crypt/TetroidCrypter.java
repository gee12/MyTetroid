package com.gee12.mytetroid.crypt;

import android.content.Context;

import com.gee12.mytetroid.ILogger;
import com.gee12.mytetroid.data.INodeIconLoader;
import com.gee12.mytetroid.data.IRecordFileCrypter;
import com.gee12.mytetroid.data.ITagsParser;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;

import org.jsoup.internal.StringUtil;

import java.util.List;

public class TetroidCrypter extends Crypter {

    private ITagsParser mTagsParser;
    private IRecordFileCrypter mRecordFileCrypter;


    public TetroidCrypter(ILogger mLogger, ITagsParser tagsParser, IRecordFileCrypter recordFileCrypter) {
        super(mLogger);

        this.mTagsParser = tagsParser;
        this.mRecordFileCrypter = recordFileCrypter;
    }

    public void initFromPass(String pass) {
        int[] key = passToKey(pass);
        // записываем в память
        setCryptKey(key);
        init(key);
    }

    public void initFromMiddleHash(String passHash) {
        int[] key = middlePassHashToKey(passHash);
        init(key);
    }

    public void init(int[] key) {
        // записываем в память
        setCryptKey(key);
    }

    /**
     * Зашифровка веток.
     * @param nodes
     * @param isReencrypt Если true, то повторное шифрование зашифрованного объекта
     *      *                    (должно быть расшифрованно перед этим)
     * @return
     */
    public boolean encryptNodes(Context context, List<TetroidNode> nodes, boolean isReencrypt) {
        boolean res = true;
        for (TetroidNode node : nodes) {
//            if (!isReencrypt && !node.isCrypted() || isReencrypt && node.isCrypted() && node.isDecrypted()) {
                res = res & encryptNode(context, node, isReencrypt);
//            }
        }
        return res;
    }

    /**
     * Зашифровка ветки.
     * @param node
     * @param isReencrypt
     * @return
     */
    public boolean encryptNode(Context context, TetroidNode node, boolean isReencrypt) {
        if (node == null)
            return false;
        boolean res = true;
        if (!isReencrypt && !node.isCrypted() || isReencrypt && node.isCrypted() && node.isDecrypted()) {
            // засшифровываем поля
            res = encryptNodeFields(node, isReencrypt);
            if (node.getRecordsCount() > 0) {
                res = res & encryptRecordsAndFiles(context, node.getRecords(), isReencrypt);
            }
        }
        // расшифровываем подветки
        if (node.getSubNodesCount() > 0) {
            res = res & encryptNodes(context, node.getSubNodes(), isReencrypt);
        }
        return res;
    }

    /**
     * Зашифровка полей ветки.
     * @param node
     * @param isReencrypt
     * @return
     */
    public boolean encryptNodeFields(TetroidNode node, boolean isReencrypt) {
        boolean res;
        // name
        String temp = encryptTextBase64(node.getName());
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
            temp = encryptTextBase64(iconName);
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
            node.setIsDecrypted(res);
        }
        return res;
    }

    /**
     * Зашифровка полей списка записей и полей их прикрепленных прифайлов.
     * @param records
     * @param isReencrypt Флаг, заставляющий шифровать файлы записи даже тогда, когда запись
     *                    уже зашифрована.
     * @return
     */
    public boolean encryptRecordsAndFiles(Context context, List<TetroidRecord> records, boolean isReencrypt) {
        boolean res = true;
        for (TetroidRecord record : records) {
            // зашифровываем файлы записи
            if (mRecordFileCrypter != null) {
                res = res & mRecordFileCrypter.cryptRecordFiles(context, record, record.isCrypted() && !isReencrypt, true);
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
    public boolean encryptRecordFields(TetroidRecord record, boolean isReencrypt) {
        boolean res;
        String temp = encryptTextBase64(record.getName());
        res = (temp != null);
        if (res) {
            if (!isReencrypt && !record.isCrypted()) {
                record.setDecryptedName(record.getName());
            }
            record.setName(temp);
        }
        String tagsString = record.getTagsString();
        if (!StringUtil.isBlank(tagsString)) {
            temp = encryptTextBase64(tagsString);
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
            temp = encryptTextBase64(author);
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
            temp = encryptTextBase64(url);
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
            record.setIsDecrypted(res);
        }
        return res;
    }

    /**
     * Зашифровка полей прикрепленного файла.
     * @param file
     * @param isReencrypt
     * @return
     */
    public boolean encryptAttach(TetroidFile file, boolean isReencrypt) {
        String temp = encryptTextBase64(file.getName());
        boolean res = (temp != null);
        if (res) {
            if (!isReencrypt && !file.isCrypted()) {
                file.setDecryptedName(file.getName());
            }
            file.setName(temp);
        }
        if (!isReencrypt && !file.isCrypted()) {
            file.setIsCrypted(res);
            file.setIsDecrypted(res);
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
    public boolean decryptNodes(Context context, List<TetroidNode> nodes, boolean isDecryptSubNodes, boolean decryptRecords,
                                       INodeIconLoader iconLoader, boolean dropCrypt, boolean decryptFiles) {
        boolean res = true;
        for (TetroidNode node : nodes) {
            res = res & decryptNode(context, node, isDecryptSubNodes, decryptRecords, iconLoader, dropCrypt, decryptFiles);
        }
        return res;
    }

    /**
     * Расшифровка ветки.
     * @param node
     * @param decryptSubNodes
     * @param iconLoader
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    public boolean decryptNode(Context context, TetroidNode node, boolean decryptSubNodes, boolean decryptRecords,
                                      INodeIconLoader iconLoader, boolean dropCrypt, boolean decryptFiles) {
        if (node == null)
            return false;
        boolean res = true;
        if (node.isCrypted() && (!node.isDecrypted() || dropCrypt || decryptFiles)) {
            // расшифровываем поля
            res = decryptNodeFields(node, dropCrypt);
            // загружаем иконку
            if (iconLoader != null) {
                iconLoader.loadIcon(context, node);
            }
            // TODO: расшифровывать список записей сразу или при выделении ?
            //  (пока сразу)
            if (decryptRecords && node.getRecordsCount() > 0) {
                res = res & decryptRecordsAndFiles(context, node.getRecords(), dropCrypt, decryptFiles);
            }
        }
        // расшифровываем подветки
        if (decryptSubNodes && node.getSubNodesCount() > 0) {
            res = res & decryptNodes(context, node.getSubNodes(), true, decryptRecords, iconLoader, dropCrypt, decryptFiles);
        }
        return res;
    }

    /**
     * Расшифровка полей ветки.
     * @param node
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    public boolean decryptNodeFields(TetroidNode node, boolean dropCrypt) {
        boolean res;
        // name
        String temp = decryptBase64(node.getName(true));
        res = (temp != null);
        if (res) {
            if (dropCrypt) {
                node.setName(temp);
                node.setDecryptedName(null);
            } else
                node.setDecryptedName(temp);
        }
        // icon
        temp = decryptBase64(node.getIconName(true));
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
            node.setIsDecrypted(!res);
        } else
            node.setIsDecrypted(res);
        return res;
    }

    /**
     * Расшифровка полей списка записей и полей их прикрепленных прифайлов.
     * @param records
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    public boolean decryptRecordsAndFiles(Context context, List<TetroidRecord> records, boolean dropCrypt, boolean decryptFiles) {
        boolean res = true;
        for (TetroidRecord record : records) {
            res = res & decryptRecordAndFiles(context, record, dropCrypt, decryptFiles);
        }
        return res;
    }

    public boolean decryptRecordAndFiles(Context context, TetroidRecord record, boolean dropCrypt, boolean decryptFiles) {
        boolean res = decryptRecordFields(record, dropCrypt);
        if (record.getAttachedFilesCount() > 0)
            for (TetroidFile file : record.getAttachedFiles()) {
                res = res & decryptAttach(file, dropCrypt);
            }
        // расшифровываем файлы записи
        if ((dropCrypt || decryptFiles) && mRecordFileCrypter != null) {
            res = res & mRecordFileCrypter.cryptRecordFiles(context, record, true,false);
        }
        return res;
    }

    /**
     * Расшифровка полей записи.
     * @param record
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    public boolean decryptRecordFields(TetroidRecord record, boolean dropCrypt) {
        boolean res;
        String temp = decryptBase64(record.getName(true));
        res = (temp != null);
        if (res) {
            if (dropCrypt) {
                record.setName(temp);
                record.setDecryptedName(null);
            } else
                record.setDecryptedName(temp);
        }
        temp = decryptBase64(record.getTagsString(true));
        res = res & (temp != null);
        if (temp != null) {
            if (dropCrypt) {
                record.setTagsString(temp);
                record.setDecryptedTagsString(null);
            } else
                record.setDecryptedTagsString(temp);
            if (mTagsParser != null) {
                mTagsParser.parseRecordTags(record, temp);
            }
        }
        temp = decryptBase64(record.getAuthor(true));
        res = res & (temp != null);
        if (temp != null) {
            if (dropCrypt) {
                record.setAuthor(temp);
                record.setDecryptedAuthor(null);
            } else
                record.setDecryptedAuthor(temp);
        }
        temp = decryptBase64(record.getUrl(true));
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
            record.setIsDecrypted(!res);
        } else
            record.setIsDecrypted(res);
        return res;
    }

    /**
     * Расшифровка полей прикрепленного файла.
     * @param file
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    public boolean decryptAttach(TetroidFile file, boolean dropCrypt) {
        String temp = decryptBase64(file.getName(true));
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
            file.setIsDecrypted(!res);
        } else
            file.setIsDecrypted(res);
        return res;
    }

}
