package com.gee12.mytetroid.interactors

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.widget.Toast
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.logs.TetroidLog
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.utils.FileUtils
import com.gee12.mytetroid.utils.Utils
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import java.util.*

/**
 * (вместо RecordsManager)
 */
class RecordsInteractor(
    val storageInteractor: StorageInteractor,
    val cryptInteractor: EncryptionInteractor,
    val dataInteractor: DataInteractor,
    val interactionInteractor: InteractionInteractor,
    val tagsParser: ITagsParser,
    val xmlLoader: TetroidXml
) {

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
    fun getRecordHtmlTextDecrypted(context: Context, record: TetroidRecord, duration: Int): String? {
//        if (record == null) {
//            LogManager.emptyParams(context, "DataManager.getRecordHtmlTextDecrypted()")
//            return null
//        }
        LogManager.log(context, context.getString(R.string.log_start_record_file_reading) + record.id, ILogger.Types.DEBUG)
        // проверка существования каталога записи
//        String dirPath = getPathToRecordFolderInBase(record);
        val dirPath = getPathToRecordFolder(context, record)
        if (checkRecordFolder(context, dirPath, true, duration) <= 0) {
            return null
        }
        val path = dirPath + Constants.SEPAR + record.fileName
        val uri: Uri = try {
            Uri.parse(path)
        } catch (ex: Exception) {
            LogManager.log(context, context.getString(R.string.log_error_generate_record_file_path) + path, ex)
            return null
        }
        // проверка существования файла записи
        val file = File(uri.path)
        if (!file.exists()) {
            LogManager.log(context, context.getString(R.string.log_record_file_is_missing), ILogger.Types.WARNING, duration)
            return null
        }
        var res: String? = null
        if (record.isCrypted) {
            if (record.isDecrypted) {
                val bytes: ByteArray? = try {
                    FileUtils.readFile(uri)
                } catch (ex: Exception) {
                    LogManager.log(context, context.getString(R.string.log_error_read_record_file) + path, ex)
                    return null
                }
                if (bytes == null) {
                    LogManager.log(context, context.getString(R.string.log_error_decrypt_record_file) + path, ILogger.Types.ERROR)
                    return null
                } else if (bytes.isEmpty()) {
                    // файл пуст
                    return ""
                }
                // расшифровываем содержимое файла
                LogManager.log(context, context.getString(R.string.log_start_record_text_decrypting), ILogger.Types.DEBUG)
                res = getCrypter().decryptText(bytes)
                if (res == null) {
                    LogManager.log(context, context.getString(R.string.log_error_decrypt_record_file) + path, ILogger.Types.ERROR)
                }
            }
        } else {
            try {
                res = FileUtils.readTextFile(uri)
            } catch (ex: Exception) {
                LogManager.log(context, context.getString(R.string.log_error_read_record_file) + path, ex)
            }
        }
        return res
    }

    /**
     * Получение содержимого записи в виде текста.
     * @param record
     * @return
     */
    fun getRecordTextDecrypted(context: Context, record: TetroidRecord): String? {
        var text: String? = null
        val html = getRecordHtmlTextDecrypted(context, record, -1)
        if (html != null) {
            try {
                text = Jsoup.parse(html).text()
            } catch (ex: java.lang.Exception) {
                LogManager.log(context, ex)
            }
        }
        return text
    }

    /**
     * Сохранение содержимого записи в файл.
     * @param record
     * @param htmlText
     * @return
     */
    fun saveRecordHtmlText(context: Context, record: TetroidRecord, htmlText: String): Boolean {
//        if (record == null) {
//            LogManager.emptyParams(context, "DataManager.saveRecordHtmlText()")
//            return false
//        }
        LogManager.log(context, context.getString(R.string.log_start_record_file_saving) + record.id, ILogger.Types.DEBUG)
        // проверка существования каталога записи
//        String dirPath = (record.isTemp()) ? getPathToRecordFolderInTrash(context, record) : getPathToRecordFolderInBase(record);
        val dirPath = getPathToRecordFolder(context, record)
        if (checkRecordFolder(context, dirPath, true, Toast.LENGTH_LONG) <= 0) {
            return false
        }
        // формирование пути к файлу записи
        val path = dirPath + Constants.SEPAR + record.fileName
        val uri: Uri = try {
            Uri.parse(path)
        } catch (ex: java.lang.Exception) {
            LogManager.log(context, context.getString(R.string.log_error_generate_record_file_path) + path, ex)
            return false
        }
        // запись файла, зашифровуя при необходимости
        try {
            if (record.isCrypted) {
                val res = getCrypter().encryptTextBytes(htmlText)
                FileUtils.writeFile(uri, res)
            } else {
                FileUtils.writeFile(uri, htmlText)
            }
        } catch (ex: IOException) {
            LogManager.log(context, context.getString(R.string.log_error_write_to_record_file) + path, ex)
            return false
        }
        return true
    }

    /**
     * Проверка существования каталога записи и создание при его отсутствии.
     * @param dirPath
     * @return 1 - успешно
     * 0 - ошибка
     * -1 - ошибка (отсутствует каталог записи)
     */
    fun checkRecordFolder(context: Context, dirPath: String, isCreate: Boolean): Int {
        return checkRecordFolder(context, dirPath, isCreate, LogManager.DURATION_NONE)
    }

    fun checkRecordFolder(context: Context, dirPath: String, isCreate: Boolean, duration: Int): Int {
        val folder = File(dirPath)
        try {
            if (!folder.exists()) {
                if (isCreate) {
                    LogManager.log(context, String.format(Locale.getDefault(), context.getString(R.string.log_create_record_dir), dirPath),ILogger.Types.WARNING)
                    return if (folder.mkdirs()) {
//                        LogManager.log(context.getString(R.string.log_record_dir_created), LogManager.Types.DEBUG, duration);
                        TetroidLog.logOperRes(context, TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.CREATE, "", duration)
                        1
                    } else {
                        LogManager.log(context, context.getString(R.string.log_create_record_dir_error), ILogger.Types.ERROR, duration)
                        0
                    }
                }
                return -1
            }
        } catch (ex: java.lang.Exception) {
            LogManager.log(context, context.getString(R.string.log_check_record_dir_error), ILogger.Types.ERROR, duration)
            return 0
        }
        return 1
    }

    /**
     * Перемещение или копирование записи в ветку.
     * @param srcRecord
     * @param node
     * @param isCutted
     * @return
     */
    suspend fun cloneRecordToNode(context: Context, srcRecord: TetroidRecord, node: TetroidNode,
                          isCutted: Boolean, breakOnFSErrors: Boolean): TetroidRecord? {
        TetroidLog.logOperStart(context, TetroidLog.Objs.RECORD, TetroidLog.Opers.INSERT, srcRecord)

        // генерируем уникальные идентификаторы, если запись копируется
        val id = if (isCutted) srcRecord.id else dataInteractor.createUniqueId()
        val dirName = if (isCutted) srcRecord.dirName else dataInteractor.createUniqueId()
        val name = srcRecord.name
        val tagsString = srcRecord.tagsString
        val author = srcRecord.author
        val url = srcRecord.url

        // создаем копию записи
        val crypted = node.isCrypted
        val record = TetroidRecord(
            crypted, id,
            encryptField(crypted, name),
            encryptField(crypted, tagsString),
            encryptField(crypted, author),
            encryptField(crypted, url),
            srcRecord.created, dirName, srcRecord.fileName, node
        )
        if (crypted) {
            record.setDecryptedValues(name, tagsString, author, url)
            record.setIsDecrypted(true)
        }
        if (isCutted) {
            record.setIsFavorite(srcRecord.isFavorite)
        }
        // добавляем прикрепленные файлы в запись
        cloneAttachesToRecord(srcRecord, record, isCutted)
        record.setIsNew(false)
        // добавляем запись в ветку (и соответственно, в дерево)
        node.addRecord(record)
        // добавляем в избранное обратно
        if (isCutted && record.isFavorite) {
            FavoritesManager.add(context, record)
        }
        // добавляем метки в запись и в коллекцию меток
        tagsParser.parseRecordTags(record, tagsString)
        val errorRes = if (breakOnFSErrors) null else record
        // проверяем существование каталога записи
        val srcDirPath = if (isCutted) {
            getPathToRecordFolderInTrash(context, srcRecord)
        } else {
            getPathToRecordFolder(context, srcRecord)
        }
        val dirRes = checkRecordFolder(context, srcDirPath, false)
        val srcDir = if (dirRes > 0) {
            File(srcDirPath)
        } else {
            return errorRes
        }
        val destDirPath = getPathToRecordFolder(context, record)
        val destDir = File(destDirPath)
        if (isCutted) {
            // вырезаем уникальную приставку в имени каталога
            val dirNameInBase = srcRecord.dirName.substring(DataInteractor.PREFIX_DATE_TIME_FORMAT.length + 1)
            // перемещаем каталог записи
            val res = moveRecordFolder(context, record, srcDirPath, storageInteractor.getPathToStorageBaseFolder(), dirNameInBase)
            if (res < 0) {
                return errorRes
            }
        } else {
            // копируем каталог записи
            try {
                if (FileUtils.copyDirRecursive(srcDir, destDir)) {
                    LogManager.log(context, String.format(context.getString(R.string.log_copy_record_dir_mask), destDirPath), ILogger.Types.ERROR)
                    // переименовываем прикрепленные файлы
                    renameRecordAttaches(context, srcRecord, record)
                } else {
                    LogManager.log(context, String.format(context.getString(R.string.log_error_copy_record_dir_mask), srcDirPath, destDirPath), ILogger.Types.ERROR)
                    return errorRes
                }
            } catch (ex: IOException) {
                LogManager.log(context, String.format(context.getString(R.string.log_error_copy_record_dir_mask), srcDirPath, destDirPath), ex)
                return errorRes
            }
        }

        // зашифровываем или расшифровываем файл записи
//        File recordFile = new File(getPathToFileInRecordFolder(record, record.getFileName()));
//        if (!cryptOrDecryptFile(recordFile, srcRecord.isCrypted(), crypted) && breakOnFSErrors) {
        return if (!cryptInteractor.cryptRecordFiles(context, record, srcRecord.isCrypted, crypted) && breakOnFSErrors) {
            errorRes
        } else record
    }

    /**
     * Перемещение или копирование прикрепленных файлов в другую запись.
     * @param srcRecord
     * @param destRecord
     * @param isCutted
     */
    private fun cloneAttachesToRecord(srcRecord: TetroidRecord, destRecord: TetroidRecord, isCutted: Boolean) {
        if (srcRecord.attachedFilesCount > 0) {
            val crypted = destRecord.isCrypted
            val attaches: MutableList<TetroidFile> = ArrayList()
            for (srcAttach in srcRecord.attachedFiles) {
                // генерируем уникальные идентификаторы, если запись копируется
                val id = if (isCutted) srcAttach.id else dataInteractor.createUniqueId()
                val name = srcAttach.name
                val attach = TetroidFile(
                    crypted, id,
                    encryptField(crypted, name), srcAttach.fileType, destRecord
                )
                if (crypted) {
                    attach.setDecryptedName(name)
                    attach.setIsCrypted(true)
                    attach.setIsDecrypted(true)
                }
                attaches.add(attach)
            }
            destRecord.attachedFiles = attaches
        }
    }

    /**
     * Переименование скопированных прикрепленных файлов в каталоге записи.
     * @param srcRecord
     * @param destRecord
     */
    private fun renameRecordAttaches(context: Context, srcRecord: TetroidRecord, destRecord: TetroidRecord) {
        for (i in 0 until srcRecord.attachedFilesCount) {
            val srcAttach = srcRecord.attachedFiles[i]
            val destAttach = destRecord.attachedFiles[i]
            val srcFileDisplayName = srcAttach.name
            val ext = FileUtils.getExtensionWithComma(srcFileDisplayName)
            val srcFileIdName = srcAttach.id + ext
            val destFileIdName = destAttach.id + ext
            // переименовываем
            val destPath = getPathToRecordFolder(context, destRecord)
            val srcFile = File(destPath, srcFileIdName)
            val destFile = File(destPath, destFileIdName)
            if (srcFile.renameTo(destFile)) {
                val to = Utils.getStringTo(context, destFile.absolutePath)
                //                LogManager.log(String.format(context.getString(R.string.log_rename_file_mask),
//                        destFile.getAbsolutePath()), LogManager.Types.DEBUG);
                TetroidLog.logOperRes(context, TetroidLog.Objs.FILE, TetroidLog.Opers.RENAME, to, -1)
            } else {
                val fromTo = Utils.getStringFromTo(context, srcFile.absolutePath, destFile.name)
                //                LogManager.log(String.format(context.getString(R.string.log_rename_file_error_mask),
//                        srcFile.getAbsolutePath(), destFile.getName()), LogManager.Types.ERROR);
                TetroidLog.logOperError(context, TetroidLog.Objs.FILE, TetroidLog.Opers.RENAME, fromTo, false, -1)
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
    suspend fun createRecord(context: Context, name: String, tagsString: String, author: String, url: String, node: TetroidNode, isFavor: Boolean): TetroidRecord? {
        if (TextUtils.isEmpty(name)) {
            LogManager.emptyParams(context, "DataManager.createRecord()")
            return null
        }
        TetroidLog.logOperStart(context, TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE)

        // генерируем уникальные идентификаторы
        val id = dataInteractor.createUniqueId()
        val dirName = dataInteractor.createUniqueId()
        val crypted = node.isCrypted
        val record = TetroidRecord(
            crypted, id,
            encryptField(crypted, name),
            encryptField(crypted, tagsString),
            encryptField(crypted, author),
            encryptField(crypted, url),
            Date(), dirName, TetroidRecord.DEF_FILE_NAME, node
        )
        if (crypted) {
            record.setDecryptedValues(name, tagsString, author, url)
            record.setIsDecrypted(true)
        }
        record.setIsFavorite(isFavor)
        record.setIsNew(true)
        // создаем каталог записи
        val dirPath = getPathToRecordFolder(context, record)
        if (checkRecordFolder(context, dirPath, true) <= 0) {
            return null
        }
        val dir = File(dirPath)
        // создаем файл записи (пустой)
        val filePath = dirPath + Constants.SEPAR + record.fileName
        val fileUri: Uri = try {
            Uri.parse(filePath)
        } catch (ex: java.lang.Exception) {
            LogManager.log(context, context.getString(R.string.log_error_generate_record_file_path) + filePath, ex)
            return null
        }
        val file = File(fileUri.path!!)
        try {
            file.createNewFile()
        } catch (ex: IOException) {
            LogManager.log(context, context.getString(R.string.log_error_creating_record_file) + filePath, ex)
            return null
        }

        // добавляем запись в ветку (и соответственно, в дерево)
        node.addRecord(record)
        // перезаписываем структуру хранилища в файл
        if (saveStorage(context)) {
            // добавляем метки в запись и в коллекцию меток
            tagsParser.parseRecordTags(record, tagsString)
            // добавляем в избранное
            if (isFavor) {
                FavoritesManager.add(context, record)
            }
        } else {
            TetroidLog.logOperCancel(context, TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE)
            // удаляем запись из ветки
            node.deleteRecord(record)
            // удаляем файл записи
            file.delete()
            // удаляем каталог записи (пустой)
            dir.delete()
            return null
        }
        return record
    }

    /**
     * Создание временной записи (без сохранения в дерево) при использовании виджета.
     * @return
     */
    suspend fun createTempRecord(context: Context, srcName: String?, url: String?, text: String?, node: TetroidNode): TetroidRecord? {
        var name = srcName
        TetroidLog.logOperStart(context, TetroidLog.Objs.TEMP_RECORD, TetroidLog.Opers.CREATE)
        // генерируем уникальный идентификатор
        val id = dataInteractor.createUniqueId()
        // имя каталога с добавлением префикса в виде текущей даты и времени
        val dirName = dataInteractor.createDateTimePrefix() + "_" + dataInteractor.createUniqueId()
        if (TextUtils.isEmpty(name)) {
//            name = String.format("%s - %s", context.getString(R.string.title_new_record),
//                    Utils.dateToString(new Date(), "yyyy.MM.dd HH:mm:ss"));
//            name = String.format(Locale.getDefault(), "%1$te %1$tb %1$tY %1$tR", new Date());
            name = String.format(Locale.getDefault(), "%1\$te %1\$tb %1\$tR", Date())
        }
//        val node = if (storageInteractor.quicklyNode != null) NodesManager.getQuicklyNode() else TetroidXml.ROOT_NODE
        val record = TetroidRecord(
            false, id,
            name, null, null, url,
            Date(), dirName, TetroidRecord.DEF_FILE_NAME, node
        )
        record.setIsNew(true)
        record.setIsTemp(true)

        // создаем каталог записи в корзине
        val dirPath = getPathToRecordFolderInTrash(context, record)
        if (checkRecordFolder(context, dirPath, true) <= 0) {
            return null
        }
        // создаем файл записи (пустой)
        val filePath = dirPath + Constants.SEPAR + record.fileName
        val fileUri: Uri = try {
            Uri.parse(filePath)
        } catch (ex: java.lang.Exception) {
            LogManager.log(context, context.getString(R.string.log_error_generate_record_file_path) + filePath, ex)
            return null
        }
        val file = File(fileUri.path!!)
        withContext(Dispatchers.IO) {
            try {
                file.createNewFile()
            } catch (ex: IOException) {
                LogManager.log(context, context.getString(R.string.log_error_creating_record_file) + filePath, ex)
                return@withContext null
            }
        }
        // текст записи
        if (!TextUtils.isEmpty(text)) {
            if (saveRecordHtmlText(context, record, text!!)) {
                record.setIsNew(false)
            } else {
                TetroidLog.logOperErrorMore(context, TetroidLog.Objs.RECORD, TetroidLog.Opers.SAVE, -1)
                return null
            }
        }
        // добавляем запись в дерево
        node.addRecord(record)
        return record
    }

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
    suspend fun editRecordFields(context: Context, record: TetroidRecord?, name: String?, tagsString: String?,
        author: String?, url: String?, node: TetroidNode?, isFavor: Boolean
    ): Boolean {
        if (record == null || node == null || TextUtils.isEmpty(name)) {
            LogManager.emptyParams(context, "DataManager.editRecordFields()")
            return false
        }
        val isTemp = record.isTemp
        if (isTemp) {
            TetroidLog.logOperStart(context, TetroidLog.Objs.TEMP_RECORD, TetroidLog.Opers.SAVE, record)
        } else {
            TetroidLog.logOperStart(context, TetroidLog.Objs.RECORD_FIELDS, TetroidLog.Opers.CHANGE, record)
        }
        val oldIsCrypted = record.isCrypted
        val oldName = record.getName(true)
        val oldAuthor = record.getAuthor(true)
        val oldTagsString = record.getTagsString(true)
        val oldUrl = record.getUrl(true)
        val oldNode = record.node
        val oldDirName = record.dirName
        val oldIsFavor = record.isFavorite
        // обновляем поля
        val crypted = node.isCrypted
        record.name = encryptField(crypted, name)
        record.tagsString = encryptField(crypted, tagsString)
        record.author = encryptField(crypted, author)
        record.url = encryptField(crypted, url)
        record.setIsCrypted(crypted)
        if (crypted) {
            record.setDecryptedValues(name, tagsString, author, url)
            record.setIsDecrypted(true)
        }
        record.setIsFavorite(isFavor)
        // обновляем ветку
        if (oldNode !== node) {
            oldNode?.deleteRecord(record)
            node.addRecord(record)
        }
        // удаляем пометку временной записи
        if (isTemp) {
            // вырезаем уникальную приставку в имени каталога
            val dirNameInBase = oldDirName.substring(DataInteractor.PREFIX_DATE_TIME_FORMAT.length + 1)
            // перемещаем каталог записи из корзины
            if (moveRecordFolder(
                    context,
                    record,
                    getPathToRecordFolderInTrash(context, record),
                    storageInteractor.getPathToStorageBaseFolder(),
                    dirNameInBase
                ) <= 0
            ) {
                return false
            }
            record.setIsTemp(false)
        }

        // перезаписываем структуру хранилища в файл
        if (saveStorage(context)) {
            if (oldTagsString == null && tagsString != null
                || oldTagsString != null && oldTagsString != tagsString
            ) {
                // удаляем старые метки
                tagsParser.deleteRecordTags(record)
                // добавляем новые метки
                tagsParser.parseRecordTags(record, tagsString)
            }
            if (App.isFullVersion()) {
                // добавляем/удаляем из избранного
                FavoritesManager.addOrRemove(context, record, isFavor)
            }
            // зашифровываем или расшифровываем файл записи и прикрепленные файлы
            // FIXME: обрабатывать результат ?
            cryptInteractor.cryptRecordFiles(context, record, oldIsCrypted, crypted)
        } else {
            if (isTemp) {
                TetroidLog.logOperCancel(context, TetroidLog.Objs.TEMP_RECORD, TetroidLog.Opers.SAVE)
            } else {
                TetroidLog.logOperCancel(context, TetroidLog.Objs.RECORD_FIELDS, TetroidLog.Opers.CHANGE)
            }
            // возвращаем изменения
            record.name = oldName
            record.tagsString = oldTagsString
            record.author = oldAuthor
            record.url = oldUrl
            if (crypted) {
                record.setDecryptedValues(
                    decryptField(crypted, oldName),
                    decryptField(crypted, oldTagsString),
                    decryptField(crypted, oldAuthor),
                    decryptField(crypted, url)
                )
            }
            node.deleteRecord(record)
            oldNode?.addRecord(record)
            if (App.isFullVersion()) {
                FavoritesManager.addOrRemove(context, record, oldIsFavor)
            }
            if (isTemp) {
                moveRecordFolder(
                    context,
                    record,
                    getPathToRecordFolder(context, record),
                    SettingsManager.getTrashPath(context),
                    oldDirName
                )
                record.setIsTemp(true)
            }
            return false
        }
        return true
    }

    /**
     * Удаление записи.
     * @param record
     * @param withoutDir Не пытаться удалить каталог записи
     * @return 1 - успешно
     * 0 - ошибка
     * -1 - ошибка (отсутствует каталог записи)
     */
    suspend fun deleteRecord(context: Context, record: TetroidRecord, withoutDir: Boolean): Int {
        return deleteRecord(context, record, withoutDir, SettingsManager.getTrashPath(context), false)
    }

    /**
     * Вырезание записи из ветки (добавление в "буфер обмена" и удаление).
     * @param record
     * @return 1 - успешно
     * 0 - ошибка
     * -1 - ошибка (отсутствует каталог записи)
     */
    suspend fun cutRecord(context: Context, record: TetroidRecord, withoutDir: Boolean): Int {
        return deleteRecord(context, record, withoutDir, SettingsManager.getTrashPath(context), true)
    }

    /**
     * Вставка записи в указанную ветку.
     * @param srcRecord
     * @param isCutted Если true, то запись была вырезана. Иначе - скопирована
     * @param node
     * @param withoutDir Не пытаться восстановить каталог записи
     * @return 1 - успешно
     * 0 - ошибка
     * -1 - ошибка (отсутствует каталог записи)
     * -2 - ошибка (не удалось вставить запись)
     */
    suspend fun insertRecord(context: Context, srcRecord: TetroidRecord, isCutted: Boolean, node: TetroidNode, withoutDir: Boolean): Int {
        TetroidLog.logOperStart(context, TetroidLog.Objs.RECORD, TetroidLog.Opers.INSERT, srcRecord)
        var srcDirPath = ""
        var srcDir: File? = null
        // проверяем существование каталога записи
        if (!withoutDir) {
            srcDirPath = if (isCutted) {
                getPathToRecordFolderInTrash(context, srcRecord)
            } else {
                getPathToRecordFolder(context, srcRecord)
            }
            val dirRes = checkRecordFolder(context, srcDirPath, false)
            srcDir = if (dirRes > 0) {
                File(srcDirPath)
            } else {
                return dirRes
            }
        }

        // генерируем уникальные идентификаторы, если запись копируется
        val id = if (isCutted) srcRecord.id else dataInteractor.createUniqueId()
        val dirName = if (isCutted) srcRecord.dirName else dataInteractor.createUniqueId()
        val name = srcRecord.name
        val tagsString = srcRecord.tagsString
        val author = srcRecord.author
        val url = srcRecord.url

        // создаем копию записи
        val crypted = node.isCrypted
        val record = TetroidRecord(
            crypted, id,
            encryptField(crypted, name),
            encryptField(crypted, tagsString),
            encryptField(crypted, author),
            encryptField(crypted, url),
            srcRecord.created, dirName, srcRecord.fileName, node
        )
        if (crypted) {
            record.setDecryptedValues(name, tagsString, author, url)
            record.setIsDecrypted(true)
        }
        // прикрепленные файлы
        cloneAttachesToRecord(srcRecord, record, isCutted)
        record.setIsNew(false)
        val destDirPath = getPathToRecordFolder(context, record)
        val destDir = File(destDirPath)
        if (!withoutDir) {
            if (isCutted) {
                // вырезаем уникальную приставку в имени каталога
                val dirNameInBase = srcRecord.dirName.substring(DataInteractor.PREFIX_DATE_TIME_FORMAT.length + 1)
                // перемещаем каталог записи
                val res = moveRecordFolder(context, record, srcDirPath, storageInteractor.getPathToStorageBaseFolder(), dirNameInBase)
                if (res < 0) {
                    return res
                }
            } else {
                // копируем каталог записи
                try {
                    if (FileUtils.copyDirRecursive(srcDir, destDir)) {
//                    TetroidLog.logOperRes(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.COPY);
                        LogManager.log(context, String.format(context.getString(R.string.log_copy_record_dir_mask), destDirPath), ILogger.Types.DEBUG)
                        // переименовываем прикрепленные файлы
                        renameRecordAttaches(context, srcRecord, record)
                    } else {
                        LogManager.log(context, String.format(context.getString(R.string.log_error_copy_record_dir_mask), srcDirPath, destDirPath), ILogger.Types.ERROR)
                        return -2
                    }
                } catch (ex: IOException) {
                    LogManager.log(context, String.format(context.getString(R.string.log_error_copy_record_dir_mask), srcDirPath, destDirPath), ex)
                    return -2
                }
            }
        }

        // добавляем запись в ветку (и соответственно, в дерево)
        node.addRecord(record)
        // перезаписываем структуру хранилища в файл
        if (saveStorage(context)) {
            // добавляем в избранное обратно
            if (isCutted && srcRecord.isFavorite) {
                FavoritesManager.add(context, record)
            }
            // добавляем метки в запись и в коллекцию меток
            tagsParser.parseRecordTags(record, tagsString)
            if (!withoutDir) {
                // зашифровываем или расшифровываем файл записи и прикрепленные файлы
                // FIXME: обрабатывать результат ?
//                File recordFile = new File(getPathToFileInRecordFolder(record, record.getFileName()));
//                cryptOrDecryptFile(recordFile, srcRecord.isCrypted(), crypted);
                cryptInteractor.cryptRecordFiles(context, record, srcRecord.isCrypted, crypted)
            }
        } else {
            TetroidLog.logOperCancel(context, TetroidLog.Objs.RECORD, TetroidLog.Opers.INSERT)
            // удаляем запись из ветки
            node.records.remove(record)
            if (!withoutDir) {
                if (isCutted) {
                    // перемещаем каталог записи обратно в корзину
                    return moveRecordFolder(context, record, destDirPath, SettingsManager.getTrashPath(context), srcRecord.dirName)
                } else {
                    // удаляем только что скопированный каталог записи
                    if (FileUtils.deleteRecursive(destDir)) {
                        TetroidLog.logOperRes(context, TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE)
                    } else {
//                        LogManager.log(context.getString(R.string.log_error_del_record_dir) + destDirPath, LogManager.Types.ERROR);
                        TetroidLog.logOperError(context, TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE,": $destDirPath", false, -1)
                        return 0
                    }
                }
            }
            return 0
        }
        return 1
    }

    /**
     * Удаление/вырезание записи из ветки.
     * @param record
     * @param withoutDir Нужно ли пропустить работу с каталогом записи
     * @param movePath Путь к каталогу, куда следует переместить каталог записи (не обязательно)
     * @param isCutting Если true, то запись вырезается, иначе - удаляется
     * @return 1 - успешно
     * 0 - ошибка
     * -1 - ошибка (отсутствует каталог записи)
     * -2 - ошибка (не удалось переместить каталог записи)
     */
    private suspend fun deleteRecord(context: Context, record: TetroidRecord, withoutDir: Boolean, movePath: String, isCutting: Boolean): Int {
        TetroidLog.logOperStart(context, TetroidLog.Objs.RECORD, if (isCutting) TetroidLog.Opers.CUT else TetroidLog.Opers.DELETE, record)
        var dirPath: String? = null
        // проверяем существование каталога записи
        if (!withoutDir) {
//            dirPath = (record.isTemp()) ? getPathToRecordFolderInTrash(context, record) : getPathToRecordFolderInBase(record);
            dirPath = getPathToRecordFolder(context, record)
            val dirRes = checkRecordFolder(context, dirPath, false)
            if (dirRes <= 0) {
                return dirRes
            }
        }

        // удаляем запись из ветки (и соответственно, из дерева)
        val node = record.node
        if (node != null) {
            if (!node.deleteRecord(record)) {
                LogManager.log(context, context.getString(R.string.log_not_found_record_in_node), ILogger.Types.ERROR)
                return 0
            }
        } else {
            LogManager.log(context, context.getString(R.string.log_record_not_have_node), ILogger.Types.ERROR)
            return 0
        }

        // перезаписываем структуру хранилища в файл
        if (saveStorage(context)) {
            // удаляем из избранного
            if (record.isFavorite) {
                FavoritesManager.remove(context, record, false)
            }
            // перезагружаем список меток
            tagsParser.deleteRecordTags(record)
        } else {
            TetroidLog.logOperCancel(context, TetroidLog.Objs.RECORD, TetroidLog.Opers.DELETE)
            return 0
        }
        if (!withoutDir) {
            val res = moveOrDeleteRecordFolder(context, record, dirPath!!, movePath)
            if (res <= 0) {
                return res
            }
        }
        return 1
    }

    /**
     *
     * @param record
     * @param dirPath
     * @param movePath
     * @return
     */
    suspend fun moveOrDeleteRecordFolder(context: Context, record: TetroidRecord, dirPath: String, movePath: String?): Int {
        return if (movePath == null) {
            // удаляем каталог записи
            val dir = File(dirPath)
            if (FileUtils.deleteRecursive(dir)) {
                TetroidLog.logOperRes(context, TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE)
                1
            } else {
                TetroidLog.logOperError(context, TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE,": $dirPath", false, -1)
                0
            }
        } else {
            // перемещаем каталог записи в корзину
            // с добавлением префикса в виде текущей даты и времени
            val newDirName = dataInteractor.createDateTimePrefix() + "_" + record.dirName
            moveRecordFolder(context, record, dirPath, movePath, newDirName)
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
    private suspend fun moveRecordFolder(context: Context, record: TetroidRecord, srcPath: String, destPath: String, newDirName: String?): Int {
        val res = dataInteractor.moveFile(context, srcPath, destPath, newDirName)
        if (res > 0 && newDirName != null) {
            // обновляем имя каталога для дальнейшей вставки
            record.dirName = newDirName
        }
        return res
    }

    /**
     * Удаление каталога записи.
     * @param record
     * @return
     */
    fun deleteRecordFolder(context: Context, record: TetroidRecord): Boolean {
        TetroidLog.logOperStart(context, TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE, record)
        // проверяем существование каталога
        val dirPath = getPathToRecordFolder(context, record)
        if (checkRecordFolder(context, dirPath, false) <= 0) {
            return false
        }
        val folder = File(dirPath)
        // удаляем каталог
        if (!FileUtils.deleteRecursive(folder)) {
            TetroidLog.logOperError(context, TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.DELETE,": $dirPath", false, -1)
            return false
        }
        return true
    }

    /**
     * Открытие каталога записи.
     * @param context
     * @param record
     * @return
     */
    fun openRecordFolder(context: Context?, record: TetroidRecord): Boolean {
        if (context == null) {
            LogManager.emptyParams(context, "RecordManager.openRecordFolder()")
            return false
        }
        LogManager.log(context, context.getString(R.string.log_start_record_folder_opening) + record.id, ILogger.Types.DEBUG)
        //        Uri uri = Uri.parse(getUriToRecordFolder(record));
        val fileFullName = getPathToRecordFolder(context, record)
        if (!interactionInteractor.openFile(context, File(fileFullName))) {
            Utils.writeToClipboard(context, context.getString(R.string.title_record_folder_path), fileFullName)
            return false
        }
        return true
    }

    /**
     * Получение даты последнего изменения записи.
     * @param context
     * @param record
     * @return
     */
    fun getEditedDate(context: Context?, record: TetroidRecord): Date? {
        if (context == null) {
            LogManager.emptyParams(context, "RecordManager.getEditedDate()")
            return null
        }
        if (record.isNew || record.isTemp) {
            return null
        }
        val fileName = getPathToFileInRecordFolder(context, record, record.fileName)
        return FileUtils.getFileModifiedDate(context, fileName)
    }

//    /**
//     * Получение пути к каталогу записи в виде Uri,
//     * с учетом того, что хранилище еще может быть не загружено.
//     * Запись находится в хранилище в каталоге base/.
//     * @param record
//     * @return
//     */
//    @Deprecated("")
//    fun getUriToRecordFolder(record: TetroidRecord): String? {
//        return if (DataManager.isLoaded()) DataManager.getStoragePathBaseUri().toString() + Constants.SEPAR + record.dirName + Constants.SEPAR else null
//    }

    /**
     * Получение пути к каталогу записи в виде Uri, с учетом того, что это может быть временная запись.
     * Запись может находиться в хранилище в каталоге base/ или в каталоге корзины.
     * @param context
     * @param record
     * @return
     */
    fun getUriToRecordFolder(context: Context, record: TetroidRecord): String {
//        return (isLoaded()) ? getStoragePathBaseUri() + SEPAR + record.getDirName() + SEPAR
//                : null;
        return (if (record.isTemp) storageInteractor.getTrashPathBaseUri(context) else storageInteractor.getPathToStorageBaseFolder())
            .toString() + Constants.SEPAR + record.dirName + Constants.SEPAR
    }

//    /**
//     * Получение пути к каталогу записи.
//     * Запись находится в хранилище в каталоге base/.
//     * @param record
//     * @return
//     */
//    @Deprecated("")
//    fun getPathToRecordFolderInBase(record: TetroidRecord): String {
//        return DataManager.getStoragePathBase() + Constants.SEPAR + record.dirName
//    }

    /**
     * Получение пути к каталогу записи, с учетом того, что это может быть временная запись.
     * Запись может находиться в хранилище в каталоге base/ или в каталоге корзины.
     * @param context
     * @param record
     * @return
     */
    fun getPathToRecordFolder(context: Context, record: TetroidRecord): String {
        return (if (record.isTemp) SettingsManager.getTrashPath(context) else storageInteractor.getPathToStorageBaseFolder()) +
                Constants.SEPAR + record.dirName
    }

    fun getPathToRecordFolderInTrash(context: Context, record: TetroidRecord): String {
        return SettingsManager.getTrashPath(context) + Constants.SEPAR + record.dirName
    }

//    @Deprecated("")
//    fun getPathToFileInRecordFolderInBase(record: TetroidRecord, fileName: String): String? {
//        return getPathToRecordFolderInBase(record) + Constants.SEPAR + fileName
//    }

    fun getPathToFileInRecordFolder(context: Context, record: TetroidRecord, fileName: String): String {
        return getPathToRecordFolder(context, record) + Constants.SEPAR + fileName
    }

    fun getRecord(id: String?): TetroidRecord? {
        val comparator = TetroidRecordComparator(TetroidRecord.FIELD_ID)
        return findRecordInHierarchy(id, comparator)
//        return (App.IsLoadedFavoritesOnly)
//                ? findRecord(FavoritesManager.getFavoritesRecords(), id, comparator)
//                : findRecordInHierarchy(Instance.getRootNodes(), id, comparator);
    }

    fun findRecordInHierarchy(fieldValue: String?, comparator: TetroidRecordComparator): TetroidRecord? {
        var found: TetroidRecord?
        if (findRecord(TetroidXml.ROOT_NODE.records, fieldValue, comparator).also { found = it } != null)
            return found
        return if (xmlLoader.mIsFavoritesMode) findRecord(FavoritesManager.getFavoritesRecords(), fieldValue, comparator)
        else findRecordInHierarchy(storageInteractor.getRootNodes(), fieldValue, comparator)
    }

    fun findRecordInHierarchy(nodes: List<TetroidNode>?, fieldValue: String?, comparator: TetroidRecordComparator?): TetroidRecord? {
        if (nodes == null || comparator == null) return null
        var found: TetroidRecord?
        for (node in nodes) {
            if (findRecord(node.records, fieldValue, comparator).also { found = it } != null)
                return found
            if (node.isExpandable) {
                if (findRecordInHierarchy(node.subNodes, fieldValue, comparator).also { found = it } != null)
                    return found
            }
        }
        return null
    }

    fun findRecord(records: List<TetroidRecord?>?, fieldValue: String?, comparator: TetroidRecordComparator): TetroidRecord? {
        if (records == null) return null
        for (record in records) {
            if (comparator.compare(fieldValue, record)) return record
        }
        return null
    }

    suspend fun saveStorage(context: Context) = storageInteractor.saveStorage(context)

    fun encryptField(isCrypted: Boolean, field: String?): String? {
        return if (isCrypted) getCrypter().encryptTextBase64(field) else field
    }

    fun decryptField(isCrypted: Boolean, field: String?): String? {
        return if (isCrypted) getCrypter().decryptBase64(field) else field
    }

    fun getCrypter() = cryptInteractor.crypter
}