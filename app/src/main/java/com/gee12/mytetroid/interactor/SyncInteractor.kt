package com.gee12.mytetroid.interactor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.SyncStateContract
import android.text.TextUtils
import android.widget.Toast
import com.gee12.mytetroid.PermissionManager
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.SettingsManager
import com.gee12.mytetroid.data.StorageManager
import com.gee12.mytetroid.data.SyncManager
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.utils.Utils
import com.gee12.mytetroid.views.Message

class SyncInteractor {

    companion object {
        const val EXTRA_APP_NAME = "com.gee12.mytetroid.EXTRA_APP_NAME"
        const val EXTRA_SYNC_COMMAND = "com.gee12.mytetroid.EXTRA_SYNC_COMMAND"
        const val STORAGE_PATH_REPLACEMENT = "%a"
    }

    /**
     * Отправка запроса на синхронизацию стороннему приложению.
     * @param storagePath
     */
    fun startStorageSync(activity: Activity, storagePath: String, requestCode: Int): Boolean {
        val command = SettingsManager.getSyncCommandDef(activity)
        return if (SettingsManager.getSyncAppNameDef(activity) == activity.getString(R.string.title_app_termux)) {
            // termux
            startTermuxSync(activity, storagePath, command)
        } else {
            // mgit
            startMGitSync(activity, storagePath, command, requestCode)
        }
    }

    /**
     * Отправка запроса на синхронизацию в MGit.
     * @param storagePath
     */
    private fun startMGitSync(activity: Activity, storagePath: String, command: String, requestCode: Int): Boolean {
        val intent = createIntentToMGit(activity, storagePath, command)
        LogManager.log(activity, activity.getString(R.string.log_start_storage_sync) + command)
        try {
            if (!SettingsManager.isNotRememberSyncApp(activity)) {
                // использовать стандартный механизм запоминания используемого приложения
                activity.startActivityForResult(intent, requestCode)
            } else { // или спрашивать постоянно
                activity.startActivityForResult(
                    Intent.createChooser(
                        intent,
                        activity.getString(R.string.title_choose_sync_app)
                    ), requestCode
                )
            }
        } catch (ex: Exception) {
            LogManager.log(activity, activity.getString(R.string.log_sync_app_not_installed), Toast.LENGTH_LONG)
            LogManager.log(activity, ex, -1)
            return false
        }
        return true
    }

    /**
     * Создание Intent для отправки в MGit.
     * @param context
     * @param storagePath
     * @param command
     * @return
     */
    private fun createIntentToMGit(context: Context, storagePath: String, command: String) =
        Intent(Intent.ACTION_SYNC).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
//        Uri uri = Uri.fromFile(new File(mStoragePath));
            val uri = Uri.parse("content://$storagePath")
            setDataAndType(uri, "text/plain")
            putExtra(EXTRA_APP_NAME, context.packageName)
            putExtra(EXTRA_SYNC_COMMAND, command)
        }

    /**
     * Запуск команды/скрипта синхронизации с помощью Termux.
     * @param activity
     * @param storagePath
     * @param command
     */
    private fun startTermuxSync(activity: Activity, storagePath: String, command: String): Boolean {
        if (TextUtils.isEmpty(command)) {
            LogManager.log(activity, R.string.log_sync_command_empty, ILogger.Types.WARNING, Toast.LENGTH_LONG)
            return false
        }
        // проверяем разрешение на запуск сервиса
        if (!PermissionManager.checkTermuxPermission(activity, Constants.REQUEST_CODE_PERMISSION_TERMUX)) {
            return false
        }
        // подставляем путь
        var res = command
        if (command.contains(STORAGE_PATH_REPLACEMENT)) {
            res = command.replace(STORAGE_PATH_REPLACEMENT, storagePath)
        }
        // проверяем есть ли параметры
        var words: Array<String>? = res.split(" ".toRegex()).toTypedArray()
        val first = words!![0]
        words = if (words.size > 1) {
            Utils.removeArrayItem(words, 0)
        } else {
            null
        }
        return sendTermuxCommand(activity, first, words, null, true)
    }

    /**
     * Отправка команды/скрипта на выполнение в Termux.
     * Дополнительная информация:
     * https://github.com/termux/termux-app/blob/master/app/src/main/java/com/termux/app/RunCommandService.java
     *
     * @param activity
     * @param command
     * @param args
     */
    private fun sendTermuxCommand(activity: Activity, command: String, args: Array<String>?, workDir: String?, bg: Boolean): Boolean {
        val intent = Intent().apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            action = "com.termux.RUN_COMMAND"
            putExtra("com.termux.RUN_COMMAND_PATH", command)
            if (args != null && args.size > 0) {
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args)
            }
            if (!TextUtils.isEmpty(workDir)) {
                putExtra("com.termux.RUN_COMMAND_WORKDIR", workDir)
            }
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", bg)
        }
        val fullCommand = (command + " " + args?.let { TextUtils.join(" ", args) })
        LogManager.log(activity, activity.getString(R.string.log_start_storage_sync) + fullCommand)
        try {
            activity.startService(intent)
        } catch (ex: Exception) {
            LogManager.log(activity, R.string.log_error_when_sync, ILogger.Types.ERROR, Toast.LENGTH_LONG)
            Message.showSnackMoreInLogs(activity)
            return false
        }
        return true
    }
}