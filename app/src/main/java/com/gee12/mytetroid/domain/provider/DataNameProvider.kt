package com.gee12.mytetroid.domain.provider

import com.gee12.mytetroid.model.ImageFileType
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

interface IDataNameProvider {
    fun createUniqueId(): String
    fun createUniqueImageName(): String
    fun createDateTimePrefix(): String
}

/**
 * Провайдер имен для данных хранилища.
 */
class DataNameProvider(
) : IDataNameProvider {

    companion object {
        const val ID_SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyz"

        const val UNIQUE_ID_HALF_LENGTH = 10
        const val PREFIX_DATE_TIME_FORMAT = "yyyyMMddHHmmssSSS"
    }

    /**
     * Генерация уникального идентификатора для объектов хранилища.
     * @return
     */
    override fun createUniqueId(): String {
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

    override fun createUniqueImageName(): String {
        return "image${createUniqueId()}.${ImageFileType.PNG.extension}"
    }

    override fun createDateTimePrefix(): String {
        return SimpleDateFormat(PREFIX_DATE_TIME_FORMAT, Locale.getDefault()).format(Date()).orEmpty()
    }

}