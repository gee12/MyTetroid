package com.gee12.mytetroid.common.extensions

import android.text.Spanned
import androidx.core.text.HtmlCompat

fun String.fromHtml(): Spanned {
    return HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY)
}

fun String.trimStartSubstring(start: String): String {
    return if (startsWith(start)) {
        substring(start.length)
    } else {
        this
    }
}