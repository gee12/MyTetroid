package com.gee12.mytetroid.model

import android.os.Parcelable
import com.gee12.mytetroid.database.entity.StorageDbEntity
import kotlinx.parcelize.Parcelize


// TODO: отделить от DbEntity
@Parcelize
class TetroidStorage(
    override var name: String,
    override var uri: String,
    var isNew: Boolean = false,
    var error: String? = null,

    var isInited: Boolean = false, // загружены ли служебные файлы хранилища
    var isLoaded: Boolean = false, // загружено ли дерево веток хранилища
    var isEncrypted: Boolean = false, // зашифровано ли хранилище
    var isDecrypted: Boolean = false, // расшифровано ли хранилище (на время сеанса)
) : StorageDbEntity(
    name = name,
    uri = uri,
), Parcelable {

    constructor(name: String, uri: String, isDefault: Boolean, isReadOnly: Boolean, isNew: Boolean)
            : this(name, uri) {
        this.isReadOnly = isReadOnly
        this.isDefault = isDefault
        this.isNew = isNew
    }

    fun resetFields(src: TetroidStorage): TetroidStorage {
        return src.also {
            it.isNew = isNew
            it.error = error
            it.isInited = isInited
            it.isLoaded = isLoaded
            it.isEncrypted = isEncrypted
            it.isDecrypted = isDecrypted
        }
    }

}