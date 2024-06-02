package com.gee12.mytetroid.ui.history

import android.app.Application
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.onComplete
import com.gee12.mytetroid.common.onFailure
import com.gee12.mytetroid.common.onSuccess
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.manager.HistoryManager
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.HistoryEntity
import com.gee12.mytetroid.model.enums.HistorySortMode
import com.gee12.mytetroid.ui.base.BaseStorageViewModel

class HistoryViewModel(
    app: Application,
    buildInfoProvider: BuildInfoProvider,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    settingsManager: CommonSettingsManager,
    appPathProvider: IAppPathProvider,
    storageProvider: IStorageProvider,
    storagePathProvider: IStoragePathProvider,
    private val historyManager: HistoryManager,
) : BaseStorageViewModel(
    app = app,
    buildInfoProvider = buildInfoProvider,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,
    settingsManager = settingsManager,
    appPathProvider = appPathProvider,
    storageProvider = storageProvider,
    storagePathProvider = storagePathProvider,
) {

    var currentSortMode: HistorySortMode = HistorySortMode.DATE_DESC

    fun loadData() {
        launchOnMain {
            loadStorageHistory()
        }
    }

    fun sortData(sortMode: HistorySortMode) {
        currentSortMode = sortMode
        loadData()
    }

    private suspend fun loadStorageHistory() {
        showProgressWithText(R.string.state_loading)
        withIo {
            historyManager.getAll(storageId = getStorageId(), sortMode = currentSortMode)
        }.onComplete {
            hideProgress()
        }.onFailure {
            logFailure(failure = it, show = true)
        }.onSuccess { items ->
            sendEvent(HistoryEvent.LoadData(items))
        }
    }

    fun clearStorageHistory() {
        launchOnMain {
            withIo {
                historyManager.deleteAll(getStorageId())
            }.onFailure {
                logFailure(failure = it, show = true)
            }.onSuccess {
                logDebug(R.string.log_storage_history_was_cleared, show = true)
                loadStorageHistory()
            }
        }
    }

    fun deleteItemFromHistory(historyEntity: HistoryEntity) {
        launchOnMain {
            withIo {
                historyManager.delete(historyEntity)
            }.onFailure {
                logFailure(failure = it, show = true)
            }.onSuccess {
                logOperRes(LogObj.HISTORY_ITEM, LogOper.DELETE, historyEntity.obj, show = false)
                loadStorageHistory()
            }
        }
    }

}