package com.gee12.mytetroid.usecase.tag

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.helpers.IStorageProvider
import com.gee12.mytetroid.interactors.TagsInteractor
import com.gee12.mytetroid.model.TetroidRecord
import java.util.*

/**
 * Удаление меток записи из списка.
 */
class DeleteRecordTagsUseCase(
    private val storageProvider: IStorageProvider,
    private val tagsInteractor: TagsInteractor,
) : UseCase<UseCase.None, DeleteRecordTagsUseCase.Params>() {

    data class Params(
        val record: TetroidRecord?,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        // TODO: new failure
        val record = params.record ?: return Failure.ArgumentIsEmpty().toLeft()

        for (tag in record.tags) {
            val foundedTag = tagsInteractor.getTag(tag.name)
            if (foundedTag != null) {
                // удаляем запись из метки
                foundedTag.records.remove(record)
                if (foundedTag.records.isEmpty()) {
                    // удаляем саму метку из списка
                    storageProvider.getTagsMap().remove(tag.name.lowercase(Locale.getDefault()))
                }
            }
        }
        record.tags.clear()

        return None.toRight()
    }

}