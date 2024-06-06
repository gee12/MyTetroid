package com.gee12.mytetroid.model

import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.model.obj.ITetroidObject
import java.util.Date

data class TetroidScript(
    val storageId: Int,
    var id: Int? = null,
    var name: String,
    var fileName: String,
    var description: String?,
    var order: Int = 0,
    var createdDate: Date? = null,
    var editedDate: Date? = null,
    var objects: List<TetroidScriptToObject>? = null,
    var isActive: Boolean = false,
) {
    var errors: List<Failure>? = null

    fun isHasErrors(): Boolean {
        return !errors.isNullOrEmpty()
    }

    fun isActiveWithoutErrors(): Boolean {
        return isActive && !isHasErrors()
    }

    // Скрипт можно активировать/деактивировать, если:
    //  1) данные скрипты для всего хранилища (currentObject == null)
    //  2) или скрипт включен для текущего объекта
    //  3) или скрипт НЕ включен для какого либо из родительских объектов
    //    (т.е. если скрипт ВКЛЮЧЕН для какого-либо родительского объекта, то его НЕЛЬЗЯ отключить для текущего объекта)
    fun isCanSwitchActivity(obj: ITetroidObject?): Boolean {
        val isActiveForCurrentObject = getScriptObject(obj)?.isActive ?: false
        return (obj == null || !isActive || isActiveForCurrentObject)
                && !isHasErrors()
    }

    fun getObjects(obj: ITetroidObject?): List<TetroidScriptToObject>? {
        return objects?.filterNot {
            obj == null && it.objectId == null && it.objectType == null
                    || obj != null && it.objectId == obj.id && it.objectType == obj.type
        }
    }

    fun getScriptObject(obj: ITetroidObject?): TetroidScriptToObject? {
        return objects?.firstOrNull { it.objectId == obj?.id && it.objectType == obj?.type }
    }

    fun isActiveForObject(obj: ITetroidObject?): Boolean {
        return objects?.any {
            it.objectId == obj?.id
                    && it.objectType == obj?.type
                    && it.isActive
        } ?: false
    }

    fun isFileExist(): Boolean {
        return errors?.any { it is Failure.Script.FileIsNotExist }?.not() ?: true
    }

}