package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.data.settings.TetroidPreferenceDataStore
import com.gee12.mytetroid.data.xml.IStorageLoadHelper
import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.model.TetroidTag
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.utils.FileUtils
import kotlinx.coroutines.launch
import java.io.File

open class StorageSettingsViewModel(
    app: Application,
    private val repo: StoragesRepo
) : BaseStorageViewModel(app, repo), IStorageCallback {

    protected val _storage = MutableLiveData<TetroidStorage>()
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

    open val storageLoadHelper = StorageLoadHelper()
    val xmlLoader = TetroidXml(storageLoadHelper)

    val dataInteractor = DataInteractor(this)
    val storageInteractor = StorageInteractor(storage.value!!, logger, xmlLoader, dataInteractor)
    val interactionInteractor =  InteractionInteractor()
    val cryptInteractor = EncryptionInteractor(xmlLoader, logger, this)
    val recordsInteractor = RecordsInteractor(storageInteractor, cryptInteractor, dataInteractor, interactionInteractor, storageLoadHelper, xmlLoader)
    val nodesInteractor = NodesInteractor(storageInteractor, cryptInteractor, dataInteractor, recordsInteractor, storageLoadHelper, xmlLoader)
    val syncInteractor =  SyncInteractor()

    // FIXME: Проверить:
    var quicklyNode: TetroidNode?
        get() {
            val nodeId = storage.value?.quickNodeId
            if (nodeId != null && isLoaded() && !isLoadedFavoritesOnly()) {
                storage.value?.quicklyNode = nodesInteractor.getNode(nodeId)
                onStorageUpdated(getString(R.string.pref_key_quickly_node_id), getQuicklyNodeName())
            }
            return storage.value?.quicklyNode
        }
        set(value) {
            storage.value?.quicklyNode = value
            onStorageUpdated(getString(R.string.pref_key_quickly_node_id), getQuicklyNodeName())
        }

    open fun setStorageFromBase(id: Int) {
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

    /**
     * Актуализация ветки для быстрой вставки в дереве.
     */
    fun updateQuicklyNode() {
        val nodeId = SettingsManager.getQuicklyNodeId(getContext())
        if (nodeId != null && xmlLoader.mIsStorageLoaded && !xmlLoader.mIsFavoritesMode) {
            val node = nodesInteractor.getNode(nodeId)
            // обновление значений или обнуление (если не найдено)
            SettingsManager.setQuicklyNode(getContext(), node)
            this.quicklyNode = node
        }
    }

    fun dropSavedLocalPassHash() {
        _storage.value?.apply {
            // удаляем хэш пароля и сбрасываем галку
            middlePassHash = null
            isSavePassLocal = false
            updateStorage(this)
        }
    }

    fun clearTrashFolder(): Boolean {
        val trashDir = File(SettingsManager.getTrashPath(getContext()))
        // очищаем "буфер обмена", т.к. каталог(и) записи из корзины будут удалены
        // и нечего будет вставлять
        TetroidClipboard.clear()
        return FileUtils.clearDir(trashDir)
    }

    //region Getters

    fun getStorageId() = _storage.value?.id ?: 0

    fun getStoragePath() = _storage.value?.path ?: ""

    fun getStorageName() = _storage.value?.name ?: ""

    fun getTrashPath() = _storage.value?.trashPath ?: ""

    fun getQuicklyNodeName() = _storage.value?.quicklyNode?.name ?: ""

    fun getSyncProfile() = _storage.value?.syncProfile

    fun getSyncAppName() = _storage.value?.syncProfile?.appName ?: ""

    fun getSyncCommand() = _storage.value?.syncProfile?.command ?: ""

    fun isDefault() = _storage.value?.isDefault ?: false

    fun isReadOnly() = _storage.value?.isReadOnly ?: false

    fun isLoadFavoritesOnly() = _storage.value?.isLoadFavoritesOnly ?: false

    fun isKeepLastNode() = _storage.value?.isKeepLastNode ?: false

    fun isInited() = _storage.value?.isInited ?: false

    fun isLoaded() = _storage.value?.isLoaded ?: false

    fun isCrypted() = nodesInteractor.isExistCryptedNodes(false)
/*    fun isCrypted(): Boolean {
        if (DataManager.Instance == null || DataManager.Instance.mDatabaseConfig == null)
            return false
        var iniFlag = false
        try {
            iniFlag = DataManager.Instance.mDatabaseConfig.isCryptMode
        } catch (ex: Exception) {
            addlog(ex)
        }
        *//*return (iniFlag == 1 && instance.mIsExistCryptedNodes) ? true
                : (iniFlag != 1 && !instance.mIsExistCryptedNodes) ? false
                : (iniFlag == 1 && !instance.mIsExistCryptedNodes) ? true
                : (iniFlag == 0 && instance.mIsExistCryptedNodes) ? true : false;*//*
        return iniFlag || DataManager.Instance.mXml.mIsExistCryptedNodes
    }*/

    fun isDecrypted() = _storage.value?.isDecrypted ?: false

//    fun isInFavoritesMode() = DataManager.isFavoritesMode()

    fun isSaveMiddlePassLocal() = _storage.value?.isSavePassLocal ?: false

    fun isDecryptToTemp() = _storage.value?.isDecyptToTemp ?: false

    fun getMiddlePassHash() = _storage.value?.middlePassHash

    fun isCheckOutsideChanging() = storage.value?.syncProfile?.isCheckOutsideChanging ?: false

    fun getCrypter() = cryptInteractor.crypter

    fun isNodesExist() = xmlLoader.mRootNodesList != null && xmlLoader.mRootNodesList.isNotEmpty()

    fun isLoadedFavoritesOnly() = xmlLoader.mIsFavoritesMode

    fun getRootNodes(): List<TetroidNode> = xmlLoader.mRootNodesList

    fun getTags(): Map<String,TetroidTag> = xmlLoader.mTagsMap

    //endregion Getters

    //region Helpers

    private fun onStorageUpdated(key: String, value: Any) {
        _updateStorageField.postValue(Pair(key, value))
    }

    //endregion Helpers

    //region IStorageCallback

    override suspend fun saveStorage(context: Context): Boolean {
        return storageInteractor.saveStorage(context)
    }

    override fun getPathToRecordFolder(record: TetroidRecord): String {
        return recordsInteractor.getPathToRecordFolder(getContext(), record)
    }

    //endregion IStorageCallback

    /**
     *
     */
    open inner class StorageLoadHelper : IStorageLoadHelper {
        override fun decryptNode(context: Context, node: TetroidNode?): Boolean = false

        override fun decryptRecord(context: Context, record: TetroidRecord?): Boolean = false

        override fun isRecordFavorite(id: String?): Boolean = false

        override fun addRecordFavorite(record: TetroidRecord?) {}

        override fun parseRecordTags(record: TetroidRecord?, tagsString: String) {}

        override fun deleteRecordTags(record: TetroidRecord?) {}

        override fun loadIcon(context: Context, node: TetroidNode) {}
    }
}

interface IStorageCallback {
    suspend fun saveStorage(context: Context): Boolean
    fun getPathToRecordFolder(record: TetroidRecord): String
}
