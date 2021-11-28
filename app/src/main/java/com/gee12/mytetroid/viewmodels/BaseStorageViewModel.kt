package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.util.Log
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.SingleLiveEvent
import com.gee12.mytetroid.interactors.StorageInteractor
import com.gee12.mytetroid.logs.TetroidLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

open class BaseStorageViewModel(
    app: Application/*,
    logger: TetroidLogger?*/
) : BaseViewModel(
    app/*,
    logger*/
), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    val storageEvent = SingleLiveEvent<ViewModelEvent<Constants.StorageEvents, Any>>()
    val objectAction = SingleLiveEvent<ViewModelEvent<Any, Any>>()

    //region Storage event

    fun postStorageEvent(event: Constants.StorageEvents, param: Any? = null) {
        Log.i("MYTETROID", "postStorageEvent(): state=$event param=$param")
        storageEvent.postValue(ViewModelEvent(event, param))
    }

    fun setStorageEvent(event: Constants.StorageEvents, param: Any? = null) {
        Log.i("MYTETROID", "setStorageEvent(): state=$event param=$param")
        storageEvent.value = ViewModelEvent(event, param)
    }

    //endregion Storage event

    //region Event

    fun postEvent(event: Any, param: Any? = null) {
        Log.i("MYTETROID", "postEvent(): state=$event param=$param")
        objectAction.postValue(ViewModelEvent(event, param))
    }

    fun setEvent(event: Any, param: Any? = null) {
        Log.i("MYTETROID", "setEvent(): state=$event param=$param")
        objectAction.value = ViewModelEvent(event, param)
    }

    fun postEventFromCallbackParam(callback: EventCallbackParams) {
        when (callback.event) {
            is Constants.ViewEvents -> postViewEvent(callback.event, callback.data)
            is Constants.StorageEvents -> postStorageEvent(callback.event, callback.data)
            is Constants.MainEvents -> postEvent(callback.event, callback.data)
            is Constants.RecordEvents -> postEvent(callback.event, callback.data)
            else -> {}
        }
    }

    //endregion Event

    //region Other

    fun getLastFolderPathOrDefault(forWrite: Boolean) = StorageInteractor.getLastFolderPathOrDefault(getContext(), forWrite)

    fun onPermissionChecked() {
        postStorageEvent(Constants.StorageEvents.PermissionChecked)
    }

    //endregion Other

}