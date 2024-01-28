package com.gee12.mytetroid.ui.base

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anggrayudi.storage.file.DocumentFileCompat
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.extensions.getIdNameString
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.manager.FileStorageManager
import com.gee12.mytetroid.domain.manager.PermissionManager
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.*
import com.gee12.mytetroid.model.TetroidObject
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.model.permission.PermissionRequestData
import com.gee12.mytetroid.model.permission.TetroidPermission
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

abstract class BaseViewModel(
    app: Application,
    val buildInfoProvider: BuildInfoProvider,
    val resourcesProvider: IResourcesProvider,
    val logger: ITetroidLogger,
    val notificator: INotificator,
    val failureHandler: IFailureHandler,
    val settingsManager: CommonSettingsManager,
    val appPathProvider: IAppPathProvider,
) : AndroidViewModel(app) {

    private val internalCoroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
            logError(throwable)
            if (isBusy) {
                launchOnMain {
                    hideProgress()
                }
            }
        }

    private val _eventFlow = MutableSharedFlow<BaseEvent>(extraBufferCapacity = 0)
    val eventFlow = _eventFlow.asSharedFlow()

    var isBusy = false

    val permissionManager = PermissionManager(buildInfoProvider, resourcesProvider, this.logger)
    val fileStorageManager = FileStorageManager(context = app, buildInfoProvider)

    private val _messageEventFlow = MutableSharedFlow<Message>(extraBufferCapacity = 0)
    val messageEventFlow = _messageEventFlow.asSharedFlow()


    open fun initialize() {

    }

    fun setNotificatorCallbacks() {
        with(notificator) {
            showMessageCallback = { message, type ->
                this@BaseViewModel.showMessage(message, type)
            }
            showSnackMoreInLogsCallback = {
                this@BaseViewModel.showSnackMoreInLogs()
            }
        }
    }

    suspend fun sendEvent(event: BaseEvent) {
        if (buildInfoProvider.isDebug) {
            Log.d("MYTETROID", "sendEvent($event)")
        }
        _eventFlow.emit(event)
    }

    //region Get

    fun getLastFolderPathOrDefault(forWrite: Boolean) = settingsManager.getLastSelectedFolderPathOrDefault(forWrite)

    //endregion Get

    //region Permission

    fun checkAndRequestReadFileStoragePermission(
        uri: Uri,
        requestCode: PermissionRequestCode,
    ) {
        val permission = TetroidPermission.FileStorage.Read(uri)

        DocumentFileCompat.fromUri(getContext(), uri)?.let { file ->
            if (fileStorageManager.checkReadFileStoragePermission(file)) {
                onPermissionGranted(permission, requestCode)
            } else {
                showManualPermissionRequest(permission, requestCode)
            }
        } ?: showManualPermissionRequest(permission, requestCode)
    }

    fun checkAndRequestWriteFileStoragePermission(
        uri: Uri,
        requestCode: PermissionRequestCode,
    ) {
        val permission = TetroidPermission.FileStorage.Write(uri)

        DocumentFileCompat.fromUri(getContext(), uri)?.let { file ->
            if (fileStorageManager.checkWriteFileStoragePermission(file)) {
                onPermissionGranted(permission, requestCode)
            } else {
                showManualPermissionRequest(permission, requestCode)
            }
        } ?: showManualPermissionRequest(permission, requestCode)
    }

    fun checkCameraPermission(activity: Activity) {
        val permission = TetroidPermission.Camera

        if (permissionManager.checkPermission(
                PermissionRequestData(
                    permission = permission,
                    activity = activity,
                    requestCode = PermissionRequestCode.OPEN_CAMERA,
                    onManualPermissionRequest = { requestCallback ->
                        showManualPermissionRequest(
                            permission = permission,
                            requestCode = PermissionRequestCode.OPEN_CAMERA,
                            requestCallback = requestCallback
                        )
                    }
                )
            )
        ) {
            onPermissionGranted(permission, PermissionRequestCode.OPEN_CAMERA)
        }
    }

    fun checkAndRequestRecordAudioPermission(activity: Activity) {
        val permission = TetroidPermission.RecordAudio
        val requestCode = PermissionRequestCode.RECORD_AUDIO

        if (permissionManager.checkPermission(
                PermissionRequestData(
                    permission = permission,
                    activity = activity,
                    requestCode = requestCode,
                    onManualPermissionRequest = { requestCallback ->
                        showManualPermissionRequest(
                            permission = permission,
                            requestCode = requestCode,
                            requestCallback = requestCallback
                        )
                    }
                )
            )
        ) {
            launchOnMain {
                sendEvent(BaseEvent.Permission.Granted(permission, requestCode))
            }
        }
    }

    // TODO: убрать boolean result
    fun checkAndRequestTermuxPermission(activity: Activity): Boolean {
        val permission = TetroidPermission.Termux

        return permissionManager.checkPermission(
            PermissionRequestData(
                permission = permission,
                activity = activity,
                requestCode = PermissionRequestCode.TERMUX,
                onManualPermissionRequest = { requestCallback ->
                    showManualPermissionRequest(
                        permission = permission,
                        requestCode = PermissionRequestCode.TERMUX,
                        requestCallback = requestCallback
                    )
                }
            )
        )
    }

    open fun onPermissionGranted(permission: TetroidPermission, requestCode: PermissionRequestCode) {
        launchOnMain {
            sendEvent(BaseEvent.Permission.Granted(permission, requestCode))
        }
    }

    open fun onPermissionGranted(requestCode: PermissionRequestCode) {
        requestCode.toPermission()?.let { permission ->
            launchOnMain {
                sendEvent(BaseEvent.Permission.Granted(permission, requestCode))
            }
        }
    }

    open fun onPermissionCanceled(permission: TetroidPermission, requestCode: PermissionRequestCode) {
        launchOnMain {
            sendEvent(BaseEvent.Permission.Canceled(permission, requestCode))
        }
    }

    open fun onPermissionCanceled(requestCode: PermissionRequestCode) {
        requestCode.toPermission()?.let { permission ->
            launchOnMain {
                sendEvent(BaseEvent.Permission.Canceled(permission, requestCode))
            }
        }
    }

    open fun showManualPermissionRequest(
        permission: TetroidPermission,
        requestCode: PermissionRequestCode,
        requestCallback: (() -> Unit)? = null,
    ) {
        launchOnMain {
            sendEvent(BaseEvent.Permission.ShowRequest(permission, requestCode, requestCallback))
        }
    }

    //endregion Permission

    //region Log

    // info
    fun log(mes: String, show: Boolean = false) {
        logger.log(mes, show)
    }

    fun log(@StringRes resId: Int, show: Boolean = false) {
        logger.log(resId, show)
    }

    // debug
    fun logDebug(mes: String, show: Boolean = false) {
        logger.logDebug(mes, show)
    }

    fun logDebug(@StringRes resId: Int, show: Boolean = false) {
        logger.logDebug(resId, show)
    }

    // warning
    fun logWarning(mes: String, show: Boolean = true) {
        logger.logWarning(mes, show)
    }

    fun logWarning(@StringRes resId: Int, show: Boolean = true) {
        logger.logWarning(resId, show)
    }

    // error
    fun logError(mes: String, show: Boolean = true) {
        logger.logError(mes, show)
    }

    fun logError(@StringRes resId: Int, show: Boolean = true) {
        logger.logError(resId, show)
    }

    fun logError(ex: Throwable, show: Boolean = true) {
        logger.logError(ex, show)
    }

    fun logError(s: String, ex: Exception, show: Boolean = true) {
        logger.logError(s, ex, show)
    }

    fun logFailure(failure: Failure, show: Boolean = true) {
        val message = failureHandler.getFailureMessage(failure)

        // TODO: сделать многострочные уведомления
        if (show) {
            showError(message.title)
        }
        logger.logError(message.getFullMessage(), show = false)
    }

    // common
    fun log(mes: String, type: LogType, show: Boolean = false) {
        logger.log(mes, type, show)
    }

    fun log(@StringRes resId: Int, type: LogType, show: Boolean = false) {
        logger.log(resId, type, show)
    }

    private fun addIdName(obj: TetroidObject?): String {
        return logger.addIdName(obj)
    }

    // operation result
    open fun logOperRes(obj: LogObj, oper: LogOper): String {
        return logger.logOperRes(obj, oper, "", true)
    }

    open fun logOperRes(obj: LogObj, oper: LogOper, tetroidObj: TetroidObject, show: Boolean): String {
        return logger.logOperRes(obj, oper, tetroidObj.getIdNameString(resourcesProvider), show)
    }

    open fun logOperRes(obj: LogObj, oper: LogOper, add: String, show: Boolean): String {
        return logger.logOperRes(obj, oper, add, show)
    }

    // operation start
    open fun logOperStart(obj: LogObj, oper: LogOper): String {
        return logger.logOperStart(obj, oper, "")
    }

    open fun logOperStart(obj: LogObj, oper: LogOper, tetroidObj: TetroidObject?): String {
        return logger.logOperStart(obj, oper, addIdName(tetroidObj))
    }

    open fun logOperStart(obj: LogObj, oper: LogOper, add: String): String {
        return logger.logOperStart(obj, oper, add)
    }

    open fun logOperError(obj: LogObj, oper: LogOper, add: String?, more: Boolean, show: Boolean): String? {
        return logger.logOperError(obj, oper, add, more, show)
    }

    open fun logOperError(obj: LogObj, oper: LogOper, show: Boolean = false): String? {
        return logger.logOperError(obj, oper, null, false, show)
    }

    open fun logOperErrorMore(obj: LogObj, oper: LogOper, show: Boolean = false): String? {
        return logger.logOperError(obj, oper, null, true, show)
    }

    open fun logDuringOperErrors(obj: LogObj, oper: LogOper, show: Boolean): String? {
        return logger.logDuringOperErrors(obj, oper, show)
    }

    //endregion Log

    //region Message

    fun showMessage(message: String) {
        showMessage(message, LogType.INFO)
    }

    fun showMessage(@StringRes resId: Int) {
        showMessage(getString(resId), LogType.INFO)
    }

    fun showWarning(message: String) {
        showMessage(message, LogType.WARNING)
    }

    fun showError(message: String) {
        showMessage(message, LogType.ERROR)
    }

    fun showFailure(failure: Failure) {
        val message = failureHandler.getFailureMessage(failure).getFullMessage()
        showMessage(message, LogType.ERROR)
    }

    fun showMessage(message: String, type: LogType) {
        launchOnMain {
            _messageEventFlow.emit(Message(message, type))
        }
    }

    fun showSnackMoreInLogs() {
        launchOnMain {
            sendEvent(BaseEvent.ShowMoreInLogs)
        }
    }

    //endregion Message

    //region Progress

    protected suspend fun showProgressWithText(resId: Int) {
        showProgressWithText(message = getString(resId))
    }

    protected suspend fun showProgressWithText(message: String) {
        sendEvent(BaseEvent.ShowProgressWithText(message))
    }

    protected suspend fun hideProgress() {
        sendEvent(BaseEvent.HideProgress)
    }

    //endregion Progress

    //region Context

    fun getString(resId: Int) = resourcesProvider.getString(resId)

    fun getString(resId: Int, vararg params: Any) = resourcesProvider.getString(resId, *params)

    fun getStringArray(resId: Int) = resourcesProvider.getStringArray(resId)

    fun getContext(): Context = getApplication()

    //endregion Context

    suspend fun <T> withMain(block: suspend () -> T) : T{
        return withContext(Dispatchers.Main) {
            block()
        }
    }

    suspend fun <T> withIo(block: suspend CoroutineScope.() -> T): T {
        return withContext(Dispatchers.IO) {
            block()
        }
    }

    suspend fun <T> withComputation(block: suspend CoroutineScope.() -> T): T {
        return withContext(Dispatchers.Default) {
            block()
        }
    }

    fun launchOnMain(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(internalCoroutineExceptionHandler + Dispatchers.Main) {
            block()
        }
    }

    fun launchOnIo(
        coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
        exceptionHandler: CoroutineExceptionHandler? = null,
        block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch((exceptionHandler ?: internalCoroutineExceptionHandler) + coroutineDispatcher) {
            block()
        }
    }

}
