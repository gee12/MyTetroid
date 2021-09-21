package com.gee12.mytetroid.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

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
    val isSearchInNode: Boolean = false,

    /**
     * Id ветки дл поиска.
     */
    val nodeId: String? = null,

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


//    constructor(parcel: Parcel) : this(
//        query = parcel.readString() ?: "",
//        inText = parcel.readInt() == 1,
//        inRecordsNames = parcel.readInt() == 1,
//        inAuthor = parcel.readInt() == 1,
//        inUrl = parcel.readInt() == 1,
//        inTags = parcel.readInt() == 1,
//        inNodes = parcel.readInt() == 1,
//        inFiles = parcel.readInt() == 1,
//        inIds = parcel.readInt() == 1,
//        isSplitToWords = parcel.readInt() == 1,
//        isOnlyWholeWords = parcel.readInt() == 1,
//        isSearchInNode = parcel.readInt() == 1,
//        nodeId = parcel.readString()
//    )

//    constructor(query: String) : this(
//        query = query
//    )

//    override fun writeToParcel(dest: Parcel, flags: Int) {
//        dest.writeString(query)
//        dest.writeInt(if (inText) 1 else 0)
//        dest.writeInt(if (inRecordsNames) 1 else 0)
//        dest.writeInt(if (inAuthor) 1 else 0)
//        dest.writeInt(if (inUrl) 1 else 0)
//        dest.writeInt(if (inTags) 1 else 0)
//        dest.writeInt(if (inNodes) 1 else 0)
//        dest.writeInt(if (inFiles) 1 else 0)
//        dest.writeInt(if (inIds) 1 else 0)
//        dest.writeInt(if (isSplitToWords) 1 else 0)
//        dest.writeInt(if (isOnlyWholeWords) 1 else 0)
//        dest.writeInt(if (isSearchInNode) 1 else 0)
//        dest.writeString(nodeId)
//    }
//
//    override fun describeContents(): Int {
//        return 0
//    }
//
//    companion object CREATOR : Parcelable.Creator<SearchProfile> {
//        override fun createFromParcel(parcel: Parcel): SearchProfile {
//            return SearchProfile(parcel)
//        }
//
//        override fun newArray(size: Int): Array<SearchProfile?> {
//            return arrayOfNulls(size)
//        }
//    }

}