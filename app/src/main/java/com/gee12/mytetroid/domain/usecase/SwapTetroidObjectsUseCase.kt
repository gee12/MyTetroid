package com.gee12.mytetroid.domain.usecase

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase
import java.util.*

/**
 * Замена местами 2 элемента в списке.
 * @return true - успешно, false - перемещение невозможно (крайний элемент и through=false)
 */
class SwapTetroidObjectsUseCase(
    private val saveStorageUseCase: SaveStorageUseCase,
) : UseCase<Boolean, SwapTetroidObjectsUseCase.Params>() {

    data class Params(
        val list: List<*>,
        val position: Int,
        val isUp: Boolean,
        val through: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, Boolean> {
        val list = params.list
        val pos = params.position
        val isUp = params.isUp
        val through = params.through
        val size = list.size

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
                Collections.swap(list, pos, newPos)

                saveStorageUseCase.run()
                    .map { true }
            } else {
                false.toRight()
            }
        } catch (ex: Exception) {
            Failure.UnknownError(ex).toLeft()
        }
    }

}