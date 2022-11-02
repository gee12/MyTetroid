package com.gee12.mytetroid.views.dialogs

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
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.focusAndShowKeyboard
import com.gee12.mytetroid.common.extensions.hideKeyboard
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.viewmodels.BaseStorageViewModel
import com.gee12.mytetroid.viewmodels.BaseViewModel
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.views.IViewEventListener
import com.gee12.mytetroid.views.TetroidMessage
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

abstract class TetroidDialogFragment<VM : BaseViewModel> : DialogFragment() {

    private val viewEventListener: IViewEventListener?
        get() = requireActivity() as IViewEventListener?

    lateinit var dialog: AlertDialog

    lateinit var dialogView: View

    protected lateinit var viewModel: VM

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

    protected open var storageId: Int? = null

    protected abstract fun getLayoutResourceId(): Int

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
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

    abstract fun createViewModel()

    protected open fun initViewModel() {
        viewModel.logDebug(getString(R.string.log_dialog_opened_mask, javaClass.simpleName))

        viewModel.messageObservable.observe(requireActivity()) { TetroidMessage.show(requireActivity(), it) }
        lifecycleScope.launch {
            viewModel.viewEventFlow.collect { (event, data) -> onViewEvent(event, data) }
        }

        // для диалогов, которым необходимо хранилище
        if (viewModel is StorageViewModel) {
            val storageViewModel = viewModel as StorageViewModel
            lifecycleScope.launch {
                storageViewModel.storageEventFlow.collect { (state, data) -> onStorageEvent(state, data) }
            }
            storageViewModel.startInitStorageFromBase(storageId ?: CommonSettings.getLastStorageId(context))
        }
    }

    protected open fun onViewEvent(event: Constants.ViewEvents, data: Any?) {
        when (event) {
            Constants.ViewEvents.ShowProgress -> viewEventListener?.setProgressVisibility(data as? Boolean ?: false)
            Constants.ViewEvents.ShowProgressText -> viewEventListener?.setProgressText(data as? String)
            Constants.ViewEvents.TaskStarted -> viewEventListener?.setProgressVisibility(true, data as String)
            Constants.ViewEvents.TaskFinished -> viewEventListener?.setProgressVisibility(false)
            Constants.ViewEvents.ShowMoreInLogs -> viewEventListener?.showSnackMoreInLogs()
            else -> {}
        }
    }

    open fun onStorageEvent(event: Constants.StorageEvents?, data: Any?) {
        when (event) {
            Constants.StorageEvents.Inited -> onStorageInited()
            else -> {}
        }
    }

    open fun onStorageInited() {}

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
        dialog.setTitle(title ?: "")
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

}