package com.gee12.mytetroid.common.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import com.gee12.mytetroid.common.utils.FileUtils

/**
 * Получение каталога приложения во внешнем хранилище (удаляется вместе с приложением).
 */
fun Context.getAppExternalFilesDir(): String? {
    return getExternalFilesDir(null)?.absolutePath
}


/**
 * Получение общедоступного каталога "Документы" или каталога приложения, если первый недоступен.
 * @param forWrite
 */
fun Context.getExternalPublicDocsOrAppDir(forWrite: Boolean): String? {
    val externalState = Environment.getExternalStorageState()
    return if ((!forWrite || Environment.MEDIA_MOUNTED_READ_ONLY != externalState)
        && Environment.MEDIA_MOUNTED == externalState
    ) {
        FileUtils.getExternalPublicDocsDir()
    } else {
        getAppExternalFilesDir()
    }
}

fun Context.writeToClipboard(label: String?, text: String?) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}