package com.gee12.mytetroid;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class FileObserverService extends Service {

    public static final String EXTRA_ACTION_ID = "EXTRA_ACTION_ID";
    public static final String EXTRA_FILE_PATH = "EXTRA_FILE_PATH";
    public static final String EXTRA_EVENT_MASK = "EXTRA_EVENT_MASK";
    public static final String EXTRA_IS_START = "EXTRA_IS_START";
    public static final String ACTION_OBSERVER_EVENT_COME = "com.gee12.mytetroid.ACTION_OBSERVER_EVENT_COME";

    public static final int ACTION_START = 1;
    public static final int ACTION_STOP = 2;
    public static final int ACTION_RESTART = 3;
    public static final int ACTION_START_OR_STOP = 4;

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
//                    mFileObserver.startObserver();
                }
                break;
            case ACTION_STOP:
                if (mFileObserver != null) {
                    mFileObserver.stopObserver();
                }
                break;
            case ACTION_RESTART:
                if (mFileObserver != null) {
                    mFileObserver.restartObserver();
                }
                break;
            /*case ACTION_START_OR_STOP:
                boolean isStart = intent.getBooleanExtra(EXTRA_IS_START, true);
                mFileObserver.startOrStopObserver(isStart);
                break;*/
        }
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void callback() {
        Intent intent = new Intent(ACTION_OBSERVER_EVENT_COME);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public static void sendCommand(Activity activity, int actionId) {
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_ACTION_ID, actionId);
        sendCommand(activity, bundle);
    }

    public static void sendCommand(Activity activity, Bundle bundle) {
        Intent intent = new Intent(activity, FileObserverService.class);
        intent.putExtras(bundle);
        activity.startService(intent);
    }

    public static void stop(Activity activity) {
        Intent intent = new Intent(activity, FileObserverService.class);
        activity.stopService(intent);
    }
}
