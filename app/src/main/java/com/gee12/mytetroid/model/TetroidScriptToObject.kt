package com.gee12.mytetroid.model

import com.gee12.mytetroid.model.enums.TetroidObjectType

data class TetroidScriptToObject(
    val id: Int = 0,
    val scriptId: Int,
    val objectId: String?,
    val objectType: TetroidObjectType?,
    val objectName: String?,
    var isEnabled: Boolean = true,
) {

    fun isObjectFilled(): Boolean {
        return objectId != null && objectType != null
    }

}