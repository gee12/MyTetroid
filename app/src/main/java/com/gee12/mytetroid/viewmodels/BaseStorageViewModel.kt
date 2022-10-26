package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.util.Log
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.SingleLiveEvent
import com.gee12.mytetroid.helpers.CommonSettingsProvider
import com.gee12.mytetroid.helpers.IFailureHandler
import com.gee12.mytetroid.helpers.INotificator
import com.gee12.mytetroid.helpers.IStorageProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

open class BaseStorageViewModel(
    app: Application,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    commonSettingsProvider: CommonSettingsProvider,
    protected val storageProvider: IStorageProvider,
) : BaseViewModel(
    app,
    logger,
    notificator,
    failureHandler,
    commonSettingsProvider,
), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    open val storage: TetroidStorage?
        get() = storageProvider.storage

    val storageEvent = SingleLiveEvent<ViewModelEvent<Constants.StorageEvents, Any>>()
    val objectAction = SingleLiveEvent<ViewModelEvent<Any, Any>>()


    fun getLastFolderPathOrDefault(forWrite: Boolean) = commonSettingsProvider.getLastFolderPathOrDefault(forWrite)

    fun getRootNode() = storageProvider.getRootNode()

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
            is MainViewModel.MainEvents -> postEvent(callback.event, callback.data)
            is RecordViewModel.RecordEvents -> postEvent(callback.event, callback.data)
            else -> {}
        }
    }

    //endregion Event

}

data class PermissionRequestParams(
    val permission: Constants.TetroidPermission,
    val requestCallback: () -> Unit
)