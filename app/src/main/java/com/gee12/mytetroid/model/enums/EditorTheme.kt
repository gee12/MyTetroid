package com.gee12.mytetroid.model.enums

import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider

enum class EditorTheme(val id: String) {
    AS_APP_THEME("0"),
    LIGHT("1"),
    DARK("2");

    fun getString(resourcesProvider: IResourcesProvider): String {
        return resourcesProvider.getString(
            when (this) {
                AS_APP_THEME -> R.string.title_settings_editor_theme_as_app_theme
                LIGHT -> R.string.title_settings_editor_theme_light
                DARK -> R.string.title_settings_editor_theme_dark
            }
        )
    }

    companion object {
        fun getById(id: String): EditorTheme? {
            return values().firstOrNull { it.id == id }
        }
    }

}