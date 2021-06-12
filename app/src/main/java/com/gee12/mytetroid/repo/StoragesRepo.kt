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

    suspend fun addStorage(name: String, path: String, isReadOnly: Boolean) = withContext(Dispatchers.IO) {
        dataBase.storagesDao.insert(toStorageEntity(TetroidStorage(0, name, path, isReadOnly)))
    }

    suspend fun updateStorage(id: Int, name: String, path: String, isReadOnly: Boolean) = withContext(Dispatchers.IO) {
        dataBase.storagesDao.update(toStorageEntity(TetroidStorage(id, name, path, isReadOnly)))
    }

    private fun toStorage(entity: StorageEntity) = TetroidStorage(
        entity.id.toInt(),
        entity.name,
        entity.path,
        entity.isReadOnly
    )

    private fun toStorageEntity(storage: TetroidStorage) = StorageEntity(
        id = storage.id.toLong(),
        name = storage.name,
        path = storage.path,
        isReadOnly = storage.isReadOnly
    )
}