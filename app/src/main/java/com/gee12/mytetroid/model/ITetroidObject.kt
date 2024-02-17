package com.gee12.mytetroid.model

/**
 * Общий интерфейс для объектов всех типов в хранилище.
 */
interface ITetroidObject {
    val type: Int
    var name: String
    val id: String
    val isCrypted: Boolean
}
