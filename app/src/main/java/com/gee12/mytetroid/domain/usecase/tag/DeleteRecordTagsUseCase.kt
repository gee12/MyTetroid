package com.gee12.mytetroid.domain.usecase.tag

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.domain.provider.IStorageProvider
import java.util.*

/**
 * Удаление меток записи из списка.
 */
class DeleteRecordTagsUseCase(
    private val storageProvider: IStorageProvider,
    private val getTagByNameUseCase: GetTagByNameUseCase,
) : UseCase<UseCase.None, DeleteRecordTagsUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val record = params.record
        val tagsMap = storageProvider.getTagsMap()

        for (tag in record.tags) {
            getTagByNameUseCase.run(tag.name)
                .onSuccess { foundedTag ->
                    // удаляем запись из метки
                    foundedTag.records.remove(record)
                    if (foundedTag.records.isEmpty()) {
                        // удаляем саму метку из списка
                        tagsMap.remove(tag.name.lowercase(Locale.getDefault()))
                    }
                }
        }
        record.tags.clear()

        return None.toRight()
    }

}