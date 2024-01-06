package com.gee12.mytetroid.domain.usecase.tag

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidTag
import java.util.*

/**
 * Переименование метки в записях.
 */
class RenameTagInRecordsUseCase(
    private val storageProvider: IStorageProvider,
    private val saveStorageUseCase: SaveStorageUseCase,
) : UseCase<UseCase.None, RenameTagInRecordsUseCase.Params>() {

    data class Params(
        val tag: TetroidTag,
        val newName: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val tag = params.tag
        val newName = params.newName

        if (newName.isEmpty()) {
            return Failure.Tag.NameIsEmpty.toLeft()
        }
        // если новое имя метки совпадает со старым (в т.ч. и по регистру), ничего не делаем
        if (newName.contentEquals(tag.name, ignoreCase = false)) {
            return None.toRight()
        }
        val tagsMap = storageProvider.getTagsMap()
        val lowerCaseNewName = newName.lowercase(Locale.getDefault())
        val lowerCaseOldName = tag.name.lowercase(Locale.getDefault())
        // смотрим, если есть метка с таким же названием в списке после переименования
        if (tagsMap.containsKey(lowerCaseNewName)) {
            val existsTag = tagsMap[lowerCaseNewName]!!
            // если новая и существующая метки отличаются только регистром
            // (т.е. по сути, в глобальном списке меток это одна и та же запись),
            //  то обновляем название метки
            if (tag == existsTag) {
                existsTag.name = newName
            }
            // если есть, то сливаем 2 метки в одну уже имеющуюся в списке:
            //  1) уже имеющуюся используем вместо старой (только что переименованной)
            //  2) старую удаляем из общего списка
            for (record in tag.records) {
                if (tag !== existsTag) {
                    val index = record.tags.indexOf(tag)
                    // удаляем старую метку-дубликат из записей
                    record.tags.remove(tag)

                    // добавляем записи из старой метки в существующую, только если записи еще нет
                    // (исправление дублирования записей по метке, если одна и та же метка
                    // добавлена в запись несколько раз)
                    if (!existsTag.records.contains(record)) {
                        existsTag.addRecord(record)
                        // вставляем на ту же позицию, где была старая метка
                        record.tags.add(index, existsTag)
                    }
                }
                // формируем заново tagsString у записей метки
                updateTagsString(record)
            }
            // удаляем старую метку-дубликат из общего списка,
            //  но только, если это полностью разные метки, а не отличающиеся только регистром
            if (tag !== existsTag) {
                tagsMap.remove(lowerCaseOldName)
            }
        } else {
            // если название новой метки - уникально, то
            //  1) удаляем запись из общего списка по старому ключу
            tagsMap.remove(lowerCaseOldName)
            //  2) добавляем ее по-новой в общий список (по новому ключу)
            tagsMap[lowerCaseNewName] = tag

            // обновим название метки
            tag.name = newName
            // сформируем заново tagsString у записей метки
            for (record in tag.records) {
                updateTagsString(record)
            }
        }
        return saveStorageUseCase.run()
    }

    /**
     * Сформировать заново строку со метками записи.
     */
    fun updateTagsString(record: TetroidRecord) {
        record.tagsString = record.tags?.joinToString(separator = Constants.TAGS_SEPARATOR) { it.name }
    }

}