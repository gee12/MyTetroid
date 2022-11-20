package com.gee12.mytetroid.viewmodels

import android.app.Activity
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gee12.mytetroid.R
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.interactors.StoragesInteractor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidStorage

class StoragesViewModel(
    app: Application,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    commonSettingsProvider: CommonSettingsProvider,
    storageProvider: IStorageProvider,
    private val appBuildHelper: AppBuildHelper,
    private val storagePathHelper: IStoragePathHelper,
    private val storagesInteractor: StoragesInteractor,
) : BaseStorageViewModel(
    app,
    logger,
    notificator,
    failureHandler,
    commonSettingsProvider,
    storageProvider,
) {

    sealed class StoragesEvent : VMEvent() {
        object ShowAddNewStorageDialog : StoragesEvent()
    }

    private val _storages = MutableLiveData<List<TetroidStorage>>()
    val storages: LiveData<List<TetroidStorage>> get() = _storages

    var checkStoragesFilesExisting: Boolean = false

    fun loadStorages() {
        launchOnIo {
            val storages = storagesInteractor.getStorages()
                .onEach {
                    if (checkStoragesFilesExisting) checkStorageFilesExisting(it)
                }
            _storages.postValue(storages)
        }
    }

    fun checkStorageFilesExisting(storage: TetroidStorage) {
        storage.error = storagePathHelper.checkStorageFilesExistingError(getContext())
    }

    fun setDefault(storage: TetroidStorage) {
        launchOnMain {
            if (withIo { storagesInteractor.setIsDefault(storage) }) {
                log(getString(R.string.log_storage_set_is_default_mask).format(storage.name), true)
                loadStorages()
            } else {
                logError(getString(R.string.error_storage_set_is_default_mask).format(storage.name), true)
                showSnackMoreInLogs()
            }
        }
    }

    fun addNewStorage(activity: Activity) {
        if (appBuildHelper.isFreeVersion() && storages.value?.isNotEmpty() == true) {
            showMessage(R.string.mes_cant_more_one_storage_on_free)
        } else {
            // проверка разрешения перед диалогом добавления хранилища
            checkWriteExtStoragePermission(activity) {
                launchOnMain {
                    sendEvent(StoragesEvent.ShowAddNewStorageDialog)
                }
            }
        }
    }

    fun addStorage(storage: TetroidStorage) {
        // заполняем поля настройками по-умолчанию
        storagesInteractor.initStorage(getContext(), storage)

        launchOnMain {
            if (withIo { storagesInteractor.addStorage(storage) }) {
                log(getString(R.string.log_storage_added_mask).format(storage.name), true)
                loadStorages()
                sendStorageEvent(StorageViewModel.StorageEvent.Added(storage))
            } else {
                logDuringOperErrors(LogObj.STORAGE, LogOper.ADD, true)
            }
        }
    }

    fun deleteStorage(storage: TetroidStorage) {
        launchOnMain {
            if (withIo { storagesInteractor.deleteStorage(storage) }) {
                log(getString(R.string.log_storage_deleted_mask).format(storage.name), true)
                loadStorages()
            } else {
                logDuringOperErrors(LogObj.STORAGE, LogOper.DELETE, true)
            }
        }
    }

}