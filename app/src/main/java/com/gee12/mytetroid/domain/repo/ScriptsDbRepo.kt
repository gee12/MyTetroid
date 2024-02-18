package com.gee12.mytetroid.domain.repo

import android.content.Context
import com.gee12.mytetroid.database.TetroidDatabase
import com.gee12.mytetroid.database.entity.ScriptDbEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class ScriptsDbRepo(context: Context) : DbRepo() {

    private val dataBase = TetroidDatabase.create(context)

    suspend fun getAll(storageId: Int): List<ScriptDbEntity> = withContext(Dispatchers.IO) {
        dataBase.scriptsDao.getAll(
            storageId = storageId,
        )
    }

    suspend fun insert(
        entity: ScriptDbEntity,
        updateOrder: Boolean = true,
    ): Boolean = withContext(Dispatchers.IO) {
        entity.createdDate = Date()
        val id = if (updateOrder) {
            dataBase.scriptsDao.insertAndSetOrder(entity)
        } else {
            dataBase.scriptsDao.insert(entity)
        }
        entity.id = id.toInt()
        id > 0
    }

    suspend fun update(entity: ScriptDbEntity): Boolean = withContext(Dispatchers.IO) {
        entity.editedDate = Date()
        dataBase.scriptsDao.update(entity) > 0
    }

    suspend fun delete(entity: ScriptDbEntity): Boolean = withContext(Dispatchers.IO) {
        dataBase.scriptsDao.delete(
            storageId = entity.storageId,
            id = entity.id,
        ) > 0
    }

    suspend fun deleteByStorageId(storageId: Int): Boolean = withContext(Dispatchers.IO) {
        dataBase.scriptsDao.deleteByStorageId(
            storageId = storageId,
        ) > 0
    }

    suspend fun getMaxOrder(storageId: Int): Int = withContext(Dispatchers.IO) {
        dataBase.scriptsDao.getMaxOrder(storageId)
    }

    suspend fun getMinOrder(storageId: Int): Int = withContext(Dispatchers.IO) {
        dataBase.scriptsDao.getMinOrder(storageId)
    }

}