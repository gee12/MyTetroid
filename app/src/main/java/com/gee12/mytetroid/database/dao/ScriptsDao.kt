package com.gee12.mytetroid.database.dao

import androidx.room.*
import com.gee12.mytetroid.database.entity.ScriptDbEntity

@Dao
interface ScriptsDao {

    @Query(
        """
        SELECT *
        FROM scripts 
        WHERE storageId = :storageId 
        ORDER BY orderNum
    """
    )
    fun getAll(storageId: Int): List<ScriptDbEntity>

    @Query("SELECT COUNT(1) FROM scripts WHERE storageId = :storageId")
    fun getCount(storageId: Int): Int

    @Query("SELECT MIN(orderNum) FROM scripts WHERE storageId = :storageId")
    fun getMinOrder(storageId: Int): Int

    @Query("SELECT MAX(orderNum) FROM scripts WHERE storageId = :storageId")
    fun getMaxOrder(storageId: Int): Int

    @Query("SELECT * FROM scripts WHERE id = :id")
    fun getById(id: Int): ScriptDbEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: ScriptDbEntity): Long

    @Transaction
    fun insertAndSetOrder(entity: ScriptDbEntity): Long {
        val newOrder = getMaxOrder(entity.storageId) + 1
        entity.order = newOrder
        return insert(entity)
    }

    @Update
    fun update(entity: ScriptDbEntity): Int

    @Query("UPDATE scripts SET orderNum = :order WHERE storageId = :storageId AND id = :id")
    fun updateOrder(storageId: Int, id: Int, order: Int): Int

    @Delete
    fun delete(entity: ScriptDbEntity): Int

    @Query("DELETE FROM scripts WHERE storageId = :storageId")
    fun deleteByStorageId(storageId: Int): Int

    @Query("DELETE FROM scripts WHERE storageId = :storageId AND id = :id")
    fun delete(storageId: Int, id: Int): Int

}