package com.gee12.mytetroid.ui.base

import android.app.Application
import androidx.documentfile.provider.DocumentFile
import com.gee12.mytetroid.common.extensions.orFalse
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.model.TetroidTag
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IStoragePathProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.model.permission.TetroidPermission
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
    settingsManager: CommonSettingsManager,
    appPathProvider: IAppPathProvider,
    val storageProvider: IStorageProvider,
    val storagePathProvider: IStoragePathProvider,
) : BaseViewModel(
    application = app,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,
    settingsManager = settingsManager,
    appPathProvider = appPathProvider,
), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    open val storage: TetroidStorage?
        get() = storageProvider.storage

    open val storageFolder: DocumentFile?
        get() = storageProvider.rootFolder

    val storageFolderPath: String
        get() = storageFolder?.uri?.path.orEmpty()

    abstract fun startInitStorageFromBase(storageId: Int)


    // region Permissions

    fun onStorageAccessGranted(requestCode: Int, root: DocumentFile) {
        PermissionRequestCode.fromCode(requestCode)?.also {
            onPermissionGranted(
                permission = TetroidPermission.FileStorage.Write(root),
                requestCode = it
            )
        }
    }

    // endregion Permissions

    //region Event

    //endregion Event


    //region Getters

    fun getRootNode() = storageProvider.getRootNode()

    fun isStorageInited() = storage?.isInited.orFalse()

    fun isStorageLoaded() = storage?.isLoaded.orFalse() && storageProvider.isLoaded()

    abstract fun isStorageEncrypted(): Boolean

    fun isStorageDecrypted() = storage?.isDecrypted.orFalse()

    fun isStorageNonEncryptedOrDecrypted() = !isStorageEncrypted() || isStorageDecrypted()

    fun getStorageId() = storage?.id ?: 0

    fun getStorageUri() = storage?.uri

    fun getStorageFolderPath(): FilePath {
        return storagePathProvider.getPathToRootFolder()
    }

    fun getStorageName() = storage?.name.orEmpty()

    fun isDefaultStorage() = storage?.isDefault.orFalse()

    fun isStorageReadOnly() = storage?.isReadOnly.orFalse()

    fun getStorageTrashFolderPath(): FilePath {
        return storagePathProvider.getPathToStorageTrashFolder()
    }

    fun getStorageSyncProfile() = storage?.syncProfile

    fun isStorageSyncEnabled() = storage?.syncProfile?.isEnabled.orFalse()

    fun getStorageSyncAppName() = storage?.syncProfile?.appName.orEmpty()

    fun getStorageSyncCommand() = storage?.syncProfile?.command.orEmpty()

    fun isLoadFavoritesOnly() = /*storageProvider.isLoadedFavoritesOnly()*/ storage?.isLoadFavoritesOnly.orFalse()

    fun isKeepLastNode() = storage?.isKeepLastNode.orFalse()

    fun getLastNodeId() = storage?.lastNodeId

    fun isSaveMiddlePassLocal() = storage?.isSavePassLocal.orFalse()

    fun isDecryptAttachesToTemp() = storage?.isDecryptToTemp.orFalse()

    fun getMiddlePassHash() = storage?.middlePassHash

    fun isCheckOutsideChanging() = storage?.syncProfile?.isCheckOutsideChanging.orFalse()

    fun isNodesExist() = storageProvider.getRootNodes().isNotEmpty()

    fun isLoadedFavoritesOnly() = storageProvider.isLoadedFavoritesOnly()

    fun getRootNodes(): List<TetroidNode> = storageProvider.getRootNodes()

    fun getTagsMap(): Map<String, TetroidTag> = storageProvider.getTagsMap()

    //endregion Getters

}