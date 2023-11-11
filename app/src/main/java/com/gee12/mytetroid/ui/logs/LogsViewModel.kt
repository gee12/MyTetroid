package com.gee12.mytetroid.ui.logs

import android.app.Application
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.createFileIsNotExist
import com.gee12.mytetroid.common.onFailure
import com.gee12.mytetroid.common.onSuccess
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.usecase.file.ReadTextBlocksFromFileUseCase
import com.gee12.mytetroid.domain.usecase.file.ReadTextBlocksFromStringUseCase
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
    private val readTextBlocksFromFileUseCase: ReadTextBlocksFromFileUseCase,
    private val readTextBlocksFromStringUseCase: ReadTextBlocksFromStringUseCase,
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
        if (CommonSettings.isWriteLogToFile(getContext())) {
            loadLogsFromFile()
        } else {
            loadLogsFromBuffer()
        }
    }

    /**
     * Читаем лог-файл, разбивая его на блоки.
     */
    private fun loadLogsFromFile() {
        launchOnMain {
            // читаем лог-файл
            log(R.string.log_open_log_file)
            sendEvent(LogsEvent.LoadFromFile.InProcess)

            val logFullFileName = logger.fullFileName
            if (logFullFileName == null) {
                sendEvent(LogsEvent.LoadFromFile.LogPathIsEmpty)
            } else {
                withIo {
                    logFullFileName.createFileIsNotExist()
                    readTextBlocksFromFileUseCase.run(
                        ReadTextBlocksFromFileUseCase.Params(
                            fullFileName = logFullFileName,
                            linesInBlock = LINES_IN_RECYCLER_VIEW_ITEM,
                        )
                    )
                }.onFailure {
                    logFailure(it, show = false)
                    sendEvent(LogsEvent.LoadFromFile.Failed(it, logFullFileName))
                }.onSuccess { data ->
                    sendEvent(LogsEvent.LoadFromFile.Success(data))
                }
            }
        }
    }

    /**
     * Выводим логи текущего сеанса запуска приложения, разбивая поток на блоки.
     */
    fun loadLogsFromBuffer() {
        launchOnMain {
            sendEvent(LogsEvent.LoadFromBuffer.InProcess)

            withIo {
                readTextBlocksFromStringUseCase.run(
                    ReadTextBlocksFromStringUseCase.Params(
                        data = logger.bufferString,
                        linesInBlock = LINES_IN_RECYCLER_VIEW_ITEM,
                    )
                )
            }.onFailure {
                logFailure(it, show = false)
                sendEvent(LogsEvent.LoadFromBuffer.Failed(it))
            }.onSuccess { data ->
                sendEvent(LogsEvent.LoadFromBuffer.Success(data))
            }
        }
    }

}
