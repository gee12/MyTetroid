package com.gee12.mytetroid.interactors

import com.gee12.mytetroid.data.xml.IStorageLoadHelper
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidFavorite
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.repo.FavoritesRepo
import java.lang.Exception

/**
 * Создается для конкретного хранилища.
 */
class FavoritesInteractor(
    private val logger: ITetroidLogger,
    private val favoritesRepo: FavoritesRepo,
    private val storageHelper: IStorageLoadHelper
) {
    companion object {
        val FAVORITES_NODE = TetroidNode("FAVORITES_NODE", "", 0)
    }

    private val storageId: Int get() = storageHelper.getStorageId()

    private val favorites = mutableListOf<TetroidFavorite>()

    suspend fun init() {
        reset()
        favorites.addAll(favoritesRepo.getFavorites(storageId))
    }

    fun reset() {
        favorites.clear()
    }

    fun getFavoriteRecords() = favorites.mapNotNull { it.obj as? TetroidRecord }

    private suspend fun addFavorite(favorite: TetroidFavorite) = favoritesRepo.addFavorite(favorite, true)
        .also {
            if (it) favorites.add(favorite)
        }

    private suspend fun addFavorite(record: TetroidRecord) = addFavorite(storageId, record)

    suspend fun addFavorite(storageId: Int, record: TetroidRecord) = addFavorite(
        TetroidFavorite(
            storageId = storageId,
            record = record
        )
    )

    suspend fun addFavorite(storageId: Int, recordId: String) = addFavorite(
        TetroidFavorite(
            storageId = storageId,
            objectId = recordId
        )
    )

    private suspend fun updateOrder(favorite: TetroidFavorite) = favoritesRepo.updateOrder(favorite)

    private suspend fun deleteFavorite(record: TetroidRecord) = favoritesRepo.deleteFavorite(storageId, record.id)
        .also {
            if (it) favorites.removeAll { it.objectId == record.id }
        }

    /**
     * Проверка состоит ли запись в списке избранных у хранилища.
     */
    fun isFavorite(recordId: String): Boolean {
        return favorites.any { it.objectId == recordId }
    }

    /**
     * Загрузка объекта избранной записи из хранилища.
     */
    fun setObject(record: TetroidRecord): Boolean {
        return favorites.firstOrNull { it.objectId == record.id }
            ?.also {
                it.obj = record
                record.setIsFavorite(true)
            } != null
    }

    /**
     * Добавление новой записи в избранное.
     */
    suspend fun add(record: TetroidRecord) = addFavorite(record)
        .also {
            if (it) record.setIsFavorite(true)
        }

    /**
     * Удаление записи из избранного.
     * @param resetFlag Нужно ли сбрасывать флаг isFavorite у записи
     */
    suspend fun remove(record: TetroidRecord, resetFlag: Boolean) = deleteFavorite(record)
        .also {
            if (it && resetFlag) {
                record.setIsFavorite(false)
            }
        }

    suspend fun addOrRemoveIfNeed(record: TetroidRecord, isFavorite: Boolean): Boolean {
        return when {
            !isFavorite(record.id) -> {
                true
            }
            isFavorite -> {
                add(record)
            }
            else -> {
                remove(record, true)
            }
        }
    }

    /**
     * Замена местами 2 избранных записи в списке.
     * @param pos
     * @param isUp
     * @return 1 - успешно
     * 0 - перемещение невозможно (пограничный элемент)
     * -1 - ошибка
     */
    suspend fun swap(pos: Int, isUp: Boolean, through: Boolean): Int {
        try {
            var newPos: Int? = null
            val size = favorites.size
            if (isUp) {
                if (pos > 0 || through && pos == 0) {
                    newPos = if (through && pos == 0) size - 1 else pos - 1
                }
            } else {
                if (pos < size - 1 || through && pos == size - 1) {
                    newPos = if (through && pos == size - 1) 0 else pos + 1
                }
            }

            if (newPos != null) {
                val first = favorites[pos]
                val second = favorites[newPos]

                // позиции элементов в списке могут не совпадать с order записей,
                //  поэтому оперируем их order, а не pos/newPos

                // не меняем order второго элемента, если текущий перемещается в начало или конец списка
                //  (по сути весь список сдвигается вверх или вниз)
                val newOrders = when {
                    isUp && pos == 0 -> Pair(favoritesRepo.getMaxOrder(storageId) + 1, null)
                    !isUp && pos == size - 1 -> Pair(favoritesRepo.getMinOrder(storageId) - 1, null)
                    else -> Pair(second.order, first.order)
                }
                second.order = newOrders.second ?: second.order
                first.order = newOrders.first

                updateOrder(first)
                updateOrder(second)

                favorites.sortBy { it.order }

                return 1
            }
        } catch (ex: Exception) {
            return -1
        }
        return 0
    }

}