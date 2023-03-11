package com.gee12.mytetroid.model

enum class ImageFileType(
    val extension: String,
    val supportedAsNodeIcon: Boolean,
) {
    SVG("svg", supportedAsNodeIcon = true),
    PNG("png", supportedAsNodeIcon = true);

    companion object {

        fun extensions() = values().map { it.extension }

        fun fromExtension(extension: String) = values().firstOrNull { it.extension == extension }

    }

}