package com.gee12.mytetroid.data;

import android.os.FileObserver;

public class TetroidFileObserver {

    private static FileObserver mFileObserver;

    /**
     * Запуск отслеживания изменения mytetra.xml.
     * @param callback
     */
    public static void startStorageObserver(/*Context context, */ICallback callback) {
       /* Intent intent = new Intent(context, FileObserverService.class);
        String filePath = DataManager.getStoragePath() + "/" + DataManager.MYTETRA_XML_FILE_NAME;
        intent.putExtra(FileObserverService.EXTRA_FILE_PATH, filePath);
        context.startService(intent);
        instance.mStorageObserver = intent;*/

        String filePath = DataManager.getStoragePath() + "/" + DataManager.MYTETRA_XML_FILE_NAME;
        mFileObserver = new FileObserver(filePath, FileObserver.MODIFY) {
            @Override
            public void onEvent(int event, String path) {
                if (event == FileObserver.MODIFY) {
                    callback.run(true);
                }
            }

        };
        mFileObserver.startWatching();
    }

    /**
     * Перезапуск отслеживания изменения mytetra.xml.
     */
    public static void restartStorageObserver() {
        if (mFileObserver != null) {
            // перезапускаем отслеживание, например, тогда, когда
            // при сохранении "исходный" файл mytetra.xml перемещается в корзину,
            // и нужно запустить отслеживание по указанному пути заново, чтобы привязаться
            // к только что созданному актуальному файлу mytetra.xml
            mFileObserver.stopWatching();
            mFileObserver.startWatching();
        }
    }

    /**
     * Остановка отслеживания изменения mytetra.xml.
//     * @param context
     */
    public static void stopStorageObserver(/*Context context*/) {
//        context.stopService(instance.mStorageObserver);
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
            mFileObserver = null;
        }
    }

    /**
     * Запуск или остановка отслеживания (после изменения настроек программы).
     * @param isStart
     * @param callback
     */
    public static void startOrStopStorageObserver(boolean isStart, ICallback callback) {
        if (isStart) {
            if (mFileObserver == null) {
                startStorageObserver(callback);
            } else {
                restartStorageObserver();
            }
        } else {
            stopStorageObserver();
        }
    }
}
