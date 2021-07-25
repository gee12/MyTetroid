package com.gee12.mytetroid.model

import com.gee12.mytetroid.database.entity.StorageEntity
import java.io.File
import java.util.*

class TetroidStorage(
    name: String,
    path: String
) : StorageEntity(
    name,
    path
) {
    var isNew = false

//    val isWriteLogToFile = 0
//    val logPath: String? = null

    /**
     * Ветка для быстрой вставки.
     */
    var quicklyNode: TetroidNode? = null

    /**
     * Состояние
     */
    var isInited = false
    var isLoaded = false
    var isDecrypted = false
    var isCrypted = false


    constructor(name: String, path: String, isDefault: Boolean, isReadOnly: Boolean, isNew: Boolean)
            : this(name, path) {
        val curDate = Date()
        init(curDate, curDate)
        this.isReadOnly = isReadOnly
        this.isDefault = isDefault
        this.isNew = isNew
    }

    constructor(path: String) : this(getFolderNameFromPath(path), path) {
        val curDate = Date()
        init(curDate, curDate)
    }

//    constructor(name: String, path: String, created: Date, edited: Date) {
//        init(0, name, path, created, edited)
//    }

    private fun init(created: Date, edited: Date) {
        createdDate = created
        editedDate = edited
    }

    companion object {
        fun getFolderNameFromPath(path: String): String {
            return File(path).name
        }
    }
}