package com.gee12.mytetroid.model.enums

import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider

enum class TagsSearchMode(val id: Int) {
    OR(0),
    AND(1);

    fun getStringValue(resourcesProvider: IResourcesProvider) = resourcesProvider.getString(
        when (this) {
            OR -> R.string.action_tags_search_mode_or
            AND -> R.string.action_tags_search_mode_and
        }
    )

    companion object {
        fun getById(id: Int) = values().firstOrNull { it.id == id }
    }

}