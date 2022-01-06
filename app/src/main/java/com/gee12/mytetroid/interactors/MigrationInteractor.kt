package com.gee12.mytetroid.interactors

import android.content.Context
import com.gee12.mytetroid.App
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.repo.CommonSettingsRepo

class MigrationInteractor(
    private val logger: ITetroidLogger,
    private val commonSettingsRepo: CommonSettingsRepo,
    private val storagesInteractor: StoragesInteractor,
    private val favoritesInteractor: FavoritesInteractor
) {

    suspend fun isNeedMigrateStorageFromPrefs(context: Context): Boolean {
        return CommonSettings.getSettingsVersion(context) < Constants.SETTINGS_VERSION_CURRENT
            && storagesInteractor.getStoragesCount() == 0
            && CommonSettings.getStoragePath(context) != null
    }

    /**
     * Миграция с версии < 5.0, когда не было многобазовости.
     */
    suspend fun addDefaultStorageFromPrefs(context: Context): Boolean {
        val storage = storagesInteractor.initStorage(
            context,
            TetroidStorage(
                path = CommonSettings.getStoragePath(context)
            )
        ).apply {
            isDefault = true
            middlePassHash = CommonSettings.getMiddlePassHash(context)
            quickNodeId = CommonSettings.getQuicklyNodeId(context)
            lastNodeId = CommonSettings.getLastNodeId(context)
        }

        val result = storagesInteractor.addStorage(storage)

        if (result && App.isFullVersion()) {
            val favorites = CommonSettings.getFavorites(context)
            favorites.forEach { recordId ->
                favoritesInteractor.addFavorite(storage.id, recordId)
            }
        }

        return result
    }

}