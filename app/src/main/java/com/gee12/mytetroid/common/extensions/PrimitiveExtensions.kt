package com.gee12.mytetroid.common.extensions

fun Int?.orZero(): Int {
    return this ?: 0
}

fun String.ifNotEmpty(block: (String) -> Unit) {
    if (this.isNotEmpty()) {
        block(this)
    }
}

fun <T> Boolean.ifTrueOrNull(predicate: () -> T): T? {
    return if (this) {
        predicate()
    } else {
        null
    }
}