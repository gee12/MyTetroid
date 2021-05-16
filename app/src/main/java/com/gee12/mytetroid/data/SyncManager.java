package com.gee12.mytetroid.data;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.gee12.mytetroid.PermissionManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.views.Message;

public class SyncManager {

    public static final String EXTRA_APP_NAME = "com.gee12.mytetroid.EXTRA_APP_NAME";
    public static final String EXTRA_SYNC_COMMAND = "com.gee12.mytetroid.EXTRA_SYNC_COMMAND";
    public static final String STORAGE_PATH_REPLACEMENT = "%a";

    /**
     * Отправка запроса на синхронизацию стороннему приложению.
     * @param storagePath
     */
    public static boolean startStorageSync(Activity activity, String storagePath, int requestCode) {
        String command = SettingsManager.getSyncCommand(activity);
        if (SettingsManager.getSyncAppName(activity).equals(activity.getString(R.string.title_app_termux))) {
            // termux
            return startTermuxSync(activity, storagePath, command);
        } else {
            // mgit
            return startMGitSync(activity, storagePath, command, requestCode);
        }
    }

    /**
     * Отправка запроса на синхронизацию в MGit.
     * @param storagePath
     */
    private static boolean startMGitSync(Activity activity, String storagePath, String command, int requestCode) {
        Intent intent = SyncManager.createIntentToMGit(activity, storagePath, command);

        LogManager.log(activity, activity.getString(R.string.log_start_storage_sync) + command);
        try {
            if (!SettingsManager.isNotRememberSyncApp(activity)) {
                // использовать стандартный механизм запоминания используемого приложения
                activity.startActivityForResult(intent, requestCode);
            } else { // или спрашивать постоянно
                activity.startActivityForResult(Intent.createChooser(intent,
                        activity.getString(R.string.title_choose_sync_app)), requestCode);
            }
        } catch (Exception ex) {
            LogManager.log(activity, activity.getString(R.string.log_sync_app_not_installed), Toast.LENGTH_LONG);
            LogManager.log(activity, ex, -1);
            return false;
        }
        return true;
    }

    /**
     * Создание Intent для отправки в MGit.
     * @param context
     * @param storagePath
     * @param command
     * @return
     */
    private static Intent createIntentToMGit(Context context, String storagePath, String command) {
        Intent intent = new Intent(Intent.ACTION_SYNC);
        intent.addCategory(Intent.CATEGORY_DEFAULT);

//        Uri uri = Uri.fromFile(new File(mStoragePath));
        Uri uri = Uri.parse("content://" + storagePath);
        intent.setDataAndType(uri, "text/plain");
        intent.putExtra(EXTRA_APP_NAME, context.getPackageName());
        intent.putExtra(EXTRA_SYNC_COMMAND, command);
        return intent;
    }

    /**
     * Запуск команды/скрипта синхронизации с помощью Termux.
     * @param activity
     * @param storagePath
     * @param command
     */
    private static boolean startTermuxSync(Activity activity, String storagePath, String command) {
        if (TextUtils.isEmpty(command)) {
            LogManager.log(activity, R.string.log_sync_command_empty, ILogger.Types.WARNING, Toast.LENGTH_LONG);
            return false;
        }
        // проверяем разрешение на запуск сервиса
        if (!PermissionManager.checkTermuxPermission(activity, StorageManager.REQUEST_CODE_PERMISSION_TERMUX)) {
            return false;
        }
        // подставляем путь
        if (command.contains(STORAGE_PATH_REPLACEMENT)) {
            command = command.replace(STORAGE_PATH_REPLACEMENT, storagePath);
        }
        // проверяем есть ли параметры
        String[] words = command.split(" ");
        String first = words[0];
        if (words.length > 1) {
            Utils.removeArrayItem(words, 0);
        } else {
            words = null;
        }
        return sendTermuxCommand(activity, first, words, null,true);
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
    private static boolean sendTermuxCommand(Activity activity, String command, String[] args, String workDir, boolean bg) {
        Intent intent = new Intent();
        intent.setClassName("com.termux", "com.termux.app.RunCommandService");
        intent.setAction("com.termux.RUN_COMMAND");

        intent.putExtra("com.termux.RUN_COMMAND_PATH", command);
        if (args != null && args.length > 0) {
            intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args);
        }
        if (!TextUtils.isEmpty(workDir)) {
            intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", workDir);
        }
        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", bg);

        String fullCommand = command + " "
                + ((args != null && args.length > 0) ? TextUtils.join(" ", args) : "");

        LogManager.log(activity, activity.getString(R.string.log_start_storage_sync) + fullCommand);
        try {
            activity.startService(intent);
        } catch (Exception ex) {
            LogManager.log(activity, R.string.log_error_when_sync, ILogger.Types.ERROR, Toast.LENGTH_LONG);
            Message.showSnackMoreInLogs(activity);
            return false;
        }
        return true;
    }
}
