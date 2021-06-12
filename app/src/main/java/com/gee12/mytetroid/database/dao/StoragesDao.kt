package com.gee12.mytetroid.database.dao

import androidx.room.*
import com.gee12.mytetroid.database.entity.StorageEntity

@Dao
interface StoragesDao {
    @Query("SELECT * FROM storages")
    fun getAll(): List<StorageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: StorageEntity): Long

    @Update
    fun update(entity: StorageEntity): Int

    @Delete
    fun delete(entity: StorageEntity): Int

    @Query("DELETE FROM storages WHERE id = :id")
    fun deleteById(id: Long): Int
}