package com.gee12.mytetroid.logs

import android.text.format.DateFormat
import android.util.Log
import com.gee12.mytetroid.domain.IFailureHandler
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*

abstract class FileTetroidLogger(
    failureHandler: IFailureHandler,
) : BaseTetroidLogger(
    failureHandler,
) {

    private var dirPath: String? = null
    private var isWriteToFile = false
    private val buffer = StringBuilder()

    /**
     * Получение списка логов, которые не удалось записать в файл.
     * @return
     */
    override val bufferString: String
        get() = buffer.toString()

    /**
     * Получение полного пути к лог-файлу.
     * @return
     */
    override var fullFileName: String? = null


    override fun init(path: String, isWriteToFile: Boolean) {
        setLogPath(path)
        this.isWriteToFile = isWriteToFile
    }

    override fun setLogPath(path: String) {
        dirPath = path
        fullFileName = "%s%s%s".format(path, File.separator, LOG_FILE_NAME)
    }

    fun log(s: String, type: LogType) {
        logToFile(s, type, isWriteToFile)
    }

    override fun logWithoutShow(s: String, type: LogType) {
        log(s, type)
    }

    // FIXME: как отсюда вызвать отображение лога если show=true ?
    fun logWithoutFile(s: String, type: LogType, show: Boolean = true) {
        logToFile(s, type, false)
    }

    fun logErrorWithoutFile(s: String, show: Boolean = true) {
        logWithoutFile(s, LogType.ERROR, show)
    }

    fun logErrorWithoutFile(ex: Exception, show: Boolean = true) {
        logErrorWithoutFile(failureHandler.getExceptionInfo(ex), show)
    }

    fun logToFile(s: String, type: LogType, isWriteToFile: Boolean) {
//        if (type == LogType.DEBUG && !BuildConfig.DEBUG) return

        when (type) {
            LogType.INFO -> Log.i(LOG_TAG, s)
            LogType.WARNING -> Log.w(LOG_TAG, s)
            LogType.ERROR -> Log.e(LOG_TAG, s)
            else -> Log.d(LOG_TAG, s)
        }

        val fullMessage = addTimestamp("${type.tag}: $s")
        writeRawString(fullMessage, isWriteToFile)
    }

    private fun writeRawString(s: String, isWriteToFile: Boolean) {
        writeToBuffer(s)
        if (isWriteToFile) {
            writeToFile(s)
        }
    }

    fun writeRawString(s: String) {
        writeRawString(s, isWriteToFile)
    }

    private fun addTimestamp(s: String): String {
        return "%s - %s".format(
            DateFormat.format("yyyy.MM.dd HH:mm:ss", Calendar.getInstance().time),
            s
        )
    }

    /**
     * Запись логов в буфер.
     * @param s
     */
    private fun writeToBuffer(s: String) {
        buffer.append(s)
        buffer.appendLine()
    }

    /**
     * Запись логов в файл.
     * @param mes
     */
    private fun writeToFile(mes: String) {
        if (fullFileName.isNullOrBlank() || dirPath.isNullOrBlank()) return

        val logFile = File(fullFileName!!)
        if (!logFile.exists()) {
            try {
                // проверка существования каталога
                val dir = File(dirPath!!)
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        logErrorWithoutFile("Unable to create directory to save log: $dirPath", true)
                        return
                    } else {
                        log("The log directory was created: $dirPath", LogType.DEBUG, false)
                    }
                }
                // попытка создания лог-файла
                logFile.createNewFile()
            } catch (ex: IOException) {
                logErrorWithoutFile(ex, true)
                return
            }
        }
        try {
            BufferedWriter(FileWriter(logFile, true)).let {
                it.append(mes)
                it.newLine()
                it.close()
            }
        } catch (ex: IOException) {
            logErrorWithoutFile(ex, true)
        }
    }

    companion object {
        private const val LOG_TAG = "MYTETROID"
        private const val LOG_FILE_NAME = "mytetroid.log"
    }
}