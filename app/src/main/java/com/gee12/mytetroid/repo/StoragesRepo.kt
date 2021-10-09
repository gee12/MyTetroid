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
        dataBase.storagesDao.getAll().map(::toStorage)
    }

    suspend fun getDefaultStorage() = withContext(Dispatchers.IO) {
        dataBase.storagesDao.getDefaultStorage().firstOrNull()?.let {
            toStorage(it)
        }
    }

    suspend fun getStorage(id: Int) = withContext(Dispatchers.IO) {
        toStorage(dataBase.storagesDao.getById(id))
    }

    suspend fun addStorage(storage: TetroidStorage) = withContext(Dispatchers.IO) {
        if (storage.isDefault) {
            dataBase.storagesDao.insertDefault(storage)
        } else {
            dataBase.storagesDao.insert(storage)
        }
    }

    suspend fun updateStorage(storage: TetroidStorage) = withContext(Dispatchers.IO) {
        if (storage.isDefault) {
            dataBase.storagesDao.updateDefault(storage)
        } else {
            dataBase.storagesDao.update(storage)
        }
    }

    suspend fun deleteStorage(storage: TetroidStorage) = withContext(Dispatchers.IO) {
        dataBase.storagesDao.deleteById(storage.id)
    }

    /**
     * TODO: добавить остальные поля
     */
    private fun toStorage(entity: StorageEntity) = TetroidStorage(
        entity.name,
        entity.path,
        entity.isDefault,
        entity.isReadOnly,
        false
    ).apply {
        id = entity.id
        createdDate = entity.createdDate
        editedDate = entity.editedDate
        order = entity.order
        trashPath = entity.trashPath
        quickNodeId = entity.quickNodeId
        isLoadFavoritesOnly = entity.isLoadFavoritesOnly
        isKeepLastNode = entity.isKeepLastNode
        lastNodeId = entity.lastNodeId
        isSavePassLocal = entity.isSavePassLocal
        middlePassHash = entity.middlePassHash
        isDecyptToTemp = entity.isDecyptToTemp
        syncProfile = entity.syncProfile
    }
}