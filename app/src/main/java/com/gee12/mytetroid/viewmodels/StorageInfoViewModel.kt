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
import com.gee12.mytetroid.usecase.crypt.CheckStoragePasswordAndDecryptUseCase
import com.gee12.mytetroid.usecase.crypt.CheckStoragePasswordAndAskUseCase
import com.gee12.mytetroid.usecase.crypt.DecryptStorageUseCase
import com.gee12.mytetroid.usecase.storage.InitOrCreateStorageUseCase
import com.gee12.mytetroid.usecase.storage.ReadStorageUseCase
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import kotlinx.coroutines.*

class StorageInfoViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
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
    checkStoragePasswordUseCase: CheckStoragePasswordAndAskUseCase,
    changePasswordUseCase: ChangePasswordUseCase,
    private val getFolderSizeUseCase: GetFolderSizeUseCase,
    private val getFileModifiedDateUseCase: GetFileModifiedDateUseCase,
    decryptStorageUseCase: DecryptStorageUseCase,
    checkStoragePasswordAndDecryptUseCase: CheckStoragePasswordAndDecryptUseCase,
) : StorageViewModel(
    app,
    resourcesProvider,
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
    decryptStorageUseCase,
    checkStoragePasswordAndDecryptUseCase,
), CoroutineScope {

    sealed class StorageInfoEvent : VMEvent() {
        sealed class GetMyTetraXmlLastModifiedDate : StorageInfoEvent() {
            object InProgress : GetMyTetraXmlLastModifiedDate()
            data class Failed(val failure: Failure) : GetMyTetraXmlLastModifiedDate()
            data class Success(val date: String) : GetMyTetraXmlLastModifiedDate()
        }
        sealed class GetStorageFolderSize : StorageInfoEvent() {
            object InProgress : GetStorageFolderSize()
            data class Failed(val failure: Failure) : GetStorageFolderSize()
            data class Success(val size: String) : GetStorageFolderSize()
        }
    }

    fun getStorageInfo(): IStorageInfoProvider = storageDataProcessor

    fun startInitStorage(intent: Intent) {
        launchOnMain {
            sendViewEvent(ViewEvent.TaskStarted())
            if (!initStorage(intent)) {
                sendStorageEvent(StorageEvent.InitFailed(isOnlyFavorites = checkIsNeedLoadFavoritesOnly()))
            }
            sendViewEvent(ViewEvent.TaskFinished)
        }
    }

    fun computeStorageFolderSize() {
        launchOnMain {
            sendEvent(StorageInfoEvent.GetStorageFolderSize.InProgress)
            withIo {
                getFolderSizeUseCase.run(
                    GetFolderSizeUseCase.Params(
                        folderPath = storagePathHelper.getStoragePath(),
                    )
                ).onFailure {
                    sendEvent(StorageInfoEvent.GetStorageFolderSize.Failed(it))
                    val title = failureHandler.getFailureMessage(it)
                    logError(getString(R.string.error_get_storage_folder_size_mask).format(title))
                }.onSuccess { size ->
                    sendEvent(StorageInfoEvent.GetStorageFolderSize.Success(size))
                }
            }
        }
    }

    fun computeMyTetraXmlLastModifiedDate() {
        launchOnMain {
            sendEvent(StorageInfoEvent.GetMyTetraXmlLastModifiedDate.InProgress)
            getFileModifiedDateUseCase.run(
                GetFileModifiedDateUseCase.Params(
                    filePath = storagePathHelper.getPathToMyTetraXml(),
                )
            ).map { date ->
                Utils.dateToString(date, getString(R.string.full_date_format_string))
            }.onFailure {
                sendEvent(StorageInfoEvent.GetMyTetraXmlLastModifiedDate.Failed(it))
                val title = failureHandler.getFailureMessage(it)
                logError(getString(R.string.error_get_mytetra_xml_modified_date_mask).format(title))
            }.onSuccess { dateString ->
                sendEvent(StorageInfoEvent.GetMyTetraXmlLastModifiedDate.Success(dateString))
            }
        }
    }

}
