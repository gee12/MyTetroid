package com.gee12.mytetroid.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.crypt.IEncryptHelper
import com.gee12.mytetroid.data.settings.TetroidPreferenceDataStore
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.usecase.InitAppUseCase
import com.gee12.mytetroid.usecase.crypt.ChangePasswordUseCase
import com.gee12.mytetroid.usecase.crypt.CheckStoragePasswordAndDecryptUseCase
import com.gee12.mytetroid.usecase.crypt.CheckStoragePasswordAndAskUseCase
import com.gee12.mytetroid.usecase.crypt.DecryptStorageUseCase
import com.gee12.mytetroid.usecase.storage.CheckStorageFilesExistingUseCase
import com.gee12.mytetroid.usecase.storage.InitOrCreateStorageUseCase
import com.gee12.mytetroid.usecase.storage.ReadStorageUseCase
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase

class StorageSettingsViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    appBuildHelper: AppBuildHelper,
    commonSettingsProvider: CommonSettingsProvider,
    storageProvider: IStorageProvider,
    favoritesInteractor: FavoritesInteractor,
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
    checkStorageFilesExistingUseCase,
) {

    private val _updateStorageField = MutableLiveData<Pair<String,Any>>()
    val updateStorageField: LiveData<Pair<String,Any>> get() = _updateStorageField

    val prefsDataStore = TetroidPreferenceDataStore(object : TetroidPreferenceDataStore.IDataStore {
        override fun saveValue(key: String, value: Any?) {
            updateStorageOption(key, value ?: "")
        }
        override fun getValue(key: String): Any? {
            return getStorageOption(key)
        }
    })

    var isFieldsChanged = false
    var isStoragePathChanged = false

    /**
     * Установка отдельного параметра хранилища и сохранение в бд.
     */
    override fun updateStorageOption(key: String, value: Any) {
        storage?.apply {
            when (key) {
                // основное
                getString(R.string.pref_key_storage_path) -> {
                    isFieldChanged(key, path, value.toString()) {
                        path = it
                        isStoragePathChanged = true
                        log(getString(R.string.log_storage_path_changed_mask, path), false)
                    }
                }
                getString(R.string.pref_key_storage_name) -> {
                    isFieldChanged(key, name, value.toString()) {
                        name = it
                        log(getString(R.string.log_storage_name_changed_mask, name), false)
                    }
                }
                getString(R.string.pref_key_temp_path) -> {
                    isFieldChanged(key, trashPath, value) {
                        trashPath = it
                        log(getString(R.string.log_storage_trash_path_changed_mask, name), false)
                    }
                }
                getString(R.string.pref_key_is_def_storage) -> isFieldChanged(key, isDefault, value) { isDefault = it }
                getString(R.string.pref_key_is_read_only) -> isFieldChanged(key, isReadOnly, value) { isReadOnly = it }
                getString(R.string.pref_key_is_clear_trash_before_exit) -> isFieldChanged(key, isClearTrashBeforeExit, value) { isClearTrashBeforeExit = it }
                getString(R.string.pref_key_is_ask_before_clear_trash_before_exit) -> isFieldChanged(key, isAskBeforeClearTrashBeforeExit, value) { isAskBeforeClearTrashBeforeExit = it }
                getString(R.string.pref_key_is_load_favorites) -> isFieldChanged(key, isLoadFavoritesOnly, value) { isLoadFavoritesOnly = it }
                getString(R.string.pref_key_is_keep_selected_node) -> isFieldChanged(key, isKeepLastNode, value) { isKeepLastNode = it }
                getString(R.string.pref_key_quickly_node_id) -> isFieldChanged(key, quickNodeId, value) { quickNodeId = it }

                // шифрование
                getString(R.string.pref_key_is_save_pass_hash_local) -> isFieldChanged(key, isSavePassLocal, value) { isSavePassLocal = it }
                getString(R.string.pref_key_is_decrypt_in_temp) -> isFieldChanged(key, isDecyptToTemp, value) { isDecyptToTemp = it }

                // синхронизация
                getString(R.string.pref_key_is_sync_storage) -> isFieldChanged(key, syncProfile.isEnabled, value) { syncProfile.isEnabled = it }
                getString(R.string.pref_key_app_for_sync) -> isFieldChanged(key, syncProfile.appName, value) { syncProfile.appName = it }
                getString(R.string.pref_key_sync_command) -> isFieldChanged(key, syncProfile.command, value) { syncProfile.command = it }
                getString(R.string.pref_key_is_sync_before_init) -> isFieldChanged(key, syncProfile.isSyncBeforeInit, value) { syncProfile.isSyncBeforeInit = it }
                getString(R.string.pref_key_is_ask_before_sync) -> isFieldChanged(key, syncProfile.isAskBeforeSyncOnInit, value) { syncProfile.isAskBeforeSyncOnInit = it }
                getString(R.string.pref_key_is_sync_before_exit) -> isFieldChanged(key, syncProfile.isSyncBeforeExit, value) { syncProfile.isSyncBeforeExit = it }
                getString(R.string.pref_key_is_ask_before_exit_sync) -> isFieldChanged(key, syncProfile.isAskBeforeSyncOnExit, value) { syncProfile.isAskBeforeSyncOnExit = it }
                getString(R.string.pref_key_check_outside_changing) -> isFieldChanged(key, syncProfile.isCheckOutsideChanging, value) { syncProfile.isCheckOutsideChanging = it }
            }
            if (isFieldsChanged) {
                launchOnMain {
                    updateStorage(this@apply)
                    onStorageOptionChanged(key, value)
                }
            }
        }
    }

    private fun isFieldChanged(key: String, curValue: String?, newValue: Any, onChanged: ((String) -> Unit)? = null): Boolean {
        val newStringValue = newValue.toString()
        return (if (curValue != newStringValue) {
            onChanged?.invoke(newStringValue)
            logDebug(getString(R.string.log_field_changed_mask, key, curValue ?: "null", newValue), false)
            isFieldsChanged = true
            true
        } else {
            false
        })
    }

    private fun isFieldChanged(key: String, curValue: Boolean, newValue: Any, onChanged: ((Boolean) -> Unit)? = null): Boolean {
        val newBoolValue = newValue.toString().toBoolean()
        return (if (curValue != newBoolValue) {
            onChanged?.invoke(newBoolValue)
            logDebug(getString(R.string.log_field_changed_mask, key, curValue, newValue), false)
            isFieldsChanged = true
            true
        } else {
            false
        })
    }

    fun getStorageOption(key: String): Any? {
        return storage?.let {
            when (key) {
                // основное
                getString(R.string.pref_key_storage_path) -> it.path
                getString(R.string.pref_key_storage_name) -> it.name
                getString(R.string.pref_key_is_def_storage) -> it.isDefault
                getString(R.string.pref_key_is_read_only) -> it.isReadOnly
                getString(R.string.pref_key_temp_path) -> it.trashPath
                getString(R.string.pref_key_is_clear_trash_before_exit) -> it.isClearTrashBeforeExit
                getString(R.string.pref_key_is_ask_before_clear_trash_before_exit) -> it.isAskBeforeClearTrashBeforeExit
                getString(R.string.pref_key_is_load_favorites) -> it.isLoadFavoritesOnly
                getString(R.string.pref_key_is_keep_selected_node) -> it.isKeepLastNode
                // шифрование
                getString(R.string.pref_key_is_save_pass_hash_local) -> it.isSavePassLocal
                getString(R.string.pref_key_is_decrypt_in_temp) -> it.isDecyptToTemp
                // синхронизация
                getString(R.string.pref_key_is_sync_storage) -> it.syncProfile.isEnabled
                getString(R.string.pref_key_app_for_sync) -> it.syncProfile.appName
                getString(R.string.pref_key_sync_command) -> it.syncProfile.command
                getString(R.string.pref_key_is_sync_before_init) -> it.syncProfile.isSyncBeforeInit
                getString(R.string.pref_key_is_ask_before_sync) -> it.syncProfile.isAskBeforeSyncOnInit
                getString(R.string.pref_key_is_sync_before_exit) -> it.syncProfile.isSyncBeforeExit
                getString(R.string.pref_key_is_ask_before_exit_sync) -> it.syncProfile.isAskBeforeSyncOnExit
                getString(R.string.pref_key_check_outside_changing) -> it.syncProfile.isCheckOutsideChanging
                else -> null
            }
        }
    }

    private fun onStorageOptionChanged(key: String, value: Any) {
        _updateStorageField.postValue(Pair(key, value))
    }

}
