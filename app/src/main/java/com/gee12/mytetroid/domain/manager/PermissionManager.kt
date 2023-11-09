package com.gee12.mytetroid.domain.manager

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.model.permission.PermissionRequestData
import com.gee12.mytetroid.model.permission.TetroidPermission


class PermissionManager(
    buildInfoProvider: BuildInfoProvider,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
) {
    companion object {
        const val PERMISSION_TERMUX = "com.termux.permission.RUN_COMMAND"
    }

    // has permission MANAGE_EXTERNAL_STORAGE
    private val hasAllFilesAccessVersion = buildInfoProvider.hasAllFilesAccessVersion()

    /**
     * Проверка разрешения на запись во внешнюю память.
     */
    fun hasWriteExtStoragePermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (hasAllFilesAccessVersion) {
                    Environment.isExternalStorageManager()
                } else {
                    // используем SAF, специально разрешения спрашивать не нужно
                    true
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                true
            }
        }
    }

    /**
     * Запрос разрешения на запись во внешнюю память.
     */
    fun requestWriteExtStoragePermissions(
        activity: Activity,
        requestCode: PermissionRequestCode,
        onManualPermissionRequest: (() -> Unit) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (hasAllFilesAccessVersion) {
                logger.log(activity.getString(R.string.log_permission_request_mask, Manifest.permission.MANAGE_EXTERNAL_STORAGE), false)
                onManualPermissionRequest {
                    requestManageExternalStoragePermission(activity, requestCode)
                }
            } else {
                // используем SAF, специально разрешения спрашивать не нужно
            }
        } else {
            requestPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, requestCode)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestManageExternalStoragePermission(activity: Activity, requestCode: PermissionRequestCode) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                addCategory("android.intent.category.DEFAULT")
                data = Uri.parse("package:%s".format(activity.packageName))
            }
            activity.startActivityForResult(intent, requestCode.code)
        } catch (ex: Exception) {
            val intent = Intent().apply {
                action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            }
            activity.startActivityForResult(intent, requestCode.code)
        }
    }

    /**
     * Проверка разрешений.
     * @return true, если разрешение не требуется или уже предоставлено. false, если сделан запрос разрешения
     */
    fun checkPermission(
        activity: Activity,
        androidPermission: String,
        requestCode: PermissionRequestCode,
        onManualPermissionRequest: (() -> Unit) -> Unit
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // проверяем разрешение
            if (ContextCompat.checkSelfPermission(activity, androidPermission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, androidPermission)) {
                    // спрашиваем у пользователя
                    onManualPermissionRequest {
                        requestPermission(activity, androidPermission, requestCode)
                    }
                } else {
                    // отправляем запрос на разрешение
                    requestPermission(activity, androidPermission, requestCode)
                }
                return false
            }
        }
        return true
    }

    private fun requestPermission(
        activity: Activity,
        permission: String,
        requestCode: PermissionRequestCode,
    ) {
        logger.log(resourcesProvider.getString(R.string.log_permission_request_mask, permission), false)
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode.code)
    }

    /**
     * Универсальная проверка разрешения.
     */
    fun checkPermission(data: PermissionRequestData): Boolean {
        return if (data.permission is TetroidPermission.FileStorage.Write) {
            checkWriteExtStoragePermission(data)
        } else {
            checkPermission(
                activity = data.activity,
                androidPermission = data.permission.toAndroidPermission(),
                requestCode = data.requestCode,
                onManualPermissionRequest = data.onManualPermissionRequest,
            )
        }
    }

    fun checkWriteExtStoragePermission(data: PermissionRequestData): Boolean {
        return if (hasWriteExtStoragePermission(data.activity)) {
            true
        } else {
            requestWriteExtStoragePermissions(
                activity = data.activity,
                requestCode = data.requestCode,
                onManualPermissionRequest = data.onManualPermissionRequest,
            )
            false
        }
    }

}
