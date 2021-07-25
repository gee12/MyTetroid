package com.gee12.mytetroid.database.dao

import androidx.room.*

/**
 * From: https://gist.github.com/florina-muntenescu/1c78858f286d196d545c038a71a3e864
 */
interface BaseDao<T> {

    /**
     * Insert an object in the database.
     *
     * @param obj the object to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(obj: T): Long

    /**
     * Insert an array of objects in the database.
     *
     * @param obj the objects to be inserted.
     */
    @Insert
    suspend fun insert(vararg obj: T): LongArray

    /**
     * Update an object from the database.
     *
     * @param obj the object to be updated
     */
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(obj: T)

    /**
     * Delete an object from the database
     *
     * @param obj the object to be deleted
     */
    @Delete
    suspend fun delete(obj: T)

}

@Transaction
suspend inline fun <reified T> BaseDao<T>.insertOrUpdate(item: T) {
    if (insert(item) != -1L) return
    update(item)
}
