package com.gee12.mytetroid.common.extensions

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Environment
import com.gee12.mytetroid.common.utils.FileUtils

fun Context.getAppVersionName(): String? {
    return try {
        val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
        return pInfo.versionName
    } catch (ex: PackageManager.NameNotFoundException) {
        null
    }
}

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