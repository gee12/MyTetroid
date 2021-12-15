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
    var isInited = false // загружены ли служебные файлы хранилища
    var isLoaded = false // загружено ли дерево веток хранилища
    var isCrypted = false // зашифровано ли хранилище
    var isDecrypted = false // расшифровано ли хранилище (на время сеанса)


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

    private fun init(created: Date, edited: Date) {
        createdDate = created
        editedDate = edited
    }

    fun getCopy(src: TetroidStorage): TetroidStorage {
        return src.also {
            it.isInited = isInited
            it.isLoaded = isLoaded
            it.isCrypted = isCrypted
            it.isDecrypted = isDecrypted
            it.isNew = isNew
            it.quicklyNode = quicklyNode
        }
    }

    companion object {
        fun getFolderNameFromPath(path: String): String {
            return File(path).name
        }
    }
}