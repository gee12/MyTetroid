package com.gee12.mytetroid.data.ini

import com.gee12.mytetroid.logs.ITetroidLogger
import org.ini4j.Ini
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception

/**
 * Параметры хранилища.
 */
open class INIConfig(
    protected var logger: ITetroidLogger?
) {

    protected var config: Ini = Ini()

    /**
     * Загрузка параметров из файла.
     */
    fun load(inputStream: InputStream): Boolean {
        try {
            config.load(inputStream)
        } catch (ex: IOException) {
            logger?.logError("Configuration error: ", ex, show = false)
            return false
        }
        return true
    }

    /**
     * Сохранение параметров в файл.
     */
    fun save(outputStream: OutputStream): Boolean {
        try {
            config.store(outputStream)
        } catch (ex: Exception) {
            logger?.logError("Configuration error: ", ex, show = false)
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