package com.gee12.mytetroid.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.interactors.StoragesInteractor
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.repo.StoragesRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StoragesViewModel(
    app: Application,
    /*logger: TetroidLogger?,*/
) : BaseStorageViewModel(app/*, logger*/) {

    private val storagesInteractor = StoragesInteractor(StoragesRepo(app))

    private val _storages = MutableLiveData<List<TetroidStorage>>()
    val storages: LiveData<List<TetroidStorage>> get() = _storages

    fun loadStorages() {
        launch(Dispatchers.IO) {
            val storages = storagesInteractor.getStorages()
            _storages.postValue(storages)
        }
    }

    fun setDefault(storage: TetroidStorage) {
        launch(Dispatchers.IO) {
            if (storagesInteractor.setIsDefault(storage)) {
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
        storagesInteractor.initStorage(getContext(), storage)

        launch(Dispatchers.IO) {
            if (storagesInteractor.addStorage(storage)) {
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
            if (storagesInteractor.deleteStorage(storage)) {
                log(getString(R.string.log_storage_deleted_mask).format(storage.name), true)
                loadStorages()
            } else {
                logDuringOperErrors(LogObj.STORAGE, LogOper.DELETE, true)
            }
        }
    }

}