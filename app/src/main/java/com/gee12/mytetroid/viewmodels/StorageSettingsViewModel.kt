package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

open class StorageSettingsViewModel(
    app: Application,
    protected val storagesRepo: StoragesRepo,
    val xmlLoader: TetroidXml
) : BaseStorageViewModel(app, storagesRepo), IStorageCallback, IStorageLoadHelper {

//    protected val _storage = MutableLiveData<TetroidStorage>()
//    val storage: LiveData<TetroidStorage> get() = _storage
    var storage: TetroidStorage? = null
        protected set

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

    // TODO: расшарить между разными viewModel
//    val xmlLoader = TetroidXml(this)

    val dataInteractor = DataInteractor(logger, this)
    val storageInteractor = StorageInteractor(logger, this, xmlLoader, dataInteractor)
    val interactionInteractor =  InteractionInteractor(logger)
    val cryptInteractor = EncryptionInteractor(logger, xmlLoader, this, this)
    val recordsInteractor = RecordsInteractor(logger, storageInteractor, cryptInteractor, dataInteractor, interactionInteractor, this, xmlLoader)
    val nodesInteractor = NodesInteractor(logger, storageInteractor, cryptInteractor, dataInteractor, recordsInteractor, this, xmlLoader)
    val syncInteractor =  SyncInteractor(logger)

    // FIXME: Проверить:
    var quicklyNode: TetroidNode?
        get() {
            val nodeId = storage?.quickNodeId
            if (nodeId != null && isLoaded() && !isLoadedFavoritesOnly()) {
                storage?.quicklyNode = nodesInteractor.getNode(nodeId)
                onStorageUpdated(getString(R.string.pref_key_quickly_node_id), getQuicklyNodeName())
            }
            return storage?.quicklyNode
        }
        set(value) {
            storage?.quicklyNode = value
            onStorageUpdated(getString(R.string.pref_key_quickly_node_id), getQuicklyNodeName())
        }

    open fun setStorageFromBase(id: Int) {
        launch {
            // TODO: нужно обрабатывать ошибки бд
            this@StorageSettingsViewModel.storage = withContext(Dispatchers.IO) { storagesRepo.getStorage(id) }
            setStorageEvent(Constants.StorageEvents.Changed, storage)
//            _storage.postValue(storage)

            if (xmlLoader.mIsStorageLoaded) {
                storage?.isLoaded = true
            }
            setStorageEvent(Constants.StorageEvents.Inited, storage)
        }
    }

    fun updateStorage(storage: TetroidStorage) {
        launch {
            if (storagesRepo.updateStorage(storage) > 0) {
            }
        }
    }

    /**
     *
     */
    override fun decryptNode(context: Context, node: TetroidNode?): Boolean = false

    override fun decryptRecord(context: Context, record: TetroidRecord?): Boolean = false

    override fun isRecordFavorite(id: String?): Boolean = false

    override fun addRecordFavorite(record: TetroidRecord?) {}

    override fun parseRecordTags(record: TetroidRecord?, tagsString: String) {}

    override fun deleteRecordTags(record: TetroidRecord?) {}

    override fun loadIcon(context: Context, node: TetroidNode) {}

    override fun isStorageLoaded() = isLoaded()


    /**
     *
     */
    fun updateStorageOption(key: String, value: Any) {
        storage?.apply {
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
        return storage?.let {
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
        storage?.apply {
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

    fun getStorageId() = storage?.id ?: 0

    override fun getStoragePath() = storage?.path ?: ""

    fun getStorageName() = storage?.name ?: ""

    override fun getTrashPath() = storage?.trashPath ?: ""

    fun getQuicklyNodeName() = storage?.quicklyNode?.name ?: ""

    fun getSyncProfile() = storage?.syncProfile

    fun getSyncAppName() = storage?.syncProfile?.appName ?: ""

    fun getSyncCommand() = storage?.syncProfile?.command ?: ""

    fun isDefault() = storage?.isDefault ?: false

    fun isReadOnly() = storage?.isReadOnly ?: false

    fun isLoadFavoritesOnly() = storage?.isLoadFavoritesOnly ?: false

    fun isKeepLastNode() = storage?.isKeepLastNode ?: false

    fun isInited() = storage?.isInited ?: false

    fun isLoaded() = (storage?.isLoaded ?: false) && xmlLoader.mIsStorageLoaded

    // TODO: пока нигде не устанавливается
    //  Добавить установку или удалить поле
//    fun isCrypted() = storage?.isCrypted ?: false

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

    fun isDecrypted() = storage?.isDecrypted ?: false

//    fun isInFavoritesMode() = DataManager.isFavoritesMode()

    fun isSaveMiddlePassLocal() = storage?.isSavePassLocal ?: false

    fun isDecryptToTemp() = storage?.isDecyptToTemp ?: false

    fun getMiddlePassHash() = storage?.middlePassHash

    fun isCheckOutsideChanging() = storage?.syncProfile?.isCheckOutsideChanging ?: false

    fun getCrypter() = cryptInteractor.crypter

    fun isNodesExist() = xmlLoader.mRootNodesList != null && xmlLoader.mRootNodesList.isNotEmpty()

    fun isLoadedFavoritesOnly() = xmlLoader.mIsFavoritesMode

    fun getRootNodes(): List<TetroidNode> = xmlLoader.mRootNodesList ?: emptyList()

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
        return recordsInteractor.getPathToRecordFolder(record)
    }

    //endregion IStorageCallback

}

interface IStorageCallback {
    suspend fun saveStorage(context: Context): Boolean
    fun getPathToRecordFolder(record: TetroidRecord): String
}
