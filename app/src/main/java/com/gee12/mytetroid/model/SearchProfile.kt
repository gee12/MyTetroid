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
    val inText: Boolean = false,
    val inRecordsNames: Boolean = false,
    val inAuthor: Boolean = false,
    val inUrl: Boolean = false,

    /**
     * Поиск по меткам.
     * Тип поиска 1 - добавление в результат самих меток.
     * Тип поиска 2 - добавление в результат записей меток.
     */
    val inTags: Boolean = false,
    val inNodes: Boolean = false,
    val inFiles: Boolean = false,
    val inIds: Boolean = false,

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
    fun isInRecords() =
        inRecordsNames
                || inText
                || inAuthor
                || inUrl
                || inFiles
                || inIds
                // 2 - если при поиске по меткам добавляем в результат сами записи, а не метки
                || inTags

}