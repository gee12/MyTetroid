package com.gee12.mytetroid.data;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.logs.TetroidLog;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.FileUtils;
import com.gee12.mytetroid.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class AttachesManager extends DataManager {

    /**
     * Открытие прикрепленного файла сторонним приложением.
     * @param context
     * @param file
     * @return
     */
    @RequiresPermission(WRITE_EXTERNAL_STORAGE)
    public static boolean openAttach(Context context, @NonNull TetroidFile file) {
        if (context == null || file == null) {
            LogManager.emptyParams(context, "AttachesManager.openAttach()");
            return false;
        }
        LogManager.log(context, context.getString(R.string.log_start_attach_file_opening) + file.getId(), ILogger.Types.DEBUG);
        TetroidRecord record = file.getRecord();
        String fileDisplayName = file.getName();
        String ext = FileUtils.getExtensionWithComma(fileDisplayName);
        String fileIdName = file.getId() + ext;
        String fullFileName = RecordsManager.getPathToFileInRecordFolderInBase(record, fileIdName);
        File srcFile;
        try {
            srcFile = new File(fullFileName);
        } catch (Exception ex) {
            LogManager.log(context, context.getString(R.string.log_error_file_open) + fullFileName,
                    ILogger.Types.ERROR, Toast.LENGTH_LONG);
            LogManager.log(context, ex);
            return false;
        }
        //
        LogManager.log(context, context.getString(R.string.log_open_file) + fullFileName);
        if (!srcFile.exists()) {
            LogManager.log(context, context.getString(R.string.log_file_is_absent) + fullFileName, Toast.LENGTH_SHORT);
            return false;
        }
        // если запись зашифрована
        if (record.isCrypted() && SettingsManager.isDecryptFilesInTempDef(context)) {
            // создаем временный файл
//                File tempFile = createTempCacheFile(context, fileIdName);
//                File tempFile = new File(String.format("%s%s/_%s", getStoragePathBase(), record.getDirName(), fileIdName));
//                File tempFile = createTempExtStorageFile(context, fileIdName);
//                String tempFolderPath = SettingsManager.getTrashPath() + SEPAR + record.getDirName();
            String tempFolderPath = RecordsManager.getPathToRecordFolderInTrash(context, record);
            File tempFolder = new File(tempFolderPath);
            if (!tempFolder.exists() && !tempFolder.mkdirs()) {
                LogManager.log(context, context.getString(R.string.log_could_not_create_temp_dir) + tempFolderPath, Toast.LENGTH_LONG);
            }
            File tempFile = new File(tempFolder, fileIdName);

            // расшифровываем во временный файл
            try {
                if ((tempFile.exists() || tempFile.createNewFile())
                        && Instance.mCrypter.encryptDecryptFile(srcFile, tempFile, false)) {
                    srcFile = tempFile;
                } else {
                    LogManager.log(context, context.getString(R.string.log_could_not_decrypt_file) + fullFileName, Toast.LENGTH_LONG);
                    return false;
                }
            } catch (IOException ex) {
                LogManager.log(context, context.getString(R.string.log_file_decryption_error) + ex.getMessage(), Toast.LENGTH_LONG);
                return false;
            }
        }
        return openFile(context, srcFile);
    }

    public static TetroidFile attachFile(Context context, String fullName, TetroidRecord record, boolean deleteSrcFile) {
        TetroidFile res = attachFile(context, fullName, record);
        if (deleteSrcFile && res != null) {
            File srcFile = new File(fullName);
            if (!FileUtils.deleteRecursive(srcFile)) {
                LogManager.log(context, context.getString(R.string.log_error_delete_file) + fullName, ILogger.Types.ERROR);
            }
        }
        return res;
    }

    /**
     * Прикрепление нового файла к записи.
     * @param fullName
     * @param record
     * @return
     */
    public static TetroidFile attachFile(Context context, String fullName, TetroidRecord record) {
        if (record == null || TextUtils.isEmpty(fullName)) {
            LogManager.emptyParams(context, "AttachesManager.attachFile()");
            return null;
        }
        TetroidLog.logOperStart(context, TetroidLog.Objs.FILE, TetroidLog.Opers.ATTACH, ": " + fullName);

        String id = createUniqueId();
        // проверка исходного файла
        File srcFile = new File(fullName);
        try {
            if (!srcFile.exists()) {
                LogManager.log(context, context.getString(R.string.log_file_is_absent) + fullName, ILogger.Types.ERROR);
                return null;
            }
        } catch (Exception ex) {
            LogManager.log(context, context.getString(R.string.log_file_checking_error) + fullName, ex);
            return null;
        }

        String fileDisplayName = srcFile.getName();
        String ext = FileUtils.getExtensionWithComma(fileDisplayName);
        String fileIdName = id + ext;
        // создание объекта хранилища
        boolean crypted = record.isCrypted();
        TetroidFile file = new TetroidFile(crypted, id,
                Instance.encryptField(crypted, fileDisplayName),
                TetroidFile.DEF_FILE_TYPE, record);
        if (crypted) {
            file.setDecryptedName(fileDisplayName);
            file.setIsDecrypted(true);
        }
        // проверка каталога записи
        String dirPath = RecordsManager.getPathToRecordFolderInBase(record);
        if (RecordsManager.checkRecordFolder(context, dirPath, true, Toast.LENGTH_LONG) <= 0) {
            return null;
        }
        // формируем путь к файлу назначения в каталоге записи
        String destFilePath = dirPath + SEPAR + fileIdName;
        Uri destFileUri;
        try {
            destFileUri = Uri.parse(destFilePath);
        } catch (Exception ex) {
            LogManager.log(context, context.getString(R.string.log_error_generate_file_path) + destFilePath, ex);
            return null;
        }
        // копирование файла в каталог записи, зашифровуя при необходимости
        File destFile = new File(destFileUri.getPath());
        String fromTo = getStringFromTo(context, fullName, destFilePath);
        try {
            if (record.isCrypted()) {
                TetroidLog.logOperStart(context, TetroidLog.Objs.FILE, TetroidLog.Opers.ENCRYPT);
                if (!Instance.mCrypter.encryptDecryptFile(srcFile, destFile, true)) {
                    TetroidLog.logOperError(context, TetroidLog.Objs.FILE, TetroidLog.Opers.ENCRYPT,
                            fromTo, false, -1);
                    return null;
                }
            } else {
                TetroidLog.logOperStart(context, TetroidLog.Objs.FILE, TetroidLog.Opers.COPY);
                if (!FileUtils.copyFile(srcFile, destFile)) {
                    TetroidLog.logOperError(context, TetroidLog.Objs.FILE, TetroidLog.Opers.COPY,
                            fromTo, false, -1);
                    return null;
                }
            }
        } catch (IOException ex) {
            TetroidLog.logOperError(context, TetroidLog.Objs.FILE, (record.isCrypted()) ? TetroidLog.Opers.ENCRYPT : TetroidLog.Opers.COPY,
                    fromTo, false, -1);
            return null;
        }

        // добавляем файл к записи (и соответственно, в дерево)
        List<TetroidFile> files = record.getAttachedFiles();
        if (files == null) {
            files = new ArrayList<>();
            record.setAttachedFiles(files);
        }
        files.add(file);
        // перезаписываем структуру хранилища в файл
        if (Instance.saveStorage(context)) {
         /*   instance.mFilesCount++;*/
        } else {
            TetroidLog.logOperCancel(context, TetroidLog.Objs.FILE, TetroidLog.Opers.ATTACH);
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
    public static int editAttachedFileFields(Context context, TetroidFile file, String name) {
        if (file == null || TextUtils.isEmpty(name)) {
            LogManager.emptyParams(context, "AttachesManager.editAttachedFileFields()");
            return 0;
        }
        TetroidLog.logOperStart(context, TetroidLog.Objs.FILE_FIELDS, TetroidLog.Opers.CHANGE, file);

        TetroidRecord record = file.getRecord();
        if (record == null) {
            LogManager.log(context, context.getString(R.string.log_file_record_is_null), ILogger.Types.ERROR);
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
            dirPath = RecordsManager.getPathToRecordFolderInBase(record);
            int dirRes = RecordsManager.checkRecordFolder(context, dirPath, false);
            if (dirRes <= 0) {
                return dirRes;
            }
            // проверяем существование самого файла
            String fileIdName = file.getId() + ext;
            filePath = dirPath + SEPAR + fileIdName;
            srcFile = new File(filePath);
            if (!srcFile.exists()) {
                LogManager.log(context, context.getString(R.string.log_file_is_missing) + filePath, ILogger.Types.ERROR);
                return -2;
            }
        }

        String oldName = file.getName(true);
        // обновляем поля
        boolean crypted = file.isCrypted();
        file.setName(Instance.encryptField(crypted, name));
        if (crypted) {
            file.setDecryptedName(name);
        }

        // перезаписываем структуру хранилища в файл
        if (!Instance.saveStorage(context)) {
            TetroidLog.logOperCancel(context, TetroidLog.Objs.FILE_FIELDS, TetroidLog.Opers.CHANGE);
            // возвращаем изменения
            file.setName(oldName);
            if (crypted) {
                file.setDecryptedName(Instance.decryptField(crypted, oldName));
            }
            return 0;
        }
        // меняем расширение, если изменилось
        if (isExtChanged) {
            String newFileIdName = file.getId() + newExt;
            String newFilePath = dirPath + SEPAR + newFileIdName;
            File destFile = new File(newFilePath);
            String fromTo = getStringFromTo(context, filePath, newFileIdName);
            if (srcFile.renameTo(destFile)) {
                TetroidLog.logOperRes(context, TetroidLog.Objs.FILE, TetroidLog.Opers.RENAME, fromTo, -1);
            } else {
                    TetroidLog.logOperError(context, TetroidLog.Objs.FILE, TetroidLog.Opers.RENAME, fromTo, false, -1);
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
    public static int deleteAttachedFile(Context context, TetroidFile file, boolean withoutFile) {
        if (file == null) {
            LogManager.emptyParams(context, "AttachesManager.deleteAttachedFile()");
            return 0;
        }
        TetroidLog.logOperStart(context, TetroidLog.Objs.FILE, TetroidLog.Opers.DELETE, file);

        TetroidRecord record = file.getRecord();
        if (record == null) {
            LogManager.log(context, context.getString(R.string.log_file_record_is_null), ILogger.Types.ERROR);
            return 0;
        }

        String dirPath;
        String destFilePath = null;
        File destFile = null;
        if (!withoutFile) {
            // проверяем существование каталога записи
            dirPath = RecordsManager.getPathToRecordFolderInBase(record);
            int dirRes = RecordsManager.checkRecordFolder(context, dirPath, false);
            if (dirRes <= 0) {
                return dirRes;
            }
            // проверяем существование самого файла
            String ext = FileUtils.getExtensionWithComma(file.getName());
            String fileIdName = file.getId() + ext;
            destFilePath = dirPath + SEPAR + fileIdName;
            destFile = new File(destFilePath);
            if (!destFile.exists()) {
                LogManager.log(context, context.getString(R.string.log_file_is_missing) + destFilePath, ILogger.Types.ERROR);
                return -2;
            }
        }

        // удаляем файл из списка файлов записи (и соответственно, из дерева)
        List<TetroidFile> files = record.getAttachedFiles();
        if (files != null) {
            if (!files.remove(file)) {
                LogManager.log(context, context.getString(R.string.log_not_found_file_in_record), ILogger.Types.ERROR);
                return 0;
            }
        } else {
            LogManager.log(context, context.getString(R.string.log_record_not_have_attached_files), ILogger.Types.ERROR);
            return 0;
        }

        // перезаписываем структуру хранилища в файл
        if (!Instance.saveStorage(context)) {
            TetroidLog.logOperCancel(context, TetroidLog.Objs.FILE, TetroidLog.Opers.DELETE);
            return 0;
        }

        // удаляем сам файл
        if (!withoutFile) {
            if (!FileUtils.deleteRecursive(destFile)) {
                LogManager.log(context, context.getString(R.string.log_error_delete_file) + destFilePath, ILogger.Types.ERROR);
                return 0;
            }
        }
        return 1;
    }

    /**
     * Сохранение прикрепленного файла по указанному пути.
     * @param file
     * @param destPath
     * @return
     */
    public static boolean saveFile(Context context, TetroidFile file, String destPath) {
        if (file == null || TextUtils.isEmpty(destPath)) {
            LogManager.emptyParams(context, "AttachesManager.saveFile()");
            return false;
        }
        String mes = TetroidLog.getIdString(context, file) + DataManager.getStringTo(context, destPath);
        TetroidLog.logOperStart(context, TetroidLog.Objs.FILE, TetroidLog.Opers.SAVE, ": " + mes);

        // проверка исходного файла
        String fileIdName = file.getIdName();
        String recordPath = RecordsManager.getPathToRecordFolderInBase(file.getRecord());
        File srcFile = new File(recordPath, fileIdName);
        try {
            if (!srcFile.exists()) {
                LogManager.log(context, context.getString(R.string.log_file_is_absent) + fileIdName, ILogger.Types.ERROR);
                return false;
            }
        } catch (Exception ex) {
            LogManager.log(context, context.getString(R.string.log_file_checking_error) + fileIdName, ex);
            return false;
        }
        // копирование файла в указанный каталог, расшифровуя при необходимости
        File destFile = new File(destPath, file.getName());
        String fromTo = getStringFromTo(context, srcFile.getAbsolutePath(), destFile.getAbsolutePath());
        try {
            if (file.isCrypted()) {
                TetroidLog.logOperStart(context, TetroidLog.Objs.FILE, TetroidLog.Opers.DECRYPT);
                if (!Instance.mCrypter.encryptDecryptFile(srcFile, destFile, false)) {
                    TetroidLog.logOperError(context, TetroidLog.Objs.FILE, TetroidLog.Opers.DECRYPT,
                            fromTo, false, -1);
                    return false;
                }
            } else {
                TetroidLog.logOperStart(context, TetroidLog.Objs.FILE, TetroidLog.Opers.COPY);
                if (!FileUtils.copyFile(srcFile, destFile)) {
                    TetroidLog.logOperError(context, TetroidLog.Objs.FILE, TetroidLog.Opers.COPY,
                            fromTo, false, -1);
                    return false;
                }
            }
        } catch (IOException ex) {
            TetroidLog.logOperError(context, TetroidLog.Objs.FILE,
                    (file.isCrypted()) ? TetroidLog.Opers.DECRYPT : TetroidLog.Opers.COPY,
                    fromTo, false, -1);
            return false;
        }
        return true;
    }

    /**
     * Получение полного имени файла.
     * @param attach
     * @return
     */
    public static String getAttachFullName(Context context, TetroidFile attach) {
        if (attach == null) {
            return null;
        }
        TetroidRecord record = attach.getRecord();
        if (record == null) {
            LogManager.log(context, context.getString(R.string.log_file_record_is_null), ILogger.Types.ERROR);
            return null;
        }
        String ext = FileUtils.getExtensionWithComma(attach.getName());
        return RecordsManager.getPathToFileInRecordFolder(context, record, attach.getId() + ext);
    }

    /**
     * Получение размера прикрепленного файла.
     * @param context
     * @param attach
     * @return
     */
    public static String getAttachedFileSize(Context context, TetroidFile attach) {
        if (context == null || attach == null) {
            LogManager.emptyParams(context, "AttachesManager.getAttachedFileSize()");
            return null;
        }
        return getFileSize(context, getAttachFullName(context, attach));
    }

    /**
     * Получение даты последнего изменения файла.
     * @param context
     * @param attach
     * @return
     */
    public static Date getEditedDate(Context context, TetroidFile attach) {
        if (context == null || attach == null) {
            LogManager.emptyParams(context, "RecordManager.getEditedDate()");
            return null;
        }
        return getFileModifiedDate(context, getAttachFullName(context, attach));
    }

}
