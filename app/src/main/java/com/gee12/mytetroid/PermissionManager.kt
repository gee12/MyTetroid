package com.gee12.mytetroid

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.views.dialogs.AskDialogs


class PermissionInteractor(
    private val logger: ITetroidLogger
) {
    companion object {
        const val PERMISSION_TERMUX = "com.termux.permission.RUN_COMMAND"
    }

    /**
     * Проверка разрешений.
     * @param activity
     * @param permission
     * @param code
     * @param askStringRes
     * @return
     */
    //    @TargetApi(Build.VERSION_CODES.M)
    fun checkPermission(activity: Activity, permission: String, code: Int, askStringRes: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // проверяем разрешение
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    // выводим диалог с объяснием зачем нужно разрешение
                    AskDialogs.showYesDialog(
                        activity, { requestPermission(activity, permission, code) },
                        askStringRes
                    )
                } else {
                    // отправляем запрос на разрешение
                    requestPermission(activity, permission, code)
                }
                return false
            }
        }
        return true
    }

    fun requestPermission(activity: Activity, permission: String, code: Int) {
        logger.log(activity.getString(R.string.log_request_perm) + permission, false)
        ActivityCompat.requestPermissions(activity, arrayOf(permission), code)
    }

    /**
     * Проверка разрешения на запись во внешнюю память.
     * @return
     */
    fun checkWriteExtStoragePermission(activity: Activity, code: Int): Boolean {
        return checkPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, code, R.string.ask_request_write_ext_storage)
    }

    /**
     * Проверка разрешения на использование камеры.
     * @return
     */
    fun checkCameraPermission(activity: Activity, code: Int): Boolean {
        return checkPermission(activity, Manifest.permission.CAMERA, code, R.string.ask_request_camera)
    }

    fun writeExtStoragePermGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Проверка разрешения на запуск команд Termux.
     * @return
     */
    fun checkTermuxPermission(activity: Activity, code: Int): Boolean {
        return checkPermission(activity, PERMISSION_TERMUX, code, R.string.ask_permission_termux)
    }
}