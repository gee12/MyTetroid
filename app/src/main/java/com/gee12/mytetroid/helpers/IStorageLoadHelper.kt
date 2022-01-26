package com.gee12.mytetroid.helpers

import android.content.Context
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord

interface IStorageLoadHelper /*: *//*INodeIconLoader,*//* ITagsParser*/ {

    /**
     * Обработчик события о необходимости расшифровки ветки (без дочерних объектов)
     * сразу после загрузки ветки из xml.
     * @param node
     * @return
     */
    suspend fun decryptNode(context: Context, node: TetroidNode): Boolean

    /**
     * Обработчик события о необходимости расшифровки записи (вместе с прикрепленными файлами)
     * сразу после загрузки записи из xml.
     * @param record
     * @return
     */
    suspend fun decryptRecord(context: Context, record: TetroidRecord): Boolean

    /**
     * Проверка является ли запись избранной.
     * @param id
     * @return
     */
    fun checkRecordIsFavorite(id: String): Boolean

    /**
     * Добавление записи в избранное.
     * @param record
     * @return
     */
    fun loadRecordToFavorites(record: TetroidRecord)

}