package com.gee12.mytetroid.common.extensions

fun String.ifNotEmpty(block: (String) -> Unit) {
    if (this.isNotEmpty()) {
        block(this)
    }
}
