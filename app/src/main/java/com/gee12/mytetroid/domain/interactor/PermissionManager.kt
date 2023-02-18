package com.gee12.mytetroid.domain.interactor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.enums.TetroidPermission


class PermissionManager(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
) {
    companion object {
        const val PERMISSION_TERMUX = "com.termux.permission.RUN_COMMAND"
    }

    /**
     * Проверка разрешения на запись во внешнюю память.
     */
    fun hasWriteExtStoragePermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                true
            }
        }
        //return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Запрос разрешения на запись во внешнюю память.
     */
    fun requestWriteExtStoragePermissions(
        activity: Activity,
        requestCode: Int,
        onManualPermissionRequest: (() -> Unit) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            logger.log(activity.getString(R.string.log_request_permission_mask, Manifest.permission.MANAGE_EXTERNAL_STORAGE), false)
            onManualPermissionRequest {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        addCategory("android.intent.category.DEFAULT")
                        data = Uri.parse("package:%s".format(activity.packageName))
                    }
                    activity.startActivityForResult(intent, requestCode)
                } catch (ex: Exception) {
                    val intent = Intent().apply {
                        action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    }
                    activity.startActivityForResult(intent, requestCode)
                }
            }
        } else {
            requestPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, requestCode)
        }
        //requestPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, requestCode)
    }

    /**
     * Проверка разрешений.
     * @return true, если разрешение не требуется или уже предоставлено. false, если сделан запрос разрешения
     */
    fun checkPermission(
        activity: Activity,
        androidPermission: String,
        requestCode: Int,
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

    fun requestPermission(activity: Activity, permission: String, requestCode: Int) {
        logger.log(resourcesProvider.getString(R.string.log_request_permission_mask, permission), false)
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    /**
     * Универсальная проверка разрешения.
     */
    fun checkPermission(data: PermissionRequestData): Boolean {
        return if (data.permission == TetroidPermission.WriteStorage) {
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

data class PermissionRequestData(
    val permission: TetroidPermission,
    val activity: Activity,
    val requestCode: Int,
    val onManualPermissionRequest: (() -> Unit) -> Unit
)
