package com.gee12.mytetroid.model

import java.util.Date

data class TetroidScript(
    val storageId: Int,
    var id: Int? = null,
    var fileName: String,
    var description: String?,
    var order: Int = 0,
    var error: String? = null,
    var createdDate: Date? = null,
    var editedDate: Date? = null,
    var objects: List<TetroidScriptToObject>? = null,
    var isEnabled: Boolean = false,
) {

    // Скрипт можно активировать/деактивировать, если:
    //  1) данные скрипты для всего хранилища (currentObject == null)
    //  2) или скрипт включен для текущего объекта
    //  3) или скрипт НЕ включен для какого либо из родительских объектов
    //    (т.е. если скрипт ВКЛЮЧЕН для какого-либо родительского объекта, то его НЕЛЬЗЯ отключить для текущего объекта)
    fun isCanChangeEnabled(obj: ITetroidObject?): Boolean {
        val isEnabledForCurrentObject = getScriptObject(obj)?.isEnabled ?: false
        return obj == null || !isEnabled || isEnabledForCurrentObject
    }

    fun getObjects(obj: ITetroidObject?): List<TetroidScriptToObject>? {
        return objects?.filterNot {
            obj == null && it.objectId == null && it.objectType == null
                    || obj != null && it.objectId == obj.id && it.objectType?.id == obj.type
        }
    }

    fun getScriptObject(obj: ITetroidObject?): TetroidScriptToObject? {
        return objects?.firstOrNull { it.objectId == obj?.id && it.objectType?.id == obj?.type }
    }

    fun isEnabledForObject(obj: ITetroidObject?): Boolean {
        return objects?.any {
            it.objectId == obj?.id
                    && it.objectType?.id == obj?.type
                    && it.isEnabled
        } ?: false
    }

}