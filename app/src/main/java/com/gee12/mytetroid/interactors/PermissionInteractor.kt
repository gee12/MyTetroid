package com.gee12.mytetroid.interactors

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
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.logs.ITetroidLogger
import java.lang.Exception


class PermissionInteractor(
    private val logger: ITetroidLogger
) {
    companion object {
        const val PERMISSION_TERMUX = "com.termux.permission.RUN_COMMAND"
    }

    /**
     * Проверка разрешения на запись во внешнюю память.
     */
    fun hasWriteExtStoragePermission(context: Context): Boolean {
        /*return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                true
            }
        }*/
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Запрос разрешения на запись во внешнюю память.
     */
    fun requestWriteExtStoragePermissions(
        activity: Activity,
        requestCode: Int,
        onManualPermissionRequest: (() -> Unit) -> Unit
    ) {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            logger.log(activity.getString(R.string.log_request_perm_mask, Manifest.permission.MANAGE_EXTERNAL_STORAGE), false)
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
        }*/
        requestPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, requestCode)
    }

    /**
     * Проверка разрешений.
     * @return true, если разрешение не требуется или уже предоставлено. false, если сделан запрос разрешения
     */
    fun checkPermission(
        activity: Activity,
        permission: String,
        requestCode: Int,
        onManualPermissionRequest: (() -> Unit) -> Unit
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // проверяем разрешение
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    // спрашиваем у пользователя
                    onManualPermissionRequest {
                        requestPermission(activity, permission, requestCode)
                    }
                } else {
                    // отправляем запрос на разрешение
                    requestPermission(activity, permission, requestCode)
                }
                return false
            }
        }
        return true
    }

    fun requestPermission(activity: Activity, permission: String, requestCode: Int) {
        logger.log(activity.getString(R.string.log_request_perm_mask) + permission, false)
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    /**
     * Универсальная проверка разрешения.
     */
    fun checkPermission(data: PermissionRequestData): Boolean {
        return if (data.permission == Constants.TetroidPermission.WriteStorage) {
            checkWriteExtStoragePermission(data)
        } else {
            checkPermission(
                data.activity,
                data.permission.toAndroidPermission(),
                data.requestCode,
                data.onManualPermissionRequest
            )
        }
    }

    private fun Constants.TetroidPermission.toAndroidPermission(): String {
        return when (this) {
            Constants.TetroidPermission.ReadStorage -> Manifest.permission.READ_EXTERNAL_STORAGE
            // запуск камеры для захвата изображения
            Constants.TetroidPermission.Camera -> Manifest.permission.CAMERA
            // запуск команд Termux
            Constants.TetroidPermission.Termux -> PERMISSION_TERMUX
            else -> ""
        }
    }

    fun checkWriteExtStoragePermission(data: PermissionRequestData): Boolean {
        return if (hasWriteExtStoragePermission(data.activity)) {
            true
        } else {
            requestWriteExtStoragePermissions(
                data.activity,
                data.requestCode,
                data.onManualPermissionRequest
            )
            false
        }
    }

}

data class PermissionRequestData(
    val permission: Constants.TetroidPermission,
    val activity: Activity,
    val requestCode: Int,
    val onManualPermissionRequest: (() -> Unit) -> Unit
)
