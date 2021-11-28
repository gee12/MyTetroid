package com.gee12.mytetroid.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.CommonSettings
import com.gee12.mytetroid.interactors.StorageInteractor
import com.gee12.mytetroid.logs.TetroidLogger
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.repo.StoragesRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StoragesViewModel(
    app: Application,
    /*logger: TetroidLogger?,*/
//    private val storageInteractor: StorageInteractor,
    private val storagesRepo: StoragesRepo
) : BaseStorageViewModel(app/*, logger*/) {

    private val _storages = MutableLiveData<List<TetroidStorage>>()
    val storages: LiveData<List<TetroidStorage>> get() = _storages

    fun loadStorages() {
        launch(Dispatchers.IO) {
            var storages = storagesRepo.getStorages()

            if (storages.isEmpty() && CommonSettings.getStoragePath(getContext())?.isNotEmpty() == true) {
                addDefaultStorageFromPrefs()
                storages = storagesRepo.getStorages()
            }

            _storages.postValue(storages)
        }
    }

    fun addStorage(storage: TetroidStorage) {
        // заполняем поля настройками по-умолчанию
        initStorage(storage)

        launch(Dispatchers.IO) {
            if (storagesRepo.addStorage(storage)) {
                loadStorages()

                postStorageEvent(Constants.StorageEvents.Added, storage)
            }
        }
    }

    fun deleteStorage(storage: TetroidStorage) {
        launch(Dispatchers.IO) {
            if (storagesRepo.deleteStorage(storage) > 0) {
                loadStorages()
            }
        }
    }

    private fun initStorage(storage: TetroidStorage): TetroidStorage {
        return storage.apply {
            val context = getContext()
            // основное
            trashPath = CommonSettings.getTrashPath(context)
            isLoadFavoritesOnly = CommonSettings.isLoadFavoritesOnlyDef(context)
            isKeepLastNode = CommonSettings.isKeepLastNodeDef(context)
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

    private suspend fun addDefaultStorageFromPrefs() {
        storagesRepo.addStorage(initStorage(TetroidStorage(CommonSettings.getStoragePath(getContext()))).apply {
            isDefault = true
        })
    }
}