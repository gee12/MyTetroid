package com.gee12.mytetroid.common.extensions

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


fun String.toDate(pattern: String): Date? {
    return try {
        val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        dateFormat.parse(this)
    } catch (ex: ParseException) {
        ex.printStackTrace()
        null
    }
}

fun Date.format(pattern: String): String? {
    return try {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.format(this)
    } catch (ex: Exception) {
        ex.printStackTrace()
        null
    }
}

fun checkDateFormatString(format: String): Boolean {
    return Date().format(format) != null
}
