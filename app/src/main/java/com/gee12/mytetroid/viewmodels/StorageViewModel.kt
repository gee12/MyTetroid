package com.gee12.mytetroid.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.DataManager
import com.gee12.mytetroid.data.NodesManager
import com.gee12.mytetroid.data.SettingsManager
import com.gee12.mytetroid.data.settings.TetroidPreferenceDataStore
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.repo.StoragesRepo
import kotlinx.coroutines.launch

class StorageViewModel(app: Application, private val repo: StoragesRepo) : BaseViewModel(app) {

    private val _storage = MutableLiveData<TetroidStorage>()
    val storage: LiveData<TetroidStorage> get() = _storage

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


    fun loadStorage(id: Int) {
        viewModelScope.launch {
            // TODO: нужно обрабатывать ошибки бд
            val storage = repo.getStorage(id)
            _storage.postValue(storage)
        }
    }

    fun updateStorage(storage: TetroidStorage) {
        viewModelScope.launch {
            if (repo.updateStorage(storage) > 0) {
            }
        }
    }

    /**
     *
     */
    fun updateStorageOption(key: String, value: Any) {
        _storage.value?.apply {
            var isFieldChanged = true
            when (key) {
                // основное
                getString(R.string.pref_key_storage_path) -> path = value.toString()
                getString(R.string.pref_key_storage_name) -> name = value.toString()
                getString(R.string.pref_key_is_def_storage) -> isDefault = value.toString().toBoolean()
                getString(R.string.pref_key_is_read_only) -> isReadOnly = value.toString().toBoolean()
                getString(R.string.pref_key_temp_path) -> trashPath = value.toString()
                getString(R.string.pref_key_is_load_favorites) -> isLoadFavoritesOnly = value.toString().toBoolean()
                getString(R.string.pref_key_is_keep_selected_node) -> isKeepLastNode = value.toString().toBoolean()

                // шифрование
                getString(R.string.pref_key_is_save_pass_hash_local) -> isSavePassLocal = value.toString().toBoolean()
                getString(R.string.pref_key_is_decrypt_in_temp) -> isDecyptToTemp = value.toString().toBoolean()

                // синхронизация
                getString(R.string.pref_key_is_sync_storage) -> syncProfile.isEnabled = value.toString().toBoolean()
                getString(R.string.pref_key_app_for_sync) -> syncProfile.appName = value.toString()
                getString(R.string.pref_key_sync_command) -> syncProfile.command = value.toString()
                getString(R.string.pref_key_is_sync_before_init) -> syncProfile.isSyncBeforeInit = value.toString().toBoolean()
                getString(R.string.pref_key_is_ask_before_sync) -> syncProfile.isAskBeforeSyncOnInit = value.toString().toBoolean()
                getString(R.string.pref_key_is_sync_before_exit) -> syncProfile.isSyncBeforeExit = value.toString().toBoolean()
                getString(R.string.pref_key_is_ask_before_exit_sync) -> syncProfile.isAskBeforeSyncOnExit = value.toString().toBoolean()
                getString(R.string.pref_key_check_outside_changing) -> syncProfile.isCheckOutsideChanging = value.toString().toBoolean()
                else -> isFieldChanged = false
            }
            if (isFieldChanged) {
                updateStorage(this)
                onStorageUpdated(key, value)
            }
        }
    }

    fun getStorageOption(key: String): Any? {
        return _storage.value?.let {
            when (key) {
                // основное
                getString(R.string.pref_key_storage_path) -> it.path
                getString(R.string.pref_key_storage_name) -> it.name
                getString(R.string.pref_key_is_def_storage) -> it.isDefault
                getString(R.string.pref_key_is_read_only) -> it.isReadOnly
                getString(R.string.pref_key_temp_path) -> it.trashPath
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

    fun setQuicklyNode(node: TetroidNode) {
        // TODO: переделать
        SettingsManager.setQuicklyNode(getContext(), node)
        NodesManager.setQuicklyNode(node)
        // TODO: в т.ч. установить node_name
        onStorageUpdated(getString(R.string.pref_key_quickly_node_id), getQuicklyNodeName())
    }

    fun updateQuicklyNode() {
        NodesManager.updateQuicklyNode(getContext())
    }

    fun dropSavedLocalPassHash() {
        _storage.value?.apply {
            // удаляем хэш пароля и сбрасываем галку
            middlePassHash = null
            isSavePassLocal = false
            updateStorage(this)
        }
    }

    fun clearTrashFolder() = DataManager.clearTrashFolder(getContext())

    //region Getters

    fun getStoragePath() = _storage.value?.path ?: ""

    fun getStorageName() = _storage.value?.name ?: ""

    fun getTrashPath() = _storage.value?.trashPath ?: ""

    fun getQuicklyNode(): TetroidNode? = NodesManager.getQuicklyNode()

    fun getQuicklyNodeName() = _storage.value?.quickNodeName ?: ""

    fun getSyncProfile() = _storage.value?.syncProfile

    fun getSyncAppName() = _storage.value?.syncProfile?.appName ?: ""

    fun getSyncCommand() = _storage.value?.syncProfile?.command ?: ""

    fun isDefault() = _storage.value?.isDefault ?: false

    fun isReadOnly() = _storage.value?.isReadOnly ?: false

    fun isLoadFavoritesOnly() = _storage.value?.isLoadFavoritesOnly ?: false

    fun isKeepLastNode() = _storage.value?.isKeepLastNode ?: false

    fun isInited() = _storage.value?.isInited ?: false

    fun isLoaded() = _storage.value?.isLoaded ?: false

    fun isCrypted() = DataManager.isCrypted(getContext())

    fun isDecrypted() = _storage.value?.isDecrypted ?: false

    fun isFavoritesMode() = DataManager.isFavoritesMode()

    fun isSaveMiddlePassLocal() = _storage.value?.isSavePassLocal ?: false

    fun isDecryptToTemp() = _storage.value?.isDecyptToTemp ?: false

    fun getMiddlePassHash() = _storage.value?.middlePassHash

    //endregion Getters

    //region Helpers

    private fun onStorageUpdated(key: String, value: Any) {
        _updateStorageField.postValue(Pair(key, value))
    }

    //endregion Helpers

}