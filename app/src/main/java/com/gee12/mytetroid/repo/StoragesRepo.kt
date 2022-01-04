package com.gee12.mytetroid.repo

import android.content.Context
import com.gee12.mytetroid.database.TetroidDatabase
import com.gee12.mytetroid.database.entity.StorageEntity
import com.gee12.mytetroid.model.TetroidStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StoragesRepo(context: Context) {
    val dataBase = TetroidDatabase.create(context)

    suspend fun getStorages() = withContext(Dispatchers.IO) {
        dataBase.storagesDao.getAll().map(::toTetroidStorage)
    }

    suspend fun getStoragesCount() = withContext(Dispatchers.IO) {
        dataBase.storagesDao.getCount()
    }

    suspend fun getDefaultStorage() = withContext(Dispatchers.IO) {
        dataBase.storagesDao.getDefaultStorage().firstOrNull()?.let {
            toTetroidStorage(it)
        }
    }

    suspend fun getStorage(id: Int) = withContext(Dispatchers.IO) {
        dataBase.storagesDao.getById(id)?.let {
            toTetroidStorage(it)
        }
    }

    suspend fun addStorage(storage: TetroidStorage) = withContext(Dispatchers.IO) {
        val id = if (storage.isDefault) {
            dataBase.storagesDao.insertDefault(storage)
        } else {
            dataBase.storagesDao.insert(storage)
        }
        storage.id = id.toInt()
        id > 0
    }

    suspend fun updateStorage(storage: TetroidStorage) = withContext(Dispatchers.IO) {
        val id = if (storage.isDefault) {
            dataBase.storagesDao.updateDefault(storage)
        } else {
            dataBase.storagesDao.update(storage)
        }
        id > 0
    }

    suspend fun setIsDefault(storage: TetroidStorage) = withContext(Dispatchers.IO) {
        dataBase.storagesDao.setIsDefault(storage.id) > 0
    }

    suspend fun deleteStorage(storage: TetroidStorage) = withContext(Dispatchers.IO) {
        dataBase.storagesDao.deleteById(storage.id) > 0
    }

    private fun toTetroidStorage(entity: StorageEntity) = TetroidStorage(
        entity.name,
        entity.path,
        entity.isDefault,
        entity.isReadOnly,
        false
    ).apply {
        id = entity.id
        order = entity.order
        trashPath = entity.trashPath
        isClearTrashBeforeExit = entity.isClearTrashBeforeExit
        isAskBeforeClearTrashBeforeExit = entity.isAskBeforeClearTrashBeforeExit
        quickNodeId = entity.quickNodeId
        isLoadFavoritesOnly = entity.isLoadFavoritesOnly
        isKeepLastNode = entity.isKeepLastNode
        lastNodeId = entity.lastNodeId
        isSavePassLocal = entity.isSavePassLocal
        middlePassHash = entity.middlePassHash
        isDecyptToTemp = entity.isDecyptToTemp
        syncProfile = entity.syncProfile
        createdDate = entity.createdDate
        editedDate = entity.editedDate
    }
}