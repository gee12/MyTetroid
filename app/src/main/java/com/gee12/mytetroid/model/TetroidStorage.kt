package com.gee12.mytetroid.model

import com.gee12.mytetroid.database.entity.StorageEntity
import java.io.File

class TetroidStorage(
    name: String,
    path: String
) : StorageEntity(
    name,
    path
) {
    var isNew = false
    var error: String? = null

//    val isWriteLogToFile = 0
//    val logPath: String? = null

    /**
     * Состояние
     */
    var isInited = false // загружены ли служебные файлы хранилища
    var isLoaded = false // загружено ли дерево веток хранилища
    var isCrypted = false // зашифровано ли хранилище
    var isDecrypted = false // расшифровано ли хранилище (на время сеанса)


    constructor(name: String, path: String, isDefault: Boolean, isReadOnly: Boolean, isNew: Boolean)
            : this(name, path) {
        this.isReadOnly = isReadOnly
        this.isDefault = isDefault
        this.isNew = isNew
    }

    constructor(path: String) : this(getFolderNameFromPath(path), path) {
    }

    fun resetFields(src: TetroidStorage): TetroidStorage {
        return src.also {
            it.isNew = isNew
            it.error = error
            it.isInited = isInited
            it.isLoaded = isLoaded
            it.isCrypted = isCrypted
            it.isDecrypted = isDecrypted
        }
    }

    companion object {
        fun getFolderNameFromPath(path: String): String {
            return File(path).name
        }
    }
}