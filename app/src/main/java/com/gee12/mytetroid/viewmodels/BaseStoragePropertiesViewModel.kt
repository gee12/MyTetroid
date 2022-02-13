package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Context
import android.text.TextUtils
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.*
import com.gee12.mytetroid.data.crypt.ITetroidCrypter
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.helpers.IStorageLoadHelper
import com.gee12.mytetroid.data.xml.StorageDataXmlProcessor
import com.gee12.mytetroid.helpers.INodeIconLoader
import com.gee12.mytetroid.helpers.IStorageHelper
import com.gee12.mytetroid.helpers.StoragePathHelper
import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.model.TetroidTag
import com.gee12.mytetroid.repo.StoragesRepo
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

abstract class BaseStoragePropertiesViewModel(
    app: Application,
    /*logger: TetroidLogger?,*/
) : BaseStorageViewModel(
    app,
    /*logger*/
) {

    var storagesRepo = StoragesRepo(app)
    abstract var storageDataProcessor: IStorageDataProcessor

    abstract var storageCrypter: ITetroidCrypter
    abstract val storageInteractor: StorageInteractor
    abstract val cryptInteractor: EncryptionInteractor
    abstract val favoritesInteractor: FavoritesInteractor
    abstract val recordsInteractor: RecordsInteractor
    abstract val nodesInteractor: NodesInteractor
    abstract val tagsInteractor: TagsInteractor
    abstract val attachesInteractor: AttachesInteractor

    var storagePathHelper = StoragePathHelper(storageProvider)
    val commonSettingsInteractor = CommonSettingsInteractor(this.logger)
    val dataInteractor = DataInteractor(this.logger)
    val settingsInteractor = CommonSettingsInteractor(this.logger)
    val interactionInteractor = InteractionInteractor(this.logger)

    val syncInteractor =  SyncInteractor(
        logger = this.logger,
        permissionInteractor = permissionInteractor
    )
    val trashInteractor = TrashInteractor(
        logger = this.logger,
        storagesRepo = storagesRepo
    )

    var quicklyNode: TetroidNode?
        get() {
            val nodeId = storage?.quickNodeId
            if (nodeId != null && isStorageLoaded() && !isLoadedFavoritesOnly()) {
                return nodesInteractor.getNode(nodeId)
            }
            return null
        }
        set(value) {
            updateStorageOption(getString(R.string.pref_key_quickly_node_id), value?.id ?: "")
        }

    open fun updateStorageOption(key: String, value: Any) {}


    fun updateStorageAsync(storage: TetroidStorage) {
        launch {
            if (!storagesRepo.updateStorage(storage)) {
                //...
            }
        }
    }

    fun updateStorageAsync() {
        storage?.let {
            updateStorageAsync(it)
        }
    }

    suspend fun updateStorage(storage: TetroidStorage): Boolean {
        return storagesRepo.updateStorage(storage)
    }

    val storageHelper: IStorageHelper = object : IStorageHelper {
        override fun getStorageId() = storage?.id ?: 0

        override fun createDefaultNode(): Boolean {
            return runBlocking {
                nodesInteractor.createNode(getContext(), getString(R.string.title_first_node), storageDataProcessor.getRootNode()) != null
            }
        }

        override fun onBeforeStorageTreeSave() {
            this@BaseStoragePropertiesViewModel.onBeforeStorageTreeSave()
        }

        override fun onStorageTreeSaved() {
            this@BaseStoragePropertiesViewModel.onStorageTreeSaved()
        }

    }

    open fun onBeforeStorageTreeSave() {
    }

    open fun onStorageTreeSaved() {
    }

    val nodeIconLoader: INodeIconLoader = object : INodeIconLoader {
        override fun loadIcon(context: Context, node: TetroidNode) {
            if (node.isNonCryptedOrDecrypted) {
                nodesInteractor.loadNodeIcon(node)
            }
        }
    }

    val tagsParser: ITagsParser = object : ITagsParser {

        /**
         * Разбор строки с метками и добавление каждой метки в запись и в дерево.
         * @param record
         * @param tagsString Строка с метками (не зашифрована).
         * Передается отдельно, т.к. поле в записи может быть зашифровано.
         */
        override fun parseRecordTags(record: TetroidRecord?, tagsString: String) {
            if (record == null || TextUtils.isEmpty(tagsString)) return

            for (tagName in tagsString.split(StorageDataXmlProcessor.TAGS_SEPAR.toRegex()).toTypedArray()) {
                val lowerCaseTagName = tagName.lowercase(Locale.getDefault())
                var tag: TetroidTag
                if (storageDataProcessor.getTagsMap().containsKey(lowerCaseTagName)) {
                    tag = storageDataProcessor.getTagsMap().get(lowerCaseTagName) ?: continue
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
                    storageDataProcessor.getTagsMap().put(lowerCaseTagName, tag)
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
                        storageDataProcessor.getTagsMap().remove(tag.name.lowercase(Locale.getDefault()))
                    }
                }
            }
            record.tags.clear()
        }

    }

    val storageLoadHelper: IStorageLoadHelper = object : IStorageLoadHelper {
        override suspend fun decryptNode(context: Context, node: TetroidNode): Boolean {
            return storageCrypter.decryptNode(
                context = context,
                node = node,
                decryptSubNodes = false,
                decryptRecords = false,
                iconLoader = nodeIconLoader,
                dropCrypt = false,
                decryptFiles = false
            )
        }

        override suspend fun decryptRecord(context: Context, record: TetroidRecord): Boolean {
            return storageCrypter.decryptRecordAndFiles(
                context = context,
                record = record,
                dropCrypt = false,
                decryptFiles = false
            )
        }

        override fun checkRecordIsFavorite(id: String): Boolean {
            return runBlocking { favoritesInteractor.isFavorite(id) }
        }

        override fun loadRecordToFavorites(record: TetroidRecord) {
            return runBlocking { favoritesInteractor.setObject(record) }
        }

    }

    fun getFavoriteRecords(): List<TetroidRecord> {
        return favoritesInteractor.getFavoriteRecords()
    }

    /**
     * Задана ли ветка для быстрой вставки в дереве.
     */
    fun isQuicklyNodeSet(): Boolean {
        return storage?.quickNodeId != null
    }

    /**
     * Актуализация ветки для быстрой вставки в дереве.
     */
    fun updateQuicklyNode() {
        val nodeId = storage?.quickNodeId
        if (nodeId != null && isStorageLoaded() && !isLoadedFavoritesOnly()) {
            this.quicklyNode = nodesInteractor.getNode(nodeId)
        }
    }

    fun dropSavedLocalPassHash() {
        storage?.apply {
            // удаляем хэш пароля и сбрасываем галку
            middlePassHash = null
            isSavePassLocal = false
            updateStorageAsync(this)
        }
    }

    fun clearTrashFolder() {
        launch {
            if (trashInteractor.clearTrashFolder(storage ?: return@launch)) {
                log(R.string.title_trash_cleared, true)
            } else {
                logError(R.string.title_trash_clear_error, true)
            }
        }
    }

    //region Migration

    fun isNeedMigration(): Boolean {
        val fromVersion = CommonSettings.getSettingsVersion(getContext())
        return (fromVersion != 0 && fromVersion < Constants.SETTINGS_VERSION_CURRENT)
    }

    //endregion Migration

    //region Getters

    fun isStorageInited() = storage?.isInited ?: false

    fun isStorageLoaded() = (storage?.isLoaded ?: false) && storageDataProcessor.isLoaded()

    abstract fun isStorageCrypted(): Boolean

    fun isStorageDecrypted() = storage?.isDecrypted ?: false

    fun isStorageNonEncryptedOrDecrypted() = !isStorageCrypted() || isStorageDecrypted()

    fun getStorageId() = storage?.id ?: 0

    fun getStoragePath() = storage?.path.orEmpty()

    fun getStorageName() = storage?.name ?: ""

    fun isStorageDefault() = storage?.isDefault ?: false

    fun isStorageReadOnly() = storage?.isReadOnly ?: false

    fun getTrashPath() = storage?.trashPath.orEmpty()

    fun getQuicklyNodeName() = quicklyNode?.name.orEmpty()

    fun getQuicklyNodeNameOrMessage(): String? {
        return if (storage?.quickNodeId != null) {
            if (!isStorageLoaded()) {
                getString(R.string.hint_need_load_storage)
            } else if (isLoadedFavoritesOnly()) {
                getString(R.string.hint_need_load_all_nodes)
            } else quicklyNode?.name
        } else null
    }

    fun getQuicklyNodeId() = quicklyNode?.id.orEmpty()

    fun getStorageSyncProfile() = storage?.syncProfile

    fun isStorageSyncEnabled() = storage?.syncProfile?.isEnabled ?: false

    fun getStorageSyncAppName() = storage?.syncProfile?.appName.orEmpty()

    fun getStorageSyncCommand() = storage?.syncProfile?.command.orEmpty()

    fun isLoadFavoritesOnly() = storage?.isLoadFavoritesOnly ?: false

    fun isKeepLastNode() = storage?.isKeepLastNode ?: false

    fun getLastNodeId() = storage?.lastNodeId

    fun isSaveMiddlePassLocal() = storage?.isSavePassLocal ?: false

    fun isDecryptToTemp() = storage?.isDecyptToTemp ?: false

    fun getMiddlePassHash() = storage?.middlePassHash

    fun isCheckOutsideChanging() = storage?.syncProfile?.isCheckOutsideChanging ?: false

    fun isNodesExist() = storageDataProcessor.getRootNodes().isNotEmpty()

    fun isLoadedFavoritesOnly() = storageDataProcessor.isLoadFavoritesOnlyMode()

    fun getRootNodes(): List<TetroidNode> = storageDataProcessor.getRootNodes()

    fun getTags(): Map<String,TetroidTag> = storageDataProcessor.getTagsMap()

    //endregion Getters

    // region Setters

    fun setIsDecrypted(value: Boolean) {
        storage?.isDecrypted = value
    }

    fun setLastNodeId(nodeId: String?) {
        storage?.lastNodeId = nodeId
    }

    //endregion Setters

    //region IStorageCallback

    suspend fun saveStorage(): Boolean {
        return storageInteractor.saveStorage(getContext())
    }

    fun getPathToRecordFolder(record: TetroidRecord): String {
        return recordsInteractor.getPathToRecordFolder(record)
    }

    //endregion IStorageCallback

}
