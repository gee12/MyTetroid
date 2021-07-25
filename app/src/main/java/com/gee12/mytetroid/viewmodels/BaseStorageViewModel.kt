package com.gee12.mytetroid.viewmodels

import android.app.Application
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.SingleLiveEvent
import com.gee12.mytetroid.interactors.StorageInteractor
import com.gee12.mytetroid.repo.StoragesRepo

open class BaseStorageViewModel(app: Application, private val repo: StoragesRepo) : BaseViewModel(app) {

    val viewEvent: SingleLiveEvent<ViewModelEvent<Constants.ViewEvents, Any>> = SingleLiveEvent()
    val storageEvent: SingleLiveEvent<ViewModelEvent<Constants.StorageEvents, Any>> = SingleLiveEvent()


    fun updateViewState(state: Constants.ViewEvents, param: Any? = null) {
        viewEvent.postValue(ViewModelEvent(state, param))
    }

    fun updateStorageState(state: Constants.StorageEvents, param: Any? = null) {
        storageEvent.postValue(ViewModelEvent(state, param))
    }

    fun getLastFolderPathOrDefault(forWrite: Boolean) = StorageInteractor.getLastFolderPathOrDefault(getContext(), forWrite)

    fun onPermissionChecked() {
        storageEvent.postValue(ViewModelEvent(Constants.StorageEvents.PermissionChecked))
    }
}