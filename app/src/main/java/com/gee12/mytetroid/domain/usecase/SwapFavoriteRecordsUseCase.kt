package com.gee12.mytetroid.domain.usecase

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.repo.FavoritesRepo
import com.gee12.mytetroid.model.TetroidFavorite

/**
 * Замена местами 2 избранных записи в списке.
 * @return true - успешно, false - перемещение невозможно (крайний элемент и through=false)
 */
class SwapFavoriteRecordsUseCase(
    private val favoritesRepo: FavoritesRepo,
) : UseCase<Boolean, SwapFavoriteRecordsUseCase.Params>() {

    data class Params(
        val favorites: MutableList<TetroidFavorite>,
        val storageId: Int,
        val position: Int,
        val isUp: Boolean,
        val through: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, Boolean> {
        val favorites = params.favorites
        val storageId = params.storageId
        val pos = params.position
        val isUp = params.isUp
        val through = params.through
        val size = favorites.size

        return try {
            var newPos: Int? = null

            if (isUp) {
                if (pos > 0 || through && pos == 0) {
                    newPos = if (through && pos == 0) size - 1 else pos - 1
                }
            } else {
                if (pos < size - 1 || through && pos == size - 1) {
                    newPos = if (through && pos == size - 1) 0 else pos + 1
                }
            }

            return if (newPos != null) {
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

                favoritesRepo.updateOrder(first)
                favoritesRepo.updateOrder(second)

                favorites.sortBy { it.order }

                true.toRight()
            } else {
                false.toRight()
            }
        } catch (ex: Exception) {
            Failure.Favorites.UnknownError(ex).toLeft()
        }
    }

}