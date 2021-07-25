package com.gee12.mytetroid.interactor

import android.content.Context
import android.widget.Toast
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.DataManager
import com.gee12.mytetroid.data.SettingsManager
import com.gee12.mytetroid.data.TetroidXml
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.logs.TetroidLog
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.services.FileObserverService
import com.gee12.mytetroid.utils.FileUtils
import com.gee12.mytetroid.views.activities.MainActivity
import java.io.File
import java.io.FileOutputStream

class StorageInteractor(
    val storage: TetroidStorage,
    val mLogger: ILogger,
    val mXml: TetroidXml,
    val mDataInteractor: DataInteractor
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
            mLogger.log(ex)
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
            /*// очищаем каталог
            LogManager.log(context, R.string.log_clear_storage_dir, ILogger.Types.INFO);
            FileUtils.clearDir(storageDir);*/

            // проверяем, пуст ли каталог
            if (!FileUtils.isDirEmpty(storageDir)) {
                mLogger.log(R.string.log_dir_not_empty)
                return false
            }
        } else {
            mLogger.log(R.string.log_dir_is_missing, ILogger.Types.ERROR)
            return false
        }

        // сохраняем новый database.ini
        val databaseConfig = DatabaseConfig(mLogger, storagePath + DataManager.SEPAR + DataManager.DATABASE_INI_FILE_NAME)
        if (!databaseConfig.saveDefault()) {
            return false;
        }

        // создаем каталог base
        val baseDir = File(storagePath, DataManager.BASE_FOLDER_NAME)
        if (!baseDir.mkdir()) {
            return false
        }
        return true
    }

    /**
     * Сохранение хранилища в файл mytetra.xml.
     * @return
     */
    fun saveStorage(context: Context): Boolean {
        if (mXml.mRootNodesList == null) {
//            LogManager.log("Попытка сохранения mytetra.xml в режиме загрузки только избранных записей", LogManager.Types.WARNING);
            LogManager.log(context, R.string.log_attempt_save_empty_nodes, ILogger.Types.ERROR)
            return false
        }
        val destPath = getPathToMyTetraXml()
        val tempPath = destPath + "_tmp"
        LogManager.log(context, context.getString(R.string.log_saving_mytetra_xml), ILogger.Types.DEBUG)
        try {
            val fos = FileOutputStream(tempPath, false)
            if (mXml.save(fos)) {
                val to = File(destPath)
                //                if (moveOld) {
                // перемещаем старую версию файла mytetra.xml в корзину
                val nameInTrash = mDataInteractor.createDateTimePrefix() + "_" + DataManager.MYTETRA_XML_FILE_NAME
                if (mDataInteractor.moveFile(context, destPath, SettingsManager.getTrashPath(context), nameInTrash) <= 0) {
                    // если не удалось переместить в корзину, удаляем
                    if (to.exists() && !to.delete()) {
//                        LogManager.log(context.getString(R.string.log_failed_delete_file) + destPath, LogManager.Types.ERROR);
                        TetroidLog.logOperError(context, TetroidLog.Objs.FILE, TetroidLog.Opers.DELETE, destPath, false, -1)
                        return false
                    }
                }
                // задаем правильное имя актуальной версии файла mytetra.xml
                val from = File(tempPath)
                if (!from.renameTo(to)) {
                    val fromTo = DataManager.getStringFromTo(context, tempPath, destPath)
                    //                    LogManager.log(String.format(context.getString(R.string.log_rename_file_error_mask),
//                            tempPath, destPath), LogManager.Types.ERROR);
                    TetroidLog.logOperError(
                        context, TetroidLog.Objs.FILE, TetroidLog.Opers.RENAME,
                        fromTo, false, -1
                    )
                    return false
                }

                // перезапускаем отслеживание, чтобы проверять новосозданный файл
                if (context is MainActivity) {
                    // но только для MainActivity
                    FileObserverService.sendCommand(context, FileObserverService.ACTION_RESTART)
                    LogManager.log(
                        context, context.getString(
                            R.string.log_mytetra_xml_observer_mask,
                            context.getString(R.string.relaunched)
                        ), ILogger.Types.INFO
                    )
                }
                return true
            }
        } catch (ex: java.lang.Exception) {
            LogManager.log(context, ex)
        }
        return false
    }

    fun getPathToMyTetraXml(): String {
        return storage.path + DataManager.SEPAR + DataManager.MYTETRA_XML_FILE_NAME
    }

}