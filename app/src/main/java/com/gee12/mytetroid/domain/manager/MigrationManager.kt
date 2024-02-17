package com.gee12.mytetroid.domain.manager

import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.getNameFromPath
import com.gee12.mytetroid.common.flatMap
import com.gee12.mytetroid.common.map
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.domain.usecase.storage.FillStorageFieldsFromDefaultSettingsUseCase
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.repo.FavoritesRepo
import com.gee12.mytetroid.domain.repo.StoragesRepo
import com.gee12.mytetroid.model.TetroidStorage

class MigrationManager(
    private val buildInfoProvider: BuildInfoProvider,
    private val settingsManager: CommonSettingsManager,
    private val storagesRepo: StoragesRepo,
    private val favoritesRepo: FavoritesRepo,
    private val fillStorageFieldsFromDefaultSettingsUseCase: FillStorageFieldsFromDefaultSettingsUseCase,
) {

    suspend fun isNeedMigrateStorageFromPrefs(): Boolean {
        return settingsManager.getSettingsVersion() < Constants.SETTINGS_VERSION_CURRENT
            && storagesRepo.getStoragesCount() == 0
            && settingsManager.getStoragePath().isNotEmpty()
    }

    /**
     * Миграция с версии < 5.0, когда не было многобазовости.
     */
    suspend fun addDefaultStorageFromPrefs(): Boolean {
        return fillStorageFieldsFromDefaultSettingsUseCase.run(
            storage = settingsManager.getStoragePath().let { path ->
                TetroidStorage(
                    name = path.getNameFromPath(),
                    uri = path,
                )
            }
        ).map { storage ->
            storage.also {
                it.isDefault = true
                it.middlePassHash = settingsManager.getMiddlePassHash()
                it.quickNodeId = settingsManager.getQuicklyNodeId()
                it.lastNodeId = settingsManager.getLastNodeId()
            }
        }.flatMap { storage ->
            storagesRepo.addStorage(storage).toRight()
                .flatMap { result ->
                    if (result && buildInfoProvider.isFullVersion()) {
                        val favorites = settingsManager.getFavorites()
                        favorites.forEach { recordId ->
                            favoritesRepo.addFavorite(
                                storageId = storage.id,
                                recordId = recordId,
                            )
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