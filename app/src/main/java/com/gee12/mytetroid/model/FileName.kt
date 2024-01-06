package com.gee12.mytetroid.model


sealed class FileName {

    abstract val fullName: String

    data class FromParts(
        val base: String,
        val extension: String,
    ) : FileName() {
        override val fullName = "$base.$extension"
    }

    data class FromFullName(
        override val fullName: String,
    ) : FileName()

}
