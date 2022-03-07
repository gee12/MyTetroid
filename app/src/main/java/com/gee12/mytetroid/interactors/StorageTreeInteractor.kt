package com.gee12.mytetroid.interactors

import android.app.Application
import android.os.FileObserver
import com.gee12.mytetroid.R
import com.gee12.mytetroid.TetroidFileObserver
import com.gee12.mytetroid.logs.ITetroidLogger

class StorageTreeInteractor(
    private val app: Application,
    private val logger: ITetroidLogger
) {

    private var fileObserver: TetroidFileObserver? = null

    suspend fun startStorageTreeObserver(storagePath: String, callback: (TetroidFileObserver.Event) -> Unit) {
        fileObserver?.stop()
        fileObserver = null

        fileObserver = TetroidFileObserver(
            filePath = storagePath,
            mask = FileObserver.ALL_EVENTS
        ) { event ->
            callback(event)
        }

        fileObserver?.create()
        fileObserver?.start()
    }

    fun stopStorageTreeObserver() {
        fileObserver?.stop()
        fileObserver = null
        logger.log(app.getString(R.string.log_mytetra_xml_observer_mask, app.getString(R.string.stopped)), false)
    }

}