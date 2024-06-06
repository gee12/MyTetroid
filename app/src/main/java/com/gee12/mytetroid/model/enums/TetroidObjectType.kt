package com.gee12.mytetroid.model.enums

import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.FoundType

enum class TetroidObjectType(val id: Int) {
    NONE(FoundType.TYPE_NONE),
    RECORD(FoundType.TYPE_RECORD),
    NODE(FoundType.TYPE_NODE),
    ATTACH(FoundType.TYPE_FILE),
    TAG(FoundType.TYPE_TAG),
    IMAGE(FoundType.TYPE_IMAGE);

    fun getPrefix(): String? {
        return when (this) {
            RECORD -> "note"
            NODE -> "branch"
            ATTACH -> "file"
            TAG -> "tag"
            else -> null
        }
    }

    fun getTypeName(resourcesProvider: IResourcesProvider): String {
        return resourcesProvider.getString(
            when (this) {
                NONE -> R.string.enum_tetroid_object_type_none
                RECORD -> R.string.enum_tetroid_object_type_record
                NODE -> R.string.enum_tetroid_object_type_node
                ATTACH -> R.string.enum_tetroid_object_type_attach
                TAG -> R.string.enum_tetroid_object_type_tag
                IMAGE -> R.string.enum_tetroid_object_type_image
            }
        )
    }

    fun getTypeNameForAction(resourcesProvider: IResourcesProvider): String {
        return resourcesProvider.getString(
            when (this) {
                NONE -> R.string.enum_tetroid_object_action_none
                RECORD -> R.string.enum_tetroid_object_action_record
                NODE -> R.string.enum_tetroid_object_action_node
                ATTACH -> R.string.enum_tetroid_object_action_attach
                TAG -> R.string.enum_tetroid_object_action_tag
                IMAGE -> R.string.enum_tetroid_object_action_image
            }
        )
    }

    companion object {
        fun getById(id: Int) = values().firstOrNull { it.id == id }
    }
}