package com.gee12.mytetroid.ui.logs

import android.app.Application
import android.net.Uri
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.ui.base.BaseViewModel
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class LogsViewModel(
    app: Application,
    buildInfoProvider: BuildInfoProvider,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    settingsManager: CommonSettingsManager,
    appPathProvider: IAppPathProvider,
) : BaseViewModel(
    app = app,
    buildInfoProvider = buildInfoProvider,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,
    settingsManager = settingsManager,
    appPathProvider = appPathProvider,
), CoroutineScope {

    companion object {
        const val LINES_IN_RECYCLER_VIEW_ITEM = 10
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()


    fun loadLogs() {
        launchOnMain {
            if (CommonSettings.isWriteLogToFile(getContext())) {
                // читаем лог-файл
                log(R.string.log_open_log_file)
                sendEvent(LogsEvent.Loading.InProcess)

                if (logger.fullFileName == null) {
                    sendEvent(LogsEvent.Loading.Failed(getString(R.string.error_log_file_path_is_null)))
                    return@launchOnMain
                }

                try {
                    val data = withIo {
                        val fileUri = Uri.parse(logger.fullFileName)
                        FileUtils.readTextFile(fileUri, LINES_IN_RECYCLER_VIEW_ITEM)
                    }
                    sendEvent(LogsEvent.Loading.Success(data))
                } catch (ex: Exception) {
                    // ошибка чтения
                    val text = ex.localizedMessage.orEmpty()
                    logError(text, true)

                    sendEvent(LogsEvent.Loading.Failed(text))
                }
            } else {
                // выводим логи текущего сеанса запуска приложения
                sendEvent(LogsEvent.ShowBufferLogs)
            }
        }
    }

    fun getLogsBufferString() = logger.bufferString

}
