package com.gee12.mytetroid.interactors

import android.content.Context
import android.net.Uri
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants.SEPAR
import com.gee12.mytetroid.data.CommonSettings
import com.gee12.mytetroid.data.FavoritesManager
import com.gee12.mytetroid.data.TetroidXml
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.data.xml.IStorageLoadHelper
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.services.FileObserverService
import com.gee12.mytetroid.utils.FileUtils
import com.gee12.mytetroid.utils.StringUtils
import com.gee12.mytetroid.views.activities.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.internal.StringUtil
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * Создается для конкретного хранилища.
 */
class StorageInteractor(
    private val logger: ITetroidLogger,
    private val storageHelper: IStorageLoadHelper,
    private val xmlLoader: TetroidXml,
    private val dataInteractor: DataInteractor
) {

    companion object {
        const val BASE_FOLDER_NAME = "base"
        const val ICONS_FOLDER_NAME = "icons"
        const val MYTETRA_XML_FILE_NAME = "mytetra.xml"
        const val DATABASE_INI_FILE_NAME = "database.ini"
        const val FILE_URI_PREFIX = "file://"

        fun getLastFolderPathOrDefault(context: Context, forWrite: Boolean): String? {
            val lastFolder = CommonSettings.getLastChoosedFolderPath(context)
            return if (!StringUtil.isBlank(lastFolder) && File(lastFolder).exists()) lastFolder
            else FileUtils.getExternalPublicDocsOrAppDir(context, forWrite)
        }
    }
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
            setFileName(storagePath + SEPAR + DATABASE_INI_FILE_NAME)
        }
        if (!databaseConfig.saveDefault()) {
            return false
        }

        // создаем каталог base
        if (!createBaseFolder(storagePath)) {
            return false
        }

        // добавляем корневую ветку
        xmlLoader.init()
        if (!storageHelper.createDefaultNode()) {
            return false
        }

        // создаем Favorites
        FavoritesManager.create()

        return true
    }

    /**
     * Сохранение хранилища в файл mytetra.xml.
     * @return Результат выполнения операции
     */
    suspend fun saveStorage(context: Context): Boolean {
        if (xmlLoader.mRootNodesList == null) {
//            LogManager.log("Попытка сохранения mytetra.xml в режиме загрузки только избранных записей", LogManager.Types.WARNING);
            logger.logError(R.string.log_attempt_save_empty_nodes)
            return false
        }
        val destPath = getPathToMyTetraXml()
        val tempPath = destPath + "_tmp"
        logger.logDebug(context.getString(R.string.log_saving_mytetra_xml))
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            val saveResult = withContext(Dispatchers.IO) {
                val fos = FileOutputStream(tempPath, false)
                xmlLoader.save(fos)
            }
            if (saveResult) {
                val to = File(destPath)
                // перемещаем старую версию файла mytetra.xml в корзину
                val nameInTrash = dataInteractor.createDateTimePrefix() + "_" + MYTETRA_XML_FILE_NAME
                if (dataInteractor.moveFile(context, destPath, CommonSettings.getTrashPathDef(context), nameInTrash) <= 0) {
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
            FileUtils.getFileSize(context, getStoragePath())
        } catch (ex: Exception) {
            logger.logError(context.getString(R.string.error_get_storage_folder_size_mask).format(ex.localizedMessage))
            null
        }
    }

    fun getMyTetraXmlLastModifiedDate(context: Context): Date? {
        return try {
            FileUtils.getFileModifiedDate(context, getPathToMyTetraXml())
        } catch (ex: Exception) {
            logger.logError(context.getString(R.string.error_get_mytetra_xml_modified_date_mask).format(ex.localizedMessage))
            null
        }
    }

    private fun createBaseFolder(storagePath: String): Boolean {
        val baseDir = File(storagePath, BASE_FOLDER_NAME)
        return baseDir.mkdir()
    }

    fun isLoaded() = storageHelper.isStorageLoaded()

    fun isLoadedFavoritesOnly() = xmlLoader.mIsFavoritesMode

    fun getPathToMyTetraXml(): String {
        return "${getStoragePath()}$SEPAR$MYTETRA_XML_FILE_NAME"
    }

    fun getPathToStorageBaseFolder(): String {
        return "${getStoragePath()}$SEPAR$BASE_FOLDER_NAME"
    }

    fun getUriToStorageBaseFolder(): Uri {
        return Uri.parse("$FILE_URI_PREFIX${getPathToStorageBaseFolder()}")
    }

    fun getPathToDatabaseIniConfig(): String {
        return "${getStoragePath()}$SEPAR$DATABASE_INI_FILE_NAME"
    }

    fun getPathToIcons(): String {
        return "${getStoragePath()}$SEPAR$ICONS_FOLDER_NAME"
    }

    fun getPathToFileInIconsFolder(fileName: String): String {
        return "${getPathToIcons()}$SEPAR$fileName"
    }

    fun getPathToStorageTrashFolder(): String {
        return getStorageTrashPath()
    }

    fun getUriToStorageTrashFolder(): Uri {
        return Uri.parse("$FILE_URI_PREFIX${getStorageTrashPath()}")
    }

    private fun getStoragePath() = storageHelper.getStoragePath()

    private fun getStorageTrashPath() = storageHelper.getTrashPath()

    fun getRootNodes(): List<TetroidNode> {
        return xmlLoader.mRootNodesList
    }

}