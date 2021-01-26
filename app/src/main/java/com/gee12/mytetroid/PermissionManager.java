package com.gee12.mytetroid;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.gee12.mytetroid.dialogs.AskDialogs;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;

public class PermissionManager {

    public static final String PERMISSION_TERMUX = "com.termux.permission.RUN_COMMAND";

    /**
     * Проверка разрешений.
     * @param activity
     * @param permission
     * @param code
     * @param askStringRes
     * @return
     */
//    @TargetApi(Build.VERSION_CODES.M)
    public static boolean checkPermission(Activity activity, String permission, int code, int askStringRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // проверяем разрешение
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    // выводим диалог с объяснием зачем нужно разрешение
                    AskDialogs.showYesDialog(activity, () ->
                                    requestPermission(activity, permission, code),
                                    askStringRes);
                } else {
                    // отправляем запрос на разрешение
                    requestPermission(activity, permission, code);
                }
                return false;
            }
        }
        return true;
    }

    public static void requestPermission(Activity activity, String permission, int code) {
        LogManager.log(activity, activity.getString(R.string.log_request_perm) + permission, ILogger.Types.INFO);
        ActivityCompat.requestPermissions(activity, new String[] { permission }, code);
    }

    /**
     * Проверка разрешения на запись во внешнюю память.
     * @return
     */
    public static boolean checkWriteExtStoragePermission(Activity activity, int code) {
        return checkPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, code, R.string.ask_request_write_ext_storage);
    }

    /**
     * Проверка разрешения на использование камеры.
     * @return
     */
    public static boolean checkCameraPermission(Activity activity, int code) {
        return checkPermission(activity, Manifest.permission.CAMERA, code, R.string.ask_request_camera);
    }

    public static boolean writeExtStoragePermGranted(Context context) {
        return (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Проверка разрешения на запуск команд Termux.
     * @return
     */
    public static boolean checkTermuxPermission(Activity activity, int code) {
        return checkPermission(activity, PERMISSION_TERMUX, code, R.string.ask_permission_termux);
    }

}
