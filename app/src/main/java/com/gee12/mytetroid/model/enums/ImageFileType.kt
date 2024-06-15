package com.gee12.mytetroid.model.enums

enum class ImageFileType(
    val extension: String,
    val supportedAsNodeIcon: Boolean,
) {
    SVG("svg", supportedAsNodeIcon = true),
    PNG("png", supportedAsNodeIcon = true),
    JPG("jpg", supportedAsNodeIcon = false),
    WEBP("webp", supportedAsNodeIcon = false);

    companion object {

        fun extensions() = values().map { it.extension }

        fun fromExtension(extension: String) = values().firstOrNull { it.extension == extension }

    }

}