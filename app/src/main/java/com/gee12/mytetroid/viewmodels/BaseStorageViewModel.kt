package com.gee12.mytetroid.viewmodels

import android.app.Application
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.SingleLiveEvent
import com.gee12.mytetroid.interactors.StorageInteractor
import com.gee12.mytetroid.repo.StoragesRepo

open class BaseStorageViewModel(app: Application, private val repo: StoragesRepo) : BaseViewModel(app) {

    val viewEvent: SingleLiveEvent<ViewModelEvent<Constants.ViewEvents, Any>> = SingleLiveEvent()
    val storageEvent: SingleLiveEvent<ViewModelEvent<Constants.StorageEvents, Any>> = SingleLiveEvent()
    val objectAction: SingleLiveEvent<ViewModelEvent<Any, Any>> = SingleLiveEvent()

    fun makeViewEvent(state: Constants.ViewEvents, param: Any? = null) {
        viewEvent.postValue(ViewModelEvent(state, param))
    }

    fun makeStorageEvent(state: Constants.StorageEvents, param: Any? = null) {
        storageEvent.postValue(ViewModelEvent(state, param))
    }

    fun makeEvent(event: Any, param: Any? = null) {
        objectAction.postValue(ViewModelEvent(event, param))
    }

    fun makeEvent(callback: CallbackParam) {
        makeEvent(callback.event, callback.data)
    }

    fun getLastFolderPathOrDefault(forWrite: Boolean) = StorageInteractor.getLastFolderPathOrDefault(getContext(), forWrite)

    fun onPermissionChecked() {
        makeStorageEvent(Constants.StorageEvents.PermissionChecked)
    }
}