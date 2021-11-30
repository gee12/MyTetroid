package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.data.crypt.IRecordFileCrypter
import com.gee12.mytetroid.data.crypt.TetroidCrypter
import com.gee12.mytetroid.data.settings.TetroidPreferenceDataStore
import com.gee12.mytetroid.data.xml.IStorageLoadHelper
import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.logs.TetroidLogger
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.model.TetroidTag
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*

abstract class StorageSettingsViewModel(
    app: Application,
    /*logger: TetroidLogger?,*/
    val storagesRepo: StoragesRepo,
    val xmlLoader: TetroidXml,
    crypter: TetroidCrypter?
) : BaseStorageViewModel(
    app/*,
    logger*/
), IStorageLoadHelper, IRecordFileCrypter {

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

    var crypter = crypter ?: TetroidCrypter(this.logger, tagsParser = this, recordFileCrypter = this)

    val dataInteractor = DataInteractor(this.logger)
    val storageInteractor = StorageInteractor(this.logger, storageHelper = this, xmlLoader, dataInteractor)
    val interactionInteractor =  InteractionInteractor(this.logger)
    val cryptInteractor = EncryptionInteractor(this.logger, this.crypter, xmlLoader, storageHelper = this)
    val recordsInteractor = RecordsInteractor(this.logger, storageInteractor, cryptInteractor, dataInteractor, interactionInteractor, tagsParser = this, xmlLoader)
    val nodesInteractor = NodesInteractor(this.logger, storageInteractor, cryptInteractor, dataInteractor, recordsInteractor, storageHelper = this, xmlLoader)
    val tagsInteractor = TagsInteractor(this.logger, storageInteractor, xmlLoader)
    val attachesInteractor = AttachesInteractor(this.logger, storageInteractor, cryptInteractor, dataInteractor, interactionInteractor, recordsInteractor)
    val syncInteractor =  SyncInteractor(this.logger)

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

    fun updateStorage(storage: TetroidStorage) {
        launch {
            if (storagesRepo.updateStorage(storage)) {
            }
        }
    }

    fun updateStorage() {
        storage?.let {
            updateStorage(it)
        }
    }

    //region IStorageLoadHelper

    override fun decryptNode(context: Context, node: TetroidNode?): Boolean {
        //FIXME: переписать TetroidXml на kotlin и убрать runBlocking()
        return runBlocking(Dispatchers.IO) {
            crypter.decryptNode(
                context = context,
                node = node,
                decryptSubNodes = false,
                decryptRecords = false,
                iconLoader = this@StorageSettingsViewModel,
                dropCrypt = false,
                decryptFiles = false
            )
        }
    }

    override fun decryptRecord(context: Context, record: TetroidRecord?): Boolean {
        //FIXME: переписать TetroidXml на kotlin и убрать runBlocking()
        return runBlocking(Dispatchers.IO) {
            crypter.decryptRecordAndFiles(
                context = context,
                record = record,
                dropCrypt = false,
                decryptFiles = false
            )
        }
    }

    override fun isRecordFavorite(id: String?): Boolean {
        // TODO: в Interactor
        return FavoritesManager.isFavorite(id)
    }

    override fun addRecordFavorite(record: TetroidRecord?) {
        // TODO: в Interactor
        FavoritesManager.set(record)
    }

    override fun createDefaultNode(): Boolean {
        return runBlocking {
            nodesInteractor.createNode(getContext(), getString(R.string.title_first_node), TetroidXml.ROOT_NODE) != null
        }
    }

    /**
     * Разбор строки с метками и добавление каждой метки в запись и в дерево.
     * @param record
     * @param tagsString Строка с метками (не зашифрована).
     * Передается отдельно, т.к. поле в записи может быть зашифровано.
     */
    override fun parseRecordTags(record: TetroidRecord?, tagsString: String) {
        if (record == null || TextUtils.isEmpty(tagsString)) return

        for (tagName in tagsString.split(TetroidXml.TAGS_SEPAR.toRegex()).toTypedArray()) {
            val lowerCaseTagName = tagName.lowercase(Locale.getDefault())
            var tag: TetroidTag
            if (xmlLoader.mTagsMap.containsKey(lowerCaseTagName)) {
                tag = xmlLoader.mTagsMap.get(lowerCaseTagName) ?: continue
                // добавляем запись по метке, только если ее еще нет
                // (исправление дублирования записей по метке, если одна и та же метка
                // добавлена в запись несколько раз)
                if (!tag.records.contains(record)) {
                    tag.addRecord(record)
                }
            } else {
                val tagRecords: MutableList<TetroidRecord> = ArrayList()
                tagRecords.add(record)
                tag = TetroidTag(lowerCaseTagName, tagRecords)
                xmlLoader.mTagsMap.put(lowerCaseTagName, tag)
            }
            record.addTag(tag)
        }
    }

    /**
     * Удаление меток записи из списка.
     * @param record
     */
    override fun deleteRecordTags(record: TetroidRecord?) {
        if (record == null || record.tags.isEmpty()) return

        for (tag in record.tags) {
            val foundedTag = tagsInteractor.getTag(tag.name)
            if (foundedTag != null) {
                // удаляем запись из метки
                foundedTag.records.remove(record)
                if (foundedTag.records.isEmpty()) {
                    // удаляем саму метку из списка
                    xmlLoader.mTagsMap.remove(tag.name.lowercase(Locale.getDefault()))
                }
            }
        }
        record.tags.clear()
    }

    override fun loadIcon(context: Context, node: TetroidNode) {
        if (node.isNonCryptedOrDecrypted) {
            nodesInteractor.loadNodeIcon(node)
        }
    }

    override fun isStorageLoaded() = isLoaded()

    //endregion IStorageLoadHelper

    /**
     * Установка отдельного параметра хранилища и сохранение в бд.
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
        val nodeId = CommonSettings.getQuicklyNodeId(getContext())
        if (nodeId != null && xmlLoader.mIsStorageLoaded && !xmlLoader.mIsFavoritesMode) {
            val node = nodesInteractor.getNode(nodeId)
            // обновление значений или обнуление (если не найдено)
            CommonSettings.setQuicklyNode(getContext(), node)
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
        val trashDir = File(CommonSettings.getTrashPath(getContext()))
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

    fun getLastNodeId() = storage?.lastNodeId

    fun setLastNodeId(nodeId: String?) {
        storage?.lastNodeId = nodeId
    }

    fun isInited() = storage?.isInited ?: false

    fun isLoaded() = (storage?.isLoaded ?: false) && xmlLoader.mIsStorageLoaded

    // TODO: пока нигде не устанавливается
    //  Добавить установку или удалить поле
//    fun isCrypted() = storage?.isCrypted ?: false

    abstract fun isCrypted(): Boolean

    fun isDecrypted() = storage?.isDecrypted ?: false

    fun isNonEncryptedOrDecrypted() = !isCrypted() || isDecrypted()

//    fun isInFavoritesMode() = DataManager.isFavoritesMode()

    fun isSaveMiddlePassLocal() = storage?.isSavePassLocal ?: false

    fun isDecryptToTemp() = storage?.isDecyptToTemp ?: false

    fun getMiddlePassHash() = storage?.middlePassHash

    fun isCheckOutsideChanging() = storage?.syncProfile?.isCheckOutsideChanging ?: false

//    fun getCrypter() = cryptInteractor.crypter

    fun isNodesExist() = xmlLoader.mRootNodesList != null && xmlLoader.mRootNodesList.isNotEmpty()

    fun isLoadedFavoritesOnly() = xmlLoader.mIsFavoritesMode

    fun getRootNodes(): List<TetroidNode> = xmlLoader.mRootNodesList ?: emptyList()

    fun getTags(): Map<String,TetroidTag> = xmlLoader.mTagsMap

    //endregion Getters

    // region Setters

    fun setIsDecrypted(value: Boolean) {
        storage?.isDecrypted = value
    }

    //endregion Setters

    //region Helpers

    private fun onStorageUpdated(key: String, value: Any) {
        _updateStorageField.postValue(Pair(key, value))
    }

    //endregion Helpers

    //region IStorageCallback

    suspend fun saveStorage(): Boolean {
        return storageInteractor.saveStorage(getContext())
    }

    fun getPathToRecordFolder(record: TetroidRecord): String {
        return recordsInteractor.getPathToRecordFolder(record)
    }

    //endregion IStorageCallback

}
