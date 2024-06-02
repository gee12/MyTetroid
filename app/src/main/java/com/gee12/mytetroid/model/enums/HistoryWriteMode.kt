package com.gee12.mytetroid.model.enums

import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider

enum class HistoryWriteMode(val id: Int) {
    ALL(0),
    LAST(1);

    fun getTitle(resourcesProvider: IResourcesProvider): String {
        return resourcesProvider.getString(
            when (this) {
                ALL -> R.string.title_settings_history_write_mode_all
                LAST -> R.string.title_settings_history_write_mode_last
            }
        )
    }

    fun getDescription(resourcesProvider: IResourcesProvider): String {
        return resourcesProvider.getString(
            when (this) {
                ALL -> R.string.title_settings_history_write_mode_all_summ
                LAST -> R.string.title_settings_history_write_mode_last_summ
            }
        )
    }

    companion object {
        fun getById(id: Int) = values().firstOrNull { it.id == id }
    }
}