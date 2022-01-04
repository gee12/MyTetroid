package com.gee12.mytetroid.interactors

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants.SEPAR
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.data.xml.TetroidXml
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.utils.FileUtils
import com.gee12.mytetroid.utils.StringUtils
import com.gee12.mytetroid.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Создается для конкретного хранилища.
 */
class RecordsInteractor(
    private val logger: ITetroidLogger,
    private val storageInteractor: StorageInteractor,
    private val cryptInteractor: EncryptionInteractor,
    private val dataInteractor: DataInteractor,
    private val interactionInteractor: InteractionInteractor,
    private val tagsParser: ITagsParser,
    private val favoritesInteractor: FavoritesInteractor,
    private val xmlLoader: TetroidXml
) {

    /**
     * Получение содержимого записи в виде "сырого" html.
     * @param record
     * @return
     */
    suspend fun getRecordHtmlTextDecrypted(context: Context, record: TetroidRecord, showMessage: Boolean): String? {
        logger.logDebug(context.getString(R.string.log_start_record_file_reading) + record.id)
        // проверка существования каталога записи
        val dirPath = getPathToRecordFolder(record)
        if (checkRecordFolder(context, dirPath, true, showMessage) <= 0) {
            return null
        }
        val path = dirPath + SEPAR + record.fileName
        val uri: Uri = try {
            Uri.parse(path)
        } catch (ex: Exception) {
            logger.logError(context.getString(R.string.log_error_generate_record_file_path) + path, ex)
            return null
        }
        // проверка существования файла записи
        val file = File(uri.path)
        if (!file.exists()) {
            logger.logWarning(context.getString(R.string.log_record_file_is_missing), showMessage)
            return null
        }
        var res: String? = null
        if (record.isCrypted) {
            if (record.isDecrypted) {
                val bytes: ByteArray? = try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    withContext(Dispatchers.IO) { FileUtils.readFile(uri) }
                } catch (ex: Exception) {
                    logger.logError(context.getString(R.string.log_error_read_record_file) + path, ex)
                    return null
                }
                if (bytes == null) {
                    logger.logError(context.getString(R.string.log_error_decrypt_record_file) + path)
                    return null
                } else if (bytes.isEmpty()) {
                    // файл пуст
                    return ""
                }
                // расшифровываем содержимое файла
                logger.logDebug(context.getString(R.string.log_start_record_text_decrypting))
                res = withContext(Dispatchers.IO) { getCrypter().decryptText(bytes) }
                if (res == null) {
                    logger.logError(context.getString(R.string.log_error_decrypt_record_file) + path)
                }
            }
        } else {
            try {
                @Suppress("BlockingMethodInNonBlockingContext")
                res = withContext(Dispatchers.IO) { FileUtils.readTextFile(uri) }
            } catch (ex: Exception) {
                logger.logError(context.getString(R.string.log_error_read_record_file) + path, ex)
            }
        }
        return res
    }

    /**
     * Получение содержимого записи в виде текста.
     * @param record
     * @return
     */
    suspend fun getRecordTextDecrypted(context: Context, record: TetroidRecord): String? {
        var text: String? = null
        val html = getRecordHtmlTextDecrypted(context, record, false)
        if (html != null) {
            try {
                text = Jsoup.parse(html).text()
            } catch (ex: java.lang.Exception) {
                logger.logError(ex)
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
        logger.logDebug(context.getString(R.string.log_start_record_file_saving) + record.id)
        // проверка существования каталога записи
        val dirPath = getPathToRecordFolder(record)
        if (checkRecordFolder(context, dirPath, true, true) <= 0) {
            return false
        }
        // формирование пути к файлу записи
        val path = dirPath + SEPAR + record.fileName
        val uri: Uri = try {
            Uri.parse(path)
        } catch (ex: java.lang.Exception) {
            logger.logError(context.getString(R.string.log_error_generate_record_file_path) + path, ex)
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
            logger.logError(context.getString(R.string.log_error_write_to_record_file) + path, ex)
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
        return checkRecordFolder(context, dirPath, isCreate, false)
    }

    fun checkRecordFolder(context: Context, dirPath: String, isCreate: Boolean, showMessage: Boolean): Int {
        val folder = File(dirPath)
        try {
            if (!folder.exists()) {
                if (isCreate) {
                    logger.logWarning(String.format(Locale.getDefault(), context.getString(R.string.log_create_record_dir), dirPath), showMessage)
                    return if (folder.mkdirs()) {
                        logger.logOperRes(LogObj.RECORD_DIR, LogOper.CREATE, "", showMessage)
                        1
                    } else {
                        logger.logError(context.getString(R.string.log_create_record_dir_error), showMessage)
                        0
                    }
                }
                return -1
            }
        } catch (ex: java.lang.Exception) {
            logger.logError(context.getString(R.string.log_check_record_dir_error), showMessage)
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
    suspend fun cloneRecordToNode(
        context: Context,
        srcRecord: TetroidRecord,
        node: TetroidNode,
        isCutted: Boolean,
        breakOnFSErrors: Boolean
    ): TetroidRecord? {
        logger.logOperStart(LogObj.RECORD, LogOper.INSERT, srcRecord)

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
            favoritesInteractor.add(record)
        }
        // добавляем метки в запись и в коллекцию меток
        tagsParser.parseRecordTags(record, tagsString)
        val errorRes = if (breakOnFSErrors) null else record
        // проверяем существование каталога записи
        val srcDirPath = if (isCutted) {
            getPathToRecordFolderInTrash(srcRecord)
        } else {
            getPathToRecordFolder(srcRecord)
        }
        val dirRes = checkRecordFolder(context, srcDirPath, false)
        val srcDir = if (dirRes > 0) {
            File(srcDirPath)
        } else {
            return errorRes
        }
        val destDirPath = getPathToRecordFolder(record)
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
                @Suppress("BlockingMethodInNonBlockingContext")
                val res = withContext(Dispatchers.IO) { FileUtils.copyDirRecursive(srcDir, destDir) }
                if (res) {
                    logger.logDebug(context.getString(R.string.log_copy_record_dir_mask).format(destDirPath))
                    // переименовываем прикрепленные файлы
                    renameRecordAttaches(context, srcRecord, record)
                } else {
                    logger.logError(context.getString(R.string.log_error_copy_record_dir_mask).format(srcDirPath, destDirPath))
                    return errorRes
                }
            } catch (ex: IOException) {
                logger.logError(context.getString(R.string.log_error_copy_record_dir_mask).format(srcDirPath, destDirPath), ex)
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
            val destPath = getPathToRecordFolder(destRecord)
            val srcFile = File(destPath, srcFileIdName)
            val destFile = File(destPath, destFileIdName)
            if (srcFile.renameTo(destFile)) {
                val to = StringUtils.getStringTo(context, destFile.absolutePath)
                logger.logOperRes(LogObj.FILE, LogOper.RENAME, to, false)
            } else {
                val fromTo = StringUtils.getStringFromTo(context, srcFile.absolutePath, destFile.name)
                logger.logOperError(LogObj.FILE, LogOper.RENAME, fromTo, false, false)
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
            logger.logEmptyParams("DataManager.createRecord()")
            return null
        }
        logger.logOperStart(LogObj.RECORD, LogOper.CREATE)

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
        val dirPath = getPathToRecordFolder(record)
        if (checkRecordFolder(context, dirPath, true) <= 0) {
            return null
        }
        val dir = File(dirPath)
        // создаем файл записи (пустой)
        val filePath = dirPath + SEPAR + record.fileName
        val fileUri: Uri = try {
            Uri.parse(filePath)
        } catch (ex: java.lang.Exception) {
            logger.logError(context.getString(R.string.log_error_generate_record_file_path) + filePath, ex)
            return null
        }
        val file = File(fileUri.path!!)
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            withContext(Dispatchers.IO) { file.createNewFile() }
        } catch (ex: IOException) {
            logger.logError(context.getString(R.string.log_error_creating_record_file) + filePath, ex)
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
                favoritesInteractor.add(record)
            }
        } else {
            logger.logOperCancel(LogObj.RECORD, LogOper.CREATE)
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
        logger.logOperStart(LogObj.TEMP_RECORD, LogOper.CREATE)
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
        val record = TetroidRecord(
            false, id,
            name, null, null, url,
            Date(), dirName, TetroidRecord.DEF_FILE_NAME, node
        )
        record.setIsNew(true)
        record.setIsTemp(true)

        // создаем каталог записи в корзине
        val dirPath = getPathToRecordFolderInTrash(record)
        if (checkRecordFolder(context, dirPath, true) <= 0) {
            return null
        }
        // создаем файл записи (пустой)
        val filePath = dirPath + SEPAR + record.fileName
        val fileUri: Uri = try {
            Uri.parse(filePath)
        } catch (ex: java.lang.Exception) {
            logger.logError(context.getString(R.string.log_error_generate_record_file_path) + filePath, ex)
            return null
        }
        val file = File(fileUri.path!!)
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            withContext(Dispatchers.IO) { file.createNewFile() }
        } catch (ex: IOException) {
            logger.logError(context.getString(R.string.log_error_creating_record_file) + filePath, ex)
            return null
        }
        // текст записи
        if (!TextUtils.isEmpty(text)) {
            if (saveRecordHtmlText(context, record, text!!)) {
                record.setIsNew(false)
            } else {
                logger.logOperErrorMore(LogObj.RECORD, LogOper.SAVE, false)
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
            logger.logEmptyParams("DataManager.editRecordFields()")
            return false
        }
        val isTemp = record.isTemp
        if (isTemp) {
            logger.logOperStart(LogObj.TEMP_RECORD, LogOper.SAVE, record)
        } else {
            logger.logOperStart(LogObj.RECORD_FIELDS, LogOper.CHANGE, record)
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
                    getPathToRecordFolderInTrash(record),
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
                favoritesInteractor.addOrRemoveIfNeed(record, isFavor)
            }
            // зашифровываем или расшифровываем файл записи и прикрепленные файлы
            // FIXME: обрабатывать результат ?
            cryptInteractor.cryptRecordFiles(context, record, oldIsCrypted, crypted)
        } else {
            if (isTemp) {
                logger.logOperCancel(LogObj.TEMP_RECORD, LogOper.SAVE)
            } else {
                logger.logOperCancel(LogObj.RECORD_FIELDS, LogOper.CHANGE)
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
                favoritesInteractor.addOrRemoveIfNeed(record, oldIsFavor)
            }
            if (isTemp) {
                moveRecordFolder(
                    context,
                    record,
                    getPathToRecordFolder(record),
                    CommonSettings.getTrashPathDef(context),
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
        return deleteRecord(context, record, withoutDir, CommonSettings.getTrashPathDef(context), false)
    }

    /**
     * Вырезание записи из ветки (добавление в "буфер обмена" и удаление).
     * @param record
     * @return 1 - успешно
     * 0 - ошибка
     * -1 - ошибка (отсутствует каталог записи)
     */
    suspend fun cutRecord(context: Context, record: TetroidRecord, withoutDir: Boolean): Int {
        return deleteRecord(context, record, withoutDir, CommonSettings.getTrashPathDef(context), true)
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
        logger.logOperStart(LogObj.RECORD, LogOper.INSERT, srcRecord)
        var srcDirPath = ""
        var srcDir: File? = null
        // проверяем существование каталога записи
        if (!withoutDir) {
            srcDirPath = if (isCutted) {
                getPathToRecordFolderInTrash(srcRecord)
            } else {
                getPathToRecordFolder(srcRecord)
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
        val destDirPath = getPathToRecordFolder(record)
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
                    @Suppress("BlockingMethodInNonBlockingContext")
                    val copyDirResult = withContext(Dispatchers.IO) { FileUtils.copyDirRecursive(srcDir, destDir) }
                    if (copyDirResult) {
                        logger.logDebug(String.format(context.getString(R.string.log_copy_record_dir_mask), destDirPath))
                        // переименовываем прикрепленные файлы
                        renameRecordAttaches(context, srcRecord, record)
                    } else {
                        logger.log(String.format(context.getString(R.string.log_error_copy_record_dir_mask), srcDirPath, destDirPath))
                        return -2
                    }
                } catch (ex: IOException) {
                    logger.logError(String.format(context.getString(R.string.log_error_copy_record_dir_mask), srcDirPath, destDirPath), ex)
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
                favoritesInteractor.add(record)
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
            logger.logOperCancel(LogObj.RECORD, LogOper.INSERT)
            // удаляем запись из ветки
            node.records.remove(record)
            if (!withoutDir) {
                if (isCutted) {
                    // перемещаем каталог записи обратно в корзину
                    return moveRecordFolder(context, record, destDirPath, CommonSettings.getTrashPathDef(context), srcRecord.dirName)
                } else {
                    // удаляем только что скопированный каталог записи
                    if (FileUtils.deleteRecursive(destDir)) {
                        logger.logOperRes(LogObj.RECORD_DIR, LogOper.DELETE)
                    } else {
                        logger.logOperError(LogObj.RECORD_DIR, LogOper.DELETE,": $destDirPath", false, false)
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
    private suspend fun deleteRecord(
        context: Context,
        record: TetroidRecord,
        withoutDir: Boolean,
        movePath: String,
        isCutting: Boolean
    ): Int {
        logger.logOperStart(LogObj.RECORD, if (isCutting) LogOper.CUT else LogOper.DELETE, record)
        var dirPath: String? = null
        // проверяем существование каталога записи
        if (!withoutDir) {
//            dirPath = (record.isTemp()) ? getPathToRecordFolderInTrash(context, record) : getPathToRecordFolderInBase(record);
            dirPath = getPathToRecordFolder(record)
            val dirRes = checkRecordFolder(context, dirPath, false)
            if (dirRes <= 0) {
                return dirRes
            }
        }

        // удаляем запись из ветки (и соответственно, из дерева)
        val node = record.node
        if (node != null) {
            if (!node.deleteRecord(record)) {
                logger.logError(context.getString(R.string.log_not_found_record_in_node))
                return 0
            }
        } else {
            logger.logError(context.getString(R.string.log_record_not_have_node))
            return 0
        }

        // перезаписываем структуру хранилища в файл
        if (saveStorage(context)) {
            // удаляем из избранного
            if (record.isFavorite) {
                favoritesInteractor.remove(record, false)
            }
            // перезагружаем список меток
            tagsParser.deleteRecordTags(record)
        } else {
            logger.logOperCancel(LogObj.RECORD, LogOper.DELETE)
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
    suspend fun moveOrDeleteRecordFolder(
        context: Context,
        record: TetroidRecord,
        dirPath: String,
        movePath: String?
    ): Int {
        return if (movePath == null) {
            // удаляем каталог записи
            val dir = File(dirPath)
            if (FileUtils.deleteRecursive(dir)) {
                logger.logOperRes(LogObj.RECORD_DIR, LogOper.DELETE)
                1
            } else {
                logger.logOperError(LogObj.RECORD_DIR, LogOper.DELETE,": $dirPath", false, false)
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
    private suspend fun moveRecordFolder(
        context: Context,
        record: TetroidRecord,
        srcPath: String,
        destPath: String,
        newDirName: String?
    ): Int {
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
        logger.logOperStart(LogObj.RECORD_DIR, LogOper.DELETE, record)
        // проверяем существование каталога
        val dirPath = getPathToRecordFolder(record)
        if (checkRecordFolder(context, dirPath, false) <= 0) {
            return false
        }
        val folder = File(dirPath)
        // удаляем каталог
        if (!FileUtils.deleteRecursive(folder)) {
            logger.logOperError(LogObj.RECORD_DIR, LogOper.DELETE,": $dirPath", false, false)
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
            logger.logEmptyParams("RecordManager.openRecordFolder()")
            return false
        }
        logger.logDebug(context.getString(R.string.log_start_record_folder_opening) + record.id)
//        Uri uri = Uri.parse(getUriToRecordFolder(record));
        val fileFullName = getPathToRecordFolder(record)
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
    fun getEditedDate(context: Context, record: TetroidRecord): Date? {
        if (record.isNew || record.isTemp) {
            return null
        }
        val fileName = getPathToFileInRecordFolder(record, record.fileName)
        return try {
            FileUtils.getFileModifiedDate(context, fileName)
        } catch (ex: Exception) {
            logger.logError(context.getString(R.string.error_get_record_modified_date_mask).format(ex.localizedMessage))
            null
        }
    }

    // TODO: переделать на Either, чтобы вернуть строку с ошибкой
    fun getRecordFolderSize(context: Context, record: TetroidRecord): String? {
        return try {
            FileUtils.getFileSize(context, getPathToRecordFolder(record))
        } catch (ex: Exception) {
            logger.logError(context.getString(R.string.error_get_record_folder_size_mask).format(ex.localizedMessage))
            null
        }
    }

    /**
     * Получение пути к каталогу записи в виде Uri, с учетом того, что это может быть временная запись.
     * Запись может находиться в хранилище в каталоге base/ или в каталоге корзины.
     * @param record
     * @return
     */
    fun getUriToRecordFolder(record: TetroidRecord): String {
        val storageUri = if (record.isTemp) storageInteractor.getUriToStorageTrashFolder() else storageInteractor.getUriToStorageBaseFolder()
        return "$storageUri$SEPAR${record.dirName}$SEPAR"
    }

    /**
     * Получение пути к каталогу записи, с учетом того, что это может быть временная запись.
     * Запись может находиться в хранилище в каталоге base/ или в каталоге корзины.
     * @param record
     * @return
     */
    fun getPathToRecordFolder(record: TetroidRecord): String {
        val storagePath = if (record.isTemp) storageInteractor.getPathToStorageTrashFolder() else storageInteractor.getPathToStorageBaseFolder()
        return "$storagePath$SEPAR${record.dirName}"
    }

    fun getPathToRecordFolderInTrash(record: TetroidRecord): String {
        return "${storageInteractor.getPathToStorageTrashFolder()}$SEPAR${record.dirName}"
    }

    fun getPathToFileInRecordFolder(record: TetroidRecord, fileName: String): String {
        return "${getPathToRecordFolder(record)}$SEPAR$fileName"
    }

    fun getRecord(id: String?): TetroidRecord? {
        val comparator = TetroidRecordComparator(TetroidRecord.FIELD_ID)
        return findRecordInHierarchy(id, comparator)
    }

    fun findRecordInHierarchy(fieldValue: String?, comparator: TetroidRecordComparator): TetroidRecord? {
        var found: TetroidRecord?
        if (findRecord(TetroidXml.ROOT_NODE.records, fieldValue, comparator).also { found = it } != null)
            return found
        return if (xmlLoader.mIsFavoritesMode) findRecord(favoritesInteractor.getFavoriteRecords(), fieldValue, comparator)
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

    fun encryptField(isCrypted: Boolean, field: String?): String? {
        return if (isCrypted) getCrypter().encryptTextBase64(field) else field
    }

    fun decryptField(isCrypted: Boolean, field: String?): String? {
        return if (isCrypted) getCrypter().decryptBase64(field) else field
    }

    suspend fun saveStorage(context: Context) = storageInteractor.saveStorage(context)

    fun getCrypter() = cryptInteractor.crypter
}