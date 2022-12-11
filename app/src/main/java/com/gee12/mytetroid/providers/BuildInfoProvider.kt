package com.gee12.mytetroid.providers

import android.content.Context
import android.os.Build
import com.gee12.mytetroid.BuildConfig

class BuildInfoProvider(context: Context) {

    val isDebug: Boolean = BuildConfig.DEBUG

    fun isFullVersion() = BuildConfig.FLAVOR == "pro"

    fun isFreeVersion() = BuildConfig.FLAVOR == "free"

    val appUpdateTime: Long = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime

    val appVersionName: String = BuildConfig.VERSION_NAME
    val appVersionCode: Int = BuildConfig.VERSION_CODE

    val androidVersionName: String = Build.VERSION.RELEASE
    val androidVersionCode: Int = Build.VERSION.SDK_INT

    val isLegacyStorage: Boolean = androidVersionCode < Build.VERSION_CODES.Q

    val brand = Build.BRAND

    val model = Build.MODEL

}