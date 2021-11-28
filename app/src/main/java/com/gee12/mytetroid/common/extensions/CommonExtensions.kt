package com.gee12.mytetroid.common.extensions

import com.gee12.htmlwysiwygeditor.Dialogs

fun Dialogs.IApplyResult.toApplyCancelResult() = object : Dialogs.IApplyCancelResult {
    override fun onApply() {
        this@toApplyCancelResult.onApply()
    }
    override fun onCancel() {}
}