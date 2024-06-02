package com.gee12.mytetroid.database.query.history

import com.gee12.mytetroid.common.extensions.toSql
import com.gee12.mytetroid.database.query.DbQuery
import com.gee12.mytetroid.model.enums.HistorySortMode

class GetAllHistoryItemsQuery(
    private val storageId: Int,
    private val sortMode: HistorySortMode
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

        return """
            SELECT *
            FROM history
            WHERE storageId = ${storageId.toSql()}
            ORDER BY $orderBy
        """
    }

}