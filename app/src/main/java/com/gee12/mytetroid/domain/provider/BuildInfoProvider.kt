package com.gee12.mytetroid.domain.provider

import android.content.Context
import android.os.Build
import com.gee12.mytetroid.BuildConfig

class BuildInfoProvider(context: Context) {

    val isDebug: Boolean = BuildConfig.DEBUG

    val applicationId: String = BuildConfig.APPLICATION_ID

    fun isFullVersion() = BuildConfig.FLAVOR == "pro"
    fun isFreeVersion() = BuildConfig.FLAVOR == "free"

    val appUpdateTime: Long = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime

    val appVersionName: String = BuildConfig.VERSION_NAME
    val appVersionCode: Int = BuildConfig.VERSION_CODE

    val sdkVersionName: String = Build.VERSION.RELEASE
    val sdkVersionCode: Int = Build.VERSION.SDK_INT

    val brand = Build.BRAND
    val model = Build.MODEL

}