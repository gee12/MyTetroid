package com.gee12.mytetroid.domain.usecase.tag

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.TetroidTag

/**
 * Поиск метки по наименованию.
 */
class GetTagByNameUseCase(
    private val storageProvider: IStorageProvider,
) : UseCase<TetroidTag, GetTagByNameUseCase.Params>() {

    data class Params(
        val name: String,
    )

    suspend fun run(name: String): Either<Failure, TetroidTag> {
        return run(Params(name))
    }

    override suspend fun run(params: Params): Either<Failure, TetroidTag> {
        val tagName = params.name

        if (tagName.isEmpty()) {
            return Failure.Tag.NameIsEmpty.toLeft()
        }
        val lowerCaseTagName = tagName.lowercase()
        for ((key, value) in storageProvider.getTagsMap().entries) {
            if (key.contentEquals(lowerCaseTagName, ignoreCase = true)) {
                return value.toRight()
            }
        }
        return Failure.Tag.NotFoundByName(name = tagName).toLeft()
    }

}