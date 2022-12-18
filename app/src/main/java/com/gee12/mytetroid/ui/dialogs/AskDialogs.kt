package com.gee12.mytetroid.ui.dialogs

import android.content.Context
import androidx.annotation.StringRes
import com.gee12.mytetroid.R

object AskDialogs {

    fun showYesNoDialog(
        context: Context,
        isCancelable: Boolean = true,
        @StringRes messageResId: Int,
        onApply: () -> Unit,
        onCancel: () -> Unit,
        onDismiss: (() -> Unit)? = null,
    ) {
        showYesNoDialog(
            context = context,
            isCancelable = isCancelable,
            message = context.getString(messageResId),
            onApply = onApply,
            onCancel = onCancel,
            onDismiss = onDismiss,
        )
    }

    fun showYesNoDialog(
        context: Context,
        isCancelable: Boolean = true,
        message: CharSequence,
        onApply: () -> Unit,
        onCancel: () -> Unit,
        onDismiss: (() -> Unit)? = null,
    ) {
        showDialog(
            context = context,
            isCancelable = isCancelable,
            message = message,
            applyResId = R.string.answer_yes,
            cancelResId = R.string.answer_no,
            onApply = onApply,
            onCancel = onCancel,
            onDismiss = onDismiss,
        )
    }

    fun showYesDialog(
        context: Context,
        @StringRes messageResId: Int,
        onApply: () -> Unit,
    ) {
        showYesDialog(
            context = context,
            message = context.getString(messageResId),
            onApply = onApply,
        )
    }

    @JvmStatic
    fun showYesDialog(
        context: Context,
        message: String,
        onApply: () -> Unit,
    ) {
        showDialog(
            context = context,
            message = message,
            isCancelable = true,
            applyResId = R.string.answer_yes,
            onApply = onApply,
            onCancel = {},
        )
    }

    fun showOkDialog(
        context: Context,
        @StringRes messageRes: Int,
        @StringRes applyResId: Int,
        isCancelable: Boolean,
        onApply: () -> Unit,
    ) {
        showDialog(
            context = context,
            message = context.getString(messageRes),
            isCancelable = isCancelable,
            applyResId = applyResId,
            onApply = onApply,
        )
    }

    fun showDialog(
        context: Context,
        message: CharSequence,
        isCancelable: Boolean,
        @StringRes applyResId: Int,
        @StringRes cancelResId: Int = R.string.cancel,
        onApply: () -> Unit,
        onCancel: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
    ) {
        BaseAskDialog.show(
            context = context,
            message = message,
            isCancelable = isCancelable,
            applyResId = applyResId,
            cancelResId = cancelResId,
            onApply = onApply,
            onCancel = onCancel,
            onDismiss = onDismiss,
        )
    }

}