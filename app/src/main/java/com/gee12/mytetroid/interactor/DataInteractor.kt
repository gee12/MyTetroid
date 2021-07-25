package com.gee12.mytetroid.interactor

import android.content.Context
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.DataManager
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.logs.TetroidLog
import com.gee12.mytetroid.utils.FileUtils
import com.gee12.mytetroid.utils.Utils
import java.io.File
import java.util.*

class DataInteractor(
    val mStorageInteractor: StorageInteractor
) {

    /**
     * Генерация уникального идентификатора для объектов хранилища.
     * @return
     */
    fun createUniqueId(): String {
        val sb = StringBuilder()
        // 10 цифр количества (милли)секунд с эпохи UNIX
        val seconds = System.currentTimeMillis().toString()
        val length = seconds.length
        if (length > DataManager.UNIQUE_ID_HALF_LENGTH) {
            sb.append(seconds.substring(0, DataManager.UNIQUE_ID_HALF_LENGTH))
        } else if (length < DataManager.UNIQUE_ID_HALF_LENGTH) {
            sb.append(seconds)
            for (i in 0 until DataManager.UNIQUE_ID_HALF_LENGTH - length) {
                sb.append('0')
            }
        }
        // 10 случайных символов
        val rand = Random()
        for (i in 0 until DataManager.UNIQUE_ID_HALF_LENGTH) {
            val randIndex = Math.abs(rand.nextInt()) % DataManager.ID_SYMBOLS.length
            sb.append(DataManager.ID_SYMBOLS[randIndex])
        }
        return sb.toString()
    }

    /**
     * Замена местами 2 объекта хранилища в списке.
     * @param list
     * @param pos
     * @param isUp
     * @return 1 - успешно
     * 0 - перемещение невозможно (пограничный элемент)
     * -1 - ошибка
     */
    fun swapTetroidObjects(context: Context, list: List<*>?, pos: Int, isUp: Boolean, through: Boolean): Int {
        val isSwapped: Boolean = try {
            Utils.swapListItems(list, pos, isUp, through)
        } catch (ex: Exception) {
            LogManager.log(context, ex, -1)
            return -1
        }

        // перезаписываем файл структуры хранилища
        return if (isSwapped) {
            if (mStorageInteractor.saveStorage(context)) 1 else -1
        } else 0
    }

    /**
     * Перемещение файла или каталога рекурсивно с дальнейшим переименованием, если нужно.
     * @param srcFullFileName
     * //     * @param srcFileName
     * @param destPath
     * @param newFileName
     * @return 1 - успешно
     * -2 - ошибка (не удалось переместить или переименовать)
     */
    fun moveFile(context: Context, srcFullFileName: String,  /* String srcFileName, */
        destPath: String, newFileName: String?
    ): Int {
        var srcFile = File(srcFullFileName)
        val srcFileName = srcFile.name
        val destDir = File(destPath)
        // перемещаем файл или каталог
        if (!FileUtils.moveToDirRecursive(srcFile, destDir)) {
            val fromTo = DataManager.getStringFromTo(context, srcFullFileName, destPath)
            //            LogManager.log(String.format(context.getString(R.string.log_error_move_file_mask),
//                    srcFullFileName, destPath), LogManager.Types.ERROR);
            TetroidLog.logOperError(
                context, TetroidLog.Objs.FILE, TetroidLog.Opers.MOVE,
                fromTo, false, -1
            )
            return -2
        }
        if (newFileName == null) {
            val destDirPath = destDir.absolutePath + File.separator + srcFileName
            val to = Utils.getStringFormat(context, R.string.log_to_mask, destDirPath)
            //            LogManager.log(String.format(context.getString(R.string.log_file_moved_mask),
//                    destDirPath), LogManager.Types.DEBUG);
            TetroidLog.logOperRes(context, TetroidLog.Objs.FILE, TetroidLog.Opers.MOVE, to, -1)
        } else {
            // добавляем к имени каталога записи уникальную приставку
            srcFile = File(destPath, srcFileName)
            val destFile = File(destPath, newFileName)
            if (srcFile.renameTo(destFile)) {
                val to = Utils.getStringFormat(context, R.string.log_to_mask, destFile.absolutePath)
                //                LogManager.log(String.format(context.getString(R.string.log_file_moved_mask),
//                        destFile.getAbsolutePath()), LogManager.Types.DEBUG);
                TetroidLog.logOperRes(context, TetroidLog.Objs.FILE, TetroidLog.Opers.MOVE, to, -1)
            } else {
                val fromTo = DataManager.getStringFromTo(context, srcFile.absolutePath, destFile.absolutePath)
                //                LogManager.log(String.format(context.getString(R.string.log_error_move_file_mask),
//                        srcFile.getAbsolutePath(), destFile.getAbsolutePath()), LogManager.Types.ERROR);
                TetroidLog.logOperError(
                    context, TetroidLog.Objs.FILE, TetroidLog.Opers.MOVE,
                    fromTo, false, -1
                )
                return -2
            }
        }
        return 1
    }

    fun createUniqueImageName() =  "image" + createUniqueId() + ".png"

    fun createDateTimePrefix() = Utils.dateToString(Date(), DataManager.PREFIX_DATE_TIME_FORMAT)

}