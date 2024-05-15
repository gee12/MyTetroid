package com.gee12.mytetroid.database.dao

import androidx.room.*
import com.gee12.mytetroid.database.entity.ScriptToObjectDbEntity

@Dao
interface ScriptsToObjectsDao {

    @Query(
        """
        SELECT *
        FROM scriptsToObjects 
        WHERE scriptId = :scriptId 
            AND IFNULL(objectTypeId, 0) = :objectTypeId 
            AND IFNULL(objectId, '') = :objectId 
        GROUP BY scriptId 
        ORDER BY objectTypeId, objectId
    """
    )
    fun getAll(
        scriptId: Int,
        objectTypeId: Int,
        objectId: String,
    ): List<ScriptToObjectDbEntity>

    @Query(
        """
        SELECT *
        FROM scriptsToObjects 
        WHERE scriptId = :scriptId 
        ORDER BY objectTypeId, objectId
    """
    )
    fun getAllByScriptId(scriptId: Int): List<ScriptToObjectDbEntity>

    @Query("SELECT COUNT(1) FROM scriptsToObjects WHERE scriptId = :scriptId")
    fun getCount(scriptId: Int): Int

    @Query("SELECT * FROM scriptsToObjects WHERE id = :id AND scriptId = :scriptId")
    fun getById(id: Int, scriptId: Int): ScriptToObjectDbEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: ScriptToObjectDbEntity): Long

    @Update
    fun update(entity: ScriptToObjectDbEntity): Int

    @Delete
    fun delete(entity: ScriptToObjectDbEntity): Int

    @Query(
        """
        DELETE FROM scriptsToObjects 
        WHERE scriptId = :scriptId
            AND IFNULL(objectTypeId, 0) = :objectTypeId 
            AND IFNULL(objectId, '') = :objectId 
        """
    )
    fun delete(
        scriptId: Int,
        objectTypeId: Int,
        objectId: String,
    ): Int

    @Query(
        """
        DELETE FROM scriptsToObjects 
        WHERE IFNULL(objectTypeId, 0) = :objectTypeId 
            AND IFNULL(objectId, '') = :objectId 
        """
    )
    fun deleteByObject(
        objectTypeId: Int,
        objectId: String,
    ): Int

    @Query("DELETE FROM scriptsToObjects WHERE scriptId = :scriptId")
    fun deleteByScriptId(scriptId: Int): Int

    @Query("DELETE FROM scriptsToObjects WHERE id = :id")
    fun deleteById(id: Int): Int

}