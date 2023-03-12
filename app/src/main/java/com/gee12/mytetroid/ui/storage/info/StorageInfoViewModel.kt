package com.gee12.mytetroid.ui.storage.info

import android.app.Application
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.interactor.*
import com.gee12.mytetroid.domain.manager.*
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.domain.repo.StoragesRepo
import com.gee12.mytetroid.ui.storage.StorageEvent
import com.gee12.mytetroid.domain.usecase.file.GetFileModifiedDateInStorageUseCase
import com.gee12.mytetroid.domain.usecase.file.GetFolderSizeInStorageUseCase
import com.gee12.mytetroid.domain.usecase.InitAppUseCase
import com.gee12.mytetroid.domain.usecase.crypt.*
import com.gee12.mytetroid.domain.usecase.node.GetNodeByIdUseCase
import com.gee12.mytetroid.domain.usecase.record.GetRecordByIdUseCase
import com.gee12.mytetroid.domain.usecase.storage.*
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import com.gee12.mytetroid.ui.storage.StorageParams
import com.gee12.mytetroid.ui.storage.StorageViewModel
import kotlinx.coroutines.*

class StorageInfoViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,

    settingsManager: CommonSettingsManager,
    appPathProvider: IAppPathProvider,
    buildInfoProvider: BuildInfoProvider,
    storageProvider: IStorageProvider,
    sensitiveDataProvider: ISensitiveDataProvider,
    dataNameProvider: IDataNameProvider,
    storagePathProvider: IStoragePathProvider,
    recordPathProvider: IRecordPathProvider,

    cryptManager: IStorageCryptManager,
    storagesRepo: StoragesRepo,
    private val storageDataProcessor: IStorageDataProcessor,

    favoritesManager: FavoritesManager,
    interactionManager: InteractionManager,
    syncInteractor: SyncInteractor,

    initAppUseCase: InitAppUseCase,
    getFolderSizeUseCase: GetFolderSizeInStorageUseCase,
    getFileModifiedDateUseCase: GetFileModifiedDateInStorageUseCase,

    initOrCreateStorageUseCase: InitOrCreateStorageUseCase,
    readStorageUseCase: ReadStorageUseCase,
    saveStorageUseCase: SaveStorageUseCase,
    decryptStorageUseCase: DecryptStorageUseCase,
    checkStorageFilesExistingUseCase: CheckStorageFilesExistingUseCase,
    clearStorageTrashFolderUseCase : ClearStorageTrashFolderUseCase,
    checkStoragePasswordAndDecryptUseCase: CheckPasswordOrPinAndDecryptUseCase,
    checkStoragePasswordUseCase: CheckPasswordOrPinAndAskUseCase,
    changePasswordUseCase: ChangePasswordUseCase,
    setupPasswordUseCase : SetupPasswordUseCase,
    initPasswordUseCase : InitPasswordUseCase,
    clearSavedPasswordUseCase: ClearSavedPasswordUseCase,

    getNodeByIdUseCase: GetNodeByIdUseCase,
    getRecordByIdUseCase: GetRecordByIdUseCase,

    private val cryptRecordFilesIfNeedUseCase: CryptRecordFilesIfNeedUseCase,
    private val parseRecordTagsUseCase: ParseRecordTagsUseCase,
) : StorageViewModel(
    app = app,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,

    settingsManager = settingsManager,
    appPathProvider = appPathProvider,
    buildInfoProvider = buildInfoProvider,
    storageProvider = storageProvider,
    sensitiveDataProvider = sensitiveDataProvider,
    storagePathProvider = storagePathProvider,
    recordPathProvider = recordPathProvider,
    dataNameProvider = dataNameProvider,

    storagesRepo = storagesRepo,
    cryptManager = cryptManager,

    favoritesManager = favoritesManager,
    interactionManager = interactionManager,
    syncInteractor = syncInteractor,

    initAppUseCase = initAppUseCase,
    getFileModifiedDateUseCase = getFileModifiedDateUseCase,
    getFolderSizeUseCase = getFolderSizeUseCase,

    initOrCreateStorageUseCase = initOrCreateStorageUseCase,
    readStorageUseCase = readStorageUseCase,
    saveStorageUseCase = saveStorageUseCase,
    decryptStorageUseCase = decryptStorageUseCase,
    checkStorageFilesExistingUseCase = checkStorageFilesExistingUseCase,
    clearStorageTrashFolderUseCase = clearStorageTrashFolderUseCase,
    checkPasswordOrPinAndDecryptUseCase = checkStoragePasswordAndDecryptUseCase,
    checkPasswordOrPinUseCase = checkStoragePasswordUseCase,
    changePasswordUseCase = changePasswordUseCase,
    setupPasswordUseCase = setupPasswordUseCase,
    initPasswordUseCase = initPasswordUseCase,
    clearSavedPasswordUseCase = clearSavedPasswordUseCase,

    getNodeByIdUseCase = getNodeByIdUseCase,
    getRecordByIdUseCase = getRecordByIdUseCase,
), CoroutineScope {

    sealed class StorageInfoEvent : StorageEvent() {
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

    fun getStorageInfo(): IStorageInfoProvider = storageProvider.dataProcessor

    override fun checkPermissionsAndInitStorageById(storageId: Int) {
        if (storage?.id == storageId) {
            storage?.let {
                launchOnMain {
                    sendEvent(StorageEvent.FoundInBase(it))
                    sendEvent(StorageEvent.Inited(it))
                }
            }
        } else {
            if (storageId > 0) {
                prepareToAnotherStorage()
                startInitStorageFromBase(storageId)
            } else {
                launchOnMain {
                    sendEvent(StorageEvent.InitFailed(isOnlyFavorites = checkIsNeedLoadFavoritesOnly()))
                }
            }
        }
    }

    private fun prepareToAnotherStorage() {
        // FIXME: koin: из-за циклической зависимости вместо инжекта storageDataProcessor в конструкторе,
        //  задаем его вручную позже
        storageProvider.init(storageDataProcessor)
        // TODO: нужно ли ?
        cryptManager.init(
            cryptRecordFilesIfNeedUseCase,
            parseRecordTagsUseCase,
        )
    }

    override fun loadOrDecryptStorage(params: StorageParams) {
        super.loadOrDecryptStorage(
            params = params.apply {
                isDecrypt = false
            }
        )
    }

    fun computeStorageFolderSize() {
        launchOnMain {
            sendEvent(StorageInfoEvent.GetStorageFolderSize.InProgress)
            withIo {
                getFolderSizeUseCase.run(
                    GetFolderSizeInStorageUseCase.Params(
                        folderRelativePath = "",
                    )
                ).onFailure {
                    sendEvent(StorageInfoEvent.GetStorageFolderSize.Failed(it))
                    val title = failureHandler.getFailureMessage(it)
                    logError(getString(R.string.error_get_storage_folder_size_mask, title))
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
                GetFileModifiedDateInStorageUseCase.Params(
                    fileRelativePath = Constants.MYTETRA_XML_FILE_NAME,
                )
            ).map { date ->
                Utils.dateToString(date, getString(R.string.full_date_format_string))
            }.onFailure {
                sendEvent(StorageInfoEvent.GetMyTetraXmlLastModifiedDate.Failed(it))
                val title = failureHandler.getFailureMessage(it)
                logError(getString(R.string.error_get_mytetra_xml_modified_date_mask, title))
            }.onSuccess { dateString ->
                sendEvent(StorageInfoEvent.GetMyTetraXmlLastModifiedDate.Success(dateString))
            }
        }
    }

}
