package com.gee12.mytetroid.ui.dialogs

import com.gee12.mytetroid.common.extensions.ifTrueOrNull
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.providers.IStorageProvider
import com.gee12.mytetroid.viewmodels.BaseEvent
import com.gee12.mytetroid.viewmodels.BaseStorageViewModel
import com.gee12.mytetroid.viewmodels.StorageViewModel.StorageEvent

abstract class TetroidStorageDialogFragment<VM : BaseStorageViewModel> : TetroidDialogFragment<VM>() {

    protected open var storageId: Int? = null

    protected open var isInitCurrentStorage: Boolean = true

    override fun createDependencyScope() {
        val currentStorageProvider = ScopeSource.current.scope.get<IStorageProvider>()
        val currentStorageId = currentStorageProvider.storage?.id
        // создавать новый koin scope или использовать существующий current.scope
        scopeSource = if (currentStorageId?.let { it == storageId } == true) {
            ScopeSource.current
        } else {
            ScopeSource.createNew()
        }
    }

    override fun initViewModel() {
        super.initViewModel()

        storageId
            ?: isInitCurrentStorage.ifTrueOrNull {
                viewModel.getStorageId()
            }?.let {
                viewModel.startInitStorageFromBase(storageId = it)
            }
    }

    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is StorageEvent -> onStorageEvent(event)
            else -> super.onBaseEvent(event)
        }
    }

    open fun onStorageEvent(event: StorageEvent) {
        when (event) {
            is StorageEvent.Inited -> onStorageInited(event.storage)
            else -> {}
        }
    }

    open fun onStorageInited(storage: TetroidStorage) {}

}