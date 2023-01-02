package com.gee12.mytetroid.domain.manager

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.TetroidFavorite
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.domain.repo.FavoritesRepo
import com.gee12.mytetroid.domain.usecase.SwapFavoriteRecordsUseCase

/**
 * Работа с избранными записями.
 */
class FavoritesManager(
    private val favoritesRepo: FavoritesRepo,
    private val storageProvider: IStorageProvider,
    private val swapFavoriteRecordsUseCase: SwapFavoriteRecordsUseCase,
) {

    companion object {
        val FAVORITES_NODE = TetroidNode("FAVORITES_NODE", "", 0)
    }

    private val storageId: Int
        get() = storageProvider.storage?.id ?: 0

    private val favorites: MutableList<TetroidFavorite>
        get() = storageProvider.favorites

    suspend fun initIfNeed() {
        if (favorites.isEmpty() || favorites.any { it.storageId != storageId }) {
            init()
        }
    }

    suspend fun init() {
        reset()
        favorites.addAll(favoritesRepo.getFavorites(storageId))
    }

    fun reset() {
        favorites.clear()
    }

    fun getFavoriteRecords(): List<TetroidRecord> {
        return favorites.sortedBy { it.order }.mapNotNull { it.obj as? TetroidRecord }
    }

    private suspend fun addFavorite(favorite: TetroidFavorite): Boolean {
        return favoritesRepo.addFavorite(favorite, true)
            .also {
                if (it) favorites.add(favorite)
            }
    }

    private suspend fun addFavorite(record: TetroidRecord): Boolean {
        return addFavorite(storageId, record)
    }

    suspend fun addFavorite(storageId: Int, record: TetroidRecord): Boolean {
        return addFavorite(
            TetroidFavorite(
                storageId = storageId,
                record = record
            )
        )
    }

    suspend fun addFavorite(storageId: Int, recordId: String): Boolean {
        return addFavorite(
            TetroidFavorite(
                storageId = storageId,
                objectId = recordId
            )
        )
    }

    private suspend fun updateOrder(favorite: TetroidFavorite): Boolean {
        return favoritesRepo.updateOrder(favorite)
    }

    private suspend fun deleteFavorite(record: TetroidRecord): Boolean {
        return favoritesRepo.deleteFavorite(storageId, record.id)
            .also {
                if (it) favorites.removeAll { it.objectId == record.id }
            }
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
    suspend fun add(record: TetroidRecord): Boolean {
        return addFavorite(record)
            .also {
                if (it) record.setIsFavorite(true)
            }
    }

    /**
     * Удаление записи из избранного.
     * @param resetFlag Нужно ли сбрасывать флаг isFavorite у записи
     */
    suspend fun remove(record: TetroidRecord, resetFlag: Boolean): Boolean {
        return deleteFavorite(record)
            .also {
                if (it && resetFlag) {
                    record.setIsFavorite(false)
                }
            }
    }

    suspend fun addOrRemoveIfNeed(record: TetroidRecord, isNeedFavorite: Boolean): Boolean {
        val isAlreadyFavorite = isFavorite(record.id)
        return if (isNeedFavorite) {
            if (isAlreadyFavorite) true
            else add(record)
        } else {
            if (!isAlreadyFavorite) true
            else remove(record, true)
        }
    }

    suspend fun swap(pos: Int, isUp: Boolean, through: Boolean): Either<Failure, Boolean> {
        return swapFavoriteRecordsUseCase.run(
            SwapFavoriteRecordsUseCase.Params(
                favorites = favorites.sortedBy { it.order }.filter { it.obj != null }.toMutableList(),
                storageId = storageId,
                position = pos,
                isUp = isUp,
                through = through,
            )
        )
    }

}