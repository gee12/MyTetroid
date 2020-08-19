package com.gee12.mytetroid.data;

import android.app.Service;
import android.content.Intent;
import android.os.FileObserver;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.gee12.mytetroid.LogManager;

public class FileObserverService extends Service {

    public static final String EXTRA_FILE_PATH = "EXTRA_FILE_PATH";

    private FileObserver mFileObserver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ((intent.hasExtra(EXTRA_FILE_PATH))) {
            String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
            mFileObserver = new FileObserver(filePath) {
                @Override
                public void onEvent(int event, String path) {
                    LogManager.log("Event " + event + " with " + filePath);
                    if (event == FileObserver.MODIFY) {
                        LogManager.log(filePath +" modified!!!!!!!!!!!!!!!");
                    }
                }
            };
            mFileObserver.startWatching();
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
