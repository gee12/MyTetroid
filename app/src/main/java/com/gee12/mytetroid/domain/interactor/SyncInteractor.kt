package com.gee12.mytetroid.domain.interactor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.buildIntent
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.ui.TetroidMessage


class SyncInteractor(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
) {

    companion object {
        const val EXTRA_APP_NAME = "com.gee12.mytetroid.EXTRA_APP_NAME"
        const val EXTRA_SYNC_COMMAND = "com.gee12.mytetroid.EXTRA_SYNC_COMMAND"
        const val STORAGE_PATH_REPLACEMENT = "%a"
    }

    /**
     * Отправка запроса на синхронизацию стороннему приложению.
     */
    fun startStorageSync(
        activity: Activity,
        storagePath: String,
        command: String,
        appName: String,
        requestCode: Int
    ): Boolean {
        return when (appName) {
            resourcesProvider.getString(R.string.title_app_termux) -> {
                startTermuxSync(activity, storagePath, command)
            }
            resourcesProvider.getString(R.string.title_app_mgit) -> {
                startMGitSync(activity, storagePath, command, requestCode)
            }
            resourcesProvider.getString(R.string.title_app_autosync) -> {
                startAutosyncSync(activity, command)
            }
            else -> {
                logger.logError(resourcesProvider.getString(R.string.log_storage_sync_unknown_app_mask, appName))
                false
            }
        }
    }

    fun isWaitSyncResult(appName: String): Boolean {
        return appName == resourcesProvider.getString(R.string.title_app_mgit)
    }

    /**
     * Отправка запроса на синхронизацию в MGit.
     */
    private fun startMGitSync(activity: Activity, storagePath: String, command: String, requestCode: Int): Boolean {
        logger.log(resourcesProvider.getString(R.string.log_start_storage_sync_mask, resourcesProvider.getString(R.string.title_app_mgit), command), false)
        if (command.isEmpty()) {
            logger.logError(R.string.log_sync_command_empty, true)
            return false
        }
        val intent = createIntentToMGit(activity, storagePath, command)
        try {
            if (!CommonSettings.isNotRememberSyncApp(activity)) {
                // использовать стандартный механизм запоминания используемого приложения
                activity.startActivityForResult(intent, requestCode)
            } else { // или спрашивать постоянно
                activity.startActivityForResult(
                    Intent.createChooser(
                        intent,
                        resourcesProvider.getString(R.string.title_choose_sync_app)
                    ), requestCode
                )
            }
        } catch (ex: Exception) {
            logger.logError(ex, false)
            logger.logError(resourcesProvider.getString(R.string.log_sync_app_not_installed), true)
            return false
        }
        return true
    }

    /**
     * Создание Intent для отправки в MGit.
     */
    private fun createIntentToMGit(context: Context, storagePath: String, command: String): Intent {
        return Intent(Intent.ACTION_SYNC).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
//        Uri uri = Uri.fromFile(new File(mStoragePath));
            val uri = Uri.parse("content://$storagePath")
            setDataAndType(uri, "text/plain")
            putExtra(EXTRA_APP_NAME, context.packageName)
            putExtra(EXTRA_SYNC_COMMAND, command)
        }
    }

    /**
     * Запуск команды/скрипта синхронизации с помощью Termux.
     */
    private fun startTermuxSync(activity: Activity, storagePath: String, command: String): Boolean {
        logger.log(resourcesProvider.getString(R.string.log_start_storage_sync_mask, resourcesProvider.getString(R.string.title_app_termux), command), false)
        if (command.isEmpty()) {
            logger.logError(R.string.log_sync_command_empty, true)
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
     */
    private fun sendTermuxCommand(activity: Activity, command: String, args: Array<String>?, workDir: String?, bg: Boolean): Boolean {
        val intent = buildIntent {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            action = "com.termux.RUN_COMMAND"
            putExtra("com.termux.RUN_COMMAND_PATH", command)
            if (args?.isNotEmpty() == true) {
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args)
            }
            if (!workDir.isNullOrEmpty()) {
                putExtra("com.termux.RUN_COMMAND_WORKDIR", workDir)
            }
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", bg)
        }
        val fullCommand = (command + " " + args?.let { TextUtils.join(" ", args) })
        logger.log(resourcesProvider.getString(R.string.log_start_storage_sync_mask) + fullCommand)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(intent)
            } else {
                activity.startService(intent)
            }
            logger.log(resourcesProvider.getString(R.string.log_started_storage_sync_mask, resourcesProvider.getString(R.string.title_app_termux)), true)
        } catch (ex: Exception) {
            logger.logError(ex, false)
            logger.logError(R.string.log_error_when_sync, true)
            TetroidMessage.showSnackMoreInLogs(activity)
            return false
        }
        return true
    }

    /**
     * Отправка намерения на выполнение синхронизации в Autosync.
     * Дополнительная информация:
     * https://metactrl.com/autosync/automation/
     */
    private fun startAutosyncSync(activity: Activity, command: String): Boolean {
        val intent = createIntentToAutosync(parseExtrasToAutosync(command))
        logger.log(resourcesProvider.getString(R.string.log_start_storage_sync_mask, resourcesProvider.getString(R.string.title_app_autosync), command), false)
        try {
            activity.sendBroadcast(intent)
            logger.log(resourcesProvider.getString(R.string.log_started_storage_sync_mask, resourcesProvider.getString(R.string.title_app_autosync)), true)
        } catch (ex: Exception) {
            logger.logError(ex, false)
            logger.logError(R.string.log_error_when_sync, true)
            TetroidMessage.showSnackMoreInLogs(activity)
            return false
        }
        return true
    }

    /**
     * Получение extras из строки.
     */
    private fun parseExtrasToAutosync(command: String): Array<Pair<String, String>>? {
        return if (command.isBlank()) {
            null
        } else {
            val extras = command.split(",")
            if (extras.isNotEmpty()) {
                extras.mapNotNull {
                    val withoutQuotes = it.trim('{', '}')
                    val keyValue = withoutQuotes.split(":")
                    if (keyValue.size >= 2) {
                        Pair(
                            first = keyValue[0].trim(),
                            second = keyValue[1].trim()
                        )
                    } else {
                        logger.logError(resourcesProvider.getString(R.string.log_error_when_parse_autosync_extra_mask, it), false)
                        null
                    }
                }.toTypedArray()
            } else {
                logger.logError(R.string.log_error_when_parse_autosync_extras, false)
                null
            }
        }
    }

    /**
     * Создание Intent для отправки в Autosync.
     */
    private fun createIntentToAutosync(extras: Array<Pair<String, String>>?): Intent {
        return buildIntent {
            setClassName("com.ttxapps.autosync", "com.ttxapps.autosync.Automation")
            action = "syncNow"
            extras?.forEach {
                putExtra(it.first, it.second)
            }
        }
    }
}