package com.gee12.mytetroid.data;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TetroidLog;
import com.gee12.mytetroid.crypt.CryptManager;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.FileUtils;
import com.gee12.mytetroid.utils.Utils;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordsManager extends DataManager {

    /**
     * Получение пути к файлу с содержимым записи.
     * Если расшифрован, то в tempPath. Если не был зашифрован, то в mStoragePath.
     * @return
     */
/*    public static String getRecordFilePath(@NonNull TetroidRecord record) {
        if (record == null) {
            LogManager.emptyParams("DataManager.getRecordFilePath()");
            return null;
        }
        String path = null;
        if (record.isCrypted()) {
            if (record.isDecrypted()) {
                path = getPathToRecordFolderInTrash(record) + SEPAR + record.getFileName();
            }
        } else {
            path = RecordsManager.getPathToFileInRecordFolder(record, record.getFileName());
        }
//        return "file:///" + file.getAbsolutePath();
        return (path != null) ? "file:///" + path : null;
    }*/

    /**
     * Получение содержимого записи в виде "сырого" html.
     * @param record
     * @return
     */
    public static String getRecordHtmlTextDecrypted(@NonNull TetroidRecord record, int duration) {
        if (record == null) {
            LogManager.emptyParams("DataManager.getRecordHtmlTextDecrypted()");
            return null;
        }
        LogManager.log(context.getString(R.string.log_start_record_file_reading) + record.getId(), LogManager.Types.DEBUG);
        // проверка существования каталога записи
        String dirPath = getPathToRecordFolder(record);
        if (checkRecordFolder(dirPath, true, duration) <= 0) {
            return null;
        }
        String path = dirPath + SEPAR + record.getFileName();
        Uri uri;
        try {
            uri = Uri.parse(path);
        } catch (Exception ex) {
            LogManager.log(context.getString(R.string.log_error_generate_record_file_path) + path, ex);
            return null;
        }
        // проверка существования файла записи
        File file = new File(uri.getPath());
        if (!file.exists()) {
            LogManager.log(context.getString(R.string.log_record_file_is_missing), LogManager.Types.WARNING, duration);
            return null;
        }
        String res = null;
        if (record.isCrypted()) {
            if (record.isDecrypted()) {
                byte[] bytes;
                try {
                    bytes = FileUtils.readFile(uri);
                } catch (Exception ex) {
                    LogManager.log(context.getString(R.string.log_error_read_record_file) + path, ex);
                    return null;
                }
                if (bytes == null) {
                    LogManager.log(context.getString(R.string.log_error_decrypt_record_file) + path, LogManager.Types.ERROR);
                    return null;
                } else if (bytes.length == 0) {
                    // файл пуст
                    return "";
                }
                // расшифровываем содержимое файла
                LogManager.log(context.getString(R.string.log_start_record_text_decrypting), LogManager.Types.DEBUG);
                res = CryptManager.decryptText(bytes);
                if (res == null) {
                    LogManager.log(context.getString(R.string.log_error_decrypt_record_file) + path, LogManager.Types.ERROR);
                }
            }
        } else {
            try {
                res = FileUtils.readTextFile(uri);
            } catch (Exception ex) {
                LogManager.log(context.getString(R.string.log_error_read_record_file) + path, ex);
            }
        }
        return res;
    }

    /**
     * Получение содержимого записи в виде текста.
     * @param record
     * @return
     */
    public static String getRecordTextDecrypted(@NonNull TetroidRecord record) {
        String text = null;
        String html = getRecordHtmlTextDecrypted(record, -1);
        if (html != null) {
            try {
                text = Jsoup.parse(html).text();
            } catch (Exception ex) {
                LogManager.log(ex);
            }
        }
        return text;
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
        LogManager.log(context.getString(R.string.log_start_record_file_saving) + record.getId(), LogManager.Types.DEBUG);
        // проверка существования каталога записи
        String dirPath = (record.isTemp()) ? getPathToRecordFolderInTrash(record) : getPathToRecordFolder(record);
        if (checkRecordFolder(dirPath, true, Toast.LENGTH_LONG) <= 0) {
            return false;
        }
        // формирование пути к файлу записи
        String path = dirPath + SEPAR + record.getFileName();
        Uri uri;
        try {
            uri = Uri.parse(path);
        } catch (Exception ex) {
            LogManager.log(context.getString(R.string.log_error_generate_record_file_path) + path, ex);
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
            LogManager.log(context.getString(R.string.log_error_write_to_record_file) + path, ex);
            return false;
        }
        return true;
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
                    LogManager.log(String.format(Locale.getDefault(), context.getString(R.string.log_create_record_dir), dirPath),
                            LogManager.Types.WARNING);
                    if (folder.mkdirs()) {
//                        LogManager.log(context.getString(R.string.log_record_dir_created), LogManager.Types.DEBUG, duration);
                        TetroidLog.logOperRes(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.CREATE, "", duration);
                        return 1;
                    } else {
                        LogManager.log(context.getString(R.string.log_create_record_dir_error), LogManager.Types.ERROR, duration);
                        return 0;
                    }
                }
                return -1;
            }
        } catch (Exception ex) {
            LogManager.log(context.getString(R.string.log_check_record_dir_error), LogManager.Types.ERROR, duration);
            return 0;
        }
        return 1;
    }

    /**
     * Перемещение или копирование записи в ветку.
     * @param srcRecord
     * @param node
     * @param isCutted
     * @return
     */
    protected static TetroidRecord cloneRecordToNode(TetroidRecord srcRecord, TetroidNode node,
                                                   boolean isCutted, boolean breakOnFSErrors) {
        if (srcRecord == null)
            return null;
        TetroidLog.logOperStart(TetroidLog.Objs.RECORD, TetroidLog.Opers.INSERT, srcRecord);

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
        if (isCutted) {
            record.setIsFavorite(srcRecord.isFavorite());
        }
        // добавляем прикрепленные файлы в запись
        cloneAttachesToRecord(srcRecord, record, isCutted);
        record.setIsNew(false);
        // добавляем запись в ветку (и соответственно, в дерево)
        node.addRecord(record);
        // добавляем в избранное обратно
        if (isCutted && record.isFavorite()) {
            FavoritesManager.add(record);
        }
        // добавляем метки в запись и в коллекцию меток
        Instance.parseRecordTags(record, tagsString);

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
                    LogManager.log(String.format(context.getString(R.string.log_copy_record_dir_mask),
                            destDirPath), LogManager.Types.ERROR);
                    // переименовываем прикрепленные файлы
                    renameRecordAttaches(srcRecord, record);
                } else {
                    LogManager.log(String.format(context.getString(R.string.log_error_copy_record_dir_mask),
                            srcDirPath, destDirPath), LogManager.Types.ERROR);
                    return errorRes;
                }
            } catch (IOException ex) {
                LogManager.log(String.format(context.getString(R.string.log_error_copy_record_dir_mask),
                        srcDirPath, destDirPath), ex);
                return errorRes;
            }
        }

        // зашифровываем или расшифровываем файл записи
//        File recordFile = new File(getPathToFileInRecordFolder(record, record.getFileName()));
//        if (!cryptOrDecryptFile(recordFile, srcRecord.isCrypted(), crypted) && breakOnFSErrors) {
        if (!Instance.cryptRecordFiles(record, srcRecord.isCrypted(), crypted) && breakOnFSErrors) {
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
                String to = getStringTo(destFile.getAbsolutePath());
//                LogManager.log(String.format(context.getString(R.string.log_rename_file_mask),
//                        destFile.getAbsolutePath()), LogManager.Types.DEBUG);
                TetroidLog.logOperRes(TetroidLog.Objs.FILE, TetroidLog.Opers.RENAME, to, -1);
            } else {
                String fromTo = getStringFromTo(srcFile.getAbsolutePath(), destFile.getName());
//                LogManager.log(String.format(context.getString(R.string.log_rename_file_error_mask),
//                        srcFile.getAbsolutePath(), destFile.getName()), LogManager.Types.ERROR);
                TetroidLog.logOperError(TetroidLog.Objs.FILE, TetroidLog.Opers.RENAME, fromTo, false,-1);
            }
        }
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
    public static TetroidRecord createRecord(String name, String tagsString, String author, String url,
                                             TetroidNode node, boolean isFavor) {
        if (node == null || TextUtils.isEmpty(name)) {
            LogManager.emptyParams("DataManager.createRecord()");
            return null;
        }
        TetroidLog.logOperStart(TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE);

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
        record.setIsFavorite(isFavor);
        if (crypted) {
            record.setDecryptedValues(name, tagsString, author, url);
            record.setDecrypted(true);
        }
        record.setIsNew(true);
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
            LogManager.log(context.getString(R.string.log_error_generate_record_file_path) + filePath, ex);
            return null;
        }
        File file = new File(fileUri.getPath());
        try {
            file.createNewFile();
        } catch (IOException ex) {
            LogManager.log(context.getString(R.string.log_error_creating_record_file) + filePath, ex);
            return null;
        }

        // добавляем запись в ветку (и соответственно, в дерево)
        node.addRecord(record);
        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
            // добавляем метки в запись и в коллекцию меток
            Instance.parseRecordTags(record, tagsString);
            // добавляем в избранное
            if (isFavor) {
                FavoritesManager.add(record);
            }
        } else {
            TetroidLog.logOperCancel(TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE);
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
    /*public static TetroidRecord createRecord(String name, String url, String text, TetroidNode node) {
        TetroidLog.logOperStart(TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE);

        if (TextUtils.isEmpty(name)) {
            name = Utils.dateToString(new Date(), "yyyy.MM.dd HH:mm:ss");
        }

        TetroidRecord record = createRecord(name, null, null, url, node);
        if (record == null) {
            TetroidLog.logOperErrorMore(TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE, -1);
            return null;
        }

        if (saveRecordHtmlText(record, text)) {
            record.setIsNew(false);
        } else {
            TetroidLog.logOperErrorMore(TetroidLog.Objs.RECORD, TetroidLog.Opers.SAVE, -1);
            return null;
        }

        return record;
    }*/

    /**
     * Создание временной записи (без сохранения в дерево) при использовании виджета.
     * @return
     */
    public static TetroidRecord createTempRecord(String name, String url, String text) {
        TetroidLog.logOperStart(TetroidLog.Objs.TEMP_RECORD, TetroidLog.Opers.CREATE);
        // генерируем уникальный идентификатор
        String id = createUniqueId();
        // имя каталога с добавлением префикса в виде текущей даты и времени
        String dirName = createDateTimePrefix() + "_" + createUniqueId();

        if (TextUtils.isEmpty(name)) {
            name = String.format("%s - %s", context.getString(R.string.title_new_record), 
                    Utils.dateToString(new Date(), "yyyy.MM.dd HH:mm:ss"));
        }

        TetroidRecord record = new TetroidRecord(false, id,
                name, null, null, url,
                new Date(), dirName, TetroidRecord.DEF_FILE_NAME, null);
        record.setIsNew(true);
        record.setIsTemp(true);

        // создаем каталог записи в корзине
        String dirPath = getPathToRecordFolderInTrash(record);
        if (checkRecordFolder(dirPath, true) <= 0) {
            return null;
        }
        // создаем файл записи (пустой)
        String filePath = dirPath + SEPAR + record.getFileName();
        Uri fileUri;
        try {
            fileUri = Uri.parse(filePath);
        } catch (Exception ex) {
            LogManager.log(context.getString(R.string.log_error_generate_record_file_path) + filePath, ex);
            return null;
        }
        File file = new File(fileUri.getPath());
        try {
            file.createNewFile();
        } catch (IOException ex) {
            LogManager.log(context.getString(R.string.log_error_creating_record_file) + filePath, ex);
            return null;
        }
        // текст записи
        if (!TextUtils.isEmpty(text)) {
            if (saveRecordHtmlText(record, text)) {
                record.setIsNew(false);
            } else {
                TetroidLog.logOperErrorMore(TetroidLog.Objs.RECORD, TetroidLog.Opers.SAVE, -1);
                return null;
            }
        }

        return record;
    }

    /**
     * Сохранение временной записи в дереве.
     * Хранилище уже должно быть загружено.
     * @param record
     * @param node
     */
    /*public static boolean saveTempRecord(TetroidRecord record, TetroidNode node) {
        if (record == null || node == null) {
            LogManager.emptyParams("DataManager.saveTempRecord()");
            return false;
        }
        if (Instance == null || !Instance.mIsStorageLoaded) {
            LogManager.log("Хранилище должно быть уже загружено");
            return false;
        }
        // добавляем запись в ветку (и соответственно, в дерево)
        node.addRecord(record);
        TetroidLog.logOperStart(TetroidLog.Objs.TEMP_RECORD, TetroidLog.Opers.SAVE);

        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
            // добавляем метки в запись и в коллекцию меток
            Instance.parseRecordTags(record, record.getTagsString());

            // вырезаем уникальную приставку в имени каталога
            String dirNameInBase = record.getDirName().substring(PREFIX_DATE_TIME_FORMAT.length() + 1);
            // перемещаем каталог записи из корзины
            return (moveRecordFolder(record, getPathToRecordFolderInTrash(record), getStoragePathBase(), dirNameInBase) > 0);
        } else {
            TetroidLog.logOperCancel(TetroidLog.Objs.TEMP_RECORD, TetroidLog.Opers.SAVE);
            // удаляем запись из ветки
            node.getRecords().remove(record);
            return false;
        }
    }*/

    /**
     * Удаление временной записи из дерева.
     * @param record
     */
    /*public static void deleteTempRecord(TetroidRecord record) {
        if (record == null) {
            LogManager.emptyParams("DataManager.deleteTempRecord()");
            return;
        }
        TetroidLog.logOperStart(TetroidLog.Objs.TEMP_RECORD, TetroidLog.Opers.DELETE);
        TetroidNode node = record.getNode();
        if (node != null) {
            node.getRecords().remove(record);
        }

        String dirPath = getPathToRecordFolderInTrash(record);
        File dir = new File(dirPath);
        String filePath = dirPath + SEPAR + record.getFileName();
        File file = new File(filePath);

        // удаляем файл записи
        file.delete();
        // удаляем каталог записи (пустой)
        dir.delete();
    }*/

    /**
     * Изменение свойств записи или сохранение временной записи.
     * @param record
     * @param name
     * @param tagsString
     * @param author
     * @param url
     * @param node
     * @param isFavor
     * @return
     */
    public static boolean editRecordFields(TetroidRecord record, String name, String tagsString,
                                           String author, String url, TetroidNode node, boolean isFavor) {
        if (record == null || node == null || TextUtils.isEmpty(name)) {
            LogManager.emptyParams("DataManager.editRecordFields()");
            return false;
        }
        boolean isTemp = record.isTemp();
        if (isTemp) {
            TetroidLog.logOperStart(TetroidLog.Objs.TEMP_RECORD, TetroidLog.Opers.SAVE, record);
        } else {
            TetroidLog.logOperStart(TetroidLog.Objs.RECORD_FIELDS, TetroidLog.Opers.CHANGE, record);
        }

        String oldName = record.getName(true);
        String oldAuthor = record.getAuthor(true);
        String oldTagsString = record.getTagsString(true);
        String oldUrl = record.getUrl(true);
        TetroidNode oldNode = record.getNode();
        boolean oldIsFavor = record.isFavorite();
        // обновляем поля
        boolean crypted = node.isCrypted();
        record.setName(encryptField(crypted, name));
        record.setTagsString(encryptField(crypted, tagsString));
        record.setAuthor(encryptField(crypted, author));
        record.setUrl(encryptField(crypted, url));
        record.setIsFavorite(isFavor);
        if (crypted) {
            record.setDecryptedValues(name, tagsString, author, url);
        }
        // обновляем ветку
        if (oldNode != node) {
            if (oldNode != null) {
                oldNode.deleteRecord(record);
            }
            node.addRecord(record);
        }

        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
            // удаляем пометку временной записи
            if (isTemp) {
                // вырезаем уникальную приставку в имени каталога
                String dirNameInBase = record.getDirName().substring(PREFIX_DATE_TIME_FORMAT.length() + 1);
                // перемещаем каталог записи из корзины
                if (moveRecordFolder(record, getPathToRecordFolderInTrash(record), getStoragePathBase(), dirNameInBase) <= 0) {
                    return false;
                }
                record.setIsTemp(false);
            }
            if (oldTagsString == null && tagsString != null
                    || oldTagsString != null && !oldTagsString.equals(tagsString)) {
                // удаляем старые метки
                Instance.deleteRecordTags(record);
                // добавляем новые метки
                Instance.parseRecordTags(record, tagsString);
            }
            if (App.isFullVersion()) {
                // добавляем/удаляем из избранного
                FavoritesManager.addOrRemove(record, isFavor);
            }
        } else {
            if (isTemp) {
                TetroidLog.logOperCancel(TetroidLog.Objs.TEMP_RECORD, TetroidLog.Opers.SAVE);
            } else {
                TetroidLog.logOperCancel(TetroidLog.Objs.RECORD_FIELDS, TetroidLog.Opers.CHANGE);
            }
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
                node.deleteRecord(record);
                if (oldNode != null) {
                    oldNode.addRecord(record);
                }
                if (App.isFullVersion()) {
                    FavoritesManager.addOrRemove(record, oldIsFavor);
                }
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
        TetroidLog.logOperStart(TetroidLog.Objs.RECORD, TetroidLog.Opers.INSERT, srcRecord);

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
//                    TetroidLog.logOperRes(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.COPY);
                        LogManager.log(String.format(context.getString(R.string.log_copy_record_dir_mask),
                                destDirPath), LogManager.Types.DEBUG);
                        // переименовываем прикрепленные файлы
                        renameRecordAttaches(srcRecord, record);
                    } else {
                        LogManager.log(String.format(context.getString(R.string.log_error_copy_record_dir_mask),
                                srcDirPath, destDirPath), LogManager.Types.ERROR);
                        return -2;
                    }
                } catch (IOException ex) {
                    LogManager.log(String.format(context.getString(R.string.log_error_copy_record_dir_mask),
                            srcDirPath, destDirPath), ex);
                    return -2;
                }
            }
        }

        // добавляем запись в ветку (и соответственно, в дерево)
        node.addRecord(record);
        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
            // добавляем в избранное обратно
            if (isCutted && srcRecord.isFavorite()) {
                FavoritesManager.add(record);
            }
            // добавляем метки в запись и в коллекцию меток
            Instance.parseRecordTags(record, tagsString);

            if (!withoutDir) {
                // зашифровываем или расшифровываем файл записи и прикрепленные файлы
                // FIXME: обрабатывать результат ?
//                File recordFile = new File(getPathToFileInRecordFolder(record, record.getFileName()));
//                cryptOrDecryptFile(recordFile, srcRecord.isCrypted(), crypted);
                Instance.cryptRecordFiles(record, srcRecord.isCrypted(), crypted);
            }
        } else {
            TetroidLog.logOperCancel(TetroidLog.Objs.RECORD, TetroidLog.Opers.INSERT);
            // удаляем запись из ветки
            node.getRecords().remove(record);

            if (!withoutDir) {
                if (isCutted) {
                    // перемещаем каталог записи обратно в корзину
                    return moveRecordFolder(record, destDirPath, SettingsManager.getTrashPath(), srcRecord.getDirName());
                } else {
                    // удаляем только что скопированный каталог записи
                    if (FileUtils.deleteRecursive(destDir)) {
                        TetroidLog.logOperRes(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE);
                    } else {
//                        LogManager.log(context.getString(R.string.log_error_del_record_dir) + destDirPath, LogManager.Types.ERROR);
                        TetroidLog.logOperError(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE,
                                ": " + destDirPath, false, -1);
                        return 0;
                    }
                }
            }
            return 0;
        }
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
        TetroidLog.logOperStart(TetroidLog.Objs.RECORD, (isCutting) ? TetroidLog.Opers.CUT : TetroidLog.Opers.DELETE, record);

        String dirPath = null;
        // проверяем существование каталога записи
        if (!withoutDir) {
            dirPath = (record.isTemp()) ? getPathToRecordFolderInTrash(record) : getPathToRecordFolder(record);
            int dirRes = checkRecordFolder(dirPath, false);
            if (dirRes <= 0) {
                return dirRes;
            }
        }

        // удаляем запись из ветки (и соответственно, из дерева)
        TetroidNode node = record.getNode();
        if (node != null) {
            if (!node.deleteRecord(record)) {
                LogManager.log(context.getString(R.string.log_not_found_record_in_node), LogManager.Types.ERROR);
                return 0;
            }
        } else {
            LogManager.log(context.getString(R.string.log_record_not_have_node), LogManager.Types.ERROR);
            return 0;
        }

        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
            // удаляем из избранного
            if (record.isFavorite()) {
                FavoritesManager.remove(record, false);
            }
            // перезагружаем список меток
            Instance.deleteRecordTags(record);
        } else {
            TetroidLog.logOperCancel(TetroidLog.Objs.RECORD, TetroidLog.Opers.DELETE);
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

    /**
     *
     * @param record
     * @param dirPath
     * @param movePath
     * @return
     */
    protected static int moveOrDeleteRecordFolder(TetroidRecord record, String dirPath, String movePath) {
        if (record == null || dirPath == null)
            return 0;
        if (movePath == null) {
            // удаляем каталог записи
            File dir = new File(dirPath);
            if (FileUtils.deleteRecursive(dir)) {
                TetroidLog.logOperRes(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE);
                return 1;
            } else {
                TetroidLog.logOperError(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE,
                        ": " + dirPath, false, -1);
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
    }

    /**
     * Удаление каталога записи.
     * @param record
     * @return
     */
    public boolean deleteRecordFolder(TetroidRecord record) {
        TetroidLog.logOperStart(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE, record);
        // проверяем существование каталога
        String dirPath = RecordsManager.getPathToRecordFolder(record);
        if (RecordsManager.checkRecordFolder(dirPath, false) <= 0) {
            return false;
        }
        File folder = new File(dirPath);
        // удаляем каталог
        if (!FileUtils.deleteRecursive(folder)) {
            TetroidLog.logOperError(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE,
                    ": " + dirPath, false, -1);
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
    public static boolean openRecordFolder(Context context, @NotNull TetroidRecord record) {
        if (context == null || record == null) {
            LogManager.emptyParams("RecordManager.openRecordFolder()");
            return false;
        }
        LogManager.log(context.getString(R.string.log_start_record_folder_opening) + record.getId(), LogManager.Types.DEBUG);
//        Uri uri = Uri.parse(getUriToRecordFolder(record));
        String fileFullName = getPathToRecordFolder(record);
        if (!openFile(context, new File(fileFullName))) {
            Utils.writeToClipboard(context, context.getString(R.string.title_record_folder_path), fileFullName);
            return false;
        }
        return true;
    }

    public static Date getEditedDate(Context context, TetroidRecord record) {
        if (context == null || record == null) {
            LogManager.emptyParams("RecordManager.getEditedDate()");
            return null;
        }
        String fileName = RecordsManager.getPathToFileInRecordFolder(record, record.getFileName());
        return getFileModifiedDate(context, fileName);
    }

    public static String getUriToRecordFolder(@NonNull TetroidRecord record) {
        return getStoragePathBaseUri() + SEPAR + record.getDirName() + SEPAR;
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

    public static TetroidRecord getRecord(String id) {
        TetroidRecordComparator comparator = new TetroidRecordComparator(TetroidRecord.FIELD_ID);
        return (App.IsLoadedFavoritesOnly)
                ? findRecord(FavoritesManager.getFavoritesRecords(), id, comparator)
                : findRecordInHierarchy(Instance.mRootNodesList, id, comparator);
    }

    public static TetroidRecord findRecordInHierarchy(List<TetroidNode> nodes, String fieldValue, TetroidRecordComparator comparator) {
        if (nodes == null || comparator == null)
            return null;
        TetroidRecord found;
        for (TetroidNode node : nodes) {
            if ((found = findRecord(node.getRecords(), fieldValue, comparator)) != null)
                return found;
            if (node.isExpandable()) {
                if ((found = findRecordInHierarchy(node.getSubNodes(), fieldValue, comparator)) != null)
                    return found;
            }
        }
        return null;
    }

    public static TetroidRecord findRecord(List<TetroidRecord> records, String fieldValue, TetroidRecordComparator comparator) {
        if (records == null)
            return null;
        for (TetroidRecord record : records) {
            if (comparator.compare(fieldValue, record))
                return record;
        }
        return null;
    }
}
