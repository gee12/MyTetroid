package com.gee12.mytetroid.viewmodels

import android.app.Activity
import android.app.Application
import android.util.Log
import com.gee12.mytetroid.PermissionInteractor
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.SingleLiveEvent
import com.gee12.mytetroid.helpers.IStorageProvider
import com.gee12.mytetroid.model.TetroidStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

open class BaseStorageViewModel(
    app: Application,
    /*logger: TetroidLogger?*/
) : BaseViewModel(
    app,
    /*,logger*/
), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    open var storage: TetroidStorage? = null
        protected set

    val storageEvent = SingleLiveEvent<ViewModelEvent<Constants.StorageEvents, Any>>()
    val objectAction = SingleLiveEvent<ViewModelEvent<Any, Any>>()

    val permissionInteractor = PermissionInteractor(this.logger)

    val storageProvider: IStorageProvider = object : IStorageProvider {
        override fun getStorageOrNull(): TetroidStorage? = storage
    }


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

    //region Other

    @JvmOverloads
    fun checkReadExtStoragePermission(
        activity: Activity,
        requestCode: Int = Constants.REQUEST_CODE_PERMISSION_READ_STORAGE,
        callback: (() -> Unit)? = null
    ): Boolean {
        if (permissionInteractor.checkReadExtStoragePermission(activity, requestCode)) {
            if (callback != null) callback.invoke()
            else onPermissionGranted(requestCode)
            return true
        }
        return false
    }

    @JvmOverloads
    fun checkWriteExtStoragePermission(
        activity: Activity,
        requestCode: Int = Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE,
        callback: (() -> Unit)? = null
    ): Boolean {
        if (permissionInteractor.checkWriteExtStoragePermission(activity, requestCode)) {
            if (callback != null) callback.invoke()
            else onPermissionGranted(requestCode)
            return true
        }
        return false
    }

    open fun onPermissionGranted(requestCode: Int) {
        postStorageEvent(Constants.StorageEvents.PermissionGranted, requestCode)
    }

    open fun onPermissionCanceled(requestCode: Int) {
        postStorageEvent(Constants.StorageEvents.PermissionCanceled, requestCode)
    }

    //endregion Other

}