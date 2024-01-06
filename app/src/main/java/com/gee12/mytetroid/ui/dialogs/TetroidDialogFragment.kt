package com.gee12.mytetroid.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.gee12.mytetroid.R
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.logs.Message
import com.gee12.mytetroid.ui.TetroidMessage
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.BaseViewModel
import com.gee12.mytetroid.ui.base.ITetroidComponent
import kotlinx.coroutines.launch
import org.koin.core.scope.Scope
import org.koin.core.scope.get

abstract class TetroidDialogFragment<VM : BaseViewModel> : BaseDialogFragment() {

    protected lateinit var scopeSource: ScopeSource

    protected val koinScope: Scope
        get() = scopeSource.scope

    protected lateinit var viewModel: VM

    private val componentListener: ITetroidComponent?
        get() = requireActivity() as? ITetroidComponent

    protected abstract fun getViewModelClazz(): Class<VM>


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        createDependencyScope()
        createViewModel()
        initViewModel()

        return super.onCreateDialog(savedInstanceState)
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
            is BaseEvent.ShowProgress -> componentListener?.setProgressVisibility(true)
            is BaseEvent.HideProgress -> componentListener?.setProgressVisibility(false)
            is BaseEvent.ShowProgressWithText -> componentListener?.showProgress(event.message)
            is BaseEvent.TaskStarted -> {
                componentListener?.setProgressVisibility(true, event.titleResId?.let { getString(it) })
            }
            BaseEvent.TaskFinished -> componentListener?.setProgressVisibility(false)
            BaseEvent.ShowMoreInLogs -> componentListener?.showSnackMoreInLogs()
            else -> {}
        }
    }

    protected fun showMessage(message: Message) {
        TetroidMessage.show(requireActivity(), message)
    }

}