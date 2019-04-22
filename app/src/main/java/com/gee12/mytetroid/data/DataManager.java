package com.gee12.mytetroid.data;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.gee12.mytetroid.Utils;
import com.gee12.mytetroid.crypt.CryptManager;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.List;

public class DataManager extends XMLManager implements IDecryptHandler {

//    public static final Exception EmptyFieldException = new Exception("Отсутствуют данные для проверки пароля (поле middle_hash_check_data пустое)");
    public static class EmptyFieldException extends Exception {

        private String fieldName;

        public EmptyFieldException(String fieldName) {
            super();
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

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
     * @return
     */
    public static boolean init(String dataFolderPath) {
        instance = new DataManager();
        instance.storagePath = dataFolderPath;
        databaseINI = new INIProperties();
//        boolean xmlParsed = false;
        boolean res = false;
        try {
//            FileInputStream fis = new FileInputStream(dataFolderPath + "/mytetra.xml");
//            IDecryptHandler decryptHandler = (isDecrypt) ? instance : null;
//            instance.rootNodesCollection = instance.parse(fis, decryptHandler);
//            xmlParsed = true;
            res = databaseINI.load(dataFolderPath + "/database.ini");
        } catch (Exception ex) {
//            if (xmlParsed)
                // ошибка загрузки ini
//            else
                // ошибка загрузки xml
            return false;
        }
        return res;
    }

    /**
     *
     * @param isDecrypt Расшифровывать ли ветки
     * @return
     */
    public static boolean readStorage(boolean isDecrypt) {
        try {
            FileInputStream fis = new FileInputStream(instance.storagePath + "/mytetra.xml");
            IDecryptHandler decryptHandler = (isDecrypt) ? instance : null;
            instance.rootNodesCollection = instance.parse(fis, decryptHandler);
        } catch (Exception ex) {
            // ошибка загрузки xml
            return false;
        }
        return true;
    }

    public static boolean decryptAll() {
        // достаем сохраненный пароль
        return CryptManager.decryptAll(instance.rootNodesCollection, true, instance);
    }

    /**
     * From IDecryptHandler
     * @param node
     */
    @Override
    public void decryptNode(TetroidNode node) {
        CryptManager.decryptNode(node, false, this);
    }

    public static boolean checkPass(String pass) throws EmptyFieldException {
        // нужно тоже обработать варианты, когда эти поля пустые (!)
        // ...
        String salt = databaseINI.getWithoutQuotes("crypt_check_salt");
        if (Utils.isNullOrEmpty(salt)) {
            throw new EmptyFieldException("crypt_check_salt");
        }
        String checkhash = databaseINI.getWithoutQuotes("crypt_check_hash");
        if (Utils.isNullOrEmpty(checkhash)) {
            throw new EmptyFieldException("crypt_check_hash");
        }
        // ...
        return CryptManager.checkPass(pass, salt, checkhash);
    }

    public static boolean checkMiddlePassHash(String passHash) throws EmptyFieldException {
        String checkdata = databaseINI.get("middle_hash_check_data");
        if (Utils.isNullOrEmpty(checkdata)) {
            throw new EmptyFieldException("middle_hash_check_data");
        }
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

    @Override
    public void loadIcon(TetroidNode node) {
        if (node.isNonCryptedOrDecrypted())
            node.loadIconFromStorage(storagePath + "/icons");
    }

    public static String getRecordTextDecrypted(TetroidRecord record) {
        String pathURI = getStoragePathBaseUri() + record.getDirName() + "/" + record.getFileName();
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

    @NonNull
    private static String getStoragePathBaseUri() {
        return "file://" + instance.storagePath + "/base/";
    }

    @NonNull
    private static String getStoragePathBase() {
        return instance.storagePath + "/base/";
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

    public static boolean openFile(Context context, TetroidRecord record, TetroidFile file) {
        String fileDisplayName = file.getFileName();
        String ext = Utils.getExtWithComma(fileDisplayName);
        String fullFileName = String.format("%s%s/%s%s", getStoragePathBase(), record.getDirName(), file.getId(), ext);
        File f = new File(fullFileName);
        if(f.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.substring(1));
            intent.setDataAndType(Uri.fromFile(f), mimeType);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try {
                context.startActivity(intent);
            }
            catch(ActivityNotFoundException e) {
                Toast.makeText(context, "Ошибка открытия файла " + fileDisplayName, Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            Toast.makeText(context, "Файл отсутствует: " + fileDisplayName, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static List<TetroidNode> getRootNodes() {
        return instance.rootNodesCollection;
    }

    public static boolean isNodesExist() {
        return (instance.rootNodesCollection != null && !instance.rootNodesCollection.isEmpty());
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

    public static boolean isCrypted() {
        return (Integer.parseInt(databaseINI.get("crypt_mode")) == 1);
    }
}
