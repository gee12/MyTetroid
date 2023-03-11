package com.gee12.mytetroid.model

import com.gee12.mytetroid.common.extensions.getNameFromPath
import com.gee12.mytetroid.common.extensions.uriToPath
import com.gee12.mytetroid.database.entity.StorageEntity

class TetroidStorage(
    name: String,
    uri: String
) : StorageEntity(
    name,
    uri
) {
    var isNew = false
    var error: String? = null

    /**
     * Состояние
     */
    var isInited = false // загружены ли служебные файлы хранилища
    var isLoaded = false // загружено ли дерево веток хранилища
    var isCrypted = false // зашифровано ли хранилище
    var isDecrypted = false // расшифровано ли хранилище (на время сеанса)


    constructor(name: String, uri: String, isDefault: Boolean, isReadOnly: Boolean, isNew: Boolean)
            : this(name, uri) {
        this.isReadOnly = isReadOnly
        this.isDefault = isDefault
        this.isNew = isNew
    }

    constructor(uri: String) : this(name = uri.uriToPath().getNameFromPath(), uri = uri)

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TetroidStorage) return false

        if (isNew != other.isNew) return false
        if (error != other.error) return false
        if (isInited != other.isInited) return false
        if (isLoaded != other.isLoaded) return false
        if (isCrypted != other.isCrypted) return false
        if (isDecrypted != other.isDecrypted) return false

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isNew.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + isInited.hashCode()
        result = 31 * result + isLoaded.hashCode()
        result = 31 * result + isCrypted.hashCode()
        result = 31 * result + isDecrypted.hashCode()
        return result
    }

}