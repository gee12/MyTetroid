package com.gee12.mytetroid.ui.dialogs

import android.content.Context
import androidx.annotation.StringRes
import com.gee12.mytetroid.R

object AskDialogs {

    fun showYesNoDialog(
        context: Context,
        isCancelable: Boolean = true,
        @StringRes titleResId: Int? = null,
        @StringRes messageResId: Int,
        onApply: () -> Unit,
        onCancel: () -> Unit,
        onDismiss: (() -> Unit)? = null,
    ) {
        showYesNoDialog(
            context = context,
            isCancelable = isCancelable,
            title = titleResId?.let { context.getString(titleResId) },
            message = context.getString(messageResId),
            onApply = onApply,
            onCancel = onCancel,
            onDismiss = onDismiss,
        )
    }

    fun showYesNoDialog(
        context: Context,
        isCancelable: Boolean = true,
        title: CharSequence? = null,
        message: CharSequence,
        onApply: () -> Unit,
        onCancel: () -> Unit,
        onDismiss: (() -> Unit)? = null,
    ) {
        showDialog(
            context = context,
            isCancelable = isCancelable,
            title = title,
            message = message,
            positiveResId = R.string.answer_yes,
            negativeResId = R.string.answer_no,
            onApply = onApply,
            onCancel = onCancel,
            onDismiss = onDismiss,
        )
    }

    fun showYesDialog(
        context: Context,
        @StringRes titleResId: Int? = null,
        @StringRes messageResId: Int,
        onApply: () -> Unit,
    ) {
        showYesDialog(
            context = context,
            title = titleResId?.let { context.getString(titleResId) },
            message = context.getString(messageResId),
            onApply = onApply,
        )
    }

    @JvmStatic
    fun showYesDialog(
        context: Context,
        title: CharSequence? = null,
        message: String,
        onApply: () -> Unit,
    ) {
        showDialog(
            context = context,
            title = title,
            message = message,
            isCancelable = true,
            positiveResId = R.string.answer_yes,
            onApply = onApply,
            onCancel = {},
        )
    }

    fun showOkDialog(
        context: Context,
        @StringRes titleResId: Int? = null,
        @StringRes messageResId: Int,
        @StringRes applyResId: Int,
        isCancelable: Boolean,
        onApply: () -> Unit,
    ) {
        showDialog(
            context = context,
            title = titleResId?.let { context.getString(titleResId) },
            message = context.getString(messageResId),
            isCancelable = isCancelable,
            positiveResId = applyResId,
            onApply = onApply,
        )
    }

    fun showOkCancelDialog(
        context: Context,
        title: CharSequence? = null,
        message: CharSequence,
        isCancelable: Boolean = true,
        @StringRes okResId: Int = R.string.answer_ok,
        @StringRes cancelResId: Int = R.string.action_cancel,
        onYes: () -> Unit,
        onCancel: () -> Unit,
        onDismiss: (() -> Unit)? = null,
    ) {
        showDialog(
            context = context,
            isCancelable = isCancelable,
            title = title,
            message = message,
            positiveResId = okResId,
            negativeResId = cancelResId,
            neutralResId = null,
            onApply = onYes,
            onNeutral = null,
            onCancel = onCancel,
            onDismiss = onDismiss,
        )
    }

    fun showYesNoCancelDialog(
        context: Context,
        title: CharSequence? = null,
        message: CharSequence,
        isCancelable: Boolean = true,
        @StringRes yesResId: Int = R.string.answer_yes,
        @StringRes noResId: Int = R.string.answer_no,
        @StringRes cancelResId: Int = R.string.action_cancel,
        onYes: () -> Unit,
        onNo: () -> Unit,
        onCancel: () -> Unit,
        onDismiss: (() -> Unit)? = null,
    ) {
        showDialog(
            context = context,
            isCancelable = isCancelable,
            title = title,
            message = message,
            positiveResId = yesResId,
            negativeResId = cancelResId,
            neutralResId = noResId,
            onApply = onYes,
            onNeutral = onNo,
            onCancel = onCancel,
            onDismiss = onDismiss,
        )
    }

    fun showDialog(
        context: Context,
        theme: Int = R.style.AppDialog,
        title: CharSequence? = null,
        message: CharSequence,
        isCancelable: Boolean,
        @StringRes positiveResId: Int,
        @StringRes negativeResId: Int = R.string.action_cancel,
        @StringRes neutralResId: Int? = null,
        onApply: () -> Unit,
        onNeutral: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
    ) {
        BaseAskDialog.show(
            context = context,
            theme = theme,
            title = title,
            message = message,
            isCancelable = isCancelable,
            positiveResId = positiveResId,
            negativeResId = negativeResId,
            neutralResId = neutralResId,
            onApply = onApply,
            onNeutral = onNeutral,
            onCancel = onCancel,
            onDismiss = onDismiss,
        )
    }

}