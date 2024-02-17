package com.gee12.mytetroid.common.extensions


fun Byte.toHex(): String {
    return "0x%02x".format(this)
}

fun Int.toHex(): String {
    return "0x%02x".format(this)
}

fun ByteArray.toHex(): String {
    return joinToString(separator = "") { it.toHex() }
}

