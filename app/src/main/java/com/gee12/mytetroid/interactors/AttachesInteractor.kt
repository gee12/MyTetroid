package com.gee12.mytetroid.interactors

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.*
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.helpers.IRecordPathHelper
import com.gee12.mytetroid.helpers.IResourcesProvider
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.util.*

/**
 * Создается для конкретного хранилища.
 */
class AttachesInteractor(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val cryptInteractor: EncryptionInteractor,
    private val dataInteractor: DataInteractor,
    private val interactionInteractor: InteractionInteractor,
    private val recordsInteractor: RecordsInteractor,
    private val recordPathHelper: IRecordPathHelper,
    private val saveStorageUseCase: SaveStorageUseCase,
) {

    /**
     * Открытие прикрепленного файла сторонним приложением с предварительной расшифровкой (если необходимо).
     * @param context
     * @param file
     * @return
     */
    suspend fun openAttach(context: Context, file: TetroidFile): Boolean {
        logger.logDebug(resourcesProvider.getString(R.string.log_start_attach_file_opening) + file.id)
        val record = file.record
        val fileDisplayName = file.name
        val ext = FileUtils.getExtensionWithComma(fileDisplayName)
        val fileIdName = file.id + ext
        val fullFileName: String = recordPathHelper.getPathToFileInRecordFolder(record, fileIdName)
        var srcFile: File
        srcFile = try {
            File(fullFileName)
        } catch (ex: Exception) {
            logger.logError(resourcesProvider.getString(R.string.log_error_file_open) + fullFileName, true)
            logger.logError(ex, false)
            return false
        }
        //
        logger.log(resourcesProvider.getString(R.string.log_open_file) + fullFileName, false)
        if (!srcFile.exists()) {
            logger.logError(resourcesProvider.getString(R.string.log_file_is_absent) + fullFileName, true)
            return false
        }
        // если запись зашифрована
        if (record.isCrypted && CommonSettings.isDecryptFilesInTempDef(context)) {
            // создаем временный файл
//                File tempFile = createTempCacheFile(context, fileIdName);
//                File tempFile = new File(String.format("%s%s/_%s", getStoragePathBase(), record.getDirName(), fileIdName));
//                File tempFile = createTempExtStorageFile(context, fileIdName);
            val tempFolderPath = recordPathHelper.getPathToRecordFolderInTrash(record)
            val tempFolder = File(tempFolderPath)
            if (!tempFolder.exists() && !tempFolder.mkdirs()) {
                logger.logError(resourcesProvider.getString(R.string.log_could_not_create_temp_dir) + tempFolderPath, true)
            }
            val tempFile = File(tempFolder, fileIdName)

            // расшифровываем во временный файл
            srcFile = try {
                val existOrCreated = if (!tempFile.exists()) {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    withContext(Dispatchers.IO) { tempFile.createNewFile() }
                } else true

                if (existOrCreated && cryptInteractor.encryptDecryptFile(srcFile, tempFile, false)) {
                    tempFile
                } else {
                    logger.logError(resourcesProvider.getString(R.string.log_could_not_decrypt_file) + fullFileName, true)
                    return false
                }
            } catch (ex: IOException) {
                logger.logError(resourcesProvider.getString(R.string.log_file_decryption_error) + ex.message, true)
                return false
            }
        }
        return interactionInteractor.openFile(context, srcFile)
    }

    suspend fun attachFile(fullName: String, record: TetroidRecord?, deleteSrcFile: Boolean): TetroidFile? {
        val res = attachFile(fullName, record)
        if (deleteSrcFile && res != null) {
            val srcFile = File(fullName)
            if (!FileUtils.deleteRecursive(srcFile)) {
                logger.logError(resourcesProvider.getString(R.string.log_error_delete_file) + fullName)
            }
        }
        return res
    }

    /**
     * Прикрепление нового файла к записи.
     * @param fullName
     * @param record
     * @return
     */
    suspend fun attachFile(fullName: String, record: TetroidRecord?): TetroidFile? {
        if (record == null || TextUtils.isEmpty(fullName)) {
            logger.logEmptyParams("AttachesManager.attachFile()")
            return null
        }
        logger.logOperStart(LogObj.FILE, LogOper.ATTACH, ": $fullName")
        val id: String = dataInteractor.createUniqueId()
        // проверка исходного файла
        val srcFile = File(fullName)
        try {
            if (!srcFile.exists()) {
                logger.logError(resourcesProvider.getString(R.string.log_file_is_absent) + fullName, true)
                return null
            }
        } catch (ex: Exception) {
            logger.logError(resourcesProvider.getString(R.string.log_file_checking_error) + fullName, ex)
            return null
        }
        val fileDisplayName = srcFile.name
        val ext = FileUtils.getExtensionWithComma(fileDisplayName)
        val fileIdName = id + ext
        // создание объекта хранилища
        val crypted = record.isCrypted
        val file = TetroidFile(
            crypted, id,
            cryptInteractor.encryptField(crypted, fileDisplayName),
            TetroidFile.DEF_FILE_TYPE, record
        )
        if (crypted) {
            file.setDecryptedName(fileDisplayName)
            file.setIsDecrypted(true)
        }
        // проверка каталога записи
        val dirPath: String = recordPathHelper.getPathToRecordFolder(record)
        if (recordsInteractor.checkRecordFolder(dirPath, true, true) <= 0) {
            return null
        }
        // формируем путь к файлу назначения в каталоге записи
        val destFilePath = makePath(dirPath, fileIdName)
        val destFileUri: Uri = try {
            Uri.parse(destFilePath)
        } catch (ex: Exception) {
            logger.logError(resourcesProvider.getString(R.string.log_error_generate_file_path) + destFilePath, ex)
            return null
        }
        // копирование файла в каталог записи, зашифровуя при необходимости
        val destFile = File(destFileUri.path!!)
        val fromTo: String = resourcesProvider.getStringFromTo(fullName, destFilePath)
        try {
            if (record.isCrypted) {
                logger.logOperStart(LogObj.FILE, LogOper.ENCRYPT)
                if (!cryptInteractor.encryptDecryptFile(srcFile, destFile, true)) {
                    logger.logOperError(LogObj.FILE, LogOper.ENCRYPT, fromTo, false, false)
                    return null
                }
            } else {
                logger.logOperStart(LogObj.FILE, LogOper.COPY)
                @Suppress("BlockingMethodInNonBlockingContext")
                val copyFile = withContext(Dispatchers.IO) { FileUtils.copyFile(srcFile, destFile) }
                if (!copyFile) {
                    logger.logOperError(LogObj.FILE, LogOper.COPY, fromTo, false, false)
                    return null
                }
            }
        } catch (ex: IOException) {
            logger.logOperError(LogObj.FILE, if (record.isCrypted) LogOper.ENCRYPT else LogOper.COPY,
                fromTo, false, false)
            return null
        }

        // добавляем файл к записи (и соответственно, в дерево)
        var files = record.attachedFiles
        if (files == null) {
            files = ArrayList()
            record.attachedFiles = files
        }
        files.add(file)
        // перезаписываем структуру хранилища в файл
        if (saveStorage()) {
            /*   instance.mFilesCount++;*/
        } else {
            logger.logOperCancel(LogObj.FILE, LogOper.ATTACH)
            // удаляем файл из записи
            files.remove(file)
            // удаляем файл
            destFile.delete()
            return null
        }
        return file
    }

    /**
     * Изменение свойств прикрепленного файла.
     * Проверка существования каталога записи и файла происходит только
     * если у имени файла было изменено расширение.
     * @param file
     * @param name
     * @return 1 - успешно
     * 0 - ошибка
     * -1 - ошибка (отсутствует каталог записи)
     * -2 - ошибка (отсутствует файл в каталоге записи)
     */
    suspend fun editAttachedFileFields(file: TetroidFile?, name: String?): Int {
        if (file == null || TextUtils.isEmpty(name)) {
            logger.logEmptyParams("AttachesManager.editAttachedFileFields()")
            return 0
        }
        logger.logOperStart(LogObj.FILE_FIELDS, LogOper.CHANGE, file)
        val record = file.record
        if (record == null) {
            logger.logError(resourcesProvider.getString(R.string.log_file_record_is_null))
            return 0
        }
        // сравниваем расширения
        val ext = FileUtils.getExtensionWithComma(file.name)
        val newExt = FileUtils.getExtensionWithComma(name)
        val isExtChanged = !Utils.isEquals(ext, newExt, true)
        var dirPath: String? = null
        var filePath: String? = null
        var srcFile: File? = null
        if (isExtChanged) {
            // проверяем существование каталога записи
            dirPath = recordPathHelper.getPathToRecordFolder(record)
            val dirRes: Int = recordsInteractor.checkRecordFolder(dirPath, false)
            if (dirRes <= 0) {
                return dirRes
            }
            // проверяем существование самого файла
            val fileIdName = file.id + ext
            filePath = makePath(dirPath, fileIdName)
            srcFile = File(filePath)
            if (!srcFile.exists()) {
                logger.logError(resourcesProvider.getString(R.string.error_file_is_missing_mask) + filePath)
                return -2
            }
        }
        val oldName = file.getName(true)
        // обновляем поля
        val crypted = file.isCrypted
        file.name = cryptInteractor.encryptField(crypted, name)
        if (crypted) {
            file.setDecryptedName(name)
        }

        // перезаписываем структуру хранилища в файл
        if (!saveStorage()) {
            logger.logOperCancel(LogObj.FILE_FIELDS, LogOper.CHANGE)
            // возвращаем изменения
            file.name = oldName
            if (crypted) {
                file.setDecryptedName(cryptInteractor.decryptField(crypted, oldName))
            }
            return 0
        }
        // меняем расширение, если изменилось
        if (isExtChanged) {
            val newFileIdName = file.id + newExt
            val destFile = makePathToFile(dirPath.orEmpty(), newFileIdName)
            val fromTo: String = resourcesProvider.getStringFromTo(filePath.orEmpty(), newFileIdName)
            if (srcFile!!.renameTo(destFile)) {
                logger.logOperRes(LogObj.FILE, LogOper.RENAME, fromTo, false)
            } else {
                logger.logOperError(LogObj.FILE, LogOper.RENAME, fromTo, false, false)
                return 0
            }
        }
        return 1
    }

    /**
     * Удаление прикрепленного файла.
     * @param file
     * @param withoutFile не пытаться удалить сам файл на диске
     * @return 1 - успешно
     * 0 - ошибка
     * -1 - ошибка (отсутствует каталог записи)
     * -2 - ошибка (отсутствует файл в каталоге записи)
     */
    suspend fun deleteAttachedFile(file: TetroidFile?, withoutFile: Boolean): Int {
        if (file == null) {
            logger.logEmptyParams("AttachesManager.deleteAttachedFile()")
            return 0
        }
        logger.logOperStart(LogObj.FILE, LogOper.DELETE, file)
        val record = file.record
        if (record == null) {
            logger.logError(resourcesProvider.getString(R.string.log_file_record_is_null))
            return 0
        }
        val dirPath: String
        var destFilePath: String? = null
        var destFile: File? = null
        if (!withoutFile) {
            // проверяем существование каталога записи
            dirPath = recordPathHelper.getPathToRecordFolder(record)
            val dirRes: Int = recordsInteractor.checkRecordFolder(dirPath, false)
            if (dirRes <= 0) {
                return dirRes
            }
            // проверяем существование самого файла
            val ext = FileUtils.getExtensionWithComma(file.name)
            val fileIdName = file.id + ext
            destFilePath = makePath(dirPath, fileIdName)
            destFile = File(destFilePath)
            if (!destFile.exists()) {
                logger.logError(resourcesProvider.getString(R.string.error_file_is_missing_mask) + destFilePath)
                return -2
            }
        }

        // удаляем файл из списка файлов записи (и соответственно, из дерева)
        val files = record.attachedFiles
        if (files != null) {
            if (!files.remove(file)) {
                logger.logError(resourcesProvider.getString(R.string.log_not_found_file_in_record))
                return 0
            }
        } else {
            logger.logError(resourcesProvider.getString(R.string.log_record_not_have_attached_files))
            return 0
        }

        // перезаписываем структуру хранилища в файл
        if (!saveStorage()) {
            logger.logOperCancel(LogObj.FILE, LogOper.DELETE)
            return 0
        }

        // удаляем сам файл
        if (!withoutFile) {
            if (!FileUtils.deleteRecursive(destFile)) {
                logger.logError(resourcesProvider.getString(R.string.log_error_delete_file) + destFilePath)
                return 0
            }
        }
        return 1
    }

    /**
     * Сохранение прикрепленного файла по указанному пути.
     * @param file
     * @param destPath
     * @return
     */
    suspend fun saveFile(file: TetroidFile, destPath: String): Boolean {
        val mes = file.getIdString(resourcesProvider) + resourcesProvider.getStringTo(destPath)
        logger.logOperStart(LogObj.FILE, LogOper.SAVE, ": $mes")

        // проверка исходного файла
        val fileIdName = file.idName
        val recordPath: String = recordPathHelper.getPathToRecordFolder(file.record)
        val srcFile = File(recordPath, fileIdName)
        try {
            if (!srcFile.exists()) {
                logger.logError(resourcesProvider.getString(R.string.log_file_is_absent) + fileIdName)
                return false
            }
        } catch (ex: Exception) {
            logger.logError(resourcesProvider.getString(R.string.log_file_checking_error) + fileIdName, ex)
            return false
        }
        // копирование файла в указанный каталог, расшифровуя при необходимости
        val destFile = File(destPath, file.name)
        val fromTo: String = resourcesProvider.getStringFromTo(srcFile.absolutePath, destFile.absolutePath)
        try {
            if (file.isCrypted) {
                logger.logOperStart(LogObj.FILE, LogOper.DECRYPT)
                if (!cryptInteractor.encryptDecryptFile(srcFile, destFile, false)) {
                    logger.logOperError(LogObj.FILE, LogOper.DECRYPT, fromTo, false, false)
                    return false
                }
            } else {
                logger.logOperStart(LogObj.FILE, LogOper.COPY)
                @Suppress("BlockingMethodInNonBlockingContext")
                val copyFileResult = withContext(Dispatchers.IO) { FileUtils.copyFile(srcFile, destFile) }
                if (!copyFileResult) {
                    logger.logOperError(LogObj.FILE, LogOper.COPY, fromTo, false, false)
                    return false
                }
            }
        } catch (ex: IOException) {
            logger.logOperError(LogObj.FILE, if (file.isCrypted) LogOper.DECRYPT else LogOper.COPY,
                fromTo, false, false)
            return false
        }
        return true
    }

    /**
     * Получение полного имени файла.
     * @param attach
     * @return
     */
    fun getAttachFullName(attach: TetroidFile?): String? {
        if (attach == null) {
            return null
        }
        val record = attach.record
        if (record == null) {
            logger.logError(resourcesProvider.getString(R.string.log_file_record_is_null))
            return null
        }
        val ext = FileUtils.getExtensionWithComma(attach.name)
        return recordPathHelper.getPathToFileInRecordFolder(record, attach.id + ext)
    }

    /**
     * Получение размера прикрепленного файла.
     * @param context
     * @param attach
     * @return
     */
    fun getAttachedFileSize(context: Context, attach: TetroidFile): String? {
        return try {
            FileUtils.getFileSize(context, getAttachFullName(attach))
        } catch (ex: Exception) {
            logger.logError(resourcesProvider.getString(R.string.error_get_attach_file_size_mask, ex.localizedMessage), false)
            null
        }
    }


    /**
     * Получение даты последнего изменения файла.
     * @param context
     * @param attach
     * @return
     */
    fun getEditedDate(context: Context, attach: TetroidFile): Date? {
        return FileUtils.getFileModifiedDate(context, getAttachFullName(attach))
    }

    private suspend fun saveStorage(): Boolean {
        return withContext(Dispatchers.IO) {
            saveStorageUseCase.run()
        }.foldResult(
            onLeft = {
                logger.logFailure(it, show = false)
                false
            },
            onRight = { it }
        )
    }

}