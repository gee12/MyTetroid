package com.gee12.mytetroid.data;

import com.gee12.mytetroid.Utils;
import com.gee12.mytetroid.crypt.CryptManager;

import java.io.FileInputStream;
import java.net.URI;
import java.util.List;

public class DataManager extends XMLManager implements IDecryptHandler {

    /**
     *
     */
    private List<TetroidNode> rootNodesCollection;
    /**
     *
     */
    private String storagePath;
    /**
     * ИСПРАВИТЬ !
     */
    private String tempPath = "/tmp/";

    private static INIProperties databaseINI;

    /**
     *
     */
    private static DataManager instance;

    /**
     *
     * @param dataFolderPath
     * @param isDecrypt Расшифровывать ли ветки
     * @return
     */
    public static boolean init(String dataFolderPath, boolean isDecrypt) {
        instance = new DataManager();
        instance.storagePath = dataFolderPath;
        databaseINI = new INIProperties();
        boolean xmlParsed = false;
        try {
            FileInputStream fis = new FileInputStream(dataFolderPath + "/mytetra.xml");
            IDecryptHandler decryptHandler = (isDecrypt) ? DataManager.this : null;
            instance.rootNodesCollection = instance.parse(fis, decryptHandler);
            xmlParsed = true;
            databaseINI.load(dataFolderPath + "/database.ini");
        } catch (Exception ex) {
            if (xmlParsed)
                // ошибка загрузки ini
//            else
                // ошибка загрузки xml
            return false;
        }
        return true;
    }

    public static boolean decryptAll() {
        // достаем сохраненный пароль
//        String pass = "iHMy5~sv62";
//        return CryptManager.decryptAll(pass, instance.rootNodesCollection);
        return CryptManager.decryptAll(instance.rootNodesCollection);
    }


    @Override
    public void decryptNode(TetroidNode node) {
        CryptManager.decryptNode(node);
    }

    public static boolean checkPass(String pass) {
        String salt = databaseINI.getWithoutQuotes("crypt_check_salt");
        String checkhash = databaseINI.getWithoutQuotes("crypt_check_hash");
        return CryptManager.checkPass(pass, salt, checkhash);
    }

    public static boolean checkMiddlePassHash(String passHash) {
        String checkdata = databaseINI.get("middle_hash_check_data");
        return CryptManager.checkMiddlePassHash(passHash, checkdata);
    }

    /**
     * Получение пути к файлу с содержимым записи.
     * Если расшифрован, то в tempPath. Если не был зашифрован, то в storagePath.
     * @return
     */
    public static String getRecordTextUrl(TetroidRecord record) {
        String path = null;
        if (record.isCrypted()) {
            if (record.isDecrypted()) {
                // расшифровываем файл и ложим в tempPath
                String temp = instance.tempPath+"/"+record.getDirName()+"/"+record.getFileName();

                path = temp;
            }
        } else {
            path = instance.storagePath+"/base/"+record.getDirName()+"/"+record.getFileName();
        }
        /*String path = (isCrypted && isDecrypted)    // логическая ошибка в условии
                ? tempPath+dirName+"/"+fileName
                : storagePath+"/base/"+dirName+"/"+fileName;*/
//        File file = new File(storagePath+"/base/"+dirName+"/"+fileName);
//        return "file:///" + file.getAbsolutePath();
        return (path != null) ? "file:///" + path : null;
    }

    public void loadIcon(TetroidNode node) {
        if (node.isNonCryptedOrDecrypted())
            node.loadIconFromStorage(storagePath + "/icons");
    }

    public static String getRecordTextDecrypted(TetroidRecord record) {
        String pathURI = "file:///"+instance.storagePath+"/base/"+record.getDirName()+"/"+record.getFileName();
//        String text = Utils.readAllFile(URI.create(pathURI));
        String res = null;
        if (record.isCrypted()) {
            if (record.isDecrypted()) {
                byte[] text = Utils.readFile(URI.create(pathURI));
                // расшифровываем файл
                res = CryptManager.decryptText(text);
            }
        } else {
            res = Utils.readTextFile(URI.create(pathURI));
        }
        return res;
    }

    public static TetroidNode getNode(String id) {
//        for (TetroidNode node : instance.rootNodesCollection) {
//            if (getNodeInHierarchy(node, id) != null)
//                return node;
//        }
        return getNodeInHierarchy(instance.rootNodesCollection, id);
    }

    public static TetroidNode getNodeInHierarchy(List<TetroidNode> nodes, String id) {
        for (TetroidNode node : nodes) {
//            if (getNodeInHierarchy(node, id) != null)
//                return node;
            if (id.equals(node.getId()))
                return node;
            else if (node.isExpandable())
                return getNodeInHierarchy(node.getSubNodes(), id);
        }
        return null;
    }

    /*private static TetroidNode getNodeInHierarchy(TetroidNode node, String id) {
        if (id.equals(node.getId()))
            return node;
        else if (node.isExpandable()) {
            return getNodeInHierarchy(node.getSubNodes(), id);
//            for (TetroidNode subNode : node.getSubNodes()) {
//                if (getNodeInHierarchy(subNode, id) != null)
//                    return subNode;
//            }
        }
        return null;
    }*/

    public static List<TetroidNode> getRootNodes() {
        return instance.rootNodesCollection;
    }

    public static String getStoragePath() {
        return instance.storagePath;
    }

    public static String getTempPath() {
        return instance.tempPath;
    }

    public static boolean isExistsCryptedNodes() {
        return instance.isExistCryptedNodes;
    }
}
