package com.gee12.mytetroid.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.SettingsManager
import com.gee12.mytetroid.interactors.StorageInteractor
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.repo.StoragesRepo
import kotlinx.coroutines.launch

class StoragesViewModel(
    app: Application,
    private val storageInteractor: StorageInteractor,
    private val storagesRepo: StoragesRepo
) : BaseStorageViewModel(app, storagesRepo) {

    private val _storages = MutableLiveData<List<TetroidStorage>>()
    val storages: LiveData<List<TetroidStorage>> get() = _storages

    fun loadStorages() {
        viewModelScope.launch {
            var storages = storagesRepo.getStorages()

            if (storages.isEmpty() && SettingsManager.getStoragePath(getContext())?.isNotEmpty() == true) {
                addDefaultStorageFromPrefs()
                storages = storagesRepo.getStorages()
            }

            _storages.postValue(storages)
        }
    }

//    fun getDefaultStorage(): TetroidStorage {
//        viewModelScope.launch {
//            return@launch repo.getDefaultStorage()
//        }
//    }

    fun addStorage(storage: TetroidStorage) {
        // заполняем поля настройками по-умолчанию
        initStorage(storage)

        viewModelScope.launch {
            if (storagesRepo.addStorage(storage) > 0) {
                loadStorages()

                storageEvent.postValue(ViewModelEvent(Constants.StorageEvents.Added, storage))
            }
        }
    }

    fun createStorage(storage: TetroidStorage) {
        viewModelScope.launch {
            log(getString(R.string.log_start_storage_creating) + storage.path, ILogger.Types.DEBUG)

            if (storageInteractor.createStorage(storage)) {
                log((R.string.log_storage_created), true)
                storageEvent.postValue(ViewModelEvent(Constants.StorageEvents.FilesCreated, storage))
            } else {
                logError(getString(R.string.log_failed_storage_create_mask, storage.path), true)
            }
        }
    }

    fun deleteStorage(storage: TetroidStorage) {
        viewModelScope.launch {
            if (storagesRepo.deleteStorage(storage) > 0) {
                loadStorages()
            }
        }
    }

    private fun initStorage(storage: TetroidStorage): TetroidStorage {
        return storage.apply {
            val context = getContext()
            // основное
            trashPath = SettingsManager.getTrashPath(context)
            isLoadFavoritesOnly = SettingsManager.isLoadFavoritesOnlyDef(context)
            isKeepLastNode = SettingsManager.isKeepLastNodeDef(context)
            // шифрование
            isSavePassLocal = SettingsManager.isSaveMiddlePassHashLocalDef(context)
            isDecyptToTemp = SettingsManager.isDecryptFilesInTempDef(context)
            // синхронизация
            syncProfile.apply {
                isEnabled = SettingsManager.isSyncStorageDef(context)
                appName = SettingsManager.getSyncAppNameDef(context)
                command = SettingsManager.getSyncCommandDef(context) ?: ""
                isSyncBeforeInit = SettingsManager.isSyncBeforeInitDef(context)
                isAskBeforeSyncOnInit = SettingsManager.isAskBeforeSyncOnInitDef(context)
                isSyncBeforeExit = SettingsManager.isSyncBeforeExitDef(context)
                isAskBeforeSyncOnExit = SettingsManager.isAskBeforeSyncOnExitDef(context)
                isCheckOutsideChanging = SettingsManager.isCheckOutsideChangingDef(context)
            }
        }
    }

    private suspend fun addDefaultStorageFromPrefs() {
        storagesRepo.addStorage(initStorage(TetroidStorage(SettingsManager.getStoragePath(getContext()))).apply {
            isDefault = true
        })
    }
}