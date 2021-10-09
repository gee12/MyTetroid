package com.gee12.mytetroid.logs

import android.text.format.DateFormat
import android.util.Log
import com.gee12.mytetroid.BuildConfig
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

abstract class FileTetroidLogger : BaseTetroidLogger() {

    private var dirPath: String? = null
    private var isWriteToFile = false
    private val buffer = StringBuilder()

    /**
     * Получение списка логов, которые не удалось записать в файл.
     * @return
     */
    val bufferString: String
        get() = buffer.toString()

    /**
     * Получение полного пути к лог-файлу.
     * @return
     */
    var fullFileName: String? = null
        private set

    fun init(path: String, isWriteToFile: Boolean) {
        setLogPath(path)
        this.isWriteToFile = isWriteToFile
    }

    fun setLogPath(path: String) {
        dirPath = path
        fullFileName = String.format("%s%s%s", path, File.separator, LOG_FILE_NAME)
    }

    fun log(s: String, type: LogType) {
        logToFile(s, type, isWriteToFile)
    }

    override fun logWithoutShow(s: String, type: LogType) {
        log(s, type)
    }

    fun logWithoutFile(s: String, type: LogType, show: Boolean = true) {
        logToFile(s, type, show)
    }

    fun logErrorWithoutFile(s: String, show: Boolean = true) {
        logWithoutFile(s, LogType.ERROR, show)
    }

    fun logErrorWithoutFile(ex: Exception, show: Boolean = true) {
        logErrorWithoutFile(getExceptionInfo(ex), show)
    }

    fun logToFile(s: String, type: LogType, isWriteToFile: Boolean) {
//        if (type == LogType.DEBUG && !BuildConfig.DEBUG) return

        when (type) {
            LogType.INFO -> Log.i(LOG_TAG, s)
            LogType.WARNING -> Log.w(LOG_TAG, s)
            LogType.ERROR -> Log.e(LOG_TAG, s)
            else -> Log.d(LOG_TAG, s)
        }

        val fullMessage = addTime("${type.tag}: $s")
        writeToBuffer(fullMessage)
        if (isWriteToFile) {
            writeToFile(fullMessage)
        }
    }

    private fun addTime(s: String): String {
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
        val logFile = File(fullFileName)
        if (!logFile.exists()) {
            try {
                // проверка существования каталога
                val dir = File(dirPath)
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
        private const val CALLER_STACK_INDEX = 5
        private const val LOG_TAG = "MYTETROID"
        private const val LOG_FILE_NAME = "mytetroid.log"

        fun getExceptionInfo(ex: Exception): String {
            val caller = Thread.currentThread().stackTrace[CALLER_STACK_INDEX]
            val fullClassName = caller.className
            val className = fullClassName.substring(fullClassName.lastIndexOf('.') + 1)
            val methodName = caller.methodName
            val lineNumber = caller.lineNumber
            return "%s.%s():%d\n%s".format(className, methodName, lineNumber, ex.message)
        }
    }
}