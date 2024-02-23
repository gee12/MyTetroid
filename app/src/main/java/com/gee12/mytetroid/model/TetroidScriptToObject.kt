package com.gee12.mytetroid.model

import com.gee12.mytetroid.model.enums.TetroidObjectType

data class TetroidScriptToObject(
    val id: Int = 0,
    val scriptId: Int,
    val objectId: String?,
    val objectType: TetroidObjectType?,
    var isActive: Boolean = true,
) {
    var script: TetroidScript? = null
    var obj: TetroidObject? = null

    fun isObjectFilled(): Boolean {
        return objectId != null && objectType != null
    }

}