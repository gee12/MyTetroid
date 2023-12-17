package com.gee12.mytetroid.common.extensions

import android.app.Activity
import android.content.Intent
import android.os.Bundle


inline fun buildIntent(predicate: Intent.() -> Unit): Intent {
    return Intent().apply {
        predicate()
    }
}

inline fun buildBundle(predicate: Bundle.() -> Unit): Bundle {
    return Bundle().apply {
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
