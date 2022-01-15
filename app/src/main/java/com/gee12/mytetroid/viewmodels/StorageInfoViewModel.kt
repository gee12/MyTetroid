package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Intent
import com.gee12.mytetroid.TetroidStorageData
import com.gee12.mytetroid.common.Constants
import kotlinx.coroutines.*

class StorageInfoViewModel(
    app: Application,
    val storageData: TetroidStorageData?
) : StorageViewModel(app, storageData), CoroutineScope {

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
        launch {
            val size = withContext(Dispatchers.IO) { super.getStorageFolderSize() }
            setEvent(Event.StorageFolderSize, size)
        }
    }

    fun computeMyTetraXmlLastModifiedDate() {
        launch {
            val date = withContext(Dispatchers.IO) { super.getMyTetraXmlLastModifiedDate() }
            setEvent(Event.MyTetraXmlLastModifiedDate, date)
        }
    }

    fun getStorageInfo() = xmlLoader

    enum class Event {
        MyTetraXmlLastModifiedDate,
        StorageFolderSize
    }

}
