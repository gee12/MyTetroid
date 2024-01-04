package com.gee12.mytetroid.ui.dialogs

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

// TODO: переписать на DialogFragment
object BaseAskDialog {

    fun show(
        context: Context,
        theme: Int = 0,
        title: CharSequence? = null,
        message: CharSequence,
        isCancelable: Boolean,
        @StringRes positiveResId: Int,
        @StringRes neutralResId: Int? = null,
        @StringRes negativeResId: Int,
        onApply: () -> Unit,
        onNeutral: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
    ) {
        val builder = AlertDialog.Builder(context, theme).apply {
            title?.also { setTitle(it) }
            setMessage(message)
            setCancelable(isCancelable)
            setPositiveButton(positiveResId) { _: DialogInterface?, _: Int ->
                onApply()
            }
            if (onNeutral != null && neutralResId != null) {
                setNeutralButton(neutralResId) { _: DialogInterface?, _: Int ->
                    onNeutral()
                }
            }
            if (onCancel != null) {
                setNegativeButton(negativeResId) { _: DialogInterface?, _: Int ->
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