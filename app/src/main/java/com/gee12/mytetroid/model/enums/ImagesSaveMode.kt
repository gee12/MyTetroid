package com.gee12.mytetroid.model.enums

import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider

enum class ImagesSaveMode(val id: Int) {
    AS_IS(0),
    CONVERT_TO_PNG(1),
    CONVERT_TO_JPG(2);

    fun getTitle(resourcesProvider: IResourcesProvider): String {
        return resourcesProvider.getString(
            when (this) {
                AS_IS -> R.string.title_settings_images_save_mode_as_is
                CONVERT_TO_PNG -> R.string.title_settings_images_save_mode_convert_to_png
                CONVERT_TO_JPG -> R.string.title_settings_images_save_mode_convert_to_jpg
            }
        )
    }

    fun getDescription(resourcesProvider: IResourcesProvider): String {
        return resourcesProvider.getString(
            when (this) {
                AS_IS -> R.string.title_settings_images_save_mode_as_is_summ
                CONVERT_TO_PNG -> R.string.title_settings_images_save_mode_convert_to_png_summ
                CONVERT_TO_JPG -> R.string.title_settings_images_save_mode_convert_to_jpg_summ
            }
        )
    }

    companion object {
        fun getById(id: Int) = values().firstOrNull { it.id == id }
    }
}