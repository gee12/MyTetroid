package com.gee12.mytetroid.data;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;

import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.Utils;
import com.gee12.mytetroid.crypt.CryptManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

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

    public static final String BASE_FOLDER = "base/";
    public static final String ICONS_FOLDER = "icons/";
    public static final String MYTETRA_XML_FILE = "mytetra.xml";
    public static final String DATABASE_INI_FILE = "database.ini";

    /**
     *
     */
    private String storagePath;

    /**
     *
     */
    private static INIProperties databaseINI;

    /**
     *
     */
    private static DataManager instance;

    /**
     *
     * @param storagePath
     * @return
     */
    public static boolean init(String storagePath) {
        DataManager.instance = new DataManager();
        DataManager.instance.storagePath = storagePath;
        DataManager.databaseINI = new INIProperties();
        boolean res;
        try {
            res = databaseINI.load(storagePath + File.separator + DATABASE_INI_FILE);
        } catch (Exception ex) {
            LogManager.addLog(ex);
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
        boolean res = false;
        try {
            FileInputStream fis = new FileInputStream(
                    instance.storagePath + File.separator + MYTETRA_XML_FILE);
            IDecryptHandler decryptHandler = (isDecrypt) ? instance : null;
//            DataManager.instance.rootNodesCollection = instance.parse(fis, decryptHandler);
            res = instance.parse(fis, decryptHandler);
        } catch (Exception ex) {
            // ошибка загрузки xml
            LogManager.addLog(ex);
        }
        return res;
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
    public boolean decryptNode(TetroidNode node) {
//        boolean res = CryptManager.decryptNode(node, false, this);
//        if (res) {
//            // парсим метки
//            parseNodeTags(node);
//        }
        return CryptManager.decryptNode(node, false, this);
    }

    public static boolean checkPass(String pass) throws EmptyFieldException {
        // нужно также обработать варианты, когда эти поля пустые (!)
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
    public static String getRecordTextUri(TetroidRecord record) {
        String path = null;
        if (record.isCrypted()) {
            if (record.isDecrypted()) {
                // расшифровываем файл и ложим в temp
                path = SettingsManager.getTempPath()+"/"+record.getDirName()+File.separator+record.getFileName();
            }
        } else {
            path = SettingsManager.getStoragePath()+File.separator+BASE_FOLDER+record.getDirName()+File.separator+record.getFileName();
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
            node.loadIconFromStorage(storagePath + File.separator + ICONS_FOLDER);
    }

    /**
     * Получение текста записи.
     * @param record
     * @return
     */
    public static String getRecordTextDecrypted(TetroidRecord record) {
        String pathUri = getStoragePathBaseUri() + File.separator
                + record.getDirName() + File.separator + record.getFileName();
//        String text = Utils.readAllFile(URI.create(pathUri));
        String res = null;
        if (record.isCrypted()) {
            if (record.isDecrypted()) {
                byte[] text = new byte[0];
                try {
                    text = Utils.readFile(URI.create(pathUri));
                } catch (IOException ex) {
                    LogManager.addLog("Ошибка чтения файла записи: ", ex);
                }
                // расшифровываем файл
                res = CryptManager.decryptText(text);
                if (res == null) {
                    LogManager.addLog("Ошибка расшифровки файла записи: " + pathUri, LogManager.Types.ERROR);
                }
            }
        } else {
            try {
                res = Utils.readTextFile(URI.create(pathUri));
            } catch (IOException ex) {
                LogManager.addLog("Ошибка чтения файла записи: ", ex);
            }
        }
        return res;
    }

    public static String getRecordDirUri(TetroidRecord record) {
        return getStoragePathBaseUri() + File.separator + record.getDirName() + File.separator;
    }

    @NonNull
    private static Uri getStoragePathBaseUri() {
//        return "file://" + instance.storagePath + "/base/";
        return Uri.fromFile(new File(getStoragePathBase()));
    }

    @NonNull
    private static String getStoragePathBase() {
        return instance.storagePath + File.separator + BASE_FOLDER;
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

    /**
     * Открытие файла записи сторонным приложением.
     * @param context
     * @param record
     * @param file
     * @return
     */
    public static boolean openFile(Context context, TetroidRecord record, TetroidFile file) {
        String fileDisplayName = file.getName();
        String ext = Utils.getExtWithComma(fileDisplayName);
        String fileIdName = file.getId() + ext;
        String fullFileName = String.format("%s%s/%s", getStoragePathBase(), record.getDirName(), fileIdName);
        File srcFile = new File(fullFileName);
        //
        LogManager.addLog("Открытие файла: " + fullFileName);
        if (srcFile.exists()) {
            // если запись зашифрована
            if (record.isCrypted() && SettingsManager.isDecryptFilesInTemp()) {
                // создаем временный файл
//                File tempFile = createTempCacheFile(context, fileIdName);
//                File tempFile = new File(String.format("%s%s/_%s", getStoragePathBase(), record.getDirName(), fileIdName));
//                File tempFile = createTempExtStorageFile(context, fileIdName);
                String tempFolderPath = SettingsManager.getTempPath() + File.separator + record.getDirName();
                File tempFolder = new File(tempFolderPath);
                if (!tempFolder.exists() && !tempFolder.mkdirs()) {
                    LogManager.addLog(context.getString(R.string.could_not_create_temp_dir) + tempFolderPath, Toast.LENGTH_LONG);
                }
                File tempFile = new File(tempFolder, fileIdName);
//                File tempFile = new File(getTempPath()+File.separator, fileIdName);

                // расшифровываем во временный файл
                try {
                    if ((tempFile.exists() || tempFile.createNewFile()) && CryptManager.decryptFile(srcFile, tempFile)) {
                        srcFile = tempFile;
                    } else {
                        LogManager.addLog(context.getString(R.string.could_not_decrypt_file) + fullFileName, Toast.LENGTH_LONG);
                        return false;
                    }
                } catch (IOException ex) {
                    LogManager.addLog(context.getString(R.string.file_decryption_error) + ex.getMessage(), Toast.LENGTH_LONG);
                    return false;
                }
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.substring(1));
            intent.setDataAndType(Uri.fromFile(srcFile), mimeType);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent);
            }
            catch(ActivityNotFoundException e) {
                LogManager.addLog("Ошибка открытия файла " + fileDisplayName, Toast.LENGTH_LONG);
                return false;
            }
        } else {
            LogManager.addLog("Файл отсутствует: " + fileDisplayName, Toast.LENGTH_SHORT);
            return false;
        }
        return true;
    }

//    public static File createTempExtStorageFile(Context context, String fileName) {
//        return new File(context.getExternalFilesDir(null), fileName);
//    }

    /*
    * Создание файла в частном хранилище приложения во внутренней памяти устройства в кэше
    * Файл позже может удалиться системой при очистке
     */
//    public static File createTempIntCacheFile(Context context, String fileName) {
//        File file = null;
//        try {
//            file = File.createTempFile(fileName, null, context.getCacheDir());
//        } catch (IOException e) {
//            Toast.makeText(context, "Не удалось создать временный файл в частном хранилище приложения во внутренней памяти устройства", Toast.LENGTH_LONG).show();
//        }
//        return file;
//    }

    /**
     * Получение размера прикрепленного файла.
     * @param context
     * @param record
     * @param file
     * @return
     */
    public static String getFileSize(Context context, TetroidRecord record, TetroidFile file) {
        String ext = Utils.getExtWithComma(file.getName());
        String fullFileName = String.format("%s%s/%s%s", getStoragePathBase(), record.getDirName(), file.getId(), ext);

        long size = new File(fullFileName).length() / 1024;
        if (size == 0) {
            return null;
        } else if (size >= 1024) {
            return (size / 1024) + context.getString(R.string.m_bytes);
        } else {
            return size + context.getString(R.string.k_bytes);
        }
    }

    public static List<TetroidNode> getRootNodes() {
        return instance.rootNodesCollection;
    }

    public static TreeMap<String, List<TetroidRecord>> getTagsHashMap() {
        return instance.tagsMap;
    }

    public static boolean isNodesExist() {
        return (instance.rootNodesCollection != null && !instance.rootNodesCollection.isEmpty());
    }

    public static String getStoragePath() {
        return instance.storagePath;
    }

    public static boolean isExistsCryptedNodes() {
        return instance.isExistCryptedNodes;
    }

    public static boolean isCrypted() {
        return (Integer.parseInt(databaseINI.get("crypt_mode")) == 1);
    }

    public static DataManager getInstance() {
        return instance;
    }
}
