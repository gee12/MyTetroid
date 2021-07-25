package com.gee12.mytetroid.data;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.gee12.mytetroid.BuildConfig;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.xml.IStorageLoadHelper;
import com.gee12.mytetroid.views.activities.MainActivity;
import com.gee12.mytetroid.data.crypt.IRecordFileCrypter;
import com.gee12.mytetroid.data.crypt.TetroidCrypter;
import com.gee12.mytetroid.data.ini.DatabaseConfig;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.logs.TetroidLog;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.ITetroidObject;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidObject;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;
import com.gee12.mytetroid.services.FileObserverService;
import com.gee12.mytetroid.utils.FileUtils;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.views.Message;

import org.jetbrains.annotations.NotNull;
import org.jsoup.internal.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DataManager implements IRecordFileCrypter {

//    public static final String ID_SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyz";
//    public static final String QUOTES_PARAM_STRING = "\"\"";
//
//    public static final String SEPAR = File.separator;
//    public static final int UNIQUE_ID_HALF_LENGTH = 10;
//    public static final String PREFIX_DATE_TIME_FORMAT = "yyyyMMddHHmmssSSS";
//
//    public static final String BASE_FOLDER_NAME = "base";
//    public static final String ICONS_FOLDER_NAME = "icons";
//    public static final String MYTETRA_XML_FILE_NAME = "mytetra.xml";
//    public static final String DATABASE_INI_FILE_NAME = "database.ini";

    /**
     * Путь к хранилищу.
     */
    protected String mStoragePath;

    /**
     * Название хранилища.
     */
    protected String mStorageName;

    /**
     * Проинициализировано ли хранилище (установлен путь, но может быть еще не загружено).
     */
    protected boolean mIsStorageInited;

    /**
     * Расшифровано ли в данный момент хранилище (временно).
     */
    protected boolean mIsStorageDecrypted;

    /**
     * Ветка для быстрой вставки.
     */
    protected TetroidNode mQuicklyNode;

    /**
     *
     */
    protected TetroidCrypter mCrypter;

    /**
     *
     */
    protected DatabaseConfig mDatabaseConfig;


    // TODO: заменено на ViewModel
//    protected TetroidXml mXml = new TetroidXml(new IStorageLoadHelper() {
//        @Override
//        public boolean decryptNode(Context context, TetroidNode node) {
//            return mCrypter.decryptNode(context, node, false, false,
//                    this, false, false);
//        }
//
//        @Override
//        public boolean decryptRecord(Context context, TetroidRecord record) {
//            return mCrypter.decryptRecordAndFiles(context, record, false, false);
//        }
//
//        @Override
//        public boolean isRecordFavorite(String id) {
//            return FavoritesManager.isFavorite(id);
//        }
//
//        @Override
//        public void addRecordFavorite(TetroidRecord record) {
//            FavoritesManager.set(record);
//        }
//
//        @Override
//        public void parseRecordTags(TetroidRecord record, String tagsString) {
//            DataManager.this.parseRecordTags(record, tagsString);
//        }
//
//        @Override
//        public void deleteRecordTags(TetroidRecord record) {
//            DataManager.this.deleteRecordTags(record);
//        }
//
//        @Override
//        public void loadIcon(Context context, TetroidNode node) {
//            if (node.isNonCryptedOrDecrypted()) {
//                node.loadIcon(context, mStoragePath + SEPAR + ICONS_FOLDER_NAME);
//            }
//        }
//    });


    /**
     *
     */
    protected static StorageManager Instance;

    /**
     * Инициализация ключа шифрования с помощью пароля или его хэша.
     * @param pass
     * @param isMiddleHash
     */
    public void initCryptPass(String pass, boolean isMiddleHash) {
        if (isMiddleHash) {
            mCrypter.initFromMiddleHash(pass);
        } else {
            mCrypter.initFromPass(pass);
        }
    }

    /**
     * Создание минимально требуемых объектов хранилища.
     * @return
     */
    public static boolean createDefault(Context context) {
        return (NodesManager.createNode(context, context.getString(R.string.title_first_node), TetroidXml.ROOT_NODE) != null);
    }

    // TODO: заменено на ViewModel
//    /**
//     * Загрузка хранилища из файла mytetra.xml.
//     * @param isDecrypt Расшифровывать ли ветки
//     * @return
//     */
//    public boolean readStorage(Context context, boolean isDecrypt, boolean isFavorite) {
//        boolean res = false;
////        File file = new File(Instance.mStoragePath + SEPAR + MYTETRA_XML_FILE_NAME);
//        File file = new File(getPathToMyTetraXml());
//        if (!file.exists()) {
//            LogManager.log(context, context.getString(R.string.log_file_is_absent) + MYTETRA_XML_FILE_NAME, ILogger.Types.ERROR);
//            return false;
//        }
//        // получаем id избранных записей из настроек
//        FavoritesManager.load(context);
//        try {
//            FileInputStream fis = new FileInputStream(file);
//            res = Instance.mXml.parse(context, fis, isDecrypt, isFavorite);
//
////            if (BuildConfig.DEBUG) {
////                TestData.addNodes(mInstance.mRootNodesList, 100, 100);
////            }
//            // удаление не найденных записей из избранного
//            FavoritesManager.check();
//            // загрузка ветки для быстрой вставки
//            NodesManager.updateQuicklyNode(context);
//        } catch (Exception ex) {
//            LogManager.log(context, ex);
//            Message.showSnackMoreInLogs(context, R.id.layout_coordinator);
//        }
//        return res;
//    }

    /**
     * Перешифровка хранилища (перед этим ветки должны быть расшифрованы).
     * @return
     */
    public boolean reencryptStorage(Context context) {
//        LogManager.log(R.string.log_start_storage_reencrypt);
        return mCrypter.encryptNodes(context, mXml.mRootNodesList, true);
    }

    /**
     * Расшифровка хранилища (временная).
     * @return
     */
    public boolean decryptStorage(Context context, boolean decryptFiles) {
//        LogManager.log(R.string.log_start_storage_decrypt);
        boolean res = mCrypter.decryptNodes(context, mXml.mRootNodesList, true, true,
                mXml.getLoadHelper(), false, decryptFiles);
        mIsStorageDecrypted = res;
        return res;
    }

    /**
     * Расшифровка ветки с подветками (постоянная).
     * @param node
     * @return
     */
    public boolean dropCryptNode(Context context, @NonNull TetroidNode node) {
//        TetroidLog.logOperStart(TetroidLog.Objs.NODE, TetroidLog.Opers.DROPCRYPT, node);
        boolean res = mCrypter.decryptNode(context, node, true, true, mXml.getLoadHelper(), true, false);
        if (res) {
            return saveStorage(context);
        }
        return false;
    }

    public String decryptField(TetroidObject obj, String field) {
        return (obj != null && obj.isCrypted()) ? Instance.mCrypter.decryptBase64(field) : field;
    }

    public String decryptField(boolean isCrypted, String field) {
        return (isCrypted) ? Instance.mCrypter.decryptBase64(field) : field;
    }

    public String encryptField(TetroidObject obj, String field) {
        return encryptField(obj != null && obj.isCrypted() && obj.isDecrypted(), field); // последняя проверка не обязательна
    }

    public String encryptField(boolean isCrypted, String field) {
        return (isCrypted) ? Instance.mCrypter.encryptTextBase64(field) : field;
    }

    /**
     * Зашифровка (незашифрованной) ветки с подветками.
     * @param node
     * @return
     */
    public boolean encryptNode(Context context, @NotNull TetroidNode node) {
//        TetroidLog.logOperStart(TetroidLog.Objs.NODE, TetroidLog.Opers.ENCRYPT, node);
        boolean res = Instance.mCrypter.encryptNode(context, node, false);
        if (res) {
            return saveStorage(context);
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
    private int cryptOrDecryptFile(Context context, File file, boolean isCrypted, boolean isEncrypt) {
        if (isCrypted && !isEncrypt) {
            try {
                // расшифровуем файл записи
                return (Instance.mCrypter.encryptDecryptFile(file, file, false)) ? 1 : -1;
            } catch (Exception ex) {
                LogManager.log(context, context.getString(R.string.log_error_file_decrypt) + file.getAbsolutePath(), ex);
                return -1;
            }
        } else if (!isCrypted && isEncrypt) {
            try {
                // зашифровуем файл записи
                return (Instance.mCrypter.encryptDecryptFile(file, file, true)) ? 1 : -1;
            } catch (Exception ex) {
                LogManager.log(context, context.getString(R.string.log_error_file_encrypt) + file.getAbsolutePath(), ex);
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
    public boolean cryptRecordFiles(Context context, TetroidRecord record, boolean isCrypted, boolean isEncrypt) {
        // файл записи
        String recordFolderPath = RecordsManager.getPathToRecordFolderInBase(record);
        File file = new File(recordFolderPath, record.getFileName());
        if (cryptOrDecryptFile(context, file, isCrypted, isEncrypt) < 0) {
            return false;
        }
        // прикрепленные файлы
        if (record.getAttachedFilesCount() > 0) {
            for (TetroidFile attach : record.getAttachedFiles()) {
                file = new File(recordFolderPath, attach.getIdName());
                if (!file.exists()) {
                    LogManager.log(context, context.getString(R.string.log_file_is_missing)
                            + TetroidLog.getIdString(context, attach), ILogger.Types.WARNING, Toast.LENGTH_LONG);
                    continue;
                }
                if (cryptOrDecryptFile(context, file, isCrypted, isEncrypt) < 0) {
                    return false;
                }
            }
        }
        return true;
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

    /**
     * Замена местами 2 объекта хранилища в списке.
     * @param list
     * @param pos
     * @param isUp
     * @return 1 - успешно
     *         0 - перемещение невозможно (пограничный элемент)
     *        -1 - ошибка
     */
    public static int swapTetroidObjects(Context context, List list, int pos, boolean isUp, boolean through) {
        boolean isSwapped;
        try {
            isSwapped = Utils.swapListItems(list, pos, isUp, through);
        } catch (Exception ex) {
            LogManager.log(context, ex, -1);
            return -1;
        }

        // перезаписываем файл структуры хранилища
        if (isSwapped) {
            return (Instance.saveStorage(context)) ? 1 : -1;
        }
        return 0;
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
    protected static int moveFile(Context context, String srcFullFileName,/* String srcFileName, */String destPath, String newFileName) {
        File srcFile = new File(srcFullFileName);
        String srcFileName = srcFile.getName();
        File destDir = new File(destPath);
        // перемещаем файл или каталог
        if (!FileUtils.moveToDirRecursive(srcFile, destDir)) {
            String fromTo = getStringFromTo(context, srcFullFileName, destPath);
//            LogManager.log(String.format(context.getString(R.string.log_error_move_file_mask),
//                    srcFullFileName, destPath), LogManager.Types.ERROR);
            TetroidLog.logOperError(context, TetroidLog.Objs.FILE, TetroidLog.Opers.MOVE,
                    fromTo, false, -1);
            return -2;
        }

        if (newFileName == null) {
            String destDirPath = destDir.getAbsolutePath() + File.separator + srcFileName;
            String to = Utils.getStringFormat(context, R.string.log_to_mask, destDirPath);
//            LogManager.log(String.format(context.getString(R.string.log_file_moved_mask),
//                    destDirPath), LogManager.Types.DEBUG);
            TetroidLog.logOperRes(context, TetroidLog.Objs.FILE, TetroidLog.Opers.MOVE, to, -1);
        } else {
            // добавляем к имени каталога записи уникальную приставку
            srcFile = new File(destPath, srcFileName);
            File destFile = new File(destPath, newFileName);
            if (srcFile.renameTo(destFile)) {
                String to = Utils.getStringFormat(context, R.string.log_to_mask, destFile.getAbsolutePath());
//                LogManager.log(String.format(context.getString(R.string.log_file_moved_mask),
//                        destFile.getAbsolutePath()), LogManager.Types.DEBUG);
                TetroidLog.logOperRes(context, TetroidLog.Objs.FILE, TetroidLog.Opers.MOVE, to, -1);
            } else {
                String fromTo = getStringFromTo(context, srcFile.getAbsolutePath(), destFile.getAbsolutePath());
//                LogManager.log(String.format(context.getString(R.string.log_error_move_file_mask),
//                        srcFile.getAbsolutePath(), destFile.getAbsolutePath()), LogManager.Types.ERROR);
                TetroidLog.logOperError(context, TetroidLog.Objs.FILE, TetroidLog.Opers.MOVE,
                        fromTo, false, -1);
                return -2;
            }
        }
        return 1;
    }

    /**
     * Сохранение хранилища в файл mytetra.xml.
     * @return
     */
    protected boolean saveStorage(Context context) {
        if (mXml.mRootNodesList == null) {
//            LogManager.log("Попытка сохранения mytetra.xml в режиме загрузки только избранных записей", LogManager.Types.WARNING);
            LogManager.log(context, R.string.log_attempt_save_empty_nodes, ILogger.Types.ERROR);
            return false;
        }

        String destPath = getPathToMyTetraXml();
        String tempPath = destPath + "_tmp";

        LogManager.log(context, context.getString(R.string.log_saving_mytetra_xml), ILogger.Types.DEBUG);
        try {
            FileOutputStream fos = new FileOutputStream(tempPath, false);
            if (mXml.save(fos)) {
                File to = new File(destPath);
//                if (moveOld) {
                // перемещаем старую версию файла mytetra.xml в корзину
                String nameInTrash = createDateTimePrefix() + "_" + MYTETRA_XML_FILE_NAME;
                if (moveFile(context, destPath, SettingsManager.getTrashPath(context), nameInTrash) <= 0) {
                    // если не удалось переместить в корзину, удаляем
                    if (to.exists() && !to.delete()) {
//                        LogManager.log(context.getString(R.string.log_failed_delete_file) + destPath, LogManager.Types.ERROR);
                        TetroidLog.logOperError(context, TetroidLog.Objs.FILE, TetroidLog.Opers.DELETE, destPath, false, -1);
                        return false;
                    }
                }
                // задаем правильное имя актуальной версии файла mytetra.xml
                File from = new File(tempPath);
                if (!from.renameTo(to)) {
                    String fromTo = getStringFromTo(context, tempPath, destPath);
//                    LogManager.log(String.format(context.getString(R.string.log_rename_file_error_mask),
//                            tempPath, destPath), LogManager.Types.ERROR);
                    TetroidLog.logOperError(context, TetroidLog.Objs.FILE, TetroidLog.Opers.RENAME,
                            fromTo, false, -1);
                    return false;
                }

                // перезапускаем отслеживание, чтобы проверять новосозданный файл
                if (context instanceof MainActivity) {
                    // но только для MainActivity
                    FileObserverService.sendCommand((MainActivity)context, FileObserverService.ACTION_RESTART);
                    LogManager.log(context, context.getString(R.string.log_mytetra_xml_observer_mask,
                            context.getString(R.string.relaunched)), ILogger.Types.INFO);
                }

                return true;
            }
        } catch (Exception ex) {
            LogManager.log(context, ex);
        }
        return false;
    }

    /**
     * Разбираем строку с метками записи и добавляем метки в запись и в дерево.
     * @param record
     * @param tagsString Строка с метками (не зашифрована).
     *                   Передается отдельно, т.к. поле в записи может быть зашифровано.
     */
    public void parseRecordTags(TetroidRecord record, String tagsString) {
        if (record == null)
            return;
        if (!TextUtils.isEmpty(tagsString)) {
            for (String tagName : tagsString.split(TetroidXml.TAGS_SEPAR)) {
                String lowerCaseTagName = tagName.toLowerCase();
                TetroidTag tag;
                if (mXml.mTagsMap.containsKey(lowerCaseTagName)) {
                    tag = mXml.mTagsMap.get(lowerCaseTagName);
                    // добавляем запись по метке, только если ее еще нет
                    // (исправление дублирования записей по метке, если одна и та же метка
                    // добавлена в запись несколько раз)
                    if (!tag.getRecords().contains(record)) {
                        tag.addRecord(record);
                    }
                } else {
                    List<TetroidRecord> tagRecords = new ArrayList<>();
                    tagRecords.add(record);
                    tag = new TetroidTag(lowerCaseTagName, tagRecords);
                    mXml.mTagsMap.put(lowerCaseTagName, tag);
                }
                record.addTag(tag);
            }
        }
    }

    /**
     * Удаление меток записи из списка.
     * @param record
     */
    public void deleteRecordTags(TetroidRecord record) {
        if (record == null)
            return;
        if (!record.getTags().isEmpty()) {
            for (TetroidTag tag : record.getTags()) {
                TetroidTag foundedTag = getTag(tag.getName());
                if (foundedTag != null) {
                    // удаляем запись из метки
                    foundedTag.getRecords().remove(record);
                    if (foundedTag.getRecords().isEmpty()) {
                        // удаляем саму метку из списка
                        mXml.mTagsMap.remove(tag.getName().toLowerCase());
                    }
                }
            }
            record.getTags().clear();
        }
    }

    public static boolean clearTrashFolder(Context context) {
        File trashDir = new File(SettingsManager.getTrashPath(context));
        // очищаем "буфер обмена", т.к. каталог(и) записи из корзины будут удалены
        // и нечего будет вставлять
        TetroidClipboard.clear();
        return FileUtils.clearDir(trashDir);
    }

    @NonNull
    public static Uri getStoragePathBaseUri() {
//        return Uri.fromFile(new File(getStoragePathBase()));
        return Uri.parse("file://" + getStoragePathBase());
    }

    @NonNull
    public static Uri getTrashPathBaseUri(Context context) {
        return Uri.parse("file://" + SettingsManager.getTrashPath(context));
    }

    @NonNull
    public static String getStoragePathBase() {
        return (Instance != null) ? Instance.mStoragePath + SEPAR + BASE_FOLDER_NAME : "";
    }

    /**
     * Поиск объекта TetroidTag в списке всех меток по ключу.
     * @param tagName Имя метки
     * @return
     */
    public static TetroidTag getTag(String tagName) {
        if (TextUtils.isEmpty(tagName))
            return null;
        String lowerCaseTagName = tagName.toLowerCase();
        for (Map.Entry<String,TetroidTag> tag : Instance.getTags().entrySet()) {
            if (tag.getKey().contentEquals(lowerCaseTagName))
                return tag.getValue();
        }
        return null;
    }

    public static String getLastFolderPathOrDefault(Context context, boolean forWrite) {
        String lastFolder = SettingsManager.getLastChoosedFolderPath(context);
        return (!StringUtil.isBlank(lastFolder) && new File(lastFolder).exists())
                ? lastFolder : FileUtils.getExternalPublicDocsOrAppDir(context, forWrite);
    }

    /**
     * Получение размера файла/каталога.
     * @param context
     * @param fullFileName
     * @return
     */
    public static String getFileSize(Context context, String fullFileName) {
        long size;
        try {
            File file = new File(fullFileName);
            if (!file.exists()) {
                LogManager.log(context, context.getString(R.string.log_file_is_missing) + fullFileName, ILogger.Types.ERROR);
                return null;
            }
            size = FileUtils.fileSize(file);
        } catch (SecurityException ex) {
            LogManager.log(context, context.getString(R.string.log_denied_read_file_access) + fullFileName, ex);
            return null;
        } catch (Exception ex) {
            LogManager.log(context, context.getString(R.string.log_get_file_size_error) + fullFileName, ex);
            return null;
        }
//        return FileUtils.fileSizeToStringBin(context, size);
        return android.text.format.Formatter.formatFileSize(context, size);
    }

    /**
     *
     * @param context
     * @param fullFileName
     * @return
     */
    public static Date getFileModifiedDate(Context context, String fullFileName) {
        Date date;
        try {
            File file = new File(fullFileName);
            if (!file.exists()) {
                LogManager.log(context, context.getString(R.string.log_file_is_missing) + fullFileName, ILogger.Types.ERROR);
                return null;
            }
            date = FileUtils.fileLastModifiedDate(file);
        } catch (SecurityException ex) {
            LogManager.log(context, context.getString(R.string.log_denied_read_file_access) + fullFileName, ex);
            return null;
        } catch (Exception ex) {
            LogManager.log(context, context.getString(R.string.log_get_file_size_error) + fullFileName, ex);
            return null;
        }
        return date;
    }

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

    public static String getStringFromTo(Context context, String from, String to) {
        return Utils.getStringFormat(context, R.string.log_from_to_mask, from, to);
    }

    public static String getStringTo(Context context, String to) {
        return Utils.getStringFormat(context, R.string.log_to_mask, to);
    }

    public static String getStorageName() {
        return Instance.mStorageName;
    }

    public static List<TetroidNode> getRootNodes() {
//        return Instance.mXml.mRootNodesList;
        return getInstance().mXml.mRootNodesList;
    }

    public static Map<String,TetroidTag> getTags() {
        return getInstance().mXml.mTagsMap;
    }

    public Collection<TetroidTag> getTagsValues() {
        return mXml.mTagsMap.values();
    }

    public boolean isNodesExist() {
        return (mXml.mRootNodesList != null && !mXml.mRootNodesList.isEmpty());
    }

    public static String getStoragePath() {
        return Instance.mStoragePath;
    }

    public static String getPathToMyTetraXml() {
        return Instance.mStoragePath + SEPAR + MYTETRA_XML_FILE_NAME;
    }

    public static boolean isInited() {
        return (Instance != null && Instance.mIsStorageInited);
    }

    public static boolean isLoaded() {
        return (Instance != null && Instance.mXml.mIsStorageLoaded);
    }

    public static boolean isCrypted(Context context) {
        if (Instance == null || Instance.mDatabaseConfig == null)
            return false;
        boolean iniFlag = false;
        try {
            iniFlag = Instance.mDatabaseConfig.isCryptMode();
        } catch (Exception ex) {
            LogManager.log(context, ex);
        }
        /*return (iniFlag == 1 && instance.mIsExistCryptedNodes) ? true
                : (iniFlag != 1 && !instance.mIsExistCryptedNodes) ? false
                : (iniFlag == 1 && !instance.mIsExistCryptedNodes) ? true
                : (iniFlag == 0 && instance.mIsExistCryptedNodes) ? true : false;*/
        return (iniFlag || Instance.mXml.mIsExistCryptedNodes);
    }

    public static boolean isDecrypted() {
        return Instance != null && Instance.mIsStorageDecrypted;
    }

    public static boolean isFavoritesMode() {
        return Instance != null && Instance.mXml.mIsFavoritesMode;
    }

    public static DatabaseConfig getDatabaseConfig() {
        return (Instance != null) ? Instance.mDatabaseConfig : null;
    }

    public static StorageManager getInstance() {
        if (Instance == null) {
            Instance = createInstance();
        }
        return Instance;
    }

    public static StorageManager createInstance() {
        return new StorageManager();
    }

    public static void destruct() {
        Instance = null;
    }

    public TetroidXml getXmlManager() {
        return mXml;
    }

    public TetroidCrypter getCrypterManager() {
        return mCrypter;
    }

    public static String getTetroidObjectTypeString(Context context, ITetroidObject obj) {
        int resId;
        int type = obj.getType();
        switch (type) {
            case FoundType.TYPE_RECORD:
                resId = R.string.title_record;
                break;
            case FoundType.TYPE_NODE:
                resId = R.string.title_node;
                break;
            case FoundType.TYPE_TAG:
                resId = R.string.title_tag;
                break;
            default:
                String[] strings = context.getResources().getStringArray(R.array.found_types);
                return (strings != null && strings.length < type) ? strings[type] : "";
        }
        return context.getString(resId);
    }
}
