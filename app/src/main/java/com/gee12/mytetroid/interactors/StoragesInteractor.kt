package com.gee12.mytetroid.interactors

import android.content.Context
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.repo.StoragesRepo

class StoragesInteractor(
    private val storagesRepo: StoragesRepo
) {

    suspend fun getStorages() = storagesRepo.getStorages()

    suspend fun getStoragesCount() = storagesRepo.getStoragesCount()

    suspend fun addStorage(storage: TetroidStorage) = storagesRepo.addStorage(storage)

    suspend fun deleteStorage(storage: TetroidStorage) = storagesRepo.deleteStorage(storage)

    suspend fun setIsDefault(storage: TetroidStorage) = storagesRepo.setIsDefault(storage)

    fun initStorage(context: Context, storage: TetroidStorage): TetroidStorage {
        return storage.apply {
            // основное
            isLoadFavoritesOnly = CommonSettings.isLoadFavoritesOnlyDef(context)
            isKeepLastNode = CommonSettings.isKeepLastNodeDef(context)
            // корзина
            trashPath = CommonSettings.getTrashPathDef(context)
            isClearTrashBeforeExit = CommonSettings.isClearTrashBeforeExitDef(context)
            isAskBeforeClearTrashBeforeExit = CommonSettings.isAskBeforeClearTrashBeforeExitDef(context)
            // шифрование
            isSavePassLocal = CommonSettings.isSaveMiddlePassHashLocalDef(context)
            isDecyptToTemp = CommonSettings.isDecryptFilesInTempDef(context)
            // синхронизация
            syncProfile.apply {
                isEnabled = CommonSettings.isSyncStorageDef(context)
                appName = CommonSettings.getSyncAppNameDef(context)
                command = CommonSettings.getSyncCommandDef(context) ?: ""
                isSyncBeforeInit = CommonSettings.isSyncBeforeInitDef(context)
                isAskBeforeSyncOnInit = CommonSettings.isAskBeforeSyncOnInitDef(context)
                isSyncBeforeExit = CommonSettings.isSyncBeforeExitDef(context)
                isAskBeforeSyncOnExit = CommonSettings.isAskBeforeSyncOnExitDef(context)
                isCheckOutsideChanging = CommonSettings.isCheckOutsideChangingDef(context)
            }
        }
    }

}