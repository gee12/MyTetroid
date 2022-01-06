package com.gee12.mytetroid.data.ini

import com.gee12.mytetroid.logs.ITetroidLogger
import org.ini4j.Ini
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.lang.Exception

/**
 * Параметры хранилища.
 */
open class INIConfig(
    protected var logger: ITetroidLogger?
) {

    protected var config: Ini = Ini()
    private var fileName: String? = null

    /**
     * Установка имени конфигурационного файла.
     */
    fun setFileName(fileName: String?) {
        this.fileName = fileName
    }

    /**
     * Загрузка параметров из файла.
     * @return
     */
    fun load(): Boolean {
        try {
            config.load(FileReader(fileName))
        } catch (e: IOException) {
            logger?.logError("Configuration error: ", e, false)
            return false
        }
        return true
    }

    /**
     * Сохранение параметров в файл.
     * @return
     */
    fun save(): Boolean {
        try {
            config.store(FileWriter(fileName))
        } catch (e: Exception) {
            logger?.logError("Configuration error: ", e, false)
            return false
        }
        return true
    }

    fun getSection(key: String): MutableMap<String, String>? {
        return config[key]
    }

    operator fun get(sectionKey: String, key: String): String? {
        return getSection(sectionKey)?.get(key)
    }

    operator fun set(sectionKey: String, key: String, value: String) {
        var section = getSection(sectionKey)
        if (section == null) {
            section = config.add(sectionKey)
        }
        section?.set(key, value)
    }
}