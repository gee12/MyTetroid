package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Intent
import com.gee12.mytetroid.R
import com.gee12.mytetroid.TetroidStorageData
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.helpers.IStorageInfoProvider
import kotlinx.coroutines.*

class StorageInfoViewModel(
    app: Application,
    val storageData: TetroidStorageData?
) : StorageViewModel(app, storageData), CoroutineScope {

    enum class Event {
        MyTetraXmlLastModifiedDate,
        StorageFolderSize
    }

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
            val size = withContext(Dispatchers.IO) {
                storageInteractor.getStorageFolderSize(getContext()) ?: getString(R.string.title_error)
            }
            setEvent(Event.StorageFolderSize, size)
        }
    }

    fun computeMyTetraXmlLastModifiedDate() {
        launch {
            val date = withContext(Dispatchers.IO) {
                storageInteractor.getMyTetraXmlLastModifiedDate(getContext())?.let {
                    Utils.dateToString(it, getString(R.string.full_date_format_string))
                } ?: getString(R.string.title_error)
            }
            setEvent(Event.MyTetraXmlLastModifiedDate, date)
        }
    }

    fun getStorageInfo(): IStorageInfoProvider = storageDataProcessor

}
