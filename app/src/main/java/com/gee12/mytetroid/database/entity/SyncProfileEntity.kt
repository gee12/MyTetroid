package com.gee12.mytetroid.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

open class SyncProfileEntity (
    @ColumnInfo(name = "isEnabled")
    var isEnabled: Boolean
) {

    @ColumnInfo(name = "appName")
    var appName: String = ""

    @ColumnInfo(name = "command")
    var command: String = ""

    @ColumnInfo(name = "isSyncBeforeInit")
    var isSyncBeforeInit: Boolean = true

    @ColumnInfo(name = "isSyncBeforeExit")
    var isSyncBeforeExit: Boolean = false

    @ColumnInfo(name = "isAskBeforeSyncOnInit")
    var isAskBeforeSyncOnInit: Boolean = true

    @ColumnInfo(name = "isAskBeforeSyncOnExit")
    var isAskBeforeSyncOnExit: Boolean = false

    @ColumnInfo(name = "isCheckOutsideChanging")
    var isCheckOutsideChanging: Boolean = true
}
