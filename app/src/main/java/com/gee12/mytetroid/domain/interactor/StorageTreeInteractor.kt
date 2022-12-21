package com.gee12.mytetroid.domain.interactor

import android.app.Application
import android.os.FileObserver
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.TetroidFileObserver
import com.gee12.mytetroid.logs.ITetroidLogger

class StorageTreeInteractor(
    private val app: Application,
    private val logger: ITetroidLogger
) {

    private var fileObserver: TetroidFileObserver? = null

    var treeChangedCallback: ((TetroidFileObserver.Event) -> Unit)? = null
    var observerStartedCallback: (() -> Unit)? = null
    var observerStoppedCallback: (() -> Unit)? = null

    suspend fun startStorageTreeObserver(storagePath: String/*, callback: (TetroidFileObserver.Even) -> Unitt*/) {
        fileObserver?.stop()
        fileObserver = null

        fileObserver = TetroidFileObserver(
            filePath = storagePath,
            mask = FileObserver.ALL_EVENTS
        ) { event ->
            treeChangedCallback?.invoke(event)
        }

        fileObserver?.create()
        fileObserver?.start()

        observerStartedCallback?.invoke()
    }

    fun stopStorageTreeObserver() {
        fileObserver?.stop()
        fileObserver = null
        logger.log(app.getString(R.string.log_mytetra_xml_observer_mask, app.getString(R.string.stopped)), false)

        observerStoppedCallback?.invoke()
    }

}