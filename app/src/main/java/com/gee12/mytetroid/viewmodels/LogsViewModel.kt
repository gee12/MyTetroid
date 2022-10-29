package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.net.Uri
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.SingleLiveEvent
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.helpers.CommonSettingsProvider
import com.gee12.mytetroid.helpers.IFailureHandler
import com.gee12.mytetroid.helpers.INotificator
import com.gee12.mytetroid.logs.ITetroidLogger
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class LogsViewModel(
    app: Application,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    commonSettingsProvider: CommonSettingsProvider,
) : BaseViewModel(
    app,
    logger,
    notificator,
    failureHandler,
    commonSettingsProvider,
), CoroutineScope {

    companion object {
        const val LINES_IN_RECYCLER_VIEW_ITEM = 10
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    val event = SingleLiveEvent<ViewModelEvent<Event, Any>>()

    fun load() {
        launchOnMain {
            if (CommonSettings.isWriteLogToFile(getContext())) {
                // читаем лог-файл
                log(R.string.log_open_log_file)
                postEvent(Event.PreLoading)

                if (logger.fullFileName == null) {
                    postEvent(Event.PostLoading, FileReadResult.Failure(getString(R.string.error_log_file_path_is_null)))
                    return@launchOnMain
                }

                try {
                    val data = withContext(Dispatchers.IO) {
                        val fileUri = Uri.parse(logger.fullFileName)
                        FileUtils.readTextFile(fileUri, LINES_IN_RECYCLER_VIEW_ITEM)
                    }
                    postEvent(Event.PostLoading, FileReadResult.Success(data))
                } catch (ex: Exception) {
                    // ошибка чтения
                    val text = ex.localizedMessage ?: ""
                    logError(text, true)

                    postEvent(Event.PostLoading, FileReadResult.Failure(text))
                }
            } else {
                // выводим логи текущего сеанса запуска приложения
                postEvent(Event.ShowBufferLogs)
            }
        }
    }

    fun getLogsBufferString() = logger.bufferString

    fun postEvent(e: Event, param: Any? = null) {
        event.postValue(ViewModelEvent(e, param))
    }

    enum class Event {
        ShowBufferLogs,
        PreLoading,
        PostLoading
    }

    sealed class FileReadResult {
        class Success(var data: List<String>)
        class Failure(var text: String)
    }

}
