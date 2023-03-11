package com.gee12.mytetroid.ui.dialogs

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

// TODO: переписать на DialogFragment
object BaseAskDialog {

    fun show(
        context: Context,
        message: CharSequence,
        isCancelable: Boolean,
        @StringRes applyResId: Int,
        @StringRes neutralResId: Int? = null,
        @StringRes cancelResId: Int,
        onApply: () -> Unit,
        onNeutral: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
    ) {
        val builder = AlertDialog.Builder(context).apply {
            setMessage(message)
            setCancelable(isCancelable)
            setPositiveButton(applyResId) { _: DialogInterface?, _: Int ->
                onApply()
            }
            if (onNeutral != null && neutralResId != null) {
                setNeutralButton(neutralResId) { _: DialogInterface?, _: Int ->
                    onNeutral()
                }
            }
            if (onCancel != null) {
                setNegativeButton(cancelResId) { _: DialogInterface?, _: Int ->
                    onCancel()
                }
            }
        }
        val dialog = builder.create().apply {
//        dialog.setCanceledOnTouchOutside();
            if (onDismiss != null) {
                setOnDismissListener {
                    onDismiss()
                }
            }
        }
        dialog.show()
    }

}