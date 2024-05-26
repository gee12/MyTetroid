package com.gee12.mytetroid.model

import android.os.Parcelable
import com.gee12.mytetroid.model.enums.SearchInNodeMode
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

//FIXME: по-хорошему, профили поиска нужно хранить для каждого хранилища отдельно.
// Как минимум т.к. сейчас nodeId подойдет только от 1 хранилища..
@Parcelize
data class SearchProfile(
    /**
     * Запрос.
     */
    val query: String,

    /**
     * Источники поиска.
     */
    val inRecordText: Boolean = false,
    val inRecordName: Boolean = false,
    val inRecordAuthor: Boolean = false,
    val inRecordUrl: Boolean = false,

    /**
     * Поиск по меткам.
     * Тип поиска 1 - добавление в результат самих меток.
     * Тип поиска 2 - добавление в результат записей меток.
     */
    val inRecordTags: Boolean = false,
    val inNodeName: Boolean = false,
    val inAttachName: Boolean = false,
    val inObjectsId: Boolean = false,

    /**
     * Разбивать ли запрос на слова.
     */
    val isSplitToWords: Boolean = false,

    /**
     * Искать только целые слова.
     */
    val isOnlyWholeWords: Boolean = false,

    /**
     * Искать только в ветке (текущей или указанной).
     */
    val searchInNodeMode: SearchInNodeMode = SearchInNodeMode.NONE,

    /**
     * Id ветки дл поиска.

     * FIXME: Параметр зависит от хранилища
     */
    var nodeId: String? = null,

) : Parcelable {

    /**
     * Целевая ветка для поиска.
     */
    @IgnoredOnParcel
    var node: TetroidNode? = null

    // поиск по веткам, записям, реквизитам записей, файлам
    fun isSearchInRecords() =
        inRecordName
                || inRecordText
                || inRecordAuthor
                || inRecordUrl
                || inAttachName
                || inObjectsId
                // 2 - если при поиске по меткам добавляем в результат сами записи, а не метки
                || inRecordTags

}