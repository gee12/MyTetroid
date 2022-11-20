package com.gee12.mytetroid.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.utils.StringUtils
import com.gee12.mytetroid.helpers.CommonSettingsProvider
import com.gee12.mytetroid.helpers.IFailureHandler
import com.gee12.mytetroid.helpers.INotificator
import com.gee12.mytetroid.interactors.PermissionInteractor
import com.gee12.mytetroid.interactors.PermissionRequestData
import com.gee12.mytetroid.logs.*
import com.gee12.mytetroid.model.TetroidObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

open class BaseViewModel(
    application: Application,
    val logger: ITetroidLogger,
    val notificator: INotificator,
    val failureHandler: IFailureHandler,
    val commonSettingsProvider: CommonSettingsProvider,
) : AndroidViewModel(application) {

    private val internalCoroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
        }

    private val _viewEventFlow = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 0)
    val viewEventFlow = _viewEventFlow.asSharedFlow()

    var isBusy = false

    // TODO: inject
    val permissionInteractor = PermissionInteractor(this.logger)

    private val _messageEventFlow = MutableSharedFlow<Message>(extraBufferCapacity = 0)
    val messageEventFlow = _messageEventFlow.asSharedFlow()


    open fun initialize() {
        logger.init(
            path = commonSettingsProvider.getLogPath(),
            isWriteToFile = commonSettingsProvider.isWriteLogToFile()
        )
        with(notificator) {
            showMessageCallback = { message, type ->
                this@BaseViewModel.showMessage(message, type)
            }
            showSnackMoreInLogsCallback = {
                this@BaseViewModel.showSnackMoreInLogs()
            }
        }
    }

    //region View event

    suspend fun sendViewEvent(event: ViewEvent) {
        Log.i("MYTETROID", "postViewEvent(): state=$event")
        _viewEventFlow.emit(event)
    }

    //endregion View event

    //region Permission

    @JvmOverloads
    fun checkReadExtStoragePermission(
        activity: Activity,
        requestCode: Int = Constants.REQUEST_CODE_PERMISSION_READ_STORAGE,
        callback: (() -> Unit)? = null
    ): Boolean {
        if (permissionInteractor.checkPermission(
                PermissionRequestData(
                    permission = Constants.TetroidPermission.ReadStorage,
                    activity = activity,
                    requestCode = requestCode,
                    onManualPermissionRequest = { requestCallback ->
                        showManualPermissionRequest(
                            PermissionRequestParams(
                                permission = Constants.TetroidPermission.ReadStorage,
                                requestCallback = requestCallback
                            )
                        )
                    }
                )
            )
        ) {
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
        if (permissionInteractor.checkPermission(
                PermissionRequestData(
                    permission = Constants.TetroidPermission.WriteStorage,
                    activity = activity,
                    requestCode = requestCode,
                    onManualPermissionRequest = { requestCallback ->
                        showManualPermissionRequest(
                            PermissionRequestParams(
                                permission = Constants.TetroidPermission.WriteStorage,
                                requestCallback = requestCallback
                            )
                        )
                    }
                )
            )
        ) {
            if (callback != null) callback.invoke()
            else onPermissionGranted(requestCode)
            return true
        }
        return false
    }

    fun checkCameraPermission(activity: Activity): Boolean {
        return permissionInteractor.checkPermission(
            PermissionRequestData(
                permission = Constants.TetroidPermission.Camera,
                activity = activity,
                requestCode = Constants.REQUEST_CODE_PERMISSION_CAMERA,
                onManualPermissionRequest = { requestCallback ->
                    showManualPermissionRequest(
                        PermissionRequestParams(
                            permission = Constants.TetroidPermission.Camera,
                            requestCallback = requestCallback
                        )
                    )
                }
            )
        )
    }

    fun checkTermuxPermission(activity: Activity): Boolean {
        return permissionInteractor.checkPermission(
            PermissionRequestData(
                permission = Constants.TetroidPermission.Termux,
                activity = activity,
                requestCode = Constants.REQUEST_CODE_PERMISSION_TERMUX,
                onManualPermissionRequest = { requestCallback ->
                    showManualPermissionRequest(
                        PermissionRequestParams(
                            permission = Constants.TetroidPermission.Termux,
                            requestCallback = requestCallback
                        )
                    )
                }
            )
        )
    }

    open fun onPermissionGranted(requestCode: Int) {
        launchOnMain {
            this@BaseViewModel.sendViewEvent(ViewEvent.PermissionGranted(requestCode))
        }
    }

    open fun onPermissionCanceled(requestCode: Int) {
        launchOnMain {
            this@BaseViewModel.sendViewEvent(ViewEvent.PermissionCanceled(requestCode))
        }
    }

    open fun showManualPermissionRequest(request: PermissionRequestParams) {
        launchOnMain {
            this@BaseViewModel.sendViewEvent(ViewEvent.ShowPermissionRequest(request))
        }
    }

    //endregion Permission

    //region Log

    // info
    @JvmOverloads
    fun log(mes: String, show: Boolean = false) {
        logger.log(mes, show)
    }

    @JvmOverloads
    fun log(@StringRes resId: Int, show: Boolean = false) {
        logger.log(resId, show)
    }

    // debug
    @JvmOverloads
    fun logDebug(mes: String, show: Boolean = false) {
        logger.logDebug(mes, show)
    }

    @JvmOverloads
    fun logDebug(@StringRes resId: Int, show: Boolean = false) {
        logger.logDebug(resId, show)
    }

    // warning
    @JvmOverloads
    fun logWarning(mes: String, show: Boolean = true) {
        logger.logWarning(mes, show)
    }

    @JvmOverloads
    fun logWarning(@StringRes resId: Int, show: Boolean = true) {
        logger.logWarning(resId, show)
    }

    // error
    @JvmOverloads
    fun logError(mes: String, show: Boolean = true) {
        logger.logError(mes, show)
    }

    @JvmOverloads
    fun logError(@StringRes resId: Int, show: Boolean = true) {
        logger.logError(resId, show)
    }

    @JvmOverloads
    fun logError(ex: Exception, show: Boolean = true) {
        logger.logError(ex, show)
    }

    @JvmOverloads
    fun logError(s: String, ex: Exception, show: Boolean = true) {
        logger.logError(s, ex, show)
    }

    @JvmOverloads
    fun logFailure(failure: Failure, show: Boolean = true) {
        val message = failureHandler.getFailureMessage(failure)
        // TODO: сделать многострочные уведомления

        logger.logError(message.title, show)
    }

    // common
    @JvmOverloads
    fun log(mes: String, type: LogType, show: Boolean = false) {
        logger.log(mes, type, show)
    }

    @JvmOverloads
    fun log(@StringRes resId: Int, type: LogType, show: Boolean = false) {
        logger.log(resId, type, show)
    }

    private fun addIdName(obj: TetroidObject?): String {
        return logger.addIdName(obj) ?: ""
    }

    // operation result
    open fun logOperRes(obj: LogObj, oper: LogOper): String {
        return logger.logOperRes(obj, oper, "", true) ?: ""
    }

    open fun logOperRes(obj: LogObj, oper: LogOper, tetroidObj: TetroidObject?, show: Boolean): String {
        return logger.logOperRes(obj, oper, StringUtils.getIdNameString(getContext(), tetroidObj), show) ?: ""
    }

    open fun logOperRes(obj: LogObj, oper: LogOper, add: String, show: Boolean): String {
        return logger.logOperRes(obj, oper, add, show) ?: ""
    }

    // operation start
    open fun logOperStart(obj: LogObj, oper: LogOper): String {
        return logger.logOperStart(obj, oper, "") ?: ""
    }

    open fun logOperStart(obj: LogObj, oper: LogOper, tetroidObj: TetroidObject?): String {
        return logger.logOperStart(obj, oper, addIdName(tetroidObj)) ?: ""
    }

    open fun logOperStart(obj: LogObj, oper: LogOper, add: String): String {
        return logger.logOperStart(obj, oper, add) ?: ""
    }

    open fun logOperError(obj: LogObj, oper: LogOper, add: String?, more: Boolean, show: Boolean): String? {
        return logger.logOperError(obj, oper, add, more, show)
    }

    open fun logOperError(obj: LogObj, oper: LogOper, show: Boolean = false): String? {
        return logger.logOperError(obj, oper, null, false, show)
    }

    @JvmOverloads
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

    fun showMessage(message: String, type: LogType) {
        launchOnMain {
            _messageEventFlow.emit(Message(message, type))
        }
    }

    fun showSnackMoreInLogs() {
        launchOnMain {
            this@BaseViewModel.sendViewEvent(ViewEvent.ShowMoreInLogs)
        }
    }

    //endregion Message

    //region Context

    fun getString(resId: Int) = getApplication<Application>().resources.getString(resId)

    fun getString(resId: Int, vararg params: Any) = getApplication<Application>().resources.getString(resId, *params)

    fun getStringArray(resId: Int) = getApplication<Application>().resources.getStringArray(resId)

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
