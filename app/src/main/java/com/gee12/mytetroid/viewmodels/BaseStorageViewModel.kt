package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.util.Log
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.viewmodels.StorageViewModel.StorageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.coroutines.CoroutineContext

open class BaseStorageViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    commonSettingsProvider: CommonSettingsProvider,
    protected val storageProvider: IStorageProvider,
) : BaseViewModel(
    app,
    resourcesProvider,
    logger,
    notificator,
    failureHandler,
    commonSettingsProvider,
), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    open val storage: TetroidStorage?
        get() = storageProvider.storage

    private val _storageEventFlow = MutableSharedFlow<StorageEvent>(extraBufferCapacity = 0)
    val storageEventFlow = _storageEventFlow.asSharedFlow()

    private val _objectEventFlow = MutableSharedFlow<VMEvent>(extraBufferCapacity = 0)
    val objectEventFlow = _objectEventFlow.asSharedFlow()


    fun getLastFolderPathOrDefault(forWrite: Boolean) = commonSettingsProvider.getLastFolderPathOrDefault(forWrite)

    fun getRootNode() = storageProvider.getRootNode()

    //region Storage event

    suspend fun sendStorageEvent(event: StorageEvent) {
        Log.i("MYTETROID", "postStorageEvent(): state=$event")
        _storageEventFlow.emit(event)
    }

    //endregion Storage event

    //region Event

    suspend fun sendEvent(event: VMEvent) {
        Log.i("MYTETROID", "sendEvent(): state=$event")
        _objectEventFlow.emit(event)
    }

    suspend fun sendEventFromCallbackParam(callbackEvent: VMEvent) {
        when (callbackEvent) {
            is ViewEvent -> this.sendViewEvent(callbackEvent)
            is StorageEvent -> this.sendStorageEvent(callbackEvent)
            is MainViewModel.MainEvent -> this.sendEvent(callbackEvent)
            is RecordViewModel.RecordEvent -> this.sendEvent(callbackEvent)
            else -> {}
        }
    }

    //endregion Event

}

data class PermissionRequestParams(
    val permission: Constants.TetroidPermission,
    val requestCallback: () -> Unit
)