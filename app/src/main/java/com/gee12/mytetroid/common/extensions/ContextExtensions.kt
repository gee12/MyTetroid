package com.gee12.mytetroid.common.extensions

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

fun Context.getAppVersionName(): String? {
    return try {
        val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
        return pInfo.versionName
    } catch (ex: PackageManager.NameNotFoundException) {
        null
    }
}