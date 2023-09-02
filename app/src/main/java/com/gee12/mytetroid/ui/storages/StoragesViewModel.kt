package com.gee12.mytetroid.ui.storages

import android.app.Application
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.uriToAbsolutePath
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.domain.repo.StoragesRepo
import com.gee12.mytetroid.ui.base.BaseViewModel
import com.gee12.mytetroid.domain.usecase.storage.CheckStorageFilesExistingUseCase
import com.gee12.mytetroid.domain.usecase.storage.DeleteStorageUseCase
import com.gee12.mytetroid.domain.usecase.storage.FillStorageFieldsFromDefaultSettingsUseCase
import com.gee12.mytetroid.model.FilePath

class StoragesViewModel(
    app: Application,
    buildInfoProvider: BuildInfoProvider,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    settingsManager: CommonSettingsManager,
    appPathProvider: IAppPathProvider,
    private val storagesRepo: StoragesRepo,
    private val checkStorageFilesExistingUseCase: CheckStorageFilesExistingUseCase,
    private val fillStorageFieldsFromDefaultSettingsUseCase: FillStorageFieldsFromDefaultSettingsUseCase,
    private val deleteStorageUseCase: DeleteStorageUseCase,
) : BaseViewModel(
    app = app,
    buildInfoProvider = buildInfoProvider,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,
    settingsManager = settingsManager,
    appPathProvider = appPathProvider,
) {

    private val _storages = MutableLiveData<List<TetroidStorage>>()
    val storages: LiveData<List<TetroidStorage>> get() = _storages

    var checkStoragesFilesExisting: Boolean = false


    fun loadStorages() {
        launchOnIo {
            val storages = storagesRepo.getStorages()
                .onEach {
                    if (checkStoragesFilesExisting) {
                        checkStorageFilesExisting(it)
                    }
                }
            _storages.postValue(storages)
        }
    }

    fun getCurrentStorageId(): Int? {
        val currentStorageProvider = getCurrentStorageProvider()
        return currentStorageProvider.storage?.id
    }

    private fun getCurrentStorageProvider(): IStorageProvider {
        return ScopeSource.current.scope.get()
    }

    private suspend fun checkStorageFilesExisting(storage: TetroidStorage) {
        storage.error = checkStorageFilesExistingUseCase.run(
            CheckStorageFilesExistingUseCase.Params(storage)
        ).foldResult(
            onLeft = {
                failureHandler.getFailureMessage(it).getFullMassage()
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
            if (withIo { storagesRepo.setIsDefault(storage) }) {
                log(getString(R.string.log_storage_set_is_default_mask, storage.name), true)
                loadStorages()
            } else {
                logError(getString(R.string.error_storage_set_is_default_mask, storage.name), true)
                showSnackMoreInLogs()
            }
        }
    }

    fun addNewStorage(isNew: Boolean) {
        if (buildInfoProvider.isFreeVersion() && storages.value?.isNotEmpty() == true) {
            showMessage(R.string.mes_cant_more_one_storage_on_free)
        } else {
            launchOnMain {
                sendEvent(StoragesEvent.ShowAddStorageDialog(isNew))
            }
        }
    }

    fun addStorage(storage: TetroidStorage) {
        launchOnMain {
            withIo {
                // заполняем поля настройками по-умолчанию
                fillStorageFieldsFromDefaultSettingsUseCase.run(storage)
                    .flatMap { storagesRepo.addStorage(storage).toRight() }
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

    fun deleteStorage(
        storage: TetroidStorage,
        withFiles: Boolean,
        deleteIfAlreadyLoaded: Boolean
    ) {
        launchOnMain {
            withIo {
                deleteStorageUseCase.run(
                    DeleteStorageUseCase.Params(
                        storage = storage,
                        withFiles = withFiles,
                    )
                ).map {
                    if (storage.id == getCurrentStorageId() && deleteIfAlreadyLoaded) {
                        clearCurrentStorageDataFromMemory()
                    }
                }
            }.onComplete {
                loadStorages()
            }.onFailure {
                logFailure(it)
            }.onSuccess {
                showMessage(getString(R.string.log_storage_deleted_mask, storage.name))
                log(getString(R.string.log_storage_deleted_detailed_mask,
                    storage.name,
                    storage.uri.uriToAbsolutePath(getContext()),
                    withFiles
                ), show = false)
            }
        }
    }

    private fun clearCurrentStorageDataFromMemory() {
        val currentScope = ScopeSource.current.scope
        val sensitiveDataProvider = currentScope.getOrNull<ISensitiveDataProvider>()
        sensitiveDataProvider?.resetMiddlePasswordHash()
        val storageCryptManager = currentScope.getOrNull<IStorageCryptManager>()
        storageCryptManager?.reset()
        val currentStorageProvider = currentScope.getOrNull<IStorageProvider>()
        currentStorageProvider?.reset()
    }

    fun checkFolderForNewStorage(folder: DocumentFile, isNew: Boolean) {
        launchOnIo {
            fileStorageManager.checkFolder(folder)?.also { folder ->
                val folderPath = FilePath.FolderFull(folder.getAbsolutePath(getContext()))
                if (folder.exists()) {
                    if (isNew) {
                        if (folder.isEmpty(getContext())) {
                            launchOnMain {
                                sendEvent(StoragesEvent.SetStorageFolder(folder))
                            }
                        } else {
                            showFailure(Failure.Storage.Create.FolderNotEmpty(folderPath))
                        }
                    } else {
                        launchOnMain {
                            sendEvent(StoragesEvent.SetStorageFolder(folder))
                        }
                    }
                } else {
                    showFailure(Failure.Folder.NotExist(folderPath))
                }
            }
        }
    }

}