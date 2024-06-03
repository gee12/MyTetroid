package com.gee12.mytetroid.database.query.history

import com.gee12.mytetroid.common.extensions.appendTo
import com.gee12.mytetroid.common.extensions.appendToIf
import com.gee12.mytetroid.common.extensions.toSql
import com.gee12.mytetroid.common.extensions.toSqlLike
import com.gee12.mytetroid.database.query.DbQuery
import com.gee12.mytetroid.model.enums.HistorySortMode

class GetAllHistoryItemsQuery(
    private val storageId: Int,
    private val sortMode: HistorySortMode,
    private val filterBy: String?,
) : DbQuery {

    override fun getQuery(): String {
        val orderBy = when (sortMode) {
            HistorySortMode.DATE_ASC -> "createdDate ASC"
            HistorySortMode.DATE_DESC -> "createdDate DESC"
            HistorySortMode.NAME_ASC -> "name ASC"
            HistorySortMode.NAME_DESC -> "name DESC"
            HistorySortMode.TYPE_ASC -> "type_id ASC"
            HistorySortMode.TYPE_DESC -> "type_id DESC"
        }

        return buildString {
         """|SELECT *
            |FROM history
            |WHERE storageId = ${storageId.toSql()}""".appendTo(this)
         """|   AND (name LIKE ${filterBy.toSqlLike()}
            |       OR object_id LIKE ${filterBy.toSqlLike()})
         """.appendToIf(this, !filterBy.isNullOrEmpty())
         """|ORDER BY $orderBy""".appendTo(this)
        }
    }

}