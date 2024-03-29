package com.gee12.mytetroid.model.enums

import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider

enum class AppTheme(val id: String) {
    SYSTEM("0"),
    LIGHT("1"),
    DARK("2");

    fun getString(resourcesProvider: IResourcesProvider): String {
        return resourcesProvider.getString(
            when (this) {
                SYSTEM -> R.string.title_settings_app_theme_system
                LIGHT -> R.string.title_settings_app_theme_light
                DARK -> R.string.title_settings_app_theme_dark
            }
        )
    }

    companion object {
        fun getById(id: String): AppTheme? {
            return values().firstOrNull { it.id == id }
        }
    }

}