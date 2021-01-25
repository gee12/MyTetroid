package com.gee12.mytetroid.data;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.logs.LogManager;

public class SyncManager {

    public static final String EXTRA_APP_NAME = "com.gee12.mytetroid.EXTRA_APP_NAME";
    public static final String EXTRA_SYNC_COMMAND = "com.gee12.mytetroid.EXTRA_SYNC_COMMAND";

    /**
     * Отправка запроса на синхронизацию стороннему приложению.
     * @param storagePath
     */
    public static void startStorageSync(Activity activity, String storagePath, int requestCode) {
        String command = SettingsManager.getSyncCommand(activity);
        if (SettingsManager.getSyncAppName(activity).equals(activity.getString(R.string.app_termux))) {
            // termux

        } else {
            // mgit
            startMGitSync(activity, storagePath, command, requestCode);
        }
    }

    /**
     * Отправка запроса на синхронизацию в MGit.
     * @param storagePath
     */
    private static void startMGitSync(Activity activity, String storagePath, String command, int requestCode) {
        Intent intent = SyncManager.createCommandSender(activity, storagePath, command);

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
        }
    }

    private static Intent createCommandSender(Context context, String storagePath, String command) {
        Intent intent = new Intent(Intent.ACTION_SYNC);
        intent.addCategory(Intent.CATEGORY_DEFAULT);

//        Uri uri = Uri.fromFile(new File(mStoragePath));
        Uri uri = Uri.parse("content://" + storagePath);
        intent.setDataAndType(uri, "text/plain");
        intent.putExtra(EXTRA_APP_NAME, context.getPackageName());
        intent.putExtra(EXTRA_SYNC_COMMAND, command);
        return intent;
    }

    private static void startTermuxSync(Activity activity, String storagePath, String command) {
        Intent intent = new Intent();
        intent.setClassName("com.termux", "com.termux.app.RunCommandService");
        intent.setAction("com.termux.RUN_COMMAND");
        intent.putExtra("com.termux.RUN_COMMAND_PATH", command);
        // TODO: parse command for params
//        intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-n", "5"});
//        intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        activity.startService(intent);
    }
}
