package com.gee12.mytetroid.ui.base

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.isWritable
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.extensions.getIdNameString
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.manager.PermissionManager
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.model.permission.PermissionRequestData
import com.gee12.mytetroid.logs.*
import com.gee12.mytetroid.model.TetroidObject
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.model.permission.TetroidPermission
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

open class BaseViewModel(
    application: Application,
    val resourcesProvider: IResourcesProvider,
    val logger: ITetroidLogger,
    val notificator: INotificator,
    val failureHandler: IFailureHandler,
    val settingsManager: CommonSettingsManager,
    val appPathProvider: IAppPathProvider,
) : AndroidViewModel(application) {

    private val internalCoroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
            logError(throwable)
        }

    private val _eventFlow = MutableSharedFlow<BaseEvent>(extraBufferCapacity = 0)
    val eventFlow = _eventFlow.asSharedFlow()

    var isBusy = false

    // TODO: inject
    val permissionManager = PermissionManager(resourcesProvider, this.logger)

    private val _messageEventFlow = MutableSharedFlow<Message>(extraBufferCapacity = 0)
    val messageEventFlow = _messageEventFlow.asSharedFlow()


    open fun initialize() {
        // FIXME: перенести в InitAppUseCase ?
        logger.init(
            path = appPathProvider.getPathToLogsFolder(),
            isWriteToFile = settingsManager.isWriteLogToFile()
        )
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
        Log.i("MYTETROID", "sendEvent($event)")
        _eventFlow.emit(event)
    }

    //region Get

    fun getLastFolderPathOrDefault(forWrite: Boolean) = settingsManager.getLastSelectedFolderPathOrDefault(forWrite)

    //endregion Get

    //region Permission

    fun checkReadFileStoragePermission(root: DocumentFile): Boolean {
        return root.canRead() //TODO || Build.VERSION.SDK_INT < 21 ?
    }

    fun checkWriteFileStoragePermission(root: DocumentFile): Boolean {
        return root.isWritable(getContext()) //TODO || Build.VERSION.SDK_INT < 21 ?
    }

    fun requestFileStorageAccess(
        permission: TetroidPermission,
        requestCode: PermissionRequestCode
    ) {
        launchOnMain {
            sendEvent(BaseEvent.Permission.Check(permission, requestCode))
        }
    }

    fun checkAndRequestReadFileStoragePermission(
        file: DocumentFile,
        requestCode: PermissionRequestCode,
    ) {
        val permission = TetroidPermission.FileStorage.Read(file)

        if (checkReadFileStoragePermission(file)) {
            onPermissionGranted(permission, requestCode)
        } else {
            requestFileStorageAccess(permission, requestCode)
        }
    }

    fun checkAndRequestReadFileStoragePermission(
        uri: Uri,
        requestCode: PermissionRequestCode,
    ) {
        DocumentFileCompat.fromUri(getContext(), uri)?.let { root ->
            checkAndRequestReadFileStoragePermission(root, requestCode)
        }
    }

    fun checkAndRequestWriteFileStoragePermission(
        file: DocumentFile,
        requestCode: PermissionRequestCode,
    ) {
        val permission = TetroidPermission.FileStorage.Write(file)

        if (checkWriteFileStoragePermission(file)) {
            onPermissionGranted(permission, requestCode)
        } else {
            requestFileStorageAccess(permission, requestCode)
        }
    }

    fun checkAndRequestWriteFileStoragePermission(
        uri: Uri,
        requestCode: PermissionRequestCode,
    ) {
        DocumentFileCompat.fromUri(getContext(), uri)?.let { root ->
            checkAndRequestWriteFileStoragePermission(root, requestCode)
        }
    }

    // TODO: убрать boolean result
    fun checkCameraPermission(activity: Activity) {
        val permission = TetroidPermission.Camera

        permissionManager.checkPermission(
            PermissionRequestData(
                permission = permission,
                activity = activity,
                requestCode = PermissionRequestCode.OPEN_CAMERA,
                onManualPermissionRequest = { requestCallback ->
                    showManualPermissionRequest(
                        permission = permission,
                        requestCallback = requestCallback
                    )
                }
            )
        )
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
        requestCallback: () -> Unit
    ) {
        launchOnMain {
            sendEvent(BaseEvent.Permission.ShowRequest(permission, requestCallback))
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

        logger.logError(message.title, show)
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
        val message = failureHandler.getFailureMessage(failure)
        showMessage(message.title, LogType.ERROR)
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
