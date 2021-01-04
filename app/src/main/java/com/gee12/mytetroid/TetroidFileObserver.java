package com.gee12.mytetroid;

import android.os.FileObserver;

import com.gee12.mytetroid.data.ICallback;

import java.io.File;

public class TetroidFileObserver {

    private FileObserver mFileObserver;
//    private Thread thread;

    /**
     *
     * @param filePath
     * @param mask
     */
    public TetroidFileObserver(String filePath, int mask, ICallback callback) {
        if (filePath == null)
            return;
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
//        Thread thread = new Thread(new Runnable() {
//        this.thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
        mFileObserver = new FileObserver(filePath, mask) {
//                mFileObserver = new FileObserver(file, mask) {
            @Override
            public void onEvent(int event, String path) {
//                if ((event & mask) > 0) {
                    callback.run(true);
//                }
            }
        };
        startObserver();
//            }
//        });
//        thread.setPriority(Thread.MIN_PRIORITY);
//        thread.start();
    }

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
    public void stopObserver() {
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
            mFileObserver = null;
        }
    }
}
