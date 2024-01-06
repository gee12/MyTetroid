package com.gee12.mytetroid.database.entity

import androidx.room.ColumnInfo
import java.util.*

open class BaseEntity {
    @ColumnInfo(name = "createdDate")
    var createdDate: Date? = null

    @ColumnInfo(name = "editedDate")
    var editedDate: Date? = null

    init {
        setCurrentDates()
    }

    protected fun setDates(created: Date, edited: Date) {
        createdDate = created
        editedDate = edited
    }

    protected fun setCurrentDates() {
        val curDate = Date()
        setDates(curDate, curDate)
    }

}