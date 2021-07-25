package com.gee12.mytetroid.viewmodels

import android.app.Application
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.SingleLiveEvent
import com.gee12.mytetroid.repo.StoragesRepo

open class BaseStorageViewModel(app: Application, private val repo: StoragesRepo) : BaseViewModel(app) {

    val stateEvent: SingleLiveEvent<ViewModelEvent<Constants.StorageEvents, Any>> = SingleLiveEvent()

    val readStorageStateEvent: SingleLiveEvent<ViewModelEvent<Constants.ActivityEvents, ReadDecryptStorageState>> = SingleLiveEvent()

    fun onPermissionChecked() {
        stateEvent.postValue(ViewModelEvent(Constants.StorageEvents.PermissionChecked))
    }
}