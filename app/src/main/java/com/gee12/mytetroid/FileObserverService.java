package com.gee12.mytetroid;

import android.app.Service;
import android.content.Intent;
import android.os.FileObserver;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.gee12.mytetroid.activities.MainActivity;

public class FileObserverService extends Service {

    public static final String EXTRA_ACTION_ID = "EXTRA_ACTION_ID";
    public static final String EXTRA_FILE_PATH = "EXTRA_FILE_PATH";
    public static final String EXTRA_EVENT_MASK = "EXTRA_EVENT_MASK";

    public static final int ACTION_START = 1;
    public static final int ACTION_STOP = 2;
    public static final int ACTION_RESTART = 3;

    private TetroidFileObserver mFileObserver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!intent.hasExtra(EXTRA_ACTION_ID)) {
            return super.onStartCommand(intent, flags, startId);
        }

        int actionId = intent.getIntExtra(EXTRA_ACTION_ID, 0);
        switch (actionId) {
            case ACTION_START:
                if (intent.hasExtra(EXTRA_FILE_PATH)) {
                    String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
                    int mask = intent.getIntExtra(EXTRA_EVENT_MASK, FileObserver.ALL_EVENTS);
                    this.mFileObserver = new TetroidFileObserver(filePath, mask, res -> callback());
                    mFileObserver.startObserver();
                }
                break;
            case ACTION_STOP:
                mFileObserver.stopStorageObserver();
                break;
            case ACTION_RESTART:
                mFileObserver.restartStorageObserver();
                break;
        }
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void callback() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
