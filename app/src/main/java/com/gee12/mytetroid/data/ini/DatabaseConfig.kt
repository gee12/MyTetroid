package com.gee12.mytetroid.data.ini

import com.gee12.mytetroid.logs.ITetroidLogger
import java.io.OutputStream
import kotlin.Throws
import java.lang.Exception

class DatabaseConfig(
    logger: ITetroidLogger?
) : INIConfig(logger) {

    inner class EmptyFieldException(val fieldName: String) : Exception("Field '$fieldName' is empty in database.ini")

    companion object {
        const val INI_SECTION_GENERAL = "General"
        const val INI_CRYPT_CHECK_SALT = "crypt_check_salt"
        const val INI_CRYPT_CHECK_HASH = "crypt_check_hash"
        const val INI_MIDDLE_HASH_CHECK_DATA = "middle_hash_check_data"
        const val INI_CRYPT_MODE = "crypt_mode"
        const val INI_VERSION = "version"
        const val DEF_VERSION = "1"
    }

    init {
        config.config.apply {
            // отключаем экранирование символов, т.к. сохраняем значения в кавычках
            isEscape = false
            // убираем пробелы между ключем и значением
            isStrictOperator = true
            // убираем повторения разделов
            isMultiSection = false
            // убираем повторения параметров
            isMultiOption = false
        }
    }

    @get:Throws(EmptyFieldException::class)
    val cryptCheckHash: String
        get() = getWithoutQuotes(INI_CRYPT_CHECK_HASH)

    @get:Throws(EmptyFieldException::class)
    val cryptCheckSalt: String
        get() = getWithoutQuotes(INI_CRYPT_CHECK_SALT)

    @get:Throws(EmptyFieldException::class)
    val middleHashCheckData: String
        get() = getWithoutQuotes(INI_MIDDLE_HASH_CHECK_DATA)

    @get:Throws(EmptyFieldException::class)
    val isCryptMode: Boolean
        get() = getValueFromGeneral(INI_CRYPT_MODE) == "1"

    @Throws(Exception::class)
    fun savePassword(outputStream: OutputStream, passHash: String?, salt: String?, cryptMode: Boolean) {
        setValueToGeneralWithQuotes(INI_CRYPT_CHECK_HASH, passHash)
        setValueToGeneralWithQuotes(INI_CRYPT_CHECK_SALT, salt)
        setValueToGeneral(INI_CRYPT_MODE, if (cryptMode) "1" else "0")
        save(outputStream)
    }

    @Throws(Exception::class)
    fun saveCheckData(outputStream: OutputStream, checkData: String?) {
        setValueToGeneralWithQuotes(INI_MIDDLE_HASH_CHECK_DATA, checkData)
        save(outputStream)
    }

    @Throws(Exception::class)
    fun saveDefault(outputStream: OutputStream) {
        setValueToGeneral(INI_CRYPT_CHECK_HASH, null)
        setValueToGeneral(INI_CRYPT_CHECK_SALT, null)
        setValueToGeneral(INI_CRYPT_MODE, "0")
        setValueToGeneral(INI_MIDDLE_HASH_CHECK_DATA, null)
        setValueToGeneral(INI_VERSION, DEF_VERSION)
        save(outputStream)
    }

    /**
     * Получение значения по ключу.
     * @param key
     * @return
     */
    @Throws(EmptyFieldException::class)
    fun getValueFromGeneral(key: String): String {
        val res = get(INI_SECTION_GENERAL, key)
        if (res == null || res.isEmpty()) {
            throw EmptyFieldException(key)
        }
        return res
    }

    /**
     * Получение значения по ключу без двойных кавычек вначале и вконце.
     * @param key
     * @return
     */
    @Throws(EmptyFieldException::class)
    fun getWithoutQuotes(key: String): String {
        return getValueFromGeneral(key).replace("^\"|\"$".toRegex(), "")
    }

    fun setValueToGeneral(key: String, value: String?) {
        set(INI_SECTION_GENERAL, key, value.orEmpty())
    }

    fun setValueToGeneralWithQuotes(key: String, value: String?) {
        setValueToGeneral(key, if (value != null) "\"" + value + "\"" else "")
    }
}