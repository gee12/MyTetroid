package com.gee12.mytetroid.database.dao

import androidx.room.*
import com.gee12.mytetroid.database.entity.HistoryDbEntity

@Dao
interface HistoryDao {

    @Query(
        """
        SELECT *
        FROM history 
        WHERE storageId = :storageId 
        ORDER BY id DESC
        """
    )
    fun getAll(storageId: Int): List<HistoryDbEntity>

    @Query("SELECT COUNT(1) FROM history WHERE storageId = :storageId")
    fun getCount(storageId: Int): Int

    @Query("SELECT * FROM history WHERE id = :id")
    fun getById(id: Int): HistoryDbEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: HistoryDbEntity): Long

    @Update
    fun update(entity: HistoryDbEntity): Int

    @Query("DELETE FROM history WHERE storageId = :storageId")
    fun deleteByStorageId(storageId: Int): Int

    @Query(
        """
        DELETE FROM history 
        WHERE id = (SELECT MIN(id) FROM history WHERE storageId = :storageId)
        """
    )
    fun deleteWithMinId(storageId: Int): Int

    @Query("DELETE FROM history WHERE storageId = :storageId AND id = :id")
    fun delete(storageId: Int, id: Int): Int

}