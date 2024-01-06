package com.gee12.mytetroid.database.dao

import androidx.room.*
import com.gee12.mytetroid.database.entity.FavoriteEntity

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites WHERE storageId = :storageId ORDER BY orderNum")
    fun getAll(storageId: Int): List<FavoriteEntity>

    @Query("SELECT COUNT(1) FROM favorites WHERE storageId = :storageId")
    fun getCount(storageId: Int): Int

    @Query("SELECT MIN(orderNum) FROM favorites WHERE storageId = :storageId")
    fun getMinOrder(storageId: Int): Int

    @Query("SELECT MAX(orderNum) FROM favorites WHERE storageId = :storageId")
    fun getMaxOrder(storageId: Int): Int

    @Query("SELECT * FROM favorites WHERE id = :id")
    fun getById(id: Int): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: FavoriteEntity): Long

    @Transaction
    fun insertAndSetOrder(entity: FavoriteEntity): Long {
        val newOrder = getMaxOrder(entity.storageId) + 1
        entity.order = newOrder
        return insert(entity)
    }

    @Update
    fun update(entity: FavoriteEntity): Int

    @Query("UPDATE favorites SET orderNum = :order WHERE storageId = :storageId AND objectId = :objectId")
    fun updateOrder(storageId: Int, objectId: String, order: Int): Int

    @Delete
    fun delete(entity: FavoriteEntity): Int

    @Query("DELETE FROM favorites WHERE storageId = :storageId AND objectId = :objectId")
    fun delete(storageId: Int, objectId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE storageId = :storageId AND objectId = :objectId LIMIT 1)")
    fun isExists(storageId: Int, objectId: String): Boolean
}