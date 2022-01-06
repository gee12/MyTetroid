package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Intent
import com.gee12.mytetroid.common.Constants
import kotlinx.coroutines.*

class StorageInfoViewModel(
    app: Application
) : StorageViewModel(app), CoroutineScope {

    fun startInitStorage(intent: Intent) {
        launch {
            setViewEvent(Constants.ViewEvents.TaskStarted)
            withContext(Dispatchers.IO) {
                if (!super.initStorage(intent)) {
                    postStorageEvent(Constants.StorageEvents.InitFailed)
                }
            }
            setViewEvent(Constants.ViewEvents.TaskFinished)
        }
    }

    fun computeStorageFolderSize() {
        launch(Dispatchers.IO) {
            val size = super.getStorageFolderSize()
            postEvent(Event.StorageFolderSize, size)
        }
    }

    fun computeMyTetraXmlLastModifiedDate() {
        launch(Dispatchers.IO) {
            val date = super.getMyTetraXmlLastModifiedDate()
            postEvent(Event.MyTetraXmlLastModifiedDate, date)
        }
    }

    fun getStorageInfo() = xmlLoader

    enum class Event {
        MyTetraXmlLastModifiedDate,
        StorageFolderSize
    }

}
