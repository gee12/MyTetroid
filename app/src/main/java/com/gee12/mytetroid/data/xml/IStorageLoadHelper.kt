package com.gee12.mytetroid.data.xml

import android.content.Context
import com.gee12.mytetroid.data.INodeIconLoader
import com.gee12.mytetroid.data.ITagsParser
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord

interface IStorageLoadHelper : INodeIconLoader, ITagsParser {

    /**
     * Обработчик события о необходимости расшифровки ветки (без дочерних объектов)
     * сразу после загрузки ветки из xml.
     * @param node
     * @return
     */
    fun decryptNode(context: Context, node: TetroidNode?): Boolean

    /**
     * Обработчик события о необходимости расшифровки записи (вместе с прикрепленными файлами)
     * сразу после загрузки записи из xml.
     * @param record
     * @return
     */
    fun decryptRecord(context: Context, record: TetroidRecord?): Boolean

    /**
     * Проверка является ли запись избранной.
     * @param id
     * @return
     */
    fun isRecordFavorite(id: String?): Boolean

    /**
     * Добавление записи в избранное.
     * @param record
     * @return
     */
    fun addRecordFavorite(record: TetroidRecord?)

    /**
     * Разбираем метки у незашифрованных записей ветки.
     * @param node
     */
    /*    protected void parseNodeTags(TetroidNode node) {
        for (TetroidRecord record : node.getRecords()) {
            parseRecordTags(record, record.getTagsString());
        }
    }*/

    fun createDefaultNode(): Boolean

    fun isStorageLoaded(): Boolean

    fun getStoragePath(): String

    fun getTrashPath(): String

}