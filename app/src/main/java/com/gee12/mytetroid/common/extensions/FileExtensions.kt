package com.gee12.mytetroid.common.extensions

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.Formatter
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.extension.toDocumentFile
import com.anggrayudi.storage.file.getAbsolutePath
import com.gee12.mytetroid.model.FileName
import java.io.File
import java.io.InputStream
import kotlin.io.readBytes as kotlinReadBytes


fun String.parseUri(): Uri {
    var uri = Uri.parse(this)
    if (uri.scheme == null) {
        uri = Uri.fromFile(File(this))
    }
    return uri
}

fun Uri.toPath(): String? {
    return path
}

fun String.uriToAbsolutePath(context: Context): String {
    val uri = Uri.parse(this)
    return uri.toDocumentFile(context)?.getAbsolutePath(context)?.toNullIfEmpty()
        ?: uri.toPath()
        ?: this
}

fun String.uriToAbsolutePathIfPossible(context: Context): String? {
    val uri = Uri.parse(this)
    return uri.toDocumentFile(context)?.getAbsolutePath(context)
}

fun String.toFile(): File {
    return File(this)
}

fun makePath(vararg destinations: String): String {
    return destinations.joinToString(File.separator) { it.trimEnd(File.separatorChar) }
}

fun makeFolderPath(vararg destinations: String): String {
    return makePath(*destinations).plus(File.separator)
}

fun makePathToFile(vararg destinations: String): File {
    return makePath(*destinations).toFile()
}

fun String.getNameFromPath(): String {
    val lastIndex = lastIndexOf(File.separatorChar)
    return if (lastIndex > -1) substring(lastIndex + 1) else ""
}

fun String.getExtensionWithoutComma(): String {
    val lastIndex = lastIndexOf(".")
    return if (lastIndex > -1) substring(lastIndex + 1) else ""
}

fun String.withExtension(ext: String): String {
    return "$this.$ext".trimEnd('.')
}

fun String.splitToBaseAndExtension(): FileName {
    val nameParts = this.split(".")
    val size = nameParts.size
    return if (size >= 2) {
        FileName.FromParts(
            base = nameParts[size-2],
            extension = nameParts[size-1]
        )
    } else {
        FileName.FromFullName(
            fullName = this
        )
    }
}

fun Uri.getFileName(context: Context): String? {
    return when(scheme) {
        ContentResolver.SCHEME_CONTENT -> this.getContentFileName(context)
        ContentResolver.SCHEME_FILE -> lastPathSegment // pathSegments.last()
        else -> null
    }
}

private fun Uri.getContentFileName(context: Context): String? {
    return runCatching {
        context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
        }
    }.getOrNull()
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

fun InputStream.readBytes(): ByteArray {
    return use {
        it.kotlinReadBytes()
    }
}
fun InputStream.readText(): String {
    return use {
        bufferedReader().use {
            it.readText()
        }
    }
}

/**
 * Получение размера файла/каталога в байтах.
 */
fun DocumentFile.getSize(): Long {
    if (!this.exists()) return 0
    var size: Long = 0
    if (!this.isDirectory) {
        size = this.length()
    } else {
        val folders = mutableListOf<DocumentFile>()
        folders.add(this)
        while (folders.isNotEmpty()) {
            val folder = folders.removeAt(0)
            if (!folder.exists()) {
                continue
            }
            val listFiles = folder.listFiles()
            if (listFiles.isEmpty()) {
                continue
            }
            for (child in listFiles) {
                size += child.length()
                if (child.isDirectory) {
                    folders.add(child)
                }
            }
        }
    }
    return size
}

fun DocumentFile.getFormattedSize(context: Context): String {
    return Formatter.formatFileSize(context, getSize())
}
