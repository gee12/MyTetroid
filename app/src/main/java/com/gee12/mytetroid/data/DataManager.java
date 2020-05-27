package com.gee12.mytetroid.data;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import com.gee12.mytetroid.TetroidLog;
import com.gee12.mytetroid.crypt.CryptManager;
import com.gee12.mytetroid.crypt.Crypter;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidImage;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidObject;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;
import com.gee12.mytetroid.utils.FileUtils;
import com.gee12.mytetroid.utils.ImageUtils;
import com.gee12.mytetroid.utils.Utils;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class DataManager extends XMLManager implements IRecordFileCrypter {

    public interface ICallback {
        void run();
    }

    public static final String ID_SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyz";
    public static final String QUOTES_PARAM_STRING = "\"\"";

    public static final String SEPAR = File.separator;
    public static final int UNIQUE_ID_HALF_LENGTH = 10;
    public static final String PREFIX_DATE_TIME_FORMAT = "yyyyMMddHHmmssSSS";

    public static final String BASE_FOLDER_NAME = "base";
    public static final String ICONS_FOLDER_NAME = "icons";
    public static final String MYTETRA_XML_FILE_NAME = "mytetra.xml";
    public static final String DATABASE_INI_FILE_NAME = "database.ini";

    private static Context context;

    /**
     *
     */
    protected String storagePath;

    /**
     * Расшифровано ли в данный момент хранилище (временно).
     */
    protected boolean isDecrypted;

    /**
     *
     */
    protected static DatabaseConfig databaseConfig;

    /**
     *
     */
    protected static DataManager instance;

    /**
     * Загрузка параметров из файла database.ini и инициализация переменных.
     * @param storagePath
     * @return
     */
    public static boolean init(Context ctx, String storagePath) {
        context = ctx;
        DataManager.instance = new DataManager();
        DataManager.instance.storagePath = storagePath;
        DataManager.databaseConfig = new DatabaseConfig(storagePath + SEPAR + DATABASE_INI_FILE_NAME);
        boolean res;
        try {
            res = databaseConfig.load();
        } catch (Exception ex) {
            LogManager.addLog(ex);
            return false;
        }
        return res;
    }

    public static void initCryptPass(String pass, boolean isMiddleHash) {
        if (isMiddleHash) {
            CryptManager.initFromMiddleHash(pass, instance, instance);
        } else {
            CryptManager.initFromPass(pass, instance, instance);
        }
    }

    /**
     * Загрузка хранилища из файла mytetra.xml.
     * @param isDecrypt Расшифровывать ли ветки
     * @return
     */
    public static boolean readStorage(boolean isDecrypt) {
        boolean res = false;
        File file = new File(instance.storagePath + SEPAR + MYTETRA_XML_FILE_NAME);
        if (!file.exists()) {
            LogManager.addLog(context.getString(R.string.log_file_is_absent) + MYTETRA_XML_FILE_NAME, LogManager.Types.ERROR);
            return false;
        }
        try {
            FileInputStream fis = new FileInputStream(file);
            res = instance.parse(fis, isDecrypt);

//            if (BuildConfig.DEBUG) {
//                TestData.addNodes(mInstance.mRootNodesList, 100, 100);
//            }

        } catch (Exception ex) {
            LogManager.addLog(ex);
        }
        return res;
    }

    /**
     * Перешифровка хранилища (перед этим ветки должны быть расшифрованы).
     * @return
     */
    public static boolean reencryptStorage() {
        LogManager.addLog(R.string.log_start_storage_reencrypt);
        return CryptManager.encryptNodes(instance.mRootNodesList, true);
    }

    /**
     * Расшифровка хранилища (временная).
     * @return
     */
    public static boolean decryptStorage(boolean decryptFiles) {
        LogManager.addLog(R.string.log_start_storage_decrypt);
        boolean res = CryptManager.decryptNodes(instance.mRootNodesList, true, instance, false, decryptFiles);
        instance.isDecrypted = res;
        return res;
    }

    /**
     * Обработчик события о необходимости (временной) расшифровки ветки (вместе с дочерними объектами)
     * сразу после загрузки ветки из xml.
     * @param node
     */
    @Override
    protected boolean decryptNode(@NonNull TetroidNode node) {
        // isDecryptSubNodes = false, т.к. подветки еще не загружены из xml
        // и расшифровка каждой из них запустится сама при их загрузке по очереди в XMLManager
        return CryptManager.decryptNode(node, false, this, false, false);
    }

    /**
     * Расшифровка ветки с подветками (постоянная).
     * @param node
     * @return
     */
    public static boolean dropCryptNode(@NonNull TetroidNode node) {
        boolean res = CryptManager.decryptNode(node, true, instance, true, false);
        if (res) {
            return saveStorage();
        }
        return false;
    }

    public static String decryptField(TetroidObject obj, String field) {
        return (obj != null && obj.isCrypted()) ? CryptManager.decryptBase64(field) : field;
    }

    public static String decryptField(boolean isCrypted, String field) {
        return (isCrypted) ? CryptManager.decryptBase64(field) : field;
    }

    public static String encryptField(TetroidObject obj, String field) {
        return encryptField(obj != null && obj.isCrypted() && obj.isDecrypted(), field); // последняя проверка не обязательна
    }

    public static String encryptField(boolean isCrypted, String field) {
        return (isCrypted) ? CryptManager.encryptTextBase64(field) : field;
    }

    /**
     * Зашифровка (незашифрованной) ветки с подветками.
     * @param node
     * @return
     */
    public static boolean encryptNode(@NotNull TetroidNode node) {
        boolean res = CryptManager.encryptNode(node, false);
        if (res) {
            return saveStorage();
        }
        return false;
    }

    /**
     * Зашифровка или расшифровка файла при необходимости.
     * @param file
     * @param isCrypted
     * @param isEncrypt
     * @return
     */
    private static int cryptOrDecryptFile(File file, boolean isCrypted, boolean isEncrypt) {
        if (isCrypted && !isEncrypt) {
            try {
                // расшифровуем файл записи
                return (Crypter.decryptFile(file, file)) ? 1 : -1;
            } catch (Exception ex) {
                LogManager.addLog(context.getString(R.string.log_error_file_decrypt) + file.getAbsolutePath(), ex);
                return -1;
            }
        } else if (!isCrypted && isEncrypt) {
            try {
                // зашифровуем файл записи
                return (Crypter.encryptFile(file, file)) ? 1 : -1;
            } catch (Exception ex) {
                LogManager.addLog(context.getString(R.string.log_error_file_encrypt) + file.getAbsolutePath(), ex);
                return -1;
            }
        }
        return 0;
    }

    /**
     * Зашифровка или расшифровка файла записи и прикрепленных файлов при необходимости.
     * @param record
     * @param isEncrypt
     */
    @Override
    public boolean cryptRecordFiles(TetroidRecord record, boolean isCrypted, boolean isEncrypt) {
        // файл записи
        String recordFolderPath = getPathToRecordFolder(record);
        File file = new File(recordFolderPath, record.getFileName());
        if (cryptOrDecryptFile(file, isCrypted, isEncrypt) < 0) {
            return false;
        }
        // прикрепленные файлы
        if (record.getAttachedFilesCount() > 0) {
            for (TetroidFile attach : record.getAttachedFiles()) {
                file = new File(recordFolderPath, attach.getIdName());
                if (cryptOrDecryptFile(file, isCrypted, isEncrypt) < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Зашифровка или расшифровка файла записи и прикрепленных файлов при необходимости.
     * @param record
     * @param isEncrypt
     */
/*    @Override
    public boolean cryptRecordFiles(TetroidRecord record, boolean isEncrypt) {
        return cryptRecordFiles(record, record.isCrypted(), isEncrypt);
    }

    *//**
     * Перешифровка файла записи и прикрепленных файлов при необходимости.
     * Файлы уже должны быть расшифрованы.
     * @param record
     * @return
     *//*
    @Override
    public boolean reencryptRecordFiles(TetroidRecord record) {
        if (!record.isCrypted() || !record.isDecrypted())
            return false;
        return cryptRecordFiles(record, false, true);
    }*/

    /**
     * Получение пути к файлу с содержимым записи.
     * Если расшифрован, то в tempPath. Если не был зашифрован, то в storagePath.
     * @return
     */
    public static String getRecordTextUri(@NonNull TetroidRecord record) {
        if (record == null) {
            LogManager.emptyParams("DataManager.getRecordTextUri()");
            return null;
        }
        String path = null;
        if (record.isCrypted()) {
            if (record.isDecrypted()) {
                // расшифровываем файл и ложим в temp
//                path = SettingsManager.getTrashPath() + SEPAR + record.getDirName()
                path = getPathToRecordFolderInTrash(record) + SEPAR + record.getFileName();
            }
        } else {
            path = SettingsManager.getStoragePath() + SEPAR + BASE_FOLDER_NAME
                    + SEPAR + record.getDirName()
                    + SEPAR + record.getFileName();
        }
        /*String path = (isCrypted && isDecrypted)    // логическая ошибка в условии
                ? tempPath+dirName+"/"+fileName
                : storagePath+"/base/"+dirName+"/"+fileName;*/
//        File file = new File(storagePath+"/base/"+dirName+"/"+fileName);
//        return "file:///" + file.getAbsolutePath();
        return (path != null) ? "file:///" + path : null;
    }

    /**
     * Обработчик события, когда необходимо загрузить иконку ветки.
     * @param node
     */
    @Override
    public void loadIcon(@NonNull TetroidNode node) {
        if (node.isNonCryptedOrDecrypted()) {
            node.loadIcon(storagePath + SEPAR + ICONS_FOLDER_NAME);
        }
    }

    /**
     * Получение содержимого записи в виде "сырого" html.
     * @param record
     * @return
     */
    public static String getRecordHtmlTextDecrypted(@NonNull TetroidRecord record) {
        if (record == null) {
            LogManager.emptyParams("DataManager.getRecordHtmlTextDecrypted()");
            return null;
        }
        LogManager.addLog(context.getString(R.string.log_start_record_file_reading) + record.getId(), LogManager.Types.DEBUG);
        // проверка существования каталога записи
        String dirPath = getPathToRecordFolder(record);
        if (checkRecordFolder(dirPath, true, Toast.LENGTH_LONG) <= 0) {
            return null;
        }
        String path = dirPath + SEPAR + record.getFileName();
        Uri uri;
        try {
            uri = Uri.parse(path);
        } catch (Exception ex) {
            LogManager.addLog(context.getString(R.string.log_error_generate_record_file_path) + path, ex);
            return null;
        }
        // проверка существования файла записи
        File file = new File(uri.getPath());
        if (!file.exists()) {
            LogManager.addLog(context.getString(R.string.log_record_file_is_missing), LogManager.Types.WARNING, Toast.LENGTH_LONG);
            return null;
        }
        String res = null;
        if (record.isCrypted()) {
            if (record.isDecrypted()) {
                byte[] bytes;
                try {
                    bytes = FileUtils.readFile(uri);
                } catch (Exception ex) {
                    LogManager.addLog(context.getString(R.string.log_error_read_record_file) + path, ex);
                    return null;
                }
                if (bytes == null) {
                    LogManager.addLog(context.getString(R.string.log_error_decrypt_record_file) + path, LogManager.Types.ERROR);
                    return null;
                } else if (bytes.length == 0) {
                    // файл пуст
                    return "";
                }
                // расшифровываем содержимое файла
                LogManager.addLog(context.getString(R.string.log_start_record_text_decrypting), LogManager.Types.DEBUG);
                res = CryptManager.decryptText(bytes);
                if (res == null) {
                    LogManager.addLog(context.getString(R.string.log_error_decrypt_record_file) + path, LogManager.Types.ERROR);
                }
            }
        } else {
            try {
                res = FileUtils.readTextFile(uri);
            } catch (Exception ex) {
                LogManager.addLog(context.getString(R.string.log_error_read_record_file) + path, ex);
            }
        }
        return res;
    }

    /**
     * Сохранение содержимого записи в файл.
     * @param record
     * @param htmlText
     * @return
     */
    public static boolean saveRecordHtmlText(@NonNull TetroidRecord record, String htmlText) {
        if (record == null) {
            LogManager.emptyParams("DataManager.saveRecordHtmlText()");
            return false;
        }
        LogManager.addLog(context.getString(R.string.log_start_record_file_saving) + record.getId(), LogManager.Types.DEBUG);
        // проверка существования каталога записи
        String dirPath = getPathToRecordFolder(record);
        if (checkRecordFolder(dirPath, true, Toast.LENGTH_LONG) <= 0) {
            return false;
        }
        // формирование пути к файлу записи
        String path = dirPath + SEPAR + record.getFileName();
        Uri uri;
        try {
            uri = Uri.parse(path);
        } catch (Exception ex) {
            LogManager.addLog(context.getString(R.string.log_error_generate_record_file_path) + path, ex);
            return false;
        }
        // запись файла, зашифровуя при необходимости
        try {
            if (record.isCrypted()) {
                byte[] res = CryptManager.encryptTextBytes(htmlText);
                FileUtils.writeFile(uri, res);
            } else {
                FileUtils.writeFile(uri, htmlText);
            }
        } catch (IOException ex) {
            LogManager.addLog(context.getString(R.string.log_error_write_to_record_file) + path, ex);
            return false;
        }
        return true;
    }

    public static boolean shareText(Context context, String subject, String text) {
        if (context == null)
            return false;
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(android.content.Intent.EXTRA_TEXT, text);
//        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_using)));
        // всегда отображать диалог выбора приложения (не использовать выбор по-умолчанию)
        Intent chooser = Intent.createChooser(intent, context.getString(R.string.title_send_to));
        try {
            // проверить, есть ли подходящее приложение для открытия файла
            if (intent.resolveActivity(context.getPackageManager()) != null) {
//                    context.startActivity(intent);
                context.startActivity(chooser);
            } else {
                LogManager.addLog(context.getString(R.string.log_no_app_found_for_share_text), Toast.LENGTH_LONG);
                return false;
            }
        }
        catch (ActivityNotFoundException ex) {
            LogManager.addLog(context.getString(R.string.log_no_app_found_for_share_text), Toast.LENGTH_LONG);
            return false;
        }
        return true;
    }

    /**
     * Открытие прикрепленного файла сторонним приложением.
     * @param context
     * @param file
     * @return
     */
    @RequiresPermission(WRITE_EXTERNAL_STORAGE)
    public static boolean openAttach(Context context, @NonNull TetroidFile file) {
        if (context == null || file == null) {
            LogManager.emptyParams("DataManager.openAttach()");
            return false;
        }
        LogManager.addLog(context.getString(R.string.log_start_attach_file_opening) + file.getId(), LogManager.Types.DEBUG);
        TetroidRecord record = file.getRecord();
        String fileDisplayName = file.getName();
        String ext = FileUtils.getExtensionWithComma(fileDisplayName);
        String fileIdName = file.getId() + ext;
        String fullFileName = getPathToFileInRecordFolder(record, fileIdName);
        File srcFile;
        try {
            srcFile = new File(fullFileName);
        } catch (Exception ex) {
            LogManager.addLog(context.getString(R.string.log_error_file_open) + fullFileName,
                    LogManager.Types.ERROR, Toast.LENGTH_LONG);
            LogManager.addLog(ex);
            return false;
        }
        //
        LogManager.addLog(context.getString(R.string.log_open_file) + fullFileName);
        if (srcFile.exists()) {
            // если запись зашифрована
            if (record.isCrypted() && SettingsManager.isDecryptFilesInTemp()) {
                // создаем временный файл
//                File tempFile = createTempCacheFile(context, fileIdName);
//                File tempFile = new File(String.format("%s%s/_%s", getStoragePathBase(), record.getDirName(), fileIdName));
//                File tempFile = createTempExtStorageFile(context, fileIdName);
//                String tempFolderPath = SettingsManager.getTrashPath() + SEPAR + record.getDirName();
                String tempFolderPath = getPathToRecordFolderInTrash(record);
                File tempFolder = new File(tempFolderPath);
                if (!tempFolder.exists() && !tempFolder.mkdirs()) {
                    LogManager.addLog(context.getString(R.string.log_could_not_create_temp_dir) + tempFolderPath, Toast.LENGTH_LONG);
                }
                File tempFile = new File(tempFolder, fileIdName);
//                File tempFile = new File(getTrashPath()+File.separator, fileIdName);

                // расшифровываем во временный файл
                try {
                    if ((tempFile.exists() || tempFile.createNewFile()) && CryptManager.decryptFile(srcFile, tempFile)) {
                        srcFile = tempFile;
                    } else {
                        LogManager.addLog(context.getString(R.string.log_could_not_decrypt_file) + fullFileName, Toast.LENGTH_LONG);
                        return false;
                    }
                } catch (IOException ex) {
                    LogManager.addLog(context.getString(R.string.log_file_decryption_error) + ex.getMessage(), Toast.LENGTH_LONG);
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
                LogManager.addLog(context.getString(R.string.log_file_sharing_error) + srcFile.getAbsolutePath(),
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
            Intent chooser = Intent.createChooser(intent, context.getString(R.string.title_open_with));
            try {
                // проверить, есть ли подходящее приложение для открытия файла
                if (intent.resolveActivity(context.getPackageManager()) != null) {
//                    context.startActivity(intent);
                    context.startActivity(chooser);
                } else {
                    LogManager.addLog(context.getString(R.string.log_no_app_found_for_open_file) + fullFileName, Toast.LENGTH_LONG);
                    return false;
                }
            }
            catch (ActivityNotFoundException ex) {
                LogManager.addLog(context.getString(R.string.log_error_file_open) + fullFileName, Toast.LENGTH_LONG);
                return false;
            }
        } else {
            LogManager.addLog(context.getString(R.string.log_file_is_absent) + fullFileName, Toast.LENGTH_SHORT);
            return false;
        }
        return true;
    }

    /**
     * Открытие каталога записи.
     * @param context
     * @param record
     * @return
     */
    public static void openRecordFolder(Context context, @NotNull TetroidRecord record){
        if (context == null || record == null) {
            LogManager.emptyParams("DataManager.openRecordFolder()");
            return;
        }
        LogManager.addLog(context.getString(R.string.log_start_record_folder_opening) + record.getId(), LogManager.Types.DEBUG);
        Uri uri = Uri.parse(getRecordDirUri(record));
        if (!openFolder(context, uri)) {
            Utils.writeToClipboard(context, context.getString(R.string.title_record_folder_path), uri.getPath());
            LogManager.addLog(R.string.log_missing_file_manager, Toast.LENGTH_LONG);
        }
    }

    /**
     * Открытие каталога в файловом менеджере.
     * @param context
     * @param uri
     * @return
     */
    public static boolean openFolder(Context context, Uri uri) {
        if (context == null || uri == null) {
            LogManager.emptyParams("DataManager.openRecordFolder()");
            return false;
        }
        // ACTION_VIEW is not supported by most of the file managers, it's crashing.
        Intent intent = new Intent(Intent.ACTION_VIEW);

        // Of course it can show you the contents,
        // but when you will click on any file - activity will close and return you a link to selected file,
        // because that's the purpose of ACTION_GET_CONTENT.
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

//        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setDataAndType(uri, "resource/folder");
//        intent.setDataAndType(uri, "text/csv");
//        intent.setDataAndType(uri, "*/*");
//        Intent chooser = Intent.createChooser(intent, context.getString(R.string.title_open_folder));
//        if (intent.resolveActivityInfo(context.getPackageManager(), 0) != null) {
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            try {
                context.startActivity(intent);
    //            context.startActivity(chooser);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    /**
     * Проверка существования каталога записи и создание при его отсутствии.
     * @param dirPath
     * @return 1 - успешно
     *         0 - ошибка
     *         -1 - ошибка (отсутствует каталог записи)
     */
    public static int checkRecordFolder(String dirPath, boolean isCreate) {
        return checkRecordFolder(dirPath, isCreate, LogManager.DURATION_NONE);
    }

    public static int checkRecordFolder(String dirPath, boolean isCreate, int duration) {
        File folder = new File(dirPath);
        try {
            if (!folder.exists()) {
                if (isCreate) {
                    LogManager.addLog(String.format(Locale.getDefault(), context.getString(R.string.log_create_record_dir), dirPath),
                            LogManager.Types.WARNING);
                    if (folder.mkdirs()) {
//                        LogManager.addLog(context.getString(R.string.log_record_dir_created), LogManager.Types.DEBUG, duration);
                        TetroidLog.addOperResLog(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.CREATE, duration);
                        return 1;
                    } else {
                        LogManager.addLog(context.getString(R.string.log_create_record_dir_error), LogManager.Types.ERROR, duration);
                        return 0;
                    }
                }
                return -1;
            }
        } catch (Exception ex) {
            LogManager.addLog(context.getString(R.string.log_check_record_dir_error), LogManager.Types.ERROR, duration);
            return 0;
        }
        return 1;
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

    /**
     * Генерация уникального идентификатора для объектов хранилища.
     * @return
     */
    public static String createUniqueId() {
        StringBuilder sb = new StringBuilder();
        // 10 цифр количества (милли)секунд с эпохи UNIX
        String seconds = String.valueOf(System.currentTimeMillis());
        int length = seconds.length();
        if (length > UNIQUE_ID_HALF_LENGTH) {
            sb.append(seconds.substring(0, UNIQUE_ID_HALF_LENGTH));
        } else if (length < UNIQUE_ID_HALF_LENGTH) {
            sb.append(seconds);
            for (int i = 0; i < UNIQUE_ID_HALF_LENGTH - length; i++){
                sb.append('0');
            }
        }
        // 10 случайных символов
        Random rand = new Random();
        for (int i = 0; i < UNIQUE_ID_HALF_LENGTH; i++){
            int randIndex = Math.abs(rand.nextInt()) % ID_SYMBOLS.length();
            sb.append(ID_SYMBOLS.charAt(randIndex));
        }

        return sb.toString();
    }

    public static String createUniqueImageName() {
        return "image" + createUniqueId() + ".png";
    }

    public static String createDateTimePrefix() {
        return Utils.dateToString(new Date(), PREFIX_DATE_TIME_FORMAT);
    }

    public static byte[] createRandomBytes(int length) {
        byte[] res = new byte[length];
        Random rand = new Random();
        for (int i = 0; i < length; i++){
            res[i] = (byte) Math.abs(rand.nextInt() % 0xFF);
        }
        return res;
    }

    /**
     * Поменять местами 2 объекта хранилища в списке.
     * @param list
     * @param pos
     * @param isUp
     * @return 1 - успешно
     *         0 - перемещение невозможно (пограничный элемент)
     *        -1 - ошибка
     */
    public static int swapTetroidObjects(List list, int pos, boolean isUp) {
        if (list == null)
            return -1;
        boolean isSwapped = false;
        if (isUp) {
            if (pos > 0) {
                Collections.swap(list, pos-1, pos);
                isSwapped = true;
            }
        } else {
            if (pos < list.size() - 1) {
                Collections.swap(list, pos, pos+1);
                isSwapped = true;
            }
        }
        // перезаписываем файл структуры хранилища
        if (isSwapped) {
            return (saveStorage()) ? 1 : -1;
        }
        return 0;
    }

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
//        LogManager.addLog(context.getString(R.string.log_start_node_creating), LogManager.Types.DEBUG);
        TetroidLog.addOperStartLog(TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE);

        // генерируем уникальные идентификаторы
        String id = createUniqueId();

        boolean crypted = (parentNode != null && parentNode.isCrypted());
        int level = (parentNode != null) ? parentNode.getLevel() + 1 : 0;
//        TetroidNode node = new TetroidNode(crypted, id, name, null, level);
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
//            LogManager.addLog(context.getString(R.string.log_cancel_node_creating), LogManager.Types.ERROR);
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
//        LogManager.addLog(context.getString(R.string.log_start_node_fields_editing), LogManager.Types.DEBUG);
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
//            LogManager.addLog(context.getString(R.string.log_cancel_node_changing), LogManager.Types.ERROR);
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
                if (cloneRecordToNode(srcRecord, node, isCutted, breakOnFSErrors) == null && breakOnFSErrors) {
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
     * Перемещение или копирование записи в ветку.
     * @param srcRecord
     * @param node
     * @param isCutted
     * @return
     */
    private static TetroidRecord cloneRecordToNode(TetroidRecord srcRecord, TetroidNode node,
                                                   boolean isCutted, boolean breakOnFSErrors) {
        if (srcRecord == null)
            return null;
        TetroidLog.addOperStartLog(TetroidLog.Objs.RECORD, TetroidLog.Opers.INSERT);

        // генерируем уникальные идентификаторы, если запись копируется
        String id = (isCutted) ? srcRecord.getId() : createUniqueId();
        String dirName = (isCutted) ? srcRecord.getDirName() : createUniqueId();
        String name = srcRecord.getName();
        String tagsString = srcRecord.getTagsString();
        String author = srcRecord.getAuthor();
        String url = srcRecord.getUrl();

        // создаем копию записи
        boolean crypted = node.isCrypted();
        TetroidRecord record = new TetroidRecord(crypted, id,
                encryptField(crypted, name),
                encryptField(crypted, tagsString),
                encryptField(crypted, author),
                encryptField(crypted, url),
                srcRecord.getCreated(), dirName, srcRecord.getFileName(), node);
        if (crypted) {
            record.setDecryptedValues(name, tagsString, author, url);
            record.setDecrypted(true);
        }
        // добавляем прикрепленные файлы в запись
        cloneAttachesToRecord(srcRecord, record, isCutted);
        record.setIsNew(false);
        // добавляем запись в ветку (и соответственно, в коллекцию)
        node.addRecord(record);
        // добавляем метки в запись и в коллекцию
        instance.parseRecordTags(record, tagsString);

        TetroidRecord errorRes = (breakOnFSErrors) ? null : record;
        String srcDirPath = null;
        File srcDir = null;
        // проверяем существование каталога записи
        if (isCutted) {
            srcDirPath = getPathToRecordFolderInTrash(srcRecord);
        } else {
            srcDirPath = getPathToRecordFolder(srcRecord);
        }
        int dirRes = checkRecordFolder(srcDirPath, false);
        if (dirRes > 0) {
            srcDir = new File(srcDirPath);
        } else {
            return errorRes;
        }

        String destDirPath = getPathToRecordFolder(record);
        File destDir = new File(destDirPath);
        if (isCutted) {
            // вырезаем уникальную приставку в имени каталога
            String dirNameInBase = srcRecord.getDirName().substring(PREFIX_DATE_TIME_FORMAT.length() + 1);
            // перемещаем каталог записи
            int res = moveRecordFolder(record, srcDirPath, getStoragePathBase(), dirNameInBase);
            if (res < 0) {
                return errorRes;
            }
        } else {
            // копируем каталог записи
            try {
                if (FileUtils.copyDirRecursive(srcDir, destDir)) {
                    LogManager.addLog(String.format(context.getString(R.string.log_copy_record_dir_mask),
                            destDirPath), LogManager.Types.ERROR);
                    // переименовываем прикрепленные файлы
                    renameRecordAttaches(srcRecord, record);
                } else {
                    LogManager.addLog(String.format(context.getString(R.string.log_error_copy_record_dir_mask),
                            srcDirPath, destDirPath), LogManager.Types.ERROR);
                    return errorRes;
                }
            } catch (IOException ex) {
                LogManager.addLog(String.format(context.getString(R.string.log_error_copy_record_dir_mask),
                        srcDirPath, destDirPath), ex);
                return errorRes;
            }
        }

        // зашифровываем или расшифровываем файл записи
//        File recordFile = new File(getPathToFileInRecordFolder(record, record.getFileName()));
//        if (!cryptOrDecryptFile(recordFile, srcRecord.isCrypted(), crypted) && breakOnFSErrors) {
        if (!instance.cryptRecordFiles(record, srcRecord.isCrypted(), crypted) && breakOnFSErrors) {
            return errorRes;
        }

        return record;
    }

    /**
     * Перемещение или копирование прикрепленных файлов в другую запись.
     * @param srcRecord
     * @param destRecord
     * @param isCutted
     */
    private static void cloneAttachesToRecord(TetroidRecord srcRecord, TetroidRecord destRecord, boolean isCutted) {
        if (srcRecord.getAttachedFilesCount() > 0) {
            boolean crypted = destRecord.isCrypted();
            List<TetroidFile> attaches = new ArrayList<>();
            for (TetroidFile srcAttach : srcRecord.getAttachedFiles()) {
                // генерируем уникальные идентификаторы, если запись копируется
                String id = (isCutted) ? srcAttach.getId() : createUniqueId();
                String name = srcAttach.getName();
                TetroidFile attach = new TetroidFile(crypted, id,
                        encryptField(crypted, name), srcAttach.getFileType(), destRecord);
                if (crypted) {
                    attach.setDecryptedName(name);
                    attach.setIsCrypted(true);
                    attach.setDecrypted(true);
                }
                attaches.add(attach);
            }
            destRecord.setAttachedFiles(attaches);
        }
    }

    /**
     * Переименование скопированных прикрепленных файлов в каталоге записи.
     * @param srcRecord
     * @param destRecord
     */
    private static void renameRecordAttaches(TetroidRecord srcRecord, TetroidRecord destRecord) {
        for (int i = 0; i < srcRecord.getAttachedFilesCount(); i++) {
            TetroidFile srcAttach = srcRecord.getAttachedFiles().get(i);
            TetroidFile destAttach = destRecord.getAttachedFiles().get(i);

            String srcFileDisplayName = srcAttach.getName();
            String ext = FileUtils.getExtensionWithComma(srcFileDisplayName);
            String srcFileIdName = srcAttach.getId() + ext;
            String destFileIdName = destAttach.getId() + ext;
            // переименовываем
            String destPath = getPathToRecordFolder(destRecord);
            File srcFile = new File(destPath, srcFileIdName);
            File destFile = new File(destPath, destFileIdName);
            if (srcFile.renameTo(destFile)) {
                LogManager.addLog(String.format(context.getString(R.string.log_rename_file_mask),
                        destFile.getAbsolutePath()), LogManager.Types.DEBUG);
            } else {
                LogManager.addLog(String.format(context.getString(R.string.log_rename_file_error_mask),
                        srcFile.getAbsolutePath(), destFile.getName()), LogManager.Types.ERROR);
            }
        }
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
            LogManager.addLog(context.getString(R.string.log_not_found_node_id) + node.getId(), LogManager.Types.ERROR);
            return false;
        }

        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
            /*// TODO: необходим обход всего дерева веток для пересчета следующих счетчиков:
            instance.mMaxSubnodesCount = -1;
            instance.mMaxDepthLevel = -1;
            instance.mUniqueTagsCount = -1;*/
            // удаление всех объектов ветки рекурсивно
            instance.deleteNodeRecursively(node, movePath, false);
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
    private boolean deleteNodeRecursively(TetroidNode node, String movePath, boolean breakOnFSErrors) {
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
                deleteRecordTags(record);
//                deleteRecordFolder(record);
                // проверяем существование каталога
                String dirPath = getPathToRecordFolder(record);
                if (checkRecordFolder(dirPath, false) <= 0) {
                    if (breakOnFSErrors) {
                        return false;
                    } else {
                        continue;
                    }
                }
                // перемещаем каталог
                if (moveOrDeleteRecordFolder(record, dirPath, movePath) <= 0 && breakOnFSErrors) {
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
     * Удаление каталога записи.
     * @param record
     * @return
     */
    public boolean deleteRecordFolder(TetroidRecord record) {
        TetroidLog.addOperStartLog(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE);
        // проверяем существование каталога
        String dirPath = getPathToRecordFolder(record);
        if (checkRecordFolder(dirPath, false) <= 0) {
            return false;
        }
        File folder = new File(dirPath);
        // удаляем каталог
        if (!FileUtils.deleteRecursive(folder)) {
            LogManager.addLog(context.getString(R.string.log_error_del_record_dir) + dirPath, LogManager.Types.ERROR);
            return false;
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
     * Создание записи (пустую без текста):
     * 1) создание объекта в памяти
     * 2) создание каталога
     * 3) добавление в структуру mytetra.xml
     * @param name
     * @param tagsString
     * @param author
     * @param url
     * @param node
     * @return
     */
    public static TetroidRecord createRecord(String name, String tagsString, String author, String url, TetroidNode node) {
        if (node == null || TextUtils.isEmpty(name)) {
            LogManager.emptyParams("DataManager.createRecord()");
            return null;
        }
//        LogManager.addLog(context.getString(R.string.log_start_record_creating), LogManager.Types.DEBUG);
        TetroidLog.addOperStartLog(TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE);

        // генерируем уникальные идентификаторы
        String id = createUniqueId();
        String dirName = createUniqueId();

        boolean crypted = node.isCrypted();
        TetroidRecord record = new TetroidRecord(crypted, id,
                encryptField(crypted, name),
                encryptField(crypted, tagsString),
                encryptField(crypted, author),
                encryptField(crypted, url),
                new Date(), dirName, TetroidRecord.DEF_FILE_NAME, node);
        if (crypted) {
            record.setDecryptedValues(name, tagsString, author, url);
            record.setDecrypted(true);
        }
//        record.setDecryptedName(name);
//        record.setDecryptedTagsString(tagsString);
//        record.setDecryptedAuthor(author);
//        record.setDecryptedUrl(url);
        record.setIsNew(true);
//        if (crypted) {
//            record.setDecrypted(true);
//        }
        // создаем каталог записи
        String dirPath = getPathToRecordFolder(record);
        if (checkRecordFolder(dirPath, true) <= 0) {
            return null;
        }
        File dir = new File(dirPath);
        // создаем файл записи (пустой)
        String filePath = dirPath + SEPAR + record.getFileName();
        Uri fileUri;
        try {
            fileUri = Uri.parse(filePath);
        } catch (Exception ex) {
            LogManager.addLog(context.getString(R.string.log_error_generate_record_file_path) + filePath, ex);
            return null;
        }
        File file = new File(fileUri.getPath());
        try {
            file.createNewFile();
        } catch (IOException ex) {
            LogManager.addLog(context.getString(R.string.log_error_creating_record_file) + filePath, ex);
            return null;
        }

        // добавляем запись в ветку (и соответственно, в коллекцию)
        node.addRecord(record);
        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
            // добавляем метки в запись и в коллекцию
            instance.parseRecordTags(record, tagsString);
/*            // обновляем счетчики
            instance.mRecordsCount++;
            if (crypted) {
                instance.mCryptedRecordsCount++;
            }
            if (!StringUtil.isBlank(author)) {
                instance.mAuthorsCount++;
            }*/

        } else {
//            LogManager.addLog(context.getString(R.string.log_cancel_record_creating), LogManager.Types.ERROR);
            TetroidLog.addOperCancelLog(TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE);
            // удаляем запись из ветки
            node.getRecords().remove(record);
            // удаляем файл записи
            file.delete();
            // удаляем каталог записи (пустой)
            dir.delete();
            return null;
        }
        return record;
    }


    /**
     * Создание записи при приеме внешнего Intent.
     * @param name
     * @param url
     * @return
     */
    public static TetroidRecord createRecord(String name, String url, String text) {
        TetroidLog.addOperStartLog(TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE);

        if (TextUtils.isEmpty(name)) {
            name = Utils.dateToString(new Date(), "yyyy.MM.dd HH:mm:ss");
        }

        if (instance.mRootNodesList.isEmpty()) {
            LogManager.addLog("В хранилище не загружено ни одной ветки", LogManager.Types.ERROR);
            return null;
        }

        // TODO: пока что выбираем просто первую ветку в хранилище
        TetroidNode node = instance.mRootNodesList.get(0);

        TetroidRecord record = createRecord(name, null, null, url, node);
        if (record == null) {
            TetroidLog.addOperErrorLog(TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE, -1);
            return null;
        }

        if (saveRecordHtmlText(record, text)) {
            record.setIsNew(false);
        } else {
            TetroidLog.addOperErrorLog(TetroidLog.Objs.RECORD, TetroidLog.Opers.SAVE, -1);
            return null;
        }

        return record;
    }

    /**
     * Изменение свойств записи.
     * @param record
     * @param name
     * @param tagsString
     * @param author
     * @param url
     * @return
     */
    public static boolean editRecordFields(TetroidRecord record, String name, String tagsString, String author, String url) {
        if (record == null || TextUtils.isEmpty(name)) {
            LogManager.emptyParams("DataManager.editRecordFields()");
            return false;
        }
        TetroidLog.addOperStartLog(TetroidLog.Objs.RECORD_FIELDS, TetroidLog.Opers.CHANGE);

        String oldName = record.getName(true);
        String oldAuthor = record.getAuthor(true);
        String oldTagsString = record.getTagsString(true);
        String oldUrl = record.getUrl(true);
        // обновляем поля
        boolean crypted = record.isCrypted();
        record.setName(encryptField(crypted, name));
        record.setTagsString(encryptField(crypted, tagsString));
        record.setAuthor(encryptField(crypted, author));
        record.setUrl(encryptField(crypted, url));
        if (crypted) {
            record.setDecryptedValues(name, tagsString, author, url);
        }

        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
            if (oldTagsString == null && tagsString != null
                    || oldTagsString != null && !oldTagsString.equals(tagsString)) {
                // удаляем старые метки
                instance.deleteRecordTags(record);
                // добавляем новые метки
                instance.parseRecordTags(record, tagsString);
            }
        } else {
            TetroidLog.addOperCancelLog(TetroidLog.Objs.RECORD_FIELDS, TetroidLog.Opers.CHANGE);
            // возвращаем изменения
            record.setName(oldName);
            record.setTagsString(oldTagsString);
            record.setAuthor(oldAuthor);
            record.setUrl(oldUrl);
            if (crypted) {
                record.setDecryptedValues(decryptField(crypted, oldName),
                        decryptField(crypted, oldTagsString),
                        decryptField(crypted, oldAuthor),
                        decryptField(crypted, url));
            }
            return false;
        }
        return true;
    }

    /**
     * Удаление записи.
     * @param record
     * @param withoutDir Не пытаться удалить каталог записи
     * @return 1 - успешно
     *         0 - ошибка
     *         -1 - ошибка (отсутствует каталог записи)
     */
    public static int deleteRecord(TetroidRecord record, boolean withoutDir) {
        return deleteRecord(record, withoutDir, SettingsManager.getTrashPath(), false);
    }

    /**
     * Вырезание записи из ветки (добавление в "буфер обмена" и удаление).
     * @param record
     * @return 1 - успешно
     *         0 - ошибка
     *         -1 - ошибка (отсутствует каталог записи)
     */
    public static int cutRecord(TetroidRecord record, boolean withoutDir) {
        return deleteRecord(record, withoutDir, SettingsManager.getTrashPath(), true);
    }

    /**
     * Вставка записи в указанную ветку.
     * @param srcRecord
     * @param isCutted Если true, то запись была вырезана. Иначе - скопирована
     * @param node
     * @param withoutDir Не пытаться восстановить каталог записи
     * @return
     */
    public static int insertRecord(TetroidRecord srcRecord, boolean isCutted, TetroidNode node, boolean withoutDir) {
        if (srcRecord == null || node == null) {
            LogManager.emptyParams("DataManager.insertRecord()");
            return 0;
        }
        TetroidLog.addOperStartLog(TetroidLog.Objs.RECORD, TetroidLog.Opers.INSERT);

        String srcDirPath = null;
        File srcDir = null;
        // проверяем существование каталога записи
        if (!withoutDir) {
            srcDirPath = (isCutted) ? getPathToRecordFolderInTrash(srcRecord) : getPathToRecordFolder(srcRecord);
            int dirRes = checkRecordFolder(srcDirPath, false);
            if (dirRes > 0) {
                srcDir = new File(srcDirPath);
            } else {
                return dirRes;
            }
        }

        // генерируем уникальные идентификаторы, если запись копируется
        String id = (isCutted) ? srcRecord.getId() : createUniqueId();
        String dirName = (isCutted) ? srcRecord.getDirName() : createUniqueId();
        String name = srcRecord.getName();
        String tagsString = srcRecord.getTagsString();
        String author = srcRecord.getAuthor();
        String url = srcRecord.getUrl();

        // создаем копию записи
        boolean crypted = node.isCrypted();
        TetroidRecord record = new TetroidRecord(crypted, id,
                encryptField(crypted, name),
                encryptField(crypted, tagsString),
                encryptField(crypted, author),
                encryptField(crypted, url),
                srcRecord.getCreated(), dirName, srcRecord.getFileName(), node);
        if (crypted) {
            record.setDecryptedValues(name, tagsString, author, url);
            record.setDecrypted(true);
        }
        // прикрепленные файлы
        cloneAttachesToRecord(srcRecord, record, isCutted);
        record.setIsNew(false);

        String destDirPath = getPathToRecordFolder(record);
        File destDir = new File(destDirPath);
        if (!withoutDir) {
            if (isCutted) {
                // вырезаем уникальную приставку в имени каталога
                String dirNameInBase = srcRecord.getDirName().substring(PREFIX_DATE_TIME_FORMAT.length() + 1);
                // перемещаем каталог записи
                int res = moveRecordFolder(record, srcDirPath, getStoragePathBase(), dirNameInBase);
                if (res < 0) {
                    return res;
                }
            } else {
                // копируем каталог записи
                try {
                    if (FileUtils.copyDirRecursive(srcDir, destDir)) {
//                    TetroidLog.addOperResLog(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.COPY);
                        LogManager.addLog(String.format(context.getString(R.string.log_copy_record_dir_mask),
                                destDirPath), LogManager.Types.DEBUG);
                        // переименовываем прикрепленные файлы
                        renameRecordAttaches(srcRecord, record);
                    } else {
                        LogManager.addLog(String.format(context.getString(R.string.log_error_copy_record_dir_mask),
                                srcDirPath, destDirPath), LogManager.Types.ERROR);
                        return -2;
                    }
                } catch (IOException ex) {
                    LogManager.addLog(String.format(context.getString(R.string.log_error_copy_record_dir_mask),
                            srcDirPath, destDirPath), ex);
                    return -2;
                }
            }
        }

        // добавляем запись в ветку (и соответственно, в коллекцию)
        node.addRecord(record);
        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
            // добавляем метки в запись и в коллекцию
            instance.parseRecordTags(record, tagsString);

            if (!withoutDir) {
                // зашифровываем или расшифровываем файл записи и прикрепленные файлы
                // FIXME: обрабатывать результат ?
//                File recordFile = new File(getPathToFileInRecordFolder(record, record.getFileName()));
//                cryptOrDecryptFile(recordFile, srcRecord.isCrypted(), crypted);
                instance.cryptRecordFiles(record, srcRecord.isCrypted(), crypted);
            }
        } else {
            TetroidLog.addOperCancelLog(TetroidLog.Objs.RECORD, TetroidLog.Opers.INSERT);
            // удаляем запись из ветки
            node.getRecords().remove(record);

            if (!withoutDir) {
                if (isCutted) {
                    // перемещаем каталог записи обратно в корзину
                    return moveRecordFolder(record, destDirPath, SettingsManager.getTrashPath(), srcRecord.getDirName());
                } else {
                    // удаляем только что скопированный каталог записи
                    if (FileUtils.deleteRecursive(destDir)) {
                        TetroidLog.addOperResLog(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE);
                    } else {
                        LogManager.addLog(context.getString(R.string.log_error_del_record_dir) + destDirPath, LogManager.Types.ERROR);
                        return 0;
                    }
                }
            }
            return 0;
        }
        // сохраняем объект для вставки в список;
        // нельзя было положить вместо srcRecord, т.к. он нужен, если потребуется
        // еще раз вставить скопированную запись
//        clipboard.setObjForInsert(record);
        return 1;
    }

    /**
     * Удаление/вырезание записи из ветки.
     * @param record
     * @param withoutDir Нужно ли пропустить работу с каталогом записи
     * @param movePath Путь к каталогу, куда следует переместить каталог записи (не обязательно)
     * @param isCutting Если true, то запись вырезается, иначе - удаляется
     * @return 1 - успешно
     *         0 - ошибка
     *         -1 - ошибка (отсутствует каталог записи)
     *         -2 - ошибка (не удалось переместить каталог записи)
     */
    private static int deleteRecord(TetroidRecord record, boolean withoutDir, String movePath, boolean isCutting) {
        if (record == null) {
            LogManager.emptyParams("DataManager.deleteRecord()");
            return 0;
        }
        TetroidLog.addOperStartLog(TetroidLog.Objs.RECORD, (isCutting) ? TetroidLog.Opers.CUT : TetroidLog.Opers.DELETE);

        String dirPath = null;
//        File dir = null;
        // проверяем существование каталога записи
        if (!withoutDir) {
            dirPath = getPathToRecordFolder(record);
            int dirRes = checkRecordFolder(dirPath, false);
//            if (dirRes > 0) {
//                dir = new File(dirPath);
//            } else {
//                return dirRes;
//            }
            if (dirRes <= 0) {
                return dirRes;
            }
        }

        // удаляем запись из ветки (и соответственно, из коллекции)
        TetroidNode node = record.getNode();
        if (node != null) {
            if (!node.getRecords().remove(record)) {
                LogManager.addLog(context.getString(R.string.log_not_found_record_in_node), LogManager.Types.ERROR);
                return 0;
            }
        } else {
            LogManager.addLog(context.getString(R.string.log_record_not_have_node), LogManager.Types.ERROR);
            return 0;
        }

        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
            /*instance.mRecordsCount--;
            if (isCrypted())
                instance.mCryptedRecordsCount--;
            if (!StringUtil.isBlank(record.getAuthor()))
                instance.mAuthorsCount--;
            if (record.getAttachedFilesCount() > 0)
                instance.mFilesCount -= record.getAttachedFilesCount();*/
            // перезагружаем список меток
            instance.deleteRecordTags(record);
        } else {
//            LogManager.addLog(context.getString(R.string.log_cancel_record_deleting), LogManager.Types.ERROR);
            TetroidLog.addOperCancelLog(TetroidLog.Objs.RECORD, TetroidLog.Opers.DELETE);
            return 0;
        }

        if (!withoutDir) {
            int res = moveOrDeleteRecordFolder(record, dirPath, movePath);
            if (res <= 0) {
                return res;
            }
        }
        return 1;
    }

    private static int moveOrDeleteRecordFolder(TetroidRecord record, String dirPath, String movePath) {
        if (record == null || dirPath == null)
            return 0;
        if (movePath == null) {
            // удаляем каталог записи
            File dir = new File(dirPath);
            if (FileUtils.deleteRecursive(dir)) {
                TetroidLog.addOperResLog(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE);
                return 1;
            } else {
                LogManager.addLog(context.getString(R.string.log_error_del_record_dir) + dirPath, LogManager.Types.ERROR);
                return 0;
            }
        } else {
            // перемещаем каталог записи в корзину
            // с добавлением префикса в виде текущей даты и времени
            String newDirName = createDateTimePrefix() + "_" + record.getDirName();
            return moveRecordFolder(record, dirPath, movePath, newDirName);
        }
    }

    /**
     * Премещение каталога записи в указанный каталог.
     * @param record
     * @param srcPath Исходный каталог записи
     * @param destPath Каталог назначения
     * @param newDirName Если не null - новое имя каталога записи
     * @return
     */
    private static int moveRecordFolder(TetroidRecord record, String srcPath, String destPath, String newDirName) {
        if (record == null) {
            return 0;
        }
        int res = moveFile(srcPath, destPath, newDirName);
        if (res > 0 && newDirName != null) {
            // обновляем имя каталога для дальнейшей вставки
            record.setDirName(newDirName);
        }
        return res;
        /*File srcDir = new File(srcPath);
        File destDir = new File(destPath);
        // перемещаем каталог записи
        if (!FileUtils.moveToDirRecursive(srcDir, destDir)) {
            LogManager.addLog(String.format(context.getString(R.string.log_error_move_record_dir_mask),
                    srcPath, destPath), LogManager.Types.ERROR);
            return -2;
        }

        if (newDirName == null) {
            String destDirPath = destDir.getAbsolutePath() + File.separator + record.getDirName();
            LogManager.addLog(String.format(context.getString(R.string.log_record_folder_moved_mask),
                    destDirPath), LogManager.Types.DEBUG);
        } else {
            // добавляем к имени каталога записи уникальную приставку
            srcDir = new File(destPath, record.getDirName());
            destDir = new File(destPath, newDirName);
            if (srcDir.renameTo(destDir)) {
                LogManager.addLog(String.format(context.getString(R.string.log_record_folder_moved_mask),
                        destDir.getAbsolutePath()), LogManager.Types.DEBUG);
                // обновляем имя каталога для дальнейшей вставки
                record.setDirName(newDirName);
            } else {
                LogManager.addLog(String.format(context.getString(R.string.log_error_move_record_dir_mask),
                        srcDir.getAbsolutePath(), destDir.getAbsolutePath()), LogManager.Types.ERROR);
                return -2;
            }
        }
        return 1;*/
    }

    /**
     * Прикрепление нового файла к записи.
     * @param fullName
     * @param record
     * @return
     */
    public static TetroidFile attachFile(String fullName, TetroidRecord record) {
        if (record == null || TextUtils.isEmpty(fullName)) {
            LogManager.emptyParams("DataManager.attachFile()");
            return null;
        }
        TetroidLog.addOperStartLog(TetroidLog.Objs.FILE, TetroidLog.Opers.ATTACH);

        String id = createUniqueId();
        // проверка исходного файла
        File srcFile = new File(fullName);
        try {
            if (!srcFile.exists()) {
                LogManager.addLog(context.getString(R.string.log_file_is_absent) + fullName, LogManager.Types.ERROR);
                return null;
            }
        } catch (Exception ex) {
            LogManager.addLog(context.getString(R.string.log_file_checking_error) + fullName, ex);
            return null;
        }

        String fileDisplayName = srcFile.getName();
        String ext = FileUtils.getExtensionWithComma(fileDisplayName);
        String fileIdName = id + ext;
        // создание объекта хранилища
        boolean crypted = record.isCrypted();
        TetroidFile file = new TetroidFile(crypted, id,
                encryptField(crypted, fileDisplayName),
                TetroidFile.DEF_FILE_TYPE, record);
        if (crypted) {
            file.setDecryptedName(fileDisplayName);
            file.setDecrypted(true);
        }
        // проверка каталога записи
        String dirPath = getPathToRecordFolder(record);
        if (checkRecordFolder(dirPath, true, Toast.LENGTH_LONG) <= 0) {
            return null;
        }
        // формируем путь к файлу назначения в каталоге записи
        String destFilePath = dirPath + SEPAR + fileIdName;
        Uri destFileUri;
        try {
            destFileUri = Uri.parse(destFilePath);
        } catch (Exception ex) {
            LogManager.addLog(context.getString(R.string.log_error_generate_file_path) + destFilePath, ex);
            return null;
        }
        // копирование файла в каталог записи, зашифровуя при необходимости
        File destFile = new File(destFileUri.getPath());
        try {
            if (record.isCrypted()) {
                if (!CryptManager.encryptFile(srcFile, destFile)) {
                    LogManager.addLog(String.format(Locale.getDefault(),
                            context.getString(R.string.log_error_encrypt_file), fullName, destFilePath), LogManager.Types.ERROR);
                    return null;
                }
            } else {
                if (!FileUtils.copyFile(srcFile, destFile)) {
                    LogManager.addLog(String.format(Locale.getDefault(),
                            context.getString(R.string.log_error_copy_file), fullName, destFilePath), LogManager.Types.ERROR);
                    return null;
                }
            }
        } catch (IOException ex) {
            LogManager.addLog(String.format(Locale.getDefault(),
                    context.getString(R.string.log_error_copy_file), fullName, destFilePath), ex);
            return null;
        }

        // добавляем файл к записи (и соответственно, в коллекцию)
        List<TetroidFile> files = record.getAttachedFiles();
        if (files == null) {
            files = new ArrayList<>();
            record.setAttachedFiles(files);
        }
        files.add(file);
        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
         /*   instance.mFilesCount++;*/
        } else {
            TetroidLog.addOperCancelLog(TetroidLog.Objs.FILE, TetroidLog.Opers.ATTACH);
            // удаляем файл из записи
            files.remove(file);
            // удаляем файл
            destFile.delete();
            return null;
        }
        return file;
    }

    /**
     * Изменение свойств прикрепленного файла.
     * Проверка существования каталога записи и файла происходит только
     * если у имени файла было изменено расширение.
     * @param file
     * @param name
     * @return 1 - успешно
     *         0 - ошибка
     *         -1 - ошибка (отсутствует каталог записи)
     *         -2 - ошибка (отсутствует файл в каталоге записи)
     */
    public static int editAttachedFileFields(TetroidFile file, String name) {
        if (file == null || TextUtils.isEmpty(name)) {
            LogManager.emptyParams("DataManager.editAttachedFileFields()");
            return 0;
        }
        TetroidLog.addOperStartLog(TetroidLog.Objs.FILE_FIELDS, TetroidLog.Opers.CHANGE);

        TetroidRecord record = file.getRecord();
        if (record == null) {
            LogManager.addLog(context.getString(R.string.log_file_record_is_null), LogManager.Types.ERROR);
            return 0;
        }
        // сравниваем расширения
        String ext = FileUtils.getExtensionWithComma(file.getName());
        String newExt = FileUtils.getExtensionWithComma(name);
        boolean isExtChanged = !Utils.isEquals(ext, newExt, true);

        String dirPath = null;
        String filePath = null;
        File srcFile = null;
        if (isExtChanged) {
            // проверяем существование каталога записи
            dirPath = getPathToRecordFolder(record);
            int dirRes = checkRecordFolder(dirPath, false);
            if (dirRes <= 0) {
                return dirRes;
            }
            // проверяем существование самого файла
            String fileIdName = file.getId() + ext;
            filePath = dirPath + SEPAR + fileIdName;
            srcFile = new File(filePath);
            if (!srcFile.exists()) {
                LogManager.addLog(context.getString(R.string.log_attach_file_is_missing) + filePath, LogManager.Types.ERROR);
                return -2;
            }
        }

        String oldName = file.getName(true);
        // обновляем поля
        boolean crypted = file.isCrypted();
        file.setName(encryptField(crypted, name));
//        file.setDecryptedName(name);
        if (crypted) {
            file.setDecryptedName(name);
        }

        // перезаписываем структуру хранилища в файл
        if (!saveStorage()) {
            TetroidLog.addOperCancelLog(TetroidLog.Objs.FILE_FIELDS, TetroidLog.Opers.CHANGE);
            // возвращаем изменения
            file.setName(oldName);
            if (crypted) {
                file.setDecryptedName(decryptField(crypted, oldName));
            }
            return 0;
        }
        // меняем расширение, если изменилось
        if (isExtChanged) {
            String newFileIdName = file.getId() + newExt;
            String newFilePath = dirPath + SEPAR + newFileIdName;
            File destFile = new File(newFilePath);
            if (!srcFile.renameTo(destFile)) {
                LogManager.addLog(String.format(Locale.getDefault(),
                        context.getString(R.string.log_rename_file_error_mask), filePath, newFilePath), LogManager.Types.ERROR);
                return 0;
            }
        }

        return 1;
    }

    /**
     * Удаление прикрепленного файла.
     * @param file
     * @param withoutFile не пытаться удалить сам файл на диске
     * @return 1 - успешно
     *         0 - ошибка
     *         -1 - ошибка (отсутствует каталог записи)
     *         -2 - ошибка (отсутствует файл в каталоге записи)
     */
    public static int deleteAttachedFile(TetroidFile file, boolean withoutFile) {
        if (file == null) {
            LogManager.emptyParams("DataManager.deleteAttachedFile()");
            return 0;
        }
        TetroidLog.addOperStartLog(TetroidLog.Objs.FILE, TetroidLog.Opers.DELETE);

        TetroidRecord record = file.getRecord();
        if (record == null) {
            LogManager.addLog(context.getString(R.string.log_file_record_is_null), LogManager.Types.ERROR);
            return 0;
        }

        String dirPath;
        String destFilePath = null;
        File destFile = null;
        if (!withoutFile) {
            // проверяем существование каталога записи
            dirPath = getPathToRecordFolder(record);
            int dirRes = checkRecordFolder(dirPath, false);
            if (dirRes <= 0) {
                return dirRes;
            }
            // проверяем существование самого файла
            String ext = FileUtils.getExtensionWithComma(file.getName());
            String fileIdName = file.getId() + ext;
            destFilePath = dirPath + SEPAR + fileIdName;
            destFile = new File(destFilePath);
            if (!destFile.exists()) {
                LogManager.addLog(context.getString(R.string.log_attach_file_is_missing) + destFilePath, LogManager.Types.ERROR);
                return -2;
            }
        }

        // удаляем файл из списка файлов записи (и соответственно, из коллекции)
        List<TetroidFile> files = record.getAttachedFiles();
        if (files != null) {
            if (!files.remove(file)) {
                LogManager.addLog(context.getString(R.string.log_not_found_file_in_record), LogManager.Types.ERROR);
                return 0;
            }
        } else {
            LogManager.addLog(context.getString(R.string.log_record_not_have_attached_files), LogManager.Types.ERROR);
            return 0;
        }

        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
            /*instance.mFilesCount--;*/
        } else {
//            LogManager.addLog(context.getString(R.string.log_cancel_file_deleting), LogManager.Types.ERROR);
            TetroidLog.addOperCancelLog(TetroidLog.Objs.FILE, TetroidLog.Opers.DELETE);
            return 0;
        }

        // удаляем сам файл
        if (!withoutFile) {
            if (!FileUtils.deleteRecursive(destFile)) {
                LogManager.addLog(context.getString(R.string.log_error_delete_file) + destFilePath, LogManager.Types.ERROR);
                return 0;
            }
        }
        return 1;
    }

    /**
     * Получение размера прикрепленного файла.
     * @param context
     * @param file
     * @return
     */
    public static String getAttachedFileSize(Context context, @NonNull TetroidFile file) {
        if (context == null || file == null) {
            LogManager.emptyParams("DataManager.getAttachedFileSize()");
            return null;
        }
        TetroidRecord record = file.getRecord();
        if (record == null) {
            LogManager.addLog(context.getString(R.string.log_file_record_is_null), LogManager.Types.ERROR);
            return null;
        }

        String ext = FileUtils.getExtensionWithComma(file.getName());
//        String fullFileName = String.format("%s/%s/%s%s", getStoragePathBase(), record.getDirName(), file.getId(), ext);
        String fullFileName = getPathToFileInRecordFolder(record, file.getId() + ext);

        long size;
        try {
            File srcFile = new File(fullFileName);
            if (!srcFile.exists()) {
                LogManager.addLog(context.getString(R.string.log_attach_file_is_missing) + fullFileName, LogManager.Types.ERROR);
                return null;
            }
            size = srcFile.length();
        } catch (SecurityException ex) {
            LogManager.addLog(context.getString(R.string.log_denied_read_file_access) + fullFileName, ex);
            return null;
        } catch (Exception ex) {
            LogManager.addLog(context.getString(R.string.log_get_file_size_error) + fullFileName, ex);
            return null;
        }
        return Utils.fileSizeToString(context, size);
    }

    /**
     * Сохранение файла изображения в каталог записи.
     * @param context
     * @param record
     * @param srcUri
     * @param deleteSrcFile Нужно ли удалить исходный файл после сохранения файла назначения
     * @return
     */
    public static TetroidImage saveImage(Context context, TetroidRecord record, Uri srcUri, boolean deleteSrcFile) {
        if (record == null || srcUri == null) {
            LogManager.emptyParams("DataManager.saveImage()");
            return null;
        }
        String srcPath = srcUri.getPath();
        LogManager.addLog(String.format(context.getString(R.string.log_start_image_file_saving_mask),
                srcPath, record.getId()), LogManager.Types.DEBUG);

        // генерируем уникальное имя файла
        String nameId = createUniqueImageName();

        TetroidImage image = new TetroidImage(nameId, record);

        // проверяем существование каталога записи
        String dirPath = getPathToRecordFolder(record);
        int dirRes = checkRecordFolder(dirPath, true);
        if (dirRes <= 0) {
            return null;
        }

        String destFullName = getPathToFileInRecordFolder(record, nameId);
        LogManager.addLog(String.format(context.getString(R.string.log_start_image_file_converting_mask),
                destFullName), LogManager.Types.DEBUG);
        try {
            // конвертируем изображение в формат PNG и сохраняем в каталог записи
            ImageUtils.convertImage(context, srcUri, destFullName, Bitmap.CompressFormat.PNG, 100);
            File destFile = new File(destFullName);
            if (destFile.exists()) {
                if (deleteSrcFile) {
                    LogManager.addLog(String.format(context.getString(R.string.log_start_image_file_deleting_mask),
                            srcUri), LogManager.Types.DEBUG);
                    File srcFile = new File(srcPath);
                    // удаляем исходный файл за ненадобностью
                    if (!srcFile.delete()) {
                        LogManager.addLog(context.getString(R.string.log_error_deleting_src_image_file)
                                + srcPath, LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    }
                }
            } else {
                LogManager.addLog(context.getString(R.string.log_error_image_file_saving), LogManager.Types.ERROR);
                return null;
            }
        } catch (Exception ex) {
            LogManager.addLog(context.getString(R.string.log_error_image_file_saving), ex);
            return null;
        }
        return image;
    }

    /**
     * Перемещение файла или каталога рекурсивно с дальнейшим переименованием, если нужно.
     * @param srcFullFileName
//     * @param srcFileName
     * @param destPath
     * @param newFileName
     * @return 1 - успешно
     *         -2 - ошибка (не удалось переместить или переименовать)
     */
    private static int moveFile(String srcFullFileName,/* String srcFileName, */String destPath, String newFileName) {
        File srcFile = new File(srcFullFileName);
        String srcFileName = srcFile.getName();
        File destDir = new File(destPath);
        // перемещаем файл или каталог
        if (!FileUtils.moveToDirRecursive(srcFile, destDir)) {
            LogManager.addLog(String.format(context.getString(R.string.log_error_move_file_mask),
                    srcFullFileName, destPath), LogManager.Types.ERROR);
            return -2;
        }

        if (newFileName == null) {
            String destDirPath = destDir.getAbsolutePath() + File.separator + srcFileName;
            LogManager.addLog(String.format(context.getString(R.string.log_file_moved_mask),
                    destDirPath), LogManager.Types.DEBUG);
        } else {
            // добавляем к имени каталога записи уникальную приставку
            srcFile = new File(destPath, srcFileName);
            File destFile = new File(destPath, newFileName);
            if (srcFile.renameTo(destFile)) {
                LogManager.addLog(String.format(context.getString(R.string.log_file_moved_mask),
                        destFile.getAbsolutePath()), LogManager.Types.DEBUG);
            } else {
                LogManager.addLog(String.format(context.getString(R.string.log_error_move_file_mask),
                        srcFile.getAbsolutePath(), destFile.getAbsolutePath()), LogManager.Types.ERROR);
                return -2;
            }
        }
        return 1;
    }

    /**
     * Сохранение хранилища в файл mytetra.xml.
     * @return
     */
    public static boolean saveStorage() {
        String destPath = instance.storagePath + SEPAR + MYTETRA_XML_FILE_NAME;
        String tempPath = destPath + "_tmp";

        LogManager.addLog(context.getString(R.string.log_saving_mytetra_xml), LogManager.Types.DEBUG);
        try {
            FileOutputStream fos = new FileOutputStream(tempPath, false);
            if (instance.save(fos)) {
                File to = new File(destPath);
                // перемещаем старую версию файла mytetra.xml в корзину
                String nameInTrash = createDateTimePrefix() + "_" + MYTETRA_XML_FILE_NAME;
                if (moveFile(destPath, SettingsManager.getTrashPath(), nameInTrash) <= 0) {
                    // если не удалось переместить в корзину, удаляем
                    if (!to.delete()) {
                        LogManager.addLog(context.getString(R.string.log_failed_delete_file) + destPath, LogManager.Types.ERROR);
                        return false;
                    }
                }
                // задаем правильное имя актуальной версии файла mytetra.xml
                File from = new File(tempPath);
                if (!from.renameTo(to)) {
                    LogManager.addLog(String.format(context.getString(R.string.log_rename_file_error_mask),
                            tempPath, destPath), LogManager.Types.ERROR);
                    return false;
                }
                return true;
            }
        } catch (Exception ex) {
            LogManager.addLog(ex);
        }
        return false;
    }

    /**
     * Разбираем строку с метками записи и добавляем метки в запись и в коллекцию.
     * @param record
     * @param tagsString Строка с метками (не зашифрована).
     *                   Передается отдельно, т.к. поле в записи может быть зашифровано.
     */
    @Override
    public void parseRecordTags(TetroidRecord record, String tagsString) {
        if (record == null)
            return;
//        String tagsString = record.getTagsString();
        if (!TextUtils.isEmpty(tagsString)) {
            for (String tagName : tagsString.split(TAGS_SEPAR)) {
                TetroidTag tag;
                if (mTagsMap.containsKey(tagName)) {
                    tag = mTagsMap.get(tagName);
                    // добавляем запись по метке, только если ее еще нет
                    // (исправление дублирования записей по метке, если одна и та же метка
                    // добавлена в запись несколько раз)
                    if (!tag.getRecords().contains(record)) {
                        tag.addRecord(record);
                    }
                } else {
                    List<TetroidRecord> tagRecords = new ArrayList<>();
                    tagRecords.add(record);
                    tag = new TetroidTag(tagName, tagRecords);
                    mTagsMap.put(tagName, tag);
                    /*this.mUniqueTagsCount++;*/
                }
               /* this.mTagsCount++;*/
                record.addTag(tag);
            }
        }
    }

    /**
     * Удаление меток записи из списка.
     * @param record
     */
    @Override
    public void deleteRecordTags(TetroidRecord record) {
        if (record == null)
            return;
        if (!record.getTags().isEmpty()) {
            for (TetroidTag tag : record.getTags()) {
                TetroidTag foundedTag = getTag(tag.getName());
                if (foundedTag != null) {
                    if (foundedTag.getRecords().size() > 1) {
                        // удаляем запись из метки
                        foundedTag.getRecords().remove(record);
                    } else {
                        // удаляем саму метку из списка
                        mTagsMap.remove(foundedTag.getName());
                       /* this.mUniqueTagsCount--;*/
                    }
                    /*this.mTagsCount--;*/
                }
            }
            record.getTags().clear();
        }
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
        createModesIerarhy(hierarchy, node);
        return hierarchy;
    }

    private static void createModesIerarhy(Stack<TetroidNode> hierarchy, TetroidNode node) {
        hierarchy.push(node);
        if (node.getLevel() > 0) {
            createModesIerarhy(hierarchy, node.getParentNode());
        }
    }

    public static boolean clearTrashFolder() {
        File trashDir = new File(SettingsManager.getTrashPath());
        // очищаем "буфер обмена", т.к. каталог(и) записи из корзины будут удалены
        // и нечего будет вставлять
        TetroidClipboard.clear();
        return FileUtils.clearDir(trashDir);
    }

    public static String getRecordDirUri(@NonNull TetroidRecord record) {
        return getStoragePathBaseUri() + SEPAR + record.getDirName() + SEPAR;
    }

    @NonNull
    public static Uri getStoragePathBaseUri() {
//        return Uri.fromFile(new File(getStoragePathBase()));
        return Uri.parse("file://" + getStoragePathBase());
    }

    @NonNull
    public static String getStoragePathBase() {
        return instance.storagePath + SEPAR + BASE_FOLDER_NAME;
    }

    public static String getPathToRecordFolder(TetroidRecord record) {
        return getStoragePathBase() + SEPAR + record.getDirName();
    }

    public static String getPathToRecordFolderInTrash(TetroidRecord record) {
        return SettingsManager.getTrashPath() + SEPAR + record.getDirName();
    }

    public static String getPathToFileInRecordFolder(TetroidRecord record, String fileName) {
        return getPathToRecordFolder(record) + SEPAR + fileName;
    }

    public static TetroidNode getNode(String id) {
        return getNodeInHierarchy(instance.mRootNodesList, id);
    }

    public static TetroidRecord getRecord(String id) {
        return getRecordInHierarchy(instance.mRootNodesList, id, new TetroidRecordComparator(TetroidRecord.FIELD_ID));
    }

    public static TetroidTag getTag(String tagName) {
        for (Map.Entry<String,TetroidTag> tag : getTags().entrySet()) {
            if (tag.getKey().contentEquals(tagName))
                return tag.getValue();
        }
        return null;
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

    public static TetroidRecord getRecordInHierarchy(List<TetroidNode> nodes, String fieldValue, TetroidRecordComparator comparator) {
        if (comparator == null)
            return null;
        for (TetroidNode node : nodes) {
            for (TetroidRecord record : node.getRecords()) {
                if (comparator.compare(fieldValue, record))
                    return record;
            }
            if (node.isExpandable()) {
                TetroidRecord found = getRecordInHierarchy(node.getSubNodes(), fieldValue, comparator);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    public static String getLastFolderOrDefault(Context context, boolean forWrite) {
        String lastFolder = SettingsManager.getLastChoosedFolder();
        return (!StringUtil.isBlank(lastFolder)) ? lastFolder : FileUtils.getExternalPublicDocsOrAppDir(context, forWrite);
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

    public static List<TetroidNode> getRootNodes() {
        return instance.mRootNodesList;
    }

    public static Map<String,TetroidTag> getTags() {
//        return mInstance.tagsList;
        return instance.mTagsMap;
    }

    public static Collection<TetroidTag> getTagsValues() {
//        return mInstance.tagsList;
        return instance.mTagsMap.values();
    }

    public static boolean isNodesExist() {
        return (instance.mRootNodesList != null && !instance.mRootNodesList.isEmpty());
    }

    public static String getStoragePath() {
        return instance.storagePath;
    }

    public static boolean isExistsCryptedNodes() {
        return instance.mIsExistCryptedNodes;
    }

    public static boolean isCrypted() {
//        return instance.mIsExistCryptedNodes;
//        return (Integer.parseInt(databaseConfig.get(INI_CRYPT_MODE)) == 1);
        boolean iniFlag = false;
        try {
            iniFlag = databaseConfig.isCryptMode();
        } catch (DatabaseConfig.EmptyFieldException ex) {
            LogManager.addLog(ex);
        }
        /*return (iniFlag == 1 && instance.mIsExistCryptedNodes) ? true
                : (iniFlag != 1 && !instance.mIsExistCryptedNodes) ? false
                : (iniFlag == 1 && !instance.mIsExistCryptedNodes) ? true
                : (iniFlag == 0 && instance.mIsExistCryptedNodes) ? true : false;*/
        return (iniFlag || instance.mIsExistCryptedNodes);
    }

    public static boolean isDecrypted() {
        return instance.isDecrypted;
    }

    public static DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    public static DataManager getInstance() {
        return instance;
    }
}
