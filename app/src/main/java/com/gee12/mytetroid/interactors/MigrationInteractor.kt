package com.gee12.mytetroid.interactors

import android.content.Context
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.helpers.AppBuildHelper
import com.gee12.mytetroid.helpers.CommonSettingsProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage

class MigrationInteractor(
    private val logger: ITetroidLogger,
    private val appBuildHelper: AppBuildHelper,
    private val commonSettingsProvider: CommonSettingsProvider,
    private val storagesInteractor: StoragesInteractor,
    private val favoritesInteractor: FavoritesInteractor,
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
        val storage = storagesInteractor.initStorage(
            TetroidStorage(
                path = commonSettingsProvider.getStoragePath()
            )
        ).apply {
            isDefault = true
            middlePassHash = commonSettingsProvider.getMiddlePassHash()
            quickNodeId = commonSettingsProvider.getQuicklyNodeId()
            lastNodeId = commonSettingsProvider.getLastNodeId()
        }

        val result = storagesInteractor.addStorage(storage)

        if (result && appBuildHelper.isFullVersion()) {
            val favorites = commonSettingsProvider.getFavorites()
            favorites.forEach { recordId ->
                favoritesInteractor.addFavorite(storage.id, recordId)
            }
        }

        return result
    }

}