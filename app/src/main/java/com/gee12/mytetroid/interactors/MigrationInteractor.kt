package com.gee12.mytetroid.interactors

import android.content.Context
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.CommonSettings
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.repo.CommonSettingsRepo

class MigrationInteractor(
    private val logger: ITetroidLogger,
    private val commonSettingsRepo: CommonSettingsRepo,
    private val storagesInteractor: StoragesInteractor
) {

    suspend fun isNeedMigrateStorageFromPrefs(context: Context): Boolean {
        return BuildConfig.VERSION_CODE >= Constants.VERSION_50
            && storagesInteractor.getStoragesCount() == 0
            && CommonSettings.getStoragePath(context)?.isNotEmpty() == true
    }

    /**
     * Миграция с версии < 5.0, когда не было многобазовости.
     */
    suspend fun addDefaultStorageFromPrefs(context: Context): Boolean {
        return storagesInteractor.addStorage(
            storagesInteractor.initStorage(
                context,
                TetroidStorage(
                    path = CommonSettings.getStoragePath(context)
                )
            ).apply {
                isDefault = true
                middlePassHash = CommonSettings.getMiddlePassHash(context)
                quickNodeId = CommonSettings.getQuicklyNodeId(context)
                lastNodeId = CommonSettings.getLastNodeId(context)
                // TODO: создать миграцию Избранного
//                favorites = CommonSettings.getFavorites(context)
            })
    }

}