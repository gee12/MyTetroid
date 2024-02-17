package com.gee12.mytetroid.model.enums

import com.gee12.mytetroid.model.FoundType

enum class TetroidObjectType(val id: Int) {
    NONE(FoundType.TYPE_NONE),
    RECORD(FoundType.TYPE_RECORD),
    NODE(FoundType.TYPE_NODE),
    ATTACH(FoundType.TYPE_FILE),
    TAG(FoundType.TYPE_TAG);

    companion object {
        fun getById(id: Int) = values().firstOrNull { it.id == id }
    }
}