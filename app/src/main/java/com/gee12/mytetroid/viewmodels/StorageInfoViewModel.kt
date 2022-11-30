package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Intent
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.data.crypt.IEncryptHelper
import com.gee12.mytetroid.data.xml.StorageDataXmlProcessor
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.repo.FavoritesRepo
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.usecase.GetFileModifiedDateUseCase
import com.gee12.mytetroid.usecase.GetFolderSizeUseCase
import com.gee12.mytetroid.usecase.InitAppUseCase
import com.gee12.mytetroid.usecase.LoadNodeIconUseCase
import com.gee12.mytetroid.usecase.crypt.*
import com.gee12.mytetroid.usecase.storage.CheckStorageFilesExistingUseCase
import com.gee12.mytetroid.usecase.storage.InitOrCreateStorageUseCase
import com.gee12.mytetroid.usecase.storage.ReadStorageUseCase
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.usecase.tag.ParseRecordTagsUseCase
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
    private val currentStorageProvider: IStorageProvider,
    sensitiveDataProvider: ISensitiveDataProvider,
    passInteractor: PasswordInteractor,
    storageCrypter: IEncryptHelper,
    cryptInteractor: EncryptionInteractor,
    recordsInteractor: RecordsInteractor,
    nodesInteractor: NodesInteractor,
    tagsInteractor: TagsInteractor,
    attachesInteractor: AttachesInteractor,
    storagesRepo: StoragesRepo,
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
    decryptStorageUseCase: DecryptStorageUseCase,
    checkStoragePasswordAndDecryptUseCase: CheckStoragePasswordAndDecryptUseCase,
    checkStorageFilesExistingUseCase: CheckStorageFilesExistingUseCase,
    setupPasswordUseCase : SetupPasswordUseCase,
    initPasswordUseCase : InitPasswordUseCase,
    private val getFolderSizeUseCase: GetFolderSizeUseCase,
    private val getFileModifiedDateUseCase: GetFileModifiedDateUseCase,
) : StorageViewModel(
    app,
    resourcesProvider,
    logger,
    notificator,
    failureHandler,
    commonSettingsProvider,
    appBuildHelper,
    storageProvider = StorageProvider(logger),
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
    checkStorageFilesExistingUseCase,
    setupPasswordUseCase,
    initPasswordUseCase,
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

    fun getStorageInfo(): IStorageInfoProvider = storageProvider.dataProcessor

    fun startInitStorage(intent: Intent) {
        launchOnMain {
            sendViewEvent(ViewEvent.TaskStarted())
            if (!initStorage(intent)) {
                sendStorageEvent(StorageEvent.InitFailed(isOnlyFavorites = checkIsNeedLoadFavoritesOnly()))
            }
            sendViewEvent(ViewEvent.TaskFinished)
        }
    }

    override fun initStorage(intent: Intent): Boolean {
        val storageId = intent.getIntExtra(Constants.EXTRA_STORAGE_ID, 0)

        return if (currentStorageProvider.storage?.id == storageId) {
            storageProvider = currentStorageProvider
            launchOnMain {
                storage?.let {
                    sendStorageEvent(StorageEvent.FoundInBase(it))
                    sendStorageEvent(StorageEvent.Inited(it))
                }
            }
            true
        } else {
            if (storageId > 0) {
                resetToAnotherStorage()

                startInitStorageFromBase(storageId)
                true
            } else {
                false
            }
        }
    }

    /**
     * (Пере-)создаем объекты для работы с данным хранилищем,
     *  если оно не совпадает с уже загруженным.
     */
    private fun resetToAnotherStorage() {
        storagePathHelper = StoragePathHelper(storageProvider)
        val dataProcessor = StorageDataXmlProcessor(
            logger = logger,
            encryptHelper = storageCrypter,
            favoritesInteractor = FavoritesInteractor(
                favoritesRepo = FavoritesRepo(getContext()),
            ),
            parseRecordTagsUseCase = ParseRecordTagsUseCase(),
            loadNodeIconUseCase = LoadNodeIconUseCase(
                logger = logger,
                storagePathHelper = StoragePathHelper(storageProvider)
            ),
        )
        storageProvider.init(dataProcessor)
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
                    filePath = storagePathHelper.getPathToMyTetraXml(),
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
