package com.gee12.mytetroid.usecase.tag

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.data.xml.StorageDataXmlProcessor
import com.gee12.mytetroid.helpers.IStorageProvider
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidTag
import java.util.*
import kotlin.collections.ArrayList

/**
 * Разбираем строку с метками записи и добавляем метки в запись и в дерево.
 * @param record Запись.
 * @param tagsString Строка с метками (не зашифрована). Передается отдельно, т.к. поле в самой записи может быть зашифровано.
 */
class ParseRecordTagsUseCase(
    private val storageProvider: IStorageProvider,
) : UseCase<UseCase.None, ParseRecordTagsUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val tagsString: String,
//        val tagsMap: HashMap<String, TetroidTag>,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val record = params.record
        val tagsString = params.tagsString
//        val tagsMap = params.tagsMap
        val tagsMap = storageProvider.getTagsMap()

        for (tagName in tagsString.split(StorageDataXmlProcessor.TAGS_SEPAR.toRegex()).toTypedArray()) {
            val lowerCaseTagName = tagName.lowercase(Locale.getDefault())
            var tag: TetroidTag
            if (tagsMap.containsKey(lowerCaseTagName)) {
                tag = tagsMap.get(lowerCaseTagName) ?: continue
                // добавляем запись по метке, только если ее еще нет
                // (исправление дублирования записей по метке, если одна и та же метка
                // добавлена в запись несколько раз)
                if (!tag.records.contains(record)) {
                    tag.addRecord(record)
                }
            } else {
                val tagRecords: MutableList<TetroidRecord> = ArrayList()
                tagRecords.add(record)
                tag = TetroidTag(lowerCaseTagName, tagRecords)
                tagsMap.put(lowerCaseTagName, tag)
            }
            record.addTag(tag)
        }

        return None.toRight()
    }

}