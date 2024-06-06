package com.gee12.mytetroid.model.obj

import com.gee12.mytetroid.model.enums.TetroidObjectType

/**
 * Общий интерфейс для объектов всех типов в хранилище.
 */
interface ITetroidObject {
    val id: String
    val type: TetroidObjectType
    val name: String
    val sourceName: String
    val isEncrypted: Boolean
    val isDecrypted: Boolean
}
