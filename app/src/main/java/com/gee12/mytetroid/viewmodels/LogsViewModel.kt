package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.net.Uri
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.helpers.CommonSettingsProvider
import com.gee12.mytetroid.helpers.IFailureHandler
import com.gee12.mytetroid.helpers.INotificator
import com.gee12.mytetroid.helpers.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.coroutines.CoroutineContext

class LogsViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    commonSettingsProvider: CommonSettingsProvider,
) : BaseViewModel(
    app,
    resourcesProvider,
    logger,
    notificator,
    failureHandler,
    commonSettingsProvider,
), CoroutineScope {

    sealed class LogsEvent : VMEvent() {
        object ShowBufferLogs : LogsEvent()
        sealed class Loading : LogsEvent() {
            object InProcess : Loading()
            data class Success(var data: List<String>) : Loading()
            data class Failed(var text: String) : Loading()
        }
    }

    companion object {
        const val LINES_IN_RECYCLER_VIEW_ITEM = 10
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    private val _logsEventFlow = MutableSharedFlow<LogsEvent>(extraBufferCapacity = 0)
    val logsEventFlow = _logsEventFlow.asSharedFlow()


    fun load() {
        launchOnMain {
            if (CommonSettings.isWriteLogToFile(getContext())) {
                // читаем лог-файл
                log(R.string.log_open_log_file)
                postEvent(LogsEvent.Loading.InProcess)

                if (logger.fullFileName == null) {
                    postEvent(LogsEvent.Loading.Failed(getString(R.string.error_log_file_path_is_null)))
                    return@launchOnMain
                }

                try {
                    val data = withContext(Dispatchers.IO) {
                        val fileUri = Uri.parse(logger.fullFileName)
                        FileUtils.readTextFile(fileUri, LINES_IN_RECYCLER_VIEW_ITEM)
                    }
                    postEvent(LogsEvent.Loading.Success(data))
                } catch (ex: Exception) {
                    // ошибка чтения
                    val text = ex.localizedMessage ?: ""
                    logError(text, true)

                    postEvent(LogsEvent.Loading.Failed(text))
                }
            } else {
                // выводим логи текущего сеанса запуска приложения
                postEvent(LogsEvent.ShowBufferLogs)
            }
        }
    }

    fun getLogsBufferString() = logger.bufferString

    private suspend fun postEvent(event: LogsEvent) {
        _logsEventFlow.emit(event)
    }

}
