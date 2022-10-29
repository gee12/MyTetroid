package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Intent
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.data.crypt.IEncryptHelper
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.repo.StoragesRepo
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
    commonSettingsInteractor: CommonSettingsInteractor,
    dataInteractor: DataInteractor,
    settingsInteractor: CommonSettingsInteractor,
    interactionInteractor: InteractionInteractor,
    syncInteractor: SyncInteractor,
    trashInteractor: TrashInteractor,
    initAppUseCase: InitAppUseCase,
    initOrCreateStorageUseCase: InitOrCreateStorageUseCase,
    readStorageUseCase: ReadStorageUseCase,
    saveStorageUseCase: SaveStorageUseCase,
    checkStoragePasswordUseCase: CheckStoragePasswordUseCase,
    changePasswordUseCase: ChangePasswordUseCase,
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
    commonSettingsInteractor,
    dataInteractor,
    settingsInteractor,
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

    enum class Event {
        MyTetraXmlLastModifiedDate,
        StorageFolderSize
    }

    fun getStorageInfo(): IStorageInfoProvider = storageDataProcessor

    fun startInitStorage(intent: Intent) {
        launchOnMain {
            sendViewEvent(Constants.ViewEvents.TaskStarted)
            if (!super.initStorage(intent)) {
                sendStorageEvent(Constants.StorageEvents.InitFailed)
            }
            sendViewEvent(Constants.ViewEvents.TaskFinished)
        }
    }

    fun computeStorageFolderSize() {
        launchOnMain {
            val size = withIo {
                storageInteractor.getStorageFolderSize(getContext()) ?: getString(R.string.title_error)
            }
            sendEvent(Event.StorageFolderSize, size)
        }
    }

    fun computeMyTetraXmlLastModifiedDate() {
        launchOnMain {
            val date = withIo {
                storageInteractor.getMyTetraXmlLastModifiedDate(getContext())?.let {
                    Utils.dateToString(it, getString(R.string.full_date_format_string))
                } ?: getString(R.string.title_error)
            }
            sendEvent(Event.MyTetraXmlLastModifiedDate, date)
        }
    }

}
