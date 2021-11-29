package com.gee12.mytetroid.database.dao

import androidx.room.*
import com.gee12.mytetroid.database.entity.StorageEntity

@Dao
interface StoragesDao {
    @Query("SELECT * FROM storages")
    fun getAll(): List<StorageEntity>

    @Query("SELECT * FROM storages WHERE isDefault = 1 LIMIT 1")
    fun getDefaultStorage(): List<StorageEntity>

    @Query("SELECT * FROM storages WHERE id = :id")
    fun getById(id: Int): StorageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: StorageEntity): Long

    @Transaction
    fun insertDefault(entity: StorageEntity): Long {
        dropIsDefault()
        return insert(entity)
    }

    @Update
    fun update(entity: StorageEntity): Int

    @Transaction
    fun updateDefault(entity: StorageEntity): Int {
        dropIsDefault()
        return update(entity)
    }

    @Transaction
    fun setIsDefault(id: Int): Int {
        dropIsDefault()
        return setIsDefault(id, 1)
    }

    @Query("UPDATE storages SET isDefault = :isDefault WHERE id = :id")
    fun setIsDefault(id: Int, isDefault: Int): Int

    @Query("UPDATE storages SET isDefault = 0")
    fun dropIsDefault(): Int

    @Delete
    fun delete(entity: StorageEntity): Int

    @Query("DELETE FROM storages WHERE id = :id")
    fun deleteById(id: Int): Int
}