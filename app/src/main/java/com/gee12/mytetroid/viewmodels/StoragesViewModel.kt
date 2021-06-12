package com.gee12.mytetroid.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gee12.mytetroid.data.DataManager
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.repo.StoragesRepo
import kotlinx.coroutines.launch

class StoragesViewModel(app: Application, private val repo: StoragesRepo) : BaseViewModel(app) {

    private val _storages = MutableLiveData<List<TetroidStorage>>()
    val storages: LiveData<List<TetroidStorage>> get() = _storages

    fun loadStorages() {
        viewModelScope.launch {
            val storages = repo.getStorages().toMutableList()

            // FIXME: получать список из базы
            storages.add(TetroidStorage(DataManager.getStoragePath()))

            _storages.postValue(storages)
        }
    }

    fun saveStorage(storage: TetroidStorage?, name: String, path: String, isReadOnly: Boolean) {
        if (storage != null) {
            updateStorage(storage.id, name, path, isReadOnly)
        } else {
            addStorage(name, path, isReadOnly)
        }
    }

    private fun addStorage(name: String, path: String, isReadOnly: Boolean) {
        viewModelScope.launch {
            if (repo.addStorage(name, path, isReadOnly) > 0) {
                loadStorages()
            }
        }
    }

    private fun updateStorage(id: Int, name: String, path: String, isReadOnly: Boolean) {
        viewModelScope.launch {
            if (repo.updateStorage(id, name, path, isReadOnly) > 0) {
                loadStorages()
            }
        }
    }

}