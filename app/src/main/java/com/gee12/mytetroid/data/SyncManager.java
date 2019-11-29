package com.gee12.mytetroid.data;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class SyncManager {

    public static final String EXTRA_APP_NAME = "com.gee12.mytetroid.EXTRA_APP_NAME";
    public static final String EXTRA_SYNC_COMMAND = "com.gee12.mytetroid.EXTRA_SYNC_COMMAND";

    public static Intent createCommandSender(Context context, String storagePath, String command) {
        Intent intent = new Intent(Intent.ACTION_SYNC);
        intent.addCategory(Intent.CATEGORY_DEFAULT);

//        Uri uri = Uri.fromFile(new File(storagePath));
        Uri uri = Uri.parse("content://" + storagePath);
        intent.setDataAndType(uri, "text/plain");
        intent.putExtra(EXTRA_APP_NAME, context.getPackageName());
        intent.putExtra(EXTRA_SYNC_COMMAND, command);
        return intent;
    }
}
