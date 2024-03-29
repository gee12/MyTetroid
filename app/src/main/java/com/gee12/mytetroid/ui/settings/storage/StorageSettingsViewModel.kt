package com.gee12.mytetroid.ui.settings.storage

import android.app.Application
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.onFailure
import com.gee12.mytetroid.common.onSuccess
import com.gee12.mytetroid.data.settings.TetroidPreferenceDataStore
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.interactor.*
import com.gee12.mytetroid.domain.manager.*
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.domain.repo.StoragesRepo
import com.gee12.mytetroid.domain.usecase.crypt.*
import com.gee12.mytetroid.domain.usecase.file.GetFileModifiedDateInStorageUseCase
import com.gee12.mytetroid.domain.usecase.file.GetFolderSizeInStorageUseCase
import com.gee12.mytetroid.domain.usecase.node.GetNodeByIdUseCase
import com.gee12.mytetroid.domain.usecase.record.GetRecordByIdUseCase
import com.gee12.mytetroid.domain.usecase.storage.*
import com.gee12.mytetroid.ui.storage.StorageEvent
import com.gee12.mytetroid.ui.storage.StorageViewModel

class StorageSettingsViewModel(
    app: Application,
    buildInfoProvider: BuildInfoProvider,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,

    appPathProvider: IAppPathProvider,
    settingsManager: CommonSettingsManager,
    storageProvider: IStorageProvider,
    sensitiveDataProvider: ISensitiveDataProvider,
    storagePathProvider: IStoragePathProvider,
    recordPathProvider: IRecordPathProvider,
    dataNameProvider: IDataNameProvider,

    storagesRepo: StoragesRepo,
    cryptManager: IStorageCryptManager,
    storageDataProcessor: IStorageDataProcessor,

    favoritesManager: FavoritesManager,
    interactionManager: InteractionManager,
    syncInteractor: SyncInteractor,

    getFolderSizeUseCase: GetFolderSizeInStorageUseCase,
    getFileModifiedDateUseCase: GetFileModifiedDateInStorageUseCase,

    initOrCreateStorageUseCase: InitOrCreateStorageUseCase,
    readStorageUseCase: ReadStorageUseCase,
    saveStorageUseCase: SaveStorageUseCase,
    decryptStorageUseCase: DecryptStorageUseCase,
    checkStorageFilesExistingUseCase: CheckStorageFilesExistingUseCase,
    clearStorageTrashFolderUseCase: ClearStorageTrashFolderUseCase,
    checkStoragePasswordAndDecryptUseCase: CheckPasswordOrPinAndDecryptUseCase,
    checkStoragePasswordUseCase: CheckPasswordOrPinAndAskUseCase,
    changePasswordUseCase: ChangePasswordUseCase,
    setupPasswordUseCase : SetupPasswordUseCase,
    private val saveMiddlePasswordHashUseCase: SaveMiddlePasswordHashUseCase,
    private val clearSavedPasswordHashUseCase: ClearSavedPasswordHashUseCase,
    private val checkPasswordUseCase: CheckPasswordUseCase,

    getNodeByIdUseCase: GetNodeByIdUseCase,
    getRecordByIdUseCase: GetRecordByIdUseCase,
) : StorageViewModel(
    app = app,
    buildInfoProvider = buildInfoProvider,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,

    settingsManager = settingsManager,
    appPathProvider = appPathProvider,
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

    getNodeByIdUseCase = getNodeByIdUseCase,
    getRecordByIdUseCase = getRecordByIdUseCase,
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

    init {
        storageProvider.init(storageDataProcessor)
    }

    // region Options

    /**
     * Установка отдельного параметра хранилища и сохранение в бд.
     */
    override fun updateStorageOption(key: String, value: Any) {
        storage?.apply {
            val isFieldsChanged = when (key) {
                // основное
                getString(R.string.pref_key_storage_path) -> {
                    isFieldChanged(key, uri, value.toString()) {
                        uri = it
                        isStoragePathChanged = true
                        log(getString(R.string.log_storage_path_changed_mask, uri), false)
                    }
                }
                getString(R.string.pref_key_storage_name) -> {
                    isFieldChanged(key, name, value.toString()) {
                        name = it
                        log(getString(R.string.log_storage_name_changed_mask, name), false)
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
                getString(R.string.pref_key_is_decrypt_in_temp) -> isFieldChanged(key, isDecryptToTemp, value) { isDecryptToTemp = it }

                // синхронизация
                getString(R.string.pref_key_is_sync_storage) -> isFieldChanged(key, syncProfile.isEnabled, value) { syncProfile.isEnabled = it }
                getString(R.string.pref_key_app_for_sync) -> isFieldChanged(key, syncProfile.appName, value) { syncProfile.appName = it }
                getString(R.string.pref_key_sync_command) -> isFieldChanged(key, syncProfile.command, value) { syncProfile.command = it }
                getString(R.string.pref_key_is_sync_before_init) -> isFieldChanged(key, syncProfile.isSyncBeforeInit, value) { syncProfile.isSyncBeforeInit = it }
                getString(R.string.pref_key_is_ask_before_sync) -> isFieldChanged(key, syncProfile.isAskBeforeSyncOnInit, value) { syncProfile.isAskBeforeSyncOnInit = it }
                getString(R.string.pref_key_is_sync_before_exit) -> isFieldChanged(key, syncProfile.isSyncBeforeExit, value) { syncProfile.isSyncBeforeExit = it }
                getString(R.string.pref_key_is_ask_before_exit_sync) -> isFieldChanged(key, syncProfile.isAskBeforeSyncOnExit, value) { syncProfile.isAskBeforeSyncOnExit = it }
                getString(R.string.pref_key_check_outside_changing) -> isFieldChanged(key, syncProfile.isCheckOutsideChanging, value) { syncProfile.isCheckOutsideChanging = it }
                else -> false
            }
            if (isFieldsChanged) {
                this@StorageSettingsViewModel.isFieldsChanged = true
                launchOnMain {
                    withIo {
                        updateStorageInDb(storage = this@apply)
                    }
                    onStorageOptionChanged(key, value)

                    if (isStoragePathChanged) {
                        onStoragePathChanged()
                    }
                }
            }
        }
    }

    private fun isFieldChanged(key: String, curValue: String?, newValue: Any, onChanged: ((String) -> Unit)? = null): Boolean {
        val newStringValue = newValue.toString()
        return (if (curValue != newStringValue) {
            onChanged?.invoke(newStringValue)
            logDebug(getString(R.string.log_field_changed_mask, key, curValue ?: "null", newValue), false)
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
            true
        } else {
            false
        })
    }

    fun getStorageOption(key: String): Any? {
        return storage?.let {
            when (key) {
                // основное
                getString(R.string.pref_key_storage_path) -> it.uri
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
                getString(R.string.pref_key_is_decrypt_in_temp) -> it.isDecryptToTemp
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

    // endregion Options

    // region Storage

    fun updateStorageFolder(folder: DocumentFile) {
        fileStorageManager.checkFolder(folder)?.also {
            val path = it.uri.toString()
            updateStorageOption(getString(R.string.pref_key_storage_path), path)
        }
    }

    // endregion Storage

    // region Password

    fun checkPasswordAndChange(curPassword: String, newPassword: String): Boolean {
        return checkPasswordUseCase.execute(
            CheckPasswordUseCase.Params(password = curPassword)
        ).foldResult(
            onLeft = {
                logFailure(it)
                false
            },
            onRight = { result ->
                when (result) {
                    is CheckPasswordUseCase.Result.PasswordNotSet -> {
                        true
                    }
                    is CheckPasswordUseCase.Result.Success -> {
                        startChangePassword(curPassword, newPassword)
                        true
                    }
                    is CheckPasswordUseCase.Result.AskForEmptyPassCheckingField -> {
                        launchOnMain {
                            sendEvent(
                                StorageEvent.AskForEmptyPassCheckingField(
                                    fieldName = result.fieldName,
                                    passHash = "",
                                    callbackEvent = StorageEvent.ChangePassDirectly(
                                        curPass = curPassword,
                                        newPass = newPassword,
                                    ),
                                )
                            )
                        }
                        true
                    }
                    is CheckPasswordUseCase.Result.NotMatched -> {
                        logger.logError(R.string.log_cur_pass_is_incorrect, show = true)
                        false
                    }
                }
            }
        )
    }

    fun startSetupPassword(password: String) {
        launchOnMain {
            setupPassword(password)
        }
    }

    fun onPasswordLocalHashLocalParamChanged(isSaveLocal: Boolean): Boolean {
        return if (getMiddlePassHash() != null) {
            // если пароль задан, то проверяем ПИН-код
            askPinCode(
                specialFlag = true,
                callbackEvent = StorageEvent.SavePassHashLocalChanged(isSaveLocal)
            )
            false
        } else {
            launchOnMain {
                sendEvent(StorageEvent.SavePassHashLocalChanged(isSaveLocal))
            }
            false
        }
    }

    fun saveMiddlePassHashLocalIfCached() {
        sensitiveDataProvider.getMiddlePasswordHashOrNull()?.let { middlePasswordHash ->
            launchOnMain {
                withIo {
                    // сохраняем хеш локально, если пароль был введен
                    saveMiddlePasswordHashUseCase.run(
                        SaveMiddlePasswordHashUseCase.Params(middlePasswordHash)
                    )
                }.onFailure {
                    logFailure(it)
                }.onSuccess {
                    log(R.string.log_pass_hash_saved_local, show = true)
                }
            }
        }
    }

    fun clearSavedPasswordHash(isDropSavePasswordLocal: Boolean) {
        launchOnMain {
            withIo {
                clearSavedPasswordHashUseCase.run(
                    ClearSavedPasswordHashUseCase.Params(
                        isDropSavePasswordLocal = isDropSavePasswordLocal,
                    )
                )
            }.onFailure {
                logFailure(it)
            }
        }
    }

    // endregion Password

}
