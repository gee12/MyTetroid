package com.gee12.mytetroid.common.extensions

fun String.takeIfNotEmpty(): String? {
    return ifEmpty { null }
}

fun Boolean?.orFalse(): Boolean {
    return this ?: false
}

fun Int?.orZero(): Int {
    return this ?: 0
}

fun Int.toBoolean(): Boolean {
    return this > 0
}

fun Boolean.toInt(): Int {
    return if (this) 1 else 0
}

fun <T> String?.ifNotEmpty(block: (String) -> T) : T? {
    return if (!this.isNullOrEmpty()) {
        block(this)
    } else {
        null
    }
}

fun <T> Boolean.takeIfTrue(predicate: () -> T): T? {
    return if (this) {
        predicate()
    } else {
        null
    }
}