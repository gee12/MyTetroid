package com.gee12.mytetroid.interactors

import android.content.Context
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.Constants.SEPAR
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.services.FileObserverService
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.common.utils.StringUtils
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.helpers.IStorageHelper
import com.gee12.mytetroid.helpers.IStoragePathHelper
import com.gee12.mytetroid.views.activities.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * Создается для конкретного хранилища.
 */
class StorageInteractor(
    private val logger: ITetroidLogger,
    private val storagePathHelper: IStoragePathHelper,
    private val storageHelper: IStorageHelper,
    private val storageDataProcessor: IStorageDataProcessor,
    private val dataInteractor: DataInteractor
) {

    /**
     * Создание файлов хранилища, если оно новое.
     * @param storage
     */
    fun createStorage(storage: TetroidStorage): Boolean {
        try {
            if (storage.isNew) {
                if (createStorageFiles(storage.path)) {
                    storage.isNew = false
                    return true
                }
            }
        } catch (ex: Exception) {
            logger.logError(ex)
        }
        return false
    }

    /**
     * Создание файлов хранилища в указанном расположении.
     * @param storagePath
     */
    private fun createStorageFiles(storagePath: String): Boolean {
        val storageDir = File(storagePath)
        if (storageDir.exists()) {
            // проверяем, пуст ли каталог
            if (!FileUtils.isDirEmpty(storageDir)) {
                logger.log(R.string.log_dir_not_empty)
                return false
            }
        } else {
            logger.log(R.string.log_dir_is_missing, LogType.ERROR)
            return false
        }

        // сохраняем новый database.ini
        val databaseConfig = DatabaseConfig(logger).apply {
            setFileName(storagePath + SEPAR + Constants.DATABASE_INI_FILE_NAME)
        }
        if (!databaseConfig.saveDefault()) {
            return false
        }

        // создаем каталог base
        if (!createBaseFolder(storagePath)) {
            return false
        }

        // добавляем корневую ветку
        storageDataProcessor.init()
        if (!storageHelper.createDefaultNode()) {
            return false
        }

        return true
    }

    /**
     * Сохранение хранилища в файл mytetra.xml.
     * @return Результат выполнения операции
     */
    suspend fun saveStorage(context: Context): Boolean {
        val destPath = storagePathHelper.getPathToMyTetraXml()
        val tempPath = destPath + "_tmp"
        logger.logDebug(context.getString(R.string.log_saving_mytetra_xml))
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            val saveResult = withContext(Dispatchers.IO) {
                val fos = FileOutputStream(tempPath, false)
                storageDataProcessor.save(fos)
            }
            if (saveResult) {
                val to = File(destPath)
                // перемещаем старую версию файла mytetra.xml в корзину
                val nameInTrash = dataInteractor.createDateTimePrefix() + "_" + Constants.MYTETRA_XML_FILE_NAME
                if (dataInteractor.moveFile(context, destPath, storagePathHelper.getPathToStorageTrashFolder(), nameInTrash) <= 0) {
                    // если не удалось переместить в корзину, удаляем
                    if (to.exists() && !to.delete()) {
//                        LogManager.log(context.getString(R.string.log_failed_delete_file) + destPath, LogManager.Types.ERROR);
                        logger.logOperError(LogObj.FILE, LogOper.DELETE, destPath, false, false)
                        return false
                    }
                }
                // задаем правильное имя актуальной версии файла mytetra.xml
                val from = File(tempPath)
                if (!from.renameTo(to)) {
                    val fromTo = StringUtils.getStringFromTo(context, tempPath, destPath)
//                    LogManager.log(String.format(context.getString(R.string.log_rename_file_error_mask), tempPath, destPath), LogManager.Types.ERROR);
                    logger.logOperError(LogObj.FILE, LogOper.RENAME, fromTo, false, false)
                    return false
                }

                // перезапускаем отслеживание, чтобы проверять новосозданный файл
                if (context is MainActivity) {
                    // но только для MainActivity
                    FileObserverService.sendCommand(context, FileObserverService.ACTION_RESTART)
                    logger.log(context.getString(
                            R.string.log_mytetra_xml_observer_mask,
                            context.getString(R.string.relaunched)
                        ))
                }
                return true
            }
        } catch (ex: Exception) {
            logger.logError(ex)
        }
        return false
    }

    // TODO: переделать на Either, чтобы вернуть строку с ошибкой
    fun getStorageFolderSize(context: Context): String? {
        return try {
            FileUtils.getFileSize(context, storagePathHelper.getStoragePath())
        } catch (ex: Exception) {
            logger.logError(context.getString(R.string.error_get_storage_folder_size_mask).format(ex.localizedMessage))
            null
        }
    }

    fun getMyTetraXmlLastModifiedDate(context: Context): Date? {
        return try {
            FileUtils.getFileModifiedDate(context, storagePathHelper.getPathToMyTetraXml())
        } catch (ex: Exception) {
            logger.logError(context.getString(R.string.error_get_mytetra_xml_modified_date_mask).format(ex.localizedMessage))
            null
        }
    }

    private fun createBaseFolder(storagePath: String): Boolean {
        val baseDir = File(storagePath, Constants.BASE_DIR_NAME)
        return baseDir.mkdir()
    }

    fun isLoaded() = storageDataProcessor.isLoaded()

    fun isLoadedFavoritesOnly() = storageDataProcessor.isLoadFavoritesOnlyMode()

    fun getPathToMyTetraXml() = storagePathHelper.getPathToMyTetraXml()

    fun getPathToStorageBaseFolder() = storagePathHelper.getPathToStorageBaseFolder()

    fun getUriToStorageBaseFolder() = storagePathHelper.getUriToStorageBaseFolder()

    fun getPathToDatabaseIniConfig() = storagePathHelper.getPathToDatabaseIniConfig()

    fun getPathToIcons() = storagePathHelper.getPathToIcons()

    fun getPathToFileInIconsFolder(fileName: String) = storagePathHelper.getPathToFileInIconsFolder(fileName)

    fun getPathToStorageTrashFolder() = storagePathHelper.getPathToStorageTrashFolder()

    fun getUriToStorageTrashFolder() = storagePathHelper.getUriToStorageTrashFolder()

    fun getStoragePath() = storagePathHelper.getStoragePath()

    fun getStorageTrashPath() = storagePathHelper.getPathToTrash()

    fun getRootNodes(): List<TetroidNode> {
        return storageDataProcessor.getRootNodes()
    }

}