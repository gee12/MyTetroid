package com.gee12.mytetroid.model

import com.gee12.mytetroid.data.TetroidXml
import com.gee12.mytetroid.data.crypt.TetroidCrypter
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.database.entity.StorageEntity
import com.gee12.mytetroid.database.entity.SyncProfileEntity
import java.io.File
import java.util.*

class TetroidStorage(
    name: String,
    path: String
) : StorageEntity(
    name,
    path
) {
    var quickNodeName: String? = null

    val isWriteLogToFile = 0
    val logPath: String? = null

    /**
     * Ветка для быстрой вставки.
     */
    var quicklyNode: TetroidNode? = null
        protected set

    /**
     *
     */
    var crypter: TetroidCrypter? = null
        protected set

    /**
     *
     */
    var databaseConfig: DatabaseConfig? = null
        protected set
    var xml: TetroidXml? = null
        protected set

    /**
     * Состояние
     */
    var isInited = false
    var isLoaded = false
    var isDecrypted = false
    var isCrypted = false


    constructor(name: String, path: String, isDefault: Boolean, isReadOnly: Boolean) : this(name, path) {
        val curDate = Date()
        init(curDate, curDate)
        this.isReadOnly = isReadOnly
        this.isDefault = isDefault
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