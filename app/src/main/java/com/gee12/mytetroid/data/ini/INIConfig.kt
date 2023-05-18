package com.gee12.mytetroid.data.ini

import com.gee12.mytetroid.logs.ITetroidLogger
import org.ini4j.Ini
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception

open class INIConfig(
    protected var logger: ITetroidLogger?
) {

    protected var config: Ini = Ini()

    @Throws(Exception::class)
    fun load(inputStream: InputStream) {
        config.load(inputStream)
    }

    @Throws(Exception::class)
    fun save(outputStream: OutputStream) {
        config.store(outputStream)
    }

    fun reset() {
        config = Ini()
    }

    protected fun getSection(key: String): MutableMap<String, String>? {
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