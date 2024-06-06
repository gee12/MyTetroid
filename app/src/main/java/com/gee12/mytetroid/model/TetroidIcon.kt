package com.gee12.mytetroid.model

import android.graphics.drawable.Drawable
import com.gee12.mytetroid.common.extensions.makePath
import java.io.File

class TetroidIcon(
    val folder: String,
    val name: String,
    var icon: Drawable? = null
) {
    val path: String
        get() = File.separator + makePath(folder, name)
}
