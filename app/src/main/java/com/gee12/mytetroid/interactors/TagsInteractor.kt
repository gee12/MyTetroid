package com.gee12.mytetroid.interactors

import android.content.Context
import android.text.TextUtils
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.xml.TetroidXml
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidTag
import java.util.*

/**
 * Создается для конкретного хранилища.
 */
class TagsInteractor(
    private val logger: ITetroidLogger,
    private val storageInteractor: StorageInteractor,
    private val xmlLoader: TetroidXml
) {

    /**
     * Поиск объекта TetroidTag в списке всех меток по ключу.
     * @param tagName Имя метки
     * @return
     */
    fun getTag(tagName: String?): TetroidTag? {
        if (TextUtils.isEmpty(tagName)) return null
        val lowerCaseTagName = tagName?.lowercase()
        for ((key, value) in xmlLoader.mTagsMap.entries) {
            if (key?.contentEquals(lowerCaseTagName) == true) {
                return value
            }
        }
        return null
    }

    /**
     * Переименование метки в записях.
     * @param tag
     * @param newName
     */
    suspend fun renameTag(context: Context, tag: TetroidTag, newName: String): Boolean {
        if (TextUtils.isEmpty(newName)) {
            logger.logError(R.string.log_tag_is_null, true)
            return false
        }
        // если новое имя метки совпадает со старым (в т.ч. и по регистру), ничего не делаем
        if (newName.contentEquals(tag.name)) {
            return true
        }
        val tagsMap: HashMap<String, TetroidTag> = xmlLoader.mTagsMap
        val lowerCaseNewName = newName.lowercase(Locale.getDefault())
        val lowerCaseOldName = tag.name.lowercase(Locale.getDefault())
        // смотрим, если есть метка с таким же названием в списке после переименования
        if (tagsMap.containsKey(lowerCaseNewName)) {
            val existsTag = tagsMap[lowerCaseNewName]
            // если новая и существующая метки отличаются только регистром
            // (т.е. по сути, в глобальном списке меток это одна и та же запись),
            //  то обновляем название метки
            if (tag === existsTag) {
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
                    if (!existsTag!!.records.contains(record)) {
                        existsTag.addRecord(record)
                        // вставляем на ту же позицию, где была старая метка
                        record.tags.add(index, existsTag)
                    }
                }
                // формируем заново tagsString у записей метки
                record.updateTagsString()
            }
            // удаляем старую метку-дубликат из общего списка,
            //  но только, если это полностью разные метки, а не отличающиеся только регистром
            if (tag !== existsTag) {
                tagsMap.remove(lowerCaseOldName)
                // FIXME: нужно ли обнулять ?
//                tag = null
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
                record.updateTagsString()
            }
        }
        return storageInteractor.saveStorage(context)
    }
}