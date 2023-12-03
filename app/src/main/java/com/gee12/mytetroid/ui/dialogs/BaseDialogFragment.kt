package com.gee12.mytetroid.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.gee12.htmlwysiwygeditor.dialog.AskDialogBuilder
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.focusAndShowKeyboard
import com.gee12.mytetroid.common.extensions.hideKeyboard
import com.gee12.mytetroid.common.extensions.resizeWindowWithKeyboard

abstract class BaseDialogFragment : DialogFragment() {

    lateinit var dialog: AlertDialog

    lateinit var dialogView: View

    abstract fun getRequiredTag(): String

    abstract fun isPossibleToShow(): Boolean

    interface OnDialogOkCallback {
        fun onPositive()
    }

    interface OnDialogYesNoCallback : OnDialogOkCallback {
        fun onNegative()
    }

    interface OnDialogYesNoCancelCallback : OnDialogYesNoCallback {
        fun onCancel()
    }

    protected var onPositiveButtonCallback: DialogInterface.OnClickListener? = null
    protected var onNegativeButtonCallback: DialogInterface.OnClickListener? = null
    protected var onNeutralButtonCallback: DialogInterface.OnClickListener? = null

    protected abstract fun getLayoutResourceId(): Int


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AskDialogBuilder.create(
            context = requireContext(),
            layoutResId = getLayoutResourceId(),
            themeResId = R.style.AppDialog,
        )
        onDialogBuilderCreated(builder)

        dialogView = builder.view

        dialog = builder.create()
        onDialogCreated(dialog, dialogView)

        dialog.setOnShowListener {
            initButtons()
            onDialogShowed(dialog, dialogView)
        }

        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return dialogView
    }

    protected open fun initButtons() {
        onPositiveButtonCallback?.let { callback ->
            getPositiveButton()?.setOnClickListener { callback.onClick(null, 0) }
        }
        onNegativeButtonCallback?.let { callback ->
            getNegativeButton()?.setOnClickListener { callback.onClick(null, 0) }
        }
        onNeutralButtonCallback?.let { callback ->
            getNeutralButton()?.setOnClickListener { callback.onClick(null, 0) }
        }
    }

    open fun onDialogBuilderCreated(builder: AskDialogBuilder) {}

    open fun onDialogCreated(dialog: AlertDialog, view: View) {}

    open fun onDialogShowed(dialog: AlertDialog, view: View) {}

    fun showIfPossible(manager: FragmentManager) {
        if (isPossibleToShow()) {
            show(manager, getRequiredTag())
        } else {
            Log.i(getRequiredTag(), "Dialog fragment is not possible to show.")
        }
    }

    fun showIfNeeded(manager: FragmentManager?) {
        manager?.apply {
            if (findFragmentByTag(getRequiredTag()) == null) {
                show(this, getRequiredTag())
            }
        }
    }

    protected fun setTitle(title: String?) {
        dialog.setTitle(title.orEmpty())
    }

    protected fun setTitle(resId: Int) {
        dialog.setTitle(resId)
    }

    protected fun setPositiveButton(resId: Int, isCloseDialog: Boolean = true, listener: DialogInterface.OnClickListener? = null) {
        if (!isCloseDialog) {
            onPositiveButtonCallback = listener
            setButton(AlertDialog.BUTTON_POSITIVE, resId, null)
        } else {
            setButton(AlertDialog.BUTTON_POSITIVE, resId, listener)
        }
    }

    protected fun setNegativeButton(resId: Int, isCloseDialog: Boolean = true, listener: DialogInterface.OnClickListener? = null) {
        if (!isCloseDialog) {
            onNegativeButtonCallback = listener
            setButton(AlertDialog.BUTTON_NEGATIVE, resId, null)
        } else {
            setButton(AlertDialog.BUTTON_NEGATIVE, resId, listener)
        }
    }

    protected fun setNeutralButton(resId: Int, isCloseDialog: Boolean = true, listener: DialogInterface.OnClickListener? = null) {
        if (!isCloseDialog) {
            onNeutralButtonCallback = listener
            setButton(AlertDialog.BUTTON_NEUTRAL, resId, null)
        } else {
            setButton(AlertDialog.BUTTON_NEUTRAL, resId, listener)
        }
    }

    protected fun setButton(type: Int, resId: Int, listener: DialogInterface.OnClickListener?) {
        dialog.setButton(type, getString(resId), listener)
    }

    protected fun getPositiveButton() = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

    protected fun getNegativeButton() = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

    protected fun getNeutralButton() = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)

    protected fun showKeyboard(view: View, isResizeWindow: Boolean = true) {
        if (isResizeWindow) {
            dialog.window?.resizeWindowWithKeyboard(dialogView)
        }
        dialog.window?.decorView?.post { view.focusAndShowKeyboard() }
    }

    protected fun hideKeyboard(view: View) {
        view.hideKeyboard()
    }

}