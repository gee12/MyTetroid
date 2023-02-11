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
import androidx.lifecycle.lifecycleScope
import com.gee12.htmlwysiwygeditor.Dialogs
import com.gee12.mytetroid.R
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.common.extensions.focusAndShowKeyboard
import com.gee12.mytetroid.common.extensions.hideKeyboard
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.logs.Message
import com.gee12.mytetroid.ui.base.BaseViewModel
import com.gee12.mytetroid.ui.base.IViewEventListener
import com.gee12.mytetroid.ui.TetroidMessage
import kotlinx.coroutines.launch
import org.koin.core.scope.Scope
import org.koin.core.scope.get

abstract class TetroidDialogFragment<VM : BaseViewModel> : DialogFragment() {

    protected lateinit var scopeSource: ScopeSource

    protected val koinScope: Scope
        get() = scopeSource.scope

    protected lateinit var viewModel: VM

    private val viewEventListener: IViewEventListener?
        get() = requireActivity() as? IViewEventListener

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

    protected abstract fun getViewModelClazz(): Class<VM>


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        createDependencyScope()
        createViewModel()
        initViewModel()

        val builder = Dialogs.AskDialogBuilder.create(context, getLayoutResourceId())
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

    protected open fun createDependencyScope() {
        scopeSource = ScopeSource()
    }

    protected open fun createViewModel() {
        this.viewModel = koinScope.get(getViewModelClazz())
    }

    protected open fun initViewModel() {
        viewModel.logDebug(getString(R.string.log_dialog_opened_mask, javaClass.simpleName))

        lifecycleScope.launch {
            viewModel.eventFlow.collect { event -> onBaseEvent(event) }
        }
        lifecycleScope.launch {
            viewModel.messageEventFlow.collect { message -> showMessage(message) }
        }
    }

    protected open fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is BaseEvent.ShowProgress -> viewEventListener?.setProgressVisibility(event.isVisible)
            is BaseEvent.ShowProgressText -> viewEventListener?.showProgress(event.message)
            is BaseEvent.TaskStarted -> {
                viewEventListener?.setProgressVisibility(true, event.titleResId?.let { getString(it) })
            }
            BaseEvent.TaskFinished -> viewEventListener?.setProgressVisibility(false)
            BaseEvent.ShowMoreInLogs -> viewEventListener?.showSnackMoreInLogs()
            else -> {}
        }
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

    open fun onDialogBuilderCreated(builder: Dialogs.AskDialogBuilder) {}

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

    fun setTitle(title: String?) {
        dialog.setTitle(title.orEmpty())
    }

    fun setTitle(resId: Int) {
        dialog.setTitle(resId)
    }

    fun setPositiveButton(resId: Int, isCloseDialog: Boolean = true, listener: DialogInterface.OnClickListener? = null) {
        if (!isCloseDialog) {
            onPositiveButtonCallback = listener
            setButton(AlertDialog.BUTTON_POSITIVE, resId, null)
        } else {
            setButton(AlertDialog.BUTTON_POSITIVE, resId, listener)
        }
    }

    fun setNegativeButton(resId: Int, isCloseDialog: Boolean = true, listener: DialogInterface.OnClickListener? = null) {
        if (!isCloseDialog) {
            onNegativeButtonCallback = listener
            setButton(AlertDialog.BUTTON_NEGATIVE, resId, null)
        } else {
            setButton(AlertDialog.BUTTON_NEGATIVE, resId, listener)
        }
    }

    fun setNeutralButton(resId: Int, isCloseDialog: Boolean = true, listener: DialogInterface.OnClickListener? = null) {
        if (!isCloseDialog) {
            onNeutralButtonCallback = listener
            setButton(AlertDialog.BUTTON_NEUTRAL, resId, null)
        } else {
            setButton(AlertDialog.BUTTON_NEUTRAL, resId, listener)
        }
    }

    fun setButton(type: Int, resId: Int, listener: DialogInterface.OnClickListener?) {
        dialog.setButton(type, getString(resId), listener)
    }

    fun getPositiveButton() = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

    fun getNegativeButton() = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

    fun getNeutralButton() = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)

    protected fun showKeyboard(view: View) {
        dialog.window?.decorView?.post { view.focusAndShowKeyboard() }
    }

    protected fun hideKeyboard(view: View) {
        view.hideKeyboard()
    }

    protected fun showMessage(message: Message) {
        TetroidMessage.show(requireActivity(), message)
    }

}