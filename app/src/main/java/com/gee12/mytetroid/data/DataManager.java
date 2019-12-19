package com.gee12.mytetroid.data;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.content.FileProvider;

import com.gee12.mytetroid.BuildConfig;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.crypt.CryptManager;
import com.gee12.mytetroid.utils.FileUtils;

import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class DataManager extends XMLManager implements IDecryptHandler {

    public static final String QUOTES_PARAM_STRING = "\"\"";
    public static final String INI_CRYPT_CHECK_SALT = "crypt_check_salt";
    public static final String INI_CRYPT_CHECK_HASH = "crypt_check_hash";
    public static final String INI_MIDDLE_HASH_CHECK_DATA = "middle_hash_check_data";
    public static final String INI_CRYPT_MODE = "crypt_mode";

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

    private static Context context;

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
    public static boolean init(Context ctx, String storagePath) {
        context = ctx;
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
        File file = new File(instance.storagePath + File.separator + MYTETRA_XML_FILE);
        if (!file.exists()) {
            LogManager.addLog(context.getString(R.string.missing_file) + MYTETRA_XML_FILE, LogManager.Types.ERROR);
            return false;
        }
        try {
            FileInputStream fis = new FileInputStream(file);
            IDecryptHandler decryptHandler = (isDecrypt) ? instance : null;
            res = instance.parse(fis, decryptHandler);

            if (BuildConfig.DEBUG) {
//                TestData.addNodes(instance.rootNodesList, 100, 100);
            }

        } catch (Exception ex) {
            // ошибка загрузки xml
            LogManager.addLog(ex);
        }
        return res;
    }

    public static boolean decryptAll() {
        // достаем сохраненный пароль
        return CryptManager.decryptAll(instance.rootNodesList, true, instance);
    }

    /**
     * From IDecryptHandler
     * @param node
     */
    @Override
    public boolean decryptNode(@NonNull TetroidNode node) {
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
        String salt = databaseINI.getWithoutQuotes(INI_CRYPT_CHECK_SALT);
        if (TextUtils.isEmpty(salt)) {
            throw new EmptyFieldException(INI_CRYPT_CHECK_SALT);
        }
        String checkhash = databaseINI.getWithoutQuotes(INI_CRYPT_CHECK_HASH);
        if (TextUtils.isEmpty(checkhash)) {
            throw new EmptyFieldException(INI_CRYPT_CHECK_HASH);
        }
        // ...
        return CryptManager.checkPass(pass, salt, checkhash);
    }

    public static boolean checkMiddlePassHash(String passHash) throws EmptyFieldException {
        String checkdata = databaseINI.get(INI_MIDDLE_HASH_CHECK_DATA);
        if (TextUtils.isEmpty(checkdata) || QUOTES_PARAM_STRING.equals(checkdata)) {
            throw new EmptyFieldException(INI_MIDDLE_HASH_CHECK_DATA);
        }
        return CryptManager.checkMiddlePassHash(passHash, checkdata);
    }

    /**
     * Получение пути к файлу с содержимым записи.
     * Если расшифрован, то в tempPath. Если не был зашифрован, то в storagePath.
     * @return
     */
    public static String getRecordTextUri(@NonNull TetroidRecord record) {
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
    public void loadIcon(@NonNull TetroidNode node) {
        if (node.isNonCryptedOrDecrypted())
            node.loadIconFromStorage(storagePath + File.separator + ICONS_FOLDER);
    }

    /**
     * Получение содержимого записи в виде голого html.
     * @param record
     * @return
     */
    public static String getRecordHtmlTextDecrypted(@NonNull TetroidRecord record) {
        String path = getStoragePathBase() + File.separator
                + record.getDirName() + File.separator + record.getFileName();
//        String pathUri = null;
        Uri uri;
        try {
//            pathUri = "file://" + URLEncoder.encode(path, "UTF-8");
            uri = Uri.parse(path);
        } catch (Exception ex) {
            LogManager.addLog(context.getString(R.string.error_generate_record_file_path) + path, ex);
            return null;
        }
        String res = null;
        if (record.isCrypted()) {
            if (record.isDecrypted()) {
                byte[] text = new byte[0];
                try {
                    text = FileUtils.readFile(uri);
                } catch (Exception ex) {
                    LogManager.addLog(context.getString(R.string.error_read_record_file) + path, ex);
                }
                // расшифровываем содержимое файла
                res = CryptManager.decryptText(text);
                if (res == null) {
                    LogManager.addLog(context.getString(R.string.error_decrypt_record_file) + path,
                            LogManager.Types.ERROR);
                }
            }
        } else {
            try {
                res = FileUtils.readTextFile(uri);
            } catch (Exception ex) {
                LogManager.addLog(context.getString(R.string.error_read_record_file) + path, ex);
            }
        }
        return res;
    }

    public static boolean saveRecordHtmlText(@NonNull TetroidRecord record, String htmlText) {
        String path = getStoragePathBase() + File.separator
                + record.getDirName() + File.separator + record.getFileName();
        Uri uri;
        try {
            uri = Uri.parse(path);
        } catch (Exception ex) {
            LogManager.addLog("Ошибка формирования Uri пути к файлу" + path, ex);
            return false;
        }
        String resText = htmlText;
        if (record.isCrypted()) {
            resText = CryptManager.cryptText(htmlText);
//            if (record.isDecrypted()) {
//                byte[] text = new byte[0];
//                try {
//                    text = FileUtils.readFile(uri);
//                } catch (Exception ex) {
//                    LogManager.addLog(context.getString(R.string.error_read_record_file) + path, ex);
//                }
//                // расшифровываем содержимое файла
//                res = CryptManager.decryptText(text);
//                if (res == null) {
//                    LogManager.addLog(context.getString(R.string.error_decrypt_record_file) + path,
//                            LogManager.Types.ERROR);
//                }
//            }
        } /*else {
            try {
                res = FileUtils.readTextFile(uri);
            } catch (Exception ex) {
                LogManager.addLog(context.getString(R.string.error_read_record_file) + path, ex);
            }
        }*/



        return true;
    }

    /**
     * Получение содержимого записи в виде текста.
     * @param record
     * @return
     */
    public static String getRecordTextDecrypted(@NonNull TetroidRecord record) {
        String text = null;
        String html = getRecordHtmlTextDecrypted(record);
        if (html != null) {
            text = Jsoup.parse(html).text();
        }
        return text;
    }

    public static String getRecordDirUri(@NonNull TetroidRecord record) {
        return getStoragePathBaseUri() + File.separator + record.getDirName() + File.separator;
    }

    @NonNull
    private static Uri getStoragePathBaseUri() {
//        return "file://" + instance.storagePath + "/base/";
//        return Uri.fromFile(new File(getStoragePathBase()));
        return Uri.parse("file://" + getStoragePathBase());
    }

    @NonNull
    private static String getStoragePathBase() {
        return instance.storagePath + File.separator + BASE_FOLDER;
    }

    public static TetroidNode getNode(String id) {
        return getNodeInHierarchy(instance.rootNodesList, id);
    }

    public static TetroidRecord getRecord(String id) {
        return getRecordInHierarchy(instance.rootNodesList, id);
    }

    public static TetroidTag getTag(String tagName) {
        for (TetroidTag tag : getTags()) {
            if (tag.getName().contentEquals(tagName))
                return tag;
        }
        return null;
    }

    public static TetroidNode getNodeInHierarchy(List<TetroidNode> nodes, String id) {
        for (TetroidNode node : nodes) {
//            if (getNodeInHierarchy(node, id) != null)
//                return node;
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

    public static TetroidRecord getRecordInHierarchy(List<TetroidNode> nodes, String id) {
        for (TetroidNode node : nodes) {
            for (TetroidRecord record : node.getRecords()) {
                if (id.equals(record.getId()))
                    return record;
            }
            if (node.isExpandable()) {
                TetroidRecord found = getRecordInHierarchy(node.getSubNodes(), id);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /**
     * Открытие файла записи сторонным приложением.
     * @param context
     * @param file
     * @return
     */
    @RequiresPermission(WRITE_EXTERNAL_STORAGE)
    public static boolean openFile(Context context, @NonNull TetroidFile file) {
        TetroidRecord record = file.getRecord();
        String fileDisplayName = file.getName();
        String ext = FileUtils.getExtWithComma(fileDisplayName);
        String fileIdName = file.getId() + ext;
        String fullFileName = String.format("%s%s/%s", getStoragePathBase(), record.getDirName(), fileIdName);
        File srcFile;
        try {
            srcFile = new File(fullFileName);
        } catch (Exception ex) {
            LogManager.addLog(context.getString(R.string.error_file_open) + fullFileName,
                    LogManager.Types.ERROR, Toast.LENGTH_LONG);
            LogManager.addLog(ex);
            return false;
        }
        //
        LogManager.addLog(context.getString(R.string.open_file) + fullFileName);
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

//            Uri fileURI = Uri.fromFile(srcFile);
            // Начиная с API 24 (Android 7), для предоставления доступа к файлам, который
            // ассоциируется с приложением (для открытия файла другими приложениями с помощью Intent, короче),
            // нужно использовать механизм FileProvider.
            // Путь к файлу должен быть сформирован так: content://<Uri for a file>
            Uri fileURI;
            try {
                fileURI = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", srcFile);
            } catch (Exception ex) {
                LogManager.addLog(context.getString(R.string.file_sharing_error) + srcFile.getAbsolutePath(),
                        LogManager.Types.ERROR, Toast.LENGTH_LONG);
                LogManager.addLog(ex);
                return false;
            }
            // ?
            //grant permision for app with package "packegeName", eg. before starting other app via intent
//            context.grantUriPermission(context.getPackageName(), fileURI, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //revoke permisions
//            context.revokeUriPermission(fileURI, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            // определяем тип файла по расширению, если оно есть
            String mimeType = (!StringUtil.isBlank(ext) && ext.length() > 1)
                    ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.substring(1))
                    : "text/plain";
            intent.setDataAndType(fileURI, mimeType);
            // Add this flag if you're using an intent to make the system open your file.
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // всегда отображать диалог выбора приложения (не использовать выбор по-умолчанию)
            Intent chooser = Intent.createChooser(intent, context.getString(R.string.open_with));
            try {
                // проверить, есть ли подходящее приложение для открытия файла
                if (intent.resolveActivity(context.getPackageManager()) != null) {
//                    context.startActivity(intent);
                    context.startActivity(chooser);
                } else {
                    LogManager.addLog(context.getString(R.string.no_app_found) + fullFileName, Toast.LENGTH_LONG);
                    return false;
                }
            }
            catch(ActivityNotFoundException ex) {
                LogManager.addLog(context.getString(R.string.error_file_open) + fullFileName, Toast.LENGTH_LONG);
                return false;
            }
        } else {
            LogManager.addLog(context.getString(R.string.file_is_missing) + fullFileName, Toast.LENGTH_SHORT);
            return false;
        }
        return true;
    }

    public static boolean openFolder(Context context, String pathUri){
        Uri uri = Uri.parse(pathUri);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");
        if (intent.resolveActivityInfo(context.getPackageManager(), 0) != null) {
            context.startActivity(intent);
            return true;
        } else {
            LogManager.addLog(R.string.missing_file_manager, Toast.LENGTH_LONG);
        }
        return false;
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
    public static String getFileSize(Context context, @NonNull TetroidRecord record, @NonNull TetroidFile file) {
        String ext = FileUtils.getExtWithComma(file.getName());
        String fullFileName = String.format("%s%s/%s%s", getStoragePathBase(), record.getDirName(), file.getId(), ext);

        long size;
        try {
            size = new File(fullFileName).length() / 1024;
        } catch (SecurityException ex) {
            LogManager.addLog(context.getString(R.string.denied_read_file_access) + fullFileName, ex);
            return null;
        } catch (Exception ex) {
            LogManager.addLog(context.getString(R.string.get_file_size_error) + fullFileName, ex);
            return null;
        }
        if (size == 0) {
            return null;
        } else if (size >= 1024) {
            return (size / 1024) + context.getString(R.string.m_bytes);
        } else {
            return size + context.getString(R.string.k_bytes);
        }
    }

    public static List<TetroidNode> getRootNodes() {
        return instance.rootNodesList;
    }

    public static List<TetroidTag> getTags() {
        return instance.tagsList;
    }

    public static boolean isNodesExist() {
        return (instance.rootNodesList != null && !instance.rootNodesList.isEmpty());
    }

    public static String getStoragePath() {
        return instance.storagePath;
    }

    public static boolean isExistsCryptedNodes() {
        return instance.isExistCryptedNodes;
    }

    public static boolean isCrypted() {
        return (Integer.parseInt(databaseINI.get(INI_CRYPT_MODE)) == 1);
    }

    public static DataManager getInstance() {
        return instance;
    }
}
