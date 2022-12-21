package com.gee12.mytetroid.domain.interactor

import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.flatMap
import com.gee12.mytetroid.common.map
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.domain.FavoritesManager
import com.gee12.mytetroid.domain.usecase.storage.InitStorageFromDefaultSettingsUseCase
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.provider.CommonSettingsProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage

class MigrationInteractor(
    private val logger: ITetroidLogger,
    private val buildInfoProvider: BuildInfoProvider,
    private val commonSettingsProvider: CommonSettingsProvider,
    private val storagesInteractor: StoragesInteractor,
    private val favoritesManager: FavoritesManager,
    private val initStorageFromDefaultSettingsUseCase: InitStorageFromDefaultSettingsUseCase,
) {

    suspend fun isNeedMigrateStorageFromPrefs(): Boolean {
        return commonSettingsProvider.getSettingsVersion() < Constants.SETTINGS_VERSION_CURRENT
            && storagesInteractor.getStoragesCount() == 0
            && commonSettingsProvider.getStoragePath().isNotEmpty()
    }

    /**
     * Миграция с версии < 5.0, когда не было многобазовости.
     */
    suspend fun addDefaultStorageFromPrefs(): Boolean {
        return initStorageFromDefaultSettingsUseCase.run(
            storage = TetroidStorage(
                path = commonSettingsProvider.getStoragePath()
            )
        ).map { storage ->
            storage.also {
                it.isDefault = true
                it.middlePassHash = commonSettingsProvider.getMiddlePassHash()
                it.quickNodeId = commonSettingsProvider.getQuicklyNodeId()
                it.lastNodeId = commonSettingsProvider.getLastNodeId()
            }
        }.flatMap { storage ->
            storagesInteractor.addStorage(storage).toRight()
                .flatMap { result ->
                    if (result && buildInfoProvider.isFullVersion()) {
                        val favorites = commonSettingsProvider.getFavorites()
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