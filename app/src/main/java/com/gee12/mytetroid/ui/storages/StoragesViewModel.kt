package com.gee12.mytetroid.ui.storages

import android.app.Activity
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.flatMap
import com.gee12.mytetroid.common.onFailure
import com.gee12.mytetroid.common.onSuccess
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.interactor.StoragesInteractor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.provider.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.ui.base.BaseViewModel
import com.gee12.mytetroid.domain.usecase.storage.CheckStorageFilesExistingUseCase
import com.gee12.mytetroid.domain.usecase.storage.InitStorageFromDefaultSettingsUseCase

class StoragesViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    settingsManager: CommonSettingsManager,
    private val buildInfoProvider: BuildInfoProvider,
    private val storagesInteractor: StoragesInteractor,
    private val checkStorageFilesExistingUseCase: CheckStorageFilesExistingUseCase,
    private val initStorageFromDefaultSettingsUseCase: InitStorageFromDefaultSettingsUseCase,
) : BaseViewModel(
    app,
    resourcesProvider,
    logger,
    notificator,
    failureHandler,
    settingsManager,
) {

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

    fun getCurrentStorageId(): Int? {
        val currentStorageProvider = ScopeSource.current.scope.get<IStorageProvider>()
        return currentStorageProvider.storage?.id
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
        if (buildInfoProvider.isFreeVersion() && storages.value?.isNotEmpty() == true) {
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
        launchOnMain {
            withIo {
                // заполняем поля настройками по-умолчанию
                initStorageFromDefaultSettingsUseCase.run(storage)
                    .flatMap { storagesInteractor.addStorage(storage).toRight() }
            }.onFailure {
                logFailure(it)
            }.onSuccess { result ->
                if (result) {
                    log(getString(R.string.log_storage_added_mask, storage.name), true)
                    loadStorages()
                    sendEvent(StoragesEvent.AddedNewStorage(storage))
                } else {
                    logDuringOperErrors(LogObj.STORAGE, LogOper.ADD, true)
                }
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