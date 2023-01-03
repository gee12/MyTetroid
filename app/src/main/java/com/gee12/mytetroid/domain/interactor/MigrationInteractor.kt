package com.gee12.mytetroid.domain.interactor

import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.flatMap
import com.gee12.mytetroid.common.map
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.domain.manager.FavoritesManager
import com.gee12.mytetroid.domain.usecase.storage.InitStorageFromDefaultSettingsUseCase
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage

class MigrationInteractor(
    private val logger: ITetroidLogger,
    private val buildInfoProvider: BuildInfoProvider,
    private val settingsManager: CommonSettingsManager,
    private val storagesInteractor: StoragesInteractor,
    private val favoritesManager: FavoritesManager,
    private val initStorageFromDefaultSettingsUseCase: InitStorageFromDefaultSettingsUseCase,
) {

    suspend fun isNeedMigrateStorageFromPrefs(): Boolean {
        return settingsManager.getSettingsVersion() < Constants.SETTINGS_VERSION_CURRENT
            && storagesInteractor.getStoragesCount() == 0
            && settingsManager.getStoragePath().isNotEmpty()
    }

    /**
     * Миграция с версии < 5.0, когда не было многобазовости.
     */
    suspend fun addDefaultStorageFromPrefs(): Boolean {
        return initStorageFromDefaultSettingsUseCase.run(
            storage = TetroidStorage(
                path = settingsManager.getStoragePath()
            )
        ).map { storage ->
            storage.also {
                it.isDefault = true
                it.middlePassHash = settingsManager.getMiddlePassHash()
                it.quickNodeId = settingsManager.getQuicklyNodeId()
                it.lastNodeId = settingsManager.getLastNodeId()
            }
        }.flatMap { storage ->
            storagesInteractor.addStorage(storage).toRight()
                .flatMap { result ->
                    if (result && buildInfoProvider.isFullVersion()) {
                        val favorites = settingsManager.getFavorites()
                        favorites.forEach { recordId ->
                            favoritesManager.addFavorite(storage.id, recordId)
                        }
                    }
                    result.toRight()
                }
        }.foldResult(
            onLeft = { false },
            onRight = { it }
        )
    }

}