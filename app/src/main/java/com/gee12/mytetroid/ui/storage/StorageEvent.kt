package com.gee12.mytetroid.ui.storage

import com.gee12.mytetroid.common.ICallback
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.base.BaseEvent

abstract class StorageEvent : BaseEvent() {
    object NoDefaultStorage : StorageEvent()
    data class NotFoundInBase(
        val storageId: Int,
    ) : StorageEvent()
    data class FoundInBase(
        val storage: TetroidStorage,
    ) : StorageEvent()
    data class AskBeforeClearTrashOnExit(
        val callback: ICallback?
    ) : StorageEvent()
//        object AskAfterSyncManually : StorageEvents()
    sealed class AskOnSync : StorageEvent() {
        class BeforeInit(val callback: ICallback?) : AskOnSync()
        class BeforeExit(val callback: ICallback?) : AskOnSync()
        class AfterInit(val result: Boolean) : AskOnSync()
        class AfterExit(val result: Boolean) : AskOnSync()
    }
    object AskForSyncAfterFailureSyncOnExit : StorageEvent()
    data class Inited(
        val storage: TetroidStorage,
    ) : StorageEvent()
    data class InitFailed(
        val isOnlyFavorites: Boolean
    ) : StorageEvent()
    data class FilesCreated(
        val storage: TetroidStorage,
    ) : StorageEvent()
    data class LoadOrDecrypt(
        val params: StorageParams,
    ) : StorageEvent()
    object StartLoadingOrDecrypting: StorageEvent()
    data class Loaded(
        val isLoaded: Boolean,
    ) : StorageEvent()
    object Decrypted : StorageEvent()
    object TreeChangedOutside : StorageEvent()
    object TreeDeletedOutside : StorageEvent()

    // password
    data class AskPassword(
        val callbackEvent: BaseEvent,
    ) : StorageEvent()
    data class AskForEmptyPassCheckingField(
        val fieldName: String,
        val passHash: String,
        val callbackEvent: BaseEvent,
    ) : StorageEvent()
    object AskForClearStoragePass : StorageEvent()
    data class SavePassHashLocalChanged(
        val isSaveLocal: Boolean,
    ) : StorageEvent()
    object PassSetuped : StorageEvent()
    object PassChanged : StorageEvent()
    data class ChangePassDirectly(
        val curPass: String,
        val newPass: String,
    ) : StorageEvent()

    // pincode
    data class AskPinCode(
        val callbackEvent: BaseEvent,
    ) : StorageEvent()
}