package com.gee12.mytetroid.domain.repo

import android.content.Context
import com.gee12.mytetroid.common.extensions.orZero
import com.gee12.mytetroid.database.TetroidDatabase
import com.gee12.mytetroid.database.entity.ScriptToObjectDbEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class ScriptsToObjectsDbRepo(context: Context) : DbRepo() {

    private val dataBase = TetroidDatabase.create(context)

    suspend fun getAllByScriptId(scriptId: Int): List<ScriptToObjectDbEntity> = withContext(Dispatchers.IO) {
        dataBase.scriptsToObjectsDao.getAllByScriptId(
            scriptId = scriptId,
        )
    }

    suspend fun getAll(
        scriptId: Int,
        objectTypeId: Int?,
        objectId: String?,
    ): List<ScriptToObjectDbEntity> = withContext(Dispatchers.IO) {
        dataBase.scriptsToObjectsDao.getAll(
            scriptId = scriptId,
            objectTypeId = objectTypeId.orZero(),
            objectId = objectId.orEmpty(),
        )
    }

    suspend fun insert(entity: ScriptToObjectDbEntity): Boolean = withContext(Dispatchers.IO) {
        entity.createdDate = Date()
        val id = dataBase.scriptsToObjectsDao.insert(entity)
        entity.id = id.toInt()
        id > 0
    }

    suspend fun deleteByScriptId(scriptId: Int): Boolean = withContext(Dispatchers.IO) {
        dataBase.scriptsToObjectsDao.deleteByScriptId(scriptId) > 0
    }

    suspend fun deleteById(id: Int): Boolean = withContext(Dispatchers.IO) {
        dataBase.scriptsToObjectsDao.deleteById(id) > 0
    }

    suspend fun delete(
        scriptId: Int,
        objectTypeId: Int?,
        objectId: String?,
    ): Boolean = withContext(Dispatchers.IO) {
        dataBase.scriptsToObjectsDao.delete(
            scriptId = scriptId,
            objectTypeId = objectTypeId.orZero(),
            objectId = objectId.orEmpty(),
        ) > 0
    }

}