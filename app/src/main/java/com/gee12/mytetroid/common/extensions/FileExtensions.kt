package com.gee12.mytetroid.common.extensions

import java.io.File


fun String.toFile(): File {
    return File(this)
}

fun makePath(vararg destinations: String): String {
    return destinations.joinToString(File.separator)
}

fun makeFolderPath(vararg destinations: String): String {
    return destinations.joinToString(File.separator).plus(File.separator)
}

fun makePathToFile(vararg destinations: String): File {
    return makePath(*destinations).toFile()
}

fun String.isFileExist(): Boolean {
    return try {
        File(this).exists()
    } catch (ex: Exception) {
        ex.printStackTrace()
        false
    }
}

fun File?.isDirEmpty(): Boolean {
    return this == null || listFiles()?.size == 0
}

fun String.isDirEmpty(): Boolean {
    return isEmpty() || toFile().isDirEmpty()
}
