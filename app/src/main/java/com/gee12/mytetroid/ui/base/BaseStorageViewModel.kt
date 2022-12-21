package com.gee12.mytetroid.ui.base

import android.app.Application
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.model.TetroidTag
import com.gee12.mytetroid.domain.provider.CommonSettingsProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

abstract class BaseStorageViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    commonSettingsProvider: CommonSettingsProvider,
    val storageProvider: IStorageProvider,
) : BaseViewModel(
    application = app,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,
    commonSettingsProvider = commonSettingsProvider,
), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    open val storage: TetroidStorage?
        get() = storageProvider.storage


    abstract fun startInitStorageFromBase(storageId: Int)


    //region Event

    // TODO: можно избавиться
    suspend fun sendEventFromCallbackParam(callbackEvent: VMEvent) {
        when (callbackEvent) {
            is BaseEvent -> this.sendEvent(callbackEvent)
            else -> {}
        }
    }

    //endregion Event


    //region Getters

    fun getRootNode() = storageProvider.getRootNode()

    fun isStorageInited() = storage?.isInited ?: false

    fun isStorageLoaded() = (storage?.isLoaded ?: false) && storageProvider.isLoaded()

    abstract fun isStorageCrypted(): Boolean

    fun isStorageDecrypted() = storage?.isDecrypted ?: false

    fun isStorageNonEncryptedOrDecrypted() = !isStorageCrypted() || isStorageDecrypted()

    fun getStorageId() = storage?.id ?: 0

    fun getStoragePath() = storage?.path.orEmpty()

    fun getStorageName() = storage?.name ?: ""

    fun isStorageDefault() = storage?.isDefault ?: false

    fun isStorageReadOnly() = storage?.isReadOnly ?: false

    fun getTrashPath() = storage?.trashPath.orEmpty()

    fun getStorageSyncProfile() = storage?.syncProfile

    fun isStorageSyncEnabled() = storage?.syncProfile?.isEnabled ?: false

    fun getStorageSyncAppName() = storage?.syncProfile?.appName.orEmpty()

    fun getStorageSyncCommand() = storage?.syncProfile?.command.orEmpty()

    fun isLoadFavoritesOnly() = /*storageProvider.isLoadedFavoritesOnly()*/ storage?.isLoadFavoritesOnly ?: false

    fun isKeepLastNode() = storage?.isKeepLastNode ?: false

    fun getLastNodeId() = storage?.lastNodeId

    fun isSaveMiddlePassLocal() = storage?.isSavePassLocal ?: false

    fun isDecryptAttachesToTemp() = storage?.isDecyptToTemp ?: false

    fun getMiddlePassHash() = storage?.middlePassHash

    fun isCheckOutsideChanging() = storage?.syncProfile?.isCheckOutsideChanging ?: false

    fun isNodesExist() = storageProvider.getRootNodes().isNotEmpty()

    fun isLoadedFavoritesOnly() = storageProvider.isLoadedFavoritesOnly()

    fun getRootNodes(): List<TetroidNode> = storageProvider.getRootNodes()

    fun getTagsMap(): Map<String, TetroidTag> = storageProvider.getTagsMap()

    //endregion Getters

}

data class PermissionRequestParams(
    val permission: Constants.TetroidPermission,
    val requestCallback: () -> Unit
)