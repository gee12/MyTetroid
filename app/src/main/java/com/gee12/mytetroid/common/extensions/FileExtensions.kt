package com.gee12.mytetroid.common.extensions

import java.io.File


fun String.toFile(): File {
    return File(this)
}

fun makePath(vararg destinations: String): String {
    return destinations.joinToString(File.separator)
}

fun makeFilePath(vararg destinations: String): File {
    return makePath(*destinations).toFile()
}
