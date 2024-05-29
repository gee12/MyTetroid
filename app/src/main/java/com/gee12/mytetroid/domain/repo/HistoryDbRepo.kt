package com.gee12.mytetroid.domain.repo

import android.content.Context
import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.database.TetroidDatabase
import com.gee12.mytetroid.database.dao.HistoryDao
import com.gee12.mytetroid.database.entity.HistoryDbEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class HistoryDbRepo(context: Context) : DbRepo() {

    private val db = TetroidDatabase.create(context)

    private val dao: HistoryDao
        get() = db.historyDao

    suspend fun getCount(storageId: Int): Either<Failure, Int> = withContext(Dispatchers.IO) {
        handleRequest {
            dao.getCount(
                storageId = storageId,
            )
        }
    }

    suspend fun getAll(storageId: Int): Either<Failure, List<HistoryDbEntity>> = withContext(Dispatchers.IO) {
        handleRequest {
            dao.getAll(
                storageId = storageId,
            )
        }
    }

    suspend fun insert(entity: HistoryDbEntity): Either<Failure, Boolean> = withContext(Dispatchers.IO) {
        handleRequest {
            entity.createdDate = Date()
            val id = dao.insert(entity)
            entity.id = id.toInt()
            id > 0
        }
    }

    suspend fun update(entity: HistoryDbEntity): Boolean = withContext(Dispatchers.IO) {
        entity.editedDate = Date()
        dao.update(entity) > 0
    }

    suspend fun delete(entity: HistoryDbEntity): Either<Failure, Boolean> = withContext(Dispatchers.IO) {
        handleRequest {
            dao.delete(
                storageId = entity.storageId,
                id = entity.id,
            ) > 0
        }
    }

    suspend fun deleteOldestItem(storageId: Int): Either<Failure, Boolean> = withContext(Dispatchers.IO) {
        handleRequest {
            dao.deleteWithMinId(
                storageId = storageId,
            ) > 0
        }
    }

    suspend fun deleteAll(storageId: Int): Either<Failure, Boolean> = withContext(Dispatchers.IO) {
        handleRequest {
            dao.deleteByStorageId(
                storageId = storageId,
            ) > 0
        }
    }

}