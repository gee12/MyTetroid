package com.gee12.mytetroid.database.dao

import androidx.room.*
import com.gee12.mytetroid.database.entity.StorageDbEntity

@Dao
interface StoragesDao {
    @Query("SELECT * FROM storages")
    fun getAll(): List<StorageDbEntity>

    @Query("SELECT COUNT(1) FROM storages")
    fun getCount(): Int

    @Query("SELECT * FROM storages WHERE isDefault = 1 LIMIT 1")
    fun getDefaultStorage(): List<StorageDbEntity>

    @Query("SELECT id FROM storages WHERE isDefault = 1 LIMIT 1")
    fun getDefaultStorageId(): Int

    @Query("SELECT * FROM storages WHERE id = :id")
    fun getById(id: Int): StorageDbEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: StorageDbEntity): Long

    @Transaction
    fun insertDefault(entity: StorageDbEntity): Long {
        dropIsDefault()
        return insert(entity)
    }

    @Update
    fun update(entity: StorageDbEntity): Int

    @Transaction
    fun updateDefault(entity: StorageDbEntity): Int {
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
    fun delete(entity: StorageDbEntity): Int

    @Query("DELETE FROM storages WHERE id = :id")
    fun deleteById(id: Int): Int
}