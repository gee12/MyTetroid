package com.gee12.mytetroid;

import android.os.FileObserver;

import com.gee12.mytetroid.data.ICallback;

import java.io.File;

public class TetroidFileObserver {

    private FileObserver mFileObserver;

    /**
     *
     * @param filePath
     * @param mask
     */
    public TetroidFileObserver(String filePath, int mask, ICallback callback) {
        if (filePath == null)
            return;
        File file = new File(filePath);
//        Thread t = new Thread(new Runnable() {
//            @Override
//            public void run() {
                mFileObserver = new FileObserver(file, mask) {
                    @Override
                    public void onEvent(int event, String path) {
                        if ((event & mask) > 0) {
                            callback.run(true);
                        }
                    }
                };
                mFileObserver.startWatching();
//            }
//        });
//        t.setPriority(Thread.MIN_PRIORITY);
//        t.start();
    }

    /**
     * Запуск отслеживания изменения mytetra.xml.
     * @param callback
     */
//    public static void startStorageObserver(/*Context context,*/ ICallback callback) {
//        /*Intent intent = new Intent(context, FileObserverService.class);
//        String filePath = DataManager.getStoragePath() + "/" + DataManager.MYTETRA_XML_FILE_NAME;
//        intent.putExtra(FileObserverService.EXTRA_FILE_PATH, filePath);
//        context.startService(intent);
//        instance.mStorageObserver = intent;*/
//
//        Thread t = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                File file = new File(DataManager.getStoragePath() + "/" + DataManager.MYTETRA_XML_FILE_NAME);
//                mFileObserver = new FileObserver(file, FileObserver.MODIFY) {
//                    @Override
//                    public void onEvent(int event, String path) {
//                        if (event == FileObserver.MODIFY) {
//                            callback.run(true);
//                        }
//                    }
//
//                };
//                mFileObserver.startWatching();
//            }
//        });
//        t.setPriority(Thread.MIN_PRIORITY);
//        t.start();
//    }

    /**
     * Запуск отслеживания изменений файла.
     */
    public void startObserver() {
        mFileObserver.startWatching();
    }

    /**
     * Перезапуск отслеживания изменений файла.
     */
    public void restartObserver() {
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
     * Остановка отслеживания изменений файла.
//     * @param context
     */
    public void stopObserver(/*Context context*/) {
//        context.stopService(instance.mStorageObserver);
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
            mFileObserver = null;
        }
    }

    /**
     * Запуск или остановка отслеживания (после изменения настроек программы).
     * @param isStart
//     * @param callback
     */
    public void startOrStopObserver(boolean isStart/*, ICallback callback*/) {
        if (isStart) {
            if (mFileObserver == null) {
//                startObserver(callback);
                startObserver();
            } else {
                restartObserver();
            }
        } else {
            stopObserver();
        }
    }
}
