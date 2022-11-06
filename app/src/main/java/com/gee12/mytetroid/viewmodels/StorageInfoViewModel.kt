package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Intent
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.data.crypt.IEncryptHelper
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.usecase.GetFileModifiedDateUseCase
import com.gee12.mytetroid.usecase.GetFolderSizeUseCase
import com.gee12.mytetroid.usecase.InitAppUseCase
import com.gee12.mytetroid.usecase.crypt.ChangePasswordUseCase
import com.gee12.mytetroid.usecase.crypt.CheckStoragePasswordUseCase
import com.gee12.mytetroid.usecase.storage.InitOrCreateStorageUseCase
import com.gee12.mytetroid.usecase.storage.ReadStorageUseCase
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import kotlinx.coroutines.*

class StorageInfoViewModel(
    app: Application,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    commonSettingsProvider: CommonSettingsProvider,
    appBuildHelper: AppBuildHelper,
    favoritesInteractor: FavoritesInteractor,
    storageProvider: IStorageProvider,
    sensitiveDataProvider: ISensitiveDataProvider,
    passInteractor: PasswordInteractor,
    storageCrypter: IEncryptHelper,
    cryptInteractor: EncryptionInteractor,
    recordsInteractor: RecordsInteractor,
    nodesInteractor: NodesInteractor,
    tagsInteractor: TagsInteractor,
    attachesInteractor: AttachesInteractor,
    storagesRepo: StoragesRepo,
    private val storageDataProcessor: IStorageDataProcessor,
    storagePathHelper: IStoragePathHelper,
    recordPathHelper: IRecordPathHelper,
    dataInteractor: DataInteractor,
    interactionInteractor: InteractionInteractor,
    syncInteractor: SyncInteractor,
    trashInteractor: TrashInteractor,
    initAppUseCase: InitAppUseCase,
    initOrCreateStorageUseCase: InitOrCreateStorageUseCase,
    readStorageUseCase: ReadStorageUseCase,
    saveStorageUseCase: SaveStorageUseCase,
    checkStoragePasswordUseCase: CheckStoragePasswordUseCase,
    changePasswordUseCase: ChangePasswordUseCase,
    private val getFolderSizeUseCase: GetFolderSizeUseCase,
    private val getFileModifiedDateUseCase: GetFileModifiedDateUseCase,
) : StorageViewModel(
    app,
    logger,
    notificator,
    failureHandler,
    commonSettingsProvider,
    appBuildHelper,
    storageProvider,
    favoritesInteractor,
    sensitiveDataProvider,
    passInteractor,
    storageCrypter,
    cryptInteractor,
    recordsInteractor,
    nodesInteractor,
    tagsInteractor,
    attachesInteractor,
    storagesRepo,
    storagePathHelper,
    recordPathHelper,
    dataInteractor,
    interactionInteractor,
    syncInteractor,
    trashInteractor,
    initAppUseCase,
    initOrCreateStorageUseCase,
    readStorageUseCase,
    saveStorageUseCase,
    checkStoragePasswordUseCase,
    changePasswordUseCase,
), CoroutineScope {

    sealed class Event {
        sealed class GetMyTetraXmlLastModifiedDate {
            object InProgress : GetMyTetraXmlLastModifiedDate()
            data class Failed(val failure: Failure) : GetMyTetraXmlLastModifiedDate()
            data class Success(val date: String) : GetMyTetraXmlLastModifiedDate()
        }
        sealed class GetStorageFolderSize {
            object InProgress : GetStorageFolderSize()
            data class Failed(val failure: Failure) : GetStorageFolderSize()
            data class Success(val size: String) : GetStorageFolderSize()
        }
    }

    fun getStorageInfo(): IStorageInfoProvider = storageDataProcessor

    fun startInitStorage(intent: Intent) {
        launchOnMain {
            sendViewEvent(Constants.ViewEvents.TaskStarted)
            if (!initStorage(intent)) {
                sendStorageEvent(Constants.StorageEvents.InitFailed)
            }
            sendViewEvent(Constants.ViewEvents.TaskFinished)
        }
    }

    fun computeStorageFolderSize() {
        launchOnMain {
            sendEvent(Event.GetStorageFolderSize.InProgress)
            withIo {
                getFolderSizeUseCase.run(
                    GetFolderSizeUseCase.Params(
                        folderPath = storagePathHelper.getStoragePath(),
                    )
                ).onFailure {
                    sendEvent(Event.GetStorageFolderSize.Failed(it))
                    val title = failureHandler.getFailureMessage(it)
                    logError(getString(R.string.error_get_storage_folder_size_mask).format(title))
                }.onSuccess { size ->
                    sendEvent(Event.GetStorageFolderSize.Success(size))
                }
            }
        }
    }

    fun computeMyTetraXmlLastModifiedDate() {
        launchOnMain {
            sendEvent(Event.GetMyTetraXmlLastModifiedDate.InProgress)
            getFileModifiedDateUseCase.run(
                GetFileModifiedDateUseCase.Params(
                    filePath = storagePathHelper.getPathToMyTetraXml(),
                )
            ).map { date ->
                Utils.dateToString(date, getString(R.string.full_date_format_string))
            }.onFailure {
                sendEvent(Event.GetMyTetraXmlLastModifiedDate.Failed(it))
                val title = failureHandler.getFailureMessage(it)
                logError(getString(R.string.error_get_mytetra_xml_modified_date_mask).format(title))
            }.onSuccess { dateString ->
                sendEvent(Event.GetMyTetraXmlLastModifiedDate.Success(dateString))
            }
        }
    }

}
