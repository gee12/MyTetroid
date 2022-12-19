package com.gee12.mytetroid.ui.storage.info

import android.app.Application
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.data.crypt.IStorageCrypter
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.providers.*
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.ui.storage.StorageEvent
import com.gee12.mytetroid.usecase.file.GetFileModifiedDateUseCase
import com.gee12.mytetroid.usecase.file.GetFolderSizeUseCase
import com.gee12.mytetroid.usecase.InitAppUseCase
import com.gee12.mytetroid.usecase.crypt.*
import com.gee12.mytetroid.usecase.node.GetNodeByIdUseCase
import com.gee12.mytetroid.usecase.record.GetRecordByIdUseCase
import com.gee12.mytetroid.usecase.storage.CheckStorageFilesExistingUseCase
import com.gee12.mytetroid.usecase.storage.InitOrCreateStorageUseCase
import com.gee12.mytetroid.usecase.storage.ReadStorageUseCase
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.usecase.tag.ParseRecordTagsUseCase
import com.gee12.mytetroid.ui.storage.StorageParams
import com.gee12.mytetroid.viewmodels.StorageViewModel
import kotlinx.coroutines.*

class StorageInfoViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,

    commonSettingsProvider: CommonSettingsProvider,
    buildInfoProvider: BuildInfoProvider,
    storageProvider: IStorageProvider,
    sensitiveDataProvider: ISensitiveDataProvider,
    dataNameProvider: IDataNameProvider,
    storagePathProvider: IStoragePathProvider,
    recordPathProvider: IRecordPathProvider,

    storageCrypter: IStorageCrypter,
    storagesRepo: StoragesRepo,
    private val storageDataProcessor: IStorageDataProcessor,

    tagsInteractor: TagsInteractor,
    favoritesManager: FavoritesManager,
    passInteractor: PasswordInteractor,
    interactionInteractor: InteractionInteractor,
    syncInteractor: SyncInteractor,
    trashInteractor: TrashInteractor,

    initAppUseCase: InitAppUseCase,
    getFolderSizeUseCase: GetFolderSizeUseCase,
    getFileModifiedDateUseCase: GetFileModifiedDateUseCase,

    initOrCreateStorageUseCase: InitOrCreateStorageUseCase,
    readStorageUseCase: ReadStorageUseCase,
    saveStorageUseCase: SaveStorageUseCase,
    checkStoragePasswordUseCase: CheckStoragePasswordAndAskUseCase,
    changePasswordUseCase: ChangePasswordUseCase,
    decryptStorageUseCase: DecryptStorageUseCase,
    checkStoragePasswordAndDecryptUseCase: CheckStoragePasswordAndDecryptUseCase,
    checkStorageFilesExistingUseCase: CheckStorageFilesExistingUseCase,
    setupPasswordUseCase : SetupPasswordUseCase,
    initPasswordUseCase : InitPasswordUseCase,

    getNodeByIdUseCase: GetNodeByIdUseCase,
    getRecordByIdUseCase: GetRecordByIdUseCase,

    private val cryptRecordFilesUseCase: CryptRecordFilesUseCase,
    private val parseRecordTagsUseCase: ParseRecordTagsUseCase,
) : StorageViewModel(
    app = app,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,

    commonSettingsProvider = commonSettingsProvider,
    buildInfoProvider = buildInfoProvider,
    storageProvider = storageProvider,
    sensitiveDataProvider = sensitiveDataProvider,
    storagePathProvider = storagePathProvider,
    recordPathProvider = recordPathProvider,
    dataNameProvider = dataNameProvider,

    storagesRepo = storagesRepo,
    storageCrypter = storageCrypter,

    favoritesManager = favoritesManager,
    interactionInteractor = interactionInteractor,
    passInteractor = passInteractor,
    syncInteractor = syncInteractor,
    trashInteractor = trashInteractor,
    tagsInteractor = tagsInteractor,

    initAppUseCase = initAppUseCase,
    getFileModifiedDateUseCase = getFileModifiedDateUseCase,
    getFolderSizeUseCase = getFolderSizeUseCase,

    initOrCreateStorageUseCase = initOrCreateStorageUseCase,
    readStorageUseCase = readStorageUseCase,
    saveStorageUseCase = saveStorageUseCase,
    checkStoragePasswordUseCase = checkStoragePasswordUseCase,
    changePasswordUseCase = changePasswordUseCase,
    decryptStorageUseCase = decryptStorageUseCase,
    checkStoragePasswordAndDecryptUseCase = checkStoragePasswordAndDecryptUseCase,
    checkStorageFilesExistingUseCase = checkStorageFilesExistingUseCase,
    setupPasswordUseCase = setupPasswordUseCase,
    initPasswordUseCase = initPasswordUseCase,

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

    override fun startInitStorage(storageId: Int) {
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
        storageCrypter.init(
            cryptRecordFilesUseCase,
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
                    GetFolderSizeUseCase.Params(
                        folderPath = storagePathProvider.getStoragePath(),
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
                GetFileModifiedDateUseCase.Params(
                    filePath = storagePathProvider.getPathToMyTetraXml(),
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
