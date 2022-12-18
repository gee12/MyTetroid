package com.gee12.mytetroid.common.extensions

import android.content.Intent


inline fun buildIntent(predicate: Intent.() -> Unit): Intent {
    return Intent().apply {
        predicate()
    }
}