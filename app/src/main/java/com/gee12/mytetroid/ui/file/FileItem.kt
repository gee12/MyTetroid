package com.gee12.mytetroid.ui.file

data class FileItem(
    var name: String,
    var isFolder: Boolean,
    var isMoveUp: Boolean = false,
)
