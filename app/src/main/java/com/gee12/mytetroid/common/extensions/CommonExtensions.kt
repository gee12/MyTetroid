package com.gee12.mytetroid.common.extensions

import android.app.Activity
import android.content.Intent


inline fun buildIntent(predicate: Intent.() -> Unit): Intent {
    return Intent().apply {
        predicate()
    }
}

fun Activity.startActivityForResultSafety(
    intent: Intent,
    requestCode: Int
): Boolean {
    return try {
        this.startActivityForResult(intent, requestCode)
        true
    } catch (ex: Exception) {
        false
    }
}
