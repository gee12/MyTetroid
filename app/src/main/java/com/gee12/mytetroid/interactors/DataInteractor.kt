package com.gee12.mytetroid.interactors

import android.content.Context
import com.gee12.mytetroid.R
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.utils.FileUtils
import com.gee12.mytetroid.utils.StringUtils
import com.gee12.mytetroid.utils.Utils
import com.gee12.mytetroid.viewmodels.IStorageCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.math.abs

class DataInteractor(
    val logger: ITetroidLogger,
    val callback: IStorageCallback
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
        if (length > UNIQUE_ID_HALF_LENGTH) {
            sb.append(seconds.substring(0, UNIQUE_ID_HALF_LENGTH))
        } else if (length < UNIQUE_ID_HALF_LENGTH) {
            sb.append(seconds)
            for (i in 0 until UNIQUE_ID_HALF_LENGTH - length) {
                sb.append('0')
            }
        }
        // 10 случайных символов
        val rand = Random()
        for (i in 0 until UNIQUE_ID_HALF_LENGTH) {
            val randIndex = abs(rand.nextInt()) % ID_SYMBOLS.length
            sb.append(ID_SYMBOLS[randIndex])
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
    suspend fun swapTetroidObjects(context: Context, list: List<*>?, pos: Int, isUp: Boolean, through: Boolean): Int {
        val isSwapped: Boolean = try {
            Utils.swapListItems(list, pos, isUp, through)
        } catch (ex: Exception) {
            logger.logError(ex, false)
            return -1
        }

        // перезаписываем файл структуры хранилища
        return if (isSwapped) {
            if (callback.saveStorage(context)) 1 else -1
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
    suspend fun moveFile(context: Context, srcFullFileName: String,  /* String srcFileName, */
        destPath: String, newFileName: String?
    ): Int {
        var srcFile = File(srcFullFileName)
        val srcFileName = srcFile.name
        val destDir = File(destPath)
        // перемещаем файл или каталог
        val moveToDirRecursive = withContext(Dispatchers.IO) {  FileUtils.moveToDirRecursive(srcFile, destDir) }
        if (!moveToDirRecursive) {
            val fromTo = StringUtils.getStringFromTo(context, srcFullFileName, destPath)
//            logger.log(String.format(context.getString(R.string.log_error_move_file_mask),
//                    srcFullFileName, destPath), LogManager.Types.ERROR);
            logger.logOperError(LogObj.FILE, LogOper.REORDER, fromTo, false, false)
            return -2
        }
        if (newFileName == null) {
            val destDirPath = destDir.absolutePath + File.separator + srcFileName
            val to = StringUtils.getStringFormat(context, R.string.log_to_mask, destDirPath)
//            logger.log(String.format(context.getString(R.string.log_file_moved_mask),
//                    destDirPath), LogManager.Types.DEBUG);
            logger.logOperRes(LogObj.FILE, LogOper.REORDER, to, false)
        } else {
            // добавляем к имени каталога записи уникальную приставку
            srcFile = File(destPath, srcFileName)
            val destFile = File(destPath, newFileName)
            val renameTo = withContext(Dispatchers.IO) {  srcFile.renameTo(destFile) }
            if (renameTo) {
                val to = StringUtils.getStringFormat(context, R.string.log_to_mask, destFile.absolutePath)
//                logger.log(String.format(context.getString(R.string.log_file_moved_mask),
//                        destFile.getAbsolutePath()), LogManager.Types.DEBUG);
                logger.logOperRes(LogObj.FILE, LogOper.REORDER, to, false)
            } else {
                val fromTo = StringUtils.getStringFromTo(context, srcFile.absolutePath, destFile.absolutePath)
//                logger.log(String.format(context.getString(R.string.log_error_move_file_mask),
//                        srcFile.getAbsolutePath(), destFile.getAbsolutePath()), LogManager.Types.ERROR);
                logger.logOperError(LogObj.FILE, LogOper.REORDER, fromTo, false, false)
                return -2
            }
        }
        return 1
    }

    fun createUniqueImageName() =  "image" + createUniqueId() + ".png"

    fun createDateTimePrefix() = Utils.dateToString(Date(), PREFIX_DATE_TIME_FORMAT)

    companion object {
        const val ID_SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyz"
        const val QUOTES_PARAM_STRING = "\"\""

        const val UNIQUE_ID_HALF_LENGTH = 10
        const val PREFIX_DATE_TIME_FORMAT = "yyyyMMddHHmmssSSS"
    }
}