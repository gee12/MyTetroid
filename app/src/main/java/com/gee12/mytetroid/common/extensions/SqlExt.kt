package com.gee12.mytetroid.common.extensions

import android.database.DatabaseUtils

fun String?.toSql(): String {
    return if (isNullOrBlank()) "null" else DatabaseUtils.sqlEscapeString(this)
}

fun String?.toSqlLike(): String {
    return if (isNullOrBlank()) "null" else DatabaseUtils.sqlEscapeString("%${this}%")
}

fun String?.toSqlLikeNotNull(): String {
    return DatabaseUtils.sqlEscapeString("%${this.orEmpty()}%")
}

fun String?.toSqlNotNull(): String {
    return if (isNullOrBlank()) "''" else DatabaseUtils.sqlEscapeString(this)
}

fun Int?.toSql(): String {
    return if (this == null) "null" else "$this"
}

fun Int?.toSql(defaultValue: String): String {
    return this?.toSql() ?: defaultValue
}

fun Long?.toSql(): String {
    return if (this == null) "null" else "$this"
}

fun Float?.toSql(): String {
    return if (this == null) "null" else "$this"
}

fun Boolean?.toSql(): String {
    return if (this == null) "null" else (if (this) "1" else "0")
}

fun List<String>.toSqlIn(): String {
    return if (this.size > 1) {
        val query = this.joinToString(", ", "(", ")") { it.toSql() }
        " IN $query "
    } else {
        " = ${this.firstOrNull().toSqlNotNull()} "
    }
}

fun String?.toSqlIfNull(): String {
    return if (this.isNullOrEmpty()) {
        "IS NULL"
    } else {
        "= ${this.toSql()}"
    }
}