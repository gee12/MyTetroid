package com.gee12.mytetroid

import android.os.FileObserver
import android.util.Log
import com.gee12.mytetroid.common.extensions.toHex
import java.io.File

class TetroidFileObserver(
    private val filePath: String,
    private val mask: Int = FileObserver.ALL_EVENTS,
    private val callback: (Event) -> Unit
) {
    enum class Event(val id: Int) {
        Modified(1),
        Moved(2),
        Deleted(3);

        companion object {
            fun fromId(id: Int) = values().firstOrNull { it.id == id }
        }
    }

    private lateinit var fileObserver: FileObserver


    init {
        create()
    }

    private fun create() {
        val file = File(filePath)
        if (!file.exists()) {
            return
        }

        fileObserver = object : FileObserver(filePath, mask) {
            override fun onEvent(event: Int, path: String?) {
                Log.d("MyTetroid", "FileObserver event=${event.toHex()} path=$path")
                if (mask == 0 || mask == ALL_EVENTS || event and mask > 0) {
                    when (event) {
                        MODIFY -> {
                            callback(Event.Modified)
                        }
                        MOVE_SELF -> {
                            callback(Event.Moved)
                        }
                        DELETE_SELF -> {
                            callback(Event.Deleted)
                        }
                    }
                }
            }
        }
    }

    /**
     * Запуск отслеживания изменений файла.
     */
    fun start() {
        fileObserver.startWatching()
    }

    /**
     * Перезапуск отслеживания изменений файла.
     */
    fun restart() {
        // перезапускаем отслеживание, например, тогда, когда
        // при сохранении "исходный" файл mytetra.xml перемещается в корзину,
        // и нужно запустить отслеживание по указанному пути заново, чтобы привязаться
        // к только что созданному актуальному файлу mytetra.xml
        fileObserver.stopWatching()
        fileObserver.startWatching()
    }

    /**
     * Остановка отслеживания изменений файла.
     * //     * @param context
     */
    fun stop() {
        fileObserver.stopWatching()
    }

}