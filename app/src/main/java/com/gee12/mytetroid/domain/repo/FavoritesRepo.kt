package com.gee12.mytetroid.domain.repo

import android.content.Context
import com.gee12.mytetroid.database.TetroidDatabase
import com.gee12.mytetroid.database.entity.FavoriteEntity
import com.gee12.mytetroid.model.TetroidFavorite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoritesRepo(context: Context) {
    val dataBase = TetroidDatabase.create(context)

    suspend fun getFavorites(storageId: Int) = withContext(Dispatchers.IO) {
        dataBase.favoritesDao.getAll(storageId).map(::toTetroidFavorite)
    }

    suspend fun addFavorite(favorite: TetroidFavorite, updateOrder: Boolean = true) = withContext(Dispatchers.IO) {
        val id = if (updateOrder) {
            dataBase.favoritesDao.insertAndSetOrder(favorite)
        } else {
            dataBase.favoritesDao.insert(favorite)
        }
        favorite.id = id.toInt()
        id > 0
    }

    suspend fun updateFavorite(favorite: TetroidFavorite) = withContext(Dispatchers.IO) {
        val id = dataBase.favoritesDao.update(favorite)
        id > 0
    }

    suspend fun updateOrder(favorite: TetroidFavorite) = withContext(Dispatchers.IO) {
        val id = dataBase.favoritesDao.updateOrder(favorite.storageId, favorite.objectId, favorite.order)
        id > 0
    }

    suspend fun deleteFavorite(favorite: TetroidFavorite) = withContext(Dispatchers.IO) {
        dataBase.favoritesDao.delete(favorite.storageId, favorite.objectId) > 0
    }

    suspend fun deleteFavorite(storageId: Int, recordId: String) = withContext(Dispatchers.IO) {
        dataBase.favoritesDao.delete(storageId, recordId) > 0
    }

    suspend fun isFavorite(storageId: Int, recordId: String): Boolean = withContext(Dispatchers.IO) {
        dataBase.favoritesDao.isExists(storageId, recordId)
    }

    suspend fun getMaxOrder(storageId: Int): Int = withContext(Dispatchers.IO) {
        dataBase.favoritesDao.getMaxOrder(storageId)
    }

    suspend fun getMinOrder(storageId: Int): Int = withContext(Dispatchers.IO) {
        dataBase.favoritesDao.getMinOrder(storageId)
    }

    private fun toTetroidFavorite(entity: FavoriteEntity) = TetroidFavorite(
        storageId = entity.storageId,
        objectId = entity.objectId,
        order = entity.order,
    ).apply {
        id = entity.id
        createdDate = entity.createdDate
        editedDate = entity.editedDate
    }
}