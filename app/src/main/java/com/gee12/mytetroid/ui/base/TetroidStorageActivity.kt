package com.gee12.mytetroid.ui.base

import android.view.*
import androidx.documentfile.provider.DocumentFile
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.settings.storage.StorageSettingsActivity.Companion.newIntent
import com.gee12.mytetroid.ui.storages.StoragesActivity.Companion.start
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.storage.StorageEvent

abstract class TetroidStorageActivity<VM : BaseStorageViewModel> : TetroidActivity<VM>() {

    override fun createDependencyScope() {
        val currentStorageProvider = ScopeSource.current.scope.get<IStorageProvider>()
        val currentStorageId = currentStorageProvider.storage?.id
        // создавать новый koin scope или использовать существующий current.scope
        scopeSource = if (currentStorageId?.let { it == getStorageId() } == true) {
            ScopeSource.current
        } else {
            ScopeSource.createNew()
        }
    }

    fun getStorageId(): Int {
        return intent.getIntExtra(Constants.EXTRA_STORAGE_ID, 0)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (scopeSource != ScopeSource.current) {
            scopeSource.scope.close()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is StorageEvent -> onStorageEvent(event)
            else -> super.onBaseEvent(event)
        }
    }

    /**
     * Обработчик изменения состояния хранилища.
     * @param event
     * @param data
     */
    protected open fun onStorageEvent(event: StorageEvent) {
        when (event) {
            StorageEvent.NoDefaultStorage -> {
                AskDialogs.showYesNoDialog(
                    context = this,
                    messageResId = R.string.ask_no_set_default_storage,
                    onApply = {
                        showStoragesActivity()
                    },
                    onCancel = {},
                )
            }
            is StorageEvent.Inited -> afterStorageInited()
            is StorageEvent.StartLoadingOrDecrypting -> beforeStorageLoadedOrDecrypted()
            is StorageEvent.Loaded -> afterStorageLoaded(isLoaded = event.isLoaded)
            StorageEvent.Decrypted -> afterStorageDecrypted()
            else -> {}
        }
    }

    open fun afterStorageInited() {
    }

    open fun beforeStorageLoadedOrDecrypted() {

    }

    open fun afterStorageLoaded(isLoaded: Boolean) {
    }

    open fun afterStorageDecrypted() {
    }

    // region File

    override fun isUseFileStorage() = true

    override fun onStorageAccessGranted(requestCode: Int, root: DocumentFile) {
        viewModel.onStorageAccessGranted(requestCode, root)
    }

    // endregion File

    protected fun showStoragesActivity() {
        start(this, Constants.REQUEST_CODE_STORAGES_ACTIVITY)
    }

    protected fun showStorageSettingsActivity(storage: TetroidStorage?) {
        if (storage == null) return
        startActivityForResult(newIntent(this, storage), Constants.REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY)
    }

}