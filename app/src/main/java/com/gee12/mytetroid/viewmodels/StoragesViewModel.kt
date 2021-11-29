package com.gee12.mytetroid.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.CommonSettings
import com.gee12.mytetroid.interactors.StorageInteractor
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
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

    fun setDefault(storage: TetroidStorage) {
        launch(Dispatchers.IO) {
            if (storagesRepo.setIsDefault(storage)) {
                log(getString(R.string.log_storage_set_is_default_mask).format(storage.name), true)
                loadStorages()
            } else {
                logError(getString(R.string.error_storage_set_is_default_mask).format(storage.name), true)
                showSnackMoreInLogs()
            }
        }
    }

    fun addStorage(storage: TetroidStorage) {
        // заполняем поля настройками по-умолчанию
        initStorage(storage)

        launch(Dispatchers.IO) {
            if (storagesRepo.addStorage(storage)) {
                log(getString(R.string.log_storage_added_mask).format(storage.name), true)
                loadStorages()
                postStorageEvent(Constants.StorageEvents.Added, storage)
            } else {
                logDuringOperErrors(LogObj.STORAGE, LogOper.ADD, true)
            }
        }
    }

    fun deleteStorage(storage: TetroidStorage) {
        launch(Dispatchers.IO) {
            if (storagesRepo.deleteStorage(storage)) {
                log(getString(R.string.log_storage_deleted_mask).format(storage.name), true)
                loadStorages()
            } else {
                logDuringOperErrors(LogObj.STORAGE, LogOper.DELETE, true)
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