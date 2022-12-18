package com.gee12.mytetroid.common.extensions

import android.content.Intent
import com.gee12.htmlwysiwygeditor.Dialogs

fun Dialogs.IApplyResult.toApplyCancelResult() = object : Dialogs.IApplyCancelResult {
    override fun onApply() {
        this@toApplyCancelResult.onApply()
    }
    override fun onCancel() {}
}

inline fun buildIntent(predicate: Intent.() -> Unit): Intent {
    return Intent().apply {
        predicate()
    }
}