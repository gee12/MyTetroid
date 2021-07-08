package com.gee12.mytetroid.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gee12.mytetroid.data.DataManager
import com.gee12.mytetroid.data.SettingsManager
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.repo.StoragesRepo
import kotlinx.coroutines.launch

class StoragesViewModel(app: Application, private val repo: StoragesRepo) : BaseViewModel(app) {

    private val _storages = MutableLiveData<List<TetroidStorage>>()
    val storages: LiveData<List<TetroidStorage>> get() = _storages

    fun loadStorages() {
        viewModelScope.launch {
            var storages = repo.getStorages()

            if (storages.isEmpty() && SettingsManager.getStoragePath(getContext())?.isNotEmpty() == true) {
                addDefaultStorageFromPrefs()
                storages = repo.getStorages()
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
            if (repo.addStorage(storage) > 0) {
                loadStorages()
            }
        }
    }

    fun deleteStorage(storage: TetroidStorage) {
        viewModelScope.launch {
            if (repo.deleteStorage(storage) > 0) {
                loadStorages()
            }
        }
    }

    private fun initStorage(storage: TetroidStorage): TetroidStorage {
        return storage.apply {
            val context = getContext()
            // основное
            isLoadFavoritesOnly = SettingsManager.isLoadFavoritesOnly(context)
            isKeepLastNode = SettingsManager.isKeepLastNode(context)
            // шифрование
            isSavePassLocal = SettingsManager.isSaveMiddlePassHashLocal(context)
            isKeepLastNode = SettingsManager.isKeepLastNode(context)
            // синхронизация
            syncProfile.apply {
                isEnabled = SettingsManager.isSyncStorage(context)
                appName = SettingsManager.getSyncAppName(context)
                command = SettingsManager.getSyncCommand(context) ?: ""
                isSyncBeforeInit = SettingsManager.isSyncBeforeInit(context)
                isAskBeforeSyncOnInit = SettingsManager.isAskBeforeSyncOnInit(context)
                isSyncBeforeExit = SettingsManager.isSyncBeforeExit(context)
                isAskBeforeSyncOnExit = SettingsManager.isAskBeforeSyncOnExit(context)
                isCheckOutsideChanging = SettingsManager.isCheckOutsideChanging(context)
            }
        }
    }

    private suspend fun addDefaultStorageFromPrefs() {
        repo.addStorage(initStorage(TetroidStorage(DataManager.getStoragePath())).apply {
            isDefault = true
        })
    }
}