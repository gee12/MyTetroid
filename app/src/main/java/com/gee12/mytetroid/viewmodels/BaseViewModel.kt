package com.gee12.mytetroid.viewmodels

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.gee12.mytetroid.App
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.SingleLiveEvent
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.logs.*
import com.gee12.mytetroid.model.TetroidObject
import com.gee12.mytetroid.common.utils.StringUtils
import com.gee12.mytetroid.interactors.PermissionInteractor
import com.gee12.mytetroid.interactors.PermissionRequestData
import com.gee12.mytetroid.repo.CommonSettingsRepo
import java.util.*

open class BaseViewModel(
    application: Application,
    /*logger: TetroidLogger?*/
) : AndroidViewModel(application) {

    val commonSettingsRepo = CommonSettingsRepo(getContext())

    val viewEvent = SingleLiveEvent<ViewModelEvent<Constants.ViewEvents, Any>>()
    var isBusy = false

    var logger: TetroidLogger = BaseLogger().apply {
        init(CommonSettings.getLogPath(getContext()), CommonSettings.isWriteLogToFile(getContext()))
    }

    val permissionInteractor = PermissionInteractor(this.logger)


    // Общий внутренний логгер: записывает логи в буфер и в файл
    // При первом запуске, когда окружение приложения (App.current) еще не инициализировано,
    //  общий логгер для приложения инициализируется из только что созданного logger данного ViewModel.
    //  И далее в приложении уже используется только он.
    protected val innerSharedLogger: FileTetroidLogger
        get() = App.current?.logger ?: logger

    val messageObservable = SingleLiveEvent<Message>()


    fun getLastFolderPathOrDefault(forWrite: Boolean) = commonSettingsRepo.getLastFolderPathOrDefault(forWrite)

    //region View event

    fun postViewEvent(event: Constants.ViewEvents, param: Any? = null) {
        Log.i("MYTETROID", "postViewEvent(): state=$event param=$param")
        viewEvent.postValue(ViewModelEvent(event, param))
    }

    fun setViewEvent(event: Constants.ViewEvents, param: Any? = null) {
        Log.i("MYTETROID", "setViewEvent(): state=$event param=$param")
        viewEvent.value = ViewModelEvent(event, param)
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
        postViewEvent(Constants.ViewEvents.PermissionGranted, requestCode)
    }

    open fun onPermissionCanceled(requestCode: Int) {
        postViewEvent(Constants.ViewEvents.PermissionCanceled, requestCode)
    }

    open fun showManualPermissionRequest(request: PermissionRequestParams) {
        postViewEvent(Constants.ViewEvents.ShowPermissionRequest, request)
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
        messageObservable.postValue(Message(message, type))
    }

    fun showSnackMoreInLogs() {
        postViewEvent(Constants.ViewEvents.ShowMoreInLogs)
    }

    //endregion Message

    //region Context

    fun getString(resId: Int) = getApplication<Application>().resources.getString(resId)

    fun getString(resId: Int, vararg params: Any) = getApplication<Application>().resources.getString(resId, *params)

    fun getStringArray(resId: Int) = getApplication<Application>().resources.getStringArray(resId)

    fun getContext(): Context = getApplication()

    //endregion Context


    inner class BaseLogger : TetroidLogger() {

        override fun log(s: String, type: LogType, show: Boolean) {
            innerSharedLogger.log(s, type)
            if (show) {
                showMessage(s, type)
            }
        }

        override fun log(resId: Int, type: LogType, show: Boolean) {
            log(getString(resId), type, show)
        }

        override fun showMessage(s: String, type: LogType) = this@BaseViewModel.showMessage(s, type)

        override fun showSnackMoreInLogs() = this@BaseViewModel.showSnackMoreInLogs()

        override fun getString(resId: Int) = this@BaseViewModel.getString(resId)

        override fun getStringArray(resId: Int) = this@BaseViewModel.getStringArray(resId)
    }

}

data class ViewModelException<E>(val event: E, val throwable: Throwable)

data class ViewModelEvent<S, D>(var state: S, var data: D?) {
    constructor(state: S): this(state, null)
}