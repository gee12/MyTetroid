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
import com.gee12.mytetroid.usecase.storage.CheckStorageFilesExistingUseCase

class StoragesViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    commonSettingsProvider: CommonSettingsProvider,
    storageProvider: IStorageProvider,
    private val appBuildHelper: AppBuildHelper,
    private val storagesInteractor: StoragesInteractor,
    private val checkStorageFilesExistingUseCase: CheckStorageFilesExistingUseCase,
) : BaseStorageViewModel(
    app,
    resourcesProvider,
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
                    if (checkStoragesFilesExisting) {
                        checkStorageFilesExisting(it)
                    }
                }
            _storages.postValue(storages)
        }
    }

    private suspend fun checkStorageFilesExisting(storage: TetroidStorage) {
        storage.error = checkStorageFilesExistingUseCase.run(
            CheckStorageFilesExistingUseCase.Params(storage)
        ).foldResult(
            onLeft = {
                failureHandler.getFailureMessage(it).title
            },
            onRight = { result ->
                when (result) {
                    is CheckStorageFilesExistingUseCase.Result.Error -> {
                        result.errorsString
                    }
                    is CheckStorageFilesExistingUseCase.Result.Success -> {
                        null
                    }
                }
            }
        )
    }

    fun setDefault(storage: TetroidStorage) {
        launchOnMain {
            if (withIo { storagesInteractor.setIsDefault(storage) }) {
                log(getString(R.string.log_storage_set_is_default_mask, storage.name), true)
                loadStorages()
            } else {
                logError(getString(R.string.error_storage_set_is_default_mask, storage.name), true)
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
        storagesInteractor.initStorageFromDefaultSettings(storage)

        launchOnMain {
            if (withIo { storagesInteractor.addStorage(storage) }) {
                log(getString(R.string.log_storage_added_mask, storage.name), true)
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
                log(getString(R.string.log_storage_deleted_mask, storage.name), true)
                loadStorages()
            } else {
                logDuringOperErrors(LogObj.STORAGE, LogOper.DELETE, true)
            }
        }
    }

}