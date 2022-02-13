package com.gee12.mytetroid.interactors

import android.app.Application
import android.os.Bundle
import android.os.FileObserver
import com.gee12.mytetroid.R
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.services.FileObserverService

class StorageTreeInteractor(
    private val app: Application,
    private val logger: ITetroidLogger
) {

    fun startStorageTreeObserver(storagePath: String) {
        val bundle = Bundle().apply {
            putInt(FileObserverService.EXTRA_ACTION_ID, FileObserverService.ACTION_ID_START)
            putString(FileObserverService.EXTRA_FILE_PATH, storagePath)
            putInt(FileObserverService.EXTRA_EVENT_MASK, FileObserver.ALL_EVENTS)
        }
        startStorageTreeObserver(bundle)
    }

    fun startStorageTreeObserver(bundle: Bundle) {
        FileObserverService.sendCommand(app, bundle)
        logger.log(app.getString(R.string.log_mytetra_xml_observer_mask, app.getString(R.string.launched)), false)
    }

    fun stopStorageTreeObserver() {
        FileObserverService.sendCommand(app, FileObserverService.ACTION_ID_STOP)
        FileObserverService.stop(app)
        logger.log(app.getString(R.string.log_mytetra_xml_observer_mask, app.getString(R.string.stopped)), false)
    }

    fun restartStorageTreeObserver() {
        FileObserverService.sendCommand(app, FileObserverService.ACTION_ID_RESTART)
        logger.log(app.getString(R.string.log_mytetra_xml_observer_mask, app.getString(R.string.relaunched)), false)
    }

}