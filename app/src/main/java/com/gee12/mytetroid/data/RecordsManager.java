package com.gee12.mytetroid.data;

import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.TetroidLog;
import com.gee12.mytetroid.crypt.CryptManager;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.FileUtils;
import com.gee12.mytetroid.utils.Utils;

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
     * Получение содержимого записи в виде "сырого" html.
     * @param record
     * @return
     */
    public static String getRecordHtmlTextDecrypted(@NonNull TetroidRecord record) {
        if (record == null) {
            LogManager.emptyParams("DataManager.getRecordHtmlTextDecrypted()");
            return null;
        }
        LogManager.log(context.getString(R.string.log_start_record_file_reading) + record.getId(), LogManager.Types.DEBUG);
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
            LogManager.log(context.getString(R.string.log_error_generate_record_file_path) + path, ex);
            return null;
        }
        // проверка существования файла записи
        File file = new File(uri.getPath());
        if (!file.exists()) {
            LogManager.log(context.getString(R.string.log_record_file_is_missing), LogManager.Types.WARNING, Toast.LENGTH_LONG);
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
                        TetroidLog.addOperResLog(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.CREATE, duration);
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
                LogManager.log(String.format(context.getString(R.string.log_rename_file_mask),
                        destFile.getAbsolutePath()), LogManager.Types.DEBUG);
            } else {
                LogManager.log(String.format(context.getString(R.string.log_rename_file_error_mask),
                        srcFile.getAbsolutePath(), destFile.getName()), LogManager.Types.ERROR);
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
    public static TetroidRecord createRecord(String name, String tagsString, String author, String url, TetroidNode node) {
        if (node == null || TextUtils.isEmpty(name)) {
            LogManager.emptyParams("DataManager.createRecord()");
            return null;
        }
//        LogManager.log(context.getString(R.string.log_start_record_creating), LogManager.Types.DEBUG);
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
//            LogManager.log(context.getString(R.string.log_cancel_record_creating), LogManager.Types.ERROR);
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
            LogManager.log("В хранилище не загружено ни одной ветки", LogManager.Types.ERROR);
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
                        LogManager.log(context.getString(R.string.log_error_del_record_dir) + destDirPath, LogManager.Types.ERROR);
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
                LogManager.log(context.getString(R.string.log_not_found_record_in_node), LogManager.Types.ERROR);
                return 0;
            }
        } else {
            LogManager.log(context.getString(R.string.log_record_not_have_node), LogManager.Types.ERROR);
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
//            LogManager.log(context.getString(R.string.log_cancel_record_deleting), LogManager.Types.ERROR);
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

    protected static int moveOrDeleteRecordFolder(TetroidRecord record, String dirPath, String movePath) {
        if (record == null || dirPath == null)
            return 0;
        if (movePath == null) {
            // удаляем каталог записи
            File dir = new File(dirPath);
            if (FileUtils.deleteRecursive(dir)) {
                TetroidLog.addOperResLog(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE);
                return 1;
            } else {
                LogManager.log(context.getString(R.string.log_error_del_record_dir) + dirPath, LogManager.Types.ERROR);
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
            LogManager.log(String.format(context.getString(R.string.log_error_move_record_dir_mask),
                    srcPath, destPath), LogManager.Types.ERROR);
            return -2;
        }

        if (newDirName == null) {
            String destDirPath = destDir.getAbsolutePath() + File.separator + record.getDirName();
            LogManager.log(String.format(context.getString(R.string.log_record_folder_moved_mask),
                    destDirPath), LogManager.Types.DEBUG);
        } else {
            // добавляем к имени каталога записи уникальную приставку
            srcDir = new File(destPath, record.getDirName());
            destDir = new File(destPath, newDirName);
            if (srcDir.renameTo(destDir)) {
                LogManager.log(String.format(context.getString(R.string.log_record_folder_moved_mask),
                        destDir.getAbsolutePath()), LogManager.Types.DEBUG);
                // обновляем имя каталога для дальнейшей вставки
                record.setDirName(newDirName);
            } else {
                LogManager.log(String.format(context.getString(R.string.log_error_move_record_dir_mask),
                        srcDir.getAbsolutePath(), destDir.getAbsolutePath()), LogManager.Types.ERROR);
                return -2;
            }
        }
        return 1;*/
    }

    /**
     * Удаление каталога записи.
     * @param record
     * @return
     */
    public boolean deleteRecordFolder(TetroidRecord record) {
        TetroidLog.addOperStartLog(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE);
        // проверяем существование каталога
        String dirPath = RecordsManager.getPathToRecordFolder(record);
        if (RecordsManager.checkRecordFolder(dirPath, false) <= 0) {
            return false;
        }
        File folder = new File(dirPath);
        // удаляем каталог
        if (!FileUtils.deleteRecursive(folder)) {
            LogManager.log(context.getString(R.string.log_error_del_record_dir) + dirPath, LogManager.Types.ERROR);
            return false;
        }
        return true;
    }

    public static String getRecordDirUri(@NonNull TetroidRecord record) {
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
        return getRecordInHierarchy(instance.mRootNodesList, id, new TetroidRecordComparator(TetroidRecord.FIELD_ID));
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
}
